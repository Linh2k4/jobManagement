package com.company.jobmanagement.service;

import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.model.entity.*;
import com.company.jobmanagement.model.enums.Difficulty;
import com.company.jobmanagement.model.enums.Priority;
import com.company.jobmanagement.model.enums.TaskStatus;
import com.company.jobmanagement.model.enums.TimeCategory;
import com.company.jobmanagement.repository.GroupRepository;
import com.company.jobmanagement.repository.KpiComponentRepository;
import com.company.jobmanagement.repository.KpiConfigurationRepository;
import com.company.jobmanagement.repository.TaskRepository;
import com.company.jobmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

/**
 * KPI calculation service implementing the complex weighted formula from Scope.md
 *
 * Formula:
 * 1. Task Weight = difficulty × min(estimateHours, 8) × priorityMultiplier × typeMultiplier
 * 2. Task Completion Score = taskWeight × timelinessFactor
 * 3. WCR = Σ(completionScore) / Σ(taskWeight) × 100
 * 4. VI = Σ(effectiveHours of DONE tasks) / workingHours × 100 (capped 120)
 * 5. EA = (1 - avg(|actual-estimate|/estimate)) × 100
 * 6. AutoScore = (WCR × 60%) + (VI × 25%) + (EA × 15%)
 * 7. KPI Final = (AutoScore × 40%) + (LeadScore × 35%) + (ManagerScore × 25%)
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class KpiCalculationService {

    private final KpiComponentRepository kpiComponentRepository;
    private final KpiConfigurationRepository kpiConfigurationRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    private static final int MAX_EFFECTIVE_HOURS = 8;  // Cap per task
    private static final int MAX_VOLUME_INDEX = 120;   // Allow 120% for overtime
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * Calculate complete KPI for a user in a period, scoped to one Group
     * (Scope.md §13 — null groupId keeps the pre-§13 "all tasks" behavior).
     */
    public KpiComponent calculateKpi(Long userId, Long groupId, YearMonth periodMonth) {
        KpiConfiguration config = kpiConfigurationRepository.findByPeriod(periodMonth)
                .orElseGet(() -> getLatestConfiguration(periodMonth));

        List<Task> tasks = taskRepository.findTasksForKpiCalculation(userId, periodMonth.getYear(), periodMonth.getMonthValue(), groupId);

        // Calculate components
        BigDecimal wcr = calculateWeightedCompletionRate(tasks, config);
        BigDecimal vi = calculateVolumeIndex(tasks, config);
        BigDecimal ea = calculateEstimateAccuracy(tasks);

        // Calculate auto score
        BigDecimal autoScore = wcr.multiply(BigDecimal.valueOf(config.getWcrWeight()))
                .add(vi.multiply(BigDecimal.valueOf(config.getViWeight())))
                .add(ea.multiply(BigDecimal.valueOf(config.getEaWeight())))
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        log.info("KPI calculation for user={}, period={}: WCR={}, VI={}, EA={}, AutoScore={}",
                 userId, periodMonth, wcr, vi, ea, autoScore);

        // Load the full managed User (not a bare-id stub) so downstream mapping
        // to KpiResponse (e.g. userFullName) works without a lazy Hibernate proxy
        // that could outlive this transaction.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Group group = groupId != null
                ? groupRepository.findById(groupId).orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId))
                : null;

        return KpiComponent.builder()
                .user(user)
                .group(group)
                .periodMonth(periodMonth)
                .wcr(wcr)
                .vi(vi)
                .ea(ea)
                .autoScore(autoScore)
                .build();
    }

    /**
     * Calculate Weighted Completion Rate (WCR).
     *
     * Formula: Σ(completionScore) / Σ(taskWeight) × 100
     * where:
     * - taskWeight = difficulty × min(estimateHours, 8) × priorityMultiplier × typeMultiplier
     * - timelinessFactor: DONE=1.0, CLOSED_LATE=0.6, OVERDUE=0.0
     * - only include tasks with estimate > 0
     */
    private BigDecimal calculateWeightedCompletionRate(List<Task> tasks, KpiConfiguration config) {
        BigDecimal totalCompletionScore = BigDecimal.ZERO;
        BigDecimal totalTaskWeight = BigDecimal.ZERO;

        for (Task task : tasks) {
            if (!task.hasEstimate()) continue;  // Skip tasks without estimate

            BigDecimal taskWeight = calculateTaskWeight(task);
            totalTaskWeight = totalTaskWeight.add(taskWeight);

            BigDecimal timelinessFactor = calculateTimelinessFactor(task);
            BigDecimal completionScore = taskWeight.multiply(timelinessFactor);
            totalCompletionScore = totalCompletionScore.add(completionScore);
        }

        if (totalTaskWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalCompletionScore
                .divide(totalTaskWeight, 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Volume Index (VI).
     *
     * Formula: Σ(effectiveHours of DONE/CLOSED_LATE tasks) / workingHoursInPeriod × 100
     * - effectiveHours = min(estimateHours, 8)
     * - capped at 120 (allows 120% for overtime recognition)
     * - normalized to 0-100
     */
    private BigDecimal calculateVolumeIndex(List<Task> tasks, KpiConfiguration config) {
        BigDecimal totalEffectiveHours = BigDecimal.ZERO;

        for (Task task : tasks) {
            if (!task.hasEstimate()) continue;

            if (task.isDone() || task.getStatus() == TaskStatus.CLOSED_LATE) {
                int effectiveHours = Math.min(task.getEstimateHours(), MAX_EFFECTIVE_HOURS);
                totalEffectiveHours = totalEffectiveHours.add(BigDecimal.valueOf(effectiveHours));
            }
        }

        int workingHours = config.getTotalWorkingHoursInPeriod();
        BigDecimal rawVI = totalEffectiveHours
                .divide(BigDecimal.valueOf(workingHours), 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED);

        // Cap at MAX_VOLUME_INDEX and normalize back to 0-100
        BigDecimal cappedVI = rawVI.compareTo(BigDecimal.valueOf(MAX_VOLUME_INDEX)) > 0
                ? BigDecimal.valueOf(MAX_VOLUME_INDEX)
                : rawVI;

        return cappedVI
                .divide(BigDecimal.valueOf(MAX_VOLUME_INDEX), 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Estimate Accuracy (EA).
     *
     * Formula: (1 - avg(|actual-estimate|/estimate)) × 100
     * - only include tasks with both estimate and actual hours
     * - capped at 100% deviation (don't penalize more than 100%)
     */
    private BigDecimal calculateEstimateAccuracy(List<Task> tasks) {
        BigDecimal totalDeviation = BigDecimal.ZERO;
        int countWithActual = 0;

        for (Task task : tasks) {
            if (!task.hasEstimate() || task.getActualHours() == null) continue;

            double estimate = task.getEstimateHours();
            double actual = task.getActualHours().doubleValue();
            double deviation = Math.abs(actual - estimate) / estimate;

            // Cap deviation at 100% (no extra penalty beyond 100%)
            deviation = Math.min(deviation, 1.0);
            totalDeviation = totalDeviation.add(BigDecimal.valueOf(deviation));
            countWithActual++;
        }

        if (countWithActual == 0) {
            return HUNDRED;  // Perfect if no actual data to evaluate
        }

        BigDecimal avgDeviation = totalDeviation
                .divide(BigDecimal.valueOf(countWithActual), 4, RoundingMode.HALF_UP);

        return BigDecimal.ONE
                .subtract(avgDeviation)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate individual task weight.
     *
     * Formula: difficulty × min(estimateHours, 8) × priorityMultiplier × typeMultiplier
     */
    private BigDecimal calculateTaskWeight(Task task) {
        int difficulty = task.getDifficultyLevel();
        int effectiveHours = Math.min(task.getEstimateHours(), MAX_EFFECTIVE_HOURS);
        double priorityMult = task.getPriority().getMultiplier();
        double typeMult = getTypeMultiplier(task.getTaskType());

        return BigDecimal.valueOf(difficulty)
                .multiply(BigDecimal.valueOf(effectiveHours))
                .multiply(BigDecimal.valueOf(priorityMult))
                .multiply(BigDecimal.valueOf(typeMult))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get type multiplier based on task type.
     * FAST: 1.0, OFTEN: 0.8 (easier since repetitive), MULTI_STEP: 1.3 (overhead of coordination)
     * <p>
     * Was comparing {@code taskType.getCode()} (a per-type code like
     * "EMAIL_REPLY"/"MONTHLY_REPORT", never literally "FAST"/"OFTEN"/
     * "MULTI_STEP") — every task silently fell through to the 1.0 default
     * regardless of type. Fixed to compare the actual {@link TimeCategory}.
     */
    private double getTypeMultiplier(TaskType taskType) {
        if (taskType == null || taskType.getTimeCategory() == null) return 1.0;

        switch (taskType.getTimeCategory()) {
            case FAST: return 1.0;
            case OFTEN: return 0.8;
            case MULTI_STEP: return 1.3;
            default: return 1.0;
        }
    }

    /**
     * Calculate timeliness factor based on task completion status.
     * DONE (on time): 1.0
     * CLOSED_LATE: 0.6
     * OVERDUE/other: 0.0
     */
    private BigDecimal calculateTimelinessFactor(Task task) {
        if (task.isDone()) {
            return BigDecimal.ONE;
        }
        if (task.getStatus() == TaskStatus.CLOSED_LATE) {
            return BigDecimal.valueOf(0.6);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get KPI configuration for a period, or default to latest.
     */
    private KpiConfiguration getLatestConfiguration(YearMonth periodMonth) {
        return kpiConfigurationRepository.findLatest()
                .orElseGet(() -> {
                    KpiConfiguration config = new KpiConfiguration();
                    config.setPeriodMonth(periodMonth);
                    return kpiConfigurationRepository.save(config);
                });
    }
}
