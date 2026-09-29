package com.company.jobmanagement.controller.reminder;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.request.CreateReminderRequest;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.ReminderResponse;
import com.company.jobmanagement.model.entity.Reminder;
import com.company.jobmanagement.mapper.ReminderMapper;
import com.company.jobmanagement.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Reminder management controller for task reminders and personal notifications.
 * <p>
 * Provides endpoints for:
 * - Create task reminders
 * - List active reminders
 * - Update reminder status
 * - Delete reminders
 * </p>
 *
 * @author Khánh VD
 * @since 2026-06-29
 */
@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
@Tag(
    name = "Reminders",
    description = "Task reminders and personal notifications\n\n" +
        "## Reminder Types\n" +
        "- **Task Reminders**: Automatic reminders for task due dates\n" +
        "- **Personal Reminders**: User-created reminders for any task\n" +
        "- **Recurring Reminders**: Support DAILY, WEEKLY, MONTHLY patterns\n\n" +
        "## Status\n" +
        "- Active: Reminder is scheduled to trigger\n" +
        "- Inactive: Reminder has been sent or cancelled"
)
public class ReminderController extends BaseController {

    private final ReminderService reminderService;
    private final ReminderMapper reminderMapper;

    /**
     * Create a new reminder.
     * <p>
     * Can be linked to a task (optional) or be a standalone reminder.
     * </p>
     *
     * @param request reminder details
     * @return 201 CREATED with ReminderResponse
     */
    @PostMapping
    @Operation(
        summary = "Create reminder",
        description = "Create a new reminder for the authenticated user. Can be one-time or recurring.",
        operationId = "createReminder"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Reminder created successfully",
            content = @Content(schema = @Schema(implementation = ReminderResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input or missing required fields"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Associated task not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<ReminderResponse>> createReminder(
            @Valid @RequestBody CreateReminderRequest request) {
        Reminder reminder = reminderService.createReminder(
                request.getTitle(),
                request.getMessage(),
                request.getRemindAt(),
                request.getTaskId(),
                request.getIsRecurring(),
                request.getRecurrencePattern()
        );
        ReminderResponse response = reminderMapper.toDTO(reminder);
        return created(ApiResponse.success("Reminder created successfully", response));
    }

    /**
     * Get active reminders for the current user.
     * <p>
     * Returns reminders ordered by remind time (nearest first).
     * </p>
     *
     * @param pageable pagination info
     * @return Page of ReminderResponse
     */
    @GetMapping
    @Operation(
        summary = "List active reminders",
        description = "Retrieve all active reminders for authenticated user, sorted by reminder time",
        operationId = "getActiveReminders"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Reminders retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Page<ReminderResponse>>> getActiveReminders(Pageable pageable) {
        Page<Reminder> reminders = reminderService.getActiveReminders(pageable);
        Page<ReminderResponse> responses = reminders.map(reminderMapper::toDTO);
        return ok(ApiResponse.success(responses));
    }

    /**
     * Get a specific reminder by ID.
     *
     * @param reminderId reminder identifier
     * @return ReminderResponse
     */
    @GetMapping("/{reminderId}")
    @Operation(
        summary = "Get reminder details",
        description = "Retrieve details for a specific reminder",
        operationId = "getReminder"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Reminder retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - not reminder owner"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Reminder not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<ReminderResponse>> getReminder(
            @PathVariable Long reminderId) {
        Reminder reminder = reminderService.getReminder(reminderId);
        ReminderResponse response = reminderMapper.toDTO(reminder);
        return ok(ApiResponse.success(response));
    }

    /**
     * Update reminder status (activate/deactivate).
     *
     * @param reminderId reminder identifier
     * @param isActive new status
     * @return 204 No Content
     */
    @PutMapping("/{reminderId}/status")
    @Operation(
        summary = "Update reminder status",
        description = "Activate or deactivate a reminder",
        operationId = "updateReminderStatus"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "Reminder status updated successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - not reminder owner"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Reminder not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<Void> updateReminderStatus(
            @PathVariable Long reminderId,
            @RequestParam Boolean isActive) {
        reminderService.updateReminderStatus(reminderId, isActive);
        return noContent();
    }

    /**
     * Delete a reminder (deactivate).
     *
     * @param reminderId reminder identifier
     * @return 204 No Content
     */
    @DeleteMapping("/{reminderId}")
    @Operation(
        summary = "Delete reminder",
        description = "Delete a reminder (sets status to inactive)",
        operationId = "deleteReminder"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "Reminder deleted successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - not reminder owner"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Reminder not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<Void> deleteReminder(@PathVariable Long reminderId) {
        reminderService.deleteReminder(reminderId);
        return noContent();
    }
}
