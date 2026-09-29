import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-kpi-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="kpi-card" [ngClass]="'card-' + type">
      <div class="card-header">
        <h3>{{ title }}</h3>
        <div class="kpi-trend" *ngIf="trend !== null" [ngClass]="trend >= 0 ? 'trend-up' : 'trend-down'">
          <span class="icon trend-icon">{{ trend >= 0 ? 'trending_up' : 'trending_down' }}</span>
          <span class="trend-value">{{ Math.abs(trend) | number: '1.0-1' }}%</span>
        </div>
      </div>

      <div class="card-body">
        <div class="ring" *ngIf="maxValue" [style.background]="ringGradient()">
          <div class="ring-hole">
            <span class="kpi-value">{{ value | number: '1.0-0' }}</span>
            <span class="kpi-unit">{{ unit }}</span>
          </div>
        </div>
        <div class="value-only" *ngIf="!maxValue">
          <span class="kpi-value">{{ value | number: '1.0-1' }}</span>
          <span class="kpi-unit">{{ unit }}</span>
        </div>

        <div *ngIf="period" class="kpi-period">{{ period }}</div>
      </div>

      <div class="card-footer" *ngIf="target">
        <span>Mục tiêu: {{ target | number: '1.0-1' }} {{ unit }}</span>
        <span class="achievement" [ngClass]="value >= target ? 'achieved' : 'pending'">
          <span class="icon">{{ value >= target ? 'check_circle' : 'radio_button_unchecked' }}</span>
          {{ value >= target ? 'Đạt' : 'Chưa đạt' }}
        </span>
      </div>
    </div>
  `,
  styles: [`
    .kpi-card {
      border: 1px solid #ddd;
      border-radius: 16px;
      padding: 20px;
      background: white;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      transition: transform 0.2s, box-shadow 0.2s;
    }
    .kpi-card:hover { transform: translateY(-2px); box-shadow: 0 4px 8px rgba(0,0,0,0.15); }
    .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
    .card-header h3 { margin: 0; font-size: 14px; color: #666; }
    .card-body { display: flex; flex-direction: column; align-items: center; text-align: center; }
    .ring {
      width: 96px; height: 96px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      margin-bottom: 10px; transition: background 0.3s ease;
    }
    .ring-hole {
      width: 74px; height: 74px; border-radius: 50%; background: white;
      display: flex; flex-direction: column; align-items: center; justify-content: center;
    }
    .value-only { display: flex; flex-direction: column; align-items: center; margin-bottom: 10px; }
    .kpi-value { font-size: 22px; font-weight: bold; color: #333; }
    .kpi-unit { color: #999; font-size: 11px; }
    .kpi-trend { display: flex; align-items: center; gap: 4px; font-size: 12px; }
    .trend-up { color: #4CAF50; }
    .trend-down { color: #F44336; }
    .trend-icon { font-size: 16px; }
    .kpi-period { font-size: 11px; color: #999; }
    .card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; font-size: 12px; }
    .achievement { display: flex; align-items: center; gap: 4px; font-weight: bold; }
    .achievement .icon { font-size: 15px; }
    .achievement.achieved { color: #4CAF50; }
    .achievement.pending { color: #FF9800; }
    .card-PERFORMANCE { border-left: 4px solid #2563eb; }
    .card-QUALITY { border-left: 4px solid #4CAF50; }
    .card-EFFICIENCY { border-left: 4px solid #FF9800; }
    .card-PRODUCTIVITY { border-left: 4px solid #9C27B0; }
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
    if (pct >= 80) return '#22c55e';
    if (pct >= 50) return '#2563eb';
    if (pct >= 30) return '#f59e0b';
    return '#ef4444';
  }

  ringGradient(): string {
    const pct = this.maxValue ? Math.min(100, Math.max(0, (this.value / this.maxValue) * 100)) : 0;
    return `conic-gradient(${this.ringColor()} ${pct}%, #e5e7eb ${pct}%)`;
  }
}
