package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.GroupSubtask;
import com.company.jobmanagement.model.entity.Subtask;
import com.company.jobmanagement.model.entity.Task;
import com.company.jobmanagement.model.entity.TaskAssignment;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.model.enums.StepStatus;
import com.company.jobmanagement.exception.BusinessLogicException;
import com.company.jobmanagement.exception.ForbiddenOperationException;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.repository.GroupSubtaskRepository;
import com.company.jobmanagement.repository.SubtaskRepository;
import com.company.jobmanagement.repository.TaskRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Leaf for a FAST task (ticked done manually — {@link #updateSubtaskStatus});
 * parent-of-steps for a MULTI_STEP task, whose completion instead cascades
 * up from {@link StepService} (Scope.md §2.1/§2.2).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SubtaskService {

    private final SubtaskRepository subtaskRepository;
    private final GroupSubtaskRepository groupSubtaskRepository;
    private final TaskRepository taskRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<Subtask> getSubtasksForTask(Long taskId) {
        return subtaskRepository.findByTaskId(taskId);
    }

    @Transactional(readOnly = true)
    public Subtask getSubtask(Long subtaskId) {
        return subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Subtask not found with id: " + subtaskId));
    }

    public Subtask createSubtask(Long taskId, Long groupSubtaskId, String title, Integer estimateMinutes,
                                  ZonedDateTime deadline, String note) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        User user = currentUser.getCurrentUser();

        if (!task.getCreatedBy().getId().equals(user.getId()) && !user.isManager()) {
            throw new ForbiddenOperationException("Only creator or Manager can add subtasks to this task");
        }

        GroupSubtask group = null;
        if (groupSubtaskId != null) {
            group = groupSubtaskRepository.findById(groupSubtaskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupSubtaskId));
            if (!group.getTask().getId().equals(taskId)) {
                throw new BusinessLogicException("Group does not belong to this task");
            }
        }

        int nextOrder = subtaskRepository.findByTaskId(taskId).stream()
                .mapToInt(Subtask::getSubtaskOrder)
                .max()
                .orElse(0) + 1;

        Subtask subtask = Subtask.builder()
                .task(task)
                .groupSubtask(group)
                .title(title)
                .subtaskOrder(nextOrder)
                .status(StepStatus.PENDING)
                .estimateMinutes(estimateMinutes)
                .deadline(deadline)
                .note(note)
                .build();

        return subtaskRepository.save(subtask);
    }

    public Subtask updateSubtask(Long subtaskId, String title, Integer estimateMinutes, ZonedDateTime deadline,
                                  String note, Boolean isTarget, Integer estimateTarget, Integer target) {
        Subtask subtask = getSubtask(subtaskId);
        User user = currentUser.getCurrentUser();

        if (!subtask.getTask().getCreatedBy().getId().equals(user.getId()) && !user.isManager()) {
            throw new ForbiddenOperationException("Only creator or Manager can edit this subtask");
        }

        if (title != null) {
            subtask.setTitle(title);
        }
        if (estimateMinutes != null) {
            subtask.setEstimateMinutes(estimateMinutes);
        }
        if (deadline != null) {
            subtask.setDeadline(deadline);
        }
        if (note != null) {
            subtask.setNote(note);
        }
        if (isTarget != null) {
            subtask.setIsTarget(isTarget);
        }
        if (estimateTarget != null) {
            subtask.setEstimateTarget(estimateTarget);
        }
        if (target != null) {
            subtask.setTarget(target);
        }
        subtask.setUpdatedAt(ZonedDateTime.now());

        return subtaskRepository.save(subtask);
    }

    public void deleteSubtask(Long subtaskId) {
        Subtask subtask = getSubtask(subtaskId);
        User user = currentUser.getCurrentUser();

        if (!subtask.getTask().getCreatedBy().getId().equals(user.getId()) && !user.isManager()) {
            throw new ForbiddenOperationException("Only creator or Manager can delete this subtask");
        }

        subtaskRepository.delete(subtask);
    }

    /**
     * FAST-task manual completion tick (Scope.md §2.1). Not for a
     * MULTI_STEP subtask that has steps — its status derives from them
     * instead (see StepService's cascade), so this rejects that case.
     * Unlike the Step cascade, ticking a Subtask done does NOT auto-complete
     * the task — Scope.md's FAST flow shows "mark task done" as a distinct,
     * separate manual action from ticking its subtasks.
     */
    public Subtask updateSubtaskStatus(Long subtaskId, StepStatus newStatus) {
        Subtask subtask = getSubtask(subtaskId);
        if (!subtask.getSteps().isEmpty()) {
            throw new BusinessLogicException("This subtask has steps — its status is derived from them, not set manually");
        }

        User user = currentUser.getCurrentUser();
        Task task = subtask.getTask();
        // A FAST task's creator (the Member doing the work) is not
        // necessarily its assignee — FAST tasks auto-assign to the
        // creator's resolving Manager for confirmation (TaskService
        // .createTask), so the creator must still be able to tick their
        // own subtasks even though they're not "the assignee".
        boolean isCreator = task.getCreatedBy().getId().equals(user.getId());
        boolean isTaskAssignee = task.getAssignments().stream()
                .filter(TaskAssignment::getIsCurrent)
                .anyMatch(a -> a.getAssignee().getId().equals(user.getId()));
        if (!isCreator && !isTaskAssignee && !user.isLead() && !user.isManager()) {
            throw new ForbiddenOperationException("Only the task creator, assignee, lead, or manager can update subtask status");
        }

        subtask.setStatus(newStatus);
        subtask.setUpdatedAt(ZonedDateTime.now());
        return subtaskRepository.save(subtask);
    }
}
