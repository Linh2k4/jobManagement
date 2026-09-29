import { createAction, props } from '@ngrx/store';
import { Category, CreateCategoryRequest, UpdateCategoryRequest } from '../../core/models';

export const loadCategories = createAction('[Category] Load Categories', props<{ page?: number; size?: number }>());
export const loadCategoriesSuccess = createAction('[Category] Load Categories Success', props<{ categories: Category[]; total: number; page: number; size: number }>());
export const loadCategoriesFailure = createAction('[Category] Load Categories Failure', props<{ error: string }>());

export const loadCategory = createAction('[Category] Load Category', props<{ id: number }>());
export const loadCategorySuccess = createAction('[Category] Load Category Success', props<{ category: Category }>());
export const selectCategory = createAction('[Category] Select Category', props<{ id: number }>());

export const createCategory = createAction('[Category] Create Category', props<{ request: CreateCategoryRequest }>());
export const createCategorySuccess = createAction('[Category] Create Category Success', props<{ category: Category }>());
export const createCategoryFailure = createAction('[Category] Create Category Failure', props<{ error: string }>());

export const updateCategory = createAction('[Category] Update Category', props<{ id: number; request: UpdateCategoryRequest }>());
export const updateCategorySuccess = createAction('[Category] Update Category Success', props<{ category: Category }>());
export const updateCategoryFailure = createAction('[Category] Update Category Failure', props<{ error: string }>());

export const deleteCategory = createAction('[Category] Delete Category', props<{ id: number }>());
export const deleteCategorySuccess = createAction('[Category] Delete Category Success', props<{ id: number }>());
export const deleteCategoryFailure = createAction('[Category] Delete Category Failure', props<{ error: string }>());

export const clearCategoryError = createAction('[Category] Clear Error');
