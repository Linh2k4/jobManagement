import { createReducer, on } from '@ngrx/store';
import * as KpiActions from './kpi.actions';
import { initialKpiState, KpiState } from './kpi.state';

export const kpiReducer = createReducer(
  initialKpiState,

  on(KpiActions.loadCurrentUserKpi, state => ({
    ...state,
    loadingCurrentKpi: true,
    error: null
  })),

  on(KpiActions.loadCurrentUserKpiSuccess, (state, { kpi }) => ({
    ...state,
    currentUserKpi: kpi,
    lastUpdated: new Date().toISOString(),
    cacheExpiry: new Date(Date.now() + 5 * 60 * 1000).toISOString(),
    loadingCurrentKpi: false,
    isCached: true,
    error: null
  })),

  on(KpiActions.loadCurrentUserKpiFailure, (state, { error }) => ({
    ...state,
    loadingCurrentKpi: false,
    error,
    errorType: 'FETCH' as const,
    isCached: false
  })),

  on(KpiActions.loadTeamKpiSummary, state => ({
    ...state,
    loadingTeamKpi: true,
    error: null
  })),

  on(KpiActions.loadTeamKpiSummarySuccess, (state, { kpis, totalElements, page, size }) => ({
    ...state,
    teamKpi: kpis,
    teamKpiTotal: totalElements,
    teamKpiPage: page,
    teamKpiSize: size,
    loadingTeamKpi: false,
    error: null
  })),

  on(KpiActions.loadTeamKpiSummaryFailure, (state, { error }) => ({
    ...state,
    loadingTeamKpi: false,
    error,
    errorType: 'FETCH' as const
  })),

  on(KpiActions.loadKpiHistory, state => ({
    ...state,
    loadingHistory: true,
    error: null
  })),

  on(KpiActions.loadKpiHistorySuccess, (state, { kpiHistory }) => ({
    ...state,
    kpiHistory,
    loadingHistory: false,
    error: null
  })),

  on(KpiActions.loadKpiHistoryFailure, (state, { error }) => ({
    ...state,
    loadingHistory: false,
    error,
    errorType: 'FETCH' as const
  })),

  on(KpiActions.loadKpiConfig, state => ({
    ...state,
    loadingConfig: true,
    error: null
  })),

  on(KpiActions.loadKpiConfigSuccess, (state, { config }) => ({
    ...state,
    config,
    loadingConfig: false,
    error: null
  })),

  on(KpiActions.loadKpiConfigFailure, (state, { error }) => ({
    ...state,
    loadingConfig: false,
    error,
    errorType: 'FETCH' as const
  })),

  on(KpiActions.updateKpiConfig, state => ({
    ...state,
    updatingConfig: true,
    error: null
  })),

  on(KpiActions.updateKpiConfigSuccess, (state, { config }) => ({
    ...state,
    config,
    updatingConfig: false,
    error: null
  })),

  on(KpiActions.updateKpiConfigFailure, (state, { error }) => ({
    ...state,
    updatingConfig: false,
    error,
    errorType: 'CONFIG_UPDATE' as const
  })),

  on(KpiActions.invalidateKpiCache, state => ({
    ...state,
    isCached: false,
    cacheExpiry: null
  })),

  on(KpiActions.setKpiTeamPage, (state, { page }) => ({
    ...state,
    teamKpiPage: page
  })),

  on(KpiActions.clearKpiError, state => ({
    ...state,
    error: null,
    errorType: null
  }))
);
