package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.*;
import com.company.jobmanagement.repository.GroupMembershipRepository;
import com.company.jobmanagement.repository.KpiComponentRepository;
import com.company.jobmanagement.repository.KpiSnapshotRepository;
import com.company.jobmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

/**
 * Batch KPI calculation service.
 * Runs nightly (2 AM) to recalculate KPI for all users.
 * Creates immutable snapshots for month-end reporting.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class KpiBatchService {

    private final KpiCalculationService kpiCalculationService;
    private final KpiComponentRepository kpiComponentRepository;
    private final KpiSnapshotRepository kpiSnapshotRepository;
    private final UserRepository userRepository;
    private final GroupMembershipRepository groupMembershipRepository;

    /**
     * Resolve the groupIds a user's KPI should be split across (Scope.md
     * §13). A user with no group memberships (Managers, or a Lead not
     * tracked as a Member of any group) falls back to a single
     * groupId=null calculation — the pre-§13 "all tasks" behavior.
     */
    private List<Long> resolveGroupIds(Long userId) {
        List<GroupMembership> memberships = groupMembershipRepository.findByMemberId(userId);
        if (memberships.isEmpty()) {
            return java.util.Collections.singletonList(null);
        }
        return memberships.stream().map(gm -> gm.getGroup().getId()).toList();
    }

    /**
     * Nightly KPI recalculation job.
     * Runs at 2 AM every day to update all user KPIs.
     * Takes ~30 seconds for 100 users.
     */
    @Scheduled(cron = "0 0 2 * * *")  // 2 AM daily
    public void recalculateAllKpiNightly() {
        log.info("Starting nightly KPI recalculation batch job");
        long startTime = System.currentTimeMillis();

        try {
            YearMonth currentPeriod = YearMonth.now();
            List<User> allUsers = userRepository.findAll();

            int processed = 0;
            int failed = 0;

            for (User user : allUsers) {
                for (Long groupId : resolveGroupIds(user.getId())) {
                    try {
                        KpiComponent kpi = kpiCalculationService.calculateKpi(user.getId(), groupId, currentPeriod);
                        kpi.setRanking(kpi.calculateRanking());
                        kpi.setTrend(kpi.calculateTrend());
                        kpiComponentRepository.save(kpi);
                        processed++;
                    } catch (Exception e) {
                        log.error("Failed to calculate KPI for user {} group {}", user.getId(), groupId, e);
                        failed++;
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Nightly KPI recalculation completed: processed={}, failed={}, duration={}ms",
                     processed, failed, duration);

        } catch (Exception e) {
            log.error("Nightly KPI recalculation batch job failed", e);
        }
    }

    /**
     * Month-end KPI snapshot job.
     * Runs at 11:59 PM on last day of month to create immutable snapshots.
     * Freezes current month's KPI for reporting and evaluation.
     */
    @Scheduled(cron = "0 59 23 28-31 * *")  // 11:59 PM on days 28-31 (catches last day of each month)
    public void createMonthlySnapshots() {
        log.info("Starting month-end KPI snapshot creation job");
        long startTime = System.currentTimeMillis();

        try {
            YearMonth currentPeriod = YearMonth.now();
            List<User> allUsers = userRepository.findAll();

            int snapshotCount = 0;

            for (User user : allUsers) {
                for (Long groupId : resolveGroupIds(user.getId())) {
                    try {
                        KpiComponent component = kpiComponentRepository
                                .findByUserAndGroupAndPeriod(user.getId(), groupId, currentPeriod)
                                .orElseGet(() -> kpiCalculationService.calculateKpi(user.getId(), groupId, currentPeriod));

                        KpiSnapshot snapshot = KpiSnapshot.builder()
                                .user(user)
                                .group(component.getGroup())
                                .lead(user.isLead() ? null : findUserLead(user, groupId))
                                .periodMonth(currentPeriod)
                                .wcr(component.getWcr())
                                .vi(component.getVi())
                                .ea(component.getEa())
                                .autoScore(component.getAutoScore())
                                .leadScore(component.getLeadScore())
                                .managerScore(component.getManagerScore())
                                .kpiFinal(component.getKpiFinal() != null ? component.getKpiFinal() : component.getAutoScore())
                                .ranking(component.calculateRanking())
                                .isLocked(true)
                                .build();

                        kpiSnapshotRepository.save(snapshot);
                        snapshotCount++;
                    } catch (Exception e) {
                        log.error("Failed to create snapshot for user {} group {}", user.getId(), groupId, e);
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Month-end snapshot creation completed: snapshots={}, duration={}ms", snapshotCount, duration);

        } catch (Exception e) {
            log.error("Month-end snapshot creation job failed", e);
        }
    }

    /**
     * Manual recalculation trigger for a specific user in one group.
     * Can be called after evaluation finalization to update final KPI.
     */
    public void recalculateUserKpi(Long userId, Long groupId, YearMonth periodMonth) {
        try {
            KpiComponent kpi = kpiCalculationService.calculateKpi(userId, groupId, periodMonth);
            kpi.setRanking(kpi.calculateRanking());
            kpi.setTrend(kpi.calculateTrend());
            kpiComponentRepository.save(kpi);
            log.info("Manually recalculated KPI for user {} group {} in period {}", userId, groupId, periodMonth);
        } catch (Exception e) {
            log.error("Failed to manually recalculate KPI for user {} group {}", userId, groupId, e);
        }
    }

    /**
     * Find the Lead of the group a user's KPI is being snapshotted for.
     * groupId=null (no group membership at all) has no group Lead to
     * resolve, matching the pre-§13 stub's behavior for that case.
     */
    private User findUserLead(User user, Long groupId) {
        if (groupId == null) {
            return null;
        }
        return groupMembershipRepository.findByMemberId(user.getId()).stream()
                .filter(gm -> gm.getGroup().getId().equals(groupId))
                .findFirst()
                .map(gm -> gm.getGroup().getLead())
                .orElse(null);
    }
}
