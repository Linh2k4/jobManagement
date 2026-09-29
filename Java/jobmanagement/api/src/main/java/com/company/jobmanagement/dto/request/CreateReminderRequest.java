package com.company.jobmanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReminderRequest {

    @NotNull(message = "Title is required")
    @Schema(description = "Reminder title", example = "Review task #123")
    private String title;

    @Schema(description = "Reminder message")
    private String message;

    @NotNull(message = "Remind at time is required")
    @Schema(description = "When to remind (ISO 8601)", example = "2026-06-30T10:00:00+07:00")
    private ZonedDateTime remindAt;

    @Schema(description = "Associated task ID (optional)")
    private Long taskId;

    @Schema(description = "Is recurring reminder", example = "false")
    private Boolean isRecurring;

    @Schema(description = "Recurrence pattern (ONCE, DAILY, WEEKLY, MONTHLY)", example = "ONCE")
    private String recurrencePattern;
}
