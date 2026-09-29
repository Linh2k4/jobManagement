import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TaskService } from '../../../../core/services/api/task.service';
import { DeadlineExtension } from '../../../../core/models';

@Component({
  selector: 'app-deadline-extensions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="extensions-page animate-fade-in">
      <!-- PAGE HEADER -->
      <div class="page-header">
        <div>
          <div class="title-with-badge">
            <h1 class="page-title">Yêu cầu gia hạn deadline</h1>
            <span class="count-badge" *ngIf="requests.length > 0">{{ requests.length }} chờ duyệt</span>
          </div>
          <p class="page-subtitle">Xem xét và phê duyệt các yêu cầu thay đổi hạn hoàn thành từ thành viên</p>
        </div>
      </div>

      <!-- LOADING -->
      <div class="state-card" *ngIf="loading">
        <div class="spinner"></div>
        <p>Đang tải danh sách yêu cầu...</p>
      </div>

      <!-- EMPTY STATE -->
      <div class="state-card" *ngIf="!loading && requests.length === 0">
        <div class="state-icon-box success">
          <span class="icon">task_alt</span>
        </div>
        <h3>Tất cả đã được xử lý!</h3>
        <p class="text-muted">Hiện tại không có yêu cầu xin gia hạn nào đang chờ bạn duyệt.</p>
      </div>

      <!-- REQUESTS LIST -->
      <div class="requests-grid" *ngIf="!loading && requests.length > 0">
        <div class="request-card" *ngFor="let r of requests">
          <div class="request-card-header">
            <div class="task-info">
              <a (click)="router.navigate(['/tasks', r.taskId])" class="task-title-link">
                {{ r.taskTitle }}
              </a>
              <span class="ext-badge">Lần gia hạn #{{ r.extensionNumber }}</span>
            </div>
          </div>

          <div class="request-body">
            <div class="user-row">
              <div class="user-avatar">{{ initials(r.requestedBy.fullName) }}</div>
              <div class="user-meta">
                <strong>{{ r.requestedBy.fullName }}</strong>
                <span class="user-role">{{ r.requestedBy.role }}</span>
              </div>
            </div>

            <div class="deadline-diff-card">
              <div class="deadline-block current">
                <span class="deadline-label">Hạn hiện tại</span>
                <span class="deadline-val">{{ r.currentDeadline | date:'dd/MM/yyyy' }}</span>
              </div>
              <span class="icon diff-arrow">arrow_forward</span>
              <div class="deadline-block requested">
                <span class="deadline-label">Xin gia hạn đến</span>
                <span class="deadline-val">{{ r.requestedDeadline | date:'dd/MM/yyyy' }}</span>
              </div>
            </div>

            <div class="reason-box">
              <span class="reason-label">Lý do xin gia hạn:</span>
              <p class="reason-text">"{{ r.reason }}"</p>
            </div>

            <div class="request-meta-info">
              <span><span class="icon">schedule</span> Gửi lúc {{ r.createdAt | date:'HH:mm dd/MM/yyyy' }}</span>
              <span *ngIf="r.expiresAt"><span class="icon">timer</span> Hết hạn duyệt {{ r.expiresAt | date:'HH:mm dd/MM/yyyy' }}</span>
            </div>
          </div>

          <div class="request-footer">
            <input
              type="text"
              [(ngModel)]="noteInputs[r.id]"
              placeholder="Ghi chú phản hồi (bắt buộc nếu từ chối)..."
              class="note-input"
            >
            <div class="action-buttons">
              <button class="btn btn-approve" (click)="approve(r)">
                <span class="icon">check</span> Duyệt
              </button>
              <button class="btn btn-reject" (click)="reject(r)">
                <span class="icon">close</span> Từ chối
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .extensions-page {
      max-width: 900px;
      margin: 0 auto;
    }

    .page-header {
      margin-bottom: 24px;
    }
    .title-with-badge {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .page-title {
      font-size: 26px;
      font-weight: 800;
      color: var(--color-text);
      margin: 0;
      letter-spacing: -0.02em;
    }
    .count-badge {
      background: #e0f2fe;
      color: #0284c7;
      border: 1px solid #bae6fd;
      padding: 4px 12px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 800;
    }
    .page-subtitle {
      color: var(--color-text-muted);
      font-size: 14px;
      margin: 4px 0 0 0;
    }

    /* STATE CARD */
    .state-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 50px 24px;
      text-align: center;
      box-shadow: var(--shadow-sm);
    }
    .state-icon-box {
      width: 60px;
      height: 60px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 16px auto;
    }
    .state-icon-box.success { background: #ecfdf5; color: #059669; }
    .state-icon-box .icon { font-size: 32px; }
    .state-card h3 { font-size: 18px; font-weight: 800; margin: 0 0 6px 0; }
    .text-muted { color: var(--color-text-muted); margin: 0; }

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

    /* REQUEST CARDS */
    .requests-grid {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .request-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-sm);
      overflow: hidden;
      transition: all 0.2s ease;
    }
    .request-card:hover {
      box-shadow: var(--shadow-md);
      border-color: var(--color-primary-border);
    }
    .request-card-header {
      padding: 16px 20px;
      border-bottom: 1px solid var(--color-border);
      background: #f8fbff;
    }
    .task-info {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }
    .task-title-link {
      font-size: 16px;
      font-weight: 800;
      color: var(--color-primary);
      cursor: pointer;
      text-decoration: none;
    }
    .task-title-link:hover {
      text-decoration: underline;
    }
    .ext-badge {
      background: #e2e8f0;
      color: #475569;
      padding: 3px 10px;
      border-radius: 999px;
      font-size: 11px;
      font-weight: 700;
    }

    .request-body {
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 14px;
    }
    .user-row {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .user-avatar {
      width: 34px;
      height: 34px;
      border-radius: 10px;
      background: var(--gradient-primary);
      color: white;
      font-weight: 800;
      font-size: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .user-meta {
      display: flex;
      flex-direction: column;
    }
    .user-meta strong { font-size: 14px; color: var(--color-text); }
    .user-role { font-size: 11px; color: var(--color-text-muted); text-transform: uppercase; font-weight: 600; }

    .deadline-diff-card {
      display: flex;
      align-items: center;
      gap: 16px;
      background: #f8fafc;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      padding: 12px 16px;
    }
    .deadline-block {
      display: flex;
      flex-direction: column;
    }
    .deadline-label { font-size: 11px; color: var(--color-text-muted); font-weight: 600; }
    .deadline-val { font-size: 15px; font-weight: 800; color: var(--color-text); margin-top: 2px; }
    .deadline-block.requested .deadline-val { color: var(--color-primary); }
    .diff-arrow { color: #94a3b8; font-size: 20px; }

    .reason-box {
      background: #fffbeb;
      border-left: 3.5px solid #f59e0b;
      border-radius: 0 var(--radius-xs) var(--radius-xs) 0;
      padding: 10px 14px;
    }
    .reason-label { font-size: 11.5px; font-weight: 700; color: #92400e; display: block; margin-bottom: 3px; }
    .reason-text { margin: 0; font-size: 13.5px; color: #78350f; font-style: italic; }

    .request-meta-info {
      display: flex;
      gap: 18px;
      font-size: 12px;
      color: var(--color-text-muted);
      border-top: 1px dashed var(--color-border);
      padding-top: 10px;
    }
    .request-meta-info span { display: flex; align-items: center; gap: 4px; }
    .request-meta-info .icon { font-size: 15px; }

    .request-footer {
      padding: 14px 20px;
      background: #f8fafc;
      border-top: 1px solid var(--color-border);
      display: flex;
      gap: 10px;
      align-items: center;
      flex-wrap: wrap;
    }
    .note-input {
      flex: 1;
      min-width: 240px;
      background: #ffffff !important;
    }
    .action-buttons {
      display: flex;
      gap: 8px;
    }
    .btn-approve {
      background: #059669;
      color: white;
      box-shadow: 0 2px 8px rgba(5, 150, 105, 0.25);
    }
    .btn-approve:hover {
      background: #047857;
    }
    .btn-reject {
      background: #fff1f2;
      color: #e11d48;
      border: 1px solid #fecdd3;
    }
    .btn-reject:hover {
      background: #e11d48;
      color: white;
    }
  `]
})
export class DeadlineExtensionsComponent implements OnInit {
  router = inject(Router);
  private taskService = inject(TaskService);

  requests: DeadlineExtension[] = [];
  loading = false;
  noteInputs: Record<number, string> = {};

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.taskService.getPendingDeadlineExtensions().subscribe({
      next: requests => { this.requests = requests; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  initials(name: string): string {
    return (name || '').split(' ').map(p => p[0]).slice(-2).join('').toUpperCase();
  }

  approve(r: DeadlineExtension) {
    this.taskService.approveDeadlineExtension(r.id, this.noteInputs[r.id]).subscribe({
      next: () => this.load(),
      error: (err) => alert(err.error?.message || 'Không thể duyệt yêu cầu')
    });
  }

  reject(r: DeadlineExtension) {
    const note = this.noteInputs[r.id];
    if (!note || !note.trim()) {
      alert('Cần nhập lý do từ chối.');
      return;
    }
    this.taskService.rejectDeadlineExtension(r.id, note).subscribe({
      next: () => this.load(),
      error: (err) => alert(err.error?.message || 'Không thể từ chối yêu cầu')
    });
  }
}
