import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Notification } from '../../../../core/models';

@Component({
  selector: 'app-notification-item',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="notification-item" [ngClass]="{ 'unread': !notification.read }">
      <div class="notification-icon icon">{{ getIcon() }}</div>
      <div class="notification-content">
        <h4>{{ notification.title }}</h4>
        <p>{{ notification.message }}</p>
        <span class="timestamp">{{ notification.createdAt | date: 'short' }}</span>
      </div>
      <div class="notification-actions">
        <button
          *ngIf="!notification.read"
          (click)="onMarkAsRead()"
          class="btn-action mark-read"
          title="Mark as read"
        >
          <span class="icon">circle</span>
        </button>
        <button
          (click)="onDelete()"
          class="btn-action delete"
          title="Delete"
        >
          <span class="icon">close</span>
        </button>
      </div>
    </div>
  `,
  styles: [`
    .notification-item {
      display: flex;
      gap: 12px;
      padding: 12px;
      background: white;
      border: 1px solid #ddd;
      border-radius: 10px;
      transition: background-color 0.2s;
    }
    .notification-item.unread {
      background: #E3F2FD;
      border-color: #90CAF9;
    }
    .notification-icon {
      font-size: 20px;
      flex-shrink: 0;
    }
    .notification-content {
      flex: 1;
    }
    .notification-content h4 {
      margin: 0 0 4px 0;
      font-size: 14px;
    }
    .notification-content p {
      margin: 0 0 4px 0;
      color: #666;
      font-size: 13px;
    }
    .timestamp {
      font-size: 11px;
      color: #999;
    }
    .notification-actions {
      display: flex;
      gap: 8px;
      flex-shrink: 0;
    }
    .btn-action {
      background: none;
      border: none;
      cursor: pointer;
      font-size: 16px;
      padding: 4px;
      color: #999;
      transition: color 0.2s;
    }
    .btn-action:hover {
      color: #333;
    }
    .btn-action.delete:hover {
      color: #d32f2f;
    }
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
      'REMINDER': 'alarm'
    };
    return icons[this.notification.type] || 'push_pin';
  }

  onMarkAsRead() {
    this.markAsRead.emit(this.notification.id);
  }

  onDelete() {
    this.delete.emit(this.notification.id);
  }
}
