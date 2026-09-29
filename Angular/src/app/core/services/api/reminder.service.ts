import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Reminder,
  ReminderFilter,
  CreateReminderRequest,
  UpdateReminderRequest,
  DismissReminderRequest,
  ReminderResponse,
  ReminderListResponse
} from '../../models';

@Injectable({
  providedIn: 'root'
})
export class ReminderService {
  private apiUrl = '/api/v1/reminders';

  constructor(private http: HttpClient) {}

  /**
   * Get all reminders for current user
   * GET /reminders?page=0&size=20
   */
  getReminders(page: number = 0, size: number = 20, filter?: ReminderFilter): Observable<ReminderListResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filter?.taskId) params = params.set('taskId', filter.taskId.toString());
    if (filter?.isDismissed !== undefined) params = params.set('isDismissed', filter.isDismissed.toString());
    if (filter?.isRecurring !== undefined) params = params.set('isRecurring', filter.isRecurring.toString());
    if (filter?.remindAtBefore) params = params.set('remindAtBefore', filter.remindAtBefore);
    if (filter?.remindAtAfter) params = params.set('remindAtAfter', filter.remindAtAfter);

    return this.http.get<ReminderListResponse>(this.apiUrl, { params });
  }

  /**
   * Get upcoming reminders (not dismissed, remindAt soon)
   * GET /reminders/upcoming?hoursAhead=24
   */
  getUpcomingReminders(hoursAhead: number = 24): Observable<ReminderListResponse> {
    const params = new HttpParams().set('hoursAhead', hoursAhead.toString());
    return this.http.get<ReminderListResponse>(`${this.apiUrl}/upcoming`, { params });
  }

  /**
   * Create reminder
   * POST /reminders
   */
  createReminder(request: CreateReminderRequest): Observable<ReminderResponse> {
    return this.http.post<ReminderResponse>(this.apiUrl, request);
  }

  /**
   * Get reminder by ID
   * GET /reminders/{id}
   */
  getReminder(id: string): Observable<ReminderResponse> {
    return this.http.get<ReminderResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * Update reminder
   * PUT /reminders/{id}
   */
  updateReminder(id: string, request: UpdateReminderRequest): Observable<ReminderResponse> {
    return this.http.put<ReminderResponse>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Dismiss reminder
   * PATCH /reminders/{id}/dismiss
   */
  dismissReminder(id: string): Observable<{ success: boolean }> {
    return this.http.patch<{ success: boolean }>(`${this.apiUrl}/${id}/dismiss`, { isDismissed: true });
  }

  /**
   * Snooze reminder (delay next reminder)
   * PATCH /reminders/{id}/snooze
   */
  snoozeReminder(id: string, minutesToSnooze: number): Observable<ReminderResponse> {
    return this.http.patch<ReminderResponse>(`${this.apiUrl}/${id}/snooze`, { minutesToSnooze });
  }

  /**
   * Delete reminder
   * DELETE /reminders/{id}
   */
  deleteReminder(id: string): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/${id}`);
  }

  /**
   * Clear all dismissed reminders
   * DELETE /reminders/clear-dismissed
   */
  clearDismissedReminders(): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/clear-dismissed`);
  }

  /**
   * Get reminder statistics
   * GET /reminders/stats
   */
  getReminderStats(): Observable<{ data: any; success: boolean }> {
    return this.http.get<{ data: any; success: boolean }>(`${this.apiUrl}/stats`);
  }
}
