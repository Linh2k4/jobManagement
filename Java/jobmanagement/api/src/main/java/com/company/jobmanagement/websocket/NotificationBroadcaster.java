package com.company.jobmanagement.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast notification to specific user via WebSocket.
     */
    public void notifyUser(Long userId, String type, String title, String message) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("title", title);
            notification.put("message", message);
            notification.put("timestamp", ZonedDateTime.now());

            // Send to user-specific queue
            String destination = "/queue/user-" + userId;
            messagingTemplate.convertAndSend(destination, notification);

            log.debug("Notification sent to user {}: {}", userId, title);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to user {}", userId, e);
        }
    }

    /**
     * Broadcast notification to all users in a team/group.
     */
    public void broadcastToTeam(Long leadId, String type, String title, String message) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("title", title);
            notification.put("message", message);
            notification.put("timestamp", ZonedDateTime.now());

            // Send to team topic
            String destination = "/topic/team-" + leadId;
            messagingTemplate.convertAndSend(destination, notification);

            log.debug("Notification broadcasted to team {}: {}", leadId, title);
        } catch (Exception e) {
            log.error("Failed to broadcast WebSocket notification to team {}", leadId, e);
        }
    }

    /**
     * Broadcast system-wide notification (all users).
     */
    public void broadcastSystem(String type, String title, String message) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("title", title);
            notification.put("message", message);
            notification.put("timestamp", ZonedDateTime.now());

            // Send to system topic (all subscribers)
            messagingTemplate.convertAndSend("/topic/system", notification);

            log.debug("System notification broadcasted: {}", title);
        } catch (Exception e) {
            log.error("Failed to broadcast system WebSocket notification", e);
        }
    }

    /**
     * Send real-time task update.
     */
    public void notifyTaskUpdate(Long taskId, Long userId, Map<String, Object> updates) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("event", "task-updated");
            message.put("taskId", taskId);
            message.put("updates", updates);
            message.put("timestamp", ZonedDateTime.now());

            messagingTemplate.convertAndSend("/queue/user-" + userId, message);
            log.debug("Task update notification sent: taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            log.error("Failed to send task update notification", e);
        }
    }
}
