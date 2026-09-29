package com.company.jobmanagement.service;

import com.company.jobmanagement.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Abstract base service providing default CRUD implementations.
 * Concrete services can extend this and override specific methods as needed.
 *
 * @param <T> Entity type
 * @param <ID> Primary key type
 * @param <R> Repository type
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractBaseService<T, ID, R extends JpaRepository<T, ID>> implements BaseService<T, ID> {

    protected final R repository;

    /**
     * Get the entity name for error messages.
     * Override in subclasses for specific names.
     */
    protected String getEntityName() {
        return "Entity";
    }

    @Override
    public T getById(ID id) {
        return getByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        getEntityName() + " not found with id: " + id));
    }

    @Override
    public Optional<T> getByIdOptional(ID id) {
        return repository.findById(id);
    }

    @Override
    public Page<T> getAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public List<T> getAll() {
        return repository.findAll();
    }

    @Override
    public T create(T entity) {
        T saved = repository.save(entity);
        log.debug("Created {}: {}", getEntityName(), saved);
        return saved;
    }

    @Override
    public T update(ID id, T entity) {
        // Verify entity exists
        getById(id);
        T updated = repository.save(entity);
        log.debug("Updated {}: {}", getEntityName(), id);
        return updated;
    }

    @Override
    public void delete(ID id) {
        // Verify entity exists
        getById(id);
        repository.deleteById(id);
        log.debug("Deleted {}: {}", getEntityName(), id);
    }

    @Override
    public boolean exists(ID id) {
        return repository.existsById(id);
    }

    @Override
    public long count() {
        return repository.count();
    }
}
