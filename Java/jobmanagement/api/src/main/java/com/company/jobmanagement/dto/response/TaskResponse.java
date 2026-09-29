package com.company.jobmanagement.dto.response;

import com.company.jobmanagement.model.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Complete task information with status, assignments, and steps.
 * Returned when retrieving or creating a task.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "Task",
    description = "Complete task information with all related data"
)
public class TaskResponse {

    @Schema(description = "Unique task identifier", example = "1")
    private Long id;

    @Schema(description = "Task type ID this task belongs to", example = "1")
    private Long taskTypeId;

    @Schema(description = "Task title/subject", example = "Complete quarterly report")
    private String title;

    @Schema(description = "Detailed task description")
    private String description;

    @Schema(
        description = "Current task status",
        example = "PENDING",
        allowableValues = {"PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED"}
    )
    private TaskStatus status;

    @Schema(description = "Estimated duration in minutes", example = "480")
    private Integer estimateMinutes;

    @Schema(description = "Difficulty level, 1 (very easy) to 5 (very hard); set by Lead/Manager only", example = "3")
    private Integer difficulty;

    @Schema(description = "Task priority", example = "MEDIUM", allowableValues = {"LOW", "MEDIUM", "HIGH", "URGENT"})
    private String priority;

    @Schema(description = "Task due date", example = "2026-06-30")
    private LocalDate dueDate;

    @Schema(description = "Team section/department", example = "Finance")
    private String section;

    @Schema(description = "Period month for organization", example = "2026-06")
    private LocalDate periodMonth;

    @Schema(description = "User who created this task")
    private UserInfoResponse createdBy;

    @Schema(description = "When task was completed (null if not completed)", example = "2026-06-25T10:30:00+07:00")
    private ZonedDateTime completedAt;

    @Schema(description = "Whether this task tracks an overall target (Scope.md §2.2 isTarget)")
    private Boolean isTarget;

    @Schema(description = "Planned target value, if isTarget", example = "100")
    private Integer estimateTarget;

    @Schema(description = "Actual target value so far, if isTarget", example = "75")
    private Integer target;

    @Schema(description = "Group headers for this task's subtasks (optional grouping)")
    private List<GroupSubtaskResponse> groupSubtasks;

    @Schema(description = "All subtasks — grouped (groupSubtaskId set) and ungrouped (null)")
    private List<SubtaskResponse> subtasks;

    @Schema(description = "Current assignment (who is working on it)")
    private TaskAssignmentResponse currentAssignment;

    @Schema(description = "When task was created (ISO 8601)", example = "2026-06-20T10:30:00+07:00")
    private ZonedDateTime createdAt;

    @Schema(description = "When task was last updated (ISO 8601)", example = "2026-06-23T14:45:00+07:00")
    private ZonedDateTime updatedAt;
}
