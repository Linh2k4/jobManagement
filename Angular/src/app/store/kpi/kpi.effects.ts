import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, mergeMap, catchError, switchMap } from 'rxjs/operators';
import { KpiService } from '../../core/services/api/kpi.service';
import * as KpiActions from './kpi.actions';

@Injectable()
export class KpiEffects {
  private actions$ = inject(Actions);
  private kpiService = inject(KpiService);

  loadCurrentUserKpi$ = createEffect(() =>
    this.actions$.pipe(
      ofType(KpiActions.loadCurrentUserKpi),
      switchMap(() =>
        this.kpiService.getCurrentUserKPI().pipe(
          map(response => KpiActions.loadCurrentUserKpiSuccess({ kpi: response.data })),
          catchError(error => of(KpiActions.loadCurrentUserKpiFailure({
            error: error.message || 'Failed to load KPI'
          })))
        )
      )
    )
  );

  loadTeamKpiSummary$ = createEffect(() =>
    this.actions$.pipe(
      ofType(KpiActions.loadTeamKpiSummary),
      switchMap(({ page = 0, size = 10, sort }) =>
        this.kpiService.getTeamKPISummary(page, size, sort).pipe(
          map(response => KpiActions.loadTeamKpiSummarySuccess({
            kpis: response.data.content,
            totalElements: response.data.totalElements,
            page: response.data.number,
            size: response.data.size
          })),
          catchError(error => of(KpiActions.loadTeamKpiSummaryFailure({
            error: error.message || 'Failed to load team KPI summary'
          })))
        )
      )
    )
  );

  loadKpiHistory$ = createEffect(() =>
    this.actions$.pipe(
      ofType(KpiActions.loadKpiHistory),
      mergeMap(({ userId }) =>
        this.kpiService.getKPIHistory(userId).pipe(
          map(response => KpiActions.loadKpiHistorySuccess({
            kpiHistory: response.data
          })),
          catchError(error => of(KpiActions.loadKpiHistoryFailure({
            error: error.message || 'Failed to load KPI history'
          })))
        )
      )
    )
  );

  loadKpiConfig$ = createEffect(() =>
    this.actions$.pipe(
      ofType(KpiActions.loadKpiConfig),
      switchMap(() =>
        this.kpiService.getKPIConfig().pipe(
          map(response => KpiActions.loadKpiConfigSuccess({ config: response.data })),
          catchError(error => of(KpiActions.loadKpiConfigFailure({
            error: error.message || 'Failed to load KPI config'
          })))
        )
      )
    )
  );

  updateKpiConfig$ = createEffect(() =>
    this.actions$.pipe(
      ofType(KpiActions.updateKpiConfig),
      mergeMap(({ config }) =>
        this.kpiService.updateKPIConfig(config).pipe(
          map(() => KpiActions.loadKpiConfig()), // Reload config after update
          catchError(error => of(KpiActions.updateKpiConfigFailure({
            error: error.message || 'Failed to update KPI config'
          })))
        )
      )
    )
  );

  invalidateCache$ = createEffect(() =>
    this.actions$.pipe(
      ofType(KpiActions.invalidateKpiCache),
      mergeMap(({ userId }) =>
        this.kpiService.invalidateKPICache(userId).pipe(
          map(() => KpiActions.loadCurrentUserKpi()),
          catchError(() => of(KpiActions.loadCurrentUserKpi())) // Silently retry load
        )
      )
    )
  );

}
