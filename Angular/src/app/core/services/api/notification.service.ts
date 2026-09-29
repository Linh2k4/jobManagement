import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  Notification,
  NotificationPreferences,
  UpdateNotificationPreferencesRequest
} from '../../models';
import { ApiService } from '../api.service';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private apiUrl = '/api/v1/notifications';
  private api = inject(ApiService);
  private http = inject(HttpClient);

  /**
   * Get notifications for current user
   * GET /notifications
   */
  getNotifications(): Observable<Notification[]> {
    return this.api.get<Notification[]>('notifications').pipe(map(res => res.data));
  }

  /**
   * Get unread notification count
   * GET /notifications/unread-count
   */
  getUnreadCount(): Observable<number> {
    return this.api.get<{ count: number }>('notifications/unread-count').pipe(map(res => res.data.count));
  }

  /**
   * Mark notification as read
   * PATCH /notifications/{id}/read
   */
  markAsRead(id: number): Observable<Notification> {
    return this.api.patch<Notification>(`notifications/${id}/read`, {}).pipe(map(res => res.data));
  }

  /**
   * Mark all notifications as read
   * POST /notifications/mark-all-read
   */
  markAllAsRead(): Observable<number> {
    return this.api.post<{ updated: number }>('notifications/mark-all-read', {}).pipe(map(res => res.data.updated));
  }

  /**
   * Delete notification
   * DELETE /notifications/{id}
   */
  deleteNotification(id: number): Observable<void> {
    return this.api.delete<void>(`notifications/${id}`).pipe(map(() => void 0));
  }

  /**
   * Clear all notifications
   * DELETE /notifications
   */
  clearAllNotifications(): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(this.apiUrl);
  }

  // ============ NOTIFICATION PREFERENCES ============

  /**
   * Get notification preferences
   * GET /notifications/preferences
   */
  getPreferences(): Observable<{ data: NotificationPreferences; success: boolean }> {
    return this.http.get<{ data: NotificationPreferences; success: boolean }>(`${this.apiUrl}/preferences`);
  }

  /**
   * Update notification preferences
   * PUT /notifications/preferences
   */
  updatePreferences(request: UpdateNotificationPreferencesRequest): Observable<{ success: boolean }> {
    return this.http.put<{ success: boolean }>(`${this.apiUrl}/preferences`, request);
  }

  /**
   * Subscribe to notifications (WebSocket or SSE)
   * GET /notifications/subscribe (Server-Sent Events)
   */
  subscribeToNotifications(): Observable<Notification> {
    return new Observable(observer => {
      const eventSource = new EventSource(`${this.apiUrl}/subscribe`);

      eventSource.onmessage = (event) => {
        try {
          const notification = JSON.parse(event.data);
          observer.next(notification);
        } catch (e) {
          observer.error(e);
        }
      };

      eventSource.onerror = (error) => {
        observer.error(error);
        eventSource.close();
      };

      return () => eventSource.close();
    });
  }
}
