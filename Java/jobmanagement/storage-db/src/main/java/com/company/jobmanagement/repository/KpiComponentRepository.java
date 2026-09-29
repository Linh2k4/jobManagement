package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.KpiComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface KpiComponentRepository extends JpaRepository<KpiComponent, Long> {

    // Exact-match on groupId including null (not "null means match any row")
    // — this identifies one specific (user, group, period) row, mirroring
    // the DB's unique index from V20.
    @Query("SELECT kc FROM KpiComponent kc JOIN FETCH kc.user WHERE kc.user.id = :userId " +
           "AND ((:groupId IS NULL AND kc.group IS NULL) OR kc.group.id = :groupId) AND kc.periodMonth = :periodMonth")
    Optional<KpiComponent> findByUserAndGroupAndPeriod(@Param("userId") Long userId, @Param("groupId") Long groupId,
                                                         @Param("periodMonth") YearMonth periodMonth);

    @Query("SELECT kc FROM KpiComponent kc JOIN FETCH kc.user WHERE kc.user.id = :userId ORDER BY kc.periodMonth DESC LIMIT 12")
    List<KpiComponent> findRecentByUser(@Param("userId") Long userId);

    @Query("SELECT kc FROM KpiComponent kc WHERE kc.periodMonth = :periodMonth ORDER BY kc.kpiFinal DESC")
    List<KpiComponent> findByPeriodOrderedByScore(@Param("periodMonth") YearMonth periodMonth);
}
