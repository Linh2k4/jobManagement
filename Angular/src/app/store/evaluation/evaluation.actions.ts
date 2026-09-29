import { createAction, props } from '@ngrx/store';
import { Evaluation, EvaluationPeriod, SubmitSelfReviewRequest, SubmitLeadReviewRequest, FinalizeEvaluationRequest } from '../../core/models';

// Load evaluations
export const loadEvaluations = createAction(
  '[Evaluation] Load Evaluations',
  props<{ page?: number; size?: number }>()
);

export const loadEvaluationsSuccess = createAction(
  '[Evaluation] Load Evaluations Success',
  props<{ evaluations: Evaluation[]; totalElements: number; totalPages: number; page: number; size: number }>()
);

export const loadEvaluationsFailure = createAction(
  '[Evaluation] Load Evaluations Failure',
  props<{ error: string }>()
);

// Load single evaluation
export const loadEvaluation = createAction(
  '[Evaluation] Load Evaluation',
  props<{ id: number }>()
);

export const loadEvaluationSuccess = createAction(
  '[Evaluation] Load Evaluation Success',
  props<{ evaluation: Evaluation }>()
);

export const selectEvaluation = createAction(
  '[Evaluation] Select Evaluation',
  props<{ id: number }>()
);

// Evaluation periods
export const loadEvaluationPeriods = createAction(
  '[Evaluation] Load Evaluation Periods'
);

export const loadEvaluationPeriodsSuccess = createAction(
  '[Evaluation] Load Evaluation Periods Success',
  props<{ periods: EvaluationPeriod[]; currentPeriod?: EvaluationPeriod }>()
);

// Workflow actions
export const submitSelfReview = createAction(
  '[Evaluation] Submit Self Review',
  props<{ evaluationId: number; request: SubmitSelfReviewRequest }>()
);

export const submitSelfReviewSuccess = createAction(
  '[Evaluation] Submit Self Review Success',
  props<{ evaluation: Evaluation }>()
);

export const submitSelfReviewFailure = createAction(
  '[Evaluation] Submit Self Review Failure',
  props<{ error: string }>()
);

export const submitLeadReview = createAction(
  '[Evaluation] Submit Lead Review',
  props<{ evaluationId: number; request: SubmitLeadReviewRequest }>()
);

export const submitLeadReviewSuccess = createAction(
  '[Evaluation] Submit Lead Review Success',
  props<{ evaluation: Evaluation }>()
);

export const submitLeadReviewFailure = createAction(
  '[Evaluation] Submit Lead Review Failure',
  props<{ error: string }>()
);

export const finalizeEvaluation = createAction(
  '[Evaluation] Finalize Evaluation',
  props<{ evaluationId: number; request: FinalizeEvaluationRequest }>()
);

export const finalizeEvaluationSuccess = createAction(
  '[Evaluation] Finalize Evaluation Success',
  props<{ evaluation: Evaluation }>()
);

export const finalizeEvaluationFailure = createAction(
  '[Evaluation] Finalize Evaluation Failure',
  props<{ error: string }>()
);

// Utility
export const clearEvaluationError = createAction(
  '[Evaluation] Clear Error'
);
