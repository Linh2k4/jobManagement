import { createFeatureSelector, createSelector } from '@ngrx/store';
import { CategoryState } from './category.state';

export const selectCategoryState = createFeatureSelector<CategoryState>('category');

export const selectAllCategories = createSelector(
  selectCategoryState,
  (state: CategoryState) => state.categories
);

export const selectSystemCategories = createSelector(
  selectCategoryState,
  (state: CategoryState) => state.systemCategories
);

export const selectCustomCategories = createSelector(
  selectCategoryState,
  (state: CategoryState) => state.customCategories
);

export const selectSelectedCategory = createSelector(
  selectCategoryState,
  (state: CategoryState) => state.selectedCategory
);

export const selectCategoryLoading = createSelector(
  selectCategoryState,
  (state: CategoryState) => state.loading || state.creating || state.updating || state.deleting
);

export const selectCategoryError = createSelector(
  selectCategoryState,
  (state: CategoryState) => state.error
);

export const selectCategoryPagination = createSelector(
  selectCategoryState,
  (state: CategoryState) => ({
    page: state.page,
    size: state.pageSize,
    total: state.totalElements
  })
);
