package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.GroupSubtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupSubtaskRepository extends JpaRepository<GroupSubtask, Long> {

    @Query("SELECT g FROM GroupSubtask g WHERE g.task.id = :taskId ORDER BY g.groupOrder ASC")
    List<GroupSubtask> findByTaskId(@Param("taskId") Long taskId);
}
