import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { KpiService } from '../../../../core/services/api/kpi.service';
import { MemberKpiDetail, MemberKpiStatus, MemberKpiSummary } from '../../../../core/models';

type SortKey = 'fullName' | 'kpi' | 'overdueCount' | 'totalDone';

@Component({
  selector: 'app-members-kpi',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="members-kpi-page animate-fade-in">
      <!-- PAGE HEADER -->
      <div class="page-header">
        <div>
          <h1 class="page-title">Thành viên & KPI</h1>
          <p class="page-subtitle">Theo dõi hiệu suất và kết quả công việc từng thành viên theo kỳ</p>
        </div>
        <div class="header-controls">
          <div class="month-picker-wrapper">
            <span class="icon">calendar_month</span>
            <input type="month" [(ngModel)]="periodMonth" (change)="load()" class="month-input">
          </div>
        </div>
      </div>

      <!-- 4 TOP STAT CARDS -->
      <div class="stats-grid" *ngIf="members.length > 0">
        <div class="stat-card">
          <div class="stat-icon-box sky">
            <span class="icon">group</span>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ uniqueMemberCount() }}</span>
            <span class="stat-label">Tổng thành viên</span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon-box blue">
            <span class="icon">analytics</span>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ avgKpi() | number:'1.0-1' }}</span>
            <span class="stat-label">KPI trung bình</span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon-box emerald">
            <span class="icon">check_circle</span>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ countByStatus('DAT') }}<small>/{{ uniqueMemberCount() }}</small></span>
            <span class="stat-label">Đạt mục tiêu (≥80)</span>
          </div>
        </div>

        <div class="stat-card" [class.stat-card-danger]="countByStatus('NGUY_HIEM') > 0">
          <div class="stat-icon-box rose">
            <span class="icon">warning</span>
          </div>
          <div class="stat-info">
            <span class="stat-value danger-text">{{ countByStatus('NGUY_HIEM') }}</span>
            <span class="stat-label">Cần chú ý (&lt;60)</span>
          </div>
        </div>
      </div>

      <!-- FILTERS -->
      <div class="filter-card">
        <div class="search-box">
          <span class="icon search-icon">search</span>
          <input type="text" [(ngModel)]="searchText" placeholder="Tìm tên thành viên hoặc nhóm...">
        </div>
        <div class="filter-select-box">
          <select [(ngModel)]="statusFilter">
            <option [ngValue]="undefined">Tất cả trạng thái</option>
            <option value="DAT">Đạt (≥80)</option>
            <option value="CANH_BAO">Cảnh báo (60-79)</option>
            <option value="NGUY_HIEM">Nguy hiểm (&lt;60)</option>
          </select>
        </div>
      </div>

      <!-- MAIN TABLE -->
      <div class="table-wrapper" *ngIf="!loading && filteredMembers().length > 0">
        <table class="table-clean">
          <thead>
            <tr>
              <th style="width: 50px;">#</th>
              <th (click)="sortBy('fullName')" class="sortable">
                Thành viên
                <span class="icon sort-icon" *ngIf="sortKey === 'fullName'">{{ sortAsc ? 'arrow_upward' : 'arrow_downward' }}</span>
              </th>
              <th>Nhóm</th>
              <th (click)="sortBy('kpi')" class="sortable text-center">
                KPI
                <span class="icon sort-icon" *ngIf="sortKey === 'kpi'">{{ sortAsc ? 'arrow_upward' : 'arrow_downward' }}</span>
              </th>
              <th class="text-center">Fast Task</th>
              <th class="text-center">Multi-step</th>
              <th (click)="sortBy('totalDone')" class="sortable text-center">
                Hoàn thành
                <span class="icon sort-icon" *ngIf="sortKey === 'totalDone'">{{ sortAsc ? 'arrow_upward' : 'arrow_downward' }}</span>
              </th>
              <th (click)="sortBy('overdueCount')" class="sortable text-center">
                Quá hạn
                <span class="icon sort-icon" *ngIf="sortKey === 'overdueCount'">{{ sortAsc ? 'arrow_upward' : 'arrow_downward' }}</span>
              </th>
              <th class="text-center">Trạng thái</th>
              <th style="width: 100px;"></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let m of filteredMembers(); let i = index">
              <td class="text-muted">{{ i + 1 }}</td>
              <td>
                <div class="member-cell">
                  <span class="avatar-pill">{{ initials(m.fullName) }}</span>
                  <div class="member-meta">
                    <span class="member-name">{{ m.fullName }}</span>
                    <span class="member-role">{{ m.role }}</span>
                  </div>
                </div>
              </td>
              <td>
                <span class="group-tag" *ngIf="m.groupName">{{ m.groupName }}</span>
                <span class="text-muted" *ngIf="!m.groupName">—</span>
              </td>
              <td class="text-center">
                <span class="kpi-score" [ngClass]="'kpi-' + m.kpiStatus">
                  {{ m.kpi != null ? (m.kpi | number:'1.0-1') : '—' }}
                </span>
              </td>
              <td class="text-center text-muted">{{ m.fastDone }}/{{ m.fastTotal }}</td>
              <td class="text-center text-muted">{{ m.multiStepDone }}/{{ m.multiStepTotal }}</td>
              <td class="text-center"><strong>{{ m.totalDone }}</strong></td>
              <td class="text-center">
                <span [class.overdue-text]="m.overdueCount > 0">{{ m.overdueCount }}</span>
              </td>
              <td class="text-center">
                <span class="status-pill" [ngClass]="'status-' + m.kpiStatus">
                  {{ statusLabel(m.kpiStatus) }}
                </span>
              </td>
              <td class="text-right">
                <button class="btn-detail" (click)="openDetail(m.userId, m.groupId)">
                  Chi tiết
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- LOADING & EMPTY -->
      <div *ngIf="loading" class="state-container">
        <div class="spinner"></div>
        <p>Đang tải dữ liệu KPI...</p>
      </div>

      <div *ngIf="!loading && members.length === 0" class="state-container">
        <div class="state-icon-box">
          <span class="icon">group_off</span>
        </div>
        <h3>Không có thành viên nào</h3>
        <p class="text-muted">Chưa có dữ liệu thành viên trong kỳ này.</p>
      </div>

      <div *ngIf="!loading && members.length > 0 && filteredMembers().length === 0" class="state-container">
        <div class="state-icon-box">
          <span class="icon">search_off</span>
        </div>
        <h3>Không tìm thấy kết quả</h3>
        <p class="text-muted">Không có thành viên nào khớp với bộ lọc hiện tại.</p>
      </div>
    </div>

    <!-- DETAIL MODAL -->
    <div class="modal-backdrop" *ngIf="detail" (click)="detail = null">
      <div class="modal-card" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <div class="modal-title-box">
            <div class="modal-avatar">{{ initials(detail.fullName) }}</div>
            <div>
              <h3>{{ detail.fullName }}</h3>
              <p class="modal-sub">{{ detail.role }} · Kỳ {{ detail.periodMonth }} <span *ngIf="detail.groupName" class="group-tag">{{ detail.groupName }}</span></p>
            </div>
          </div>
          <button class="modal-close" (click)="detail = null"><span class="icon">close</span></button>
        </div>

        <div class="modal-body">
          <div class="kpi-hero-banner">
            <div class="kpi-hero-left">
              <span class="kpi-hero-number" [ngClass]="'kpi-' + detail.kpiStatus">{{ detail.kpiFinal != null ? (detail.kpiFinal | number:'1.0-1') : '—' }}</span>
              <span class="kpi-hero-max">/ 100 điểm</span>
            </div>
            <div class="kpi-hero-right">
              <span class="status-pill large" [ngClass]="'status-' + detail.kpiStatus">{{ statusLabel(detail.kpiStatus) }}</span>
              <div *ngIf="detail.changeVsPreviousMonth != null" class="trend-badge" [ngClass]="detail.changeVsPreviousMonth >= 0 ? 'trend-up' : 'trend-down'">
                <span class="icon">{{ detail.changeVsPreviousMonth >= 0 ? 'trending_up' : 'trending_down' }}</span>
                {{ detail.changeVsPreviousMonth >= 0 ? '+' : '' }}{{ detail.changeVsPreviousMonth | number:'1.0-1' }} so với tháng trước
              </div>
            </div>
          </div>

          <div class="kpi-check-strip">
            <div class="check-item"><span class="icon" [ngClass]="detail.leadHasScored ? 'ok' : 'pending'">{{ detail.leadHasScored ? 'check_circle' : 'pending' }}</span> Lead chấm: <strong>{{ detail.leadHasScored ? 'Đã chấm' : 'Chưa' }}</strong></div>
            <div class="check-item"><span class="icon" [ngClass]="detail.managerHasFinalized ? 'ok' : 'pending'">{{ detail.managerHasFinalized ? 'check_circle' : 'pending' }}</span> Quản lý duyệt: <strong>{{ detail.managerHasFinalized ? 'Đã duyệt' : 'Chờ duyệt' }}</strong></div>
          </div>

          <h4 class="section-title">Breakdown theo loại task</h4>
          <table class="table-clean mini-table">
            <thead>
              <tr><th>Loại</th><th>Kết quả</th><th>% Hoàn thành</th></tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>Fast Task</strong></td>
                <td>{{ detail.fastTask.done }}/{{ detail.fastTask.total }}</td>
                <td>
                  <div class="progress-bar"><div class="progress-fill" [style.width.%]="detail.fastTask.completionPercent"></div></div>
                  <span class="pct-text">{{ detail.fastTask.completionPercent }}%</span>
                </td>
              </tr>
              <tr>
                <td><strong>Multi-step</strong></td>
                <td>{{ detail.multiStep.done }}/{{ detail.multiStep.total }}</td>
                <td>
                  <div class="progress-bar"><div class="progress-fill" [style.width.%]="detail.multiStep.completionPercent"></div></div>
                  <span class="pct-text">{{ detail.multiStep.completionPercent }}%</span>
                </td>
              </tr>
            </tbody>
            <tfoot>
              <tr><td colspan="2">Auto Score</td><td>{{ detail.autoScore != null ? (detail.autoScore | number:'1.0-1') : '—' }}</td></tr>
              <tr><td colspan="2">Lead Score</td><td>{{ detail.leadScore != null ? (detail.leadScore | number:'1.0-1') : '—' }}</td></tr>
              <tr><td colspan="2">Manager Score</td><td>{{ detail.managerScore != null ? (detail.managerScore | number:'1.0-1') : '—' }}</td></tr>
              <tr class="final-score-row"><td colspan="2">ĐIỂM CUỐI CÙNG</td><td>{{ detail.kpiFinal != null ? (detail.kpiFinal | number:'1.0-1') : '—' }}</td></tr>
            </tfoot>
          </table>

          <h4 class="section-title">Danh sách task trong nhóm ({{ detail.tasks.length }})</h4>
          <div class="tasks-table-wrapper" *ngIf="detail.tasks.length > 0">
            <table class="table-clean mini-table">
              <thead><tr><th>Tên task</th><th>Loại</th><th>Trạng thái</th><th>Deadline</th></tr></thead>
              <tbody>
                <tr *ngFor="let t of detail.tasks" (click)="router.navigate(['/tasks', t.taskId]); detail = null" class="clickable-task-row">
                  <td><strong>{{ t.title }}</strong></td>
                  <td><span class="type-tag">{{ t.timeCategory }}</span></td>
                  <td><span class="status-badge" [ngClass]="'status-' + t.status">{{ t.status }}</span></td>
                  <td>{{ t.dueDate ? (t.dueDate | date:'shortDate') : '—' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p *ngIf="detail.tasks.length === 0" class="text-muted text-center" style="padding: 20px;">Không có task nào trong nhóm này.</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .members-kpi-page {
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

    .month-picker-wrapper {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #ffffff;
      border: 1.5px solid var(--color-border);
      padding: 6px 14px;
      border-radius: var(--radius-sm);
      box-shadow: var(--shadow-sm);
      color: var(--color-primary);
    }
    .month-input {
      border: none !important;
      padding: 4px !important;
      font-weight: 600;
      color: var(--color-text);
      background: transparent;
      outline: none;
      box-shadow: none !important;
      width: auto !important;
    }

    /* TOP STATS */
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }
    .stat-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 18px 20px;
      display: flex;
      align-items: center;
      gap: 16px;
      box-shadow: var(--shadow-sm);
      transition: all 0.2s ease;
    }
    .stat-card:hover {
      transform: translateY(-2px);
      box-shadow: var(--shadow-md);
      border-color: var(--color-primary-border);
    }
    .stat-card-danger {
      border-color: #fecdd3;
      background: #fff8f8;
    }
    .stat-icon-box {
      width: 46px;
      height: 46px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .stat-icon-box.sky { background: #e0f2fe; color: #0284c7; }
    .stat-icon-box.blue { background: #dbeafe; color: #2563eb; }
    .stat-icon-box.emerald { background: #dcfce7; color: #059669; }
    .stat-icon-box.rose { background: #ffe4e6; color: #e11d48; }

    .stat-info {
      display: flex;
      flex-direction: column;
    }
    .stat-value {
      font-size: 24px;
      font-weight: 800;
      color: var(--color-text);
      line-height: 1.2;
    }
    .stat-value small {
      font-size: 15px;
      font-weight: 600;
      color: var(--color-text-muted);
    }
    .stat-label {
      font-size: 12px;
      color: var(--color-text-muted);
      font-weight: 600;
      margin-top: 2px;
    }
    .danger-text { color: #e11d48; }

    /* FILTERS */
    .filter-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 12px 16px;
      display: flex;
      gap: 12px;
      margin-bottom: 20px;
      box-shadow: var(--shadow-sm);
    }
    .search-box {
      flex: 1;
      position: relative;
      display: flex;
      align-items: center;
    }
    .search-icon {
      position: absolute;
      left: 12px;
      color: #94a3b8;
      font-size: 20px;
    }
    .search-box input {
      padding-left: 40px !important;
    }
    .filter-select-box {
      width: 220px;
    }

    /* TABLE */
    .table-wrapper {
      border-radius: var(--radius-md);
      overflow: hidden;
      box-shadow: var(--shadow-sm);
    }
    .sortable {
      cursor: pointer;
      user-select: none;
    }
    .sortable:hover {
      color: var(--color-primary);
    }
    .sort-icon {
      font-size: 15px;
      vertical-align: middle;
      margin-left: 2px;
    }
    .member-cell {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .avatar-pill {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      background: var(--gradient-primary);
      color: white;
      font-size: 13px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 2px 6px rgba(14, 165, 233, 0.3);
      flex-shrink: 0;
    }
    .member-meta {
      display: flex;
      flex-direction: column;
    }
    .member-name {
      font-weight: 700;
      color: var(--color-text);
      font-size: 14px;
    }
    .member-role {
      font-size: 11px;
      color: var(--color-text-muted);
      text-transform: uppercase;
      font-weight: 600;
    }
    .group-tag {
      background: var(--color-primary-subtle);
      color: var(--color-primary);
      border: 1px solid var(--color-primary-border);
      padding: 3px 10px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 600;
    }
    .kpi-score {
      font-size: 16px;
      font-weight: 800;
    }
    .kpi-DAT { color: #059669; }
    .kpi-CANH_BAO { color: #d97706; }
    .kpi-NGUY_HIEM { color: #e11d48; }

    .status-pill {
      display: inline-flex;
      padding: 4px 10px;
      border-radius: 999px;
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.02em;
    }
    .status-pill.status-DAT { background: #dcfce7; color: #15803d; border: 1px solid #86efac; }
    .status-pill.status-CANH_BAO { background: #fef3c7; color: #b45309; border: 1px solid #fde68a; }
    .status-pill.status-NGUY_HIEM { background: #ffe4e6; color: #be123c; border: 1px solid #fecdd3; }

    .overdue-text { color: #e11d48; font-weight: 800; }

    .btn-detail {
      background: #ffffff;
      color: var(--color-primary);
      border: 1.5px solid var(--color-primary-border);
      padding: 5px 12px;
      border-radius: var(--radius-xs);
      font-weight: 700;
      font-size: 12px;
      cursor: pointer;
      transition: all 0.15s ease;
    }
    .btn-detail:hover {
      background: var(--color-primary);
      color: #ffffff;
      box-shadow: 0 2px 8px rgba(14, 165, 233, 0.35);
    }

    .text-center { text-align: center; }
    .text-right { text-align: right; }
    .text-muted { color: var(--color-text-muted); }

    /* STATES */
    .state-container {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 48px 24px;
      text-align: center;
      box-shadow: var(--shadow-sm);
    }
    .state-icon-box {
      width: 56px;
      height: 56px;
      border-radius: 50%;
      background: var(--color-primary-light);
      color: var(--color-primary);
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 16px auto;
    }
    .state-icon-box .icon { font-size: 28px; }

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

    /* MODAL */
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 42, 0.6);
      backdrop-filter: blur(6px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 20px;
    }
    .modal-card {
      background: #ffffff;
      border-radius: var(--radius-lg);
      width: 860px;
      max-width: 100%;
      max-height: 90vh;
      overflow-y: auto;
      box-shadow: var(--shadow-lg);
      animation: scaleIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .modal-header {
      padding: 20px 24px;
      border-bottom: 1px solid var(--color-border);
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .modal-title-box { display: flex; align-items: center; gap: 14px; }
    .modal-avatar {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      background: var(--gradient-primary);
      color: white;
      font-weight: 800;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .modal-title-box h3 { margin: 0; font-size: 18px; font-weight: 800; }
    .modal-sub { margin: 2px 0 0 0; font-size: 12.5px; color: var(--color-text-muted); }
    .modal-close {
      background: none;
      border: 1px solid var(--color-border);
      border-radius: 8px;
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: var(--color-text-muted);
    }
    .modal-close:hover { background: var(--color-danger-bg); color: var(--color-danger); }
    .modal-body { padding: 24px; }

    .kpi-hero-banner {
      background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
      border: 1px solid var(--color-primary-border);
      border-radius: var(--radius-md);
      padding: 20px 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
    }
    .kpi-hero-left { display: flex; align-items: baseline; gap: 6px; }
    .kpi-hero-number { font-size: 38px; font-weight: 900; }
    .kpi-hero-max { font-size: 15px; color: var(--color-text-muted); font-weight: 600; }
    .kpi-hero-right { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; }
    .status-pill.large { font-size: 13px; padding: 6px 14px; }

    .trend-badge { display: flex; align-items: center; gap: 4px; font-size: 12px; font-weight: 700; }
    .trend-badge.trend-up { color: #059669; }
    .trend-badge.trend-down { color: #e11d48; }

    .kpi-check-strip {
      display: flex;
      gap: 24px;
      background: #f8fafc;
      padding: 12px 16px;
      border-radius: var(--radius-sm);
      margin-bottom: 20px;
      border: 1px solid var(--color-border);
    }
    .check-item { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--color-text); }
    .check-item .icon.ok { color: #059669; }
    .check-item .icon.pending { color: #d97706; }

    .section-title {
      font-size: 14px;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: var(--color-text-muted);
      margin: 20px 0 10px 0;
    }

    .mini-table { font-size: 13px; }
    .mini-table th, .mini-table td { padding: 10px 14px; }
    .mini-table tfoot td { font-weight: 700; background: #f8fafc; }
    .final-score-row td { color: var(--color-primary); font-size: 14px; font-weight: 800; background: #e0f2fe; }

    .progress-bar {
      width: 100px;
      height: 8px;
      background: #e2e8f0;
      border-radius: 999px;
      overflow: hidden;
      display: inline-block;
      vertical-align: middle;
      margin-right: 8px;
    }
    .progress-fill {
      height: 100%;
      background: var(--gradient-primary);
      border-radius: 999px;
    }
    .pct-text { font-size: 12px; font-weight: 700; color: var(--color-text); }

    .clickable-task-row { cursor: pointer; transition: background 0.15s ease; }
    .clickable-task-row:hover { background: var(--color-primary-light); }
    .type-tag { font-size: 11px; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; font-weight: 600; color: #475569; }
  `]
})
export class MembersKpiComponent implements OnInit {
  router = inject(Router);
  private kpiService = inject(KpiService);

  members: MemberKpiSummary[] = [];
  detail: MemberKpiDetail | null = null;
  loading = false;

  periodMonth = new Date().toISOString().slice(0, 7);
  searchText = '';
  statusFilter?: MemberKpiStatus;
  sortKey: SortKey = 'fullName';
  sortAsc = true;

  private readonly statusLabels: Record<MemberKpiStatus, string> = {
    DAT: 'ĐẠT',
    CANH_BAO: 'CẢNH BÁO',
    NGUY_HIEM: 'NGUY HIỂM'
  };

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.kpiService.getMembersKpi(this.periodMonth).subscribe({
      next: members => { this.members = members; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  statusLabel(status: MemberKpiStatus): string {
    return this.statusLabels[status] ?? status;
  }

  initials(name: string): string {
    return name.split(' ').map(p => p[0]).slice(-2).join('').toUpperCase();
  }

  uniqueMemberCount(): number {
    return new Set(this.members.map(m => m.userId)).size;
  }

  avgKpi(): number {
    const withKpi = this.members.filter(m => m.kpi != null);
    if (withKpi.length === 0) return 0;
    return withKpi.reduce((sum, m) => sum + (m.kpi ?? 0), 0) / withKpi.length;
  }

  countByStatus(status: MemberKpiStatus): number {
    const userIds = new Set<number>();
    this.members.filter(m => m.kpiStatus === status).forEach(m => userIds.add(m.userId));
    return userIds.size;
  }

  sortBy(key: SortKey) {
    if (this.sortKey === key) {
      this.sortAsc = !this.sortAsc;
    } else {
      this.sortKey = key;
      this.sortAsc = true;
    }
  }

  filteredMembers(): MemberKpiSummary[] {
    let list = this.members;
    if (this.searchText) {
      const q = this.searchText.toLowerCase();
      list = list.filter(m =>
        m.fullName.toLowerCase().includes(q) ||
        (m.groupName && m.groupName.toLowerCase().includes(q))
      );
    }
    if (this.statusFilter) {
      list = list.filter(m => m.kpiStatus === this.statusFilter);
    }
    const dir = this.sortAsc ? 1 : -1;
    return [...list].sort((a, b) => {
      const av = a[this.sortKey] ?? 0;
      const bv = b[this.sortKey] ?? 0;
      if (av < bv) return -1 * dir;
      if (av > bv) return 1 * dir;
      return 0;
    });
  }

  openDetail(userId: number, groupId?: number) {
    this.kpiService.getMemberKpiDetail(userId, this.periodMonth, groupId).subscribe(detail => this.detail = detail);
  }
}
