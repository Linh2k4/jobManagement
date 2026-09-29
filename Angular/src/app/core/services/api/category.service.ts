import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  Category,
  ExtraField,
  CategoryFilter,
  CreateCategoryRequest,
  UpdateCategoryRequest,
  AddExtraFieldRequest,
  UpdateExtraFieldRequest,
  CategoryResponse,
  CategoryListResponse
} from '../../models';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private apiUrl = '/api/v1/categories';

  constructor(private http: HttpClient) {}

  /**
   * Get all visible categories (system + user's custom)
   * GET /categories
   */
  getCategories(page: number = 0, size: number = 20): Observable<CategoryListResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<CategoryListResponse>(this.apiUrl, { params });
  }

  /**
   * Get category by ID
   * GET /categories/{id}
   */
  getCategory(id: number): Observable<CategoryResponse> {
    return this.http.get<CategoryResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * Search categories
   * GET /categories/search
   */
  searchCategories(filter: CategoryFilter & { page?: number; size?: number }): Observable<CategoryListResponse> {
    let params = new HttpParams();

    if (filter.tier) params = params.set('tier', filter.tier);
    if (filter.status) params = params.set('status', filter.status);
    if (filter.ownerId) params = params.set('ownerId', filter.ownerId.toString());
    if (filter.searchText) params = params.set('searchText', filter.searchText);
    if (filter.allowedTaskType) params = params.set('allowedTaskType', filter.allowedTaskType);

    params = params
      .set('page', (filter.page ?? 0).toString())
      .set('size', (filter.size ?? 20).toString());

    return this.http.get<CategoryListResponse>(`${this.apiUrl}/search`, { params });
  }

  /**
   * Create new category
   * POST /categories
   */
  createCategory(request: CreateCategoryRequest): Observable<CategoryResponse> {
    return this.http.post<CategoryResponse>(this.apiUrl, request);
  }

  /**
   * Update category
   * PUT /categories/{id}
   */
  updateCategory(id: number, request: UpdateCategoryRequest): Observable<CategoryResponse> {
    return this.http.put<CategoryResponse>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Deactivate category (soft delete). Backend only supports deactivation —
   * there is no reactivate/arbitrary-status endpoint, so `status` is accepted
   * here for call-site convenience but ignored; INACTIVE is the only outcome.
   * PATCH /categories/{id}/status
   */
  updateCategoryStatus(id: number, status: 'ACTIVE' | 'INACTIVE'): Observable<{ success: boolean }> {
    return this.http.patch<{ success: boolean }>(`${this.apiUrl}/${id}/status`, {});
  }

  /**
   * Delete category (only if taskCount = 0)
   * DELETE /categories/{id}
   */
  deleteCategory(id: number): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/${id}`);
  }

  /**
   * Persist a new display order — pass ids in the desired order.
   * PUT /categories/reorder
   */
  reorderCategories(orderedIds: number[]): Observable<{ success: boolean }> {
    return this.http.put<{ success: boolean }>(`${this.apiUrl}/reorder`, { ids: orderedIds });
  }

  // ============ EXTRA FIELDS ============

  /**
   * Get extra fields for category
   * GET /categories/{id}/fields
   */
  getCategoryFields(categoryId: number): Observable<{ data: ExtraField[]; success: boolean }> {
    return this.http.get<{ data: ExtraField[]; success: boolean }>(`${this.apiUrl}/${categoryId}/fields`);
  }

  /**
   * Add extra field to category
   * POST /categories/{id}/fields
   */
  addExtraField(categoryId: number, request: AddExtraFieldRequest): Observable<{ data: ExtraField; success: boolean }> {
    return this.http.post<{ data: ExtraField; success: boolean }>(`${this.apiUrl}/${categoryId}/fields`, request);
  }

  /**
   * Update extra field
   * PUT /categories/{categoryId}/fields/{fieldId}
   * Backend's ExtraField.id is a Long — fieldId must be numeric, not a string.
   */
  updateExtraField(
    categoryId: number,
    fieldId: number,
    request: UpdateExtraFieldRequest
  ): Observable<{ data: ExtraField; success: boolean }> {
    return this.http.put<{ data: ExtraField; success: boolean }>(
      `${this.apiUrl}/${categoryId}/fields/${fieldId}`,
      request
    );
  }

  /**
   * Delete extra field
   * DELETE /categories/{categoryId}/fields/{fieldId}
   */
  deleteExtraField(categoryId: number, fieldId: number): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/${categoryId}/fields/${fieldId}`);
  }

  /**
   * Persist a new display order for one category's extra fields.
   * PUT /categories/{id}/fields/reorder
   */
  reorderExtraFields(categoryId: number, orderedFieldIds: number[]): Observable<{ success: boolean }> {
    return this.http.put<{ success: boolean }>(`${this.apiUrl}/${categoryId}/fields/reorder`, { ids: orderedFieldIds });
  }

  /**
   * Get aggregate category statistics (system/custom/total counts for the
   * current user). Backend has no per-category stats — this is global, not
   * scoped by categoryId despite the old signature implying otherwise.
   * GET /categories/stats
   */
  getCategoryStats(): Observable<{ data: { systemCategories: number; customCategories: number; total: number }; success: boolean }> {
    return this.http.get<{ data: { systemCategories: number; customCategories: number; total: number }; success: boolean }>(
      `${this.apiUrl}/stats`
    );
  }

  /**
   * Check whether `name` is free within its scope. GET /categories/check-name
   */
  checkCategoryNameAvailable(name: string, tier: 'SYSTEM' | 'CUSTOM', excludeCategoryId?: number): Observable<{ available: boolean }> {
    let params = new HttpParams().set('name', name).set('tier', tier);
    if (excludeCategoryId) params = params.set('excludeCategoryId', excludeCategoryId.toString());
    return this.http.get<{ data: { available: boolean }; success: boolean }>(`${this.apiUrl}/check-name`, { params })
      .pipe(map(res => res.data));
  }
}
