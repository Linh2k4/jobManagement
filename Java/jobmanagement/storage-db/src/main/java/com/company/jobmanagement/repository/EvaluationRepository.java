package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.Evaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    // Exact-match on groupId including null (not "null means match any row")
    // — identifies one specific (user, group, period) row, mirroring the
    // DB's unique index from V20 and KpiComponentRepository's same pattern.
    @Query("SELECT e FROM Evaluation e JOIN FETCH e.user JOIN FETCH e.manager " +
           "LEFT JOIN FETCH e.lead LEFT JOIN FETCH e.group WHERE e.user.id = :userId " +
           "AND ((:groupId IS NULL AND e.group IS NULL) OR e.group.id = :groupId) AND e.periodMonth = :periodMonth")
    Optional<Evaluation> findByUserAndGroupAndPeriod(@Param("userId") Long userId, @Param("groupId") Long groupId,
                                                       @Param("periodMonth") YearMonth periodMonth);

    // Same associations as findByUserAndGroupAndPeriod — used wherever a
    // single Evaluation is serialized straight to JSON by the controller
    // (open-in-view=false, so lazy user/lead/manager/group must already be
    // loaded by the time the @Transactional service method returns).
    @Query("SELECT e FROM Evaluation e JOIN FETCH e.user JOIN FETCH e.manager " +
           "LEFT JOIN FETCH e.lead LEFT JOIN FETCH e.group WHERE e.id = :id")
    Optional<Evaluation> findByIdWithAssociations(@Param("id") Long id);

    // "Does the user have any evaluation this period" — deliberately not
    // group-scoped, used for existence checks (deadline reminders) and for
    // paginated per-user listing where a user can now have one row per group.
    @Query("SELECT e FROM Evaluation e WHERE e.user.id = :userId AND e.periodMonth = :periodMonth")
    List<Evaluation> findAllByUserAndPeriod(@Param("userId") Long userId, @Param("periodMonth") YearMonth periodMonth);

    @Query(value = "SELECT e FROM Evaluation e JOIN FETCH e.user JOIN FETCH e.manager " +
           "LEFT JOIN FETCH e.lead LEFT JOIN FETCH e.group WHERE e.user.id = :userId AND e.periodMonth = :periodMonth ORDER BY e.id ASC",
           countQuery = "SELECT count(e) FROM Evaluation e WHERE e.user.id = :userId AND e.periodMonth = :periodMonth")
    Page<Evaluation> findByUserAndPeriodPaged(@Param("userId") Long userId, @Param("periodMonth") YearMonth periodMonth, Pageable pageable);

    @Query(value = "SELECT e FROM Evaluation e JOIN FETCH e.user JOIN FETCH e.manager " +
           "LEFT JOIN FETCH e.lead LEFT JOIN FETCH e.group WHERE e.lead.id = :leadId AND e.periodMonth = :periodMonth ORDER BY e.user.fullName ASC",
           countQuery = "SELECT count(e) FROM Evaluation e WHERE e.lead.id = :leadId AND e.periodMonth = :periodMonth")
    Page<Evaluation> findTeamEvaluations(@Param("leadId") Long leadId, @Param("periodMonth") YearMonth periodMonth, Pageable pageable);

    @Query("SELECT e FROM Evaluation e JOIN FETCH e.user JOIN FETCH e.manager " +
           "LEFT JOIN FETCH e.lead LEFT JOIN FETCH e.group WHERE e.periodMonth = :periodMonth ORDER BY e.kpiFinal DESC")
    List<Evaluation> findByPeriodOrderedByKpi(@Param("periodMonth") YearMonth periodMonth);

    @Query("SELECT e FROM Evaluation e WHERE e.user.id = :userId ORDER BY e.periodMonth DESC LIMIT 12")
    List<Evaluation> findRecentByUser(@Param("userId") Long userId);

    @Query("SELECT e FROM Evaluation e WHERE e.isLocked = false AND e.status = 'DRAFT'")
    List<Evaluation> findPendingSelfEvaluations();
}
