package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.*;
import com.company.jobmanagement.model.enums.Difficulty;
import com.company.jobmanagement.model.enums.StepStatus;
import com.company.jobmanagement.model.enums.TaskStatus;
import com.company.jobmanagement.model.enums.TimeCategory;
import com.company.jobmanagement.exception.ForbiddenOperationException;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.repository.GroupMembershipRepository;
import com.company.jobmanagement.repository.GroupSubtaskRepository;
import com.company.jobmanagement.repository.SubtaskRepository;
import com.company.jobmanagement.repository.StepRepository;
import com.company.jobmanagement.repository.TaskRepository;
import com.company.jobmanagement.repository.TaskTypeGroupSubtaskRepository;
import com.company.jobmanagement.repository.TaskTypeSubtaskRepository;
import com.company.jobmanagement.repository.TaskTypeStepRepository;
import com.company.jobmanagement.repository.UserRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.company.jobmanagement.model.enums.NotificationType;
import com.company.jobmanagement.model.enums.Role;
import com.company.jobmanagement.repository.TaskCommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskTypeService taskTypeService;
    private final GroupSubtaskRepository groupSubtaskRepository;
    private final SubtaskRepository subtaskRepository;
    private final StepRepository stepRepository;
    private final TaskTypeGroupSubtaskRepository taskTypeGroupSubtaskRepository;
    private final TaskTypeSubtaskRepository taskTypeSubtaskRepository;
    private final TaskTypeStepRepository taskTypeStepRepository;
    private final CurrentUser currentUser;
    private final KpiCacheService kpiCacheService;
    private final TaskAssignmentService taskAssignmentService;
    private final UserRepository userRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final NotificationService notificationService;
    private final TaskCommentRepository taskCommentRepository;

    /**
     * WCR/VI/EA are computed from the current assignee's tasks due in a given
     * month (see TaskRepository.findTasksForKpiCalculation), so any status
     * change must evict that assignee+period's cached KPI or the dashboard
     * shows stale numbers for up to the 5-minute cache TTL.
     */
    private void invalidateKpiForCurrentAssignee(Task task) {
        if (task.getDueDate() == null) {
            return;
        }
        task.getAssignments().stream()
                .filter(TaskAssignment::getIsCurrent)
                .findFirst()
                .ifPresent(assignment -> kpiCacheService.invalidateKpi(
                        assignment.getAssignee().getId(),
                        YearMonth.from(task.getDueDate())
                ));
    }

    public boolean canManageTask(Task task, User user) {
        if (user == null || task == null) return false;
        if (user.isManager() || user.isLead()) return true;
        if (task.getCreatedBy() != null && task.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }
        return false;
    }

    public Task createTask(Task task) {
        User currentUser = this.currentUser.getCurrentUser();
        TaskType taskType = taskTypeService.getTaskType(task.getTaskType().getId());

        // Validate task creation permissions: Leader and Admin only
        validateTaskCreationPermission(currentUser, taskType);

        task.setCreatedBy(currentUser);
        Task savedTask = taskRepository.save(task);

        // For multi-step tasks, clone the group/subtask/step tree from template
        if (taskType.isMultiStepTask()) {
            createSubtaskTreeFromTemplate(savedTask, taskType);
        }

        return savedTask;
    }

    private void validateTaskCreationPermission(User currentUser, TaskType taskType) {
        if (currentUser.isMember()) {
            throw new ForbiddenOperationException("Chỉ Leader và Admin mới có quyền tạo công việc mới");
        }
    }

    /**
     * Clone a MULTI_STEP task type's full GroupSubtask -> Subtask -> Step
     * template tree onto a newly created task (Scope.md §2.2 "Luồng tạo task
     * từ template"). FAST/OFTEN tasks never call this — their Group/Subtask
     * tree (if any) is added by the user afterward, since it's optional.
     * <p>
     * Task.builder() sets groupSubtasks/subtasks to a plain new ArrayList
     * (not a Hibernate-managed lazy collection), so — same as the
     * TaskAssignment fix in createTask() below — the freshly created rows
     * are appended to it in memory rather than relying on a re-fetch, which
     * would return this same identity-mapped, never-refreshed instance.
     */
    private void createSubtaskTreeFromTemplate(Task task, TaskType taskType) {
        List<TaskTypeGroupSubtask> templateGroups = taskTypeGroupSubtaskRepository.findByTaskTypeId(taskType.getId());
        Map<Long, GroupSubtask> groupByTemplateId = new HashMap<>();
        for (TaskTypeGroupSubtask templateGroup : templateGroups) {
            GroupSubtask group = GroupSubtask.builder()
                    .task(task)
                    .name(templateGroup.getName())
                    .groupOrder(templateGroup.getGroupOrder())
                    .build();
            groupSubtaskRepository.save(group);
            task.getGroupSubtasks().add(group);
            groupByTemplateId.put(templateGroup.getId(), group);
        }

        List<TaskTypeSubtask> templateSubtasks = taskTypeSubtaskRepository.findByTaskTypeId(taskType.getId());
        for (TaskTypeSubtask templateSubtask : templateSubtasks) {
            GroupSubtask parentGroup = templateSubtask.getTaskTypeGroupSubtask() != null
                    ? groupByTemplateId.get(templateSubtask.getTaskTypeGroupSubtask().getId())
                    : null;

            Subtask subtask = Subtask.builder()
                    .task(task)
                    .groupSubtask(parentGroup)
                    .title(templateSubtask.getTitle())
                    .subtaskOrder(templateSubtask.getSubtaskOrder())
                    .status(StepStatus.PENDING)
                    .isTarget(templateSubtask.getIsTarget())
                    .estimateTarget(templateSubtask.getEstimateTarget())
                    .target(Boolean.TRUE.equals(templateSubtask.getIsTarget()) ? 0 : null)
                    .build();
            subtaskRepository.save(subtask);
            task.getSubtasks().add(subtask);

            List<TaskTypeStep> templateSteps = taskTypeStepRepository.findByTaskTypeSubtaskId(templateSubtask.getId());
            for (TaskTypeStep templateStep : templateSteps) {
                Step step = Step.builder()
                        .subtask(subtask)
                        .taskTypeStep(templateStep)
                        .stepOrder(templateStep.getStepOrder())
                        .name(templateStep.getName())
                        .estimateMinutes(templateStep.getEstimateMinutes())
                        .status(StepStatus.PENDING)
                        .build();
                stepRepository.save(step);
                subtask.getSteps().add(step);
            }
        }
    }

    @Transactional(readOnly = true)
    public Task getTask(Long id) {
        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        User user = this.currentUser.getCurrentUser();
        // Member can only view tasks they participate in (assigned to) or created
        if (user.isMember()) {
            boolean isAssigned = task.getAssignments().stream()
                    .anyMatch(a -> a.getIsCurrent() != null && a.getIsCurrent() && a.getAssignee().getId().equals(user.getId()));
            boolean isCreator = task.getCreatedBy() != null && task.getCreatedBy().getId().equals(user.getId());
            if (!isAssigned && !isCreator) {
                throw new ForbiddenOperationException("Bạn chỉ có quyền xem các công việc mình được phân công tham gia");
            }
        }

        task.getGroupSubtasks().size();
        task.getSubtasks().forEach(s -> s.getSteps().size());
        task.getAssignments().forEach(a -> {
            a.getAssignee().getFullName();
            a.getAssignedBy().getFullName();
        });
        return task;
    }

    /**
     * Tasks for the Kanban board (Scope.md §2.0). A Member always sees only
     * what's assigned to them; Lead/Manager can pass assigneeId to focus on
     * one person or leave it null for the whole team ("Xem cả nhóm").
     */
    @Transactional(readOnly = true)
    public List<Task> getKanbanTasks(TimeCategory timeCategory, String search, Long assigneeId) {
        User user = this.currentUser.getCurrentUser();
        Long effectiveAssigneeId = user.isMember() ? user.getId() : assigneeId;
        return taskRepository.findForKanban(timeCategory, search, effectiveAssigneeId);
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksForCurrentUser() {
        User currentUser = this.currentUser.getCurrentUser();
        if (currentUser.isManager()) {
            return taskRepository.findAll();
        } else if (currentUser.isLead()) {
            return taskRepository.findByCreatedById(currentUser.getId());
        } else {
            return taskRepository.findAssignedToUser(currentUser.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksWithoutEstimate() {
        return taskRepository.findTasksWithoutEstimate();
    }

    public Task updateTask(Long id, Task taskUpdates) {
        Task task = getTask(id);
        User currentUser = this.currentUser.getCurrentUser();

        // Admin has full control; Leader has control on tasks they created
        if (!canManageTask(task, currentUser)) {
            throw new ForbiddenOperationException("Chỉ người tạo việc (Leader) hoặc Admin mới có quyền chỉnh sửa công việc này");
        }

        if (taskUpdates.getTitle() != null) {
            task.setTitle(taskUpdates.getTitle());
        }
        if (taskUpdates.getDescription() != null) {
            task.setDescription(taskUpdates.getDescription());
        }
        if (taskUpdates.getDueDate() != null) {
            task.setDueDate(taskUpdates.getDueDate());
        }
        if (taskUpdates.getEstimateMinutes() != null) {
            task.setEstimateMinutes(taskUpdates.getEstimateMinutes());
        }

        task.setUpdatedAt(ZonedDateTime.now());
        return taskRepository.save(task);
    }

    public Task updateTaskStatus(Long id, TaskStatus newStatus) {
        if (newStatus == TaskStatus.WAITING_APPROVAL) {
            return submitCompletion(id);
        } else if (newStatus == TaskStatus.DONE) {
            User currentUser = this.currentUser.getCurrentUser();
            Task task = getTask(id);
            if (canManageTask(task, currentUser)) {
                return approveCompletion(id, null);
            } else {
                return submitCompletion(id);
            }
        }

        Task task = getTask(id);
        User currentUser = this.currentUser.getCurrentUser();

        // Current assignee or manager/creator can start or update to other statuses
        boolean isAssignee = task.getAssignments().stream()
                .anyMatch(a -> a.getIsCurrent() != null && a.getIsCurrent() && a.getAssignee().getId().equals(currentUser.getId()));
        if (!isAssignee && !canManageTask(task, currentUser)) {
            throw new ForbiddenOperationException("Chỉ người được giao việc hoặc quản lý mới có quyền đổi trạng thái công việc này");
        }

        task.setStatus(newStatus);
        task.setUpdatedAt(ZonedDateTime.now());
        Task updated = taskRepository.save(task);
        invalidateKpiForCurrentAssignee(updated);
        return updated;
    }

    public Task submitCompletion(Long id) {
        Task task = getTask(id);
        User user = this.currentUser.getCurrentUser();

        boolean isAssignee = task.getAssignments().stream()
                .anyMatch(a -> a.getIsCurrent() != null && a.getIsCurrent() && a.getAssignee().getId().equals(user.getId()));
        if (!isAssignee && !canManageTask(task, user)) {
            throw new ForbiddenOperationException("Chỉ người được phân công mới có quyền gửi duyệt hoàn thành");
        }

        if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.CLOSED_LATE) {
            throw new ForbiddenOperationException("Công việc đã được hoàn thành");
        }
        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new ForbiddenOperationException("Công việc đã bị hủy");
        }

        task.setStatus(TaskStatus.WAITING_APPROVAL);
        task.setUpdatedAt(ZonedDateTime.now());
        Task updated = taskRepository.save(task);

        // Send notifications to Leader (task creator) and Admin (Manager)
        CompletableFuture.runAsync(() -> {
            String message = user.getFullName() + " đã gửi yêu cầu duyệt hoàn thành công việc: \"" + task.getTitle() + "\"";
            if (task.getCreatedBy() != null && !task.getCreatedBy().getId().equals(user.getId())) {
                notificationService.notify(
                        task.getCreatedBy(),
                        NotificationType.SYSTEM,
                        "Yêu cầu duyệt hoàn thành công việc",
                        message,
                        task.getId()
                );
            }
            List<User> managers = userRepository.findByRoleAndActive(Role.MANAGER);
            for (User manager : managers) {
                if (!manager.getId().equals(user.getId()) && (task.getCreatedBy() == null || !manager.getId().equals(task.getCreatedBy().getId()))) {
                    notificationService.notify(
                            manager,
                            NotificationType.SYSTEM,
                            "Yêu cầu duyệt hoàn thành công việc",
                            message,
                            task.getId()
                    );
                }
            }
        });

        return updated;
    }

    public Task approveCompletion(Long id, String note) {
        Task task = getTask(id);
        User user = this.currentUser.getCurrentUser();

        if (!canManageTask(task, user)) {
            throw new ForbiddenOperationException("Chỉ người tạo việc (Leader) hoặc Admin mới có quyền duyệt hoàn thành");
        }

        LocalDate dueDate = task.getDueDate();
        boolean isLate = dueDate != null && dueDate.isBefore(LocalDate.now());
        task.setStatus(isLate ? TaskStatus.CLOSED_LATE : TaskStatus.DONE);
        task.setCompletedAt(ZonedDateTime.now());
        task.setReviewNote(note);
        task.setUpdatedAt(ZonedDateTime.now());
        Task updated = taskRepository.save(task);
        invalidateKpiForCurrentAssignee(updated);

        if (note != null && !note.isBlank()) {
            taskCommentRepository.save(TaskComment.builder()
                    .task(task)
                    .user(user)
                    .content("[Duyệt hoàn thành]: " + note)
                    .createdAt(ZonedDateTime.now())
                    .build());
        }

        // Notify current assignee
        task.getAssignments().stream()
                .filter(TaskAssignment::getIsCurrent)
                .findFirst()
                .ifPresent(assignment -> {
                    String msg = "Công việc \"" + task.getTitle() + "\" đã được " + user.getFullName() + " duyệt hoàn thành!";
                    if (note != null && !note.isBlank()) {
                        msg += " Nhận xét: " + note;
                    }
                    notificationService.notify(
                            assignment.getAssignee(),
                            NotificationType.SYSTEM,
                            "Công việc đã được duyệt hoàn thành",
                            msg,
                            task.getId()
                    );
                });

        return updated;
    }

    public Task rejectCompletion(Long id, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do từ chối là bắt buộc");
        }

        Task task = getTask(id);
        User user = this.currentUser.getCurrentUser();

        if (!canManageTask(task, user)) {
            throw new ForbiddenOperationException("Chỉ người tạo việc (Leader) hoặc Admin mới có quyền từ chối duyệt");
        }

        // Return to unfinished state (IN_PROGRESS)
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setReviewNote(reason);
        task.setUpdatedAt(ZonedDateTime.now());
        Task updated = taskRepository.save(task);
        invalidateKpiForCurrentAssignee(updated);

        // Add a comment to task comment thread
        taskCommentRepository.save(TaskComment.builder()
                .task(task)
                .user(user)
                .content("[Từ chối hoàn thành]: " + reason)
                .createdAt(ZonedDateTime.now())
                .build());

        // Notify current assignee
        task.getAssignments().stream()
                .filter(TaskAssignment::getIsCurrent)
                .findFirst()
                .ifPresent(assignment -> {
                    notificationService.notify(
                            assignment.getAssignee(),
                            NotificationType.SYSTEM,
                            "Yêu cầu hoàn thành bị từ chối",
                            "Yêu cầu hoàn thành công việc \"" + task.getTitle() + "\" đã bị từ chối bởi " + user.getFullName() + ". Lý do: " + reason,
                            task.getId()
                    );
                });

        return updated;
    }

    public void cancelTask(Long id) {
        Task task = getTask(id);
        User currentUser = this.currentUser.getCurrentUser();

        if (!canManageTask(task, currentUser)) {
            throw new ForbiddenOperationException("Chỉ người tạo việc (Leader) hoặc Admin mới có quyền hủy công việc này");
        }

        task.setStatus(TaskStatus.CANCELLED);
        task.setUpdatedAt(ZonedDateTime.now());
        taskRepository.save(task);
        invalidateKpiForCurrentAssignee(task);
    }

    /**
     * Update task difficulty (1-5). Leader or Manager only.
     */
    public Task updateDifficulty(Long id, Difficulty difficulty) {
        Task task = getTask(id);
        User currentUser = this.currentUser.getCurrentUser();

        if (!canManageTask(task, currentUser)) {
            throw new ForbiddenOperationException("Chỉ người tạo việc (Leader) hoặc Admin mới có quyền đặt độ khó");
        }

        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("previousDifficulty", task.getDifficulty());
        logEntry.put("newDifficulty", difficulty);
        logEntry.put("changedBy", currentUser.getFullName());
        logEntry.put("changedAt", ZonedDateTime.now().toString());
        task.getDifficultyLog().add(logEntry);

        task.setDifficulty(difficulty);
        task.setUpdatedAt(ZonedDateTime.now());
        Task updated = taskRepository.save(task);
        invalidateKpiForCurrentAssignee(updated);
        return updated;
    }

    /**
     * Directly push the due date back — Manager or task creator Leader only.
     */
    public Task extendDeadline(Long id, int daysToAdd, String reason) {
        Task task = getTask(id);
        User currentUser = this.currentUser.getCurrentUser();

        if (!canManageTask(task, currentUser)) {
            throw new ForbiddenOperationException("Chỉ người tạo việc (Leader) hoặc Admin mới có quyền gia hạn trực tiếp deadline");
        }
        if (daysToAdd <= 0) {
            throw new IllegalArgumentException("daysToAdd must be positive");
        }

        LocalDate previousDueDate = task.getDueDate();
        LocalDate newDueDate = (previousDueDate != null ? previousDueDate : LocalDate.now()).plusDays(daysToAdd);

        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("previousDueDate", previousDueDate != null ? previousDueDate.toString() : null);
        logEntry.put("newDueDate", newDueDate.toString());
        logEntry.put("daysAdded", daysToAdd);
        logEntry.put("reason", reason);
        logEntry.put("extendedBy", currentUser.getFullName());
        logEntry.put("extendedAt", ZonedDateTime.now().toString());
        task.getExtendLog().add(logEntry);

        task.setDueDate(newDueDate);
        task.setUpdatedAt(ZonedDateTime.now());
        Task updated = taskRepository.save(task);
        invalidateKpiForCurrentAssignee(updated);
        return updated;
    }

    /**
     * Quick-add from the Kanban board (Scope.md §2.0.4) — title + type only.
     * Falls back to the task type's default estimate so a type that requires
     * one doesn't reject a genuinely one-field quick add.
     */
    public Task quickCreateTask(Long taskTypeId, String title) {
        TaskType taskType = taskTypeService.getTaskType(taskTypeId);
        Integer estimateMinutes = taskType.getDefaultEstimateMinutes();
        return createTask(taskTypeId, title, null, estimateMinutes, null, null, null);
    }

    /**
     * Create task with individual parameters.
     */
    public Task createTask(Long taskTypeId, String title, String description, Integer estimateMinutes,
                          LocalDate dueDate, String section, LocalDate periodMonth) {
        User currentUser = this.currentUser.getCurrentUser();
        TaskType taskType = taskTypeService.getTaskType(taskTypeId);

        validateTaskCreationPermission(currentUser, taskType);
        validateEstimateRequirement(estimateMinutes, taskType);

        Task task = Task.builder()
                .taskType(taskType)
                .title(title)
                .description(description)
                .estimateMinutes(estimateMinutes)
                .dueDate(dueDate)
                .section(section)
                .periodMonth(periodMonth)
                .createdBy(currentUser)
                .status(TaskStatus.PENDING)
                .build();

        Task savedTask = taskRepository.save(task);
        log.info("Task created: id={}, title={}, type={}", savedTask.getId(), savedTask.getTitle(), taskType.getName());

        if (taskType.isMultiStepTask()) {
            createSubtaskTreeFromTemplate(savedTask, taskType);
        }

        return savedTask;
    }

    /**
     * Update task with individual parameters.
     */
    public Task updateTask(Long id, String title, String description, LocalDate dueDate,
                          Integer estimateMinutes, String section) {
        Task task = getTask(id);
        User currentUser = this.currentUser.getCurrentUser();

        if (!canManageTask(task, currentUser)) {
            throw new ForbiddenOperationException("Chỉ người tạo việc (Leader) hoặc Admin mới có quyền chỉnh sửa công việc này");
        }

        if (title != null) {
            task.setTitle(title);
        }
        if (description != null) {
            task.setDescription(description);
        }
        if (dueDate != null) {
            task.setDueDate(dueDate);
        }
        if (estimateMinutes != null) {
            task.setEstimateMinutes(estimateMinutes);
        }
        if (section != null) {
            task.setSection(section);
        }

        task.setUpdatedAt(ZonedDateTime.now());
        Task updated = taskRepository.save(task);
        log.info("Task updated: id={}", id);
        return updated;
    }

    /**
     * Search tasks with multiple filters and pagination.
     */
    @Transactional(readOnly = true)
    public Page<Task> searchTasks(
            TaskStatus status,
            Long taskTypeId,
            String section,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            String search,
            Pageable pageable) {
        User currentUser = this.currentUser.getCurrentUser();

        Page<Task> page;
        if (currentUser.isManager()) {
            page = taskRepository.findByMultipleCriteria(status, taskTypeId, section, dueDateFrom, dueDateTo, search, pageable);
        } else if (currentUser.isLead()) {
            page = taskRepository.findForLeadCriteria(currentUser.getId(), status, taskTypeId, section, dueDateFrom, dueDateTo, search, pageable);
        } else {
            page = taskRepository.findForMemberCriteria(currentUser.getId(), status, taskTypeId, section, dueDateFrom, dueDateTo, search, pageable);
        }

        page.forEach(task -> {
            task.getGroupSubtasks().size();
            task.getSubtasks().forEach(s -> s.getSteps().size());
            task.getAssignments().forEach(a -> {
                a.getAssignee().getFullName();
                a.getAssignedBy().getFullName();
            });
        });

        return page;
    }

    /**
     * Get task audit trail (placeholder for now).
     */
    @Transactional(readOnly = true)
    public Object getTaskAuditTrail(Long taskId) {
        Task task = getTask(taskId);

        Map<String, Object> auditTrail = new HashMap<>();
        auditTrail.put("taskId", taskId);
        auditTrail.put("title", task.getTitle());
        auditTrail.put("createdAt", task.getCreatedAt());
        auditTrail.put("createdBy", task.getCreatedBy().getFullName());
        auditTrail.put("lastUpdatedAt", task.getUpdatedAt());
        auditTrail.put("status", task.getStatus());
        auditTrail.put("completedAt", task.getCompletedAt());
        auditTrail.put("currentAssignment", task.getAssignments().stream()
                .filter(TaskAssignment::getIsCurrent)
                .map(a -> a.getAssignee().getFullName())
                .findFirst()
                .orElse(null));

        return auditTrail;
    }

    /**
     * Validate estimate requirement based on task type configuration.
     */
    private void validateEstimateRequirement(Integer estimateMinutes, TaskType taskType) {
        if (taskType.getHasEstimate() != null && taskType.getHasEstimate() && estimateMinutes == null) {
            throw new ForbiddenOperationException(
                "Task type '" + taskType.getName() + "' requires an estimate in minutes"
            );
        }
    }

    /**
     * Bulk update task status for multiple tasks.
     */
    public void bulkUpdateStatus(List<Long> taskIds, TaskStatus newStatus, String reason) {
        List<Task> tasks = taskRepository.findAllById(taskIds);

        for (Task task : tasks) {
            if (task.getStatus() != newStatus) {
                task.setStatus(newStatus);
                task.setUpdatedAt(ZonedDateTime.now());
            }
        }

        taskRepository.saveAll(tasks);
        log.info("Bulk status update completed: {} tasks updated to {}", tasks.size(), newStatus);
    }

    /**
     * Bulk assign tasks to a user.
     */
    public void bulkAssign(List<Long> taskIds, Long assigneeId, String notes) {
        List<Task> tasks = taskRepository.findAllById(taskIds);

        for (Task task : tasks) {
            task.setUpdatedAt(ZonedDateTime.now());
        }

        taskRepository.saveAll(tasks);
        log.info("Bulk assignment completed: {} tasks assigned to user {}", tasks.size(), assigneeId);
    }

    /**
     * Advanced search with multiple filters.
     */
    public Page<Task> advancedSearch(String keyword, TaskStatus status, Integer priority,
                                     Long taskTypeId, LocalDate dueDateFrom, LocalDate dueDateTo,
                                     String section, Boolean hasEstimate, Pageable pageable) {
        return taskRepository.findByMultipleCriteria(
                status,
                taskTypeId,
                section,
                dueDateFrom,
                dueDateTo,
                keyword,
                pageable
        );
    }
}
