package com.company.jobmanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZonedDateTime;

@Entity
@Table(name = "evaluations", indexes = {
    @Index(name = "idx_eval_user_group_period", columnList = "user_id,group_id,period_month", unique = true),
    @Index(name = "idx_eval_lead_period", columnList = "lead_id,period_month"),
    @Index(name = "idx_eval_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private User lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    // Scope.md §13: one independent Evaluation per (user, group, period) —
    // a member in 2 groups gets 2 separate rows, scored by 2 different Leads.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(nullable = false)
    private YearMonth periodMonth;

    @Column(precision = 3, scale = 1)
    private BigDecimal selfQuality;

    @Column(precision = 3, scale = 1)
    private BigDecimal selfResponsibility;

    @Column(precision = 3, scale = 1)
    private BigDecimal selfTeamwork;

    @Column(precision = 3, scale = 1)
    private BigDecimal selfInitiative;

    @Column(precision = 3, scale = 1)
    private BigDecimal selfDiscipline;

    @Column(columnDefinition = "TEXT")
    private String selfNotes;

    @Column(precision = 3, scale = 1)
    private BigDecimal leadQuality;

    @Column(precision = 3, scale = 1)
    private BigDecimal leadResponsibility;

    @Column(precision = 3, scale = 1)
    private BigDecimal leadTeamwork;

    @Column(precision = 3, scale = 1)
    private BigDecimal leadDiscipline;

    @Column(columnDefinition = "TEXT")
    private String leadNotes;

    @Column(precision = 3, scale = 1)
    private BigDecimal managerQuality;

    @Column(precision = 3, scale = 1)
    private BigDecimal managerResponsibility;

    @Column(precision = 3, scale = 1)
    private BigDecimal managerInitiative;

    @Column(precision = 3, scale = 1)
    private BigDecimal managerDiscipline;

    @Column(columnDefinition = "TEXT")
    private String managerNotes;

    @Column(precision = 10, scale = 2)
    private BigDecimal leadScore;

    @Column(precision = 10, scale = 2)
    private BigDecimal managerScore;

    @Column(precision = 10, scale = 2)
    private BigDecimal kpiFinal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EvaluationStatus status = EvaluationStatus.DRAFT;

    @Column
    private Boolean isLocked = false;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private ZonedDateTime selfSubmittedAt;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private ZonedDateTime leadSubmittedAt;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private ZonedDateTime managerFinalizedAt;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @Version
    private Long version;

    public BigDecimal calculateLeadScore() {
        if (leadQuality == null || leadResponsibility == null ||
            leadTeamwork == null || leadDiscipline == null) {
            return null;
        }
        BigDecimal sum = leadQuality.add(leadResponsibility)
                .add(leadTeamwork)
                .add(leadDiscipline);
        return sum.divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.TEN);
    }

    public BigDecimal calculateManagerScore() {
        if (managerQuality == null || managerResponsibility == null ||
            managerInitiative == null || managerDiscipline == null) {
            return null;
        }
        BigDecimal sum = managerQuality.add(managerResponsibility)
                .add(managerInitiative)
                .add(managerDiscipline);
        return sum.divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.TEN);
    }

    public void lock() {
        this.isLocked = true;
        this.status = EvaluationStatus.FINALIZED;
        this.managerFinalizedAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    public enum EvaluationStatus {
        DRAFT("Dự thảo"),
        SELF_SUBMITTED("Tự đánh giá"),
        LEAD_REVIEWED("Lead đánh giá"),
        FINALIZED("Hoàn thành");

        private final String displayName;

        EvaluationStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
