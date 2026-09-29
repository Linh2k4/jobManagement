package com.company.jobmanagement.dto.response;

import com.company.jobmanagement.model.enums.StepStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Leaf for a FAST task (status ticked manually); parent-of-steps for a
 * MULTI_STEP task (status/completion derives from its steps instead —
 * Scope.md §2.1/§2.2). {@code groupSubtaskId} is null for an ungrouped
 * subtask sitting directly under the task.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "Subtask", description = "Subtask — leaf for FAST, parent-of-steps for MULTI_STEP")
public class SubtaskResponse {

    private Long id;
    private Long groupSubtaskId;
    private Integer subtaskOrder;
    private String title;
    private StepStatus status;

    // FAST-only
    private Integer estimateMinutes;
    private ZonedDateTime deadline;
    private String note;

    // MULTI_STEP-only
    private Boolean isTarget;
    private Integer estimateTarget;
    private Integer target;

    private List<StepResponse> steps;

    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
