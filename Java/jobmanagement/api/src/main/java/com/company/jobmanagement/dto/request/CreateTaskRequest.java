package com.company.jobmanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request to create a new task.
 * Task creation is subject to role-based rules:
 * - MANAGER: Can create any task, assign to any member
 * - LEAD: Can create tasks for their team
 * - MEMBER: Can create FAST_TASK and assign to their own manager
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "CreateTaskRequest",
    description = "Request to create a new task"
)
public class CreateTaskRequest {

    @NotNull(message = "Task type ID is required")
    @Schema(
        description = "ID of the task type template to use",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long taskTypeId;

    @NotBlank(message = "Title is required")
    @Schema(
        description = "Task title/subject",
        example = "Complete quarterly report",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String title;

    @Schema(
        description = "Detailed task description",
        example = "Complete and submit the Q2 financial report with all supporting documents"
    )
    private String description;

    @Min(value = 1, message = "Estimate must be positive if provided")
    @Schema(
        description = "Estimated time in minutes. Required if task type has hasEstimate=true",
        example = "480"
    )
    private Integer estimateMinutes;

    @Schema(
        description = "Task due date (YYYY-MM-DD)",
        example = "2026-06-30"
    )
    private LocalDate dueDate;

    @Schema(
        description = "Team section/department for OFTEN tasks",
        example = "Finance"
    )
    private String section;

    @Schema(
        description = "Period month (YYYY-MM) for MULTI_STEP tasks",
        example = "2026-06"
    )
    private LocalDate periodMonth;
}
