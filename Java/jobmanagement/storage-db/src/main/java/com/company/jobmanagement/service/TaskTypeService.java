package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.TaskType;
import com.company.jobmanagement.model.enums.TimeCategory;
import com.company.jobmanagement.exception.ForbiddenOperationException;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.repository.TaskTypeRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskTypeService {

    private final TaskTypeRepository taskTypeRepository;
    private final CurrentUser currentUser;

    public TaskType createTaskType(TaskType taskType) {
        if (!currentUser.getCurrentUser().isManager()) {
            throw new ForbiddenOperationException("Only Manager can create task types");
        }

        if (taskType.getTimeCategory() == TimeCategory.MULTI_STEP) {
            taskType.setIsMultiStep(true);
        }

        return taskTypeRepository.save(taskType);
    }

    @Transactional(readOnly = true)
    public TaskType getTaskType(Long id) {
        TaskType taskType = taskTypeRepository.findByIdWithSteps(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task type not found with id: " + id));
        // subtasks is JOIN FETCHed above; each subtask's steps is a second
        // collection (can't JOIN FETCH two bags in one query) — touch it
        // here, still inside the transaction, for TaskTypeController's mapper.
        taskType.getSubtasks().forEach(s -> s.getSteps().size());
        return taskType;
    }

    @Transactional(readOnly = true)
    public List<TaskType> getAllActiveTaskTypes() {
        List<TaskType> taskTypes = taskTypeRepository.findAllActive();
        taskTypes.forEach(t -> t.getSubtasks().forEach(s -> s.getSteps().size()));
        return taskTypes;
    }

    @Transactional(readOnly = true)
    public List<TaskType> getTaskTypesByTimeCategory(TimeCategory timeCategory) {
        return taskTypeRepository.findByTimeCategory(timeCategory);
    }

    public TaskType updateTaskType(Long id, TaskType taskTypeUpdates) {
        if (!currentUser.getCurrentUser().isManager()) {
            throw new ForbiddenOperationException("Only Manager can update task types");
        }

        TaskType taskType = getTaskType(id);

        if (taskTypeUpdates.getName() != null) {
            taskType.setName(taskTypeUpdates.getName());
        }
        if (taskTypeUpdates.getHasEstimate() != null) {
            taskType.setHasEstimate(taskTypeUpdates.getHasEstimate());
        }
        if (taskTypeUpdates.getDefaultEstimateMinutes() != null) {
            taskType.setDefaultEstimateMinutes(taskTypeUpdates.getDefaultEstimateMinutes());
        }
        if (taskTypeUpdates.getIsActive() != null) {
            taskType.setIsActive(taskTypeUpdates.getIsActive());
        }

        return taskTypeRepository.save(taskType);
    }

    public void deactivateTaskType(Long id) {
        if (!currentUser.getCurrentUser().isManager()) {
            throw new ForbiddenOperationException("Only Manager can deactivate task types");
        }

        TaskType taskType = getTaskType(id);
        taskType.setIsActive(false);
        taskTypeRepository.save(taskType);
    }
}
