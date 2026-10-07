import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-kpi-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="kpi-card" [ngClass]="'card-' + type">
      <div class="card-header">
        <span class="card-title">{{ title }}</span>
        <div class="kpi-trend" *ngIf="trend !== null" [ngClass]="trend >= 0 ? 'trend-up' : 'trend-down'">
          <span class="icon trend-icon">{{ trend >= 0 ? 'trending_up' : 'trending_down' }}</span>
          <span class="trend-value">{{ Math.abs(trend) | number: '1.0-1' }}%</span>
        </div>
      </div>

      <div class="card-body">
        <div class="ring-wrapper" *ngIf="maxValue">
          <div class="ring" [style.background]="ringGradient()">
            <div class="ring-hole">
              <span class="kpi-value">{{ value | number: '1.0-0' }}</span>
              <span class="kpi-unit">{{ unit }}</span>
            </div>
          </div>
        </div>
        <div class="value-only" *ngIf="!maxValue">
          <span class="kpi-value">{{ value | number: '1.0-1' }}</span>
          <span class="kpi-unit">{{ unit }}</span>
        </div>

        <div *ngIf="period" class="kpi-period">{{ period }}</div>
      </div>

      <div class="card-footer" *ngIf="target">
        <span class="target-text">Mục tiêu: <strong>{{ target | number: '1.0-1' }} {{ unit }}</strong></span>
        <span class="achievement-badge" [ngClass]="value >= target ? 'achieved' : 'pending'">
          <span class="icon">{{ value >= target ? 'check_circle' : 'pending' }}</span>
          {{ value >= target ? 'Đạt' : 'Chưa đạt' }}
        </span>
      </div>
    </div>
  `,
  styles: [`
    .kpi-card {
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 20px;
      background: #ffffff;
      box-shadow: var(--shadow-sm);
      transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .kpi-card:hover {
      transform: translateY(-3px);
      box-shadow: var(--shadow-md);
      border-color: var(--color-primary-border);
    }
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    .card-title {
      font-size: 13.5px;
      font-weight: 700;
      color: var(--color-text-muted);
    }
    .card-body {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      padding: 6px 0;
    }
    .ring-wrapper {
      margin-bottom: 8px;
    }
    .ring {
      width: 100px;
      height: 100px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: background 0.3s ease;
      box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
    }
    .ring-hole {
      width: 78px;
      height: 78px;
      border-radius: 50%;
      background: #ffffff;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      box-shadow: var(--shadow-sm);
    }
    .value-only {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 10px;
    }
    .kpi-value {
      font-size: 24px;
      font-weight: 900;
      color: var(--color-text);
      line-height: 1.1;
    }
    .kpi-unit {
      color: var(--color-text-muted);
      font-size: 11px;
      font-weight: 600;
      margin-top: 2px;
    }
    .kpi-trend {
      display: flex;
      align-items: center;
      gap: 3px;
      font-size: 11.5px;
      font-weight: 700;
      padding: 2px 8px;
      border-radius: 999px;
    }
    .trend-up {
      color: #059669;
      background: #ecfdf5;
      border: 1px solid #a7f3d0;
    }
    .trend-down {
      color: #e11d48;
      background: #fff1f2;
      border: 1px solid #fecdd3;
    }
    .trend-icon { font-size: 14px; }
    .kpi-period {
      font-size: 11px;
      color: var(--color-text-light);
      font-weight: 600;
      margin-top: 4px;
    }
    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 14px;
      padding-top: 12px;
      border-top: 1px solid var(--color-border);
      font-size: 12px;
    }
    .target-text {
      color: var(--color-text-muted);
    }
    .target-text strong {
      color: var(--color-text);
    }
    .achievement-badge {
      display: flex;
      align-items: center;
      gap: 4px;
      font-weight: 800;
      font-size: 11px;
      padding: 3px 8px;
      border-radius: 999px;
    }
    .achievement-badge .icon { font-size: 14px; }
    .achievement-badge.achieved {
      color: #059669;
      background: #ecfdf5;
    }
    .achievement-badge.pending {
      color: #d97706;
      background: #fffbeb;
    }

    .card-PERFORMANCE { border-top: 3.5px solid #0284c7; }
    .card-QUALITY { border-top: 3.5px solid #059669; }
    .card-EFFICIENCY { border-top: 3.5px solid #d97706; }
    .card-PRODUCTIVITY { border-top: 3.5px solid #8b5cf6; }
  `]
})
export class KpiCardComponent {
  @Input() title = '';
  @Input() value = 0;
  @Input() unit = '';
  @Input() trend: number | null = null;
  @Input() period = '';
  @Input() target: number | null = null;
  @Input() maxValue: number | null = null;
  @Input() type = 'PERFORMANCE';

  Math = Math;

  private ringColor(): string {
    const pct = this.maxValue ? (this.value / this.maxValue) * 100 : 0;
    if (pct >= 80) return '#059669';
    if (pct >= 50) return '#0284c7';
    if (pct >= 30) return '#d97706';
    return '#e11d48';
  }

  ringGradient(): string {
    const pct = this.maxValue ? Math.min(100, Math.max(0, (this.value / this.maxValue) * 100)) : 0;
    return `conic-gradient(${this.ringColor()} ${pct}%, #e2e8f0 ${pct}%)`;
  }
}
