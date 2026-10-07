package com.company.jobmanagement.mapper;

import com.company.jobmanagement.model.entity.KpiComponent;
import com.company.jobmanagement.dto.response.KpiResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface KpiComponentMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.fullName", target = "userFullName")
    @Mapping(target = "periodMonth", expression = "java(entity.getPeriodMonth() != null ? entity.getPeriodMonth().toString() : null)")
    @Mapping(target = "kpiFinal", expression = "java(entity.getKpiFinal() != null ? entity.getKpiFinal() : entity.getAutoScore())")
    @Mapping(target = "ranking", expression = "java(entity.getRanking() != null ? entity.getRanking() : entity.calculateRanking())")
    @Mapping(target = "isCached", constant = "true")
    KpiResponse toDTO(KpiComponent entity);
}
