package com.company.jobmanagement.dto.response;

import com.company.jobmanagement.model.enums.OrganizeBy;
import com.company.jobmanagement.model.enums.TimeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Response containing task type details and configuration.
 * Includes metadata, step templates, and timestamps.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "TaskType",
    description = "Task type template with configuration and steps"
)
public class TaskTypeResponse {

    @Schema(description = "Unique task type identifier", example = "1")
    private Long id;

    @Schema(description = "Unique code identifier", example = "MONTHLY_REPORT")
    private String code;

    @Schema(description = "Human-readable task type name", example = "Monthly Report Submission")
    private String name;

    @Schema(
        description = "Time category",
        example = "MULTI_STEP",
        allowableValues = {"FAST", "OFTEN", "MULTI_STEP"}
    )
    private TimeCategory timeCategory;

    @Schema(description = "Whether estimate is required", example = "true")
    private Boolean hasEstimate;

    @Schema(
        description = "Default time estimate in minutes",
        example = "480"
    )
    private Integer defaultEstimateMinutes;

    @Schema(
        description = "Whether this task type has multiple steps",
        example = "true"
    )
    private Boolean isMultiStep;

    @Schema(
        description = "Organization method",
        example = "SECTION",
        allowableValues = {"SECTION", "MONTH"}
    )
    private OrganizeBy organizeBy;

    @Schema(description = "Whether this task type is active and can be used", example = "true")
    private Boolean isActive;

    @Schema(description = "List of step templates for multi-step tasks")
    private List<TaskTypeStepResponse> steps;

    @Schema(description = "When this task type was created (ISO 8601)")
    private ZonedDateTime createdAt;

    @Schema(description = "When this task type was last updated (ISO 8601)")
    private ZonedDateTime updatedAt;
}
