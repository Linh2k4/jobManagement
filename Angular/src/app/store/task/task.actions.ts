import { createAction, props } from '@ngrx/store';
import { Task, TaskFilter, CreateTaskRequest, UpdateTaskRequest, UpdateTaskStatusRequest } from '../../core/models';

// ============ TASK LIST ACTIONS ============

export const loadTasks = createAction(
  '[Task] Load Tasks',
  props<{ page?: number; size?: number; filter?: TaskFilter; sort?: string }>()
);

export const loadTasksSuccess = createAction(
  '[Task] Load Tasks Success',
  props<{ tasks: Task[]; totalElements: number; totalPages: number; currentPage: number; pageSize: number }>()
);

export const loadTasksFailure = createAction(
  '[Task] Load Tasks Failure',
  props<{ error: string }>()
);

export const loadMyTasks = createAction(
  '[Task] Load My Tasks',
  props<{ page?: number; size?: number }>()
);

export const searchTasks = createAction(
  '[Task] Search Tasks',
  props<{ filter: TaskFilter; page?: number; size?: number }>()
);

// ============ TASK DETAIL ACTIONS ============

export const loadTask = createAction(
  '[Task] Load Task',
  props<{ id: number }>()
);

export const loadTaskSuccess = createAction(
  '[Task] Load Task Success',
  props<{ task: Task }>()
);

export const loadTaskFailure = createAction(
  '[Task] Load Task Failure',
  props<{ error: string }>()
);

export const selectTask = createAction(
  '[Task] Select Task',
  props<{ id: number }>()
);

export const deselectTask = createAction(
  '[Task] Deselect Task'
);

// ============ CREATE TASK ACTIONS ============

export const createTask = createAction(
  '[Task] Create Task',
  props<{ request: CreateTaskRequest }>()
);

export const createTaskSuccess = createAction(
  '[Task] Create Task Success',
  props<{ task: Task }>()
);

export const createTaskFailure = createAction(
  '[Task] Create Task Failure',
  props<{ error: string }>()
);

// ============ UPDATE TASK ACTIONS ============

export const updateTask = createAction(
  '[Task] Update Task',
  props<{ id: number; request: UpdateTaskRequest }>()
);

export const updateTaskSuccess = createAction(
  '[Task] Update Task Success',
  props<{ task: Task }>()
);

export const updateTaskFailure = createAction(
  '[Task] Update Task Failure',
  props<{ error: string }>()
);

// ============ TASK STATUS ACTIONS ============

export const updateTaskStatus = createAction(
  '[Task] Update Task Status',
  props<{ id: number; request: UpdateTaskStatusRequest }>()
);

export const updateTaskStatusSuccess = createAction(
  '[Task] Update Task Status Success',
  props<{ task: Task }>()
);

export const updateTaskStatusFailure = createAction(
  '[Task] Update Task Status Failure',
  props<{ error: string }>()
);

// ============ DELETE TASK ACTIONS ============

export const deleteTask = createAction(
  '[Task] Delete Task',
  props<{ id: number }>()
);

export const deleteTaskSuccess = createAction(
  '[Task] Delete Task Success',
  props<{ id: number }>()
);

export const deleteTaskFailure = createAction(
  '[Task] Delete Task Failure',
  props<{ error: string }>()
);

// ============ TASK STEP ACTIONS ============

export const loadTaskSteps = createAction(
  '[Task] Load Task Steps',
  props<{ taskId: number }>()
);

export const loadTaskStepsSuccess = createAction(
  '[Task] Load Task Steps Success',
  props<{ taskId: number; steps: any[] }>()
);

export const updateTaskStepStatus = createAction(
  '[Task] Update Task Step Status',
  props<{ taskId: number; stepId: number; status: string }>()
);

export const updateTaskStepStatusSuccess = createAction(
  '[Task] Update Task Step Status Success',
  props<{ taskId: number; step: any }>()
);

export const selectTaskStep = createAction(
  '[Task] Select Task Step',
  props<{ stepId: number }>()
);

// ============ FILTERING & PAGINATION ACTIONS ============

export const setTaskFilter = createAction(
  '[Task] Set Task Filter',
  props<{ filter: TaskFilter }>()
);

export const clearTaskFilter = createAction(
  '[Task] Clear Task Filter'
);

export const setTaskPage = createAction(
  '[Task] Set Task Page',
  props<{ page: number }>()
);

export const setTaskPageSize = createAction(
  '[Task] Set Task Page Size',
  props<{ size: number }>()
);

export const setTaskSort = createAction(
  '[Task] Set Task Sort',
  props<{ sort: string }>()
);

// ============ BATCH ACTIONS ============

export const selectTasks = createAction(
  '[Task] Select Tasks',
  props<{ ids: number[] }>()
);

export const clearTaskSelection = createAction(
  '[Task] Clear Task Selection'
);

// ============ UTILITY ACTIONS ============

export const invalidateTaskCache = createAction(
  '[Task] Invalidate Task Cache'
);

export const clearTaskError = createAction(
  '[Task] Clear Task Error'
);
