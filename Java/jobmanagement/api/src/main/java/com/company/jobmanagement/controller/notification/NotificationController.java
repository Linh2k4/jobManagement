package com.company.jobmanagement.controller.notification;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.model.entity.Notification;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.NotificationResponse;
import com.company.jobmanagement.security.CurrentUser;
import com.company.jobmanagement.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Notification management controller handling user notifications.
 * <p>
 * Provides endpoints for:
 * - Get user's notifications
 * - Get unread notification count
 * - Mark notification as read / mark all as read
 * - Delete notification
 * </p>
 *
 * @author Khánh VD
 * @since 2026-06-29
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(
    name = "Notifications",
    description = "User notification management\n\n" +
        "## Notification Types\n" +
        "- **TASK_ASSIGNED**: Task assigned to user\n" +
        "- **EVALUATION_REQUEST**: Evaluation requested\n" +
        "- **REMINDER**: Task reminder\n" +
        "- **SYSTEM**: System messages"
)
public class NotificationController extends BaseController {

    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    @GetMapping
    @Operation(
        summary = "Get user notifications",
        description = "Retrieve all notifications for authenticated user",
        operationId = "getUserNotifications"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Notifications retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications() {
        List<NotificationResponse> notifications = notificationService.getNotificationsForCurrentUser()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread-count")
    @Operation(
        summary = "Get unread notification count",
        description = "Count of unread notifications for authenticated user",
        operationId = "getUnreadNotificationCount"
    )
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getUnreadCount() {
        int count = notificationService.getUnreadForCurrentUser().size();
        return ok(ApiResponse.success(Map.of("count", count)));
    }

    @PatchMapping("/{id}/read")
    @Operation(
        summary = "Mark notification as read",
        operationId = "markNotificationAsRead"
    )
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Long id) {
        Notification notification = notificationService.markAsRead(id);
        return ok(ApiResponse.success(mapToResponse(notification)));
    }

    @PostMapping("/mark-all-read")
    @Operation(
        summary = "Mark all notifications as read",
        operationId = "markAllNotificationsAsRead"
    )
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead() {
        int updated = notificationService.markAllAsReadForCurrentUser();
        return ok(ApiResponse.success(Map.of("updated", updated)));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete notification",
        operationId = "deleteNotification"
    )
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteOwn(id);
        return noContent();
    }

    /**
     * Map Notification entity to NotificationResponse DTO.
     */
    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedTaskId(notification.getRelatedTaskId())
                .remindAt(notification.getRemindAt())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
