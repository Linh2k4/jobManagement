package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.GroupSubtask;
import com.company.jobmanagement.model.entity.Task;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.exception.ForbiddenOperationException;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.repository.GroupSubtaskRepository;
import com.company.jobmanagement.repository.TaskRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Optional group headers under a Task (Scope.md §2.1/§2.2). Deleting a
 * group cascades to its subtasks (and their steps) via ON DELETE CASCADE.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GroupSubtaskService {

    private final GroupSubtaskRepository groupSubtaskRepository;
    private final TaskRepository taskRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<GroupSubtask> getGroupsForTask(Long taskId) {
        return groupSubtaskRepository.findByTaskId(taskId);
    }

    public GroupSubtask createGroup(Long taskId, String name) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        User user = currentUser.getCurrentUser();

        if (!task.getCreatedBy().getId().equals(user.getId()) && !user.isManager()) {
            throw new ForbiddenOperationException("Only creator or Manager can add groups to this task");
        }

        int nextOrder = groupSubtaskRepository.findByTaskId(taskId).stream()
                .mapToInt(GroupSubtask::getGroupOrder)
                .max()
                .orElse(0) + 1;

        GroupSubtask group = GroupSubtask.builder()
                .task(task)
                .name(name)
                .groupOrder(nextOrder)
                .build();

        return groupSubtaskRepository.save(group);
    }

    public GroupSubtask updateGroup(Long groupId, String name) {
        GroupSubtask group = getGroup(groupId);
        User user = currentUser.getCurrentUser();

        if (!group.getTask().getCreatedBy().getId().equals(user.getId()) && !user.isManager()) {
            throw new ForbiddenOperationException("Only creator or Manager can edit this group");
        }

        if (name != null) {
            group.setName(name);
        }
        group.setUpdatedAt(ZonedDateTime.now());

        return groupSubtaskRepository.save(group);
    }

    public void deleteGroup(Long groupId) {
        GroupSubtask group = getGroup(groupId);
        User user = currentUser.getCurrentUser();

        if (!group.getTask().getCreatedBy().getId().equals(user.getId()) && !user.isManager()) {
            throw new ForbiddenOperationException("Only creator or Manager can delete this group");
        }

        groupSubtaskRepository.delete(group);
    }

    @Transactional(readOnly = true)
    public GroupSubtask getGroup(Long groupId) {
        return groupSubtaskRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));
    }
}
