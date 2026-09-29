package com.company.jobmanagement.mapper;

import java.util.List;

/**
 * Base mapper interface for entity <-> DTO conversions.
 * Implementations should use MapStruct for automatic generation.
 *
 * @param <E> Entity type
 * @param <D> DTO type
 */
public interface BaseMapper<E, D> {

    /**
     * Convert entity to DTO.
     */
    D toDTO(E entity);

    /**
     * Convert DTO to entity.
     */
    E toEntity(D dto);

    /**
     * Convert list of entities to DTOs.
     */
    List<D> toDTOList(List<E> entities);

    /**
     * Convert list of DTOs to entities.
     */
    List<E> toEntityList(List<D> dtos);
}
