package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.KpiSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface KpiSnapshotRepository extends JpaRepository<KpiSnapshot, Long> {

    @Query("SELECT ks FROM KpiSnapshot ks WHERE ks.user.id = :userId AND ks.periodMonth = :periodMonth")
    Optional<KpiSnapshot> findByUserAndPeriod(@Param("userId") Long userId, @Param("periodMonth") YearMonth periodMonth);

    @Query("SELECT ks FROM KpiSnapshot ks WHERE ks.user.id = :userId ORDER BY ks.periodMonth DESC LIMIT 12")
    List<KpiSnapshot> findRecentByUser(@Param("userId") Long userId);

    @Query("SELECT ks FROM KpiSnapshot ks JOIN FETCH ks.user WHERE ks.lead.id = :leadId AND ks.periodMonth = :periodMonth ORDER BY ks.kpiFinal DESC")
    List<KpiSnapshot> findTeamSnapshotsByLead(@Param("leadId") Long leadId, @Param("periodMonth") YearMonth periodMonth);

    @Query("SELECT ks FROM KpiSnapshot ks JOIN FETCH ks.user WHERE ks.periodMonth = :periodMonth AND ks.isLocked = true ORDER BY ks.kpiFinal DESC")
    List<KpiSnapshot> findLockedSnapshotsByPeriod(@Param("periodMonth") YearMonth periodMonth);
}
