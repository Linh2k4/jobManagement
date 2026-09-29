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
    <div class="evaluations-page animate-fade-in">
      <div class="page-header">
        <div class="header-left">
          <h1>Đánh giá & KPI</h1>
          <p class="subtitle">Quy trình tự đánh giá (Member) → Chấm điểm (Lead) → Chốt điểm (Manager)</p>
        </div>
        <div class="header-right">
          <div class="month-selector-wrapper">
            <span class="icon">calendar_month</span>
            <input type="month" [(ngModel)]="periodMonth" (change)="loadAll()" class="month-input">
          </div>
        </div>
      </div>

      <!-- MY EVALUATIONS SECTION -->
      <section class="eval-section card">
        <div class="section-title-row">
          <div class="title-with-icon">
            <span class="icon section-icon">person</span>
            <h2>Đánh giá của tôi</h2>
          </div>
          <span class="badge badge-primary">{{ myEvaluations.length }} bản ghi</span>
        </div>

        <div *ngIf="myLoading" class="loading-state">
          <span class="icon spinner">sync</span>
          <p>Đang tải dữ liệu đánh giá...</p>
        </div>

        <ng-container *ngIf="!myLoading">
          <div class="table-responsive" *ngIf="myEvaluations.length > 0">
            <table class="table-clean">
              <thead>
                <tr>
                  <th>Kỳ đánh giá</th>
                  <th>Nhóm</th>
                  <th>Trạng thái</th>
                  <th>KPI cuối cùng</th>
                  <th class="text-right">Hành động</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let e of myEvaluations">
                  <td><strong>{{ e.periodMonth }}</strong></td>
                  <td>
                    <span class="group-pill" *ngIf="e.groupName">{{ e.groupName }}</span>
                    <span class="text-muted" *ngIf="!e.groupName">—</span>
                  </td>
                  <td>
                    <span class="status-badge" [ngClass]="'status-' + e.status">
                      {{ statusLabel(e.status) }}
                    </span>
                  </td>
                  <td>
                    <span class="kpi-score" *ngIf="e.kpiFinal != null">{{ e.kpiFinal | number:'1.0-1' }} / 100</span>
                    <span class="text-muted" *ngIf="e.kpiFinal == null">—</span>
                  </td>
                  <td class="text-right">
                    <button
                      *ngIf="e.status === 'DRAFT' && !e.isLocked"
                      class="btn btn-primary btn-sm"
                      (click)="openModal(e, 'self')"
                    >
                      <span class="icon">edit_note</span> Tự đánh giá
                    </button>
                    <button
                      *ngIf="e.status !== 'DRAFT'"
                      class="btn btn-secondary btn-sm"
                      (click)="openModal(e, 'view')"
                    >
                      <span class="icon">visibility</span> Xem chi tiết
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div *ngIf="myEvaluations.length === 0" class="empty-state">
            <span class="icon empty-icon">sentiment_neutral</span>
            <p>Chưa có bản ghi đánh giá cho kỳ <strong>{{ periodMonth }}</strong>.</p>
            <button class="btn btn-primary" (click)="createMine()">
              <span class="icon">add_circle</span> Tạo đánh giá cho kỳ này
            </button>
          </div>
        </ng-container>
      </section>

      <!-- TEAM EVALUATIONS SECTION (LEAD) -->
      <section class="eval-section card" *ngIf="auth.isLead()">
        <div class="section-title-row">
          <div class="title-with-icon">
            <span class="icon section-icon">groups</span>
            <h2>Nhóm tôi phụ trách</h2>
          </div>
          <span class="badge badge-info">{{ teamEvaluations.length }} thành viên</span>
        </div>

        <div *ngIf="teamLoading" class="loading-state">
          <span class="icon spinner">sync</span>
          <p>Đang tải dữ liệu nhóm...</p>
        </div>

        <div class="table-responsive" *ngIf="!teamLoading && teamEvaluations.length > 0">
          <table class="table-clean">
            <thead>
              <tr>
                <th>Thành viên</th>
                <th>Nhóm</th>
                <th>Trạng thái</th>
                <th>Tự đánh giá</th>
                <th>Lead Score</th>
                <th class="text-right">Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let e of teamEvaluations">
                <td>
                  <div class="user-cell">
                    <div class="avatar-mini">{{ initials(e.userFullName) }}</div>
                    <span>{{ e.userFullName }}</span>
                  </div>
                </td>
                <td>
                  <span class="group-pill" *ngIf="e.groupName">{{ e.groupName }}</span>
                  <span class="text-muted" *ngIf="!e.groupName">—</span>
                </td>
                <td>
                  <span class="status-badge" [ngClass]="'status-' + e.status">
                    {{ statusLabel(e.status) }}
                  </span>
                </td>
                <td>
                  <span class="badge badge-success" *ngIf="e.selfSubmittedAt">Đã nộp</span>
                  <span class="badge badge-warning" *ngIf="!e.selfSubmittedAt">Chưa nộp</span>
                </td>
                <td>
                  <span class="kpi-score" *ngIf="e.leadScore != null">{{ e.leadScore | number:'1.0-1' }}</span>
                  <span class="text-muted" *ngIf="e.leadScore == null">—</span>
                </td>
                <td class="text-right">
                  <button
                    *ngIf="e.status === 'SELF_SUBMITTED'"
                    class="btn btn-primary btn-sm"
                    (click)="openModal(e, 'lead')"
                  >
                    <span class="icon">grade</span> Chấm điểm
                  </button>
                  <button
                    *ngIf="e.status === 'DRAFT'"
                    class="btn btn-secondary btn-sm"
                    disabled
                    title="Thành viên chưa gửi tự đánh giá"
                  >
                    Chờ tự đánh giá
                  </button>
                  <button
                    *ngIf="e.status === 'LEAD_REVIEWED' || e.status === 'FINALIZED'"
                    class="btn btn-secondary btn-sm"
                    (click)="openModal(e, 'view')"
                  >
                    <span class="icon">visibility</span> Xem
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div *ngIf="!teamLoading && teamEvaluations.length === 0" class="empty-state">
          <p>Không có đánh giá nào trong nhóm cho kỳ {{ periodMonth }}.</p>
        </div>
      </section>

      <!-- ALL EVALUATIONS SECTION (MANAGER) -->
      <section class="eval-section card" *ngIf="auth.isManager()">
        <div class="section-title-row">
          <div class="title-with-icon">
            <span class="icon section-icon">domain</span>
            <h2>Toàn công ty (Manager)</h2>
          </div>
          <span class="badge badge-info">{{ allEvaluations.length }} bản ghi</span>
        </div>

        <div *ngIf="allLoading" class="loading-state">
          <span class="icon spinner">sync</span>
          <p>Đang tải toàn bộ dữ liệu...</p>
        </div>

        <div class="table-responsive" *ngIf="!allLoading && allEvaluations.length > 0">
          <table class="table-clean">
            <thead>
              <tr>
                <th>Thành viên</th>
                <th>Nhóm</th>
                <th>Trạng thái</th>
                <th>Lead Score</th>
                <th>KPI cuối</th>
                <th class="text-right">Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let e of allEvaluations">
                <td>
                  <div class="user-cell">
                    <div class="avatar-mini">{{ initials(e.userFullName) }}</div>
                    <span>{{ e.userFullName }}</span>
                  </div>
                </td>
                <td>
                  <span class="group-pill" *ngIf="e.groupName">{{ e.groupName }}</span>
                  <span class="text-muted" *ngIf="!e.groupName">—</span>
                </td>
                <td>
                  <span class="status-badge" [ngClass]="'status-' + e.status">
                    {{ statusLabel(e.status) }}
                  </span>
                </td>
                <td>
                  <span class="kpi-score" *ngIf="e.leadScore != null">{{ e.leadScore | number:'1.0-1' }}</span>
                  <span class="text-muted" *ngIf="e.leadScore == null">—</span>
                </td>
                <td>
                  <span class="kpi-score font-bold" *ngIf="e.kpiFinal != null">{{ e.kpiFinal | number:'1.0-1' }}</span>
                  <span class="text-muted" *ngIf="e.kpiFinal == null">—</span>
                </td>
                <td class="text-right">
                  <button
                    *ngIf="e.status === 'LEAD_REVIEWED'"
                    class="btn btn-primary btn-sm"
                    (click)="openModal(e, 'manager')"
                  >
                    <span class="icon">done_all</span> Chốt điểm
                  </button>
                  <button
                    *ngIf="e.status !== 'LEAD_REVIEWED'"
                    class="btn btn-secondary btn-sm"
                    (click)="openModal(e, 'view')"
                  >
                    <span class="icon">visibility</span> Xem
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div *ngIf="!allLoading && allEvaluations.length === 0" class="empty-state">
          <p>Không có đánh giá nào cho kỳ {{ periodMonth }}.</p>
        </div>
      </section>
    </div>

    <!-- MODERN SCORE / REVIEW MODAL -->
    <div class="modal-overlay" *ngIf="modalEval" (click)="closeModal()">
      <div class="modal-content animate-fade-in" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <div class="modal-title-wrap">
            <div class="modal-icon-badge">
              <span class="icon">{{ modalIcon() }}</span>
            </div>
            <div>
              <h3>{{ modalTitle() }}</h3>
              <p class="modal-sub">Kỳ đánh giá: <strong>{{ modalEval.periodMonth }}</strong> · Thành viên: <strong>{{ modalEval.userFullName }}</strong></p>
            </div>
          </div>
          <button class="close-btn" (click)="closeModal()" aria-label="Đóng">
            <span class="icon">close</span>
          </button>
        </div>

        <!-- FORM MODE (Self / Lead / Manager review) -->
        <div class="score-grid" *ngIf="modalMode !== 'view'">
          <div class="score-row">
            <div class="score-label-row">
              <label>Chất lượng công việc</label>
              <span class="score-pill">{{ form.quality }} / 10</span>
            </div>
            <input type="number" min="1" max="10" step="0.5" [(ngModel)]="form.quality" class="form-input">
          </div>

          <div class="score-row">
            <div class="score-label-row">
              <label>Tinh thần trách nhiệm</label>
              <span class="score-pill">{{ form.responsibility }} / 10</span>
            </div>
            <input type="number" min="1" max="10" step="0.5" [(ngModel)]="form.responsibility" class="form-input">
          </div>

          <div class="score-row" *ngIf="modalMode === 'self' || modalMode === 'lead'">
            <div class="score-label-row">
              <label>Tinh thần đồng đội</label>
              <span class="score-pill">{{ form.teamwork }} / 10</span>
            </div>
            <input type="number" min="1" max="10" step="0.5" [(ngModel)]="form.teamwork" class="form-input">
          </div>

          <div class="score-row" *ngIf="modalMode === 'self' || modalMode === 'manager'">
            <div class="score-label-row">
              <label>Chủ động, sáng tạo</label>
              <span class="score-pill">{{ form.initiative }} / 10</span>
            </div>
            <input type="number" min="1" max="10" step="0.5" [(ngModel)]="form.initiative" class="form-input">
          </div>

          <div class="score-row">
            <div class="score-label-row">
              <label>Kỷ luật & Tác phong</label>
              <span class="score-pill">{{ form.discipline }} / 10</span>
            </div>
            <input type="number" min="1" max="10" step="0.5" [(ngModel)]="form.discipline" class="form-input">
          </div>

          <div class="score-row full-width">
            <label>Ghi chú / Nhận xét thêm</label>
            <textarea rows="3" [(ngModel)]="form.notes" placeholder="Nhập nhận xét hoặc giải trình nếu có..." class="form-textarea"></textarea>
          </div>
        </div>

        <!-- VIEW MODE: read-only structured summary -->
        <div class="view-summary" *ngIf="modalMode === 'view' && modalEval">
          <div class="view-block" *ngIf="modalEval.selfQuality != null">
            <div class="view-block-header">
              <span class="icon">person</span>
              <h4>Tự đánh giá (Member)</h4>
            </div>
            <div class="criteria-pills">
              <div class="criterion"><span>Chất lượng:</span> <strong>{{ modalEval.selfQuality }}</strong></div>
              <div class="criterion"><span>Trách nhiệm:</span> <strong>{{ modalEval.selfResponsibility }}</strong></div>
              <div class="criterion"><span>Đồng đội:</span> <strong>{{ modalEval.selfTeamwork }}</strong></div>
              <div class="criterion"><span>Chủ động:</span> <strong>{{ modalEval.selfInitiative }}</strong></div>
              <div class="criterion"><span>Kỷ luật:</span> <strong>{{ modalEval.selfDiscipline }}</strong></div>
            </div>
            <p *ngIf="modalEval.selfNotes" class="note-quote">"{{ modalEval.selfNotes }}"</p>
          </div>

          <div class="view-block" *ngIf="modalEval.leadQuality != null">
            <div class="view-block-header">
              <span class="icon">shield_person</span>
              <h4>Lead đánh giá <span class="score-tag">{{ modalEval.leadScore | number:'1.0-1' }} pts</span></h4>
            </div>
            <div class="criteria-pills">
              <div class="criterion"><span>Chất lượng:</span> <strong>{{ modalEval.leadQuality }}</strong></div>
              <div class="criterion"><span>Trách nhiệm:</span> <strong>{{ modalEval.leadResponsibility }}</strong></div>
              <div class="criterion"><span>Đồng đội:</span> <strong>{{ modalEval.leadTeamwork }}</strong></div>
              <div class="criterion"><span>Kỷ luật:</span> <strong>{{ modalEval.leadDiscipline }}</strong></div>
            </div>
            <p *ngIf="modalEval.leadNotes" class="note-quote">"{{ modalEval.leadNotes }}"</p>
          </div>

          <div class="view-block" *ngIf="modalEval.managerQuality != null">
            <div class="view-block-header">
              <span class="icon">verified</span>
              <h4>Manager đánh giá <span class="score-tag">{{ modalEval.managerScore | number:'1.0-1' }} pts</span></h4>
            </div>
            <div class="criteria-pills">
              <div class="criterion"><span>Chất lượng:</span> <strong>{{ modalEval.managerQuality }}</strong></div>
              <div class="criterion"><span>Trách nhiệm:</span> <strong>{{ modalEval.managerResponsibility }}</strong></div>
              <div class="criterion"><span>Chủ động:</span> <strong>{{ modalEval.managerInitiative }}</strong></div>
              <div class="criterion"><span>Kỷ luật:</span> <strong>{{ modalEval.managerDiscipline }}</strong></div>
            </div>
            <p *ngIf="modalEval.managerNotes" class="note-quote">"{{ modalEval.managerNotes }}"</p>
          </div>

          <div class="view-block final-card" *ngIf="modalEval.kpiFinal != null">
            <span class="final-title">KPI TỔNG KẾT KỲ</span>
            <div class="final-score">{{ modalEval.kpiFinal | number:'1.0-1' }} <span class="max">/ 100</span></div>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn btn-secondary" (click)="closeModal()">Đóng</button>
          <button
            *ngIf="modalMode !== 'view'"
            class="btn btn-primary"
            (click)="submitModal()"
            [disabled]="submitting"
          >
            <span class="icon spinner" *ngIf="submitting">sync</span>
            <span>{{ submitting ? 'Đang gửi...' : submitLabel() }}</span>
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .evaluations-page {
      max-width: 1300px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;

      h1 {
        font-size: 26px;
        font-weight: 800;
        margin: 0 0 4px;
        background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }

      .subtitle {
        margin: 0;
        color: var(--color-text-muted);
        font-size: 13.5px;
      }
    }

    .month-selector-wrapper {
      display: flex;
      align-items: center;
      gap: 8px;
      background: var(--color-surface);
      border: 1.5px solid var(--color-border);
      border-radius: var(--radius-sm);
      padding: 6px 12px;
      box-shadow: var(--shadow-sm);
      transition: all 0.2s ease;

      &:focus-within {
        border-color: var(--color-primary);
        box-shadow: 0 0 0 3px var(--color-primary-glow);
      }

      .icon {
        color: var(--color-primary);
        font-size: 20px;
      }

      .month-input {
        border: none;
        padding: 4px;
        background: transparent;
        font-weight: 700;
        color: var(--color-text);
        cursor: pointer;

        &:focus {
          box-shadow: none;
        }
      }
    }

    .eval-section {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .section-title-row {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .title-with-icon {
        display: flex;
        align-items: center;
        gap: 10px;

        .section-icon {
          color: var(--color-primary);
          background: var(--color-primary-subtle);
          padding: 6px;
          border-radius: 8px;
          font-size: 20px;
        }

        h2 {
          margin: 0;
          font-size: 18px;
          font-weight: 700;
          color: var(--color-text);
        }
      }
    }

    .table-responsive {
      overflow-x: auto;
    }

    .user-cell {
      display: flex;
      align-items: center;
      gap: 10px;
      font-weight: 600;
    }

    .avatar-mini {
      width: 30px;
      height: 30px;
      border-radius: 8px;
      background: var(--gradient-primary);
      color: white;
      font-size: 11px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .group-pill {
      background: #f1f5f9;
      color: #475569;
      padding: 3px 8px;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 600;
    }

    .kpi-score {
      font-weight: 800;
      color: var(--color-primary);
      font-size: 14px;
    }

    .font-bold {
      font-weight: 800;
      color: #0369a1;
    }

    .text-muted {
      color: var(--color-text-light);
    }

    .text-right {
      text-align: right;
    }

    .loading-state, .empty-state {
      text-align: center;
      padding: 36px 20px;
      color: var(--color-text-muted);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;

      .empty-icon {
        font-size: 40px;
        color: #cbd5e1;
      }

      .spinner {
        animation: spin 1s linear infinite;
        font-size: 28px;
        color: var(--color-primary);
      }
    }

    @keyframes spin {
      100% { transform: rotate(360deg); }
    }

    /* MODAL STYLING */
    .modal-overlay {
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 42, 0.45);
      backdrop-filter: blur(8px);
      -webkit-backdrop-filter: blur(8px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 16px;
    }

    .modal-content {
      background: #ffffff;
      border-radius: var(--radius-lg);
      padding: 28px;
      width: 620px;
      max-width: 95vw;
      max-height: 90vh;
      overflow-y: auto;
      box-shadow: var(--shadow-lg);
      border: 1px solid rgba(14, 165, 233, 0.15);
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 20px;
      padding-bottom: 16px;
      border-bottom: 1px solid var(--color-border);

      .modal-title-wrap {
        display: flex;
        align-items: center;
        gap: 12px;

        .modal-icon-badge {
          width: 44px;
          height: 44px;
          border-radius: 12px;
          background: var(--gradient-primary);
          color: white;
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 4px 12px rgba(56, 189, 248, 0.35);

          .icon {
            font-size: 24px;
          }
        }

        h3 {
          margin: 0 0 4px;
          font-size: 19px;
          font-weight: 800;
          color: var(--color-text);
        }

        .modal-sub {
          margin: 0;
          font-size: 12.5px;
          color: var(--color-text-muted);
        }
      }

      .close-btn {
        background: #f8fafc;
        border: 1px solid var(--color-border);
        color: var(--color-text-muted);
        border-radius: 8px;
        padding: 6px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s;

        &:hover {
          background: var(--color-danger-bg);
          color: var(--color-danger);
          border-color: var(--color-danger-border);
        }
      }
    }

    .score-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
      margin-bottom: 20px;

      .score-row {
        display: flex;
        flex-direction: column;
        gap: 6px;

        &.full-width {
          grid-column: 1 / -1;
        }

        .score-label-row {
          display: flex;
          justify-content: space-between;
          align-items: center;

          label {
            font-size: 13px;
            font-weight: 700;
            color: var(--color-text);
          }

          .score-pill {
            font-size: 11.5px;
            font-weight: 800;
            padding: 2px 8px;
            background: var(--color-primary-subtle);
            color: var(--color-primary);
            border-radius: 999px;
          }
        }

        .form-input {
          height: 42px;
          border-radius: 10px;
          font-weight: 600;
        }

        .form-textarea {
          border-radius: 10px;
          font-size: 13.5px;
          resize: vertical;
        }
      }
    }

    .view-summary {
      display: flex;
      flex-direction: column;
      gap: 16px;
      margin-bottom: 20px;

      .view-block {
        background: #f8fafc;
        border: 1px solid var(--color-border);
        border-radius: var(--radius-sm);
        padding: 16px;

        .view-block-header {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 12px;
          color: var(--color-primary);

          h4 {
            margin: 0;
            font-size: 15px;
            font-weight: 700;
            color: var(--color-text);
            display: flex;
            align-items: center;
            gap: 8px;
          }

          .score-tag {
            font-size: 12px;
            padding: 2px 8px;
            background: var(--color-primary-subtle);
            color: var(--color-primary);
            border-radius: 999px;
            font-weight: 800;
          }
        }

        .criteria-pills {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;

          .criterion {
            background: #ffffff;
            border: 1px solid var(--color-border);
            padding: 4px 10px;
            border-radius: 8px;
            font-size: 12.5px;
            color: var(--color-text-muted);

            strong {
              color: var(--color-text);
              font-weight: 700;
            }
          }
        }

        .note-quote {
          margin: 12px 0 0;
          font-style: italic;
          font-size: 13px;
          color: var(--color-text-muted);
          background: #ffffff;
          padding: 8px 12px;
          border-radius: 6px;
          border-left: 3px solid var(--color-primary);
        }
      }

      .final-card {
        background: var(--gradient-primary-soft);
        border: 1.5px solid var(--color-primary-border);
        text-align: center;
        padding: 20px;

        .final-title {
          font-size: 12px;
          font-weight: 800;
          letter-spacing: 0.06em;
          color: var(--color-primary);
        }

        .final-score {
          font-size: 32px;
          font-weight: 800;
          color: #0369a1;
          margin-top: 4px;

          .max {
            font-size: 16px;
            color: var(--color-text-muted);
            font-weight: 600;
          }
        }
      }
    }

    .modal-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      padding-top: 16px;
      border-top: 1px solid var(--color-border);
    }

    @media (max-width: 600px) {
      .score-grid { grid-template-columns: 1fr; }
    }
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

  initials(name: string | undefined): string {
    if (!name) return '?';
    return name.split(' ').map(p => p[0]).slice(-2).join('').toUpperCase();
  }

  modalIcon(): string {
    const icons: Record<ReviewMode, string> = {
      self: 'person_edit', lead: 'grade', manager: 'verified', view: 'rate_review'
    };
    return icons[this.modalMode] || 'rate_review';
  }

  modalTitle(): string {
    const labels: Record<ReviewMode, string> = {
      self: 'Tự đánh giá (Member)',
      lead: 'Lead chấm điểm thành viên',
      manager: 'Manager chốt điểm KPI',
      view: 'Chi tiết bản đánh giá'
    };
    return labels[this.modalMode];
  }

  submitLabel(): string {
    const labels: Record<ReviewMode, string> = {
      self: 'Gửi tự đánh giá', lead: 'Gửi kết quả chấm', manager: 'Chốt điểm KPI', view: ''
    };
    return labels[this.modalMode];
  }

  openModal(evaluation: Evaluation, mode: ReviewMode) {
    this.modalEval = evaluation;
    this.modalMode = mode;
    this.form = {
      quality: evaluation.selfQuality ?? 5,
      responsibility: evaluation.selfResponsibility ?? 5,
      teamwork: evaluation.selfTeamwork ?? 5,
      initiative: evaluation.selfInitiative ?? 5,
      discipline: evaluation.selfDiscipline ?? 5,
      notes: evaluation.selfNotes ?? ''
    };
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
