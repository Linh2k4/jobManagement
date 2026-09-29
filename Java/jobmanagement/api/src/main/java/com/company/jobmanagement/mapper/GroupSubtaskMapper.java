package com.company.jobmanagement.mapper;

import com.company.jobmanagement.model.entity.GroupSubtask;
import com.company.jobmanagement.dto.response.GroupSubtaskResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps only the group header — its subtasks are serialized flat on TaskResponse.subtasks. */
@Mapper(componentModel = "spring")
public interface GroupSubtaskMapper extends BaseMapper<GroupSubtask, GroupSubtaskResponse> {

    @Override
    GroupSubtaskResponse toDTO(GroupSubtask entity);

    @Override
    @Mapping(target = "task", ignore = true)
    @Mapping(target = "subtasks", ignore = true)
    GroupSubtask toEntity(GroupSubtaskResponse dto);
}
