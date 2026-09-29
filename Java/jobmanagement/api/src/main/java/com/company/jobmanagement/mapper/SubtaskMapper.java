package com.company.jobmanagement.mapper;

import com.company.jobmanagement.model.entity.Subtask;
import com.company.jobmanagement.dto.response.SubtaskResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Subtask entity to SubtaskResponse DTO. {@code steps}
 * auto-maps via {@link StepMapper} (empty list for FAST tasks, since they
 * never get Step children). {@code groupSubtaskId} is null for an
 * ungrouped subtask — MapStruct null-checks the nested path automatically.
 */
@Mapper(componentModel = "spring", uses = StepMapper.class)
public interface SubtaskMapper extends BaseMapper<Subtask, SubtaskResponse> {

    @Mapping(source = "groupSubtask.id", target = "groupSubtaskId")
    @Override
    SubtaskResponse toDTO(Subtask entity);

    @Override
    @Mapping(target = "task", ignore = true)
    @Mapping(target = "groupSubtask", ignore = true)
    Subtask toEntity(SubtaskResponse dto);
}
