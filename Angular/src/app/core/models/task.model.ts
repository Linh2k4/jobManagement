export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'WAITING_APPROVAL' | 'DONE' | 'CLOSED_LATE' | 'CANCELLED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type Difficulty = 1 | 2 | 3 | 4 | 5;
export type TimeCategory = 'FAST' | 'OFTEN' | 'MULTI_STEP';

export interface UserSummary {
  id: number;
  email: string;
  fullName: string;
  role: string;
}

/** Mirrors the real backend TaskTypeResponse (GET /task-types). */
export interface TaskTypeSummary {
  id: number;
  code: string;
  name: string;
  timeCategory: TimeCategory;
  hasEstimate: boolean;
  defaultEstimateMinutes?: number;
  isMultiStep: boolean;
  isActive: boolean;
}

export interface TaskAssignmentInfo {
  id: number;
  assignee: UserSummary;
  assignedBy: UserSummary;
  assignedAt: string;
  isCurrent: boolean;
}

/** Mirrors the real backend TaskResponse (GET/POST/PUT /tasks). */
export interface Task {
  id: number;
  taskTypeId: number;
  title: string;
  description?: string;
  status: TaskStatus;
  priority?: TaskPriority;
  difficulty?: Difficulty;
  estimateMinutes?: number;
  dueDate?: string; // YYYY-MM-DD
  section?: string;
  periodMonth?: string;
  createdBy: UserSummary;
  completedAt?: string;
  isTarget?: boolean;
  estimateTarget?: number;
  target?: number;
  reviewNote?: string;
  groupSubtasks?: GroupSubtask[];
  subtasks?: Subtask[];
  currentAssignment?: TaskAssignmentInfo;
  currentAssignments?: TaskAssignmentInfo[];
  createdAt: string;
  updatedAt: string;
}

export type StepStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE';

/** Optional group header for a task's subtasks (Scope.md §2.1/§2.2). */
export interface GroupSubtask {
  id: number;
  name: string;
  groupOrder: number;
}

/**
 * Leaf for a FAST task (ticked done manually — estimateMinutes/deadline/note
 * apply); parent-of-steps for a MULTI_STEP task (isTarget/estimateTarget/
 * target apply, status derives from its steps instead). groupSubtaskId is
 * undefined for an ungrouped subtask sitting directly under the task.
 */
export interface Subtask {
  id: number;
  groupSubtaskId?: number;
  subtaskOrder: number;
  title: string;
  status: StepStatus;
  estimateMinutes?: number;
  deadline?: string;
  note?: string;
  isTarget?: boolean;
  estimateTarget?: number;
  target?: number;
  steps: Step[];
  createdAt: string;
  updatedAt: string;
}

/** Leaf of a MULTI_STEP task's Subtask — has its own assignee/deadline. */
export interface Step {
  id: number;
  stepOrder: number;
  name: string;
  estimateMinutes?: number;
  deadline?: string;
  status: StepStatus;
  assignee?: UserSummary;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
}

/** Matches the real backend CreateTaskRequest. */
export interface CreateTaskRequest {
  taskTypeId: number;
  title: string;
  description?: string;
  estimateMinutes?: number;
  dueDate?: string;
  section?: string;
  periodMonth?: string;
}

/** Matches the real backend UpdateTaskRequest. */
export interface UpdateTaskRequest {
  title?: string;
  description?: string;
  estimateMinutes?: number;
  dueDate?: string;
  section?: string;
}

export interface UpdateTaskStatusRequest {
  status: TaskStatus;
}

/**
 * Backend's listTasks only supports status/search/dueDateFrom/dueDateTo —
 * see TaskService.searchTasks for how this maps to real query params.
 */
export interface TaskFilter {
  status?: TaskStatus;
  searchText?: string;
  deadlineBefore?: string;
  deadlineAfter?: string;
}

export interface TaskResponse {
  data: Task;
  success: boolean;
}

export interface TaskListResponse {
  data: Task[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  success: boolean;
}

export interface TaskComment {
  id: number;
  userId: number;
  userFullName: string;
  content: string;
  createdAt: string;
}

export interface TaskAttachment {
  id: number;
  uploadedById: number;
  uploadedByName: string;
  fileName: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
}

/** Mirrors the real backend KanbanTaskResponse (GET /tasks/kanban). */
export interface KanbanTask {
  id: number;
  title: string;
  timeCategory: TimeCategory;
  priority?: TaskPriority;
  status: TaskStatus;
  dueDate?: string;
  isOverdue: boolean;
  stepsDone: number;
  stepsTotal: number;
  commentCount: number;
  attachmentCount: number;
  assignee?: UserSummary;
}

export interface KanbanBoard {
  todo: KanbanTask[];
  doing: KanbanTask[];
  done: KanbanTask[];
}

export type KanbanColumn = 'todo' | 'doing' | 'done';

export type DeadlineExtensionStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED';

/** Mirrors the real backend DeadlineExtensionResponse (Scope.md §12). */
export interface DeadlineExtension {
  id: number;
  taskId: number;
  taskTitle: string;
  requestedBy: UserSummary;
  currentDeadline: string;
  requestedDeadline: string;
  reason: string;
  status: DeadlineExtensionStatus;
  reviewedBy?: UserSummary;
  reviewedAt?: string;
  reviewNote?: string;
  extensionNumber: number;
  createdAt: string;
  expiresAt: string;
}
