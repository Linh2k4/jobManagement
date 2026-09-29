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
    <div class="page">
      <h1>Yêu cầu gia hạn deadline</h1>
      <p class="subtitle">Chờ duyệt: {{ requests.length }}</p>

      <div class="empty" *ngIf="!loading && requests.length === 0">Không có yêu cầu nào đang chờ duyệt.</div>
      <div class="loading" *ngIf="loading">Đang tải...</div>

      <div class="request-card" *ngFor="let r of requests">
        <div class="request-header">
          <a (click)="router.navigate(['/tasks', r.taskId])">{{ r.taskTitle }}</a>
          <span class="ext-number">Lần gia hạn #{{ r.extensionNumber }}</span>
        </div>
        <div class="request-body">
          <span>{{ r.requestedBy.fullName }} ({{ r.requestedBy.role }})</span>
          <span>{{ r.currentDeadline | date:'mediumDate' }} <span class="icon">arrow_forward</span> <strong>{{ r.requestedDeadline | date:'mediumDate' }}</strong></span>
        </div>
        <p class="reason">"{{ r.reason }}"</p>
        <div class="request-meta">Gửi lúc {{ r.createdAt | date:'short' }} · Hết hạn duyệt lúc {{ r.expiresAt | date:'short' }}</div>
        <div class="actions">
          <input type="text" [(ngModel)]="noteInputs[r.id]" placeholder="Ghi chú (bắt buộc nếu từ chối)">
          <button class="btn-approve" (click)="approve(r)">Duyệt</button>
          <button class="btn-reject" (click)="reject(r)">Từ chối</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page { padding: 20px; max-width: 800px; margin: 0 auto; }
    .subtitle { color: #777; margin-top: -10px; }
    .loading, .empty { text-align: center; color: #999; padding: 40px; }
    .request-card { background: white; border: 1px solid #eee; border-radius: 16px; padding: 16px; margin-bottom: 14px; }
    .request-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .request-header a { font-weight: bold; color: #1d4ed8; cursor: pointer; }
    .ext-number { font-size: 11px; background: #eee; padding: 2px 8px; border-radius: 16px; color: #666; }
    .request-body { display: flex; justify-content: space-between; font-size: 13px; color: #555; margin-bottom: 8px; }
    .reason { font-style: italic; color: #555; margin: 8px 0; }
    .request-meta { font-size: 12px; color: #999; margin-bottom: 12px; }
    .actions { display: flex; gap: 8px; }
    .actions input { flex: 1; padding: 6px; border: 1px solid #ddd; border-radius: 10px; }
    .actions button { padding: 6px 14px; border: none; border-radius: 10px; cursor: pointer; color: white; }
    .btn-approve { background: #2e7d32; }
    .btn-reject { background: #c62828; }
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
