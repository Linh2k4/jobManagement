package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.enums.TimeCategory;
import com.company.jobmanagement.model.entity.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskTypeRepository extends JpaRepository<TaskType, Long> {

    Optional<TaskType> findByCodeAndIsActiveTrue(String code);

    // subtasks is flattened (with its steps) into TaskTypeResponse.steps.
    // Hibernate rejects JOIN FETCHing two bags in one query even when
    // chained (subtasks -> steps), not just when parallel — same
    // MultipleBagFetchException class as elsewhere this session. Fetch
    // subtasks here; TaskTypeService touches each subtask's steps
    // separately, still inside the transaction.
    @Query("SELECT DISTINCT t FROM TaskType t LEFT JOIN FETCH t.subtasks WHERE t.isActive = true")
    List<TaskType> findAllActive();

    @Query("SELECT t FROM TaskType t LEFT JOIN FETCH t.subtasks WHERE t.id = :id")
    Optional<TaskType> findByIdWithSteps(@Param("id") Long id);

    @Query("SELECT t FROM TaskType t WHERE t.timeCategory = :timeCategory AND t.isActive = true")
    List<TaskType> findByTimeCategory(@Param("timeCategory") TimeCategory timeCategory);

    @Query("SELECT t FROM TaskType t WHERE t.isMultiStep = true AND t.isActive = true")
    List<TaskType> findAllMultiStepActive();
}
