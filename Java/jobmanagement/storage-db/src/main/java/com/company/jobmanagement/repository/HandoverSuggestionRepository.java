package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.HandoverSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HandoverSuggestionRepository extends JpaRepository<HandoverSuggestion, Long> {

    @Query("SELECT h FROM HandoverSuggestion h " +
           "JOIN FETCH h.task " +
           "JOIN FETCH h.leaveRequest lr JOIN FETCH lr.member " +
           "LEFT JOIN FETCH h.step " +
           "LEFT JOIN FETCH h.suggestedAssignee " +
           "LEFT JOIN FETCH h.confirmedAssignee " +
           "WHERE h.leaveRequest.id = :leaveRequestId ORDER BY h.currentDeadline ASC")
    List<HandoverSuggestion> findByLeaveRequestId(@Param("leaveRequestId") Long leaveRequestId);
}
