package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.TaskTypeGroupSubtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskTypeGroupSubtaskRepository extends JpaRepository<TaskTypeGroupSubtask, Long> {

    @Query("SELECT g FROM TaskTypeGroupSubtask g WHERE g.taskType.id = :taskTypeId ORDER BY g.groupOrder ASC")
    List<TaskTypeGroupSubtask> findByTaskTypeId(@Param("taskTypeId") Long taskTypeId);
}
