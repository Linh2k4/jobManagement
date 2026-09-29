package com.company.jobmanagement.mapper;

import com.company.jobmanagement.model.entity.Reminder;
import com.company.jobmanagement.dto.response.ReminderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Reminder entity to ReminderResponse DTO.
 */
@Mapper(componentModel = "spring")
public interface ReminderMapper extends BaseMapper<Reminder, ReminderResponse> {

    @Mapping(source = "task.id", target = "taskId")
    ReminderResponse toDTO(Reminder entity);

    @Override
    Reminder toEntity(ReminderResponse dto);
}
