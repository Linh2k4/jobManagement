package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.DeadlineExtensionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeadlineExtensionRequestRepository extends JpaRepository<DeadlineExtensionRequest, Long> {

    @Query("SELECT r FROM DeadlineExtensionRequest r JOIN FETCH r.requestedBy JOIN FETCH r.task " +
           "LEFT JOIN FETCH r.reviewedBy WHERE r.task.id = :taskId ORDER BY r.createdAt DESC")
    List<DeadlineExtensionRequest> findByTaskIdOrderByCreatedAtDesc(@Param("taskId") Long taskId);

    @Query("SELECT r FROM DeadlineExtensionRequest r JOIN FETCH r.requestedBy JOIN FETCH r.task " +
           "WHERE r.task.id = :taskId AND r.status = 'PENDING'")
    Optional<DeadlineExtensionRequest> findPendingByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT COUNT(r) FROM DeadlineExtensionRequest r WHERE r.task.id = :taskId AND r.status = 'APPROVED'")
    long countApprovedByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT COALESCE(MAX(r.extensionNumber), 0) FROM DeadlineExtensionRequest r WHERE r.task.id = :taskId")
    int findMaxExtensionNumber(@Param("taskId") Long taskId);

    /** Manager sees every pending request system-wide. */
    @Query("SELECT r FROM DeadlineExtensionRequest r JOIN FETCH r.requestedBy JOIN FETCH r.task t " +
           "WHERE r.status = 'PENDING' ORDER BY r.createdAt ASC")
    List<DeadlineExtensionRequest> findAllPending();

    /** Lead sees pending requests from their own team members. */
    @Query("SELECT r FROM DeadlineExtensionRequest r JOIN FETCH r.requestedBy rb JOIN FETCH r.task t " +
           "WHERE r.status = 'PENDING' AND rb.lead.id = :leadId ORDER BY r.createdAt ASC")
    List<DeadlineExtensionRequest> findPendingForLeadTeam(@Param("leadId") Long leadId);

    @Query("SELECT r FROM DeadlineExtensionRequest r JOIN FETCH r.requestedBy JOIN FETCH r.task " +
           "WHERE r.status = 'PENDING' AND r.expiresAt < :now")
    List<DeadlineExtensionRequest> findExpired(@Param("now") ZonedDateTime now);
}
