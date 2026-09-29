package com.company.jobmanagement.controller.kpi;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.request.KpiConfigurationRequest;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.KpiResponse;
import com.company.jobmanagement.dto.response.MemberKpiDetailResponse;
import com.company.jobmanagement.dto.response.MemberKpiSummaryResponse;
import com.company.jobmanagement.model.entity.Evaluation;
import com.company.jobmanagement.model.entity.Group;
import com.company.jobmanagement.model.entity.GroupMembership;
import com.company.jobmanagement.model.entity.KpiComponent;
import com.company.jobmanagement.model.entity.KpiConfiguration;
import com.company.jobmanagement.model.entity.KpiSnapshot;
import com.company.jobmanagement.model.entity.Task;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.model.enums.Role;
import com.company.jobmanagement.model.enums.TaskStatus;
import com.company.jobmanagement.model.enums.TimeCategory;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.mapper.KpiComponentMapper;
import com.company.jobmanagement.security.CurrentUser;
import com.company.jobmanagement.service.KpiCacheService;
import com.company.jobmanagement.service.KpiCalculationService;
import com.company.jobmanagement.repository.EvaluationRepository;
import com.company.jobmanagement.repository.GroupMembershipRepository;
import com.company.jobmanagement.repository.KpiComponentRepository;
import com.company.jobmanagement.repository.KpiConfigurationRepository;
import com.company.jobmanagement.repository.KpiSnapshotRepository;
import com.company.jobmanagement.repository.TaskRepository;
import com.company.jobmanagement.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/kpi")
@RequiredArgsConstructor
@Tag(name = "KPI", description = "Real-time KPI calculation and reporting")
public class KpiController extends BaseController {

    private final KpiCacheService kpiCacheService;
    private final KpiCalculationService kpiCalculationService;
    private final KpiComponentRepository kpiComponentRepository;
    private final KpiConfigurationRepository kpiConfigurationRepository;
    private final KpiSnapshotRepository kpiSnapshotRepository;
    private final KpiComponentMapper kpiMapper;
    private final CurrentUser currentUser;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final EvaluationRepository evaluationRepository;
    private final GroupMembershipRepository groupMembershipRepository;

    /**
     * Resolve which group a KPI lookup should be scoped to (Scope.md §13):
     * an explicitly requested groupId wins; otherwise fall back to the
     * user's primary group; a user with no group membership at all (a
     * Manager, or a Lead untracked as a Member) keeps the pre-§13
     * groupId=null "all tasks" behavior.
     */
    private Long resolveGroupId(Long userId, Long requestedGroupId) {
        if (requestedGroupId != null) {
            return requestedGroupId;
        }
        return groupMembershipRepository.findPrimaryForMember(userId)
                .map(gm -> gm.getGroup().getId())
                .orElse(null);
    }

    @GetMapping
    @Operation(summary = "Get current user KPI", description = "Retrieve real-time KPI for authenticated user")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<KpiResponse>> getCurrentUserKpi(
            @RequestParam(required = false) Long groupId) {
        Long userId = currentUser.getCurrentUser().getId();
        YearMonth period = YearMonth.now();

        KpiComponent kpi = kpiCacheService.getOrCalculateKpi(userId, resolveGroupId(userId, groupId), period);
        KpiResponse response = kpiMapper.toDTO(kpi);

        return ok(ApiResponse.success(response));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user KPI", description = "Retrieve KPI for specific user (Lead/Manager only)")
    @PreAuthorize("hasAnyRole('LEAD', 'MANAGER')")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<KpiResponse>> getUserKpi(
            @PathVariable Long userId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Long groupId) {

        YearMonth periodMonth = period != null ? YearMonth.parse(period) : YearMonth.now();
        KpiComponent kpi = kpiCacheService.getOrCalculateKpi(userId, resolveGroupId(userId, groupId), periodMonth);
        KpiResponse response = kpiMapper.toDTO(kpi);

        return ok(ApiResponse.success(response));
    }

    @GetMapping("/{userId}/history")
    @Operation(summary = "Get KPI history", description = "Retrieve last 12 months of KPI for trend analysis")
    @PreAuthorize("hasAnyRole('LEAD', 'MANAGER')")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<KpiResponse>>> getKpiHistory(
            @PathVariable Long userId) {
        
        List<KpiComponent> history = kpiComponentRepository.findRecentByUser(userId);
        List<KpiResponse> responses = history.stream()
                .map(kpiMapper::toDTO)
                .collect(Collectors.toList());
        
        return ok(ApiResponse.success(responses));
    }

    @GetMapping("/team/summary")
    @Operation(summary = "Get team KPI summary", description = "Retrieve current month KPI for all team members (Lead: own team; Manager: whole company)")
    @PreAuthorize("hasAnyRole('LEAD', 'MANAGER')")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Page<KpiResponse>>> getTeamKpiSummary(Pageable pageable) {

        User user = currentUser.getCurrentUser();
        YearMonth currentPeriod = YearMonth.now();

        // A Lead is scoped to their own team (lead_id = them); a Manager isn't
        // "lead" of anyone, so the lead-scoped query would wrongly return
        // nothing for them — they see every locked snapshot for the period.
        List<KpiSnapshot> teamSnapshots = user.isManager()
                ? kpiSnapshotRepository.findLockedSnapshotsByPeriod(currentPeriod)
                : kpiSnapshotRepository.findTeamSnapshotsByLead(user.getId(), currentPeriod);

        // Manual pagination (in real implementation, use custom query)
        List<KpiResponse> responses = teamSnapshots.stream()
                .skip((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .map(snapshot -> {
                    KpiResponse resp = new KpiResponse();
                    resp.setUserId(snapshot.getUser().getId());
                    resp.setUserFullName(snapshot.getUser().getFullName());
                    resp.setKpiFinal(snapshot.getKpiFinal());
                    resp.setRanking(snapshot.getRanking());
                    return resp;
                })
                .collect(Collectors.toList());
        
        Page<KpiResponse> page = new PageImpl<>(responses, pageable, teamSnapshots.size());
        return ok(ApiResponse.success(page));
    }

    /**
     * Members & KPI list (Scope.md §5.6.3). Lead sees their own team;
     * Manager sees every active Member. Scope.md §13: a member belonging
     * to more than one Group appears once per group, each row scoped to
     * that group's own tasks/KPI — a genuine split, not a merged total.
     * A member with no group membership at all gets a single groupId=null
     * "all tasks" row, matching the pre-§13 behavior.
     */
    @GetMapping("/members")
    @Operation(summary = "List members with KPI summary", description = "Lead: own team; Manager: whole company")
    @PreAuthorize("hasAnyRole('LEAD', 'MANAGER')")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<MemberKpiSummaryResponse>>> getMembersKpiSummary(
            @RequestParam(required = false) String periodMonth) {

        User user = currentUser.getCurrentUser();
        YearMonth period = periodMonth != null ? YearMonth.parse(periodMonth) : YearMonth.now();

        List<User> members = user.isManager()
                ? userRepository.findByRoleAndActive(Role.MEMBER)
                : userRepository.findByLeadId(user.getId());

        List<MemberKpiSummaryResponse> summaries = new ArrayList<>();
        for (User member : members) {
            List<GroupMembership> memberships = groupMembershipRepository.findByMemberId(member.getId());
            List<Group> groups = memberships.isEmpty()
                    ? java.util.Collections.singletonList((Group) null)
                    : memberships.stream().map(GroupMembership::getGroup).toList();

            for (Group group : groups) {
                Long groupId = group != null ? group.getId() : null;
                KpiComponent kpi = kpiCacheService.getOrCalculateKpi(member.getId(), groupId, period);
                List<Task> tasks = taskRepository.findTasksForKpiCalculation(
                        member.getId(), period.getYear(), period.getMonthValue(), groupId);

                int fastDone = 0, fastTotal = 0, msDone = 0, msTotal = 0, overdue = 0;
                for (Task t : tasks) {
                    boolean done = t.getStatus() == TaskStatus.DONE || t.getStatus() == TaskStatus.CLOSED_LATE;
                    if (t.getTaskType().getTimeCategory() == TimeCategory.FAST) {
                        fastTotal++;
                        if (done) fastDone++;
                    } else {
                        msTotal++;
                        if (done) msDone++;
                    }
                    if (!done && t.getDueDate() != null && t.getDueDate().isBefore(java.time.LocalDate.now())) {
                        overdue++;
                    }
                }

                BigDecimal displayKpi = kpi.getKpiFinal() != null ? kpi.getKpiFinal() : kpi.getAutoScore();

                summaries.add(MemberKpiSummaryResponse.builder()
                        .userId(member.getId())
                        .fullName(member.getFullName())
                        .role(member.getRole().name())
                        .groupId(groupId)
                        .groupName(group != null ? group.getName() : null)
                        .kpi(displayKpi)
                        .kpiStatus(kpiStatus(displayKpi))
                        .fastDone(fastDone).fastTotal(fastTotal)
                        .multiStepDone(msDone).multiStepTotal(msTotal)
                        .totalDone(fastDone + msDone)
                        .overdueCount(overdue)
                        .build());
            }
        }

        return ok(ApiResponse.success(summaries));
    }

    /**
     * Full KPI breakdown popup for one member (Scope.md §5.6.4).
     */
    @GetMapping("/members/{userId}")
    @Operation(summary = "Get member KPI breakdown", description = "Full KPI detail popup for one member/period")
    @PreAuthorize("hasAnyRole('LEAD', 'MANAGER')")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<MemberKpiDetailResponse>> getMemberKpiDetail(
            @PathVariable Long userId,
            @RequestParam(required = false) String periodMonth,
            @RequestParam(required = false) Long groupId) {

        YearMonth period = periodMonth != null ? YearMonth.parse(periodMonth) : YearMonth.now();
        User member = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Long effectiveGroupId = resolveGroupId(userId, groupId);
        KpiComponent kpi = kpiCacheService.getOrCalculateKpi(userId, effectiveGroupId, period);
        List<Task> tasks = taskRepository.findTasksForKpiCalculation(userId, period.getYear(), period.getMonthValue(), effectiveGroupId);
        Optional<Evaluation> evaluation = evaluationRepository.findByUserAndGroupAndPeriod(userId, effectiveGroupId, period);

        int fastDone = 0, fastTotal = 0, msDone = 0, msTotal = 0;
        List<MemberKpiDetailResponse.TaskLine> taskLines = new ArrayList<>();
        for (Task t : tasks) {
            boolean done = t.getStatus() == TaskStatus.DONE || t.getStatus() == TaskStatus.CLOSED_LATE;
            if (t.getTaskType().getTimeCategory() == TimeCategory.FAST) {
                fastTotal++;
                if (done) fastDone++;
            } else {
                msTotal++;
                if (done) msDone++;
            }
            taskLines.add(MemberKpiDetailResponse.TaskLine.builder()
                    .taskId(t.getId())
                    .title(t.getTitle())
                    .timeCategory(t.getTaskType().getTimeCategory().name())
                    .status(t.getStatus().name())
                    .dueDate(t.getDueDate())
                    .completedAt(t.getCompletedAt())
                    .build());
        }

        BigDecimal displayKpi = kpi.getKpiFinal() != null ? kpi.getKpiFinal() : kpi.getAutoScore();
        BigDecimal change = (kpi.getKpiFinal() != null && kpi.getPreviousMonthKpi() != null)
                ? kpi.getKpiFinal().subtract(kpi.getPreviousMonthKpi())
                : null;

        MemberKpiDetailResponse detail = MemberKpiDetailResponse.builder()
                .userId(member.getId())
                .fullName(member.getFullName())
                .role(member.getRole().name())
                .periodMonth(period.toString())
                .groupId(effectiveGroupId)
                .groupName(kpi.getGroup() != null ? kpi.getGroup().getName() : null)
                .kpiFinal(displayKpi)
                .kpiStatus(kpiStatus(displayKpi))
                .previousMonthKpi(kpi.getPreviousMonthKpi())
                .changeVsPreviousMonth(change)
                .autoScore(kpi.getAutoScore())
                .leadScore(evaluation.map(Evaluation::getLeadScore).orElse(null))
                .managerScore(evaluation.map(Evaluation::getManagerScore).orElse(null))
                .leadHasScored(evaluation.map(e -> e.getLeadSubmittedAt() != null).orElse(false))
                .managerHasFinalized(evaluation.map(e -> e.getManagerFinalizedAt() != null).orElse(false))
                .fastTask(toBreakdown(fastDone, fastTotal))
                .multiStep(toBreakdown(msDone, msTotal))
                .tasks(taskLines)
                .build();

        return ok(ApiResponse.success(detail));
    }

    private MemberKpiDetailResponse.TypeBreakdown toBreakdown(int done, int total) {
        BigDecimal percent = total > 0
                ? BigDecimal.valueOf(done).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return MemberKpiDetailResponse.TypeBreakdown.builder()
                .done(done).total(total).completionPercent(percent)
                .build();
    }

    private String kpiStatus(BigDecimal kpi) {
        if (kpi == null) return "NGUY_HIEM";
        if (kpi.compareTo(BigDecimal.valueOf(80)) >= 0) return "DAT";
        if (kpi.compareTo(BigDecimal.valueOf(60)) >= 0) return "CANH_BAO";
        return "NGUY_HIEM";
    }

    @GetMapping("/admin/config")
    @Operation(summary = "Get KPI configuration", description = "Retrieve current KPI formula weights (Admin only)")
    @PreAuthorize("hasRole('MANAGER')")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKpiConfiguration() {
        
        KpiConfiguration config = kpiConfigurationRepository.findLatest()
                .orElseGet(KpiConfiguration::new);
        
        Map<String, Object> configMap = Map.of(
            "wcrWeight", config.getWcrWeight(),
            "viWeight", config.getViWeight(),
            "eaWeight", config.getEaWeight(),
            "autoScoreWeight", config.getAutoScoreWeight(),
            "leadScoreWeight", config.getLeadScoreWeight(),
            "managerScoreWeight", config.getManagerScoreWeight(),
            "workingHoursPerDay", config.getWorkingHoursPerDay()
        );
        
        return ok(ApiResponse.success(configMap));
    }

    @PutMapping("/admin/config")
    @Operation(summary = "Update KPI configuration", description = "Update formula weights (Admin only)")
    @PreAuthorize("hasRole('MANAGER')")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateKpiConfiguration(
            @RequestBody KpiConfigurationRequest config) {

        KpiConfiguration current = kpiConfigurationRepository.findLatest()
                .orElseGet(() -> {
                    KpiConfiguration c = new KpiConfiguration();
                    c.setPeriodMonth(YearMonth.now());
                    return c;
                });

        if (config.getWcrWeight() != null) current.setWcrWeight(config.getWcrWeight());
        if (config.getViWeight() != null) current.setViWeight(config.getViWeight());
        if (config.getEaWeight() != null) current.setEaWeight(config.getEaWeight());
        if (config.getAutoScoreWeight() != null) current.setAutoScoreWeight(config.getAutoScoreWeight());
        if (config.getLeadScoreWeight() != null) current.setLeadScoreWeight(config.getLeadScoreWeight());
        if (config.getManagerScoreWeight() != null) current.setManagerScoreWeight(config.getManagerScoreWeight());
        if (config.getWorkingHoursPerDay() != null) current.setWorkingHoursPerDay(config.getWorkingHoursPerDay());

        String rangeError = validateKpiConfigRanges(current);
        if (rangeError != null) {
            return response(ApiResponse.error(rangeError), org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if (!current.validateWeights()) {
            return response(ApiResponse.error("wcrWeight + viWeight + eaWeight must sum to 100%, and autoScoreWeight + leadScoreWeight + managerScoreWeight must sum to 100%"),
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        kpiConfigurationRepository.save(current);
        kpiCacheService.invalidateAllKpi();

        Map<String, Object> result = Map.of(
            "message", "Configuration updated successfully",
            "config", current
        );

        return ok(ApiResponse.success(result));
    }

    /**
     * Range bounds from Scope.md §7.6 — the sum-to-100% check alone would
     * still accept, say, W1=99/W2=0.5/W3=0.5, which the spec's per-field
     * min/max table explicitly rules out.
     */
    private String validateKpiConfigRanges(KpiConfiguration c) {
        if (outOfRange(c.getAutoScoreWeight(), 20, 70)) return "autoScoreWeight must be between 20% and 70%";
        if (outOfRange(c.getLeadScoreWeight(), 10, 60)) return "leadScoreWeight must be between 10% and 60%";
        if (outOfRange(c.getManagerScoreWeight(), 10, 50)) return "managerScoreWeight must be between 10% and 50%";
        if (outOfRange(c.getWcrWeight(), 40, 80)) return "wcrWeight must be between 40% and 80%";
        if (outOfRange(c.getViWeight(), 10, 40)) return "viWeight must be between 10% and 40%";
        if (outOfRange(c.getEaWeight(), 0, 30)) return "eaWeight must be between 0% and 30%";
        if (c.getWorkingHoursPerDay() == null || c.getWorkingHoursPerDay() < 1 || c.getWorkingHoursPerDay() > 24) {
            return "workingHoursPerDay must be between 1 and 24";
        }
        return null;
    }

    private boolean outOfRange(Double value, double min, double max) {
        return value == null || value < min || value > max;
    }
}
