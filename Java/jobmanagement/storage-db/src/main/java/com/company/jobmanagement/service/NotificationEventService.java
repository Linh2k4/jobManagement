package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.*;
import com.company.jobmanagement.model.enums.NotificationType;
import com.company.jobmanagement.model.enums.Priority;
import com.company.jobmanagement.model.enums.TaskStatus;
import com.company.jobmanagement.repository.TaskRepository;
import com.company.jobmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.company.jobmanagement.repository.EvaluationPeriodRepository;
import com.company.jobmanagement.repository.EvaluationRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Event-driven notification service that triggers notifications
 * when tasks undergo state changes or are assigned.
 *
 * Listens for task lifecycle events and sends appropriate notifications
 * asynchronously to avoid blocking task operations.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationEventService {

    private final NotificationService notificationService;
    private final EmailNotificationService emailNotificationService;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final EvaluationPeriodRepository evaluationPeriodRepository;
    private final EvaluationRepository evaluationRepository;
    private final PlatformTransactionManager transactionManager;

    /**
     * CompletableFuture.runAsync below runs on the common ForkJoinPool, a
     * thread Spring's @Transactional proxy never touches — so without this,
     * every lazy entity association (task.getCreatedBy(), assignee, etc.)
     * read inside those lambdas throws LazyInitializationException as soon
     * as real data makes the lazy field non-trivial to resolve. Opens a
     * real transaction/session on whichever thread runs the lambda.
     */
    private void runInTransaction(Runnable work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run());
    }

    /**
     * Send notification when a task is created.
     * Notify: task creator's manager (if applicable).
     */
    public void notifyTaskCreated(Long taskId) {
        CompletableFuture.runAsync(() -> runInTransaction(() -> {
            try {
                Task task = taskRepository.findById(taskId).orElse(null);
                if (task == null) return;

                User creator = task.getCreatedBy();
                String message = String.format(
                    "New task created: \"%s\" (%s)",
                    task.getTitle(),
                    task.getTaskType().getName()
                );

                if (creator.getManager() != null) {
                    notificationService.notify(
                        creator.getManager(),
                        NotificationType.SYSTEM,
                        "Task Created",
                        message,
                        taskId
                    );
                }

                log.info("Task created notification sent: taskId={}", taskId);
            } catch (Exception e) {
                log.error("Failed to send task created notification", e);
            }
        }));
    }

    /**
     * Send notification when a task is assigned.
     * Notify: assignee and task creator.
     */
    public void notifyTaskAssigned(Long taskId, Long assigneeId) {
        CompletableFuture.runAsync(() -> runInTransaction(() -> {
            try {
                Task task = taskRepository.findById(taskId).orElse(null);
                if (task == null) return;

                User assignee = userRepository.findById(assigneeId).orElse(null);
                if (assignee == null) return;

                String message = String.format(
                    "Task \"%s\" assigned to you",
                    task.getTitle()
                );

                // Notify assignee
                notificationService.notify(
                    assignee,
                    NotificationType.TASK_ASSIGNED,
                    "Task Assigned",
                    message,
                    taskId
                );

                // Notify creator
                User creator = task.getCreatedBy();
                String creatorMessage = String.format(
                    "Task \"%s\" assigned to %s",
                    task.getTitle(),
                    assignee.getFullName()
                );
                notificationService.notify(
                    creator,
                    NotificationType.TASK_ASSIGNED,
                    "Task Assignment",
                    creatorMessage,
                    taskId
                );

                log.info("Task assigned notification sent: taskId={}, assigneeId={}", taskId, assigneeId);
            } catch (Exception e) {
                log.error("Failed to send task assigned notification", e);
            }
        }));
    }

    /**
     * Send notification when task status changes.
     * Notify: task creator and current assignee.
     */
    public void notifyTaskStatusChanged(Long taskId, TaskStatus newStatus) {
        CompletableFuture.runAsync(() -> runInTransaction(() -> {
            try {
                Task task = taskRepository.findById(taskId).orElse(null);
                if (task == null) return;

                String statusText = newStatus.name().replace("_", " ");
                String message = String.format(
                    "Task \"%s\" status changed to: %s",
                    task.getTitle(),
                    statusText
                );

                NotificationType notifType = getNotificationTypeForStatus(newStatus);

                // Notify creator
                notificationService.notify(
                    task.getCreatedBy(),
                    notifType,
                    "Task Status Changed",
                    message,
                    taskId
                );

                // Notify current assignee
                task.getAssignments().stream()
                    .filter(TaskAssignment::getIsCurrent)
                    .map(TaskAssignment::getAssignee)
                    .forEach(assignee ->
                        notificationService.notify(
                            assignee,
                            notifType,
                            "Task Status Changed",
                            message,
                            taskId
                        )
                    );

                log.info("Task status changed notification sent: taskId={}, newStatus={}", taskId, newStatus);
            } catch (Exception e) {
                log.error("Failed to send task status changed notification", e);
            }
        }));
    }

    /**
     * Send notification when a task step is completed.
     * Notify: task assignee and task creator.
     */
    public void notifyStepCompleted(Long taskId, Long stepId, String stepName) {
        CompletableFuture.runAsync(() -> runInTransaction(() -> {
            try {
                Task task = taskRepository.findById(taskId).orElse(null);
                if (task == null) return;

                String message = String.format(
                    "Step \"%s\" completed for task \"%s\"",
                    stepName,
                    task.getTitle()
                );

                // Notify task assignee
                task.getAssignments().stream()
                    .filter(TaskAssignment::getIsCurrent)
                    .map(TaskAssignment::getAssignee)
                    .forEach(assignee ->
                        notificationService.notify(
                            assignee,
                            NotificationType.SYSTEM,
                            "Step Completed",
                            message,
                            taskId
                        )
                    );

                // Notify task creator
                notificationService.notify(
                    task.getCreatedBy(),
                    NotificationType.SYSTEM,
                    "Step Completed",
                    message,
                    taskId
                );

                log.info("Step completed notification sent: taskId={}, stepId={}", taskId, stepId);
            } catch (Exception e) {
                log.error("Failed to send step completed notification", e);
            }
        }));
    }

    /**
     * Map task status to appropriate notification type.
     */
    private NotificationType getNotificationTypeForStatus(TaskStatus status) {
        return switch (status) {
            case DONE -> NotificationType.TASK_DUE;
            case IN_PROGRESS -> NotificationType.SYSTEM;
            case CANCELLED -> NotificationType.SYSTEM;
            default -> NotificationType.SYSTEM;
        };
    }

    /**
     * Send overdue task alert.
     * Notify: task assignee and creator.
     */
    public void notifyTaskOverdue(Long taskId) {
        CompletableFuture.runAsync(() -> runInTransaction(() -> {
            try {
                Task task = taskRepository.findById(taskId).orElse(null);
                if (task == null || task.isDone() || task.isCancelled()) return;

                String message = String.format(
                    "Task \"%s\" is overdue (due: %s)",
                    task.getTitle(),
                    task.getDueDate()
                );

                // Notify assignee
                task.getAssignments().stream()
                    .filter(TaskAssignment::getIsCurrent)
                    .map(TaskAssignment::getAssignee)
                    .forEach(assignee ->
                        notificationService.notify(
                            assignee,
                            NotificationType.TASK_DUE,
                            "Task Overdue",
                            message,
                            taskId
                        )
                    );

                // Notify creator
                notificationService.notify(
                    task.getCreatedBy(),
                    NotificationType.TASK_DUE,
                    "Task Overdue Alert",
                    message,
                    taskId
                );

                log.info("Task overdue notification sent: taskId={}", taskId);
            } catch (Exception e) {
                log.error("Failed to send task overdue notification", e);
            }
        }));
    }

    /**
     * Scheduled job: Check for tasks due soon (24h or 2h before deadline).
     * Scope.md §4.1: Task sắp đến hạn (24h trước, hoặc 2h cho khẩn)
     * Runs every 30 minutes to catch upcoming deadlines.
     */
    @Scheduled(fixedDelay = 1800000) // Every 30 minutes
    @Transactional(readOnly = true)
    public void checkTasksNearDeadline() {
        log.debug("Checking for tasks near deadline...");

        try {
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);

            // Find tasks due tomorrow
            List<Task> tasksDueTomorrow = taskRepository.findTasksDueTomorrow(tomorrow);
            for (Task task : tasksDueTomorrow) {
                notifyTaskNearDeadline(task.getId(), "due tomorrow");
            }

            log.debug("Task deadline check completed: {} tasks found", tasksDueTomorrow.size());
        } catch (Exception e) {
            log.error("Error checking tasks near deadline", e);
        }
    }

    private void notifyTaskNearDeadline(Long taskId, String message) {
        CompletableFuture.runAsync(() -> runInTransaction(() -> {
            try {
                // Re-fetch inside this thread's own transaction — the Task
                // loaded by the caller's (already-closed) transaction can't
                // be reused to resolve lazy associations here.
                Task task = taskRepository.findById(taskId).orElse(null);
                if (task == null) return;

                // Notify assignees
                if (!task.getAssignments().isEmpty()) {
                    for (TaskAssignment assignment : task.getAssignments()) {
                        if (assignment.getIsCurrent() && assignment.getAssignee() != null) {
                            String notifMsg = String.format(
                                "Task \"%s\" is %s. Due date: %s",
                                task.getTitle(),
                                message,
                                task.getDueDate()
                            );

                            notificationService.notify(
                                assignment.getAssignee(),
                                NotificationType.TASK_DUE,
                                "Task Deadline Reminder",
                                notifMsg,
                                task.getId()
                            );
                        }
                    }
                }

                // Notify creator
                if (task.getCreatedBy() != null) {
                    String creatorMsg = String.format(
                        "Your task \"%s\" is %s",
                        task.getTitle(),
                        message
                    );

                    notificationService.notify(
                        task.getCreatedBy(),
                        NotificationType.TASK_DUE,
                        "Task Deadline Reminder",
                        creatorMsg,
                        task.getId()
                    );
                }

                log.info("Deadline reminder sent: taskId={}, message={}", task.getId(), message);
            } catch (Exception e) {
                log.error("Error notifying task deadline: taskId={}", taskId, e);
            }
        }));
    }

    /**
     * Scheduled job: Check for overdue tasks and send alerts.
     * Scope.md §4.1: Task quá hạn (ngay khi quá deadline)
     * Runs every hour to catch overdue tasks.
     */
    @Scheduled(fixedDelay = 3600000) // Every hour
    @Transactional(readOnly = true)
    public void checkOverdueTasks() {
        log.debug("Checking for overdue tasks...");

        try {
            // Query overdue tasks using repository method
            List<Task> overdueTasks = taskRepository.findOverdueTasksWithEagerLoad();

            for (Task task : overdueTasks) {
                notifyTaskOverdueAlert(task.getId());
            }

            log.debug("Overdue task check completed: {} tasks found", overdueTasks.size());
        } catch (Exception e) {
            log.error("Error checking overdue tasks", e);
        }
    }

    private void notifyTaskOverdueAlert(Long taskId) {
        CompletableFuture.runAsync(() -> runInTransaction(() -> {
            try {
                // Re-fetch inside this thread's own transaction — see
                // notifyTaskNearDeadline for why the caller's Task can't be reused.
                Task task = taskRepository.findById(taskId).orElse(null);
                if (task == null) return;

                // Notify assignees - urgent notification
                if (!task.getAssignments().isEmpty()) {
                    for (TaskAssignment assignment : task.getAssignments()) {
                        if (assignment.getIsCurrent() && assignment.getAssignee() != null) {
                            String message = String.format(
                                "🚨 URGENT: Task \"%s\" is NOW OVERDUE! Due date was: %s. " +
                                "Status: %s. Please complete immediately!",
                                task.getTitle(),
                                task.getDueDate(),
                                task.getStatus()
                            );

                            notificationService.notify(
                                assignment.getAssignee(),
                                NotificationType.REMINDER,
                                "⚠️ OVERDUE TASK ALERT",
                                message,
                                task.getId()
                            );
                        }
                    }
                }

                // Notify manager/lead of team member's overdue task
                if (task.getCreatedBy() != null && task.getCreatedBy().getManager() != null) {
                    String managerMsg = String.format(
                        "Team member's task \"%s\" is OVERDUE. " +
                        "Assigned to: %s. Due: %s",
                        task.getTitle(),
                        task.getAssignments().stream()
                            .filter(TaskAssignment::getIsCurrent)
                            .map(a -> a.getAssignee().getFullName())
                            .toList(),
                        task.getDueDate()
                    );

                    notificationService.notify(
                        task.getCreatedBy().getManager(),
                        NotificationType.SYSTEM,
                        "Task Overdue - Manager Alert",
                        managerMsg,
                        task.getId()
                    );
                }

                log.warn("Overdue task alert sent: taskId={}, dueDate={}", task.getId(), task.getDueDate());
            } catch (Exception e) {
                log.error("Error notifying overdue task: taskId={}", taskId, e);
            }
        }));
    }

    /**
     * Scheduled job: Check for contract expiration dates.
     * Scope.md §4.1: Hợp đồng sắp hết hạn (30 ngày, 7 ngày trước)
     * Runs daily at 9 AM to check contract expiration dates.
     */
    @Scheduled(cron = "0 0 9 * * ?") // Daily at 9:00 AM
    @Transactional(readOnly = true)
    public void checkContractExpirations() {
        log.debug("Checking for contract expiration dates...");

        try {
            LocalDate today = LocalDate.now();
            LocalDate thirtyDaysLater = today.plusDays(30);
            LocalDate sevenDaysLater = today.plusDays(7);

            // Find tasks with contract/document type expiring in next 30 days
            List<Task> expiringContracts = taskRepository.findTasksDueInRange(today, thirtyDaysLater)
                .stream()
                .filter(t -> t.getTitle().toLowerCase().contains("contract") ||
                           t.getTitle().toLowerCase().contains("document") ||
                           (t.getSection() != null && t.getSection().toLowerCase().contains("contract")))
                .toList();

            for (Task contract : expiringContracts) {
                LocalDate dueDate = contract.getDueDate();

                // 30 days warning
                if (dueDate.isBefore(thirtyDaysLater.plusDays(1)) &&
                    dueDate.isAfter(today.minusDays(1))) {
                    notifyContractExpiring(contract.getId(), 30);
                }

                // 7 days warning (more urgent)
                if (dueDate.isBefore(sevenDaysLater.plusDays(1)) &&
                    dueDate.isAfter(today.minusDays(1))) {
                    notifyContractExpiring(contract.getId(), 7);
                }
            }

            log.debug("Contract expiration check completed: {} contracts checked", expiringContracts.size());
        } catch (Exception e) {
            log.error("Error checking contract expirations", e);
        }
    }

    private void notifyContractExpiring(Long contractId, int daysUntilExpiry) {
        CompletableFuture.runAsync(() -> runInTransaction(() -> {
            try {
                // Re-fetch inside this thread's own transaction — see
                // notifyTaskNearDeadline for why the caller's Task can't be reused.
                Task contract = taskRepository.findById(contractId).orElse(null);
                if (contract == null) return;

                String urgency = daysUntilExpiry <= 7 ? "🔴 URGENT" : "⚠️ WARNING";
                String message = String.format(
                    "%s: Contract/Document \"%s\" expires in %d days (%s). " +
                    "Action required before expiration!",
                    urgency,
                    contract.getTitle(),
                    daysUntilExpiry,
                    contract.getDueDate()
                );

                // Notify task creator
                if (contract.getCreatedBy() != null) {
                    notificationService.notify(
                        contract.getCreatedBy(),
                        daysUntilExpiry <= 7 ? NotificationType.REMINDER : NotificationType.SYSTEM,
                        "Contract Expiration Notice",
                        message,
                        contract.getId()
                    );

                    // Also notify their manager
                    if (contract.getCreatedBy().getManager() != null) {
                        notificationService.notify(
                            contract.getCreatedBy().getManager(),
                            NotificationType.SYSTEM,
                            "Team Contract Expiration",
                            message + String.format(" - Created by: %s", contract.getCreatedBy().getFullName()),
                            contract.getId()
                        );
                    }
                }

                log.info("Contract expiration notice sent: contractId={}, expiresIn={}days",
                        contract.getId(), daysUntilExpiry);
            } catch (Exception e) {
                log.error("Error notifying contract expiration: contractId={}", contractId, e);
            }
        }));
    }

    /**
     * Scheduled job: Check for evaluation period deadlines.
     * Scope.md §4.1: Deadline tự đánh giá (24h trước hết hạn)
     * Runs daily at 4 PM to remind members of evaluation deadlines.
     */
    @Scheduled(cron = "0 0 16 * * ?") // Daily at 4:00 PM
    @Transactional(readOnly = true)
    public void checkEvaluationDeadlines() {
        log.debug("Checking for evaluation deadlines...");

        try {
            // Get current and ongoing evaluation periods
            List<EvaluationPeriod> activeEvaluationPeriods = evaluationPeriodRepository.findAll()
                .stream()
                .filter(ep -> {
                    LocalDate today = LocalDate.now();
                    YearMonth pm = ep.getPeriodMonth();
                    return pm.atDay(1).isBefore(today) && pm.atEndOfMonth().isAfter(today);
                })
                .toList();

            LocalDate tomorrow = LocalDate.now().plusDays(1);

            for (EvaluationPeriod period : activeEvaluationPeriods) {
                // Check if deadline is approaching (within 24 hours)
                if (period.getMemberDeadline() != null &&
                    period.getMemberDeadline().equals(tomorrow)) {

                    // Find users who haven't submitted self-evaluation yet
                    List<User> allUsers = userRepository.findAll();
                    YearMonth periodMonth = period.getPeriodMonth();

                    for (User user : allUsers) {
                        var evaluations = evaluationRepository.findAllByUserAndPeriod(user.getId(), periodMonth);

                        boolean anySubmitted = evaluations.stream().anyMatch(e -> e.getSelfQuality() != null);
                        if (!anySubmitted) {  // Not yet submitted for any of their group evaluations
                            notifyEvaluationDeadline(user.getId(), period.getId());
                        }
                    }
                }
            }

            log.debug("Evaluation deadline check completed: {} periods checked", activeEvaluationPeriods.size());
        } catch (Exception e) {
            log.error("Error checking evaluation deadlines", e);
        }
    }

    private void notifyEvaluationDeadline(Long userId, Long periodId) {
        CompletableFuture.runAsync(() -> runInTransaction(() -> {
            try {
                // Re-fetch inside this thread's own transaction — see
                // notifyTaskNearDeadline for why the caller's entities can't be reused.
                User user = userRepository.findById(userId).orElse(null);
                EvaluationPeriod period = evaluationPeriodRepository.findById(periodId).orElse(null);
                if (user == null || period == null) return;

                YearMonth pm = period.getPeriodMonth();
                String message = String.format(
                    "⏰ REMINDER: Your self-evaluation for %s - %s is due TOMORROW. " +
                    "Please submit your evaluation before the deadline!",
                    pm.atDay(1),
                    pm.atEndOfMonth()
                );

                notificationService.notify(
                    user,
                    NotificationType.REMINDER,
                    "Evaluation Submission Deadline",
                    message,
                    null
                );

                // Also notify user's manager
                if (user.getManager() != null) {
                    String managerMsg = String.format(
                        "Team member %s has not yet submitted self-evaluation. " +
                        "Deadline: %s",
                        user.getFullName(),
                        period.getMemberDeadline()
                    );

                    notificationService.notify(
                        user.getManager(),
                        NotificationType.SYSTEM,
                        "Team Member Evaluation Status",
                        managerMsg,
                        null
                    );
                }

                log.info("Evaluation deadline reminder sent: userId={}, period={}",
                        user.getId(), period.getId());
            } catch (Exception e) {
                log.error("Error notifying evaluation deadline: userId={}", userId, e);
            }
        }));
    }

    /**
     * Notify that a step is ready to start (previous step completed).
     * Scope.md §4.1: Bước Multi-step sắp đến lượt
     */
    public void notifyStepReadyToStart(Long taskId, Long stepId, String stepName, Long assigneeId) {
        CompletableFuture.runAsync(() -> runInTransaction(() -> {
            try {
                Task task = taskRepository.findById(taskId).orElse(null);
                if (task == null) return;

                User assignee = userRepository.findById(assigneeId).orElse(null);
                if (assignee == null) return;

                String message = String.format(
                    "Step \"%s\" in task \"%s\" is ready for you to start",
                    stepName,
                    task.getTitle()
                );

                notificationService.notify(
                    assignee,
                    NotificationType.SYSTEM,
                    "Step Ready",
                    message,
                    taskId
                );

                log.info("Step ready notification sent: taskId={}, stepId={}, assigneeId={}",
                         taskId, stepId, assigneeId);
            } catch (Exception e) {
                log.error("Failed to send step ready notification", e);
            }
        }));
    }
}
