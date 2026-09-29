package com.company.jobmanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "EvaluationScoresRequest", description = "Evaluation scores (1-10 scale)")
public class EvaluationScoresRequest {

    @Schema(description = "Quality score", example = "8.5")
    @DecimalMin("1") @DecimalMax("10")
    private BigDecimal quality;

    @Schema(description = "Responsibility score")
    @DecimalMin("1") @DecimalMax("10")
    private BigDecimal responsibility;

    @Schema(description = "Teamwork score")
    @DecimalMin("1") @DecimalMax("10")
    private BigDecimal teamwork;

    @Schema(description = "Initiative score")
    @DecimalMin("1") @DecimalMax("10")
    private BigDecimal initiative;

    @Schema(description = "Discipline score")
    @DecimalMin("1") @DecimalMax("10")
    private BigDecimal discipline;

    @Schema(description = "Reviewer notes")
    private String notes;
}
