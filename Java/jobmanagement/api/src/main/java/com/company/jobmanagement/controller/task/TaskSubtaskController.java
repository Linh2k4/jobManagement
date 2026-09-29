package com.company.jobmanagement.controller.task;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.SubtaskResponse;
import com.company.jobmanagement.mapper.SubtaskMapper;
import com.company.jobmanagement.model.entity.Subtask;
import com.company.jobmanagement.model.enums.StepStatus;
import com.company.jobmanagement.service.SubtaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Subtasks under a task (Scope.md §2.1/§2.2) — leaf for FAST tasks (ticked
 * done manually via {@link #updateStatus}), parent-of-steps for MULTI_STEP
 * tasks (status/target fields there instead, managed via SubtaskStepController).
 */
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/subtasks")
@RequiredArgsConstructor
@Tag(name = "Subtasks", description = "Leaf for FAST tasks, parent-of-steps for MULTI_STEP tasks")
public class TaskSubtaskController extends BaseController {

    private final SubtaskService subtaskService;
    private final SubtaskMapper subtaskMapper;

    @GetMapping
    @Operation(summary = "List subtasks (grouped and ungrouped)", operationId = "getSubtasks")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<SubtaskResponse>>> getSubtasks(@PathVariable Long taskId) {
        List<SubtaskResponse> responses = subtaskService.getSubtasksForTask(taskId).stream()
                .map(subtaskMapper::toDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(responses));
    }

    @PostMapping
    @Operation(summary = "Add a subtask", description = "groupSubtaskId is optional — omit for an ungrouped subtask", operationId = "createSubtask")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<SubtaskResponse>> createSubtask(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> request) {
        Long groupSubtaskId = request.get("groupSubtaskId") != null
                ? ((Number) request.get("groupSubtaskId")).longValue() : null;
        String title = (String) request.get("title");
        Integer estimateMinutes = request.get("estimateMinutes") != null
                ? ((Number) request.get("estimateMinutes")).intValue() : null;
        ZonedDateTime deadline = request.get("deadline") != null
                ? ZonedDateTime.parse((String) request.get("deadline")) : null;
        String note = (String) request.get("note");

        Subtask subtask = subtaskService.createSubtask(taskId, groupSubtaskId, title, estimateMinutes, deadline, note);
        return created(ApiResponse.success("Subtask added", subtaskMapper.toDTO(subtask)));
    }

    @PutMapping("/{subtaskId}")
    @Operation(summary = "Edit a subtask's fields", operationId = "updateSubtask")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<SubtaskResponse>> updateSubtask(
            @PathVariable Long taskId,
            @PathVariable Long subtaskId,
            @RequestBody Map<String, Object> request) {
        String title = (String) request.get("title");
        Integer estimateMinutes = request.get("estimateMinutes") != null
                ? ((Number) request.get("estimateMinutes")).intValue() : null;
        ZonedDateTime deadline = request.get("deadline") != null
                ? ZonedDateTime.parse((String) request.get("deadline")) : null;
        String note = (String) request.get("note");
        Boolean isTarget = (Boolean) request.get("isTarget");
        Integer estimateTarget = request.get("estimateTarget") != null
                ? ((Number) request.get("estimateTarget")).intValue() : null;
        Integer target = request.get("target") != null
                ? ((Number) request.get("target")).intValue() : null;

        Subtask subtask = subtaskService.updateSubtask(subtaskId, title, estimateMinutes, deadline, note,
                isTarget, estimateTarget, target);
        return ok(ApiResponse.success("Subtask updated", subtaskMapper.toDTO(subtask)));
    }

    @PutMapping("/{subtaskId}/status")
    @Operation(summary = "Tick a FAST subtask's status manually", operationId = "updateSubtaskStatus")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<SubtaskResponse>> updateStatus(
            @PathVariable Long taskId,
            @PathVariable Long subtaskId,
            @RequestBody Map<String, Object> request) {
        StepStatus status = StepStatus.valueOf((String) request.get("status"));
        Subtask subtask = subtaskService.updateSubtaskStatus(subtaskId, status);
        return ok(ApiResponse.success("Subtask status updated", subtaskMapper.toDTO(subtask)));
    }

    @DeleteMapping("/{subtaskId}")
    @Operation(summary = "Delete a subtask (cascades to its steps, if any)", operationId = "deleteSubtask")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> deleteSubtask(@PathVariable Long taskId, @PathVariable Long subtaskId) {
        subtaskService.deleteSubtask(subtaskId);
        return ok(ApiResponse.success("Subtask deleted"));
    }
}
