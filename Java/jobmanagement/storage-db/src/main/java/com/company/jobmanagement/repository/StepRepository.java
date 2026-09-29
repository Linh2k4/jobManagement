package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.Step;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StepRepository extends JpaRepository<Step, Long> {

    // assignee is mapped into StepResponse — eager-fetch it (single-valued,
    // safe alongside any other single-valued association) or mapping
    // crashes with LazyInitializationException once a session-less
    // controller touches an assigned step.
    @Query("SELECT s FROM Step s LEFT JOIN FETCH s.assignee WHERE s.subtask.id = :subtaskId ORDER BY s.stepOrder ASC")
    List<Step> findBySubtaskId(@Param("subtaskId") Long subtaskId);

    @Query("SELECT s FROM Step s WHERE s.assignee.id = :userId")
    List<Step> findByAssigneeId(@Param("userId") Long userId);

    /** MULTI_STEP-task Kanban progress: leaf is Step, one join up to Task. */
    @Query("SELECT s.subtask.task.id, COUNT(s) FROM Step s WHERE s.subtask.task.id IN :taskIds GROUP BY s.subtask.task.id")
    List<Object[]> countByTaskIds(@Param("taskIds") List<Long> taskIds);

    @Query("SELECT s.subtask.task.id, COUNT(s) FROM Step s WHERE s.subtask.task.id IN :taskIds AND s.status = com.company.jobmanagement.model.enums.StepStatus.DONE GROUP BY s.subtask.task.id")
    List<Object[]> countDoneByTaskIds(@Param("taskIds") List<Long> taskIds);
}
