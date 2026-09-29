package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.Group;
import com.company.jobmanagement.model.entity.KpiComponent;
import com.company.jobmanagement.repository.GroupMembershipRepository;
import com.company.jobmanagement.repository.KpiComponentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Regression test for the LazyInitializationException fixed in getOrCalculateKpi:
 * KpiComponent.group is FetchType.LAZY, and callers (the KPI controller, and the
 * Redis cache serializer backing @Cacheable) read it after this method's own
 * @Transactional boundary has closed. The fix must force-initialize the proxy
 * while the transaction is still open, before returning.
 */
@ExtendWith(MockitoExtension.class)
class KpiCacheServiceTest {

    @Mock
    private KpiComponentRepository kpiComponentRepository;
    @Mock
    private KpiCalculationService kpiCalculationService;
    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    private KpiCacheService kpiCacheService;

    private static final Long USER_ID = 4L;
    private static final Long GROUP_ID = 3L;
    private static final YearMonth PERIOD = YearMonth.of(2026, 8);

    @BeforeEach
    void setUp() {
        kpiCacheService = new KpiCacheService(kpiComponentRepository, kpiCalculationService, groupMembershipRepository);
    }

    @Test
    void getOrCalculateKpi_cacheMiss_initializesGroupBeforeReturning() {
        Group group = Group.builder().id(GROUP_ID).name("Group A").build();
        KpiComponent calculated = spy(KpiComponent.builder()
                .group(group)
                .kpiFinal(java.math.BigDecimal.valueOf(15))
                .build());

        when(kpiComponentRepository.findByUserAndGroupAndPeriod(USER_ID, GROUP_ID, PERIOD))
                .thenReturn(Optional.empty());
        when(kpiCalculationService.calculateKpi(USER_ID, GROUP_ID, PERIOD)).thenReturn(calculated);

        KpiComponent result = kpiCacheService.getOrCalculateKpi(USER_ID, GROUP_ID, PERIOD);

        // The fix calls cached.getGroup() itself (Hibernate.initialize) — assert that
        // interaction happened, so a regression that deletes the initialize call fails here.
        verify(calculated, atLeastOnce()).getGroup();
        assertThat(result.getGroup()).isNotNull();
        assertThat(result.getGroup().getName()).isEqualTo("Group A");
        verify(kpiComponentRepository).save(calculated);
    }

    @Test
    void getOrCalculateKpi_cacheHit_stillInitializesGroupBeforeReturning() {
        Group group = Group.builder().id(GROUP_ID).name("Group A").build();
        KpiComponent cached = spy(KpiComponent.builder().group(group).build());

        when(kpiComponentRepository.findByUserAndGroupAndPeriod(USER_ID, GROUP_ID, PERIOD))
                .thenReturn(Optional.of(cached));

        KpiComponent result = kpiCacheService.getOrCalculateKpi(USER_ID, GROUP_ID, PERIOD);

        verify(cached, atLeastOnce()).getGroup();
        assertThat(result.getGroup().getName()).isEqualTo("Group A");
        verify(kpiCalculationService, never()).calculateKpi(anyLong(), any(), any());
        verify(kpiComponentRepository, never()).save(any());
    }

    @Test
    void getOrCalculateKpi_nullGroup_doesNotThrow() {
        KpiComponent calculated = spy(KpiComponent.builder().group(null).build());

        when(kpiComponentRepository.findByUserAndGroupAndPeriod(USER_ID, null, PERIOD))
                .thenReturn(Optional.empty());
        when(kpiCalculationService.calculateKpi(USER_ID, null, PERIOD)).thenReturn(calculated);

        KpiComponent result = kpiCacheService.getOrCalculateKpi(USER_ID, null, PERIOD);

        assertThat(result.getGroup()).isNull();
    }
}
