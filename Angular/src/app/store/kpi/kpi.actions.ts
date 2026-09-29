import { createAction, props } from '@ngrx/store';
import { KPI, KpiConfig } from '../../core/models';

export const loadCurrentUserKpi = createAction(
  '[KPI] Load Current User KPI'
);

export const loadCurrentUserKpiSuccess = createAction(
  '[KPI] Load Current User KPI Success',
  props<{ kpi: KPI }>()
);

export const loadCurrentUserKpiFailure = createAction(
  '[KPI] Load Current User KPI Failure',
  props<{ error: string }>()
);

export const loadTeamKpiSummary = createAction(
  '[KPI] Load Team KPI Summary',
  props<{ page?: number; size?: number; sort?: string }>()
);

export const loadTeamKpiSummarySuccess = createAction(
  '[KPI] Load Team KPI Summary Success',
  props<{ kpis: KPI[]; totalElements: number; page: number; size: number }>()
);

export const loadTeamKpiSummaryFailure = createAction(
  '[KPI] Load Team KPI Summary Failure',
  props<{ error: string }>()
);

export const loadKpiHistory = createAction(
  '[KPI] Load KPI History',
  props<{ userId: number }>()
);

export const loadKpiHistorySuccess = createAction(
  '[KPI] Load KPI History Success',
  props<{ kpiHistory: KPI[] }>()
);

export const loadKpiHistoryFailure = createAction(
  '[KPI] Load KPI History Failure',
  props<{ error: string }>()
);

export const loadKpiConfig = createAction(
  '[KPI] Load KPI Config'
);

export const loadKpiConfigSuccess = createAction(
  '[KPI] Load KPI Config Success',
  props<{ config: KpiConfig }>()
);

export const loadKpiConfigFailure = createAction(
  '[KPI] Load KPI Config Failure',
  props<{ error: string }>()
);

export const updateKpiConfig = createAction(
  '[KPI] Update KPI Config',
  props<{ config: Partial<KpiConfig> }>()
);

export const updateKpiConfigSuccess = createAction(
  '[KPI] Update KPI Config Success',
  props<{ config: KpiConfig }>()
);

export const updateKpiConfigFailure = createAction(
  '[KPI] Update KPI Config Failure',
  props<{ error: string }>()
);

export const invalidateKpiCache = createAction(
  '[KPI] Invalidate KPI Cache',
  props<{ userId?: number }>()
);

export const setKpiTeamPage = createAction(
  '[KPI] Set Team KPI Page',
  props<{ page: number }>()
);

export const clearKpiError = createAction(
  '[KPI] Clear KPI Error'
);
