package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private static final String FROM_EMAIL = "noreply@jobmanagement.com";

    /**
     * Send async email notification.
     */
    @Async
    public void sendNotification(User recipient, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(recipient.getEmail());
            message.setSubject(subject);
            message.setText(buildEmailBody(recipient.getFullName(), body));

            mailSender.send(message);
            log.info("Email notification sent to {}: {}", recipient.getEmail(), subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}", recipient.getEmail(), e);
        }
    }

    /**
     * Send task deadline reminder.
     */
    @Async
    public void sendTaskDeadlineReminder(User user, String taskTitle, String dueDate) {
        String subject = "Task Deadline Reminder: " + taskTitle;
        String body = String.format(
            "Task \"%s\" is due on %s. Please complete it on time.",
            taskTitle, dueDate
        );
        sendNotification(user, subject, body);
    }

    /**
     * Send overdue task alert.
     */
    @Async
    public void sendOverdueTaskAlert(User user, String taskTitle) {
        String subject = "⚠️ OVERDUE TASK: " + taskTitle;
        String body = String.format(
            "Task \"%s\" is now OVERDUE! Please complete it immediately.",
            taskTitle
        );
        sendNotification(user, subject, body);
    }

    /**
     * Send evaluation deadline reminder.
     */
    @Async
    public void sendEvaluationReminder(User user, String deadline) {
        String subject = "Evaluation Submission Deadline Reminder";
        String body = String.format(
            "Your evaluation is due by %s. Please submit your self-evaluation before the deadline.",
            deadline
        );
        sendNotification(user, subject, body);
    }

    private String buildEmailBody(String userName, String message) {
        return String.format(
            "Hello %s,\n\n%s\n\nBest regards,\nJob Management System",
            userName, message
        );
    }
}
