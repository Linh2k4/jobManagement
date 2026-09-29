import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EvaluationService } from '../../../../core/services/api/evaluation.service';
import { AuthStore } from '../../../../core/stores/auth.store';
import { Evaluation, EvaluationScoresRequest } from '../../../../core/models';

type Scope = 'my' | 'team' | 'all';
type ReviewMode = 'self' | 'lead' | 'manager' | 'view';

@Component({
  selector: 'app-evaluation-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="evaluations-page">
      <div class="header">
        <h1>Đánh giá</h1>
        <input type="month" [(ngModel)]="periodMonth" (change)="loadAll()">
      </div>

      <section class="eval-section">
        <h2>Đánh giá của tôi</h2>
        <div *ngIf="myLoading" class="loading">Đang tải...</div>
        <ng-container *ngIf="!myLoading">
          <table class="table-clean" *ngIf="myEvaluations.length > 0">
            <thead>
              <tr>
                <th>Kỳ</th><th>Nhóm</th><th>Trạng thái</th><th>KPI cuối</th><th></th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let e of myEvaluations">
                <td>{{ e.periodMonth }}</td>
                <td>{{ e.groupName || '—' }}</td>
                <td><span class="status-badge" [ngClass]="'status-' + e.status">{{ statusLabel(e.status) }}</span></td>
                <td>{{ e.kpiFinal != null ? (e.kpiFinal | number:'1.0-1') : '—' }}</td>
                <td>
                  <button *ngIf="e.status === 'DRAFT' && !e.isLocked" (click)="openModal(e, 'self')">Tự đánh giá</button>
                  <button *ngIf="e.status !== 'DRAFT'" class="btn-secondary" (click)="openModal(e, 'view')">Xem</button>
                </td>
              </tr>
            </tbody>
          </table>
          <div *ngIf="myEvaluations.length === 0" class="empty">
            Chưa có đánh giá cho kỳ {{ periodMonth }}.
            <button (click)="createMine()">Tạo đánh giá cho kỳ này</button>
          </div>
        </ng-container>
      </section>

      <section class="eval-section" *ngIf="auth.isLead()">
        <h2>Nhóm tôi phụ trách</h2>
        <div *ngIf="teamLoading" class="loading">Đang tải...</div>
        <table class="table-clean" *ngIf="!teamLoading && teamEvaluations.length > 0">
          <thead>
            <tr>
              <th>Thành viên</th><th>Nhóm</th><th>Trạng thái</th><th>Tự đánh giá</th><th>Lead score</th><th></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let e of teamEvaluations">
              <td>{{ e.userFullName }}</td>
              <td>{{ e.groupName || '—' }}</td>
              <td><span class="status-badge" [ngClass]="'status-' + e.status">{{ statusLabel(e.status) }}</span></td>
              <td>{{ e.selfSubmittedAt ? 'Đã nộp' : 'Chưa nộp' }}</td>
              <td>{{ e.leadScore != null ? (e.leadScore | number:'1.0-1') : '—' }}</td>
              <td>
                <button *ngIf="e.status === 'SELF_SUBMITTED'" (click)="openModal(e, 'lead')">Chấm điểm</button>
                <button *ngIf="e.status === 'DRAFT'" class="btn-secondary" disabled title="Thành viên chưa tự đánh giá">Chờ tự đánh giá</button>
                <button *ngIf="e.status === 'LEAD_REVIEWED' || e.status === 'FINALIZED'" class="btn-secondary" (click)="openModal(e, 'view')">Xem</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div *ngIf="!teamLoading && teamEvaluations.length === 0" class="empty">Không có đánh giá nào trong nhóm cho kỳ {{ periodMonth }}.</div>
      </section>

      <section class="eval-section" *ngIf="auth.isManager()">
        <h2>Toàn công ty</h2>
        <div *ngIf="allLoading" class="loading">Đang tải...</div>
        <table class="table-clean" *ngIf="!allLoading && allEvaluations.length > 0">
          <thead>
            <tr>
              <th>Thành viên</th><th>Nhóm</th><th>Trạng thái</th><th>Lead score</th><th>KPI cuối</th><th></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let e of allEvaluations">
              <td>{{ e.userFullName }}</td>
              <td>{{ e.groupName || '—' }}</td>
              <td><span class="status-badge" [ngClass]="'status-' + e.status">{{ statusLabel(e.status) }}</span></td>
              <td>{{ e.leadScore != null ? (e.leadScore | number:'1.0-1') : '—' }}</td>
              <td>{{ e.kpiFinal != null ? (e.kpiFinal | number:'1.0-1') : '—' }}</td>
              <td>
                <button *ngIf="e.status === 'LEAD_REVIEWED'" (click)="openModal(e, 'manager')">Chốt điểm</button>
                <button *ngIf="e.status !== 'LEAD_REVIEWED'" class="btn-secondary" (click)="openModal(e, 'view')">Xem</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div *ngIf="!allLoading && allEvaluations.length === 0" class="empty">Không có đánh giá nào cho kỳ {{ periodMonth }}.</div>
      </section>
    </div>

    <!-- SCORE MODAL -->
    <div class="modal-overlay" *ngIf="modalEval" (click)="closeModal()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <strong>{{ modalTitle() }}</strong>
          <button class="close-btn" (click)="closeModal()"><span class="icon">close</span></button>
        </div>

        <div class="score-grid">
          <div class="score-row" *ngIf="modalMode !== 'view'">
            <label>Chất lượng công việc (1-10)</label>
            <input type="number" min="1" max="10" step="0.5" [(ngModel)]="form.quality">
          </div>
          <div class="score-row" *ngIf="modalMode !== 'view'">
            <label>Tinh thần trách nhiệm (1-10)</label>
            <input type="number" min="1" max="10" step="0.5" [(ngModel)]="form.responsibility">
          </div>
          <div class="score-row" *ngIf="modalMode === 'self' || modalMode === 'lead'">
            <label>Tinh thần đồng đội (1-10)</label>
            <input type="number" min="1" max="10" step="0.5" [(ngModel)]="form.teamwork">
          </div>
          <div class="score-row" *ngIf="modalMode === 'self' || modalMode === 'manager'">
            <label>Chủ động, sáng tạo (1-10)</label>
            <input type="number" min="1" max="10" step="0.5" [(ngModel)]="form.initiative">
          </div>
          <div class="score-row" *ngIf="modalMode !== 'view'">
            <label>Kỷ luật (1-10)</label>
            <input type="number" min="1" max="10" step="0.5" [(ngModel)]="form.discipline">
          </div>
          <div class="score-row full-width" *ngIf="modalMode !== 'view'">
            <label>Ghi chú</label>
            <textarea rows="3" [(ngModel)]="form.notes"></textarea>
          </div>
        </div>

        <!-- VIEW MODE: read-only summary -->
        <div class="view-summary" *ngIf="modalMode === 'view' && modalEval">
          <div class="view-block" *ngIf="modalEval.selfQuality != null">
            <h4>Tự đánh giá</h4>
            <p>Chất lượng: {{ modalEval.selfQuality }} · Trách nhiệm: {{ modalEval.selfResponsibility }} · Đồng đội: {{ modalEval.selfTeamwork }} · Chủ động: {{ modalEval.selfInitiative }} · Kỷ luật: {{ modalEval.selfDiscipline }}</p>
            <p *ngIf="modalEval.selfNotes" class="muted">"{{ modalEval.selfNotes }}"</p>
          </div>
          <div class="view-block" *ngIf="modalEval.leadQuality != null">
            <h4>Lead đánh giá {{ modalEval.leadScore != null ? '(' + (modalEval.leadScore | number:'1.0-1') + ')' : '' }}</h4>
            <p>Chất lượng: {{ modalEval.leadQuality }} · Trách nhiệm: {{ modalEval.leadResponsibility }} · Đồng đội: {{ modalEval.leadTeamwork }} · Kỷ luật: {{ modalEval.leadDiscipline }}</p>
            <p *ngIf="modalEval.leadNotes" class="muted">"{{ modalEval.leadNotes }}"</p>
          </div>
          <div class="view-block" *ngIf="modalEval.managerQuality != null">
            <h4>Manager đánh giá {{ modalEval.managerScore != null ? '(' + (modalEval.managerScore | number:'1.0-1') + ')' : '' }}</h4>
            <p>Chất lượng: {{ modalEval.managerQuality }} · Trách nhiệm: {{ modalEval.managerResponsibility }} · Chủ động: {{ modalEval.managerInitiative }} · Kỷ luật: {{ modalEval.managerDiscipline }}</p>
            <p *ngIf="modalEval.managerNotes" class="muted">"{{ modalEval.managerNotes }}"</p>
          </div>
          <div class="view-block final" *ngIf="modalEval.kpiFinal != null">
            <h4>KPI cuối cùng</h4>
            <p class="big">{{ modalEval.kpiFinal | number:'1.0-1' }} / 100</p>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn-secondary" (click)="closeModal()">Đóng</button>
          <button *ngIf="modalMode !== 'view'" (click)="submitModal()" [disabled]="submitting">
            {{ submitting ? 'Đang gửi...' : submitLabel() }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .evaluations-page { padding: 20px; max-width: 1100px; margin: 0 auto; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .header input[type="month"] { padding: 8px; border: 1px solid #ddd; border-radius: 10px; }
    .eval-section { margin-bottom: 32px; }
    .eval-section h2 { font-size: 16px; margin-bottom: 10px; }
    .status-badge { font-size: 11px; padding: 3px 10px; border-radius: 10px; font-weight: 600; }
    .status-DRAFT { background: #eee; color: #666; }
    .status-SELF_SUBMITTED { background: #fff3cd; color: #856404; }
    .status-LEAD_REVIEWED { background: #cfe2ff; color: #084298; }
    .status-FINALIZED { background: #c8e6c9; color: #2e7d32; }
    .loading, .empty { text-align: center; color: #999; padding: 24px; background: white; }
    .empty button { margin-left: 12px; }
    table td button, .empty button { padding: 4px 12px; border: 1px solid #2563eb; border-radius: 10px; background: #2563eb; color: white; cursor: pointer; }
    table td button.btn-secondary, .btn-secondary { background: white; color: #333; border-color: #ddd; }
    table td button:disabled { background: #eee; color: #999; border-color: #eee; cursor: not-allowed; }

    .modal-overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex;
      align-items: center; justify-content: center; z-index: 1000;
    }
    .modal-content {
      background: white; border-radius: 16px; padding: 24px; width: 600px; max-width: 92vw;
      max-height: 85vh; overflow-y: auto;
    }
    .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .close-btn { background: none; border: none; font-size: 22px; cursor: pointer; }
    .score-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 16px; }
    .score-row { display: flex; flex-direction: column; gap: 4px; }
    .score-row.full-width { grid-column: 1 / -1; }
    .score-row label { font-size: 13px; color: #555; }
    .score-row input, .score-row textarea { padding: 8px; border: 1px solid #ddd; border-radius: 10px; }
    .view-summary { margin-bottom: 16px; }
    .view-block { padding: 10px 0; border-bottom: 1px solid #eee; }
    .view-block h4 { margin: 0 0 6px; font-size: 13px; color: #2563eb; }
    .view-block p { margin: 0; font-size: 13px; }
    .view-block.final .big { font-size: 24px; font-weight: bold; color: #2563eb; }
    .muted { color: #999; font-style: italic; margin-top: 4px !important; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 8px; }
    .modal-actions button { padding: 8px 16px; border-radius: 10px; cursor: pointer; }
    .modal-actions button:not(.btn-secondary) { background: #2563eb; color: white; border: 1px solid #2563eb; }
    .modal-actions button:disabled { opacity: 0.6; cursor: not-allowed; }
  `]
})
export class EvaluationListComponent implements OnInit {
  auth = inject(AuthStore);
  private evaluationService = inject(EvaluationService);

  periodMonth = new Date().toISOString().slice(0, 7);

  myEvaluations: Evaluation[] = [];
  teamEvaluations: Evaluation[] = [];
  allEvaluations: Evaluation[] = [];
  myLoading = false;
  teamLoading = false;
  allLoading = false;

  modalEval: Evaluation | null = null;
  modalMode: ReviewMode = 'view';
  submitting = false;
  form: EvaluationScoresRequest = { quality: 5, responsibility: 5, teamwork: 5, initiative: 5, discipline: 5, notes: '' };

  private readonly statusLabels: Record<string, string> = {
    DRAFT: 'Chưa đánh giá',
    SELF_SUBMITTED: 'Đã tự đánh giá',
    LEAD_REVIEWED: 'Lead đã chấm',
    FINALIZED: 'Hoàn thành'
  };

  ngOnInit() {
    this.loadAll();
  }

  loadAll() {
    this.loadMy();
    if (this.auth.isLead()) this.loadTeam();
    if (this.auth.isManager()) this.loadAll_();
  }

  loadMy() {
    this.myLoading = true;
    this.evaluationService.getMyEvaluations(0, 50, { periodMonth: this.periodMonth }).subscribe({
      next: res => { this.myEvaluations = res.data.content; this.myLoading = false; },
      error: () => { this.myLoading = false; }
    });
  }

  loadTeam() {
    this.teamLoading = true;
    this.evaluationService.getTeamEvaluations(0, 50, { periodMonth: this.periodMonth }).subscribe({
      next: res => {
        this.teamEvaluations = res.data.content;
        this.teamLoading = false;
      },
      error: () => { this.teamLoading = false; }
    });
  }

  private loadAll_() {
    this.allLoading = true;
    this.evaluationService.getAllEvaluations(0, 100, { periodMonth: this.periodMonth }).subscribe({
      next: res => { this.allEvaluations = res.data.content; this.allLoading = false; },
      error: () => { this.allLoading = false; }
    });
  }

  createMine() {
    const me = this.auth.user();
    if (!me) return;
    this.evaluationService.createOrGetEvaluation(me.id, this.periodMonth).subscribe(() => this.loadMy());
  }

  statusLabel(status: string): string {
    return this.statusLabels[status] ?? status;
  }

  modalTitle(): string {
    const labels: Record<ReviewMode, string> = {
      self: 'Tự đánh giá', lead: 'Lead chấm điểm', manager: 'Manager chốt điểm', view: 'Chi tiết đánh giá'
    };
    return labels[this.modalMode];
  }

  submitLabel(): string {
    const labels: Record<ReviewMode, string> = {
      self: 'Gửi tự đánh giá', lead: 'Gửi chấm điểm', manager: 'Chốt điểm', view: ''
    };
    return labels[this.modalMode];
  }

  openModal(evaluation: Evaluation, mode: ReviewMode) {
    this.modalEval = evaluation;
    this.modalMode = mode;
    this.form = { quality: 5, responsibility: 5, teamwork: 5, initiative: 5, discipline: 5, notes: '' };
  }

  closeModal() {
    this.modalEval = null;
    this.submitting = false;
  }

  submitModal() {
    if (!this.modalEval) return;
    this.submitting = true;
    const request = { ...this.form };

    const done = () => {
      this.submitting = false;
      this.closeModal();
      this.loadAll();
    };

    if (this.modalMode === 'self') {
      this.evaluationService.submitSelfReview(this.modalEval.id, request).subscribe({ next: done, error: () => this.submitting = false });
    } else if (this.modalMode === 'lead') {
      this.evaluationService.submitLeadReview(this.modalEval.id, request).subscribe({ next: done, error: () => this.submitting = false });
    } else if (this.modalMode === 'manager') {
      this.evaluationService.finalizeEvaluation(this.modalEval.id, request).subscribe({ next: done, error: () => this.submitting = false });
    }
  }
}
