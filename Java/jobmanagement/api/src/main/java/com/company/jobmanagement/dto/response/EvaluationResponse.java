package com.company.jobmanagement.dto.response;

import com.company.jobmanagement.model.entity.Evaluation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Flat evaluation payload (member/lead/manager workflow, Scope.md §3.1-3.6).
 * The EvaluationController used to return the {@link Evaluation} entity
 * directly — its user/lead/manager/group associations are FetchType.LAZY,
 * so that entity is not safe to serialize (LazyInitializationException, and
 * once "fixed" by eager-fetching, User carries the bcrypt password hash).
 * This DTO is the boundary that keeps entities out of the response body.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "One evaluation row (member self-review + lead review + manager finalization)")
public class EvaluationResponse {

    private Long id;

    private Long userId;
    private String userFullName;

    private Long leadId;
    private String leadFullName;

    private Long managerId;
    private String managerFullName;

    private Long groupId;
    private String groupName;

    private String periodMonth;

    private BigDecimal selfQuality;
    private BigDecimal selfResponsibility;
    private BigDecimal selfTeamwork;
    private BigDecimal selfInitiative;
    private BigDecimal selfDiscipline;
    private String selfNotes;

    private BigDecimal leadQuality;
    private BigDecimal leadResponsibility;
    private BigDecimal leadTeamwork;
    private BigDecimal leadDiscipline;
    private String leadNotes;

    private BigDecimal managerQuality;
    private BigDecimal managerResponsibility;
    private BigDecimal managerInitiative;
    private BigDecimal managerDiscipline;
    private String managerNotes;

    private BigDecimal leadScore;
    private BigDecimal managerScore;
    private BigDecimal kpiFinal;

    private String status;
    private Boolean isLocked;

    private ZonedDateTime selfSubmittedAt;
    private ZonedDateTime leadSubmittedAt;
    private ZonedDateTime managerFinalizedAt;

    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public static EvaluationResponse from(Evaluation e) {
        return EvaluationResponse.builder()
                .id(e.getId())
                .userId(e.getUser().getId())
                .userFullName(e.getUser().getFullName())
                .leadId(e.getLead() != null ? e.getLead().getId() : null)
                .leadFullName(e.getLead() != null ? e.getLead().getFullName() : null)
                .managerId(e.getManager().getId())
                .managerFullName(e.getManager().getFullName())
                .groupId(e.getGroup() != null ? e.getGroup().getId() : null)
                .groupName(e.getGroup() != null ? e.getGroup().getName() : null)
                .periodMonth(e.getPeriodMonth().toString())
                .selfQuality(e.getSelfQuality())
                .selfResponsibility(e.getSelfResponsibility())
                .selfTeamwork(e.getSelfTeamwork())
                .selfInitiative(e.getSelfInitiative())
                .selfDiscipline(e.getSelfDiscipline())
                .selfNotes(e.getSelfNotes())
                .leadQuality(e.getLeadQuality())
                .leadResponsibility(e.getLeadResponsibility())
                .leadTeamwork(e.getLeadTeamwork())
                .leadDiscipline(e.getLeadDiscipline())
                .leadNotes(e.getLeadNotes())
                .managerQuality(e.getManagerQuality())
                .managerResponsibility(e.getManagerResponsibility())
                .managerInitiative(e.getManagerInitiative())
                .managerDiscipline(e.getManagerDiscipline())
                .managerNotes(e.getManagerNotes())
                .leadScore(e.getLeadScore())
                .managerScore(e.getManagerScore())
                .kpiFinal(e.getKpiFinal())
                .status(e.getStatus().name())
                .isLocked(e.getIsLocked())
                .selfSubmittedAt(e.getSelfSubmittedAt())
                .leadSubmittedAt(e.getLeadSubmittedAt())
                .managerFinalizedAt(e.getManagerFinalizedAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
