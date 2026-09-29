import { Evaluation, EvaluationPeriod } from '../../core/models';

export interface EvaluationState {
  // Evaluation data
  evaluations: Evaluation[];
  selectedEvaluation: Evaluation | null;
  selectedEvaluationId: number | null;

  // Evaluation periods
  periods: EvaluationPeriod[];
  currentPeriod: EvaluationPeriod | null;

  // Pagination
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;

  // Loading states
  loading: boolean;
  loadingEvaluation: boolean;
  loadingList: boolean;
  loadingPeriods: boolean;
  submittingReview: boolean;
  finalizingEvaluation: boolean;

  // Error handling
  error: string | null;
  errorType: 'FETCH' | 'SUBMIT' | 'FINALIZE' | null;

  // Workflow state
  workflowInProgress: boolean;
  lastAction: 'SELF_REVIEW' | 'LEAD_REVIEW' | 'FINALIZE' | null;
}

export const initialEvaluationState: EvaluationState = {
  evaluations: [],
  selectedEvaluation: null,
  selectedEvaluationId: null,
  periods: [],
  currentPeriod: null,
  page: 0,
  pageSize: 20,
  totalElements: 0,
  totalPages: 0,
  loading: false,
  loadingEvaluation: false,
  loadingList: false,
  loadingPeriods: false,
  submittingReview: false,
  finalizingEvaluation: false,
  error: null,
  errorType: null,
  workflowInProgress: false,
  lastAction: null
};
