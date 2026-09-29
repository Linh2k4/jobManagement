package com.company.jobmanagement.dto.response;

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
@Schema(description = "Deadline extension request (Scope.md §12)")
public class DeadlineExtensionResponse {

    private Long id;
    private Long taskId;
    private String taskTitle;
    private UserInfoResponse requestedBy;
    private LocalDate currentDeadline;
    private LocalDate requestedDeadline;
    private String reason;
    private String status;
    private UserInfoResponse reviewedBy;
    private ZonedDateTime reviewedAt;
    private String reviewNote;
    private Integer extensionNumber;
    private ZonedDateTime createdAt;
    private ZonedDateTime expiresAt;
}
