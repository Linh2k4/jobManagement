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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Task createTask(Task task) {
        User currentUser = this.currentUser.getCurrentUser();
        TaskType taskType = taskTypeService.getTaskType(task.getTaskType().getId());

        // Validate task creation permissions based on role and task type
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
        switch (taskType.getTimeCategory()) {
            case FAST:
                // Member can create Fast tasks only (self-assigned to Manager)
                if (!currentUser.isMember()) {
                    throw new ForbiddenOperationException("Only Members can create FAST tasks");
                }
                break;
            case OFTEN:
                // Lead can create Often tasks
                if (!currentUser.isLead() && !currentUser.isManager()) {
                    throw new ForbiddenOperationException("Only Leads or Manager can create OFTEN tasks");
                }
                break;
            case MULTI_STEP:
                // Manager/Lead can create Multi-step tasks
                if (!currentUser.isManager() && !currentUser.isLead()) {
                    throw new ForbiddenOperationException("Only Manager or Leads can create MULTI_STEP tasks");
                }
                break;
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
        // taskType/createdBy are JOIN FETCHed above; groupSubtasks/subtasks/
        // assignments are collections (can't JOIN FETCH more than one bag in
        // one query) — touch them here, still inside the transaction, so
        // TaskMapper can read them after this method returns (open-in-view
        // is disabled).
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
            // Lead sees tasks created by members in their team
            return taskRepository.findByCreatedById(currentUser.getId());
        } else {
            // Member sees their own and assigned tasks
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

        // Only creator or Manager can update
        if (!task.getCreatedBy().getId().equals(currentUser.getId()) && !currentUser.isManager()) {
            throw new ForbiddenOperationException("Only creator or Manager can update this task");
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
        Task task = getTask(id);
        User currentUser = this.currentUser.getCurrentUser();

        // Current assignee can update status
        task.getAssignments().stream()
                .filter(TaskAssignment::getIsCurrent)
                .findFirst()
                .ifPresentOrElse(
                    assignment -> {
                        if (!assignment.getAssignee().getId().equals(currentUser.getId())) {
                            throw new ForbiddenOperationException("Only current assignee can update task status");
                        }
                    },
                    () -> {
                        throw new ForbiddenOperationException("Task has no current assignee");
                    }
                );

        task.setStatus(newStatus);
        if (newStatus == TaskStatus.DONE) {
            task.setCompletedAt(ZonedDateTime.now());
        }

        task.setUpdatedAt(ZonedDateTime.now());
        Task updated = taskRepository.save(task);
        invalidateKpiForCurrentAssignee(updated);
        return updated;
    }

    public void cancelTask(Long id) {
        Task task = getTask(id);
        User currentUser = this.currentUser.getCurrentUser();

        // Only creator or Manager can cancel
        if (!task.getCreatedBy().getId().equals(currentUser.getId()) && !currentUser.isManager()) {
            throw new ForbiddenOperationException("Only creator or Manager can cancel this task");
        }

        task.setStatus(TaskStatus.CANCELLED);
        task.setUpdatedAt(ZonedDateTime.now());
        taskRepository.save(task);
        invalidateKpiForCurrentAssignee(task);
    }

    /**
     * Update task difficulty (1-5). Only Lead or Manager may set it — it
     * feeds the KPI task-weight calculation, not something a Member should
     * self-assign.
     */
    public Task updateDifficulty(Long id, Difficulty difficulty) {
        Task task = getTask(id);
        User currentUser = this.currentUser.getCurrentUser();

        if (!currentUser.isLead() && !currentUser.isManager()) {
            throw new ForbiddenOperationException("Only Lead or Manager can set task difficulty");
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
     * Directly push the due date back — Manager only, since a Manager's
     * extension request self-approves anyway (Scope.md §12.7). Member/Lead
     * must go through DeadlineExtensionService's request-and-approval flow.
     */
    public Task extendDeadline(Long id, int daysToAdd, String reason) {
        Task task = getTask(id);
        User currentUser = this.currentUser.getCurrentUser();

        if (!currentUser.isManager()) {
            throw new ForbiddenOperationException("Only Manager can extend a deadline directly — use the deadline extension request flow instead");
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

        // FAST tasks are "self-assigned to Manager" (see validateTaskCreationPermission)
        // — without an assignment, updateTaskStatus can never find a current
        // assignee and every status change 403s. OFTEN/MULTI_STEP tasks are
        // assigned separately by a Lead/Manager via TaskAssignmentController.
        if (taskType.getTimeCategory() == TimeCategory.FAST && currentUser.isMember()) {
            User freshCurrentUser = userRepository.findByIdWithLeadAndManager(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUser.getId()));
            User manager = freshCurrentUser.getLead() != null ? freshCurrentUser.getLead().getManager() : null;
            if (manager != null) {
                // Scope.md §13.4: a FAST task's group is the creating
                // Member's group, not the auto-assigned Manager's — a
                // Manager has no group membership of their own in this
                // model (only Leads/Members do), so applyTaskGroup's
                // default "derive from assignee" would leave the task
                // group-less. Pass the creator's primary group explicitly.
                Long creatorGroupId = groupMembershipRepository.findPrimaryForMember(freshCurrentUser.getId())
                        .map(gm -> gm.getGroup().getId())
                        .orElse(null);

                // Task.builder() sets assignments to a plain new ArrayList
                // (not a Hibernate-managed lazy collection), and since
                // savedTask is already in this transaction's persistence
                // context, re-fetching returns that same identity-mapped
                // instance — its assignments field never gets refreshed from
                // the DB. Append the just-created assignment in memory
                // instead, so the caller's response reflects it directly.
                TaskAssignment assignment = taskAssignmentService.assignTask(savedTask.getId(), manager.getId(), creatorGroupId);
                savedTask.getAssignments().add(assignment);
            }
            log.warn("Could not auto-assign FAST task {} — creator {} has no manager in their reporting chain",
                    savedTask.getId(), currentUser.getId());
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

        if (!task.getCreatedBy().getId().equals(currentUser.getId()) && !currentUser.isManager()) {
            throw new ForbiddenOperationException("Only creator or Manager can update this task");
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
        if (!currentUser.isManager()) {
            // For non-managers, restrict to tasks they created or are assigned to
            page = (status != null)
                    ? taskRepository.findByStatusAndCreator(status, currentUser.getId(), pageable)
                    : taskRepository.findByCreatedByIdWithEagerLoad(currentUser.getId(), pageable);
        } else {
            // For managers, apply full filtering
            page = taskRepository.findByMultipleCriteria(status, taskTypeId, section, dueDateFrom, dueDateTo, pageable);
        }

        // These queries JOIN FETCH subtasks (can't also JOIN FETCH
        // groupSubtasks/assignments/steps — Hibernate rejects fetching more
        // than one List-typed collection in one query), so touch the rest
        // here, still inside the transaction, for TaskMapper's
        // groupSubtasks/subtasks[].steps/currentAssignment fields.
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
            // Validate status transition
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
        // Use repository method with multiple criteria
        return taskRepository.findByMultipleCriteria(
                status,
                taskTypeId,
                section,
                dueDateFrom,
                dueDateTo,
                pageable
        );
    }
}
