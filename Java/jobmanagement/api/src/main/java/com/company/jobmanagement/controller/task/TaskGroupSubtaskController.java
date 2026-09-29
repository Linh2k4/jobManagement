package com.company.jobmanagement.controller.task;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.GroupSubtaskResponse;
import com.company.jobmanagement.mapper.GroupSubtaskMapper;
import com.company.jobmanagement.model.entity.GroupSubtask;
import com.company.jobmanagement.service.GroupSubtaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Optional group headers under a task's subtasks (Scope.md §2.1/§2.2).
 * Creator or Manager only for mutations.
 */
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/group-subtasks")
@RequiredArgsConstructor
@Tag(name = "Group Subtasks", description = "Optional group headers for a task's subtasks")
public class TaskGroupSubtaskController extends BaseController {

    private final GroupSubtaskService groupSubtaskService;
    private final GroupSubtaskMapper groupSubtaskMapper;

    @GetMapping
    @Operation(summary = "List group subtasks", operationId = "getGroupSubtasks")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<GroupSubtaskResponse>>> getGroups(@PathVariable Long taskId) {
        List<GroupSubtaskResponse> responses = groupSubtaskService.getGroupsForTask(taskId).stream()
                .map(groupSubtaskMapper::toDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(responses));
    }

    @PostMapping
    @Operation(summary = "Add a group to a task", operationId = "createGroupSubtask")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<GroupSubtaskResponse>> createGroup(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        GroupSubtask group = groupSubtaskService.createGroup(taskId, name);
        return created(ApiResponse.success("Group added", groupSubtaskMapper.toDTO(group)));
    }

    @PutMapping("/{groupId}")
    @Operation(summary = "Rename a group", operationId = "updateGroupSubtask")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<GroupSubtaskResponse>> updateGroup(
            @PathVariable Long taskId,
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        GroupSubtask group = groupSubtaskService.updateGroup(groupId, name);
        return ok(ApiResponse.success("Group updated", groupSubtaskMapper.toDTO(group)));
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "Delete a group (cascades to its subtasks and steps)", operationId = "deleteGroupSubtask")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> deleteGroup(@PathVariable Long taskId, @PathVariable Long groupId) {
        groupSubtaskService.deleteGroup(groupId);
        return ok(ApiResponse.success("Group deleted"));
    }
}
