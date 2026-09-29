package com.company.jobmanagement.dto.request;

import com.company.jobmanagement.model.enums.EvaluatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEvaluationRequest {

    @NotNull(message = "Evaluatee ID is required")
    @Schema(description = "ID of the user being evaluated", example = "1")
    private Long evaluateeId;

    @NotNull(message = "Evaluator type is required")
    @Schema(description = "Type of evaluator: SELF, LEAD, MANAGER", example = "SELF")
    private EvaluatorType evaluatorType;

    @NotNull(message = "Score is required")
    @Min(value = 1, message = "Score must be at least 1")
    @Max(value = 5, message = "Score must be at most 5")
    @Schema(description = "Rating score 1-5", example = "4")
    private Integer score;

    @Schema(description = "Evaluation comment")
    private String comment;

    @Schema(description = "Task ID if evaluating for a specific task")
    private Long taskId;

    @Schema(description = "Period month for period-based evaluations (YYYY-MM-01)")
    private LocalDate periodMonth;
}
