package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.*;
import com.company.jobmanagement.model.enums.Role;
import com.company.jobmanagement.exception.BusinessLogicException;
import com.company.jobmanagement.exception.ForbiddenOperationException;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.repository.*;
import com.company.jobmanagement.repository.GroupMembershipRepository;
import com.company.jobmanagement.repository.GroupRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationPeriodRepository evaluationPeriodRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final CurrentUser currentUser;
    private final KpiCacheService kpiCacheService;

    /**
     * Get or create the evaluation for a user in one Group/period (Scope.md
     * §13 — a member in N groups gets N independent evaluation rows).
     * groupId=null keeps the pre-§13 single-evaluation behavior for a user
     * with no group membership (e.g. a Manager).
     */
    public Evaluation getOrCreateEvaluation(Long userId, Long groupId, YearMonth periodMonth) {
        return evaluationRepository.findByUserAndGroupAndPeriod(userId, groupId, periodMonth)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Group group = groupId != null
                            ? groupRepository.findById(groupId).orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId))
                            : null;

                    Evaluation eval = Evaluation.builder()
                            .user(user)
                            .group(group)
                            .periodMonth(periodMonth)
                            .manager(getManagerForUser(user))
                            .lead(getLeadForUser(user, groupId))
                            .status(Evaluation.EvaluationStatus.DRAFT)
                            .isLocked(false)
                            .build();

                    return evaluationRepository.save(eval);
                });
    }

    @Transactional
    public void submitSelfEvaluation(Long evaluationId, BigDecimal quality, BigDecimal responsibility,
                                     BigDecimal teamwork, BigDecimal initiative, BigDecimal discipline,
                                     String notes) {
        Evaluation eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found"));

        // Validate: evaluation not locked
        if (Boolean.TRUE.equals(eval.getIsLocked())) {
            throw new BusinessLogicException("Evaluation is locked and cannot be modified");
        }

        User currentUser = this.currentUser.getCurrentUser();
        if (!eval.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenOperationException("Cannot submit evaluation for another user");
        }

        // Validate: only DRAFT can submit self-evaluation
        if (eval.getStatus() != Evaluation.EvaluationStatus.DRAFT &&
            eval.getStatus() != Evaluation.EvaluationStatus.SELF_SUBMITTED) {
            throw new BusinessLogicException(
                String.format("Cannot submit self-evaluation when status is %s", eval.getStatus().getDisplayName())
            );
        }

        // Validate scores are in range [1-10]
        validateScore(quality, "Quality");
        validateScore(responsibility, "Responsibility");
        validateScore(teamwork, "Teamwork");
        validateScore(initiative, "Initiative");
        validateScore(discipline, "Discipline");

        eval.setSelfQuality(quality);
        eval.setSelfResponsibility(responsibility);
        eval.setSelfTeamwork(teamwork);
        eval.setSelfInitiative(initiative);
        eval.setSelfDiscipline(discipline);
        eval.setSelfNotes(notes);
        eval.setStatus(Evaluation.EvaluationStatus.SELF_SUBMITTED);
        eval.setSelfSubmittedAt(ZonedDateTime.now());
        eval.setUpdatedAt(ZonedDateTime.now());

        evaluationRepository.save(eval);
        log.info("Self-evaluation submitted: evaluationId={}, userId={}", evaluationId, eval.getUser().getId());
    }

    @Transactional
    public void submitLeadEvaluation(Long evaluationId, BigDecimal quality, BigDecimal responsibility,
                                     BigDecimal teamwork, BigDecimal discipline, String notes) {
        Evaluation eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found"));

        // Validate: evaluation not locked
        if (Boolean.TRUE.equals(eval.getIsLocked())) {
            throw new BusinessLogicException("Evaluation is locked and cannot be modified");
        }

        User currentUser = this.currentUser.getCurrentUser();
        if (!currentUser.isLead() || (eval.getLead() != null && !eval.getLead().getId().equals(currentUser.getId()))) {
            throw new ForbiddenOperationException("Only the Lead can submit lead evaluation");
        }

        // Validate: member must have submitted self-evaluation first
        if (eval.getStatus() != Evaluation.EvaluationStatus.SELF_SUBMITTED) {
            throw new BusinessLogicException(
                "Member must submit self-evaluation before Lead can evaluate. Current status: " + eval.getStatus().getDisplayName()
            );
        }

        // Validate scores are in range [1-10]
        validateScore(quality, "Quality");
        validateScore(responsibility, "Responsibility");
        validateScore(teamwork, "Teamwork");
        validateScore(discipline, "Discipline");

        eval.setLeadQuality(quality);
        eval.setLeadResponsibility(responsibility);
        eval.setLeadTeamwork(teamwork);
        eval.setLeadDiscipline(discipline);
        eval.setLeadNotes(notes);
        eval.setLeadScore(eval.calculateLeadScore());
        eval.setStatus(Evaluation.EvaluationStatus.LEAD_REVIEWED);
        eval.setLeadSubmittedAt(ZonedDateTime.now());
        eval.setUpdatedAt(ZonedDateTime.now());

        evaluationRepository.save(eval);
        log.info("Lead evaluation submitted: evaluationId={}, leadId={}, userId={}",
                 evaluationId, currentUser.getId(), eval.getUser().getId());
    }

    @Transactional
    public void finalizeEvaluation(Long evaluationId, BigDecimal quality, BigDecimal responsibility,
                                   BigDecimal initiative, BigDecimal discipline, String notes) {
        Evaluation eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found"));

        // Validate: evaluation not already locked
        if (Boolean.TRUE.equals(eval.getIsLocked())) {
            throw new BusinessLogicException("Evaluation is already finalized and locked");
        }

        User currentUser = this.currentUser.getCurrentUser();
        if (!currentUser.isManager()) {
            throw new ForbiddenOperationException("Only Manager can finalize evaluation");
        }

        // Validate: Lead must have reviewed first
        if (eval.getStatus() != Evaluation.EvaluationStatus.LEAD_REVIEWED) {
            throw new BusinessLogicException(
                "Lead must review evaluation before Manager can finalize. Current status: " + eval.getStatus().getDisplayName()
            );
        }

        // Validate scores are in range [1-10]
        validateScore(quality, "Quality");
        validateScore(responsibility, "Responsibility");
        validateScore(initiative, "Initiative");
        validateScore(discipline, "Discipline");

        eval.setManagerQuality(quality);
        eval.setManagerResponsibility(responsibility);
        eval.setManagerInitiative(initiative);
        eval.setManagerDiscipline(discipline);
        eval.setManagerNotes(notes);
        eval.setManagerScore(eval.calculateManagerScore());

        // Calculate final KPI (Auto×40% + Lead×35% + Manager×25%).
        // Auto comes from the real-time KpiComponent (WCR/VI/EA), scoped to
        // this evaluation's own Group — was hardcoded to zero before §13.
        Long groupId = eval.getGroup() != null ? eval.getGroup().getId() : null;
        BigDecimal autoScore = kpiCacheService.getOrCalculateKpi(eval.getUser().getId(), groupId, eval.getPeriodMonth()).getAutoScore();
        BigDecimal leadScore = eval.getLeadScore() != null ? eval.getLeadScore() : BigDecimal.ZERO;
        BigDecimal managerScore = eval.getManagerScore() != null ? eval.getManagerScore() : BigDecimal.ZERO;
        eval.setKpiFinal(calculateKpiFinal(autoScore, leadScore, managerScore));

        eval.lock();  // Set FINALIZED status and isLocked=true

        evaluationRepository.save(eval);

        // Invalidate KPI cache after evaluation is finalized
        kpiCacheService.invalidateKpi(eval.getUser().getId(), groupId, eval.getPeriodMonth());

        log.info("Evaluation finalized: evaluationId={}, managerId={}, userId={}",
                 evaluationId, currentUser.getId(), eval.getUser().getId());
    }

    @Transactional(readOnly = true)
    public Evaluation getEvaluation(Long evaluationId) {
        return evaluationRepository.findByIdWithAssociations(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found"));
    }

    @Transactional(readOnly = true)
    public Page<Evaluation> getTeamEvaluations(Long leadId, YearMonth periodMonth, Pageable pageable) {
        return evaluationRepository.findTeamEvaluations(leadId, periodMonth, pageable);
    }

    private User getManagerForUser(User user) {
        // For now, get the first manager in the system (typically there's 1)
        // In a real system, this would traverse the org hierarchy
        return userRepository.findFirstByRoleOrderById(Role.MANAGER).orElse(null);
    }

    /**
     * Resolve the Lead for a Member in one Group (Scope.md §13).
     * Was hardcoded to null before §13; now looks up the Member's Group
     * membership and returns that Group's Lead. groupId=null (no group
     * membership) still returns null, matching the pre-§13 behavior.
     */
    private User getLeadForUser(User user, Long groupId) {
        if (groupId == null) {
            return null;
        }
        return groupMembershipRepository.findByMemberId(user.getId()).stream()
                .filter(gm -> gm.getGroup().getId().equals(groupId))
                .findFirst()
                .map(gm -> gm.getGroup().getLead())
                .orElse(null);
    }

    private void validateScore(BigDecimal score, String fieldName) {
        if (score != null) {
            if (score.compareTo(BigDecimal.ONE) < 0 || score.compareTo(BigDecimal.TEN) > 0) {
                throw new BusinessLogicException(
                    fieldName + " must be between 1 and 10, got: " + score
                );
            }
        }
    }

    private BigDecimal calculateKpiFinal(BigDecimal autoScore, BigDecimal leadScore, BigDecimal managerScore) {
        BigDecimal auto = autoScore != null ? autoScore : BigDecimal.ZERO;
        BigDecimal lead = leadScore != null ? leadScore : BigDecimal.ZERO;
        BigDecimal mgr = managerScore != null ? managerScore : BigDecimal.ZERO;

        return auto.multiply(BigDecimal.valueOf(0.4))
                .add(lead.multiply(BigDecimal.valueOf(0.35)))
                .add(mgr.multiply(BigDecimal.valueOf(0.25)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get evaluations for the current user (fixed from §13 backlog).
     * Was buggy: called findTeamEvaluations(userId, ...) which filters
     * WHERE lead.id = userId, so a Member calling this would always get
     * an empty page (they're not a Lead). Now calls the correct user-owned
     * query: a user sees the evaluations *for them*, which can now be one
     * per group they belong to.
     */
    public Page<Evaluation> getMyEvaluations(YearMonth periodMonth, Pageable pageable) {
        User user = currentUser.getCurrentUser();
        if (user == null) {
            return Page.empty(pageable);
        }
        return evaluationRepository.findByUserAndPeriodPaged(user.getId(), periodMonth, pageable);
    }

    /**
     * Get all evaluations for a period (manager-level access).
     */
    public Page<Evaluation> getAllEvaluations(YearMonth periodMonth, Pageable pageable) {
        List<Evaluation> evals = evaluationRepository.findByPeriodOrderedByKpi(periodMonth);
        return new org.springframework.data.domain.PageImpl<>(evals, pageable,
                evals.size());
    }
}
