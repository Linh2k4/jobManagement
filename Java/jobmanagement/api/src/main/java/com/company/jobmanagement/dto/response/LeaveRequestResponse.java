package com.company.jobmanagement.dto.response;

import com.company.jobmanagement.model.entity.LeaveRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "LeaveRequest", description = "Leave request & handover status (Scope.md §14)")
public class LeaveRequestResponse {

    private Long id;
    private UserInfoResponse member;
    private LeaveRequest.LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private LeaveRequest.Status status;
    private UserInfoResponse reviewedBy;
    private ZonedDateTime reviewedAt;
    private String reviewNote;
    private LeaveRequest.HandoverStatus handoverStatus;
    private ZonedDateTime createdAt;
}
