package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.GroupMembership;
import com.company.jobmanagement.model.entity.KpiComponent;
import com.company.jobmanagement.repository.GroupMembershipRepository;
import com.company.jobmanagement.repository.KpiComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

/**
 * KPI caching service using Spring Cache with Redis backend.
 * Reduces database queries by caching real-time KPI calculations.
 * 5-minute TTL with event-based invalidation.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class KpiCacheService {

    private final KpiComponentRepository kpiComponentRepository;
    private final KpiCalculationService kpiCalculationService;
    private final GroupMembershipRepository groupMembershipRepository;

    /**
     * Get or calculate cached KPI for a user, scoped to one Group
     * (Scope.md §13 — null groupId keeps the pre-§13 single-KPI behavior).
     * Cache key: kpi:{userId}:{groupId}:{periodMonth}
     * TTL: 5 minutes
     */
    @Cacheable(value = "kpi", key = "#userId + ':' + #groupId + ':' + #periodMonth", unless = "#result == null")
    @Transactional
    public KpiComponent getOrCalculateKpi(Long userId, Long groupId, YearMonth periodMonth) {
        log.debug("Cache miss for KPI: userId={}, groupId={}, period={}", userId, groupId, periodMonth);

        KpiComponent cached = kpiComponentRepository.findByUserAndGroupAndPeriod(userId, groupId, periodMonth).orElse(null);

        if (cached == null) {
            cached = kpiCalculationService.calculateKpi(userId, groupId, periodMonth);
            cached.setRanking(cached.calculateRanking());
            cached.setTrend(cached.calculateTrend());
            kpiComponentRepository.save(cached);
        }

        // Force-init lazy associations while the transaction/session is still open —
        // callers (and the Redis cache serializer) access these after this method returns.
        Hibernate.initialize(cached.getGroup());

        return cached;
    }

    /**
     * Invalidate cached KPI for a user in one group.
     * Called when task status changes or difficulty is updated.
     */
    @CacheEvict(value = "kpi", key = "#userId + ':' + #groupId + ':' + #periodMonth")
    public void invalidateKpi(Long userId, Long groupId, YearMonth periodMonth) {
        log.debug("Invalidated KPI cache: userId={}, groupId={}, period={}", userId, groupId, periodMonth);
    }

    /**
     * Invalidate current month's KPI for a user across every group they
     * belong to — a task status change doesn't know in isolation which
     * group cache entry it affected, so evict them all rather than risk
     * leaving a stale one behind.
     */
    public void invalidateCurrentMonthKpi(Long userId) {
        invalidateKpi(userId, YearMonth.now());
    }

    /**
     * Invalidate a user's KPI across every group they belong to, for one
     * period. Callers that don't know (or don't care about) a specific
     * group should use this instead of guessing a groupId.
     */
    public void invalidateKpi(Long userId, YearMonth periodMonth) {
        invalidateKpi(userId, null, periodMonth);
        for (GroupMembership gm : groupMembershipRepository.findByMemberId(userId)) {
            invalidateKpi(userId, gm.getGroup().getId(), periodMonth);
        }
    }

    /**
     * Invalidate all KPI for a user (all periods).
     * Used when difficulty is changed retroactively.
     */
    @CacheEvict(value = "kpi", allEntries = true)
    public void invalidateAllKpi() {
        log.debug("Invalidated all KPI caches");
    }

    /**
     * Invalidate team's KPI (Lead's team members).
     * Called when team member's KPI changes.
     */
    public void invalidateTeamKpi(Long leadId, YearMonth periodMonth) {
        // This would need list of team members
        // Implementation depends on User entity structure
        log.debug("Invalidated team KPI cache: leadId={}, period={}", leadId, periodMonth);
    }
}
