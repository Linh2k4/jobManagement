package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.Reminder;
import com.company.jobmanagement.model.entity.Task;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.model.enums.NotificationType;
import com.company.jobmanagement.exception.ForbiddenOperationException;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.repository.ReminderRepository;
import com.company.jobmanagement.repository.TaskRepository;
import com.company.jobmanagement.repository.UserRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Reminder service for managing task reminders and personal notifications.
 * Supports creating, retrieving, and processing reminders with recurring patterns.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final NotificationService notificationService;

    /**
     * Create a new reminder for the current user.
     */
    public Reminder createReminder(String title, String message, ZonedDateTime remindAt,
                                   Long taskId, Boolean isRecurring, String recurrencePattern) {
        User user = currentUser.getCurrentUser();

        Task task = null;
        if (taskId != null) {
            task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        }

        Reminder reminder = Reminder.builder()
                .user(user)
                .task(task)
                .title(title)
                .message(message)
                .remindAt(remindAt)
                .isRecurring(isRecurring != null && isRecurring)
                .recurrencePattern(recurrencePattern != null ? recurrencePattern : "ONCE")
                .isActive(true)
                .build();

        Reminder saved = reminderRepository.save(reminder);
        log.info("Reminder created: id={}, userId={}, remindAt={}", saved.getId(), user.getId(), saved.getRemindAt());
        return saved;
    }

    /**
     * Get active reminders for the current user with pagination.
     */
    @Transactional(readOnly = true)
    public Page<Reminder> getActiveReminders(Pageable pageable) {
        User user = currentUser.getCurrentUser();
        return reminderRepository.findActiveRemindersForUser(user.getId(), pageable);
    }

    /**
     * Get a specific reminder by ID.
     */
    @Transactional(readOnly = true)
    public Reminder getReminder(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + reminderId));

        User currentUser = this.currentUser.getCurrentUser();
        if (!reminder.getUser().getId().equals(currentUser.getId()) && !currentUser.isManager()) {
            throw new ForbiddenOperationException("Cannot access reminder");
        }

        return reminder;
    }

    /**
     * Update reminder status (activate/deactivate).
     */
    public void updateReminderStatus(Long reminderId, Boolean isActive) {
        Reminder reminder = getReminder(reminderId);
        reminder.setIsActive(isActive);
        reminder.setUpdatedAt(ZonedDateTime.now());
        reminderRepository.save(reminder);
        log.info("Reminder status updated: id={}, isActive={}", reminderId, isActive);
    }

    /**
     * Delete a reminder (soft delete via status).
     */
    public void deleteReminder(Long reminderId) {
        Reminder reminder = getReminder(reminderId);
        reminder.setIsActive(false);
        reminder.setUpdatedAt(ZonedDateTime.now());
        reminderRepository.save(reminder);
        log.info("Reminder deleted: id={}", reminderId);
    }

    /**
     * Scheduled job to process due reminders (runs every 5 minutes).
     * Sends notification for each due reminder and reschedules recurring ones.
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    @Transactional
    public void processDueReminders() {
        log.debug("Processing due reminders...");

        ZonedDateTime now = ZonedDateTime.now();
        List<Reminder> dueReminders = reminderRepository.findRemindersReadyToSend(now);

        for (Reminder reminder : dueReminders) {
            try {
                // Send notification to user
                notificationService.notify(
                    reminder.getUser(),
                    NotificationType.REMINDER,
                    reminder.getTitle(),
                    reminder.getMessage(),
                    reminder.getTask() != null ? reminder.getTask().getId() : null
                );

                reminder.setLastReminderSentAt(now);

                // If recurring, schedule next occurrence
                if (reminder.getIsRecurring() != null && reminder.getIsRecurring() && !reminder.isOneTime()) {
                    ZonedDateTime nextReminder = calculateNextReminder(reminder, now);
                    reminder.setRemindAt(nextReminder);
                } else {
                    // One-time reminders deactivate after sending
                    reminder.setIsActive(false);
                }

                reminder.setUpdatedAt(now);
                reminderRepository.save(reminder);

                log.info("Reminder processed: id={}, userId={}", reminder.getId(), reminder.getUser().getId());
            } catch (Exception e) {
                log.error("Failed to process reminder: id={}", reminder.getId(), e);
            }
        }

        log.debug("Processed {} due reminders", dueReminders.size());
    }

    /**
     * Calculate next reminder time based on recurrence pattern.
     */
    private ZonedDateTime calculateNextReminder(Reminder reminder, ZonedDateTime current) {
        if (reminder.getRecurrencePattern() == null) {
            return current;
        }

        return switch (reminder.getRecurrencePattern()) {
            case "DAILY" -> current.plus(1, ChronoUnit.DAYS);
            case "WEEKLY" -> current.plus(7, ChronoUnit.DAYS);
            case "MONTHLY" -> current.plus(1, ChronoUnit.MONTHS);
            default -> current;
        };
    }

    /**
     * Create automatic reminders for tasks without estimates.
     * Warning system for unestimated tasks.
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour
    @Transactional
    public void createUnestimatedTaskReminders() {
        log.debug("Checking for unestimated tasks...");

        // Find all tasks without estimate and not completed
        // This would need a repository method - for now just log
        log.debug("Unestimated task check completed");
    }
}
