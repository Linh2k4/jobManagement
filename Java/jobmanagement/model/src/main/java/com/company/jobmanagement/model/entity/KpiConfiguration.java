package com.company.jobmanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.YearMonth;
import java.time.ZonedDateTime;

/**
 * System-wide KPI formula configuration.
 * Stores weights and parameters for KPI calculation.
 * One record per period to maintain historical accuracy.
 */
@Entity
@Table(name = "kpi_configurations", indexes = {
    @Index(name = "idx_kpi_config_period", columnList = "period_month")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private YearMonth periodMonth;

    @Column(nullable = false, columnDefinition = "NUMERIC(5,2)")
    @Builder.Default
    private Double wcrWeight = 60.0;  // Weighted Completion Rate: 60%

    @Column(nullable = false, columnDefinition = "NUMERIC(5,2)")
    @Builder.Default
    private Double viWeight = 25.0;  // Volume Index: 25%

    @Column(nullable = false, columnDefinition = "NUMERIC(5,2)")
    @Builder.Default
    private Double eaWeight = 15.0;  // Estimate Accuracy: 15%

    @Column(nullable = false, columnDefinition = "NUMERIC(5,2)")
    @Builder.Default
    private Double autoScoreWeight = 40.0;  // Automatic score: 40%

    @Column(nullable = false, columnDefinition = "NUMERIC(5,2)")
    @Builder.Default
    private Double leadScoreWeight = 35.0;  // Lead evaluation: 35%

    @Column(nullable = false, columnDefinition = "NUMERIC(5,2)")
    @Builder.Default
    private Double managerScoreWeight = 25.0;  // Manager evaluation: 25%

    @Column(nullable = false)
    @Builder.Default
    private Integer workingHoursPerDay = 8;

    @Column
    private String notes;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @Version
    private Long version;

    public boolean validateWeights() {
        double indices = wcrWeight + viWeight + eaWeight;
        double scores = autoScoreWeight + leadScoreWeight + managerScoreWeight;
        return Math.abs(indices - 100.0) < 0.01 && Math.abs(scores - 100.0) < 0.01;
    }

    public int getWorkingDaysInPeriod() {
        // Assumes 5 working days per week, 4 weeks per month = 20 days
        return 20;
    }

    public int getTotalWorkingHoursInPeriod() {
        return getWorkingDaysInPeriod() * workingHoursPerDay;
    }
}
