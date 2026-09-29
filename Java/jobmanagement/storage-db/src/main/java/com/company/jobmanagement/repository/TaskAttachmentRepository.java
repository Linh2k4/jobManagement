package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {

    @Query("SELECT a FROM TaskAttachment a JOIN FETCH a.uploadedBy WHERE a.task.id = :taskId ORDER BY a.createdAt DESC")
    List<TaskAttachment> findByTaskIdOrderByCreatedAtDesc(@Param("taskId") Long taskId);

    @Query("SELECT a.task.id, COUNT(a) FROM TaskAttachment a WHERE a.task.id IN :taskIds GROUP BY a.task.id")
    List<Object[]> countByTaskIds(@Param("taskIds") List<Long> taskIds);
}
