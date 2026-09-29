import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../../../core/services/api/notification.service';
import { NotificationStore } from '../../../../core/stores/notification.store';
import { NotificationItemComponent } from '../../components/notification-item/notification-item.component';
import { Notification } from '../../../../core/models';

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [CommonModule, NotificationItemComponent],
  templateUrl: './notification-list.component.html',
  styleUrls: ['./notification-list.component.scss']
})
export class NotificationListComponent implements OnInit {
  private notificationService = inject(NotificationService);
  private notificationStore = inject(NotificationStore);

  readonly notifications = signal<Notification[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);
    this.notificationService.getNotifications().subscribe({
      next: (list) => {
        this.notifications.set(list);
        this.notificationStore.setUnreadCount(list.filter(n => !n.read).length);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Không tải được danh sách thông báo.');
        this.loading.set(false);
      }
    });
  }

  onMarkAsRead(id: number) {
    this.notificationService.markAsRead(id).subscribe(() => {
      this.notifications.update(list => list.map(n => n.id === id ? { ...n, read: true } : n));
      this.notificationStore.decrementUnread();
    });
  }

  onMarkAllAsRead() {
    this.notificationService.markAllAsRead().subscribe(() => {
      this.notifications.update(list => list.map(n => ({ ...n, read: true })));
      this.notificationStore.clearUnread();
    });
  }

  onDelete(id: number) {
    this.notificationService.deleteNotification(id).subscribe(() => {
      this.notifications.update(list => list.filter(n => n.id !== id));
    });
  }
}
