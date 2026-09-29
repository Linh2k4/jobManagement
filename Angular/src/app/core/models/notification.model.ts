export type NotificationType =
  | 'REMINDER'
  | 'TASK_ASSIGNED'
  | 'TASK_DUE'
  | 'SYSTEM';

export interface Notification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  relatedTaskId?: number;
  remindAt?: string;
  read: boolean;
  createdAt: string;
}

export interface NotificationResponse {
  data: Notification;
  success: boolean;
}

export interface NotificationListResponse {
  data: Notification[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  success: boolean;
}

export interface NotificationPreferences {
  userId: number;
  taskAssigned: boolean;
  taskDeadline: boolean;
  taskDeadlineUrgent: boolean;
  taskOverdue: boolean;
  stepReady: boolean;
  evaluationDeadline: boolean;
  contractExpiry: boolean;
  taskComment: boolean;
  email: boolean;
  inApp: boolean;
  quietHoursStart?: string; // HH:mm format
  quietHoursEnd?: string; // HH:mm format
  createdAt: string;
  updatedAt: string;
}

export interface UpdateNotificationPreferencesRequest {
  taskAssigned?: boolean;
  taskDeadline?: boolean;
  taskDeadlineUrgent?: boolean;
  taskOverdue?: boolean;
  stepReady?: boolean;
  evaluationDeadline?: boolean;
  contractExpiry?: boolean;
  taskComment?: boolean;
  email?: boolean;
  inApp?: boolean;
  quietHoursStart?: string;
  quietHoursEnd?: string;
}

export interface MarkNotificationAsReadRequest {
  read: boolean;
}

export interface NotificationFilter {
  type?: NotificationType;
  read?: boolean;
  taskId?: number;
  createdAfter?: string;
  createdBefore?: string;
}

export interface NotificationEvent {
  type: NotificationType;
  userId: number;
  title: string;
  message: string;
  taskId?: number;
  stepId?: number;
  evaluationId?: number;
  actionUrl?: string;
}
