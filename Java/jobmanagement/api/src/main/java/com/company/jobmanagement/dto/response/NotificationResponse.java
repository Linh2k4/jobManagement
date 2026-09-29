package com.company.jobmanagement.dto.response;

import com.company.jobmanagement.model.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    @Schema(description = "Notification ID", example = "1")
    private Long id;

    @Schema(description = "Notification type", example = "REMINDER")
    private NotificationType type;

    @Schema(description = "Notification title", example = "Task assigned")
    private String title;

    @Schema(description = "Notification message")
    private String message;

    @Schema(description = "Related task ID")
    private Long relatedTaskId;

    @Schema(description = "Reminder time")
    private ZonedDateTime remindAt;

    @Schema(description = "Read status", example = "false")
    private Boolean isRead;

    @Schema(description = "Created timestamp")
    private ZonedDateTime createdAt;
}
