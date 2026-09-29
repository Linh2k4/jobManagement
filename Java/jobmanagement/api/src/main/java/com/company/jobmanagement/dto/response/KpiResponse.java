package com.company.jobmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "KpiResponse", description = "Real-time KPI calculation results")
public class KpiResponse {

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "User full name", example = "Nguyen Van A")
    private String userFullName;

    @Schema(description = "Period (YYYY-MM)", example = "2026-06")
    private String periodMonth;

    @Schema(description = "Weighted Completion Rate (0-100)", example = "82.50")
    private BigDecimal wcr;

    @Schema(description = "Volume Index (0-120, normalized)", example = "95.00")
    private BigDecimal vi;

    @Schema(description = "Estimate Accuracy (0-100)", example = "88.00")
    private BigDecimal ea;

    @Schema(description = "Auto-calculated score", example = "85.20")
    private BigDecimal autoScore;

    @Schema(description = "Lead evaluation score (0-100)", example = "90.00")
    private BigDecimal leadScore;

    @Schema(description = "Manager evaluation score (0-100)", example = "85.00")
    private BigDecimal managerScore;

    @Schema(description = "Final KPI", example = "86.80")
    private BigDecimal kpiFinal;

    @Schema(description = "Ranking tier", example = "Xuất sắc")
    private String ranking;

    @Schema(description = "KPI trend vs previous month", example = "UP")
    private String trend;

    @Schema(description = "Previous month KPI", example = "82.50")
    private BigDecimal previousMonthKpi;

    @Schema(description = "Percentage change", example = "+5.2")
    private BigDecimal percentageChange;

    @Schema(description = "Whether result is cached", example = "true")
    private Boolean isCached;

    @Schema(description = "Cache expiry time", example = "2026-06-30T10:35:00Z")
    private String cacheExpiry;
}
