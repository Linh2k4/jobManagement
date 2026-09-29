package com.company.jobmanagement.controller.leave;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.HandoverSuggestionResponse;
import com.company.jobmanagement.dto.response.LeaveRequestResponse;
import com.company.jobmanagement.dto.response.UserInfoResponse;
import com.company.jobmanagement.model.entity.HandoverSuggestion;
import com.company.jobmanagement.model.entity.LeaveRequest;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Leave request & handover workflow (Scope.md §14). "Group" in the spec's
 * endpoint list is the requester's Lead team, since a Group entity doesn't
 * exist yet (same gap noted on V17/V19) — {@code GET /pending} scopes by
 * the caller's role instead of a separate {@code /groups/{leadId}/...} path.
 */
@RestController
@RequestMapping("/api/v1/leave-requests")
@RequiredArgsConstructor
@Tag(name = "Leave & Handover", description = "Leave requests and task handover")
public class LeaveRequestController extends BaseController {

    private final LeaveRequestService leaveRequestService;

    @PostMapping
    @Operation(summary = "Submit a leave request", operationId = "createLeaveRequest")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> create(@RequestBody Map<String, Object> body) {
        LeaveRequest.LeaveType leaveType = LeaveRequest.LeaveType.valueOf((String) body.get("leaveType"));
        LocalDate startDate = LocalDate.parse((String) body.get("startDate"));
        LocalDate endDate = LocalDate.parse((String) body.get("endDate"));
        String reason = (String) body.get("reason");
        LeaveRequest request = leaveRequestService.createLeaveRequest(leaveType, startDate, endDate, reason);
        return created(ApiResponse.success("Đã gửi yêu cầu nghỉ phép", toDTO(request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LEAD','MANAGER')")
    @Operation(summary = "Approve or reject a leave request", operationId = "reviewLeaveRequest")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> review(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approve = "APPROVED".equalsIgnoreCase((String) body.get("status"));
        String note = (String) body.get("note");
        LeaveRequest request = leaveRequestService.reviewLeaveRequest(id, approve, note);
        return ok(ApiResponse.success("Đã xử lý yêu cầu nghỉ phép", toDTO(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a leave request by id", operationId = "getLeaveRequest")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> getById(@PathVariable Long id) {
        return ok(ApiResponse.success(toDTO(leaveRequestService.getLeaveRequestById(id))));
    }

    @GetMapping("/{id}/handover-suggestions")
    @Operation(summary = "List handover suggestions generated for an approved leave request", operationId = "getHandoverSuggestions")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<HandoverSuggestionResponse>>> getSuggestions(@PathVariable Long id) {
        List<HandoverSuggestionResponse> responses = leaveRequestService.getHandoverSuggestions(id).stream()
                .map(this::toSuggestionDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/handover/{suggestionId}/confirm")
    @PreAuthorize("hasAnyRole('LEAD','MANAGER')")
    @Operation(summary = "Confirm one handover item (accept/custom-reassign/extend/skip)", operationId = "confirmHandover")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<HandoverSuggestionResponse>> confirmHandover(
            @PathVariable Long id,
            @PathVariable Long suggestionId,
            @RequestBody Map<String, Object> body) {
        HandoverSuggestion.Action action = HandoverSuggestion.Action.valueOf((String) body.get("action"));
        Long newAssigneeId = body.get("newAssigneeId") != null ? ((Number) body.get("newAssigneeId")).longValue() : null;
        HandoverSuggestion suggestion = leaveRequestService.confirmHandover(id, suggestionId, action, newAssigneeId);
        return ok(ApiResponse.success("Đã xác nhận bàn giao", toSuggestionDTO(suggestion)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('LEAD','MANAGER')")
    @Operation(summary = "Pending leave requests for the current Lead's team (or all, for Manager)", operationId = "getPendingLeaveRequests")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getPending() {
        List<LeaveRequestResponse> responses = leaveRequestService.getPendingForCurrentUser().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(responses));
    }

    private LeaveRequestResponse toDTO(LeaveRequest r) {
        return LeaveRequestResponse.builder()
                .id(r.getId())
                .member(toUserInfo(r.getMember()))
                .leaveType(r.getLeaveType())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .reason(r.getReason())
                .status(r.getStatus())
                .reviewedBy(r.getReviewedBy() != null ? toUserInfo(r.getReviewedBy()) : null)
                .reviewedAt(r.getReviewedAt())
                .reviewNote(r.getReviewNote())
                .handoverStatus(r.getHandoverStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private HandoverSuggestionResponse toSuggestionDTO(HandoverSuggestion s) {
        return HandoverSuggestionResponse.builder()
                .id(s.getId())
                .taskId(s.getTask().getId())
                .taskTitle(s.getTask().getTitle())
                .stepId(s.getStep() != null ? s.getStep().getId() : null)
                .stepName(s.getStep() != null ? s.getStep().getName() : null)
                .entityType(s.getEntityType())
                .currentDeadline(s.getCurrentDeadline())
                .suggestedAssignee(s.getSuggestedAssignee() != null ? toUserInfo(s.getSuggestedAssignee()) : null)
                .alternativeAssigneeIds(leaveRequestService.getAlternativeAssigneeIds(s, 3))
                .action(s.getAction())
                .confirmed(s.getConfirmed())
                .confirmedAssignee(s.getConfirmedAssignee() != null ? toUserInfo(s.getConfirmedAssignee()) : null)
                .confirmedAt(s.getConfirmedAt())
                .build();
    }

    private UserInfoResponse toUserInfo(User u) {
        return UserInfoResponse.builder()
                .id(u.getId()).email(u.getEmail()).fullName(u.getFullName()).role(u.getRole().name())
                .build();
    }
}
