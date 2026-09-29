package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.TaskTypeSubtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskTypeSubtaskRepository extends JpaRepository<TaskTypeSubtask, Long> {

    /** All subtasks (grouped and ungrouped) for a task type's template, ordered. */
    @Query("SELECT s FROM TaskTypeSubtask s WHERE s.taskType.id = :taskTypeId ORDER BY s.subtaskOrder ASC")
    List<TaskTypeSubtask> findByTaskTypeId(@Param("taskTypeId") Long taskTypeId);
}
