package com.company.jobmanagement.controller.task;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.request.AssignTaskRequest;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.TaskAssignmentResponse;
import com.company.jobmanagement.model.entity.TaskAssignment;
import com.company.jobmanagement.exception.ErrorResponse;
import com.company.jobmanagement.mapper.TaskAssignmentMapper;
import com.company.jobmanagement.service.TaskAssignmentService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Task assignment management controller handling task-to-user assignments.
 * <p>
 * Provides endpoints for:
 * - Assign task to a user
 * - Get assignment history for a task
 * - Reassign task to different user
 * </p>
 *
 * @author Khánh VD
 * @since 2026-06-29
 */
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/assignments")
@RequiredArgsConstructor
@Tag(
    name = "Task Assignments",
    description = "Task assignment and reassignment workflow\n\n" +
        "## Assignment Rules\n" +
        "- FAST: Member → Manager only\n" +
        "- OFTEN: Lead/Manager → Team members\n" +
        "- MULTI_STEP: Lead → Team, Manager → Anyone\n\n" +
        "## Permissions\n" +
        "- Assign: Task creator or manager\n" +
        "- Reassign: Current assignee or manager\n" +
        "- View history: Task participants"
)
public class TaskAssignmentController extends BaseController {

    private final TaskAssignmentService taskAssignmentService;
    private final TaskAssignmentMapper taskAssignmentMapper;

    /**
     * Assign a task to a user.
     * <p>
     * Role-based assignment rules:
     * - FAST: Member can assign to their manager only
     * - OFTEN: Lead/Manager can assign to team members
     * - MULTI_STEP: Lead to team members, Manager to anyone
     * </p>
     *
     * @param taskId task identifier
     * @param request assignee information
     * @return 201 CREATED with TaskAssignmentResponse
     */
    @PostMapping
    @Operation(
        summary = "Assign task to user",
        description = "Assign a task to a user based on role and task type permissions. Only creator or manager can assign.",
        operationId = "assignTask"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Task assigned successfully",
            content = @Content(schema = @Schema(implementation = TaskAssignmentResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid assignee or already assigned"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Task or assignee not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<TaskAssignmentResponse>> assignTask(
            @PathVariable Long taskId,
            @Valid @RequestBody AssignTaskRequest request) {
        TaskAssignment assignment = taskAssignmentService.assignTask(taskId, request.getAssigneeId(), request.getGroupId());
        TaskAssignmentResponse response = taskAssignmentMapper.toDTO(assignment);
        return created(ApiResponse.success("Task assigned successfully", response));
    }

    /**
     * Get assignment history for a task.
     * <p>
     * Returns all past and current assignments in reverse chronological order.
     * Only task participants and managers can view.
     * </p>
     *
     * @param taskId task identifier
     * @param pageable pagination info
     * @return Page of TaskAssignmentResponse
     */
    @GetMapping
    @Operation(
        summary = "Get assignment history",
        description = "Retrieve all assignments for a task (current and past), sorted by most recent first",
        operationId = "getAssignmentHistory"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Assignment history retrieved successfully"
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
    public ResponseEntity<ApiResponse<Page<TaskAssignmentResponse>>> getAssignmentHistory(
            @PathVariable Long taskId,
            Pageable pageable) {
        Page<TaskAssignment> assignments = taskAssignmentService.getAssignmentHistory(taskId, pageable);
        Page<TaskAssignmentResponse> responses = assignments.map(taskAssignmentMapper::toDTO);
        return ok(ApiResponse.success(responses));
    }

    /**
     * Reassign task to a different user.
     * <p>
     * Marks previous assignment as non-current and creates new assignment.
     * Only current assignee or manager can reassign.
     * </p>
     *
     * @param taskId task identifier
     * @param request new assignee information
     * @return Updated TaskAssignmentResponse
     */
    @PutMapping
    @Operation(
        summary = "Reassign task",
        description = "Reassign a task to a different user. Current assignee or manager only.",
        operationId = "reassignTask"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Task reassigned successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid assignee or task not assigned"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - not current assignee or manager"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Task or assignee not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<TaskAssignmentResponse>> reassignTask(
            @PathVariable Long taskId,
            @Valid @RequestBody AssignTaskRequest request) {
        TaskAssignment assignment = taskAssignmentService.reassignTask(taskId, request.getAssigneeId(), request.getGroupId());
        TaskAssignmentResponse response = taskAssignmentMapper.toDTO(assignment);
        return ok(ApiResponse.success("Task reassigned successfully", response));
    }

    /**
     * Get current assignment for a task.
     *
     * @param taskId task identifier
     * @return Current TaskAssignmentResponse or 404 if not assigned
     */
    @GetMapping("/current")
    @Operation(
        summary = "Get current assignment",
        description = "Retrieve the current active assignment for a task",
        operationId = "getCurrentAssignment"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Current assignment retrieved"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Task not found or not assigned"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<TaskAssignmentResponse>> getCurrentAssignment(
            @PathVariable Long taskId) {
        TaskAssignment assignment = taskAssignmentService.getCurrentAssignment(taskId);
        TaskAssignmentResponse response = taskAssignmentMapper.toDTO(assignment);
        return ok(ApiResponse.success(response));
    }
}
