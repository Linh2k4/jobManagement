import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, mergeMap, catchError, switchMap } from 'rxjs/operators';
import { CategoryService } from '../../core/services/api/category.service';
import * as CategoryActions from './category.actions';

@Injectable()
export class CategoryEffects {
  private actions$ = inject(Actions);
  private categoryService = inject(CategoryService);

  loadCategories$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CategoryActions.loadCategories),
      switchMap(({ page = 0, size = 20 }) =>
        // GET /categories is not paginated server-side (bounded list, Scope
        // §2.4 caps custom categories at 20/lead) — derive paging state from
        // the returned array instead of nonexistent response fields.
        this.categoryService.getCategories(page, size).pipe(
          map(response => CategoryActions.loadCategoriesSuccess({
            categories: response.data,
            total: response.data.length,
            page: 0,
            size: response.data.length
          })),
          catchError(error => of(CategoryActions.loadCategoriesFailure({
            error: error.message || 'Failed to load categories'
          })))
        )
      )
    )
  );

  loadCategory$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CategoryActions.loadCategory),
      mergeMap(({ id }) =>
        this.categoryService.getCategory(id).pipe(
          map(response => CategoryActions.loadCategorySuccess({
            category: response.data
          })),
          catchError(error => of(CategoryActions.loadCategoriesFailure({
            error: error.message || 'Failed to load category'
          })))
        )
      )
    )
  );

  createCategory$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CategoryActions.createCategory),
      mergeMap(({ request }) =>
        this.categoryService.createCategory(request).pipe(
          map(response => CategoryActions.createCategorySuccess({
            category: response.data
          })),
          catchError(error => of(CategoryActions.createCategoryFailure({
            error: error.message || 'Failed to create category'
          })))
        )
      )
    )
  );

  updateCategory$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CategoryActions.updateCategory),
      mergeMap(({ id, request }) =>
        this.categoryService.updateCategory(id, request).pipe(
          map(response => CategoryActions.updateCategorySuccess({
            category: response.data
          })),
          catchError(error => of(CategoryActions.updateCategoryFailure({
            error: error.message || 'Failed to update category'
          })))
        )
      )
    )
  );

  deleteCategory$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CategoryActions.deleteCategory),
      mergeMap(({ id }) =>
        this.categoryService.deleteCategory(id).pipe(
          map(() => CategoryActions.deleteCategorySuccess({ id })),
          catchError(error => of(CategoryActions.deleteCategoryFailure({
            error: error.message || 'Failed to delete category'
          })))
        )
      )
    )
  );

}
