package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.TaskPrivateNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskPrivateNoteRepository extends JpaRepository<TaskPrivateNote, Long> {

    @Query("SELECT n FROM TaskPrivateNote n WHERE n.task.id = :taskId AND n.user.id = :userId")
    Optional<TaskPrivateNote> findByTaskIdAndUserId(@Param("taskId") Long taskId, @Param("userId") Long userId);
}
