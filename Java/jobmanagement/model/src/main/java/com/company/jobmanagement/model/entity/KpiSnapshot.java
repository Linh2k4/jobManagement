package com.company.jobmanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZonedDateTime;

/**
 * Immutable monthly KPI snapshot (locked after month-end).
 * Created by nightly batch job, used for historical analysis and evaluation.
 */
@Entity
@Table(name = "kpi_snapshots", indexes = {
    @Index(name = "idx_kpi_snap_user_group_period", columnList = "user_id,group_id,period_month", unique = true),
    @Index(name = "idx_kpi_snap_lead_period", columnList = "lead_id,period_month"),
    @Index(name = "idx_kpi_snap_period", columnList = "period_month")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private User lead;  // team lead for this user (if applicable)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(nullable = false)
    private YearMonth periodMonth;

    // Auto-calculated components
    @Column(precision = 10, scale = 2)
    private BigDecimal wcr;

    @Column(precision = 10, scale = 2)
    private BigDecimal vi;

    @Column(precision = 10, scale = 2)
    private BigDecimal ea;

    @Column(precision = 10, scale = 2)
    private BigDecimal autoScore;

    // Manual evaluation scores
    @Column(precision = 10, scale = 2)
    private BigDecimal leadScore;

    @Column(precision = 10, scale = 2)
    private BigDecimal managerScore;

    // Final locked score
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal kpiFinal;

    @Column(length = 50)
    private String ranking;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Metadata
    @Column
    private Long tasksCompleted;

    @Column
    private Long tasksOverdue;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalEstimateHours;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalActualHours;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @Version
    private Long version;

    public void lock() {
        this.isLocked = true;
        this.updatedAt = ZonedDateTime.now();
    }
}
