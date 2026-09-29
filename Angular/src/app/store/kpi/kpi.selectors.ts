import { createFeatureSelector, createSelector } from '@ngrx/store';
import { KpiState } from './kpi.state';

export const selectKpiState = createFeatureSelector<KpiState>('kpi');

export const selectCurrentUserKpi = createSelector(
  selectKpiState,
  (state: KpiState) => state.currentUserKpi
);

export const selectTeamKpi = createSelector(
  selectKpiState,
  (state: KpiState) => state.teamKpi
);

export const selectKpiHistory = createSelector(
  selectKpiState,
  (state: KpiState) => state.kpiHistory
);

export const selectKpiConfig = createSelector(
  selectKpiState,
  (state: KpiState) => state.config
);

export const selectKpiLoading = createSelector(
  selectKpiState,
  (state: KpiState) => state.loadingCurrentKpi || state.loadingTeamKpi || state.loadingHistory
);

export const selectKpiError = createSelector(
  selectKpiState,
  (state: KpiState) => state.error
);

export const selectKpiPagination = createSelector(
  selectKpiState,
  (state: KpiState) => ({
    page: state.teamKpiPage,
    size: state.teamKpiSize,
    total: state.teamKpiTotal
  })
);

export const selectKpiRanking = createSelector(
  selectTeamKpi,
  (kpis) => {
    return [...kpis].sort((a, b) => (b.kpiFinal || 0) - (a.kpiFinal || 0));
  }
);

export const selectCurrentUserRank = createSelector(
  selectCurrentUserKpi,
  selectKpiRanking,
  (currentKpi, ranking) => {
    if (!currentKpi) return null;
    return ranking.findIndex(k => k.userId === currentKpi.userId) + 1;
  }
);

export const selectTeamKpiTrends = createSelector(
  selectKpiHistory,
  (history) => {
    if (history.length === 0) return null;
    const current = history[0]?.kpiFinal || 0;
    const previous = history[1]?.kpiFinal || 0;
    return {
      current,
      previous,
      change: current - previous,
      percentageChange: previous > 0 ? ((current - previous) / previous) * 100 : 0
    };
  }
);

export const selectTopPerformers = createSelector(
  selectKpiRanking,
  (ranking) => ranking.slice(0, 5)
);

export const selectNeedsImprovement = createSelector(
  selectKpiRanking,
  (ranking) => ranking.filter(k => k.ranking === 'Cần cải thiện' || k.ranking === 'Không đạt')
);

export const selectIsCached = createSelector(
  selectKpiState,
  (state: KpiState) => state.isCached
);

export const selectCacheExpiry = createSelector(
  selectKpiState,
  (state: KpiState) => state.cacheExpiry
);
