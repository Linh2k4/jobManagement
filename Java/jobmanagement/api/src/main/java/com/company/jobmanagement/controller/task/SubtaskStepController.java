package com.company.jobmanagement.controller.task;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.request.UpdateStepStatusRequest;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.StepResponse;
import com.company.jobmanagement.mapper.StepMapper;
import com.company.jobmanagement.model.entity.Step;
import com.company.jobmanagement.service.StepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Leaf steps of a Subtask, MULTI_STEP tasks only (Scope.md §2.2).
 * <p>
 * Steps in the same subtask run sequentially by order — completing one
 * unlocks the next and, once every step is done, cascades the subtask
 * (then possibly the task) to DONE.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/subtasks/{subtaskId}/steps")
@RequiredArgsConstructor
@Tag(
    name = "Steps",
    description = "Multi-step task workflow\n\n" +
        "## Step Workflow\n" +
        "PENDING → IN_PROGRESS → DONE, sequential within a subtask\n\n" +
        "## Permissions\n" +
        "- Update status: step assignee, task assignee, lead, or manager\n" +
        "- Assign: lead or manager"
)
public class SubtaskStepController extends BaseController {

    private final StepService stepService;
    private final StepMapper stepMapper;

    @GetMapping
    @Operation(summary = "List steps for a subtask", operationId = "getSteps")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<StepResponse>>> getSteps(@PathVariable Long subtaskId) {
        List<StepResponse> responses = stepService.getStepsForSubtask(subtaskId).stream()
                .map(stepMapper::toDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(responses));
    }

    @GetMapping("/{stepId}")
    @Operation(summary = "Get step details", operationId = "getStep")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<StepResponse>> getStep(
            @PathVariable Long subtaskId, @PathVariable Long stepId) {
        Step step = stepService.getStep(stepId);
        return ok(ApiResponse.success(stepMapper.toDTO(step)));
    }

    @PostMapping
    @Operation(summary = "Add a step to a subtask", description = "Creator or Manager only", operationId = "createStep")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<StepResponse>> createStep(
            @PathVariable Long subtaskId,
            @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        Integer estimateMinutes = request.get("estimateMinutes") != null
                ? ((Number) request.get("estimateMinutes")).intValue() : null;
        ZonedDateTime deadline = request.get("deadline") != null
                ? ZonedDateTime.parse((String) request.get("deadline")) : null;
        Step step = stepService.createStep(subtaskId, name, estimateMinutes, deadline);
        return created(ApiResponse.success("Step added", stepMapper.toDTO(step)));
    }

    @PutMapping("/{stepId}")
    @Operation(summary = "Update a step's name/estimate/deadline", description = "Creator or Manager only", operationId = "updateStep")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<StepResponse>> updateStep(
            @PathVariable Long subtaskId,
            @PathVariable Long stepId,
            @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        Integer estimateMinutes = request.get("estimateMinutes") != null
                ? ((Number) request.get("estimateMinutes")).intValue() : null;
        ZonedDateTime deadline = request.get("deadline") != null
                ? ZonedDateTime.parse((String) request.get("deadline")) : null;
        Step step = stepService.updateStep(stepId, name, estimateMinutes, deadline);
        return ok(ApiResponse.success("Step updated", stepMapper.toDTO(step)));
    }

    @DeleteMapping("/{stepId}")
    @Operation(summary = "Delete a step", description = "Creator or Manager only", operationId = "deleteStep")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> deleteStep(@PathVariable Long subtaskId, @PathVariable Long stepId) {
        stepService.deleteStep(stepId);
        return ok(ApiResponse.success("Step deleted"));
    }

    @PutMapping("/{stepId}/status")
    @Operation(summary = "Update step status", description = "PENDING → IN_PROGRESS → DONE, sequential within the subtask", operationId = "updateStepStatus")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<StepResponse>> updateStepStatus(
            @PathVariable Long subtaskId,
            @PathVariable Long stepId,
            @Valid @RequestBody UpdateStepStatusRequest request) {
        Step step = stepService.updateStepStatus(stepId, request.getStatus());
        return ok(ApiResponse.success("Step status updated successfully", stepMapper.toDTO(step)));
    }

    @PostMapping("/{stepId}/assign")
    @Operation(summary = "Assign a step to a user", description = "Lead or Manager only", operationId = "assignStep")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<StepResponse>> assignStep(
            @PathVariable Long subtaskId,
            @PathVariable Long stepId,
            @RequestBody Map<String, Object> request) {
        Long userId = ((Number) request.get("userId")).longValue();
        Step step = stepService.assignStep(stepId, userId);
        return ok(ApiResponse.success("Step assigned", stepMapper.toDTO(step)));
    }
}
