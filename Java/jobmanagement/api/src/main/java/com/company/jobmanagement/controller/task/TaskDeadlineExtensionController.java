package com.company.jobmanagement.controller.task;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.DeadlineExtensionResponse;
import com.company.jobmanagement.dto.response.UserInfoResponse;
import com.company.jobmanagement.model.entity.DeadlineExtensionRequest;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.service.DeadlineExtensionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/deadline-extensions")
@RequiredArgsConstructor
@Tag(name = "Deadline Extensions", description = "Request & approval flow for pushing a task's deadline back (Scope.md §12)")
public class TaskDeadlineExtensionController extends BaseController {

    private final DeadlineExtensionService deadlineExtensionService;

    @PostMapping
    @Operation(summary = "Request a deadline extension", operationId = "requestDeadlineExtension")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<DeadlineExtensionResponse>> requestExtension(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> body) {
        LocalDate requestedDeadline = LocalDate.parse((String) body.get("requestedDeadline"));
        String reason = (String) body.get("reason");
        DeadlineExtensionRequest request = deadlineExtensionService.requestExtension(taskId, requestedDeadline, reason);
        return created(ApiResponse.success("Yêu cầu gia hạn đã được gửi", toDTO(request)));
    }

    @GetMapping
    @Operation(summary = "Deadline extension history for a task", operationId = "getDeadlineExtensionHistory")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<DeadlineExtensionResponse>>> getHistory(@PathVariable Long taskId) {
        List<DeadlineExtensionResponse> responses = deadlineExtensionService.getHistory(taskId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(responses));
    }

    @GetMapping("/pending")
    @Operation(summary = "Current pending extension request for a task, if any", operationId = "getPendingDeadlineExtension")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<DeadlineExtensionResponse>> getPending(@PathVariable Long taskId) {
        Optional<DeadlineExtensionRequest> pending = deadlineExtensionService.getHistory(taskId).stream()
                .filter(DeadlineExtensionRequest::isPending)
                .findFirst();
        return ok(ApiResponse.success(pending.map(this::toDTO).orElse(null)));
    }

    private DeadlineExtensionResponse toDTO(DeadlineExtensionRequest r) {
        return DeadlineExtensionResponse.builder()
                .id(r.getId())
                .taskId(r.getTask().getId())
                .taskTitle(r.getTask().getTitle())
                .requestedBy(toUserInfo(r.getRequestedBy()))
                .currentDeadline(r.getCurrentDeadline())
                .requestedDeadline(r.getRequestedDeadline())
                .reason(r.getReason())
                .status(r.getStatus().name())
                .reviewedBy(r.getReviewedBy() != null ? toUserInfo(r.getReviewedBy()) : null)
                .reviewedAt(r.getReviewedAt())
                .reviewNote(r.getReviewNote())
                .extensionNumber(r.getExtensionNumber())
                .createdAt(r.getCreatedAt())
                .expiresAt(r.getExpiresAt())
                .build();
    }

    private UserInfoResponse toUserInfo(User u) {
        return UserInfoResponse.builder()
                .id(u.getId()).email(u.getEmail()).fullName(u.getFullName()).role(u.getRole().name())
                .build();
    }
}
