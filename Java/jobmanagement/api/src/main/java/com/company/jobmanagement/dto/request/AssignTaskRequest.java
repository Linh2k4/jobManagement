package com.company.jobmanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to assign a task to a user.
 * Assignment is subject to role-based rules based on task type and user roles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "AssignTaskRequest",
    description = "Request to assign or reassign a task to a user"
)
public class AssignTaskRequest {

    @NotNull(message = "Assignee ID is required")
    @Schema(
        description = "ID of the user to assign the task to",
        example = "5",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long assigneeId;

    @Schema(description = "Scope.md §13.4 — explicit Group this task counts toward, when the assignee " +
            "belongs to more than one. Omit to auto-derive from the assignee's primary group.", example = "2")
    private Long groupId;
}
