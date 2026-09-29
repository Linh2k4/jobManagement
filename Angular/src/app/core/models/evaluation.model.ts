export type EvaluationStatus = 'DRAFT' | 'SELF_SUBMITTED' | 'LEAD_REVIEWED' | 'FINALIZED';
export type EvaluationCriterion = 'QUALITY' | 'RESPONSIBILITY' | 'TEAMWORK' | 'INITIATIVE' | 'DISCIPLINE';

export interface Evaluation {
  id: number;
  userId: number;
  userFullName: string;
  leadId?: number;
  leadFullName?: string;
  managerId: number;
  managerFullName: string;
  periodMonth: string; // YYYY-MM format
  status: EvaluationStatus;
  isLocked: boolean;
  groupId?: number; // Scope.md §13 — nullable for pre-§13 single-evaluation case
  groupName?: string; // Group this evaluation row is scoped to

  // Self-evaluation scores (1-10 scale, null if not submitted)
  selfQuality?: number;
  selfResponsibility?: number;
  selfTeamwork?: number;
  selfInitiative?: number;
  selfDiscipline?: number;
  selfNotes?: string;

  // Lead evaluation scores (1-10 scale, null if not submitted)
  leadQuality?: number;
  leadResponsibility?: number;
  leadTeamwork?: number;
  leadDiscipline?: number;
  leadNotes?: string;
  leadScore?: number; // Calculated average

  // Manager evaluation scores (1-10 scale, null if not submitted)
  managerQuality?: number;
  managerResponsibility?: number;
  managerInitiative?: number;
  managerDiscipline?: number;
  managerNotes?: string;
  managerScore?: number; // Calculated average

  // Final KPI
  kpiFinal?: number; // 0-100: (autoScore×40% + leadScore×35% + managerScore×25%)
  ranking?: 'Xuất sắc' | 'Tốt' | 'Đạt' | 'Cần cải thiện' | 'Không đạt';

  // Timestamps
  createdAt: string;
  updatedAt: string;
  selfSubmittedAt?: string;
  leadSubmittedAt?: string;
  managerFinalizedAt?: string;
}

export interface EvaluationPeriod {
  id: number;
  periodMonth: string; // YYYY-MM format
  status: 'DRAFT' | 'OPEN' | 'SUBMITTED' | 'REVIEWED' | 'FINALIZED';
  openDate: string; // ISO date
  memberDeadline: string;
  leadDeadline: string;
  managerDeadline: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface EvaluationScoresRequest {
  quality: number; // 1-10
  responsibility: number; // 1-10
  teamwork?: number; // 1-10, optional for manager
  initiative?: number; // 1-10, optional for lead
  discipline: number; // 1-10
  notes?: string;
}

export interface SubmitSelfReviewRequest extends EvaluationScoresRequest {
  // Extends base scores + notes
}

export interface SubmitLeadReviewRequest extends EvaluationScoresRequest {
  // Extends base scores + notes
}

export interface FinalizeEvaluationRequest extends EvaluationScoresRequest {
  // Extends base scores + notes
}

export interface EvaluationResponse {
  data: Evaluation;
  success: boolean;
}

/**
 * GET /evaluations/{my,team,all} returns ApiResponse<Page<Evaluation>> —
 * `data` is a Spring Data Page, not a flat array.
 */
export interface EvaluationListResponse {
  data: {
    content: Evaluation[];
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
  success: boolean;
}

export interface EvaluationFilter {
  periodMonth?: string;
  status?: EvaluationStatus;
  userId?: number;
  leadId?: number;
  managerId?: number;
  isLocked?: boolean;
}

// State machine guards
export const EvaluationStateGuards = {
  canSubmitSelfReview: (evaluation: Evaluation): boolean =>
    evaluation.status === 'DRAFT' && !evaluation.isLocked,

  canSubmitLeadReview: (evaluation: Evaluation): boolean =>
    evaluation.status === 'SELF_SUBMITTED' && evaluation.selfSubmittedAt != null && !evaluation.isLocked,

  canFinalize: (evaluation: Evaluation): boolean =>
    evaluation.status === 'LEAD_REVIEWED' && evaluation.leadSubmittedAt != null && !evaluation.isLocked,

  canEdit: (evaluation: Evaluation): boolean =>
    !evaluation.isLocked,

  canViewLeadReview: (evaluation: Evaluation): boolean =>
    evaluation.status === 'SELF_SUBMITTED' || evaluation.status === 'LEAD_REVIEWED' || evaluation.status === 'FINALIZED'
};
