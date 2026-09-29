package com.company.jobmanagement.controller.task;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.service.TaskPrivateNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/private-notes/me")
@RequiredArgsConstructor
@Tag(name = "Task Private Notes", description = "Per-user private scratch notes on a task, not visible to other users")
public class TaskPrivateNoteController extends BaseController {

    private final TaskPrivateNoteService taskPrivateNoteService;

    @GetMapping
    @Operation(summary = "Get my private notes for a task", operationId = "getMyTaskPrivateNotes")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<String>> getMyNotes(@PathVariable Long taskId) {
        String notes = taskPrivateNoteService.getMyNotes(taskId);
        // ApiResponse.success(String) is ambiguous between the generic
        // success(T data) and the message-only success(String) overload —
        // javac silently picks the latter, dropping `notes` into `message`
        // instead of `data`. Build explicitly to avoid that.
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Success")
                .data(notes)
                .build();
        return ok(response);
    }

    @PutMapping
    @Operation(summary = "Update my private notes for a task", operationId = "updateMyTaskPrivateNotes")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> updateMyNotes(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> request) {
        taskPrivateNoteService.updateMyNotes(taskId, request.get("notes"));
        return ok(ApiResponse.success("Notes updated"));
    }
}
