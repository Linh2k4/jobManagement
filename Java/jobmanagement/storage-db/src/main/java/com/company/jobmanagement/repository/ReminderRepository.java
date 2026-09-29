package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.Reminder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    @Query("SELECT r FROM Reminder r WHERE r.user.id = :userId AND r.isActive = true ORDER BY r.remindAt ASC")
    List<Reminder> findActiveRemindersForUser(@Param("userId") Long userId);

    @Query("SELECT r FROM Reminder r WHERE r.user.id = :userId AND r.isActive = true ORDER BY r.remindAt ASC")
    Page<Reminder> findActiveRemindersForUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT r FROM Reminder r WHERE r.isActive = true AND r.remindAt <= :now ORDER BY r.remindAt ASC")
    List<Reminder> findRemindersReadyToSend(@Param("now") ZonedDateTime now);

    @Query("SELECT r FROM Reminder r WHERE r.task.id = :taskId AND r.isActive = true ORDER BY r.remindAt ASC")
    List<Reminder> findByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT r FROM Reminder r WHERE r.user.id = :userId AND r.task.id = :taskId AND r.isActive = true")
    List<Reminder> findByUserAndTask(@Param("userId") Long userId, @Param("taskId") Long taskId);
}
