import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Store } from '@ngrx/store';
import { map } from 'rxjs';
import {
  selectCurrentUserKpi,
  selectTeamKpi,
  selectTeamKpiTrends,
  selectKpiLoading
} from '../../../../store/kpi/kpi.selectors';
import * as KpiActions from '../../../../store/kpi/kpi.actions';
import { KpiCardComponent } from '../../components/kpi-card/kpi-card.component';
import { AuthStore } from '../../../../core/stores/auth.store';

@Component({
  selector: 'app-kpi-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, KpiCardComponent],
  template: `
    <div class="kpi-dashboard-container">
      <div class="dashboard-header">
        <h2>KPI Dashboard</h2>
        <div class="header-actions">
          <button (click)="refreshKPI()" class="btn-refresh"><span class="icon">refresh</span> Refresh</button>
          <select [(ngModel)]="selectedPeriod" (change)="onPeriodChange()" class="period-select">
            <option value="CURRENT">Current Period</option>
            <option value="PREVIOUS">Previous Period</option>
            <option value="YEAR_TO_DATE">Year to Date</option>
          </select>
        </div>
      </div>

      <div class="kpi-section" *ngIf="!(isLoading$ | async)">
        <h3>Your KPI</h3>
        <div class="kpi-grid" *ngIf="currentKPI$ | async as kpi">
          <app-kpi-card
            title="Weighted Completion Rate"
            [value]="kpi.wcr || 0"
            unit="%"
            [trend]="kpi.percentageChange || 0"
            period="This Month"
            [target]="85"
            [maxValue]="100"
            type="PERFORMANCE"
          ></app-kpi-card>

          <app-kpi-card
            title="Volume Index"
            [value]="kpi.vi || 0"
            unit="%"
            period="This Month"
            [target]="100"
            [maxValue]="120"
            type="PRODUCTIVITY"
          ></app-kpi-card>

          <app-kpi-card
            title="Estimate Accuracy"
            [value]="kpi.ea || 0"
            unit="%"
            period="This Month"
            [target]="90"
            [maxValue]="100"
            type="EFFICIENCY"
          ></app-kpi-card>

          <app-kpi-card
            title="KPI Final"
            [value]="kpi.kpiFinal || 0"
            [unit]="' — ' + kpi.ranking"
            period="This Month"
            [target]="80"
            [maxValue]="100"
            type="QUALITY"
          ></app-kpi-card>
        </div>
      </div>

      <div class="kpi-section" *ngIf="!(isLoading$ | async)">
        <h3>Team KPI Summary</h3>
        <div class="team-summary" *ngIf="teamKpiSummary$ | async as teamKpi">
          <div class="summary-card">
            <span class="icon summary-icon">group</span>
            <span class="summary-label">Team Members</span>
            <span class="summary-value">{{ teamKpi.totalMembers }}</span>
          </div>
          <div class="summary-card">
            <span class="icon summary-icon">insights</span>
            <span class="summary-label">Avg KPI Final</span>
            <span class="summary-value">{{ teamKpi.avgKpiFinal | number: '1.0-1' }}</span>
          </div>
          <div class="summary-card">
            <span class="icon summary-icon">bar_chart</span>
            <span class="summary-label">Avg WCR</span>
            <span class="summary-value">{{ teamKpi.avgWcr | number: '1.0-1' }}%</span>
          </div>
          <div class="summary-card" [class.summary-card--alert]="teamKpi.needsImprovementCount > 0">
            <span class="icon summary-icon">priority_high</span>
            <span class="summary-label">Needs Improvement</span>
            <span class="summary-value">{{ teamKpi.needsImprovementCount }}</span>
          </div>
        </div>
      </div>

      <div class="kpi-section" *ngIf="!(isLoading$ | async)">
        <h3>KPI Trend</h3>
        <div class="trend-container" *ngIf="kpiTrend$ | async as trend">
          <div class="trend-bars">
            <div class="trend-bar-col">
              <div class="trend-bar-track">
                <div class="trend-bar prev" [style.height.%]="barHeight(trend.previous)"></div>
              </div>
              <span class="trend-bar-value">{{ trend.previous | number: '1.0-1' }}</span>
              <span class="trend-bar-label">Kỳ trước</span>
            </div>
            <div class="trend-bar-col">
              <div class="trend-bar-track">
                <div class="trend-bar current" [ngClass]="trend.change >= 0 ? 'up' : 'down'" [style.height.%]="barHeight(trend.current)"></div>
              </div>
              <span class="trend-bar-value">{{ trend.current | number: '1.0-1' }}</span>
              <span class="trend-bar-label">Kỳ này</span>
            </div>
          </div>
          <div class="trend-change" [ngClass]="trend.change >= 0 ? 'positive' : 'negative'">
            <span class="icon">{{ trend.change >= 0 ? 'trending_up' : 'trending_down' }}</span>
            {{ Math.abs(trend.percentageChange) | number: '1.0-2' }}% so với kỳ trước
          </div>
        </div>
      </div>

      <div *ngIf="isLoading$ | async" class="loading">Loading KPI data...</div>
    </div>
  `,
  styles: [`
    .kpi-dashboard-container { padding: 20px; }
    .dashboard-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
    .header-actions { display: flex; gap: 15px; align-items: center; }
    .btn-refresh { padding: 8px 16px; background: #2563eb; color: white; border: none; border-radius: 10px; cursor: pointer; }
    .period-select { padding: 8px 12px; border: 1px solid #ddd; border-radius: 10px; }
    .kpi-section { margin-bottom: 40px; }
    .kpi-section h3 { margin: 0 0 20px 0; color: #333; }
    .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; }
    .team-summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }
    .summary-card { background: white; border: 1px solid #ddd; border-radius: 16px; padding: 15px; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 4px; }
    .summary-card--alert { border-color: #fecaca; background: #fef2f2; }
    .summary-icon { font-size: 22px; color: var(--color-primary, #2563eb); margin-bottom: 2px; }
    .summary-card--alert .summary-icon { color: #ef4444; }
    .summary-label { display: block; color: #666; font-size: 12px; }
    .summary-value { display: block; font-size: 20px; font-weight: bold; color: #333; }
    .trend-container { background: white; border: 1px solid #ddd; border-radius: 16px; padding: 24px; }
    .trend-bars { display: flex; justify-content: center; align-items: flex-end; gap: 40px; height: 140px; margin-bottom: 20px; }
    .trend-bar-col { display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; gap: 6px; }
    .trend-bar-track { width: 48px; height: 100px; background: #f1f5f9; border-radius: 8px; display: flex; align-items: flex-end; overflow: hidden; }
    .trend-bar { width: 100%; border-radius: 8px 8px 0 0; transition: height 0.4s ease; min-height: 4px; }
    .trend-bar.prev { background: #cbd5e1; }
    .trend-bar.current.up { background: #22c55e; }
    .trend-bar.current.down { background: #ef4444; }
    .trend-bar-value { font-size: 14px; font-weight: 700; color: #333; }
    .trend-bar-label { font-size: 11px; color: #999; }
    .trend-change { display: flex; align-items: center; justify-content: center; gap: 6px; font-weight: bold; }
    .trend-change.positive { color: #4CAF50; }
    .trend-change.negative { color: #F44336; }
    .loading { text-align: center; color: #2563eb; padding: 40px; }
  `]
})
export class KpiDashboardComponent implements OnInit {
  private store = inject(Store);
  private authStore = inject(AuthStore);

  currentKPI$ = this.store.select(selectCurrentUserKpi);
  teamKPI$ = this.store.select(selectTeamKpi);
  teamKpiSummary$ = this.teamKPI$.pipe(
    map(list => ({
      totalMembers: list.length,
      avgKpiFinal: list.length ? list.reduce((sum, k) => sum + (k.kpiFinal || 0), 0) / list.length : 0,
      avgWcr: list.length ? list.reduce((sum, k) => sum + (k.wcr || 0), 0) / list.length : 0,
      needsImprovementCount: list.filter(k => (k.kpiFinal || 0) < 60).length
    }))
  );
  kpiTrend$ = this.store.select(selectTeamKpiTrends);
  isLoading$ = this.store.select(selectKpiLoading);

  selectedPeriod = 'CURRENT';
  Math = Math;

  ngOnInit() {
    this.refreshKPI();
  }

  refreshKPI() {
    this.store.dispatch(KpiActions.loadCurrentUserKpi());
    this.store.dispatch(KpiActions.loadTeamKpiSummary({}));
    const userId = this.authStore.user()?.id;
    if (userId) {
      this.store.dispatch(KpiActions.loadKpiHistory({ userId }));
    }
  }

  onPeriodChange() {
    this.refreshKPI();
  }

  /** Bar height as % of the larger of the two values, so the taller bar always fills the track. */
  barHeight(value: number): number {
    return Math.max(4, Math.min(100, value));
  }
}
