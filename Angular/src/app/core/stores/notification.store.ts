import { Injectable } from '@angular/core';
import { signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class NotificationStore {
  readonly unreadCount = signal<number>(0);

  incrementUnread() {
    this.unreadCount.update(count => count + 1);
  }

  decrementUnread() {
    this.unreadCount.update(count => Math.max(0, count - 1));
  }

  setUnreadCount(count: number) {
    this.unreadCount.set(count);
  }

  clearUnread() {
    this.unreadCount.set(0);
  }
}
