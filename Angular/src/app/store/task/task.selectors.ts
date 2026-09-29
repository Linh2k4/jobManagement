import { createFeatureSelector, createSelector } from '@ngrx/store';
import { TaskState } from './task.state';
import { Task, TaskStatus, TaskPriority } from '../../core/models';

/**
 * Feature selector for task state
 */
export const selectTaskState = createFeatureSelector<TaskState>('task');

// ============ BASIC SELECTORS ============

export const selectAllTasks = createSelector(
  selectTaskState,
  (state: TaskState) => state.tasks
);

export const selectSelectedTaskId = createSelector(
  selectTaskState,
  (state: TaskState) => state.selectedTaskId
);

export const selectSelectedTask = createSelector(
  selectTaskState,
  (state: TaskState) => state.selectedTask
);

export const selectTaskLoading = createSelector(
  selectTaskState,
  (state: TaskState) => state.loading || state.loadingList || state.loadingTask
);

export const selectTaskListLoading = createSelector(
  selectTaskState,
  (state: TaskState) => state.loadingList
);

export const selectTaskDetailLoading = createSelector(
  selectTaskState,
  (state: TaskState) => state.loadingTask
);

export const selectTaskError = createSelector(
  selectTaskState,
  (state: TaskState) => state.error
);

export const selectTaskErrorType = createSelector(
  selectTaskState,
  (state: TaskState) => state.errorType
);

// ============ PAGINATION SELECTORS ============

export const selectTaskPagination = createSelector(
  selectTaskState,
  (state: TaskState) => ({
    currentPage: state.currentPage,
    pageSize: state.pageSize,
    totalElements: state.totalElements,
    totalPages: state.totalPages
  })
);

export const selectCurrentPage = createSelector(
  selectTaskState,
  (state: TaskState) => state.currentPage
);

export const selectPageSize = createSelector(
  selectTaskState,
  (state: TaskState) => state.pageSize
);

export const selectTotalElements = createSelector(
  selectTaskState,
  (state: TaskState) => state.totalElements
);

// ============ FILTER SELECTORS ============

export const selectTaskFilter = createSelector(
  selectTaskState,
  (state: TaskState) => state.filter
);

export const selectTasksByStatus = createSelector(
  selectAllTasks,
  selectTaskFilter,
  (tasks: Task[], filter) => {
    if (!filter?.status) return tasks;
    return tasks.filter(t => t.status === filter.status);
  }
);

// ============ COMPUTED SELECTORS ============

/** Past due date, not yet in a terminal state — there's no "OVERDUE" status, it's derived. */
function isOverdue(t: Task): boolean {
  if (!t.dueDate || t.status === 'DONE' || t.status === 'CANCELLED' || t.status === 'CLOSED_LATE') return false;
  return new Date(t.dueDate) < new Date();
}

export const selectTaskStats = createSelector(
  selectAllTasks,
  (tasks: Task[]) => ({
    total: tasks.length,
    pending: tasks.filter(t => t.status === 'PENDING').length,
    inProgress: tasks.filter(t => t.status === 'IN_PROGRESS').length,
    done: tasks.filter(t => t.status === 'DONE').length,
    overdue: tasks.filter(isOverdue).length,
    completionRate: tasks.length > 0 ? (tasks.filter(t => t.status === 'DONE').length / tasks.length) * 100 : 0
  })
);

export const selectHighPriorityTasks = createSelector(
  selectAllTasks,
  (tasks: Task[]) => tasks.filter(t => t.priority === 'HIGH' || t.priority === 'URGENT')
);

export const selectOverdueTasks = createSelector(
  selectAllTasks,
  (tasks: Task[]) => tasks.filter(t => isOverdue(t) || t.status === 'CLOSED_LATE')
);

export const selectUpcomingDeadlines = createSelector(
  selectAllTasks,
  (tasks: Task[]) => {
    const now = new Date();
    const twentyFourHoursFromNow = new Date(now.getTime() + 24 * 60 * 60 * 1000);
    return tasks.filter(t => {
      if (!t.dueDate) return false;
      const dueDate = new Date(t.dueDate);
      return dueDate > now && dueDate <= twentyFourHoursFromNow && t.status !== 'DONE';
    });
  }
);

// ============ STEP SELECTORS ============

export const selectTaskSteps = createSelector(
  selectTaskState,
  selectSelectedTaskId,
  (state: TaskState, selectedTaskId) => {
    if (!selectedTaskId) return [];
    return state.steps[selectedTaskId] || [];
  }
);

export const selectSelectedStepId = createSelector(
  selectTaskState,
  (state: TaskState) => state.selectedStepId
);

export const selectTaskStepProgress = createSelector(
  selectTaskSteps,
  (steps: any[]) => {
    if (steps.length === 0) return 0;
    const done = steps.filter(s => s.status === 'DONE').length;
    return (done / steps.length) * 100;
  }
);

// ============ BATCH SELECTION SELECTORS ============

export const selectSelectedTaskIds = createSelector(
  selectTaskState,
  (state: TaskState) => state.selectedTaskIds
);

export const selectSelectedTasks = createSelector(
  selectAllTasks,
  selectSelectedTaskIds,
  (tasks: Task[], selectedIds: number[]) => {
    return tasks.filter(t => selectedIds.includes(t.id));
  }
);

export const selectHasSelectedTasks = createSelector(
  selectSelectedTaskIds,
  (ids: number[]) => ids.length > 0
);

// ============ SORT & ORDER SELECTORS ============

export const selectTasksSorted = createSelector(
  selectAllTasks,
  selectTaskState,
  (tasks: Task[], state: TaskState) => {
    const [sortField, sortOrder] = state.sortBy.split(',');
    const isAsc = sortOrder !== 'desc';

    return [...tasks].sort((a, b) => {
      let aVal = (a as any)[sortField];
      let bVal = (b as any)[sortField];

      if (aVal < bVal) return isAsc ? -1 : 1;
      if (aVal > bVal) return isAsc ? 1 : -1;
      return 0;
    });
  }
);
