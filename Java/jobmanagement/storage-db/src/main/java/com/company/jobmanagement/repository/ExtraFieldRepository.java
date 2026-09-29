package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.ExtraField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtraFieldRepository extends JpaRepository<ExtraField, Long> {

    @Query("SELECT ef FROM ExtraField ef WHERE ef.category.id = :categoryId ORDER BY ef.sortOrder ASC")
    List<ExtraField> findByCategoryIdOrderBySortOrder(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(ef) FROM ExtraField ef WHERE ef.category.id = :categoryId")
    long countByCategory(@Param("categoryId") Long categoryId);
}
