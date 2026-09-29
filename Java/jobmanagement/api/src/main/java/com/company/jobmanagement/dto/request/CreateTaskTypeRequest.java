package com.company.jobmanagement.dto.request;

import com.company.jobmanagement.model.enums.OrganizeBy;
import com.company.jobmanagement.model.enums.TimeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to create a new task type template.
 * Task types define how tasks are organized (by time category or work category).
 * Supports configurable steps for multi-step tasks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "CreateTaskTypeRequest",
    description = "Request to create a new task type template"
)
public class CreateTaskTypeRequest {

    @NotBlank(message = "Code is required")
    @Schema(
        description = "Unique code identifier for task type (e.g., FAST_TASK, MONTHLY_REPORT)",
        example = "MONTHLY_REPORT",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String code;

    @NotBlank(message = "Name is required")
    @Schema(
        description = "Human-readable name of task type",
        example = "Monthly Report Submission",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @NotNull(message = "Time category is required")
    @Schema(
        description = "Time organization category (FAST: same-day, OFTEN: recurring, MULTI_STEP: multiple steps)",
        example = "MULTI_STEP",
        requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = {"FAST", "OFTEN", "MULTI_STEP"}
    )
    private TimeCategory timeCategory;

    @Builder.Default
    @Schema(
        description = "Whether tasks of this type require time estimates",
        example = "true"
    )
    private Boolean hasEstimate = true;

    @Min(value = 1, message = "Default estimate must be positive")
    @Schema(
        description = "Default time estimate in minutes for new tasks",
        example = "480"
    )
    private Integer defaultEstimateMinutes;

    @Schema(
        description = "Organization method (SECTION: organize by team section, MONTH: organize by calendar month)",
        example = "SECTION",
        allowableValues = {"SECTION", "MONTH"}
    )
    private OrganizeBy organizeBy;
}
