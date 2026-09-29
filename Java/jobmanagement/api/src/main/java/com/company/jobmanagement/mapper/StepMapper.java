package com.company.jobmanagement.mapper;

import com.company.jobmanagement.model.entity.Step;
import com.company.jobmanagement.dto.response.StepResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Step entity to StepResponse DTO (MULTI_STEP tasks
 * only — Scope.md §2.2). {@code assignee} auto-maps User -> UserInfoResponse
 * by property-name matching (role: enum -> String via MapStruct's built-in
 * enum-to-String conversion).
 */
@Mapper(componentModel = "spring")
public interface StepMapper extends BaseMapper<Step, StepResponse> {

    @Override
    StepResponse toDTO(Step entity);

    @Override
    @Mapping(target = "subtask", ignore = true)
    @Mapping(target = "taskTypeStep", ignore = true)
    Step toEntity(StepResponse dto);
}
