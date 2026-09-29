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
    <div class="members-kpi-page">
      <div class="header">
        <h1>Thành viên & KPI</h1>
        <input type="month" [(ngModel)]="periodMonth" (change)="load()">
      </div>

      <div class="summary-strip" *ngIf="members.length > 0">
        <div class="stat"><span class="value">{{ uniqueMemberCount() }}</span><span class="label">Tổng thành viên</span></div>
        <div class="stat">
          <span class="value">{{ avgKpi() | number:'1.0-1' }}</span>
          <span class="label">KPI trung bình</span>
        </div>
        <div class="stat">
          <span class="value">{{ countByStatus('DAT') }}/{{ uniqueMemberCount() }}</span>
          <span class="label">Đạt mục tiêu (≥80)</span>
        </div>
        <div class="stat danger" *ngIf="countByStatus('NGUY_HIEM') > 0">
          <span class="value">{{ countByStatus('NGUY_HIEM') }}</span>
          <span class="label">Cần chú ý (&lt;60)</span>
        </div>
      </div>

      <div class="filters">
        <input type="text" [(ngModel)]="searchText" placeholder="Tìm tên thành viên hoặc nhóm...">
        <select [(ngModel)]="statusFilter">
          <option [ngValue]="undefined">Tất cả</option>
          <option value="DAT">Đạt</option>
          <option value="CANH_BAO">Cảnh báo</option>
          <option value="NGUY_HIEM">Nguy hiểm</option>
        </select>
      </div>

      <table class="table-clean" *ngIf="!loading">
        <thead>
          <tr>
            <th>#</th>
            <th (click)="sortBy('fullName')" class="sortable">Thành viên</th>
            <th>Nhóm</th>
            <th (click)="sortBy('kpi')" class="sortable">KPI</th>
            <th>Fast Task</th>
            <th>Multi-step</th>
            <th (click)="sortBy('totalDone')" class="sortable">Hoàn thành</th>
            <th (click)="sortBy('overdueCount')" class="sortable">Quá hạn</th>
            <th>Trạng thái</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let m of filteredMembers(); let i = index">
            <td>{{ i + 1 }}</td>
            <td>
              <div class="member-cell">
                <span class="avatar">{{ initials(m.fullName) }}</span>
                <span>{{ m.fullName }}</span>
                <span class="role-badge">{{ m.role }}</span>
              </div>
            </td>
            <td>{{ m.groupName || '—' }}</td>
            <td [ngClass]="'kpi-' + m.kpiStatus">{{ m.kpi != null ? (m.kpi | number:'1.0-1') : '—' }}</td>
            <td>{{ m.fastDone }}/{{ m.fastTotal }}</td>
            <td>{{ m.multiStepDone }}/{{ m.multiStepTotal }}</td>
            <td>{{ m.totalDone }}</td>
            <td [class.overdue]="m.overdueCount > 0">{{ m.overdueCount }}</td>
            <td><span class="status-badge" [ngClass]="'status-' + m.kpiStatus">{{ statusLabel(m.kpiStatus) }}</span></td>
            <td><button (click)="openDetail(m.userId, m.groupId)">Chi tiết</button></td>
          </tr>
        </tbody>
      </table>
      <div *ngIf="loading" class="loading">Đang tải...</div>
      <div *ngIf="!loading && members.length === 0" class="empty">Không có thành viên nào.</div>
    </div>

    <!-- DETAIL POPUP -->
    <div class="modal-overlay" *ngIf="detail" (click)="detail = null">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <div>
            <strong>{{ detail.fullName }}</strong> · {{ detail.role }} · Kỳ {{ detail.periodMonth }}
            <span *ngIf="detail.groupName" class="group-badge">{{ detail.groupName }}</span>
          </div>
          <button class="close-btn" (click)="detail = null"><span class="icon">close</span></button>
        </div>

        <div class="kpi-summary">
          <span class="big-kpi" [ngClass]="'kpi-' + detail.kpiStatus">{{ detail.kpiFinal != null ? (detail.kpiFinal | number:'1.0-1') : '—' }} / 100</span>
          <span *ngIf="detail.changeVsPreviousMonth != null" [ngClass]="detail.changeVsPreviousMonth >= 0 ? 'trend-up' : 'trend-down'">
            <span class="icon">{{ detail.changeVsPreviousMonth >= 0 ? 'arrow_upward' : 'arrow_downward' }}</span> {{ detail.changeVsPreviousMonth | number:'1.0-1' }} so với tháng trước
          </span>
          <div class="status-line">
            Trạng thái: <span class="status-badge" [ngClass]="'status-' + detail.kpiStatus">{{ statusLabel(detail.kpiStatus) }}</span>
            &nbsp;·&nbsp; Lead đã chấm: {{ detail.leadHasScored ? 'Có' : 'Chưa' }}
            &nbsp;·&nbsp; Manager duyệt: {{ detail.managerHasFinalized ? 'Đã duyệt' : 'Chờ' }}
          </div>
        </div>

        <h4>Breakdown theo loại task</h4>
        <table class="breakdown-table">
          <thead><tr><th>Loại</th><th>Kết quả</th><th>% hoàn thành</th></tr></thead>
          <tbody>
            <tr>
              <td>Fast Task</td>
              <td>{{ detail.fastTask.done }}/{{ detail.fastTask.total }}</td>
              <td>{{ detail.fastTask.completionPercent }}%</td>
            </tr>
            <tr>
              <td>Multi-step</td>
              <td>{{ detail.multiStep.done }}/{{ detail.multiStep.total }}</td>
              <td>{{ detail.multiStep.completionPercent }}%</td>
            </tr>
          </tbody>
          <tfoot>
            <tr><td colspan="2">Auto Score</td><td>{{ detail.autoScore != null ? (detail.autoScore | number:'1.0-1') : '—' }}</td></tr>
            <tr><td colspan="2">Lead Score</td><td>{{ detail.leadScore != null ? (detail.leadScore | number:'1.0-1') : '—' }}</td></tr>
            <tr><td colspan="2">Manager Score</td><td>{{ detail.managerScore != null ? (detail.managerScore | number:'1.0-1') : '—' }}</td></tr>
            <tr class="final-row"><td colspan="2">ĐIỂM CUỐI</td><td>{{ detail.kpiFinal != null ? (detail.kpiFinal | number:'1.0-1') : '—' }}</td></tr>
          </tfoot>
        </table>

        <h4>Danh sách task trong nhóm ({{ detail.tasks.length }})</h4>
        <table class="task-lines-table" *ngIf="detail.tasks.length > 0">
          <thead><tr><th>Tên task</th><th>Loại</th><th>Trạng thái</th><th>Deadline</th></tr></thead>
          <tbody>
            <tr *ngFor="let t of detail.tasks" (click)="router.navigate(['/tasks', t.taskId]); detail = null" class="task-row">
              <td>{{ t.title }}</td>
              <td>{{ t.timeCategory }}</td>
              <td>{{ t.status }}</td>
              <td>{{ t.dueDate ? (t.dueDate | date:'shortDate') : '—' }}</td>
            </tr>
          </tbody>
        </table>
        <p *ngIf="detail.tasks.length === 0" class="muted">Không có task nào trong nhóm này.</p>
      </div>
    </div>
  `,
  styles: [`
    .members-kpi-page { padding: 20px; max-width: 1200px; margin: 0 auto; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .header input[type="month"] { padding: 8px; border: 1px solid #ddd; border-radius: 10px; }
    .summary-strip { display: flex; gap: 24px; margin-bottom: 16px; padding: 16px; background: #f5f6f8; border-radius: 16px; }
    .stat { display: flex; flex-direction: column; }
    .stat .value { font-size: 22px; font-weight: bold; }
    .stat .label { font-size: 12px; color: #777; }
    .stat.danger .value { color: #c62828; }
    .filters { display: flex; gap: 10px; margin-bottom: 16px; }
    .filters input, .filters select { padding: 8px; border: 1px solid #ddd; border-radius: 10px; }
    th.sortable { cursor: pointer; user-select: none; }
    th.sortable:hover { color: #2563eb; }
    .member-cell { display: flex; align-items: center; gap: 8px; }
    .avatar { width: 26px; height: 26px; border-radius: 50%; background: #2563eb; color: white; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: bold; }
    .role-badge { font-size: 10px; background: #eee; padding: 2px 6px; border-radius: 16px; color: #666; }
    .kpi-DAT { color: #2e7d32; font-weight: bold; }
    .kpi-CANH_BAO { color: #ef6c00; font-weight: bold; }
    .kpi-NGUY_HIEM { color: #c62828; font-weight: bold; }
    .overdue { color: #c62828; font-weight: bold; }
    .status-badge { font-size: 11px; padding: 3px 10px; border-radius: 10px; font-weight: 600; }
    .status-DAT { background: #c8e6c9; color: #2e7d32; }
    .status-CANH_BAO { background: #ffe0b2; color: #ef6c00; }
    .status-NGUY_HIEM { background: #ffcdd2; color: #c62828; }
    .loading, .empty { text-align: center; color: #999; padding: 40px; }
    table td button { padding: 4px 12px; border: 1px solid #ddd; border-radius: 10px; background: white; cursor: pointer; }

    .modal-overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex;
      align-items: center; justify-content: center; z-index: 1000;
    }
    .modal-content {
      background: white; border-radius: 16px; padding: 24px; width: 900px; max-width: 92vw;
      max-height: 85vh; overflow-y: auto;
    }
    .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .modal-header div { display: flex; align-items: center; gap: 8px; }
    .group-badge { font-size: 11px; background: #e3f2fd; color: #1d4ed8; padding: 3px 8px; border-radius: 10px; font-weight: 600; }
    .close-btn { background: none; border: none; font-size: 22px; cursor: pointer; }
    .kpi-summary { padding-bottom: 12px; border-bottom: 1px solid #eee; margin-bottom: 16px; }
    .big-kpi { font-size: 28px; font-weight: bold; margin-right: 12px; }
    .trend-up { color: #2e7d32; }
    .trend-down { color: #c62828; }
    .status-line { margin-top: 8px; font-size: 13px; color: #555; }
    .breakdown-table, .task-lines-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
    .breakdown-table th, .breakdown-table td, .task-lines-table th, .task-lines-table td {
      padding: 8px 10px; border-bottom: 1px solid #eee; font-size: 13px; text-align: left;
    }
    .breakdown-table tfoot td { font-weight: 600; }
    .final-row td { font-weight: bold; color: #2563eb; border-top: 2px solid #ddd; }
    .task-row { cursor: pointer; }
    .task-row:hover { background: #f5f6f8; }
    .muted { color: #999; }
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
