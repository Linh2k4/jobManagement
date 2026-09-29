package com.company.jobmanagement.dto.response;

import com.company.jobmanagement.model.enums.StepStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Leaf step of a Subtask, for MULTI_STEP tasks only (Scope.md §2.2).
 * Each step can be assigned to a different user and tracked independently.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "Step", description = "Leaf step under a Subtask (MULTI_STEP tasks)")
public class StepResponse {

    private Long id;
    private Integer stepOrder;
    private String name;
    private Integer estimateMinutes;
    private ZonedDateTime deadline;
    private StepStatus status;
    private UserInfoResponse assignee;
    private ZonedDateTime completedAt;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
