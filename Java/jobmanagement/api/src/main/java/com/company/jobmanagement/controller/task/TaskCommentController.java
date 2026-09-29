package com.company.jobmanagement.controller.task;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.TaskCommentResponse;
import com.company.jobmanagement.model.entity.TaskComment;
import com.company.jobmanagement.service.TaskCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
@RequiredArgsConstructor
@Tag(name = "Task Comments", description = "Discussion thread on a task")
public class TaskCommentController extends BaseController {

    private final TaskCommentService taskCommentService;

    @GetMapping
    @Operation(summary = "List task comments", operationId = "getTaskComments")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<TaskCommentResponse>>> getComments(@PathVariable Long taskId) {
        List<TaskCommentResponse> responses = taskCommentService.getComments(taskId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(responses));
    }

    @PostMapping
    @Operation(summary = "Add a comment to a task", operationId = "addTaskComment")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<TaskCommentResponse>> addComment(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> request) {
        TaskComment comment = taskCommentService.addComment(taskId, request.get("content"));
        return created(ApiResponse.success("Comment added", toDTO(comment)));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a task comment", operationId = "deleteTaskComment")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> deleteComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId) {
        taskCommentService.deleteComment(taskId, commentId);
        return ok(ApiResponse.success("Comment deleted"));
    }

    private TaskCommentResponse toDTO(TaskComment comment) {
        return TaskCommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUser().getId())
                .userFullName(comment.getUser().getFullName())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
