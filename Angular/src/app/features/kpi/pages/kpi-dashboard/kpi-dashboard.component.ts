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
    <div class="kpi-dashboard-container animate-fade-in">
      <!-- HEADER -->
      <div class="page-header">
        <div>
          <h1 class="page-title">KPI Dashboard</h1>
          <p class="page-subtitle">Tổng quan chỉ số hiệu suất cá nhân và toàn đội ngũ</p>
        </div>
        <div class="header-actions">
          <button (click)="refreshKPI()" class="btn btn-secondary btn-sm">
            <span class="icon">refresh</span> Làm mới
          </button>
          <div class="period-select-wrapper">
            <select [(ngModel)]="selectedPeriod" (change)="onPeriodChange()" class="period-select">
              <option value="CURRENT">Kỳ hiện tại</option>
              <option value="PREVIOUS">Kỳ trước</option>
              <option value="YEAR_TO_DATE">Từ đầu năm</option>
            </select>
          </div>
        </div>
      </div>

      <!-- LOADING -->
      <div *ngIf="isLoading$ | async" class="state-card">
        <div class="spinner"></div>
        <p>Đang tải dữ liệu KPI...</p>
      </div>

      <div class="dashboard-content" *ngIf="!(isLoading$ | async)">
        <!-- MY KPI SECTION -->
        <div class="kpi-section">
          <div class="section-header">
            <div class="section-title-box">
              <span class="icon section-icon">person</span>
              <h3>KPI Cá Nhân Của Bạn</h3>
            </div>
          </div>
          <div class="kpi-grid" *ngIf="currentKPI$ | async as kpi">
            <app-kpi-card
              title="Weighted Completion Rate"
              [value]="kpi.wcr || 0"
              unit="%"
              [trend]="kpi.percentageChange || 0"
              period="Kỳ này"
              [target]="85"
              [maxValue]="100"
              type="PERFORMANCE"
            ></app-kpi-card>

            <app-kpi-card
              title="Volume Index"
              [value]="kpi.vi || 0"
              unit="%"
              period="Kỳ này"
              [target]="100"
              [maxValue]="120"
              type="PRODUCTIVITY"
            ></app-kpi-card>

            <app-kpi-card
              title="Estimate Accuracy"
              [value]="kpi.ea || 0"
              unit="%"
              period="Kỳ này"
              [target]="90"
              [maxValue]="100"
              type="EFFICIENCY"
            ></app-kpi-card>

            <app-kpi-card
              title="KPI Final"
              [value]="kpi.kpiFinal || 0"
              [unit]="' — ' + kpi.ranking"
              period="Kỳ này"
              [target]="80"
              [maxValue]="100"
              type="QUALITY"
            ></app-kpi-card>
          </div>
        </div>

        <!-- TEAM SUMMARY & TRENDS (2 COLUMNS) -->
        <div class="analytics-row">
          <!-- TEAM KPI -->
          <div class="kpi-section col">
            <div class="section-header">
              <div class="section-title-box">
                <span class="icon section-icon">groups</span>
                <h3>Tổng Hợp Nhóm</h3>
              </div>
            </div>
            <div class="team-summary" *ngIf="teamKpiSummary$ | async as teamKpi">
              <div class="summary-card">
                <div class="summary-icon sky"><span class="icon">group</span></div>
                <div class="summary-info">
                  <span class="summary-value">{{ teamKpi.totalMembers }}</span>
                  <span class="summary-label">Tổng thành viên</span>
                </div>
              </div>
              <div class="summary-card">
                <div class="summary-icon blue"><span class="icon">insights</span></div>
                <div class="summary-info">
                  <span class="summary-value">{{ teamKpi.avgKpiFinal | number: '1.0-1' }}</span>
                  <span class="summary-label">KPI Final trung bình</span>
                </div>
              </div>
              <div class="summary-card">
                <div class="summary-icon emerald"><span class="icon">bar_chart</span></div>
                <div class="summary-info">
                  <span class="summary-value">{{ teamKpi.avgWcr | number: '1.0-1' }}%</span>
                  <span class="summary-label">WCR trung bình</span>
                </div>
              </div>
              <div class="summary-card" [class.summary-card--alert]="teamKpi.needsImprovementCount > 0">
                <div class="summary-icon rose"><span class="icon">priority_high</span></div>
                <div class="summary-info">
                  <span class="summary-value" [class.danger-text]="teamKpi.needsImprovementCount > 0">{{ teamKpi.needsImprovementCount }}</span>
                  <span class="summary-label">Cần cải thiện (&lt;60)</span>
                </div>
              </div>
            </div>
          </div>

          <!-- TREND -->
          <div class="kpi-section col">
            <div class="section-header">
              <div class="section-title-box">
                <span class="icon section-icon">show_chart</span>
                <h3>Xu Hướng KPI</h3>
              </div>
            </div>
            <div class="trend-card" *ngIf="kpiTrend$ | async as trend">
              <div class="trend-bars">
                <div class="trend-bar-col">
                  <span class="trend-bar-value">{{ trend.previous | number: '1.0-1' }}</span>
                  <div class="trend-bar-track">
                    <div class="trend-bar prev" [style.height.%]="barHeight(trend.previous)"></div>
                  </div>
                  <span class="trend-bar-label">Kỳ trước</span>
                </div>
                <div class="trend-bar-col">
                  <span class="trend-bar-value">{{ trend.current | number: '1.0-1' }}</span>
                  <div class="trend-bar-track">
                    <div class="trend-bar current" [ngClass]="trend.change >= 0 ? 'up' : 'down'" [style.height.%]="barHeight(trend.current)"></div>
                  </div>
                  <span class="trend-bar-label">Kỳ này</span>
                </div>
              </div>
              <div class="trend-footer">
                <div class="trend-change-pill" [ngClass]="trend.change >= 0 ? 'positive' : 'negative'">
                  <span class="icon">{{ trend.change >= 0 ? 'trending_up' : 'trending_down' }}</span>
                  {{ trend.change >= 0 ? '+' : '' }}{{ Math.abs(trend.percentageChange) | number: '1.0-2' }}% so với kỳ trước
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .kpi-dashboard-container {
      max-width: 1300px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;
      gap: 16px;
      flex-wrap: wrap;
    }
    .page-title {
      font-size: 26px;
      font-weight: 800;
      color: var(--color-text);
      margin: 0 0 4px 0;
      letter-spacing: -0.02em;
    }
    .page-subtitle {
      color: var(--color-text-muted);
      font-size: 14px;
      margin: 0;
    }

    .header-actions {
      display: flex;
      gap: 12px;
      align-items: center;
    }
    .period-select-wrapper {
      width: 180px;
    }
    .period-select {
      background: #ffffff;
      padding: 8px 12px;
      border: 1.5px solid var(--color-border);
      border-radius: var(--radius-sm);
      font-weight: 600;
      color: var(--color-text);
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    .section-title-box {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .section-icon {
      color: var(--color-primary);
      font-size: 22px;
    }
    .section-title-box h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 800;
      color: var(--color-text);
    }

    .kpi-section {
      margin-bottom: 28px;
    }
    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 16px;
    }

    .analytics-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
    }
    @media (max-width: 900px) {
      .analytics-row { grid-template-columns: 1fr; }
    }

    /* TEAM SUMMARY */
    .team-summary {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 14px;
    }
    .summary-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 16px;
      display: flex;
      align-items: center;
      gap: 14px;
      box-shadow: var(--shadow-sm);
      transition: all 0.2s ease;
    }
    .summary-card:hover {
      transform: translateY(-2px);
      box-shadow: var(--shadow-md);
      border-color: var(--color-primary-border);
    }
    .summary-card--alert {
      border-color: #fecdd3;
      background: #fff8f8;
    }
    .summary-icon {
      width: 42px;
      height: 42px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .summary-icon.sky { background: #e0f2fe; color: #0284c7; }
    .summary-icon.blue { background: #dbeafe; color: #2563eb; }
    .summary-icon.emerald { background: #dcfce7; color: #059669; }
    .summary-icon.rose { background: #ffe4e6; color: #e11d48; }

    .summary-info {
      display: flex;
      flex-direction: column;
    }
    .summary-value {
      font-size: 20px;
      font-weight: 800;
      color: var(--color-text);
      line-height: 1.2;
    }
    .summary-label {
      font-size: 11.5px;
      color: var(--color-text-muted);
      font-weight: 600;
    }
    .danger-text { color: #e11d48; }

    /* TREND CARD */
    .trend-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 20px;
      box-shadow: var(--shadow-sm);
      display: flex;
      flex-direction: column;
      height: calc(100% - 38px);
      justify-content: space-between;
    }
    .trend-bars {
      display: flex;
      justify-content: center;
      align-items: flex-end;
      gap: 48px;
      height: 120px;
      padding: 10px 0;
    }
    .trend-bar-col {
      display: flex;
      flex-direction: column;
      align-items: center;
      height: 100%;
      justify-content: flex-end;
      gap: 6px;
    }
    .trend-bar-track {
      width: 44px;
      height: 80px;
      background: #f1f5f9;
      border-radius: 8px;
      display: flex;
      align-items: flex-end;
      overflow: hidden;
    }
    .trend-bar {
      width: 100%;
      border-radius: 6px 6px 0 0;
      transition: height 0.4s cubic-bezier(0.16, 1, 0.3, 1);
      min-height: 4px;
    }
    .trend-bar.prev { background: #94a3b8; }
    .trend-bar.current.up { background: #059669; }
    .trend-bar.current.down { background: #e11d48; }

    .trend-bar-value {
      font-size: 13px;
      font-weight: 800;
      color: var(--color-text);
    }
    .trend-bar-label {
      font-size: 11px;
      font-weight: 600;
      color: var(--color-text-muted);
    }

    .trend-footer {
      display: flex;
      justify-content: center;
      padding-top: 14px;
      border-top: 1px solid var(--color-border);
    }
    .trend-change-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      font-weight: 800;
      padding: 4px 14px;
      border-radius: 999px;
    }
    .trend-change-pill.positive {
      color: #059669;
      background: #ecfdf5;
      border: 1px solid #a7f3d0;
    }
    .trend-change-pill.negative {
      color: #e11d48;
      background: #fff1f2;
      border: 1px solid #fecdd3;
    }

    /* SPINNER */
    .state-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 48px;
      text-align: center;
    }
    .spinner {
      width: 36px;
      height: 36px;
      border: 3px solid #e0f2fe;
      border-top-color: var(--color-primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 12px auto;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
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

  barHeight(value: number): number {
    return Math.max(4, Math.min(100, value));
  }
}
