package com.company.jobmanagement.facade.evaluation;

import com.company.jobmanagement.model.entity.Evaluation;
import com.company.jobmanagement.service.EvaluationService;
import com.company.jobmanagement.service.KpiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Orchestration layer for evaluation flows.
 * Delegates to EvaluationService and KpiService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EvaluationFacade {

    private final EvaluationService evaluationService;
    private final KpiService kpiService;

    /**
     * Get evaluation by ID - delegates to service.
     */
    @Transactional(readOnly = true)
    public Evaluation getEvaluation(Long evaluationId) {
        log.debug("Fetching evaluation {}", evaluationId);
        return evaluationService.getEvaluation(evaluationId);
    }

    /**
     * Get KPI for user.
     */
    public Object getKpi(Long userId, LocalDate periodMonth) {
        log.debug("Fetching KPI for user {} period {}", userId, periodMonth);
        return kpiService.calculateKpi(userId, periodMonth);
    }

    /**
     * Get team KPI.
     */
    public Object getTeamKpi(Long leaderId, LocalDate periodMonth) {
        log.debug("Fetching team KPI for leader {} period {}", leaderId, periodMonth);
        return kpiService.calculateTeamKpi(leaderId, periodMonth);
    }
}
