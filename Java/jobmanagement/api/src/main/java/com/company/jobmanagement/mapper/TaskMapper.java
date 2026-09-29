package com.company.jobmanagement.mapper;

import com.company.jobmanagement.model.entity.Task;
import com.company.jobmanagement.dto.response.TaskResponse;
import com.company.jobmanagement.dto.response.TaskAssignmentResponse;
import com.company.jobmanagement.dto.response.UserInfoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Task entity to TaskResponse DTO conversion.
 * Handles nested entity mappings for assignments and the subtask tree
 * (groupSubtasks/subtasks auto-map via GroupSubtaskMapper/SubtaskMapper).
 */
@Mapper(componentModel = "spring", uses = {GroupSubtaskMapper.class, SubtaskMapper.class})
public interface TaskMapper extends BaseMapper<Task, TaskResponse> {

    @Mapping(source = "taskType.id", target = "taskTypeId")
    @Mapping(source = "createdBy.id", target = "createdBy.id")
    @Mapping(source = "createdBy.email", target = "createdBy.email")
    @Mapping(source = "createdBy.fullName", target = "createdBy.fullName")
    @Mapping(source = "createdBy.role", target = "createdBy.role")
    @Mapping(target = "currentAssignment", expression = "java(getCurrentAssignment(entity))")
    @Mapping(target = "difficulty", expression = "java(entity.getDifficulty() != null ? entity.getDifficulty().getLevel() : null)")
    @Mapping(target = "priority", expression = "java(entity.getPriority() != null ? entity.getPriority().name() : null)")
    TaskResponse toDTO(Task entity);

    /**
     * Extract current (active) assignment from task's assignment list.
     * Current assignment is the one with isCurrent=true.
     */
    default TaskAssignmentResponse getCurrentAssignment(Task task) {
        if (task.getAssignments() == null || task.getAssignments().isEmpty()) {
            return null;
        }

        return task.getAssignments().stream()
                .filter(assignment -> assignment.getIsCurrent() != null && assignment.getIsCurrent())
                .map(assignment -> TaskAssignmentResponse.builder()
                        .id(assignment.getId())
                        .assignee(UserInfoResponse.builder()
                                .id(assignment.getAssignee().getId())
                                .email(assignment.getAssignee().getEmail())
                                .fullName(assignment.getAssignee().getFullName())
                                .role(assignment.getAssignee().getRole().name())
                                .build())
                        .assignedBy(UserInfoResponse.builder()
                                .id(assignment.getAssignedBy().getId())
                                .email(assignment.getAssignedBy().getEmail())
                                .fullName(assignment.getAssignedBy().getFullName())
                                .role(assignment.getAssignedBy().getRole().name())
                                .build())
                        .assignedAt(assignment.getAssignedAt())
                        .isCurrent(assignment.getIsCurrent())
                        .build())
                .findFirst()
                .orElse(null);
    }

    @Override
    @Mapping(target = "difficulty", ignore = true)
    Task toEntity(TaskResponse dto);
}
