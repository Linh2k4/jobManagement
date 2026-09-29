import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface KpiConfig {
  wcrWeight: number;
  viWeight: number;
  eaWeight: number;
  autoScoreWeight: number;
  leadScoreWeight: number;
  managerScoreWeight: number;
  workingHoursPerDay: number;
}

/**
 * Scope.md §7.6 — KPI formula weights. Backed by the real
 * GET/PUT /api/v1/kpi/admin/config (already wired into
 * KpiCalculationService, not cosmetic) — this page is the only thing that
 * was missing to make it reachable outside Swagger/curl.
 */
@Component({
  selector: 'app-kpi-config',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="kpi-config-page">
      <div class="header">
        <a routerLink="/settings" class="back-link"><span class="icon">arrow_back</span> Cài đặt</a>
        <h2>Cấu hình Công thức KPI</h2>
        <p class="subtitle">Scope.md §7.6 — thay đổi áp dụng cho kỳ chưa chốt (kỳ FINALIZED không bị ảnh hưởng)</p>
      </div>

      <div *ngIf="loading" class="loading">Đang tải...</div>

      <form *ngIf="!loading && config" (ngSubmit)="save()">
        <div class="config-card">
          <h3>Trọng số điểm KPI cuối</h3>
          <p class="formula">KPI_final = autoScore × W1 + leadScore × W2 + managerScore × W3</p>

          <div class="weight-row">
            <label>W1 — Điểm tự động (20% – 70%)</label>
            <input type="number" min="20" max="70" step="1" [(ngModel)]="config.autoScoreWeight" name="autoScoreWeight">
            <span class="unit">%</span>
          </div>
          <div class="weight-row">
            <label>W2 — Điểm Lead (10% – 60%)</label>
            <input type="number" min="10" max="60" step="1" [(ngModel)]="config.leadScoreWeight" name="leadScoreWeight">
            <span class="unit">%</span>
          </div>
          <div class="weight-row">
            <label>W3 — Điểm Manager (10% – 50%)</label>
            <input type="number" min="10" max="50" step="1" [(ngModel)]="config.managerScoreWeight" name="managerScoreWeight">
            <span class="unit">%</span>
          </div>
          <div class="sum-line" [class.sum-bad]="!sumOk(kpiSum())">
            Tổng: {{ kpiSum() }}%
            <span class="icon" *ngIf="sumOk(kpiSum())">check</span>
            <ng-container *ngIf="!sumOk(kpiSum())">(phải = 100%)</ng-container>
          </div>
        </div>

        <div class="config-card">
          <h3>Trọng số Điểm tự động (autoScore)</h3>
          <p class="formula">autoScore = WCR × w_wcr + VI × w_vi + EA × w_ea</p>

          <div class="weight-row">
            <label>w_wcr — Weighted Completion Rate (40% – 80%)</label>
            <input type="number" min="40" max="80" step="1" [(ngModel)]="config.wcrWeight" name="wcrWeight">
            <span class="unit">%</span>
          </div>
          <div class="weight-row">
            <label>w_vi — Volume Index (10% – 40%)</label>
            <input type="number" min="10" max="40" step="1" [(ngModel)]="config.viWeight" name="viWeight">
            <span class="unit">%</span>
          </div>
          <div class="weight-row">
            <label>w_ea — Estimate Accuracy (0% – 30%)</label>
            <input type="number" min="0" max="30" step="1" [(ngModel)]="config.eaWeight" name="eaWeight">
            <span class="unit">%</span>
          </div>
          <div class="sum-line" [class.sum-bad]="!sumOk(autoScoreSum())">
            Tổng: {{ autoScoreSum() }}%
            <span class="icon" *ngIf="sumOk(autoScoreSum())">check</span>
            <ng-container *ngIf="!sumOk(autoScoreSum())">(phải = 100%)</ng-container>
          </div>
        </div>

        <div class="config-card">
          <h3>Volume Index</h3>
          <div class="weight-row">
            <label>Số giờ làm việc/ngày (1 – 24)</label>
            <input type="number" min="1" max="24" step="0.5" [(ngModel)]="config.workingHoursPerDay" name="workingHoursPerDay">
            <span class="unit">giờ</span>
          </div>
        </div>

        <div class="error" *ngIf="error">{{ error }}</div>
        <div class="success" *ngIf="success"><span class="icon">check</span> Đã lưu cấu hình. Áp dụng cho lần tính KPI tiếp theo.</div>

        <button type="submit" class="btn-save" [disabled]="saving || !sumOk(kpiSum()) || !sumOk(autoScoreSum())">
          {{ saving ? 'Đang lưu...' : 'Lưu cấu hình' }}
        </button>
      </form>
    </div>
  `,
  styles: [`
    .kpi-config-page { padding: 20px; max-width: 700px; margin: 0 auto; }
    .header { margin-bottom: 20px; }
    .back-link { font-size: 13px; color: #2563eb; text-decoration: none; }
    .subtitle { color: #777; font-size: 13px; }
    .config-card { background: white; border: 1px solid #ddd; border-radius: 16px; padding: 20px; margin-bottom: 16px; }
    .config-card h3 { margin: 0 0 4px; }
    .formula { font-family: monospace; font-size: 12px; color: #666; margin: 0 0 16px; }
    .weight-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
    .weight-row label { flex: 1; font-size: 13px; }
    .weight-row input { width: 90px; padding: 6px 8px; border: 1px solid #ddd; border-radius: 10px; text-align: right; }
    .unit { width: 30px; color: #777; font-size: 13px; }
    .sum-line { text-align: right; font-size: 13px; font-weight: 600; color: #2e7d32; margin-top: 8px; }
    .sum-line.sum-bad { color: #c62828; }
    .loading { text-align: center; color: #999; padding: 40px; }
    .error { background: #ffcdd2; color: #c62828; padding: 10px; border-radius: 10px; margin-bottom: 12px; }
    .success { background: #c8e6c9; color: #2e7d32; padding: 10px; border-radius: 10px; margin-bottom: 12px; }
    .btn-save { padding: 10px 20px; background: #2563eb; color: white; border: none; border-radius: 10px; cursor: pointer; font-size: 14px; }
    .btn-save:disabled { background: #ccc; cursor: not-allowed; }
  `]
})
export class KpiConfigComponent implements OnInit {
  private http = inject(HttpClient);

  config: KpiConfig | null = null;
  loading = false;
  saving = false;
  error: string | null = null;
  success = false;

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.http.get<{ data: KpiConfig }>('/api/v1/kpi/admin/config').subscribe({
      next: res => { this.config = res.data; this.loading = false; },
      error: () => { this.loading = false; this.error = 'Không thể tải cấu hình.'; }
    });
  }

  kpiSum(): number {
    if (!this.config) return 0;
    return this.round1(this.config.autoScoreWeight + this.config.leadScoreWeight + this.config.managerScoreWeight);
  }

  autoScoreSum(): number {
    if (!this.config) return 0;
    return this.round1(this.config.wcrWeight + this.config.viWeight + this.config.eaWeight);
  }

  sumOk(sum: number): boolean {
    return Math.abs(sum - 100) < 0.01;
  }

  private round1(n: number): number {
    return Math.round(n * 10) / 10;
  }

  save() {
    if (!this.config) return;
    this.saving = true;
    this.error = null;
    this.success = false;

    this.http.put<{ success: boolean; message: string }>('/api/v1/kpi/admin/config', this.config).subscribe({
      next: () => {
        this.saving = false;
        this.success = true;
        setTimeout(() => this.success = false, 3000);
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Lưu cấu hình thất bại.';
      }
    });
  }
}
