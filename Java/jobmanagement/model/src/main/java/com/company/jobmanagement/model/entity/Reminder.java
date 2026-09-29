package com.company.jobmanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * Reminder entity for task reminders and personal notifications.
 * Supports one-time and recurring reminders with time-based triggers.
 */
@Entity
@Table(name = "reminders", indexes = {
    @Index(name = "idx_reminders_user_id", columnList = "user_id"),
    @Index(name = "idx_reminders_task_id", columnList = "task_id"),
    @Index(name = "idx_reminders_remind_at", columnList = "remind_at"),
    @Index(name = "idx_reminders_is_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private ZonedDateTime remindAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column
    @Builder.Default
    private Boolean isRecurring = false;

    @Column(length = 50)
    private String recurrencePattern; // ONCE, DAILY, WEEKLY, MONTHLY

    @Column
    private ZonedDateTime lastReminderSentAt;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    public boolean isReadyToRemind() {
        return isActive && ZonedDateTime.now().isAfter(remindAt);
    }

    public boolean isOneTime() {
        return !isRecurring || "ONCE".equals(recurrencePattern);
    }
}
