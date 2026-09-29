import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, mergeMap, catchError, switchMap, tap } from 'rxjs/operators';
import { EvaluationService } from '../../core/services/api/evaluation.service';
import * as EvaluationActions from './evaluation.actions';

@Injectable()
export class EvaluationEffects {
  private actions$ = inject(Actions);
  private evaluationService = inject(EvaluationService);

  loadEvaluations$ = createEffect(() =>
    this.actions$.pipe(
      ofType(EvaluationActions.loadEvaluations),
      switchMap(({ page = 0, size = 20 }) =>
        // response.data is a Spring Data Page (content/number/size/totalPages),
        // not a flat array — the EvaluationListResponse model's top-level
        // data/currentPage/pageSize fields don't match what the backend sends.
        this.evaluationService.getMyEvaluations(page, size).pipe(
          map(response => EvaluationActions.loadEvaluationsSuccess({
            evaluations: response.data.content,
            totalElements: response.data.totalElements,
            totalPages: response.data.totalPages,
            page: response.data.number,
            size: response.data.size
          })),
          catchError(error => of(EvaluationActions.loadEvaluationsFailure({
            error: error.message || 'Failed to load evaluations'
          })))
        )
      )
    )
  );

  loadEvaluation$ = createEffect(() =>
    this.actions$.pipe(
      ofType(EvaluationActions.loadEvaluation),
      mergeMap(({ id }) =>
        this.evaluationService.getEvaluation(id).pipe(
          map(response => EvaluationActions.loadEvaluationSuccess({
            evaluation: response.data
          })),
          catchError(error => of(EvaluationActions.loadEvaluationsFailure({
            error: error.message || 'Failed to load evaluation'
          })))
        )
      )
    )
  );

  loadEvaluationPeriods$ = createEffect(() =>
    this.actions$.pipe(
      ofType(EvaluationActions.loadEvaluationPeriods),
      switchMap(() =>
        this.evaluationService.getCurrentEvaluationPeriod().pipe(
          map(response => EvaluationActions.loadEvaluationPeriodsSuccess({
            periods: [response.data],
            currentPeriod: response.data
          })),
          catchError(() => of(EvaluationActions.loadEvaluationPeriodsSuccess({
            periods: [],
            currentPeriod: undefined
          })))
        )
      )
    )
  );

  submitSelfReview$ = createEffect(() =>
    this.actions$.pipe(
      ofType(EvaluationActions.submitSelfReview),
      mergeMap(({ evaluationId, request }) =>
        this.evaluationService.submitSelfReview(evaluationId, request).pipe(
          map(response => EvaluationActions.submitSelfReviewSuccess({
            evaluation: response.data
          })),
          catchError(error => of(EvaluationActions.submitSelfReviewFailure({
            error: error.message || 'Failed to submit self review'
          })))
        )
      )
    )
  );

  submitLeadReview$ = createEffect(() =>
    this.actions$.pipe(
      ofType(EvaluationActions.submitLeadReview),
      mergeMap(({ evaluationId, request }) =>
        this.evaluationService.submitLeadReview(evaluationId, request).pipe(
          map(response => EvaluationActions.submitLeadReviewSuccess({
            evaluation: response.data
          })),
          catchError(error => of(EvaluationActions.submitLeadReviewFailure({
            error: error.message || 'Failed to submit lead review'
          })))
        )
      )
    )
  );

  finalizeEvaluation$ = createEffect(() =>
    this.actions$.pipe(
      ofType(EvaluationActions.finalizeEvaluation),
      mergeMap(({ evaluationId, request }) =>
        this.evaluationService.finalizeEvaluation(evaluationId, request).pipe(
          map(response => EvaluationActions.finalizeEvaluationSuccess({
            evaluation: response.data
          })),
          catchError(error => of(EvaluationActions.finalizeEvaluationFailure({
            error: error.message || 'Failed to finalize evaluation'
          })))
        )
      )
    )
  );

}
