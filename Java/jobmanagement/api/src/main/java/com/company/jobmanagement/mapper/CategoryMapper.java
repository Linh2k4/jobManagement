package com.company.jobmanagement.mapper;

import com.company.jobmanagement.model.entity.Category;
import com.company.jobmanagement.dto.response.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ExtraFieldMapper.class)
public interface CategoryMapper extends BaseMapper<Category, CategoryResponse> {

    @Mapping(source = "owner.id", target = "ownerId")
    @Override
    CategoryResponse toDTO(Category entity);

    @Override
    @Mapping(target = "owner", ignore = true)
    Category toEntity(CategoryResponse dto);
}
