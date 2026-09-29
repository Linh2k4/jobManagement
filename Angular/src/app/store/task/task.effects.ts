import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, mergeMap, catchError, switchMap, tap } from 'rxjs/operators';
import { TaskService } from '../../core/services/api/task.service';
import * as TaskActions from './task.actions';

/**
 * Task Effects
 * Handle side effects for task operations (API calls, navigation, notifications, etc.)
 */
@Injectable()
export class TaskEffects {
  private actions$ = inject(Actions);
  private taskService = inject(TaskService);

  /**
   * Load tasks effect
   * Trigger: loadTasks action
   * Side effect: Call TaskService.searchTasks() with filters
   */
  loadTasks$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TaskActions.loadTasks),
      switchMap(({ page = 0, size = 20, filter, sort }) =>
        this.taskService.searchTasks({
          ...filter,
          page,
          size,
          sort
        }).pipe(
          map(response => TaskActions.loadTasksSuccess({
            tasks: response.data,
            totalElements: response.totalElements,
            totalPages: response.totalPages,
            currentPage: response.currentPage,
            pageSize: response.pageSize
          })),
          catchError(error => of(TaskActions.loadTasksFailure({
            error: error.message || 'Failed to load tasks'
          })))
        )
      )
    )
  );

  /**
   * Load my tasks effect
   * Trigger: loadMyTasks action
   * Side effect: Call TaskService.getMyTasks()
   */
  loadMyTasks$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TaskActions.loadMyTasks),
      switchMap(({ page = 0, size = 20 }) =>
        this.taskService.getMyTasks(page, size).pipe(
          map(response => TaskActions.loadTasksSuccess({
            tasks: response.data,
            totalElements: response.totalElements,
            totalPages: response.totalPages,
            currentPage: response.currentPage,
            pageSize: response.pageSize
          })),
          catchError(error => of(TaskActions.loadTasksFailure({
            error: error.message || 'Failed to load your tasks'
          })))
        )
      )
    )
  );

  /**
   * Load single task effect
   * Trigger: loadTask action
   * Side effect: Call TaskService.getTask()
   */
  loadTask$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TaskActions.loadTask),
      mergeMap(({ id }) =>
        this.taskService.getTask(id).pipe(
          map(response => TaskActions.loadTaskSuccess({
            task: response.data
          })),
          catchError(error => of(TaskActions.loadTaskFailure({
            error: error.message || 'Failed to load task'
          })))
        )
      )
    )
  );

  /**
   * Create task effect
   * Trigger: createTask action
   * Side effect: Call TaskService.createTask()
   */
  createTask$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TaskActions.createTask),
      mergeMap(({ request }) =>
        this.taskService.createTask(request).pipe(
          map(response => TaskActions.createTaskSuccess({
            task: response.data
          })),
          catchError(error => of(TaskActions.createTaskFailure({
            error: error.message || 'Failed to create task'
          })))
        )
      )
    )
  );

  /**
   * Update task effect
   * Trigger: updateTask action
   * Side effect: Call TaskService.updateTask()
   */
  updateTask$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TaskActions.updateTask),
      mergeMap(({ id, request }) =>
        this.taskService.updateTask(id, request).pipe(
          map(response => TaskActions.updateTaskSuccess({
            task: response.data
          })),
          catchError(error => of(TaskActions.updateTaskFailure({
            error: error.message || 'Failed to update task'
          })))
        )
      )
    )
  );

  /**
   * Update task status effect
   * Trigger: updateTaskStatus action
   * Side effect: Call TaskService.updateTaskStatus()
   * Note: Also triggers loadTaskSteps to unlock next sequential steps
   */
  updateTaskStatus$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TaskActions.updateTaskStatus),
      mergeMap(({ id, request }) =>
        this.taskService.updateTaskStatus(id, request).pipe(
          map(response => TaskActions.updateTaskStatusSuccess({
            task: response.data
          })),
          catchError(error => of(TaskActions.updateTaskStatusFailure({
            error: error.message || 'Failed to update task status'
          })))
        )
      )
    )
  );

  /**
   * Auto-load steps when task status updates
   * This enables auto-unlock logic for sequential steps
   */
  loadStepsAfterStatusUpdate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TaskActions.updateTaskStatusSuccess),
      map(({ task }) => TaskActions.loadTaskSteps({ taskId: task.id }))
    )
  );

  /**
   * Delete task effect
   * Trigger: deleteTask action
   * Side effect: Call TaskService.deleteTask()
   */
  deleteTask$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TaskActions.deleteTask),
      mergeMap(({ id }) =>
        this.taskService.deleteTask(id).pipe(
          map(() => TaskActions.deleteTaskSuccess({ id })),
          catchError(error => of(TaskActions.deleteTaskFailure({
            error: error.message || 'Failed to delete task'
          })))
        )
      )
    )
  );

  // loadTaskSteps$/updateTaskStepStatus$ effects removed — the flat
  // Task -> TaskStep step model they targeted no longer exists (replaced by
  // the Task -> GroupSubtask -> Subtask -> Step tree, Scope.md §2.1/§2.2).
  // Nothing dispatched their actions (task-detail.component.ts manages the
  // subtask/step tree via direct TaskService calls + a loadTask re-dispatch,
  // not this store slice), so there's nothing to port forward.

  /**
   * Search tasks effect
   * Trigger: searchTasks action
   * Side effect: Same as loadTasks but with search filter
   */
  searchTasks$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TaskActions.searchTasks),
      switchMap(({ filter, page = 0, size = 20 }) =>
        this.taskService.searchTasks({ ...filter, page, size }).pipe(
          map(response => TaskActions.loadTasksSuccess({
            tasks: response.data,
            totalElements: response.totalElements,
            totalPages: response.totalPages,
            currentPage: response.currentPage,
            pageSize: response.pageSize
          })),
          catchError(error => of(TaskActions.loadTasksFailure({
            error: error.message || 'Search failed'
          })))
        )
      )
    )
  );

}
