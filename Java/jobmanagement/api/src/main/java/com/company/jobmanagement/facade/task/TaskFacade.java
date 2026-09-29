package com.company.jobmanagement.facade.task;

import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.security.CurrentUser;
import com.company.jobmanagement.service.TaskAssignmentService;
import com.company.jobmanagement.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration layer for task management.
 * Coordinates between TaskService and TaskAssignmentService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskFacade {

    private final TaskService taskService;
    private final TaskAssignmentService taskAssignmentService;
    private final CurrentUser currentUser;

    /**
     * Assign task.
     * - Validate permissions
     * - Assign task
     * - Log event
     */
    @Transactional
    public void assignTask(Long taskId, Long assigneeId) {
        User currentUserEntity = currentUser.getCurrentUser();
        if (currentUserEntity == null) {
            throw new IllegalArgumentException("Current user not found");
        }
        log.info("Assigning task {} to user {} by {}", taskId, assigneeId, currentUserEntity.getId());

        // Core business logic - service handles notification async
        taskAssignmentService.assignTask(taskId, assigneeId);

        log.info("Task {} assigned successfully", taskId);
    }
}
