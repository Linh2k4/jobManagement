import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Store } from '@ngrx/store';
import {
  selectAllNotifications,
  selectNotificationLoading,
  selectUnreadCount,
  selectNotificationStats
} from '../../../../store/notification/notification.selectors';
import * as NotificationActions from '../../../../store/notification/notification.actions';
import { NotificationItemComponent } from '../../components/notification-item/notification-item.component';

@Component({
  selector: 'app-notification-center',
  standalone: true,
  imports: [CommonModule, FormsModule, NotificationItemComponent],
  template: `
    <div class="notification-center-container">
      <div class="header">
        <h2>Notifications</h2>
        <div class="header-actions">
          <button (click)="markAllAsRead()" class="btn-mark-all"><span class="icon">check</span> Mark All as Read</button>
          <button (click)="clearAll()" class="btn-clear"><span class="icon">delete</span> Clear All</button>
        </div>
      </div>

      <div class="stats" *ngIf="stats$ | async as stats">
        <span>Total: {{ stats.totalCount }}</span>
        <span class="unread">Unread: {{ (unreadCount$ | async) || 0 }}</span>
      </div>

      <div class="filters">
        <select [(ngModel)]="selectedType" (change)="onFilterChange()" class="filter-select">
          <option value="">All Types</option>
          <option value="TASK">Task</option>
          <option value="EVALUATION">Evaluation</option>
          <option value="SYSTEM">System</option>
        </select>
        <select [(ngModel)]="selectedStatus" (change)="onFilterChange()" class="filter-select">
          <option value="">All Status</option>
          <option value="unread">Unread Only</option>
          <option value="read">Read Only</option>
        </select>
      </div>

      <div *ngIf="isLoading$ | async" class="loading">Loading notifications...</div>

      <div class="notification-list" *ngIf="!(isLoading$ | async)">
        <app-notification-item
          *ngFor="let notif of notifications$ | async; let i = index"
          [notification]="notif"
          [index]="i"
          (markAsRead)="onMarkAsRead($event)"
          (delete)="onDelete($event)"
        ></app-notification-item>
      </div>

      <div *ngIf="!(notifications$ | async)?.length && !(isLoading$ | async)" class="no-data">
        No notifications
      </div>
    </div>
  `,
  styles: [`
    .notification-center-container { padding: 20px; max-width: 700px; margin: 0 auto; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    .header-actions { display: flex; gap: 10px; }
    .btn-mark-all, .btn-clear { padding: 8px 12px; border: 1px solid #ddd; background: white; border-radius: 10px; cursor: pointer; }
    .stats { display: flex; gap: 20px; margin-bottom: 15px; color: #666; }
    .unread { font-weight: bold; color: #2563eb; }
    .filters { display: flex; gap: 10px; margin-bottom: 20px; }
    .filter-select { padding: 8px; border: 1px solid #ddd; border-radius: 10px; }
    .notification-list { display: flex; flex-direction: column; gap: 10px; }
    .loading { text-align: center; color: #2563eb; padding: 40px; }
    .no-data { text-align: center; color: #999; padding: 40px; }
  `]
})
export class NotificationCenterComponent implements OnInit {
  notifications$ = this.store.select(selectAllNotifications);
  isLoading$ = this.store.select(selectNotificationLoading);
  unreadCount$ = this.store.select(selectUnreadCount);
  stats$ = this.store.select(selectNotificationStats);

  selectedType = '';
  selectedStatus = '';

  constructor(private store: Store) {}

  ngOnInit() {
    this.store.dispatch(NotificationActions.loadNotifications({
      page: 0,
      size: 20
    }));
  }

  onFilterChange() {
    this.store.dispatch(NotificationActions.filterNotifications({
      type: this.selectedType as any || undefined,
      status: this.selectedStatus as any || undefined
    }));
  }

  onMarkAsRead(id: string) {
    this.store.dispatch(NotificationActions.markAsRead({ id }));
  }

  onDelete(id: string) {
    this.store.dispatch(NotificationActions.deleteNotification({ id }));
  }

  markAllAsRead() {
    this.store.dispatch(NotificationActions.markAllAsRead());
  }

  clearAll() {
    if (confirm('Are you sure you want to clear all notifications?')) {
      this.store.dispatch(NotificationActions.clearAllNotifications());
    }
  }
}
