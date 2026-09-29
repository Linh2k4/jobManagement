package com.company.jobmanagement.service;

import com.company.jobmanagement.model.enums.StepStatus;
import com.company.jobmanagement.model.enums.TaskStatus;
import com.company.jobmanagement.model.entity.Step;
import com.company.jobmanagement.model.entity.Subtask;
import com.company.jobmanagement.model.entity.Task;
import com.company.jobmanagement.model.entity.TaskAssignment;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.exception.BusinessLogicException;
import com.company.jobmanagement.exception.ForbiddenOperationException;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.repository.StepRepository;
import com.company.jobmanagement.repository.SubtaskRepository;
import com.company.jobmanagement.repository.TaskRepository;
import com.company.jobmanagement.repository.UserRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Leaf of a MULTI_STEP task's Subtask (Scope.md §2.2). Completing a step
 * cascades: all steps of a Subtask done -> Subtask auto-DONE -> all
 * Subtasks of a Task done -> Task auto-DONE. Ported from the former flat
 * Task -> TaskStep model, rescoped one level deeper (Task -> Subtask).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StepService {

    private final StepRepository stepRepository;
    private final SubtaskRepository subtaskRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final NotificationEventService notificationEventService;

    @Transactional(readOnly = true)
    public List<Step> getStepsForSubtask(Long subtaskId) {
        subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Subtask not found with id: " + subtaskId));
        return stepRepository.findBySubtaskId(subtaskId);
    }

    @Transactional(readOnly = true)
    public Step getStep(Long stepId) {
        return stepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("Step not found with id: " + stepId));
    }

    /** Add an ad-hoc step beyond whatever the template pre-populated. Creator or Manager only. */
    public Step createStep(Long subtaskId, String name, Integer estimateMinutes, ZonedDateTime deadline) {
        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Subtask not found with id: " + subtaskId));
        User user = currentUser.getCurrentUser();

        if (!subtask.getTask().getCreatedBy().getId().equals(user.getId()) && !user.isManager()) {
            throw new ForbiddenOperationException("Only creator or Manager can add steps to this subtask");
        }

        int nextOrder = stepRepository.findBySubtaskId(subtaskId).stream()
                .mapToInt(Step::getStepOrder)
                .max()
                .orElse(0) + 1;

        Step step = Step.builder()
                .subtask(subtask)
                .stepOrder(nextOrder)
                .name(name)
                .estimateMinutes(estimateMinutes)
                .deadline(deadline)
                .status(StepStatus.PENDING)
                .build();

        return stepRepository.save(step);
    }

    /** Rename a step or change its estimate/deadline (not status — see {@link #updateStepStatus}). */
    public Step updateStep(Long stepId, String name, Integer estimateMinutes, ZonedDateTime deadline) {
        Step step = getStep(stepId);
        User user = currentUser.getCurrentUser();

        if (!step.getSubtask().getTask().getCreatedBy().getId().equals(user.getId()) && !user.isManager()) {
            throw new ForbiddenOperationException("Only creator or Manager can edit this step");
        }

        if (name != null) {
            step.setName(name);
        }
        if (estimateMinutes != null) {
            step.setEstimateMinutes(estimateMinutes);
        }
        if (deadline != null) {
            step.setDeadline(deadline);
        }
        step.setUpdatedAt(ZonedDateTime.now());

        return stepRepository.save(step);
    }

    /**
     * Remove a step. Creator or Manager only. Does not renumber the
     * remaining steps' stepOrder — the sequential-dependency check only
     * ever looks for "the step at order N-1", so a gap just means that
     * step has no predecessor to wait on, not a broken chain.
     */
    public void deleteStep(Long stepId) {
        Step step = getStep(stepId);
        User user = currentUser.getCurrentUser();

        if (!step.getSubtask().getTask().getCreatedBy().getId().equals(user.getId()) && !user.isManager()) {
            throw new ForbiddenOperationException("Only creator or Manager can delete this step");
        }

        stepRepository.delete(step);
    }

    public Step assignStep(Long stepId, Long userId) {
        Step step = getStep(stepId);
        User newAssignee = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        User user = currentUser.getCurrentUser();
        if (!user.isLead() && !user.isManager()) {
            throw new ForbiddenOperationException("Only Lead or Manager can assign steps");
        }

        step.setAssignee(newAssignee);
        step.setUpdatedAt(ZonedDateTime.now());

        return stepRepository.save(step);
    }

    public Step updateStepStatus(Long stepId, StepStatus newStatus) {
        Step step = getStep(stepId);
        User user = currentUser.getCurrentUser();

        validateStepUpdatePermission(step, user);
        validateStatusTransition(step.getStatus(), newStatus);

        if (newStatus == StepStatus.DONE) {
            validateStepDependencies(step);
        }

        step.setStatus(newStatus);
        if (newStatus == StepStatus.DONE) {
            step.setCompletedAt(ZonedDateTime.now());
            log.info("Step completed: stepId={}, subtaskId={}, stepOrder={}",
                    step.getId(), step.getSubtask().getId(), step.getStepOrder());

            unlockNextSequentialStep(step);

            if (allStepsCompleted(step.getSubtask())) {
                completeSubtaskAndMaybeTask(step.getSubtask());
            }
        }
        step.setUpdatedAt(ZonedDateTime.now());

        return stepRepository.save(step);
    }

    /** Step assignee, current task assignee, or Lead/Manager. */
    private void validateStepUpdatePermission(Step step, User user) {
        if (step.getAssignee() != null && step.getAssignee().getId().equals(user.getId())) {
            return;
        }

        Task task = step.getSubtask().getTask();
        boolean isTaskAssignee = task.getAssignments().stream()
                .filter(TaskAssignment::getIsCurrent)
                .anyMatch(a -> a.getAssignee().getId().equals(user.getId()));
        if (isTaskAssignee) {
            return;
        }

        if (user.isLead() || user.isManager()) {
            return;
        }

        throw new ForbiddenOperationException("Only step assignee, task assignee, lead, or manager can update step status");
    }

    /** PENDING -> IN_PROGRESS -> DONE, no backward transitions. */
    private void validateStatusTransition(StepStatus currentStatus, StepStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }
        if (currentStatus == StepStatus.DONE) {
            throw new BusinessLogicException("Cannot change status of completed step");
        }
        if (currentStatus == StepStatus.PENDING && (newStatus == StepStatus.IN_PROGRESS || newStatus == StepStatus.DONE)) {
            return;
        }
        if (currentStatus == StepStatus.IN_PROGRESS && newStatus == StepStatus.DONE) {
            return;
        }
        throw new BusinessLogicException(String.format("Cannot transition from %s to %s", currentStatus, newStatus));
    }

    /**
     * Sequential dependency within the same Subtask: a step only completes
     * once the step before it (by order) is done (Scope.md §2.2: "tất cả
     * step trong 1 subtask chạy tuần tự theo order").
     */
    private void validateStepDependencies(Step step) {
        List<Step> siblingSteps = stepRepository.findBySubtaskId(step.getSubtask().getId()).stream()
                .sorted((s1, s2) -> Integer.compare(s1.getStepOrder(), s2.getStepOrder()))
                .collect(Collectors.toList());

        if (step.getStepOrder() > 1) {
            Step previousStep = siblingSteps.stream()
                    .filter(s -> s.getStepOrder() == step.getStepOrder() - 1)
                    .findFirst()
                    .orElse(null);

            if (previousStep != null && previousStep.getStatus() != StepStatus.DONE) {
                throw new BusinessLogicException(
                        String.format("Cannot complete step %d until step %d is done",
                                step.getStepOrder(), previousStep.getStepOrder()));
            }
        }
    }

    private void unlockNextSequentialStep(Step completedStep) {
        List<Step> siblingSteps = stepRepository.findBySubtaskId(completedStep.getSubtask().getId()).stream()
                .sorted((s1, s2) -> Integer.compare(s1.getStepOrder(), s2.getStepOrder()))
                .collect(Collectors.toList());

        Step nextStep = siblingSteps.stream()
                .filter(s -> s.getStepOrder() == completedStep.getStepOrder() + 1)
                .findFirst()
                .orElse(null);

        if (nextStep != null && nextStep.getStatus() == StepStatus.PENDING) {
            log.info("Step {} is now ready to start (previous step {} completed)",
                    nextStep.getStepOrder(), completedStep.getStepOrder());
            publishStepReadyEvent(nextStep, completedStep.getSubtask().getTask());
        }
    }

    private boolean allStepsCompleted(Subtask subtask) {
        List<Step> allSteps = stepRepository.findBySubtaskId(subtask.getId());
        return !allSteps.isEmpty() && allSteps.stream().allMatch(s -> s.getStatus() == StepStatus.DONE);
    }

    /**
     * Subtask DONE when all its Steps are DONE; task tổng DONE when all its
     * Subtasks are DONE (Scope.md §2.2).
     */
    private void completeSubtaskAndMaybeTask(Subtask subtask) {
        subtask.setStatus(StepStatus.DONE);
        subtask.setUpdatedAt(ZonedDateTime.now());
        subtaskRepository.save(subtask);
        log.info("Subtask completed (all steps done): subtaskId={}", subtask.getId());

        Task task = subtask.getTask();
        List<Subtask> allSubtasks = subtaskRepository.findByTaskId(task.getId());
        boolean allDone = !allSubtasks.isEmpty() && allSubtasks.stream().allMatch(Subtask::isDone);
        if (allDone) {
            task.setStatus(TaskStatus.DONE);
            task.setUpdatedAt(ZonedDateTime.now());
            taskRepository.save(task);
            log.info("Task completed (all subtasks done): taskId={}", task.getId());
        }
    }

    private void publishStepReadyEvent(Step step, Task task) {
        try {
            Long assigneeId = step.getAssignee() != null ? step.getAssignee().getId() : null;

            if (assigneeId != null) {
                notificationEventService.notifyStepReadyToStart(task.getId(), step.getId(), step.getName(), assigneeId);
            } else if (task.getCreatedBy() != null && task.getCreatedBy().getLead() != null) {
                notificationEventService.notifyStepReadyToStart(
                        task.getId(), step.getId(), step.getName(), task.getCreatedBy().getLead().getId());
            }

            log.info("Step ready event published: taskId={}, stepId={}, stepOrder={}",
                    task.getId(), step.getId(), step.getStepOrder());
        } catch (Exception e) {
            log.error("Error publishing step ready event: taskId={}, stepId={}", task.getId(), step.getId(), e);
        }
    }
}
