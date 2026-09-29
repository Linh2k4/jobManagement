package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.*;
import com.company.jobmanagement.model.enums.NotificationType;
import com.company.jobmanagement.exception.ForbiddenOperationException;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.repository.GroupMembershipRepository;
import com.company.jobmanagement.repository.GroupRepository;
import com.company.jobmanagement.repository.TaskAssignmentRepository;
import com.company.jobmanagement.repository.TaskRepository;
import com.company.jobmanagement.repository.UserRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskAssignmentService {

    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final CurrentUser currentUser;
    private final NotificationService notificationService;

    public TaskAssignment assignTask(Long taskId, Long assigneeId) {
        return assignTask(taskId, assigneeId, null);
    }

    /**
     * @param explicitGroupId Scope.md §13.4 — set when a Lead assigns within
     *                        a specific one of the assignee's several groups.
     *                        Null auto-derives from the assignee's primary group.
     */
    public TaskAssignment assignTask(Long taskId, Long assigneeId, Long explicitGroupId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        // Fetched fresh (not the SecurityContext's detached copy) with
        // lead/manager JOIN FETCHed — validateAssignmentDirection may walk
        // either user's reporting chain, and a lazy proxy off a detached
        // entity has no session to initialize from.
        User assignee = userRepository.findByIdWithLeadAndManager(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + assigneeId));
        User currentUser = userRepository.findByIdWithLeadAndManager(this.currentUser.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate assignment direction based on role and task type
        validateAssignmentDirection(currentUser, assignee, task);

        applyTaskGroup(task, assignee, explicitGroupId);

        // Mark previous assignment as non-current — flushed immediately.
        // Hibernate's default flush order runs insertions before updates, so
        // without this the new (is_current=true) row's INSERT would execute
        // before this UPDATE, and the partial unique index on
        // task_assignments(task_id) WHERE is_current briefly sees two
        // current rows for the same task and rejects the insert.
        Optional<TaskAssignment> currentAssignment = taskAssignmentRepository.findCurrentAssignment(taskId);
        currentAssignment.ifPresent(assignment -> {
            assignment.setIsCurrent(false);
            taskAssignmentRepository.saveAndFlush(assignment);
        });

        // Create new assignment
        TaskAssignment newAssignment = TaskAssignment.builder()
                .task(task)
                .assignee(assignee)
                .assignedBy(currentUser)
                .isCurrent(true)
                .assignedAt(ZonedDateTime.now())
                .build();

        // Flushed explicitly: callers may immediately re-fetch the task
        // afterward, and Hibernate's auto-flush heuristic doesn't reliably
        // fire for a query that doesn't itself join task_assignments —
        // without this the caller can see a response with no current
        // assignment despite this insert having happened.
        TaskAssignment saved = taskAssignmentRepository.saveAndFlush(newAssignment);

        // Send notification to assignee asynchronously
        CompletableFuture.runAsync(() ->
            notificationService.notify(
                assignee,
                NotificationType.TASK_ASSIGNED,
                "Task Assigned",
                "Task \"" + task.getTitle() + "\" has been assigned to you",
                task.getId()
            )
        );

        return saved;
    }

    /**
     * Scope.md §13.4: stamp the task with the Group its KPI counts toward.
     * An explicit id (a Lead assigning within one specific group of a
     * multi-group member) wins; otherwise auto-derive from the assignee's
     * primary group. Leaves the task group untouched if the assignee has
     * no group membership at all yet (e.g. migration edge cases).
     */
    private void applyTaskGroup(Task task, User assignee, Long explicitGroupId) {
        if (explicitGroupId != null) {
            Group group = groupRepository.findById(explicitGroupId)
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + explicitGroupId));
            task.setGroup(group);
            taskRepository.save(task);
        } else {
            groupMembershipRepository.findPrimaryForMember(assignee.getId())
                    .ifPresent(gm -> {
                        task.setGroup(gm.getGroup());
                        taskRepository.save(task);
                    });
        }
    }

    /**
     * A Member's direct manager_id is always null by design (User hierarchy:
     * MANAGER → LEAD has manager_id set → MEMBER has lead_id set, not
     * manager_id) — their manager is found by walking through their Lead.
     */
    private User resolveManagerOf(User member) {
        return member.getLead() != null ? member.getLead().getManager() : null;
    }

    private void validateAssignmentDirection(User currentUser, User assignee, Task task) {
        TaskType taskType = task.getTaskType();

        switch (taskType.getTimeCategory()) {
            case FAST:
                // FAST: Member can assign to their own manager (via their Lead) only
                if (currentUser.isMember()) {
                    User manager = resolveManagerOf(currentUser);
                    if (manager != null && assignee.getId().equals(manager.getId())) {
                        return;
                    }
                } else if (currentUser.isManager()) {
                    // Manager can assign FAST tasks to any Member under them
                    if (assignee.isMember()) {
                        User assigneeManager = resolveManagerOf(assignee);
                        if (assigneeManager != null && assigneeManager.getId().equals(currentUser.getId())) {
                            return;
                        }
                    }
                }
                throw new ForbiddenOperationException("Invalid assignment direction for FAST task");

            case OFTEN:
                // OFTEN: Lead or Manager can assign to Members in their team
                if (currentUser.isLead() || currentUser.isManager()) {
                    if (assignee.isMember() && assignee.getLead().getId().equals(currentUser.getId())) {
                        return;
                    }
                }
                throw new ForbiddenOperationException("Invalid assignment direction for OFTEN task");

            case MULTI_STEP:
                // MULTI_STEP: Lead assigns to Members in their team; Manager can assign to any
                if (currentUser.isLead()) {
                    if (assignee.isMember() && assignee.getLead().getId().equals(currentUser.getId())) {
                        return;
                    }
                } else if (currentUser.isManager()) {
                    if (assignee.isMember() || assignee.isLead()) {
                        return;
                    }
                }
                throw new ForbiddenOperationException("Invalid assignment direction for MULTI_STEP task");
        }
    }

    @Transactional(readOnly = true)
    public TaskAssignment getCurrentAssignment(Long taskId) {
        return taskAssignmentRepository.findCurrentAssignment(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("No current assignment for task id: " + taskId));
    }

    /**
     * Get assignment history for a task with pagination.
     * Returns all assignments (current and past) ordered by most recent first.
     */
    @Transactional(readOnly = true)
    public Page<TaskAssignment> getAssignmentHistory(Long taskId, Pageable pageable) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        return taskAssignmentRepository.findByTaskIdOrderByAssignedAtDesc(taskId, pageable);
    }

    /**
     * Reassign a task to a different user.
     * Only the current assignee or manager can reassign.
     */
    public TaskAssignment reassignTask(Long taskId, Long newAssigneeId) {
        return reassignTask(taskId, newAssigneeId, null);
    }

    public TaskAssignment reassignTask(Long taskId, Long newAssigneeId, Long explicitGroupId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User newAssignee = userRepository.findById(newAssigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + newAssigneeId));

        User currentUser = this.currentUser.getCurrentUser();

        // Check current assignment and permissions
        TaskAssignment currentAssignment = taskAssignmentRepository.findCurrentAssignment(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task has no current assignment"));

        if (!currentAssignment.getAssignee().getId().equals(currentUser.getId()) && !currentUser.isManager()) {
            throw new ForbiddenOperationException("Only current assignee or manager can reassign this task");
        }

        // Validate new assignment direction
        validateAssignmentDirection(currentUser, newAssignee, task);

        applyTaskGroup(task, newAssignee, explicitGroupId);

        // Mark current as non-current — flushed immediately, same reason as
        // in assignTask() (Hibernate would otherwise insert the new
        // is_current=true row before this update, tripping the partial
        // unique index).
        currentAssignment.setIsCurrent(false);
        taskAssignmentRepository.saveAndFlush(currentAssignment);

        // Create new assignment
        TaskAssignment newAssignment = TaskAssignment.builder()
                .task(task)
                .assignee(newAssignee)
                .assignedBy(currentUser)
                .isCurrent(true)
                .assignedAt(ZonedDateTime.now())
                .build();

        // Flushed explicitly: callers may immediately re-fetch the task
        // afterward, and Hibernate's auto-flush heuristic doesn't reliably
        // fire for a query that doesn't itself join task_assignments —
        // without this the caller can see a response with no current
        // assignment despite this insert having happened.
        TaskAssignment saved = taskAssignmentRepository.saveAndFlush(newAssignment);
        log.info("Task reassigned: taskId={}, from={}, to={}", taskId, currentAssignment.getAssignee().getId(), newAssigneeId);

        // Send notification to new assignee
        CompletableFuture.runAsync(() ->
            notificationService.notify(
                newAssignee,
                NotificationType.TASK_ASSIGNED,
                "Task Reassigned",
                "Task \"" + task.getTitle() + "\" has been reassigned to you",
                task.getId()
            )
        );

        return saved;
    }
}
