import { UserSummary } from './task.model';

export type LeaveType = 'SICK' | 'ANNUAL' | 'UNPAID' | 'MATERNITY' | 'PATERNITY' | 'BEREAVEMENT' | 'OTHER';
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type HandoverStatus = 'NONE' | 'PARTIAL' | 'COMPLETE';
export type HandoverEntityType = 'FAST_TASK' | 'STEP';
export type HandoverAction = 'REASSIGN' | 'EXTEND_DEADLINE' | 'SKIP';

/** Mirrors the real backend LeaveRequestResponse (Scope.md §14). */
export interface LeaveRequest {
  id: number;
  member: UserSummary;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  reason: string;
  status: LeaveStatus;
  reviewedBy?: UserSummary;
  reviewedAt?: string;
  reviewNote?: string;
  handoverStatus: HandoverStatus;
  createdAt: string;
}

/** Mirrors the real backend HandoverSuggestionResponse. */
export interface HandoverSuggestion {
  id: number;
  taskId: number;
  taskTitle: string;
  stepId?: number;
  stepName?: string;
  entityType: HandoverEntityType;
  currentDeadline?: string;
  suggestedAssignee?: UserSummary;
  alternativeAssigneeIds: number[];
  action: HandoverAction;
  confirmed: boolean;
  confirmedAssignee?: UserSummary;
  confirmedAt?: string;
}
