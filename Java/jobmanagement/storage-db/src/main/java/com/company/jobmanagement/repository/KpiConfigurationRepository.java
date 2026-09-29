package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.KpiConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.Optional;

@Repository
public interface KpiConfigurationRepository extends JpaRepository<KpiConfiguration, Long> {

    @Query("SELECT kc FROM KpiConfiguration kc WHERE kc.periodMonth = :periodMonth")
    Optional<KpiConfiguration> findByPeriod(@Param("periodMonth") YearMonth periodMonth);

    @Query("SELECT kc FROM KpiConfiguration kc ORDER BY kc.periodMonth DESC LIMIT 1")
    Optional<KpiConfiguration> findLatest();
}
