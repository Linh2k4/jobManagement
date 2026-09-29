package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.Subtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubtaskRepository extends JpaRepository<Subtask, Long> {

    @Query("SELECT s FROM Subtask s WHERE s.task.id = :taskId ORDER BY s.subtaskOrder ASC")
    List<Subtask> findByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT s FROM Subtask s WHERE s.groupSubtask.id = :groupSubtaskId ORDER BY s.subtaskOrder ASC")
    List<Subtask> findByGroupSubtaskId(@Param("groupSubtaskId") Long groupSubtaskId);

    /** FAST-task Kanban progress: Subtask is the leaf, count directly. */
    @Query("SELECT s.task.id, COUNT(s) FROM Subtask s WHERE s.task.id IN :taskIds GROUP BY s.task.id")
    List<Object[]> countByTaskIds(@Param("taskIds") List<Long> taskIds);

    @Query("SELECT s.task.id, COUNT(s) FROM Subtask s WHERE s.task.id IN :taskIds AND s.status = com.company.jobmanagement.model.enums.StepStatus.DONE GROUP BY s.task.id")
    List<Object[]> countDoneByTaskIds(@Param("taskIds") List<Long> taskIds);
}
