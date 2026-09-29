import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Evaluation,
  EvaluationPeriod,
  EvaluationFilter,
  SubmitSelfReviewRequest,
  SubmitLeadReviewRequest,
  FinalizeEvaluationRequest,
  EvaluationResponse,
  EvaluationListResponse
} from '../../models';

@Injectable({
  providedIn: 'root'
})
export class EvaluationService {
  private apiUrl = '/api/v1/evaluations';

  constructor(private http: HttpClient) {}

  /**
   * Create or get existing evaluation for user/period
   * POST /evaluations?userId=1&periodMonth=2026-07
   */
  createOrGetEvaluation(userId: number, periodMonth: string): Observable<EvaluationResponse> {
    const params = new HttpParams()
      .set('userId', userId.toString())
      .set('periodMonth', periodMonth);
    return this.http.post<EvaluationResponse>(this.apiUrl, {}, { params });
  }

  /**
   * Get evaluation by ID
   * GET /evaluations/{id}
   */
  getEvaluation(id: number): Observable<EvaluationResponse> {
    return this.http.get<EvaluationResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get my evaluations
   * GET /evaluations/my?page=0&size=20
   */
  getMyEvaluations(page: number = 0, size: number = 20, filter?: EvaluationFilter): Observable<EvaluationListResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filter?.periodMonth) params = params.set('periodMonth', filter.periodMonth);
    if (filter?.status) params = params.set('status', filter.status);

    return this.http.get<EvaluationListResponse>(`${this.apiUrl}/my`, { params });
  }

  /**
   * Get team evaluations (for leads)
   * GET /evaluations/team?page=0&size=20
   */
  getTeamEvaluations(page: number = 0, size: number = 20, filter?: EvaluationFilter): Observable<EvaluationListResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filter?.periodMonth) params = params.set('periodMonth', filter.periodMonth);
    if (filter?.status) params = params.set('status', filter.status);
    if (filter?.userId) params = params.set('userId', filter.userId.toString());

    return this.http.get<EvaluationListResponse>(`${this.apiUrl}/team`, { params });
  }

  /**
   * Get all evaluations (for managers)
   * GET /evaluations/all?page=0&size=20
   */
  getAllEvaluations(page: number = 0, size: number = 20, filter?: EvaluationFilter): Observable<EvaluationListResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filter?.periodMonth) params = params.set('periodMonth', filter.periodMonth);
    if (filter?.status) params = params.set('status', filter.status);
    if (filter?.userId) params = params.set('userId', filter.userId.toString());
    if (filter?.leadId) params = params.set('leadId', filter.leadId.toString());

    return this.http.get<EvaluationListResponse>(`${this.apiUrl}/all`, { params });
  }

  /**
   * Submit self-evaluation
   * PUT /evaluations/{id}/self-review
   */
  submitSelfReview(id: number, request: SubmitSelfReviewRequest): Observable<EvaluationResponse> {
    return this.http.put<EvaluationResponse>(`${this.apiUrl}/${id}/self-review`, request);
  }

  /**
   * Submit lead evaluation
   * PUT /evaluations/{id}/lead-review
   */
  submitLeadReview(id: number, request: SubmitLeadReviewRequest): Observable<EvaluationResponse> {
    return this.http.put<EvaluationResponse>(`${this.apiUrl}/${id}/lead-review`, request);
  }

  /**
   * Finalize evaluation (manager)
   * PUT /evaluations/{id}/finalize
   */
  finalizeEvaluation(id: number, request: FinalizeEvaluationRequest): Observable<EvaluationResponse> {
    return this.http.put<EvaluationResponse>(`${this.apiUrl}/${id}/finalize`, request);
  }

  /**
   * Unlock evaluation (manager override)
   * PATCH /evaluations/{id}/unlock
   */
  unlockEvaluation(id: number, reason: string): Observable<{ success: boolean }> {
    return this.http.patch<{ success: boolean }>(`${this.apiUrl}/${id}/unlock`, { reason });
  }

  /**
   * Get evaluation result summary (after finalized)
   * GET /evaluations/{id}/result
   */
  getEvaluationResult(id: number): Observable<{ data: any; success: boolean }> {
    return this.http.get<{ data: any; success: boolean }>(`${this.apiUrl}/${id}/result`);
  }

  // ============ EVALUATION PERIODS ============

  /**
   * Get evaluation periods
   * GET /evaluation-periods?page=0&size=10
   */
  getEvaluationPeriods(page: number = 0, size: number = 10): Observable<{ data: EvaluationPeriod[]; success: boolean }> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<{ data: EvaluationPeriod[]; success: boolean }>('/api/v1/evaluation-periods', { params });
  }

  /**
   * Create evaluation period
   * POST /evaluation-periods
   */
  createEvaluationPeriod(period: Partial<EvaluationPeriod>): Observable<{ data: EvaluationPeriod; success: boolean }> {
    return this.http.post<{ data: EvaluationPeriod; success: boolean }>('/api/v1/evaluation-periods', period);
  }

  /**
   * Get current evaluation period
   * GET /evaluation-periods/current
   */
  getCurrentEvaluationPeriod(): Observable<{ data: EvaluationPeriod; success: boolean }> {
    return this.http.get<{ data: EvaluationPeriod; success: boolean }>('/api/v1/evaluation-periods/current');
  }

  /**
   * Close evaluation period
   * PATCH /evaluation-periods/{id}/close
   */
  closeEvaluationPeriod(periodId: number): Observable<{ success: boolean }> {
    return this.http.patch<{ success: boolean }>(`/api/v1/evaluation-periods/${periodId}/close`, {});
  }

  /**
   * Get evaluation period progress
   * GET /evaluation-periods/{id}/progress
   */
  getEvaluationPeriodProgress(periodId: number): Observable<{ data: any; success: boolean }> {
    return this.http.get<{ data: any; success: boolean }>(`/api/v1/evaluation-periods/${periodId}/progress`);
  }
}
