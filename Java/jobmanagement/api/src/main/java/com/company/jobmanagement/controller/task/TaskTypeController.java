package com.company.jobmanagement.controller.task;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.model.entity.TaskType;
import com.company.jobmanagement.model.entity.TaskTypeStep;
import com.company.jobmanagement.model.entity.TaskTypeSubtask;
import com.company.jobmanagement.dto.request.CreateTaskTypeRequest;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.TaskTypeResponse;
import com.company.jobmanagement.dto.response.TaskTypeStepResponse;
import com.company.jobmanagement.exception.ErrorResponse;
import com.company.jobmanagement.service.TaskTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Task Type Management controller handling task type template operations.
 * <p>
 * Provides endpoints for:
 * - Get all active task types
 * - Get specific task type with steps
 * - Create new task type (Manager only)
 * - Update task type (Manager only)
 * </p>
 *
 * @author Khánh VD
 * @since 2026-06-29
 */
@RestController
@RequestMapping("/api/v1/task-types")
@RequiredArgsConstructor
@Tag(
    name = "Task Types",
    description = "Task type template management\n\n" +
        "## Overview\n" +
        "Task types define how work is organized and tracked.\n" +
        "- **FAST**: Same-day tasks (e.g., email replies)\n" +
        "- **OFTEN**: Recurring tasks organized by team section\n" +
        "- **MULTI_STEP**: Complex tasks with multiple steps\n\n" +
        "## Permissions\n" +
        "- GET: Authenticated users\n" +
        "- POST/PUT: Manager only"
)
public class TaskTypeController extends BaseController {

    private final TaskTypeService taskTypeService;

    /**
     * Get all active task types.
     * <p>
     * Returns list of task type templates available for creating tasks.
     * </p>
     *
     * @return List of TaskTypeResponse
     */
    @GetMapping
    @Operation(
        summary = "Get all task types",
        description = "Retrieve all active task type templates",
        operationId = "getAllTaskTypes"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Task types retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<TaskTypeResponse>>> getAllTaskTypes() {
        List<TaskTypeResponse> taskTypes = taskTypeService.getAllActiveTaskTypes()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(taskTypes));
    }

    /**
     * Get task type by ID with all steps.
     *
     * @param id task type identifier
     * @return TaskTypeResponse with nested steps
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get task type by ID",
        description = "Retrieve specific task type with all its steps",
        operationId = "getTaskType"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Task type retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Task type not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<TaskTypeResponse>> getTaskType(@PathVariable Long id) {
        TaskType taskType = taskTypeService.getTaskType(id);
        return ok(ApiResponse.success(mapToResponse(taskType)));
    }

    /**
     * Create a new task type (Manager only).
     * <p>
     * Creates a template for organizing tasks with specific steps.
     * </p>
     *
     * @param request task type details with steps
     * @return 201 CREATED with TaskTypeResponse
     */
    /**
     * Map TaskType entity to TaskTypeResponse DTO. {@code steps} flattens
     * the group/subtask/step template tree (Scope.md §2.2) into the old
     * flat shape — nothing consumes the tree structure yet (no template
     * management screen this round), so this is a deliberate stand-in.
     */
    private TaskTypeResponse mapToResponse(TaskType taskType) {
        return TaskTypeResponse.builder()
                .id(taskType.getId())
                .code(taskType.getCode())
                .name(taskType.getName())
                .timeCategory(taskType.getTimeCategory())
                .hasEstimate(taskType.getHasEstimate())
                .defaultEstimateMinutes(taskType.getDefaultEstimateMinutes())
                .isMultiStep(taskType.getIsMultiStep())
                .organizeBy(taskType.getOrganizeBy())
                .isActive(taskType.getIsActive())
                .steps(taskType.getSubtasks().stream()
                        .sorted(Comparator.comparing(TaskTypeSubtask::getSubtaskOrder))
                        .flatMap(subtask -> subtask.getSteps().stream())
                        .sorted(Comparator.comparing(TaskTypeStep::getStepOrder))
                        .map(step -> TaskTypeStepResponse.builder()
                                .id(step.getId())
                                .stepOrder(step.getStepOrder())
                                .name(step.getName())
                                .estimateMinutes(step.getEstimateMinutes())
                                .createdAt(step.getCreatedAt())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(taskType.getCreatedAt())
                .updatedAt(taskType.getUpdatedAt())
                .build();
    }
}
