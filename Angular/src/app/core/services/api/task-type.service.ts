import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { TaskTypeSummary } from '../../models';

@Injectable({
  providedIn: 'root'
})
export class TaskTypeService {
  private apiUrl = '/api/v1/task-types';

  constructor(private http: HttpClient) {}

  /**
   * List active task type templates (FAST/OFTEN/MULTI_STEP)
   * GET /task-types
   */
  getTaskTypes(): Observable<TaskTypeSummary[]> {
    return this.http.get<{ data: TaskTypeSummary[]; success: boolean }>(this.apiUrl)
      .pipe(map(res => res.data));
  }
}
