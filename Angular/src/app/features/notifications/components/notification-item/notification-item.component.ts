import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Notification } from '../../../../core/models';

@Component({
  selector: 'app-notification-item',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="notification-item" [ngClass]="{ 'unread': !notification.read }">
      <div class="notif-icon-box" [ngClass]="getIconColorClass()">
        <span class="icon">{{ getIcon() }}</span>
      </div>
      <div class="notification-content">
        <div class="notif-header">
          <h4 class="notif-title">{{ notification.title }}</h4>
          <span class="timestamp">{{ notification.createdAt | date: 'HH:mm dd/MM/yyyy' }}</span>
        </div>
        <p class="notif-message">{{ notification.message }}</p>
      </div>
      <div class="notification-actions">
        <button
          *ngIf="!notification.read"
          (click)="onMarkAsRead()"
          class="btn-action read"
          title="Đánh dấu đã đọc"
        >
          <span class="icon">done</span>
        </button>
        <button
          (click)="onDelete()"
          class="btn-action delete"
          title="Xoá thông báo"
        >
          <span class="icon">close</span>
        </button>
      </div>
    </div>
  `,
  styles: [`
    .notification-item {
      display: flex;
      align-items: flex-start;
      gap: 14px;
      padding: 16px;
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-sm);
      transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .notification-item:hover {
      transform: translateY(-2px);
      box-shadow: var(--shadow-md);
      border-color: var(--color-primary-border);
    }
    .notification-item.unread {
      background: #f0f9ff;
      border-color: #bae6fd;
      border-left-width: 4px;
      border-left-color: var(--color-primary);
    }

    .notif-icon-box {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .notif-icon-box.sky { background: #e0f2fe; color: #0284c7; }
    .notif-icon-box.amber { background: #fef3c7; color: #d97706; }
    .notif-icon-box.emerald { background: #dcfce7; color: #059669; }
    .notif-icon-box.purple { background: #f3e8ff; color: #7e22ce; }
    .notif-icon-box .icon { font-size: 20px; }

    .notification-content {
      flex: 1;
      min-width: 0;
    }
    .notif-header {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      gap: 8px;
      margin-bottom: 4px;
    }
    .notif-title {
      margin: 0;
      font-size: 14.5px;
      font-weight: 700;
      color: var(--color-text);
    }
    .timestamp {
      font-size: 11px;
      color: var(--color-text-muted);
      white-space: nowrap;
    }
    .notif-message {
      margin: 0;
      color: var(--color-text-muted);
      font-size: 13px;
      line-height: 1.45;
    }

    .notification-actions {
      display: flex;
      gap: 6px;
      flex-shrink: 0;
    }
    .btn-action {
      background: #f8fafc;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-xs);
      width: 30px;
      height: 30px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: var(--color-text-muted);
      transition: all 0.15s ease;
    }
    .btn-action.read:hover {
      background: var(--color-primary-light);
      border-color: var(--color-primary-border);
      color: var(--color-primary);
    }
    .btn-action.delete:hover {
      background: var(--color-danger-bg);
      border-color: var(--color-danger-border);
      color: var(--color-danger);
    }
    .btn-action .icon { font-size: 16px; }
  `]
})
export class NotificationItemComponent {
  @Input() notification!: Notification;
  @Input() index = 0;
  @Output() markAsRead = new EventEmitter<number>();
  @Output() delete = new EventEmitter<number>();

  getIcon(): string {
    const icons: { [key: string]: string } = {
      'TASK_ASSIGNED': 'assignment',
      'TASK_DUE': 'hourglass_top',
      'SYSTEM': 'settings',
      'REMINDER': 'notifications_active'
    };
    return icons[this.notification.type] || 'notifications';
  }

  getIconColorClass(): string {
    const map: { [key: string]: string } = {
      'TASK_ASSIGNED': 'sky',
      'TASK_DUE': 'amber',
      'SYSTEM': 'purple',
      'REMINDER': 'emerald'
    };
    return map[this.notification.type] || 'sky';
  }

  onMarkAsRead() {
    this.markAsRead.emit(this.notification.id);
  }

  onDelete() {
    this.delete.emit(this.notification.id);
  }
}
