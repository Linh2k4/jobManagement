import { createReducer, on } from '@ngrx/store';
import * as TaskActions from './task.actions';
import { initialTaskState, TaskState } from './task.state';

/**
 * Task reducer
 * Handles all task state mutations
 */
export const taskReducer = createReducer(
  initialTaskState,

  // ============ LOAD TASKS ============
  on(TaskActions.loadTasks, state => ({
    ...state,
    loadingList: true,
    error: null,
    errorType: null
  })),

  on(TaskActions.loadTasksSuccess, (state, { tasks, totalElements, totalPages, currentPage, pageSize }) => ({
    ...state,
    tasks,
    totalElements,
    totalPages,
    currentPage,
    pageSize,
    loadingList: false,
    error: null
  })),

  on(TaskActions.loadTasksFailure, (state, { error }) => ({
    ...state,
    loadingList: false,
    error,
    errorType: 'FETCH' as const
  })),

  // ============ LOAD SINGLE TASK ============
  on(TaskActions.loadTask, state => ({
    ...state,
    loadingTask: true,
    error: null
  })),

  on(TaskActions.loadTaskSuccess, (state, { task }) => {
    const updatedTasks = state.tasks.some(t => t.id === task.id)
      ? state.tasks.map(t => t.id === task.id ? task : t)
      : [...state.tasks, task];
    return {
      ...state,
      tasks: updatedTasks,
      selectedTask: task,
      loadingTask: false
    };
  }),

  on(TaskActions.loadTaskFailure, (state, { error }) => ({
    ...state,
    loadingTask: false,
    error,
    errorType: 'FETCH' as const
  })),

  // ============ SELECT/DESELECT TASK ============
  on(TaskActions.selectTask, (state, { id }) => ({
    ...state,
    selectedTaskId: id
  })),

  on(TaskActions.deselectTask, state => ({
    ...state,
    selectedTaskId: null,
    selectedTask: null
  })),

  // ============ CREATE TASK ============
  on(TaskActions.createTask, state => ({
    ...state,
    loadingCreate: true,
    error: null
  })),

  on(TaskActions.createTaskSuccess, (state, { task }) => ({
    ...state,
    tasks: [task, ...state.tasks],
    totalElements: state.totalElements + 1,
    loadingCreate: false,
    error: null
  })),

  on(TaskActions.createTaskFailure, (state, { error }) => ({
    ...state,
    loadingCreate: false,
    error,
    errorType: 'CREATE' as const
  })),

  // ============ UPDATE TASK ============
  on(TaskActions.updateTask, state => ({
    ...state,
    loadingUpdate: true,
    error: null
  })),

  on(TaskActions.updateTaskSuccess, (state, { task }) => ({
    ...state,
    tasks: state.tasks.map(t => t.id === task.id ? task : t),
    selectedTask: state.selectedTask?.id === task.id ? task : state.selectedTask,
    loadingUpdate: false,
    error: null
  })),

  on(TaskActions.updateTaskFailure, (state, { error }) => ({
    ...state,
    loadingUpdate: false,
    error,
    errorType: 'UPDATE' as const
  })),

  // ============ UPDATE TASK STATUS ============
  on(TaskActions.updateTaskStatus, state => ({
    ...state,
    loadingUpdate: true,
    error: null
  })),

  on(TaskActions.updateTaskStatusSuccess, (state, { task }) => ({
    ...state,
    tasks: state.tasks.map(t => t.id === task.id ? task : t),
    selectedTask: state.selectedTask?.id === task.id ? task : state.selectedTask,
    loadingUpdate: false,
    error: null
  })),

  on(TaskActions.updateTaskStatusFailure, (state, { error }) => ({
    ...state,
    loadingUpdate: false,
    error,
    errorType: 'UPDATE' as const
  })),

  // ============ DELETE TASK ============
  on(TaskActions.deleteTask, state => ({
    ...state,
    loadingDelete: true,
    error: null
  })),

  on(TaskActions.deleteTaskSuccess, (state, { id }) => ({
    ...state,
    tasks: state.tasks.filter(t => t.id !== id),
    totalElements: state.totalElements - 1,
    selectedTaskId: state.selectedTaskId === id ? null : state.selectedTaskId,
    selectedTask: state.selectedTask?.id === id ? null : state.selectedTask,
    loadingDelete: false,
    error: null
  })),

  on(TaskActions.deleteTaskFailure, (state, { error }) => ({
    ...state,
    loadingDelete: false,
    error,
    errorType: 'DELETE' as const
  })),

  // ============ TASK STEPS ============
  on(TaskActions.loadTaskSteps, state => ({
    ...state,
    loadingSteps: true
  })),

  on(TaskActions.loadTaskStepsSuccess, (state, { taskId, steps }) => ({
    ...state,
    steps: { ...state.steps, [taskId]: steps },
    loadingSteps: false
  })),

  on(TaskActions.updateTaskStepStatusSuccess, (state, { taskId, step }) => ({
    ...state,
    steps: {
      ...state.steps,
      [taskId]: state.steps[taskId]?.map(s => s.id === step.id ? step : s) || [step]
    }
  })),

  on(TaskActions.selectTaskStep, (state, { stepId }) => ({
    ...state,
    selectedStepId: stepId
  })),

  // ============ FILTERING & PAGINATION ============
  on(TaskActions.setTaskFilter, (state, { filter }) => ({
    ...state,
    filter,
    currentPage: 0 // Reset to first page on filter change
  })),

  on(TaskActions.clearTaskFilter, state => ({
    ...state,
    filter: {},
    currentPage: 0
  })),

  on(TaskActions.setTaskPage, (state, { page }) => ({
    ...state,
    currentPage: page
  })),

  on(TaskActions.setTaskPageSize, (state, { size }) => ({
    ...state,
    pageSize: size,
    currentPage: 0
  })),

  on(TaskActions.setTaskSort, (state, { sort }) => ({
    ...state,
    sortBy: sort
  })),

  // ============ BATCH ACTIONS ============
  on(TaskActions.selectTasks, (state, { ids }) => ({
    ...state,
    selectedTaskIds: ids
  })),

  on(TaskActions.clearTaskSelection, state => ({
    ...state,
    selectedTaskIds: []
  })),

  // ============ UTILITY ============
  on(TaskActions.clearTaskError, state => ({
    ...state,
    error: null,
    errorType: null,
    errorDetails: null
  }))
);
