package com.company.jobmanagement.controller.leave;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.LeaveRequestResponse;
import com.company.jobmanagement.dto.response.UserInfoResponse;
import com.company.jobmanagement.model.entity.LeaveRequest;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/members/{id}/leave-requests")
@RequiredArgsConstructor
@Tag(name = "Leave & Handover", description = "A member's own leave request history")
public class MemberLeaveRequestController extends BaseController {

    private final LeaveRequestService leaveRequestService;

    @GetMapping
    @Operation(summary = "List a member's leave requests for a year", operationId = "getMemberLeaveRequests")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getForMember(
            @PathVariable Long id,
            @RequestParam(required = false) Integer year) {
        int resolvedYear = year != null ? year : Year.now().getValue();
        List<LeaveRequestResponse> responses = leaveRequestService.getLeaveRequestsForMember(id, resolvedYear).stream()
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

    private UserInfoResponse toUserInfo(User u) {
        return UserInfoResponse.builder()
                .id(u.getId()).email(u.getEmail()).fullName(u.getFullName()).role(u.getRole().name())
                .build();
    }
}
