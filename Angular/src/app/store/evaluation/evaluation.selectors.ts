import { createFeatureSelector, createSelector } from '@ngrx/store';
import { EvaluationState } from './evaluation.state';
import { EvaluationStateGuards } from '../../core/models';

export const selectEvaluationState = createFeatureSelector<EvaluationState>('evaluation');

export const selectAllEvaluations = createSelector(
  selectEvaluationState,
  (state: EvaluationState) => state.evaluations
);

export const selectSelectedEvaluation = createSelector(
  selectEvaluationState,
  (state: EvaluationState) => state.selectedEvaluation
);

export const selectCurrentPeriod = createSelector(
  selectEvaluationState,
  (state: EvaluationState) => state.currentPeriod
);

export const selectEvaluationLoading = createSelector(
  selectEvaluationState,
  (state: EvaluationState) => state.loadingList || state.loadingEvaluation || state.submittingReview || state.finalizingEvaluation
);

export const selectEvaluationError = createSelector(
  selectEvaluationState,
  (state: EvaluationState) => state.error
);

export const selectWorkflowInProgress = createSelector(
  selectEvaluationState,
  (state: EvaluationState) => state.workflowInProgress
);

// Workflow guards
export const selectCanSubmitSelfReview = createSelector(
  selectSelectedEvaluation,
  (evaluation) => evaluation ? EvaluationStateGuards.canSubmitSelfReview(evaluation) : false
);

export const selectCanSubmitLeadReview = createSelector(
  selectSelectedEvaluation,
  (evaluation) => evaluation ? EvaluationStateGuards.canSubmitLeadReview(evaluation) : false
);

export const selectCanFinalize = createSelector(
  selectSelectedEvaluation,
  (evaluation) => evaluation ? EvaluationStateGuards.canFinalize(evaluation) : false
);

export const selectCanEdit = createSelector(
  selectSelectedEvaluation,
  (evaluation) => evaluation ? EvaluationStateGuards.canEdit(evaluation) : false
);
