import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  Task,
  GroupSubtask,
  Subtask,
  Step,
  TaskFilter,
  CreateTaskRequest,
  UpdateTaskRequest,
  UpdateTaskStatusRequest,
  TaskResponse,
  TaskListResponse,
  TaskComment,
  TaskAttachment,
  KanbanBoard,
  TimeCategory,
  TimelineMember,
  DeadlineExtension
} from '../../models';

/** Backend envelope: { success, message, data, timestamp } */
interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

/** Spring Data Page<T> JSON shape */
interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // current page, 0-indexed
  size: number;
}

function toTaskListResponse(page: SpringPage<Task>): TaskListResponse {
  return {
    data: page.content,
    totalElements: page.totalElements,
    totalPages: page.totalPages,
    currentPage: page.number,
    pageSize: page.size,
    success: true
  };
}

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  private apiUrl = '/api/v1/tasks';

  constructor(private http: HttpClient) {}

  /**
   * Create a new task
   * POST /tasks
   */
  createTask(request: CreateTaskRequest): Observable<TaskResponse> {
    return this.http.post<TaskResponse>(this.apiUrl, request);
  }

  /**
   * Get task by ID
   * GET /tasks/{id}
   */
  getTask(id: number): Observable<TaskResponse> {
    return this.http.get<TaskResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * Search/List tasks with filters
   * GET /tasks?status=PENDING&taskTypeId=1&page=0&size=10
   *
   * Backend's `listTasks` only supports status/taskTypeId/section/dueDateFrom/
   * dueDateTo/search — type/category/priority/assigneeId/creatorId/difficulty
   * from TaskFilter aren't filterable server-side yet and are ignored here
   * rather than sent as dead params.
   */
  searchTasks(filter: TaskFilter & { page?: number; size?: number; sort?: string }): Observable<TaskListResponse> {
    let params = new HttpParams();

    if (filter.status) params = params.set('status', filter.status);
    if (filter.searchText) params = params.set('search', filter.searchText);
    if (filter.deadlineAfter) params = params.set('dueDateFrom', filter.deadlineAfter);
    if (filter.deadlineBefore) params = params.set('dueDateTo', filter.deadlineBefore);

    params = params
      .set('page', (filter.page ?? 0).toString())
      .set('size', (filter.size ?? 20).toString());

    if (filter.sort) params = params.set('sort', filter.sort);

    return this.http.get<ApiEnvelope<SpringPage<Task>>>(this.apiUrl, { params })
      .pipe(map(res => toTaskListResponse(res.data)));
  }

  /**
   * Get tasks for current user.
   * Backend's GET /tasks already scopes to "my tasks" for non-manager roles
   * server-side — there is no separate /tasks/my endpoint.
   */
  getMyTasks(page: number = 0, size: number = 20): Observable<TaskListResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<ApiEnvelope<SpringPage<Task>>>(this.apiUrl, { params })
      .pipe(map(res => toTaskListResponse(res.data)));
  }

  /**
   * Update task
   * PUT /tasks/{id}
   */
  updateTask(id: number, request: UpdateTaskRequest): Observable<TaskResponse> {
    return this.http.put<TaskResponse>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Update task status
   * PATCH /tasks/{id}/status
   */
  updateTaskStatus(id: number, request: UpdateTaskStatusRequest): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`${this.apiUrl}/${id}/status`, request);
  }

  /**
   * Submit task for completion approval
   * POST /tasks/{id}/submit-completion
   */
  submitCompletion(id: number): Observable<ApiEnvelope<Task>> {
    return this.http.post<ApiEnvelope<Task>>(`${this.apiUrl}/${id}/submit-completion`, {});
  }

  /**
   * Approve task completion (Leader / Admin)
   * POST /tasks/{id}/approve-completion
   */
  approveCompletion(id: number, note?: string): Observable<ApiEnvelope<Task>> {
    return this.http.post<ApiEnvelope<Task>>(`${this.apiUrl}/${id}/approve-completion`, { note });
  }

  /**
   * Reject task completion (Leader / Admin)
   * POST /tasks/{id}/reject-completion
   */
  rejectCompletion(id: number, reason: string): Observable<ApiEnvelope<Task>> {
    return this.http.post<ApiEnvelope<Task>>(`${this.apiUrl}/${id}/reject-completion`, { reason });
  }

  /**
   * Assign task to user
   * POST /tasks/{taskId}/assignments
   */
  assignTask(taskId: number, assigneeId: number, groupId?: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${taskId}/assignments`, { assigneeId, groupId });
  }

  /**
   * Unassign user or all users from task
   * DELETE /tasks/{taskId}/assignments
   */
  unassignTask(taskId: number, assigneeId?: number): Observable<any> {
    const url = assigneeId ? `${this.apiUrl}/${taskId}/assignments?assigneeId=${assigneeId}` : `${this.apiUrl}/${taskId}/assignments/current`;
    return this.http.delete(url);
  }

  /**
   * Delete task
   * DELETE /tasks/{id}
   */
  deleteTask(id: number): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/${id}`);
  }

  // ============ KANBAN BOARD ============

  /**
   * Kanban board: tasks bucketed into todo/doing/done.
   * GET /tasks/kanban
   */
  getKanbanBoard(filter?: { type?: TimeCategory; search?: string; assigneeId?: number }): Observable<KanbanBoard> {
    let params = new HttpParams();
    if (filter?.type) params = params.set('type', filter.type);
    if (filter?.search) params = params.set('search', filter.search);
    if (filter?.assigneeId) params = params.set('assigneeId', filter.assigneeId.toString());
    return this.http.get<ApiEnvelope<KanbanBoard>>(`${this.apiUrl}/kanban`, { params })
      .pipe(map(res => res.data));
  }

  /**
   * Quick-add a task from a Kanban column — title + task type only.
   * POST /tasks/quick
   */
  quickCreateTask(taskTypeId: number, title: string): Observable<{ data: Task; success: boolean }> {
    return this.http.post<{ data: Task; success: boolean }>(`${this.apiUrl}/quick`, { taskTypeId, title });
  }

  /**
   * Timeline/Gantt data: one row per member with their task bars.
   * GET /tasks/timeline
   */
  getTimeline(includeCompleted: boolean, assigneeId?: number): Observable<TimelineMember[]> {
    let params = new HttpParams().set('includeCompleted', includeCompleted.toString());
    if (assigneeId) params = params.set('assigneeId', assigneeId.toString());
    return this.http.get<ApiEnvelope<TimelineMember[]>>(`${this.apiUrl}/timeline`, { params })
      .pipe(map(res => res.data));
  }

  // ============ DEADLINE EXTENSIONS ============

  /**
   * Request a deadline extension (Member/Lead — Manager self-approves).
   * POST /tasks/{id}/deadline-extensions
   */
  requestDeadlineExtension(taskId: number, requestedDeadline: string, reason: string): Observable<{ data: DeadlineExtension; success: boolean }> {
    return this.http.post<{ data: DeadlineExtension; success: boolean }>(
      `${this.apiUrl}/${taskId}/deadline-extensions`, { requestedDeadline, reason }
    );
  }

  /** GET /tasks/{id}/deadline-extensions */
  getDeadlineExtensionHistory(taskId: number): Observable<DeadlineExtension[]> {
    return this.http.get<{ data: DeadlineExtension[]; success: boolean }>(`${this.apiUrl}/${taskId}/deadline-extensions`)
      .pipe(map(res => res.data));
  }

  /** GET /deadline-extensions — Lead: own team; Manager: whole company */
  listDeadlineExtensions(status?: string): Observable<DeadlineExtension[]> {
    let params = new HttpParams();
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    return this.http.get<{ data: DeadlineExtension[]; success: boolean }>(`/api/v1/deadline-extensions`, { params })
      .pipe(map(res => res.data || []));
  }

  /** GET /deadline-extensions/pending — Lead: own team; Manager: whole company */
  getPendingDeadlineExtensions(): Observable<DeadlineExtension[]> {
    return this.http.get<{ data: DeadlineExtension[]; success: boolean }>(`/api/v1/deadline-extensions/pending`)
      .pipe(map(res => res.data || []));
  }

  approveDeadlineExtension(id: number, note?: string): Observable<{ data: DeadlineExtension; success: boolean }> {
    return this.http.post<{ data: DeadlineExtension; success: boolean }>(`/api/v1/deadline-extensions/${id}/approve`, { note });
  }

  rejectDeadlineExtension(id: number, note: string): Observable<{ data: DeadlineExtension; success: boolean }> {
    return this.http.post<{ data: DeadlineExtension; success: boolean }>(`/api/v1/deadline-extensions/${id}/reject`, { note });
  }

  // ============ GROUP SUBTASKS ============
  // Task -> GroupSubtask (optional) -> Subtask -> Step (Scope.md §2.1/§2.2)

  /** POST /tasks/{id}/group-subtasks — creator or Manager only */
  createGroupSubtask(taskId: number, name: string): Observable<{ data: GroupSubtask; success: boolean }> {
    return this.http.post<{ data: GroupSubtask; success: boolean }>(`${this.apiUrl}/${taskId}/group-subtasks`, { name });
  }

  /** PUT /tasks/{taskId}/group-subtasks/{groupId} */
  updateGroupSubtask(taskId: number, groupId: number, name: string): Observable<{ data: GroupSubtask; success: boolean }> {
    return this.http.put<{ data: GroupSubtask; success: boolean }>(`${this.apiUrl}/${taskId}/group-subtasks/${groupId}`, { name });
  }

  /** DELETE /tasks/{taskId}/group-subtasks/{groupId} — cascades to its subtasks and steps */
  deleteGroupSubtask(taskId: number, groupId: number): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/${taskId}/group-subtasks/${groupId}`);
  }

  // ============ SUBTASKS ============
  // Leaf for FAST tasks (ticked done manually); parent-of-steps for MULTI_STEP tasks

  /** POST /tasks/{id}/subtasks — groupSubtaskId is optional (omit for ungrouped) */
  createSubtask(taskId: number, subtask: {
    groupSubtaskId?: number; title: string; estimateMinutes?: number; deadline?: string; note?: string;
  }): Observable<{ data: Subtask; success: boolean }> {
    return this.http.post<{ data: Subtask; success: boolean }>(`${this.apiUrl}/${taskId}/subtasks`, subtask);
  }

  /** PUT /tasks/{taskId}/subtasks/{subtaskId} — edit fields (title/estimate/deadline/note/target) */
  updateSubtask(taskId: number, subtaskId: number, fields: {
    title?: string; estimateMinutes?: number; deadline?: string; note?: string;
    isTarget?: boolean; estimateTarget?: number; target?: number;
  }): Observable<{ data: Subtask; success: boolean }> {
    return this.http.put<{ data: Subtask; success: boolean }>(`${this.apiUrl}/${taskId}/subtasks/${subtaskId}`, fields);
  }

  /** PUT /tasks/{taskId}/subtasks/{subtaskId}/status — FAST subtask manual tick only (no steps) */
  updateSubtaskStatus(taskId: number, subtaskId: number, status: string): Observable<{ data: Subtask; success: boolean }> {
    return this.http.put<{ data: Subtask; success: boolean }>(`${this.apiUrl}/${taskId}/subtasks/${subtaskId}/status`, { status });
  }

  /** DELETE /tasks/{taskId}/subtasks/{subtaskId} — cascades to its steps, if any */
  deleteSubtask(taskId: number, subtaskId: number): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/${taskId}/subtasks/${subtaskId}`);
  }

  // ============ STEPS ============
  // Leaf of a Subtask, MULTI_STEP tasks only — sequential within the subtask

  /** POST /subtasks/{subtaskId}/steps — creator or Manager only */
  createStep(subtaskId: number, step: { name: string; estimateMinutes?: number; deadline?: string }): Observable<{ data: Step; success: boolean }> {
    return this.http.post<{ data: Step; success: boolean }>(`/api/v1/subtasks/${subtaskId}/steps`, step);
  }

  /** PUT /subtasks/{subtaskId}/steps/{stepId} — rename/re-estimate, not status */
  updateStep(subtaskId: number, stepId: number, step: { name?: string; estimateMinutes?: number; deadline?: string }): Observable<{ data: Step; success: boolean }> {
    return this.http.put<{ data: Step; success: boolean }>(`/api/v1/subtasks/${subtaskId}/steps/${stepId}`, step);
  }

  /** PUT /subtasks/{subtaskId}/steps/{stepId}/status */
  updateStepStatus(subtaskId: number, stepId: number, status: string): Observable<{ data: Step; success: boolean }> {
    return this.http.put<{ data: Step; success: boolean }>(`/api/v1/subtasks/${subtaskId}/steps/${stepId}/status`, { status });
  }

  /** POST /subtasks/{subtaskId}/steps/{stepId}/assign — Lead or Manager only */
  assignStep(subtaskId: number, stepId: number, userId: number): Observable<{ data: Step; success: boolean }> {
    return this.http.post<{ data: Step; success: boolean }>(`/api/v1/subtasks/${subtaskId}/steps/${stepId}/assign`, { userId });
  }

  /** DELETE /subtasks/{subtaskId}/steps/{stepId} */
  deleteStep(subtaskId: number, stepId: number): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`/api/v1/subtasks/${subtaskId}/steps/${stepId}`);
  }

  // ============ TASK ATTACHMENTS ============

  /**
   * Upload task attachment
   * POST /tasks/{id}/attachments (multipart/form-data)
   */
  uploadAttachment(taskId: number, file: File): Observable<{ data: TaskAttachment; success: boolean }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ data: TaskAttachment; success: boolean }>(`${this.apiUrl}/${taskId}/attachments`, formData);
  }

  /**
   * List task attachments
   * GET /tasks/{id}/attachments
   */
  getAttachments(taskId: number): Observable<{ data: TaskAttachment[]; success: boolean }> {
    return this.http.get<{ data: TaskAttachment[]; success: boolean }>(`${this.apiUrl}/${taskId}/attachments`);
  }

  /**
   * Download task attachment (binary)
   * GET /tasks/{taskId}/attachments/{attachmentId}/download
   */
  downloadAttachment(taskId: number, attachmentId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${taskId}/attachments/${attachmentId}/download`, { responseType: 'blob' });
  }

  /**
   * Delete task attachment
   * DELETE /tasks/{taskId}/attachments/{attachmentId}
   */
  deleteAttachment(taskId: number, attachmentId: number): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/${taskId}/attachments/${attachmentId}`);
  }

  // ============ TASK COMMENTS ============

  /**
   * Get task comments
   * GET /tasks/{id}/comments
   */
  getTaskComments(taskId: number): Observable<{ data: TaskComment[]; success: boolean }> {
    return this.http.get<{ data: TaskComment[]; success: boolean }>(`${this.apiUrl}/${taskId}/comments`);
  }

  /**
   * Add comment to task
   * POST /tasks/{id}/comments
   */
  addComment(taskId: number, content: string): Observable<{ data: TaskComment; success: boolean }> {
    return this.http.post<{ data: TaskComment; success: boolean }>(`${this.apiUrl}/${taskId}/comments`, { content });
  }

  /**
   * Delete task comment
   * DELETE /tasks/{taskId}/comments/{commentId}
   */
  deleteComment(taskId: number, commentId: number): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/${taskId}/comments/${commentId}`);
  }

  // ============ TASK PRIVATE NOTES ============

  /**
   * Get my private notes for task
   * GET /tasks/{id}/private-notes/me
   */
  getPrivateNotes(taskId: number): Observable<{ data: string; success: boolean }> {
    return this.http.get<{ data: string; success: boolean }>(`${this.apiUrl}/${taskId}/private-notes/me`);
  }

  /**
   * Update my private notes
   * PUT /tasks/{id}/private-notes/me
   */
  updatePrivateNotes(taskId: number, notes: string): Observable<{ success: boolean }> {
    return this.http.put<{ success: boolean }>(`${this.apiUrl}/${taskId}/private-notes/me`, { notes });
  }

  // ============ TASK DIFFICULTY & EXTENSIONS ============

  /**
   * Update task difficulty (1-5). Lead/Manager only.
   * PATCH /tasks/{id}/difficulty
   */
  updateDifficulty(taskId: number, difficulty: number): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`${this.apiUrl}/${taskId}/difficulty`, { difficulty });
  }

  /**
   * Extend task deadline
   * POST /tasks/{id}/extend-deadline
   */
  extendDeadline(taskId: number, daysToAdd: number, reason: string): Observable<TaskResponse> {
    return this.http.post<TaskResponse>(`${this.apiUrl}/${taskId}/extend-deadline`, {
      daysToAdd,
      reason
    });
  }

  /**
   * Get task audit summary (who/when created, last update, current
   * assignee) — a single snapshot object, not a list of discrete events.
   * GET /tasks/{id}/audit
   */
  getTaskHistory(taskId: number): Observable<{ data: Record<string, any>; success: boolean }> {
    return this.http.get<{ data: Record<string, any>; success: boolean }>(`${this.apiUrl}/${taskId}/audit`);
  }
}
