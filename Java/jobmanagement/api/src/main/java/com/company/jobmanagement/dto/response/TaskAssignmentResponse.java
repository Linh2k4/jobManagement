package com.company.jobmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Task assignment record showing who the task is assigned to and who assigned it.
 * Tracks assignment history for auditing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "TaskAssignment",
    description = "Task assignment record with assignee and assignment history"
)
public class TaskAssignmentResponse {

    @Schema(description = "Assignment record ID", example = "1")
    private Long id;

    @Schema(description = "User the task is assigned to")
    private UserInfoResponse assignee;

    @Schema(description = "User who made the assignment")
    private UserInfoResponse assignedBy;

    @Schema(description = "Whether this is the current active assignment", example = "true")
    private Boolean isCurrent;

    @Schema(description = "When the assignment was made", example = "2026-06-20T10:30:00+07:00")
    private ZonedDateTime assignedAt;
}
