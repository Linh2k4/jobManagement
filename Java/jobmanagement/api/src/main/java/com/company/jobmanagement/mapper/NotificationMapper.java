package com.company.jobmanagement.mapper;

import com.company.jobmanagement.model.entity.Notification;
import com.company.jobmanagement.dto.response.NotificationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper extends BaseMapper<Notification, NotificationResponse> {

    @Override
    NotificationResponse toDTO(Notification entity);

    @Override
    Notification toEntity(NotificationResponse dto);
}
