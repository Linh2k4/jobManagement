package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    @Query("SELECT c FROM TaskComment c JOIN FETCH c.user WHERE c.task.id = :taskId ORDER BY c.createdAt ASC")
    List<TaskComment> findByTaskIdOrderByCreatedAtAsc(@Param("taskId") Long taskId);

    @Query("SELECT c.task.id, COUNT(c) FROM TaskComment c WHERE c.task.id IN :taskIds GROUP BY c.task.id")
    List<Object[]> countByTaskIds(@Param("taskIds") List<Long> taskIds);
}
