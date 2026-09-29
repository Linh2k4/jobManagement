package com.company.jobmanagement.mapper;

import com.company.jobmanagement.model.entity.ExtraField;
import com.company.jobmanagement.dto.response.ExtraFieldResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExtraFieldMapper extends BaseMapper<ExtraField, ExtraFieldResponse> {

    @Override
    ExtraFieldResponse toDTO(ExtraField entity);

    @Override
    @Mapping(target = "category", ignore = true)
    ExtraField toEntity(ExtraFieldResponse dto);
}
