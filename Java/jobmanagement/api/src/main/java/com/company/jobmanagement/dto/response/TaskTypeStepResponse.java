package com.company.jobmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Step template for multi-step task types.
 * Defines the workflow steps for complex tasks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "TaskTypeStep",
    description = "Step template in a multi-step task type"
)
public class TaskTypeStepResponse {

    @Schema(description = "Step template ID", example = "1")
    private Long id;

    @Schema(
        description = "Sequence order of this step",
        example = "1"
    )
    private Integer stepOrder;

    @Schema(
        description = "Step name/description",
        example = "Review and Approve"
    )
    private String name;

    @Schema(
        description = "Estimated duration in minutes for this step",
        example = "120"
    )
    private Integer estimateMinutes;

    @Schema(description = "When this step template was created (ISO 8601)")
    private ZonedDateTime createdAt;
}
