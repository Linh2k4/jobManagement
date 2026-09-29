import { Task, TaskFilter } from '../../core/models';

/**
 * Task Feature State
 * Manages task CRUD operations, filtering, pagination, and step dependencies
 */
export interface TaskState {
  // Task list
  tasks: Task[];
  selectedTaskId: number | null;
  selectedTask: Task | null;

  // Pagination & Sorting
  currentPage: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  sortBy: string; // e.g., "deadline,asc" or "createdAt,desc"

  // Filtering
  filter: TaskFilter & {
    searchText?: string;
    dateRangeStart?: string;
    dateRangeEnd?: string;
  };

  // Loading states
  loading: boolean;
  loadingTask: boolean; // Loading specific task detail
  loadingList: boolean; // Loading task list
  loadingCreate: boolean;
  loadingUpdate: boolean;
  loadingDelete: boolean;
  loadingSteps: boolean;

  // Error handling
  error: string | null;
  errorType: 'FETCH' | 'CREATE' | 'UPDATE' | 'DELETE' | null;
  errorDetails: Record<string, any> | null;

  // Step management
  steps: { [taskId: number]: any[] }; // Step data cached by task ID
  selectedStepId: number | null;

  // Batch operations
  selectedTaskIds: number[]; // For bulk actions
  bulkOperationLoading: boolean;
}

/**
 * Initial state for task feature
 */
export const initialTaskState: TaskState = {
  tasks: [],
  selectedTaskId: null,
  selectedTask: null,
  currentPage: 0,
  pageSize: 20,
  totalElements: 0,
  totalPages: 0,
  sortBy: 'deadline,asc',
  filter: {},
  loading: false,
  loadingTask: false,
  loadingList: false,
  loadingCreate: false,
  loadingUpdate: false,
  loadingDelete: false,
  loadingSteps: false,
  error: null,
  errorType: null,
  errorDetails: null,
  steps: {},
  selectedStepId: null,
  selectedTaskIds: [],
  bulkOperationLoading: false
};
