package com.company.jobmanagement.controller.task;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.request.CreateTaskRequest;
import com.company.jobmanagement.dto.request.UpdateTaskRequest;
import com.company.jobmanagement.dto.request.UpdateTaskStatusRequest;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.KanbanBoardResponse;
import com.company.jobmanagement.dto.response.KanbanTaskResponse;
import com.company.jobmanagement.dto.response.TaskResponse;
import com.company.jobmanagement.dto.response.TimelineMemberResponse;
import com.company.jobmanagement.dto.response.TimelineTaskResponse;
import com.company.jobmanagement.dto.response.UserInfoResponse;
import com.company.jobmanagement.model.entity.Task;
import com.company.jobmanagement.model.entity.TaskAssignment;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.model.enums.Difficulty;
import com.company.jobmanagement.model.enums.Role;
import com.company.jobmanagement.model.enums.TaskStatus;
import com.company.jobmanagement.model.enums.TimeCategory;
import com.company.jobmanagement.exception.ErrorResponse;
import com.company.jobmanagement.mapper.TaskMapper;
import com.company.jobmanagement.repository.TaskAssignmentRepository;
import com.company.jobmanagement.repository.TaskAttachmentRepository;
import com.company.jobmanagement.repository.TaskCommentRepository;
import com.company.jobmanagement.repository.TaskRepository;
import com.company.jobmanagement.repository.SubtaskRepository;
import com.company.jobmanagement.repository.StepRepository;
import com.company.jobmanagement.repository.UserRepository;
import com.company.jobmanagement.security.CurrentUser;
import com.company.jobmanagement.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.company.jobmanagement.dto.request.BulkUpdateTaskStatusRequest;
import com.company.jobmanagement.dto.request.BulkAssignTaskRequest;
import com.company.jobmanagement.dto.request.AdvancedTaskSearchRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Task management controller handling task CRUD operations.
 * <p>
 * Provides endpoints for:
 * - Create new tasks (role-based)
 * - List tasks (with filtering, search, pagination)
 * - Get task details with assignments and steps
 * - Update task information
 * - Update task status (with state machine validation)
 * - Cancel tasks
 * </p>
 *
 * @author Khánh VD
 * @since 2026-06-29
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(
    name = "Tasks",
    description = "Task management and workflow\n\n" +
        "## Task Types\n" +
        "- **FAST**: Same-day tasks (Member can create)\n" +
        "- **OFTEN**: Recurring tasks by section (Lead/Manager)\n" +
        "- **MULTI_STEP**: Complex tasks with steps (Manager/Lead)\n\n" +
        "## Status Flow\n" +
        "PENDING → IN_PROGRESS → DONE\n" +
        "Any status → CANCELLED\n\n" +
        "## Permissions\n" +
        "- Create: Role-based per task type\n" +
        "- Update: Creator or task assignee\n" +
        "- Cancel: Creator or manager"
)
public class TaskController extends BaseController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final SubtaskRepository subtaskRepository;
    private final StepRepository stepRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final TaskRepository taskRepository;

    /**
     * Create a new task.
     * <p>
     * Role-based creation rules:
     * - MEMBER: Can create FAST tasks
     * - LEAD: Can create OFTEN and MULTI_STEP tasks
     * - MANAGER: Can create any task type
     * </p>
     *
     * @param request task creation details
     * @return 201 CREATED with TaskResponse
     */
    @PostMapping
    @Operation(
        summary = "Create new task",
        description = "Create a new task with type, title, estimate, and due date. Permission depends on task type and user role.",
        operationId = "createTask"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Task created successfully",
            content = @Content(schema = @Schema(implementation = TaskResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input or missing required fields"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions for this task type"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody CreateTaskRequest request) {
        Task task = taskService.createTask(
                request.getTaskTypeId(),
                request.getTitle(),
                request.getDescription(),
                request.getEstimateMinutes(),
                request.getDueDate(),
                request.getSection(),
                request.getPeriodMonth()
        );
        TaskResponse response = taskMapper.toDTO(task);
        return created(ApiResponse.success("Task created successfully", response));
    }

    /**
     * Get all tasks with optional filtering and pagination.
     * <p>
     * Filters available:
     * - status: PENDING, IN_PROGRESS, DONE, CANCELLED
     * - taskTypeId: Filter by task type
     * - section: Filter by team section
     * - dueDateFrom/dueDateTo: Filter by date range
     * - search: Search in title and description
     * </p>
     *
     * @param status task status filter (optional)
     * @param taskTypeId task type filter (optional)
     * @param section team section filter (optional)
     * @param dueDateFrom start date filter (optional)
     * @param dueDateTo end date filter (optional)
     * @param search search query (optional)
     * @param pageable pagination info (default: page 0, size 20)
     * @return Page of TaskResponse with pagination
     */
    @GetMapping
    @Operation(
        summary = "List tasks",
        description = "Retrieve tasks with filtering, search, and pagination. Users see only their tasks unless they are managers.",
        operationId = "listTasks"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Tasks retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> listTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Long taskTypeId,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) LocalDate dueDateFrom,
            @RequestParam(required = false) LocalDate dueDateTo,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<Task> tasks = taskService.searchTasks(
            status, taskTypeId, section, dueDateFrom, dueDateTo, search, pageable
        );
        Page<TaskResponse> responses = tasks.map(taskMapper::toDTO);
        return ok(ApiResponse.success(responses));
    }

    /**
     * Get task details by ID with assignments and steps.
     *
     * @param taskId task identifier
     * @return TaskResponse with full details
     */
    @GetMapping("/{taskId}")
    @Operation(
        summary = "Get task details",
        description = "Retrieve complete task information including assignments and steps",
        operationId = "getTask"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Task retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Task not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable Long taskId) {
        Task task = taskService.getTask(taskId);
        TaskResponse response = taskMapper.toDTO(task);
        return ok(ApiResponse.success(response));
    }

    /**
     * Update task details (title, description, estimate, due date).
     * Only the task creator or manager can update.
     *
     * @param taskId task identifier
     * @param request update details
     * @return Updated TaskResponse
     */
    @PutMapping("/{taskId}")
    @Operation(
        summary = "Update task",
        description = "Update task details (title, description, estimate, due date). Creator or manager only.",
        operationId = "updateTask"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Task updated successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - not task creator or manager"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Task not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        Task task = taskService.updateTask(
                taskId,
                request.getTitle(),
                request.getDescription(),
                request.getDueDate(),
                request.getEstimateMinutes(),
                request.getSection()
        );
        TaskResponse response = taskMapper.toDTO(task);
        return ok(ApiResponse.success("Task updated successfully", response));
    }

    /**
     * Update task status with state machine validation.
     * <p>
     * Valid transitions:
     * - PENDING → IN_PROGRESS
     * - IN_PROGRESS → DONE
     * - Any status → CANCELLED
     * </p>
     *
     * @param taskId task identifier
     * @param request new status
     * @return Updated TaskResponse
     */
    @PatchMapping("/{taskId}/status")
    @Operation(
        summary = "Update task status",
        description = "Change task status (PENDING → IN_PROGRESS → DONE, or CANCELLED). Enforces valid state transitions.",
        operationId = "updateTaskStatus"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Status updated successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid status transition"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - not task assignee"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Task not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        Task task = taskService.updateTaskStatus(taskId, request.getStatus());
        TaskResponse response = taskMapper.toDTO(task);
        return ok(ApiResponse.success("Task status updated successfully", response));
    }

    /**
     * Cancel a task (sets status to CANCELLED).
     * Only the task creator or manager can cancel.
     *
     * @param taskId task identifier
     * @return Updated TaskResponse with CANCELLED status
     */
    @DeleteMapping("/{taskId}")
    @Operation(
        summary = "Cancel task",
        description = "Cancel a task (sets status to CANCELLED). Creator or manager only.",
        operationId = "cancelTask"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "Task cancelled successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - not task creator or manager"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Task not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<Void> cancelTask(@PathVariable Long taskId) {
        taskService.cancelTask(taskId);
        return noContent();
    }

    /**
     * Update task difficulty (1-5). Lead/Manager only — feeds KPI task-weight.
     *
     * @param taskId task identifier
     * @param request body with integer "difficulty" field, 1-5
     * @return Updated TaskResponse
     */
    @PatchMapping("/{taskId}/difficulty")
    @Operation(
        summary = "Update task difficulty",
        description = "Set difficulty level (1-5). Lead or Manager only.",
        operationId = "updateTaskDifficulty"
    )
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<TaskResponse>> updateDifficulty(
            @PathVariable Long taskId,
            @RequestBody Map<String, Integer> request) {
        Difficulty difficulty = Difficulty.fromLevel(request.get("difficulty"));
        Task task = taskService.updateDifficulty(taskId, difficulty);
        TaskResponse response = taskMapper.toDTO(task);
        return ok(ApiResponse.success("Difficulty updated", response));
    }

    /**
     * Extend a task's due date. Creator or Manager only.
     *
     * @param taskId task identifier
     * @param request body with "daysToAdd" (int) and "reason" (string)
     * @return Updated TaskResponse
     */
    @PostMapping("/{taskId}/extend-deadline")
    @Operation(
        summary = "Extend task deadline",
        description = "Push the due date back by N days, with a reason logged for audit. Creator or Manager only.",
        operationId = "extendTaskDeadline"
    )
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<TaskResponse>> extendDeadline(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> request) {
        int daysToAdd = ((Number) request.get("daysToAdd")).intValue();
        String reason = (String) request.get("reason");
        Task task = taskService.extendDeadline(taskId, daysToAdd, reason);
        TaskResponse response = taskMapper.toDTO(task);
        return ok(ApiResponse.success("Deadline extended", response));
    }

    /**
     * Get task audit trail showing all state changes.
     * Shows who changed what and when.
     *
     * @param taskId task identifier
     * @return List of audit events
     */
    @GetMapping("/{taskId}/audit")
    @Operation(
        summary = "Get task audit trail",
        description = "Retrieve history of all changes to the task",
        operationId = "getTaskAudit"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Audit trail retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Task not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Object>> getTaskAudit(@PathVariable Long taskId) {
        Object audit = taskService.getTaskAuditTrail(taskId);
        return ok(ApiResponse.success(audit));
    }

    /**
     * Kanban board (Scope.md §2.0): tasks bucketed into TODO/DOING/DONE.
     * A Member always sees only their own assigned tasks; Lead/Manager can
     * pass assigneeId to focus on one person or omit it for the whole team.
     */
    @GetMapping("/kanban")
    @Operation(summary = "Get Kanban board", description = "Tasks grouped into TODO/DOING/DONE columns", operationId = "getKanbanBoard")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<KanbanBoardResponse>> getKanbanBoard(
            @RequestParam(required = false) TimeCategory type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long assigneeId) {

        List<Task> tasks = taskService.getKanbanTasks(type, search, assigneeId);
        List<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toList());

        Map<Long, Long> commentCounts;
        Map<Long, Long> attachmentCounts;
        // FAST/OFTEN: Subtask is the leaf, count it directly. MULTI_STEP:
        // Step is the leaf, one join deeper through Subtask.
        Map<Long, Long> subtaskTotal;
        Map<Long, Long> subtaskDone;
        Map<Long, Long> stepTotal;
        Map<Long, Long> stepDone;
        Map<Long, TaskAssignment> currentAssignments;
        if (taskIds.isEmpty()) {
            commentCounts = Map.of();
            attachmentCounts = Map.of();
            subtaskTotal = Map.of();
            subtaskDone = Map.of();
            stepTotal = Map.of();
            stepDone = Map.of();
            currentAssignments = Map.of();
        } else {
            commentCounts = toCountMap(taskCommentRepository.countByTaskIds(taskIds));
            attachmentCounts = toCountMap(taskAttachmentRepository.countByTaskIds(taskIds));
            subtaskTotal = toCountMap(subtaskRepository.countByTaskIds(taskIds));
            subtaskDone = toCountMap(subtaskRepository.countDoneByTaskIds(taskIds));
            stepTotal = toCountMap(stepRepository.countByTaskIds(taskIds));
            stepDone = toCountMap(stepRepository.countDoneByTaskIds(taskIds));
            currentAssignments = taskAssignmentRepository.findCurrentAssignmentsForTasks(taskIds).stream()
                    .collect(Collectors.toMap(a -> a.getTask().getId(), a -> a));
        }

        List<KanbanTaskResponse> todo = new ArrayList<>();
        List<KanbanTaskResponse> doing = new ArrayList<>();
        List<KanbanTaskResponse> done = new ArrayList<>();

        for (Task task : tasks) {
            boolean isMultiStep = task.getTaskType().getTimeCategory() == TimeCategory.MULTI_STEP;
            Map<Long, Long> leafTotal = isMultiStep ? stepTotal : subtaskTotal;
            Map<Long, Long> leafDone = isMultiStep ? stepDone : subtaskDone;

            KanbanTaskResponse card = KanbanTaskResponse.builder()
                    .id(task.getId())
                    .title(task.getTitle())
                    .timeCategory(task.getTaskType().getTimeCategory())
                    .priority(task.getPriority() != null ? task.getPriority().name() : null)
                    .status(task.getStatus().name())
                    .dueDate(task.getDueDate())
                    .isOverdue(task.getDueDate() != null
                            && task.getDueDate().isBefore(LocalDate.now())
                            && task.getStatus() == TaskStatus.IN_PROGRESS)
                    .stepsDone(leafDone.getOrDefault(task.getId(), 0L).intValue())
                    .stepsTotal(leafTotal.getOrDefault(task.getId(), 0L).intValue())
                    .commentCount(commentCounts.getOrDefault(task.getId(), 0L).intValue())
                    .attachmentCount(attachmentCounts.getOrDefault(task.getId(), 0L).intValue())
                    .assignee(Optional.ofNullable(currentAssignments.get(task.getId()))
                            .map(a -> UserInfoResponse.builder()
                                    .id(a.getAssignee().getId())
                                    .fullName(a.getAssignee().getFullName())
                                    .email(a.getAssignee().getEmail())
                                    .role(a.getAssignee().getRole().name())
                                    .build())
                            .orElse(null))
                    .build();

            switch (task.getStatus()) {
                case PENDING -> todo.add(card);
                case DONE, CLOSED_LATE -> done.add(card);
                default -> doing.add(card);
            }
        }

        KanbanBoardResponse board = KanbanBoardResponse.builder()
                .todo(todo).doing(doing).done(done).build();
        return ok(ApiResponse.success(board));
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    /**
     * Timeline / Gantt view (Scope.md §5.5): one row per member, each with
     * their task bars. Member sees only themselves; Lead sees their team;
     * Manager sees every Member (optionally narrowed to one via assigneeId).
     * The schema has no separate task start-date, so each bar approximates
     * start = createdAt's date, end = dueDate (or start + 1 day).
     */
    @GetMapping("/timeline")
    @Operation(summary = "Get Timeline/Gantt data", description = "Per-member task bars for the Timeline view", operationId = "getTimeline")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<TimelineMemberResponse>>> getTimeline(
            @RequestParam(required = false, defaultValue = "false") boolean includeCompleted,
            @RequestParam(required = false) Long assigneeId) {

        User user = currentUser.getCurrentUser();
        List<User> members;
        if (user.isMember()) {
            members = List.of(user);
        } else if (user.isManager()) {
            // Scope.md §5.5.3's mockup shows both Member and Lead rows —
            // "toàn bộ hệ thống" isn't Members only.
            if (assigneeId != null) {
                members = userRepository.findById(assigneeId).map(List::of).orElse(List.of());
            } else {
                members = new ArrayList<>(userRepository.findByRoleAndActive(Role.MEMBER));
                members.addAll(userRepository.findByRoleAndActive(Role.LEAD));
            }
        } else {
            members = assigneeId != null
                    ? userRepository.findByLeadId(user.getId()).stream().filter(m -> m.getId().equals(assigneeId)).collect(Collectors.toList())
                    : userRepository.findByLeadId(user.getId());
        }

        List<TimelineMemberResponse> result = new ArrayList<>();
        for (User member : members) {
            List<Task> tasks = taskRepository.findForTimeline(member.getId(), includeCompleted);
            List<TimelineTaskResponse> bars = tasks.stream().map(t -> {
                LocalDate start = t.getCreatedAt().toLocalDate();
                LocalDate end = t.getDueDate() != null ? t.getDueDate() : start.plusDays(1);
                boolean done = t.getStatus() == TaskStatus.DONE || t.getStatus() == TaskStatus.CLOSED_LATE;
                boolean overdue = !done && end.isBefore(LocalDate.now());
                return TimelineTaskResponse.builder()
                        .id(t.getId())
                        .title(t.getTitle())
                        .timeCategory(t.getTaskType().getTimeCategory())
                        .status(t.getStatus().name())
                        .startDate(start)
                        .endDate(end)
                        .isOverdue(overdue)
                        .isDone(done)
                        .build();
            }).collect(Collectors.toList());

            result.add(TimelineMemberResponse.builder()
                    .userId(member.getId())
                    .fullName(member.getFullName())
                    .role(member.getRole().name())
                    .tasks(bars)
                    .build());
        }

        return ok(ApiResponse.success(result));
    }

    /**
     * Quick-add a task from a Kanban column (Scope.md §2.0.4) — title +
     * task type only, no other fields required up front.
     */
    @PostMapping("/quick")
    @Operation(summary = "Quick-create a task", description = "Minimal task creation from the Kanban board", operationId = "quickCreateTask")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<TaskResponse>> quickCreateTask(@RequestBody Map<String, Object> request) {
        Long taskTypeId = ((Number) request.get("taskTypeId")).longValue();
        String title = (String) request.get("title");
        Task task = taskService.quickCreateTask(taskTypeId, title);
        TaskResponse response = taskMapper.toDTO(task);
        return created(ApiResponse.success("Task created", response));
    }

    @PostMapping("/bulk/status")
    @Operation(summary = "Bulk update task status")
    @PreAuthorize("hasAnyRole('MANAGER', 'LEAD')")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> bulkUpdateStatus(
            @Valid @RequestBody BulkUpdateTaskStatusRequest request) {
        taskService.bulkUpdateStatus(request.getTaskIds(), request.getNewStatus(), request.getReason());
        return ok(ApiResponse.success(String.format("%d tasks updated", request.getTaskIds().size())));
    }

    @PostMapping("/bulk/assign")
    @Operation(summary = "Bulk assign tasks to user")
    @PreAuthorize("hasAnyRole('MANAGER', 'LEAD')")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> bulkAssign(
            @Valid @RequestBody BulkAssignTaskRequest request) {
        taskService.bulkAssign(request.getTaskIds(), request.getAssigneeId(), request.getNotes());
        return ok(ApiResponse.success(String.format("%d tasks assigned", request.getTaskIds().size())));
    }

    @PostMapping("/search/advanced")
    @Operation(summary = "Advanced task search with multiple filters")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Page<Task>>> advancedSearch(
            @RequestBody AdvancedTaskSearchRequest request,
            Pageable pageable) {
        Page<Task> results = taskService.advancedSearch(
                request.getKeyword(),
                request.getStatus(),
                null,
                request.getTaskTypeId(),
                request.getDueDateFrom(),
                request.getDueDateTo(),
                request.getSection(),
                request.getHasEstimate(),
                pageable
        );
        return ok(ApiResponse.success(results));
    }
}
