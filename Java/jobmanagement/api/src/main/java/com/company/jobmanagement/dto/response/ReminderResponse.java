package com.company.jobmanagement.dto.response;

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
@Schema(name = "Reminder", description = "Reminder information")
public class ReminderResponse {

    @Schema(description = "Reminder ID", example = "1")
    private Long id;

    @Schema(description = "Reminder title", example = "Review task")
    private String title;

    @Schema(description = "Reminder message", example = "Don't forget to review the task")
    private String message;

    @Schema(description = "When to send the reminder", example = "2026-06-30T14:00:00+07:00")
    private ZonedDateTime remindAt;

    @Schema(description = "Whether reminder is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Whether reminder is recurring", example = "false")
    private Boolean isRecurring;

    @Schema(description = "Recurrence pattern", example = "DAILY", allowableValues = {"ONCE", "DAILY", "WEEKLY", "MONTHLY"})
    private String recurrencePattern;

    @Schema(description = "Associated task ID (optional)", example = "5")
    private Long taskId;

    @Schema(description = "When reminder was last sent", example = "2026-06-29T14:00:00+07:00")
    private ZonedDateTime lastReminderSentAt;

    @Schema(description = "When reminder was created", example = "2026-06-20T10:30:00+07:00")
    private ZonedDateTime createdAt;

    @Schema(description = "When reminder was last updated", example = "2026-06-25T10:30:00+07:00")
    private ZonedDateTime updatedAt;
}
