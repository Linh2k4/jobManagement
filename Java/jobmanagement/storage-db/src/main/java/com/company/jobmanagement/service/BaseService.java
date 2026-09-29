package com.company.jobmanagement.service;

import com.company.jobmanagement.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Base service interface providing CRUD operations.
 * All service classes should implement this for consistency.
 *
 * @param <T> Entity type
 * @param <ID> Primary key type
 */
public interface BaseService<T, ID> {

    /**
     * Get entity by ID.
     *
     * @param id Entity ID
     * @return Entity if found
     * @throws ResourceNotFoundException if not found
     */
    T getById(ID id);

    /**
     * Get entity by ID (optional).
     *
     * @param id Entity ID
     * @return Optional entity
     */
    Optional<T> getByIdOptional(ID id);

    /**
     * Get all entities with pagination.
     *
     * @param pageable Pagination info
     * @return Page of entities
     */
    Page<T> getAll(Pageable pageable);

    /**
     * Get all entities (no pagination).
     *
     * @return List of all entities
     */
    List<T> getAll();

    /**
     * Create/save entity.
     *
     * @param entity Entity to save
     * @return Saved entity
     */
    T create(T entity);

    /**
     * Update entity.
     *
     * @param id Entity ID
     * @param entity Updated entity data
     * @return Updated entity
     * @throws ResourceNotFoundException if not found
     */
    T update(ID id, T entity);

    /**
     * Delete entity by ID.
     *
     * @param id Entity ID
     * @throws ResourceNotFoundException if not found
     */
    void delete(ID id);

    /**
     * Check if entity exists.
     *
     * @param id Entity ID
     * @return true if exists
     */
    boolean exists(ID id);

    /**
     * Get total count of entities.
     *
     * @return Count of entities
     */
    long count();
}
