export type RecurrencePattern = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY';

export interface Reminder {
  id: string; // UUID
  userId: number;
  title: string;
  message: string;
  remindAt: string; // ISO timestamp
  taskId?: number;
  isRecurring: boolean;
  recurrencePattern: RecurrencePattern;
  isDismissed: boolean;
  dismissedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateReminderRequest {
  title: string;
  message: string;
  remindAt: string; // ISO timestamp
  taskId?: number;
  isRecurring: boolean;
  recurrencePattern: RecurrencePattern;
}

export interface UpdateReminderRequest {
  title?: string;
  message?: string;
  remindAt?: string;
  taskId?: number;
  isRecurring?: boolean;
  recurrencePattern?: RecurrencePattern;
}

export interface DismissReminderRequest {
  isDismissed: boolean;
}

export interface ReminderResponse {
  data: Reminder;
  success: boolean;
}

export interface ReminderListResponse {
  data: Reminder[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  success: boolean;
}

export interface ReminderFilter {
  taskId?: number;
  isDismissed?: boolean;
  isRecurring?: boolean;
  remindAtBefore?: string;
  remindAtAfter?: string;
}

export interface ReminderEvent {
  reminderId: string;
  userId: number;
  title: string;
  message: string;
  taskId?: number;
  remindAt: string;
}
