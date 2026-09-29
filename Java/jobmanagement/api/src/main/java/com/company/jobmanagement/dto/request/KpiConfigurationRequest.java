package com.company.jobmanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KPI formula weight update (Scope.md §7.6). Fields are nullable so a caller
 * can patch a subset — {@link com.company.jobmanagement.controller.kpi.KpiController}
 * only applies the ones present, same as before this DTO existed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Partial update for KPI formula weights; omit a field to leave it unchanged")
public class KpiConfigurationRequest {

    @Schema(description = "Weighted Completion Rate weight, 40-80%")
    private Double wcrWeight;

    @Schema(description = "Volume Index weight, 10-40%")
    private Double viWeight;

    @Schema(description = "Estimate Accuracy weight, 0-30%")
    private Double eaWeight;

    @Schema(description = "Auto score weight in KPI final, 20-70%")
    private Double autoScoreWeight;

    @Schema(description = "Lead score weight in KPI final, 10-60%")
    private Double leadScoreWeight;

    @Schema(description = "Manager score weight in KPI final, 10-50%")
    private Double managerScoreWeight;

    @Schema(description = "Working hours per day used for Volume Index, 1-24")
    private Integer workingHoursPerDay;
}
