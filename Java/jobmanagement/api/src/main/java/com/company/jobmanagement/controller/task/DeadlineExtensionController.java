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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/deadline-extensions")
@RequiredArgsConstructor
@Tag(name = "Deadline Extensions", description = "Review actions (Scope.md §12)")
public class DeadlineExtensionController extends BaseController {

    private final DeadlineExtensionService deadlineExtensionService;

    @GetMapping("/pending")
    @Operation(summary = "Pending extension requests awaiting my review", description = "Lead: own team; Manager: whole company", operationId = "getMyPendingDeadlineExtensions")
    @PreAuthorize("hasAnyRole('LEAD', 'MANAGER')")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<DeadlineExtensionResponse>>> getPendingForMe() {
        List<DeadlineExtensionResponse> responses = deadlineExtensionService.getPendingForCurrentUser().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a deadline extension request", operationId = "approveDeadlineExtension")
    @PreAuthorize("hasAnyRole('LEAD', 'MANAGER')")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<DeadlineExtensionResponse>> approve(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        DeadlineExtensionRequest request = deadlineExtensionService.approve(id, note);
        return ok(ApiResponse.success("Đã duyệt yêu cầu gia hạn", toDTO(request)));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a deadline extension request", operationId = "rejectDeadlineExtension")
    @PreAuthorize("hasAnyRole('LEAD', 'MANAGER')")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<DeadlineExtensionResponse>> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        DeadlineExtensionRequest request = deadlineExtensionService.reject(id, body.get("note"));
        return ok(ApiResponse.success("Đã từ chối yêu cầu gia hạn", toDTO(request)));
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
