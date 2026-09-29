package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    @Query("SELECT lr FROM LeaveRequest lr JOIN FETCH lr.member LEFT JOIN FETCH lr.reviewedBy " +
           "WHERE lr.member.id = :memberId AND EXTRACT(YEAR FROM lr.startDate) = :year " +
           "ORDER BY lr.startDate DESC")
    List<LeaveRequest> findByMemberIdAndYear(@Param("memberId") Long memberId, @Param("year") int year);

    /** Manager sees every pending request. */
    @Query("SELECT lr FROM LeaveRequest lr JOIN FETCH lr.member WHERE lr.status = 'PENDING' ORDER BY lr.createdAt ASC")
    List<LeaveRequest> findAllPending();

    /** Lead sees pending requests from their own team members. */
    @Query("SELECT lr FROM LeaveRequest lr JOIN FETCH lr.member m " +
           "WHERE lr.status = 'PENDING' AND m.lead.id = :leadId ORDER BY lr.createdAt ASC")
    List<LeaveRequest> findPendingForLeadTeam(@Param("leadId") Long leadId);

    @Query("SELECT lr FROM LeaveRequest lr JOIN FETCH lr.member LEFT JOIN FETCH lr.reviewedBy WHERE lr.id = :id")
    Optional<LeaveRequest> findByIdWithMember(@Param("id") Long id);
}
