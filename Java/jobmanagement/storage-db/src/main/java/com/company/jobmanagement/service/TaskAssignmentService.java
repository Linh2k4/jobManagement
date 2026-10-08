package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.*;
import com.company.jobmanagement.model.enums.NotificationType;
import com.company.jobmanagement.model.enums.Role;
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
import java.util.List;
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

        User assignee = userRepository.findByIdWithLeadAndManager(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + assigneeId));
        User currentUser = userRepository.findByIdWithLeadAndManager(this.currentUser.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate assignment permission based on role: Manager or Creator Lead
        validateAssignmentPermission(currentUser, assignee, task);

        applyTaskGroup(task, assignee, explicitGroupId);

        // Check if user is already actively assigned to this task
        List<TaskAssignment> currentAssignments = taskAssignmentRepository.findCurrentAssignments(taskId);
        for (TaskAssignment existing : currentAssignments) {
            if (existing.getAssignee().getId().equals(assigneeId) && Boolean.TRUE.equals(existing.getIsCurrent())) {
                return existing;
            }
        }

        // Create new active assignment without clearing other active assignees (supporting multiple assignees)
        TaskAssignment newAssignment = TaskAssignment.builder()
                .task(task)
                .assignee(assignee)
                .assignedBy(currentUser)
                .isCurrent(true)
                .assignedAt(ZonedDateTime.now())
                .build();

        TaskAssignment saved = taskAssignmentRepository.saveAndFlush(newAssignment);

        // Send notification to assignee asynchronously
        CompletableFuture.runAsync(() ->
            notificationService.notify(
                assignee,
                NotificationType.TASK_ASSIGNED,
                "Công việc mới được phân công",
                "Công việc \"" + task.getTitle() + "\" đã được phân công cho bạn",
                task.getId()
            )
        );

        return saved;
    }

    public void unassignTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        User currentUser = userRepository.findByIdWithLeadAndManager(this.currentUser.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!currentUser.isManager() && !(currentUser.isLead() && task.getCreatedBy() != null && task.getCreatedBy().getId().equals(currentUser.getId()))) {
            throw new ForbiddenOperationException("Chỉ người tạo việc (Leader) hoặc Admin mới có quyền xóa người tham gia");
        }

        List<TaskAssignment> currentAssignments = taskAssignmentRepository.findCurrentAssignments(taskId);
        for (TaskAssignment assignment : currentAssignments) {
            assignment.setIsCurrent(false);
            taskAssignmentRepository.saveAndFlush(assignment);
        }
        log.info("Task {} all assignments cleared", taskId);
    }

    public void unassignUserFromTask(Long taskId, Long assigneeId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        User currentUser = userRepository.findByIdWithLeadAndManager(this.currentUser.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!currentUser.isManager() && !(currentUser.isLead() && task.getCreatedBy() != null && task.getCreatedBy().getId().equals(currentUser.getId()))) {
            throw new ForbiddenOperationException("Chỉ người tạo việc (Leader) hoặc Admin mới có quyền xóa người tham gia");
        }

        List<TaskAssignment> currentAssignments = taskAssignmentRepository.findCurrentAssignments(taskId);
        for (TaskAssignment assignment : currentAssignments) {
            if (assignment.getAssignee().getId().equals(assigneeId)) {
                assignment.setIsCurrent(false);
                taskAssignmentRepository.saveAndFlush(assignment);
                log.info("Task {} unassigned from user {}", taskId, assigneeId);
            }
        }
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

    private void validateAssignmentPermission(User currentUser, User assignee, Task task) {
        if (currentUser.isManager()) {
            return; // Manager has full permission to assign anyone
        }
        if (currentUser.isLead() && task.getCreatedBy() != null && task.getCreatedBy().getId().equals(currentUser.getId())) {
            // Leader can only assign to MEMBERs, NOT to other LEADs or MANAGERs
            if (assignee.getRole() != Role.MEMBER) {
                throw new ForbiddenOperationException("Trưởng nhóm chỉ có quyền phân công cho nhân viên (MEMBER), không được phân công cho Leader khác hoặc Admin");
            }
            return;
        }
        throw new ForbiddenOperationException("Chỉ người tạo việc (Leader) hoặc Admin mới có quyền phân công người tham gia công việc này");
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

        // Check permissions: Manager, task creator (Lead), or current assignee
        boolean isCreatorOrManager = currentUser.isManager() || (currentUser.isLead() && task.getCreatedBy() != null && task.getCreatedBy().getId().equals(currentUser.getId()));

        TaskAssignment currentAssignment = taskAssignmentRepository.findCurrentAssignment(taskId).orElse(null);
        if (currentAssignment != null) {
            boolean isCurrentAssignee = currentAssignment.getAssignee().getId().equals(currentUser.getId());
            if (!isCreatorOrManager && !isCurrentAssignee) {
                throw new ForbiddenOperationException("Chỉ người tạo việc (Leader), Admin hoặc người đang thực hiện mới có quyền chuyển giao công việc này");
            }
        } else if (!isCreatorOrManager) {
            throw new ForbiddenOperationException("Chỉ người tạo việc (Leader) hoặc Admin mới có quyền phân công công việc này");
        }

        applyTaskGroup(task, newAssignee, explicitGroupId);

        if (currentAssignment != null) {
            currentAssignment.setIsCurrent(false);
            taskAssignmentRepository.saveAndFlush(currentAssignment);
        }

        // Create new assignment
        TaskAssignment newAssignment = TaskAssignment.builder()
                .task(task)
                .assignee(newAssignee)
                .assignedBy(currentUser)
                .isCurrent(true)
                .assignedAt(ZonedDateTime.now())
                .build();

        TaskAssignment saved = taskAssignmentRepository.saveAndFlush(newAssignment);
        log.info("Task reassigned: taskId={}, to={}", taskId, newAssigneeId);

        // Send notification to new assignee
        CompletableFuture.runAsync(() ->
            notificationService.notify(
                newAssignee,
                NotificationType.TASK_ASSIGNED,
                "Công việc mới được chuyển giao",
                "Công việc \"" + task.getTitle() + "\" đã được giao cho bạn",
                task.getId()
            )
        );

        return saved;
    }
}
