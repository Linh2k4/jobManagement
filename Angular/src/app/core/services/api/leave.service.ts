import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { LeaveRequest, LeaveType, HandoverSuggestion, HandoverAction } from '../../models';

interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

/** Leave requests and task handover (Scope.md §14). */
@Injectable({
  providedIn: 'root'
})
export class LeaveService {
  private apiUrl = '/api/v1/leave-requests';

  constructor(private http: HttpClient) {}

  /** POST /leave-requests */
  createLeaveRequest(leaveType: LeaveType, startDate: string, endDate: string, reason: string): Observable<LeaveRequest> {
    return this.http.post<ApiEnvelope<LeaveRequest>>(this.apiUrl, { leaveType, startDate, endDate, reason })
      .pipe(map(res => res.data));
  }

  /** PATCH /leave-requests/{id}/status — Lead/Manager only */
  reviewLeaveRequest(id: number, approve: boolean, note?: string): Observable<LeaveRequest> {
    return this.http.patch<ApiEnvelope<LeaveRequest>>(`${this.apiUrl}/${id}/status`, {
      status: approve ? 'APPROVED' : 'REJECTED', note
    }).pipe(map(res => res.data));
  }

  /** GET /leave-requests/{id} */
  getLeaveRequest(id: number): Observable<LeaveRequest> {
    return this.http.get<ApiEnvelope<LeaveRequest>>(`${this.apiUrl}/${id}`).pipe(map(res => res.data));
  }

  /** GET /leave-requests/{id}/handover-suggestions */
  getHandoverSuggestions(leaveRequestId: number): Observable<HandoverSuggestion[]> {
    return this.http.get<ApiEnvelope<HandoverSuggestion[]>>(`${this.apiUrl}/${leaveRequestId}/handover-suggestions`)
      .pipe(map(res => res.data));
  }

  /** POST /leave-requests/{id}/handover/{suggestionId}/confirm */
  confirmHandover(leaveRequestId: number, suggestionId: number, action: HandoverAction, newAssigneeId?: number): Observable<HandoverSuggestion> {
    return this.http.post<ApiEnvelope<HandoverSuggestion>>(
      `${this.apiUrl}/${leaveRequestId}/handover/${suggestionId}/confirm`, { action, newAssigneeId }
    ).pipe(map(res => res.data));
  }

  /** GET /leave-requests/pending — Lead: own team; Manager: everyone */
  getPendingLeaveRequests(): Observable<LeaveRequest[]> {
    return this.http.get<ApiEnvelope<LeaveRequest[]>>(`${this.apiUrl}/pending`)
      .pipe(map(res => res.data));
  }

  /** GET /members/{id}/leave-requests?year= */
  getMemberLeaveRequests(memberId: number, year?: number): Observable<LeaveRequest[]> {
    let params = new HttpParams();
    if (year) params = params.set('year', year.toString());
    return this.http.get<ApiEnvelope<LeaveRequest[]>>(`/api/v1/members/${memberId}/leave-requests`, { params })
      .pipe(map(res => res.data));
  }
}
