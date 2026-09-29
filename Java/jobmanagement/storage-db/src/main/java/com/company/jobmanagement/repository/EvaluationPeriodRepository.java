package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.EvaluationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.Optional;

@Repository
public interface EvaluationPeriodRepository extends JpaRepository<EvaluationPeriod, Long> {

    @Query("SELECT ep FROM EvaluationPeriod ep WHERE ep.periodMonth = :periodMonth")
    Optional<EvaluationPeriod> findByPeriodMonth(@Param("periodMonth") YearMonth periodMonth);

    @Query("SELECT ep FROM EvaluationPeriod ep ORDER BY ep.periodMonth DESC LIMIT 1")
    Optional<EvaluationPeriod> findLatestPeriod();
}
