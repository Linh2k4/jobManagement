import { createReducer, on } from '@ngrx/store';
import * as EvaluationActions from './evaluation.actions';
import { initialEvaluationState, EvaluationState } from './evaluation.state';

export const evaluationReducer = createReducer(
  initialEvaluationState,

  on(EvaluationActions.loadEvaluations, state => ({
    ...state,
    loadingList: true,
    error: null
  })),

  on(EvaluationActions.loadEvaluationsSuccess, (state, { evaluations, totalElements, totalPages, page, size }) => ({
    ...state,
    evaluations,
    totalElements,
    totalPages,
    page,
    pageSize: size,
    loadingList: false,
    error: null
  })),

  on(EvaluationActions.loadEvaluationsFailure, (state, { error }) => ({
    ...state,
    loadingList: false,
    error,
    errorType: 'FETCH' as const
  })),

  on(EvaluationActions.loadEvaluation, state => ({
    ...state,
    loadingEvaluation: true,
    error: null
  })),

  on(EvaluationActions.loadEvaluationSuccess, (state, { evaluation }) => ({
    ...state,
    evaluations: state.evaluations.some(e => e.id === evaluation.id)
      ? state.evaluations.map(e => e.id === evaluation.id ? evaluation : e)
      : [...state.evaluations, evaluation],
    selectedEvaluation: evaluation,
    loadingEvaluation: false,
    error: null
  })),

  on(EvaluationActions.selectEvaluation, (state, { id }) => ({
    ...state,
    selectedEvaluationId: id,
    selectedEvaluation: state.evaluations.find(e => e.id === id) || null
  })),

  on(EvaluationActions.loadEvaluationPeriods, state => ({
    ...state,
    loadingPeriods: true
  })),

  on(EvaluationActions.loadEvaluationPeriodsSuccess, (state, { periods, currentPeriod }) => ({
    ...state,
    periods,
    currentPeriod: currentPeriod || periods[0] || null,
    loadingPeriods: false
  })),

  on(EvaluationActions.submitSelfReview, state => ({
    ...state,
    submittingReview: true,
    workflowInProgress: true,
    error: null
  })),

  on(EvaluationActions.submitSelfReviewSuccess, (state, { evaluation }) => ({
    ...state,
    evaluations: state.evaluations.map(e => e.id === evaluation.id ? evaluation : e),
    selectedEvaluation: state.selectedEvaluation?.id === evaluation.id ? evaluation : state.selectedEvaluation,
    submittingReview: false,
    workflowInProgress: false,
    lastAction: 'SELF_REVIEW' as const,
    error: null
  })),

  on(EvaluationActions.submitSelfReviewFailure, (state, { error }) => ({
    ...state,
    submittingReview: false,
    workflowInProgress: false,
    error,
    errorType: 'SUBMIT' as const
  })),

  on(EvaluationActions.submitLeadReview, state => ({
    ...state,
    submittingReview: true,
    workflowInProgress: true,
    error: null
  })),

  on(EvaluationActions.submitLeadReviewSuccess, (state, { evaluation }) => ({
    ...state,
    evaluations: state.evaluations.map(e => e.id === evaluation.id ? evaluation : e),
    selectedEvaluation: state.selectedEvaluation?.id === evaluation.id ? evaluation : state.selectedEvaluation,
    submittingReview: false,
    workflowInProgress: false,
    lastAction: 'LEAD_REVIEW' as const,
    error: null
  })),

  on(EvaluationActions.submitLeadReviewFailure, (state, { error }) => ({
    ...state,
    submittingReview: false,
    workflowInProgress: false,
    error,
    errorType: 'SUBMIT' as const
  })),

  on(EvaluationActions.finalizeEvaluation, state => ({
    ...state,
    finalizingEvaluation: true,
    workflowInProgress: true,
    error: null
  })),

  on(EvaluationActions.finalizeEvaluationSuccess, (state, { evaluation }) => ({
    ...state,
    evaluations: state.evaluations.map(e => e.id === evaluation.id ? evaluation : e),
    selectedEvaluation: state.selectedEvaluation?.id === evaluation.id ? evaluation : state.selectedEvaluation,
    finalizingEvaluation: false,
    workflowInProgress: false,
    lastAction: 'FINALIZE' as const,
    error: null
  })),

  on(EvaluationActions.finalizeEvaluationFailure, (state, { error }) => ({
    ...state,
    finalizingEvaluation: false,
    workflowInProgress: false,
    error,
    errorType: 'FINALIZE' as const
  })),

  on(EvaluationActions.clearEvaluationError, state => ({
    ...state,
    error: null,
    errorType: null
  }))
);
