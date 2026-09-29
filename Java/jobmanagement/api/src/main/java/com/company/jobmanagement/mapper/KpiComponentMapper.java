package com.company.jobmanagement.mapper;

import com.company.jobmanagement.model.entity.KpiComponent;
import com.company.jobmanagement.dto.response.KpiResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface KpiComponentMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.fullName", target = "userFullName")
    @Mapping(target = "periodMonth", expression = "java(entity.getPeriodMonth().toString())")
    @Mapping(target = "isCached", constant = "true")
    KpiResponse toDTO(KpiComponent entity);
}
