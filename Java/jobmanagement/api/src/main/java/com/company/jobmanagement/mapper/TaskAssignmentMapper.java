package com.company.jobmanagement.mapper;

import com.company.jobmanagement.model.entity.TaskAssignment;
import com.company.jobmanagement.dto.response.TaskAssignmentResponse;
import com.company.jobmanagement.dto.response.UserInfoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for TaskAssignment entity to TaskAssignmentResponse DTO.
 */
@Mapper(componentModel = "spring")
public interface TaskAssignmentMapper extends BaseMapper<TaskAssignment, TaskAssignmentResponse> {

    @Mapping(source = "assignee.id", target = "assignee.id")
    @Mapping(source = "assignee.email", target = "assignee.email")
    @Mapping(source = "assignee.fullName", target = "assignee.fullName")
    @Mapping(source = "assignee.role", target = "assignee.role")
    @Mapping(source = "assignedBy.id", target = "assignedBy.id")
    @Mapping(source = "assignedBy.email", target = "assignedBy.email")
    @Mapping(source = "assignedBy.fullName", target = "assignedBy.fullName")
    @Mapping(source = "assignedBy.role", target = "assignedBy.role")
    TaskAssignmentResponse toDTO(TaskAssignment entity);

    @Override
    TaskAssignment toEntity(TaskAssignmentResponse dto);
}
