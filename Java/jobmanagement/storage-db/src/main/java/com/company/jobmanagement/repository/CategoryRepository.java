package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.code = :code")
    Optional<Category> findByCode(@Param("code") String code);

    // extraFields is mapped into CategoryResponse — eager-fetch it (single
    // collection alongside the single-valued owner association, so no
    // MultipleBagFetchException) or mapping crashes with
    // LazyInitializationException once a session-less controller touches it.
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.extraFields WHERE c.id = :id")
    Optional<Category> findByIdWithExtraFields(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.extraFields WHERE c.tier = 'SYSTEM' AND c.status = 'ACTIVE' ORDER BY c.sortOrder ASC")
    List<Category> findAllSystemCategories();

    @Query("SELECT c FROM Category c WHERE c.tier = 'SYSTEM' AND c.status = 'ACTIVE' ORDER BY c.sortOrder ASC")
    Page<Category> findAllSystemCategories(Pageable pageable);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.extraFields WHERE c.owner.id = :ownerId AND c.status = 'ACTIVE' ORDER BY c.sortOrder ASC")
    List<Category> findCustomCategoriesByOwner(@Param("ownerId") Long ownerId);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.extraFields WHERE c.owner.id = :ownerId AND c.status = 'ACTIVE' ORDER BY c.sortOrder ASC")
    Page<Category> findCustomCategoriesByOwner(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.extraFields WHERE c.id IN :ids")
    List<Category> findAllWithExtraFields(@Param("ids") List<Long> ids);

    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.extraFields WHERE c.status = 'ACTIVE' ORDER BY c.sortOrder ASC")
    List<Category> findAllActiveWithExtraFields();

    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.extraFields " +
           "WHERE (c.tier = 'SYSTEM' OR c.owner.id = :ownerId) AND c.status = 'ACTIVE' ORDER BY c.sortOrder ASC")
    List<Category> findVisibleCategoriesForUser(@Param("ownerId") Long ownerId);

    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.extraFields WHERE c.name ILIKE :keyword AND c.status = 'ACTIVE'")
    List<Category> searchByName(@Param("keyword") String keyword);

    @Query("SELECT COUNT(c) FROM Category c WHERE c.tier = 'CUSTOM' AND c.owner.id = :ownerId AND c.status = 'ACTIVE'")
    long countCustomCategoriesForOwner(@Param("ownerId") Long ownerId);

    /**
     * Any ACTIVE category that would collide with `name`: a System category
     * sharing it (checked regardless of scope, System names are global), or
     * (when scopeOwnerId is set) another CUSTOM category owned by the same
     * Lead sharing it. Excludes `excludeCategoryId` so updating a category's
     * own unchanged name doesn't flag itself.
     */
    @Query("SELECT c FROM Category c WHERE c.status = 'ACTIVE' " +
           "AND LOWER(c.name) = LOWER(:name) " +
           "AND (:excludeCategoryId IS NULL OR c.id != :excludeCategoryId) " +
           "AND (c.tier = 'SYSTEM' OR (:scopeOwnerId IS NOT NULL AND c.owner.id = :scopeOwnerId))")
    List<Category> findConflictingName(@Param("name") String name, @Param("scopeOwnerId") Long scopeOwnerId,
                                        @Param("excludeCategoryId") Long excludeCategoryId);
}
