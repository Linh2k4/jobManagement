package com.company.jobmanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZonedDateTime;

/**
 * Real-time KPI component calculation results.
 * Calculated on-demand and cached (5-min TTL).
 * Components: WCR, VI, EA, Final Score
 */
@Entity
@Table(name = "kpi_components", indexes = {
    @Index(name = "idx_kpi_comp_user_group_period", columnList = "user_id,group_id,period_month", unique = true),
    @Index(name = "idx_kpi_comp_period", columnList = "period_month"),
    @Index(name = "idx_kpi_comp_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Scope.md §13: WCR/VI/EA are computed from only this group's tasks —
    // one component row per (user, group, period).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(nullable = false)
    private YearMonth periodMonth;

    @Column(precision = 10, scale = 2)
    private BigDecimal wcr;  // Weighted Completion Rate (0-100)

    @Column(precision = 10, scale = 2)
    private BigDecimal vi;  // Volume Index (0-120, then normalized to 0-100)

    @Column(precision = 10, scale = 2)
    private BigDecimal ea;  // Estimate Accuracy (0-100)

    @Column(precision = 10, scale = 2)
    private BigDecimal autoScore;  // Combined auto score (WCR×60% + VI×25% + EA×15%)

    @Column(precision = 10, scale = 2)
    private BigDecimal leadScore;  // Lead evaluation (0-100)

    @Column(precision = 10, scale = 2)
    private BigDecimal managerScore;  // Manager evaluation (0-100)

    @Column(precision = 10, scale = 2)
    private BigDecimal kpiFinal;  // Final KPI (AutoScore×40% + LeadScore×35% + ManagerScore×25%)

    @Column(length = 50)
    private String ranking;  // Xuất sắc / Tốt / Đạt / Cần cải thiện / Không đạt

    @Column(precision = 10, scale = 2)
    private BigDecimal previousMonthKpi;  // For trend calculation

    @Column(length = 50)
    private String trend;  // UP / DOWN / STABLE

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    public String calculateRanking() {
        if (kpiFinal == null) return null;
        double score = kpiFinal.doubleValue();
        if (score >= 90) return "Xuất sắc";
        if (score >= 75) return "Tốt";
        if (score >= 60) return "Đạt";
        if (score >= 45) return "Cần cải thiện";
        return "Không đạt";
    }

    public String calculateTrend() {
        if (previousMonthKpi == null || kpiFinal == null) return "STABLE";
        double current = kpiFinal.doubleValue();
        double previous = previousMonthKpi.doubleValue();
        double diff = current - previous;
        if (diff > 2) return "UP";
        if (diff < -2) return "DOWN";
        return "STABLE";
    }
}
