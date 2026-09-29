package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.TaskAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.task.id = :taskId AND ta.isCurrent = true")
    Optional<TaskAssignment> findCurrentAssignment(@Param("taskId") Long taskId);

    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.task.id = :taskId ORDER BY ta.assignedAt DESC")
    List<TaskAssignment> findAssignmentHistory(@Param("taskId") Long taskId);

    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.assignee.id = :userId AND ta.isCurrent = true")
    List<TaskAssignment> findCurrentAssignmentsForUser(@Param("userId") Long userId);

    @Query("SELECT ta FROM TaskAssignment ta JOIN FETCH ta.assignee WHERE ta.task.id IN :taskIds AND ta.isCurrent = true")
    List<TaskAssignment> findCurrentAssignmentsForTasks(@Param("taskIds") List<Long> taskIds);

    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.task.id = :taskId ORDER BY ta.assignedAt DESC")
    Page<TaskAssignment> findByTaskIdOrderByAssignedAtDesc(@Param("taskId") Long taskId, Pageable pageable);
}
