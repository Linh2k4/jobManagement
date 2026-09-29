package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.TaskTypeStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskTypeStepRepository extends JpaRepository<TaskTypeStep, Long> {

    @Query("SELECT ts FROM TaskTypeStep ts WHERE ts.taskTypeSubtask.id = :taskTypeSubtaskId ORDER BY ts.stepOrder ASC")
    List<TaskTypeStep> findByTaskTypeSubtaskId(@Param("taskTypeSubtaskId") Long taskTypeSubtaskId);
}
