import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { KPI, KpiConfig, KpiResponse, KpiConfigResponse, KpiTeamResponse, KpiHistoryResponse, MemberKpiSummary, MemberKpiDetail } from '../../models';

@Injectable({
  providedIn: 'root'
})
export class KpiService {
  private apiUrl = '/api/v1/kpi';

  constructor(private http: HttpClient) {}

  /**
   * Get current user's KPI for current month
   * GET /kpi
   */
  getCurrentUserKPI(): Observable<KpiResponse> {
    return this.http.get<KpiResponse>(`${this.apiUrl}`);
  }

  /**
   * Get user's KPI by period
   * GET /kpi/{userId}?periodMonth=YYYY-MM
   */
  getUserKPIByPeriod(userId: number, periodMonth?: string): Observable<KpiResponse> {
    let params = new HttpParams();
    if (periodMonth) {
      params = params.set('periodMonth', periodMonth);
    }
    return this.http.get<KpiResponse>(`${this.apiUrl}/${userId}`, { params });
  }

  /**
   * Get user's KPI history for last 12 months
   * GET /kpi/{userId}/history
   */
  getKPIHistory(userId: number): Observable<KpiHistoryResponse> {
    return this.http.get<KpiHistoryResponse>(`${this.apiUrl}/${userId}/history`);
  }

  /**
   * Get team KPI summary with pagination
   * GET /kpi/team/summary?page=0&size=10&sort=kpiFinal,desc
   */
  getTeamKPISummary(page: number = 0, size: number = 10, sort?: string): Observable<KpiTeamResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (sort) {
      params = params.set('sort', sort);
    }
    return this.http.get<KpiTeamResponse>(`${this.apiUrl}/team/summary`, { params });
  }

  /**
   * Members & KPI list (Scope.md §5.6). Lead: own team; Manager: whole company.
   * GET /kpi/members?periodMonth=YYYY-MM
   */
  getMembersKpi(periodMonth?: string): Observable<MemberKpiSummary[]> {
    let params = new HttpParams();
    if (periodMonth) params = params.set('periodMonth', periodMonth);
    return this.http.get<{ data: MemberKpiSummary[]; success: boolean }>(`${this.apiUrl}/members`, { params })
      .pipe(map(res => res.data));
  }

  /**
   * Full KPI breakdown popup for one member (Scope.md §5.6.4, §13).
   * GET /kpi/members/{userId}?periodMonth=YYYY-MM&groupId=...
   * groupId: optional, scopes to one group's KPI; null defaults to primary group
   */
  getMemberKpiDetail(userId: number, periodMonth?: string, groupId?: number): Observable<MemberKpiDetail> {
    let params = new HttpParams();
    if (periodMonth) params = params.set('periodMonth', periodMonth);
    if (groupId) params = params.set('groupId', groupId.toString());
    return this.http.get<{ data: MemberKpiDetail; success: boolean }>(`${this.apiUrl}/members/${userId}`, { params })
      .pipe(map(res => res.data));
  }

  /**
   * Get current KPI configuration
   * GET /kpi/admin/config (MANAGER only)
   */
  getKPIConfig(): Observable<KpiConfigResponse> {
    return this.http.get<KpiConfigResponse>(`${this.apiUrl}/admin/config`);
  }

  /**
   * Update KPI configuration
   * PUT /kpi/admin/config (MANAGER only)
   */
  updateKPIConfig(config: Partial<KpiConfig>): Observable<{ success: boolean; message: string }> {
    return this.http.put<{ success: boolean; message: string }>(
      `${this.apiUrl}/admin/config`,
      config
    );
  }

  /**
   * Invalidate KPI cache (MANAGER only)
   * POST /kpi/admin/invalidate-cache
   */
  invalidateKPICache(userId?: number): Observable<{ success: boolean }> {
    const params = userId ? new HttpParams().set('userId', userId.toString()) : undefined;
    return this.http.post<{ success: boolean }>(`${this.apiUrl}/admin/invalidate-cache`, {}, { params });
  }
}
