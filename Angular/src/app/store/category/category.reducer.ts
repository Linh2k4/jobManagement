import { createReducer, on } from '@ngrx/store';
import * as CategoryActions from './category.actions';
import { initialCategoryState, CategoryState } from './category.state';

export const categoryReducer = createReducer(
  initialCategoryState,

  on(CategoryActions.loadCategories, state => ({
    ...state,
    loading: true,
    error: null
  })),

  on(CategoryActions.loadCategoriesSuccess, (state, { categories, total, page, size }) => ({
    ...state,
    categories,
    systemCategories: categories.filter(c => c.tier === 'SYSTEM'),
    customCategories: categories.filter(c => c.tier === 'CUSTOM'),
    totalElements: total,
    page,
    pageSize: size,
    loading: false
  })),

  on(CategoryActions.loadCategoriesFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),

  on(CategoryActions.loadCategory, state => ({
    ...state,
    loadingCategory: true,
    error: null
  })),

  on(CategoryActions.loadCategorySuccess, (state, { category }) => ({
    ...state,
    categories: state.categories.some(c => c.id === category.id)
      ? state.categories.map(c => c.id === category.id ? category : c)
      : [...state.categories, category],
    selectedCategory: category,
    loadingCategory: false
  })),

  on(CategoryActions.selectCategory, (state, { id }) => ({
    ...state,
    selectedCategory: state.categories.find(c => c.id === id) || null
  })),

  on(CategoryActions.createCategory, state => ({
    ...state,
    creating: true,
    error: null
  })),

  on(CategoryActions.createCategorySuccess, (state, { category }) => ({
    ...state,
    categories: [...state.categories, category],
    creating: false,
    error: null,
    totalElements: state.totalElements + 1
  })),

  on(CategoryActions.createCategoryFailure, (state, { error }) => ({
    ...state,
    creating: false,
    error
  })),

  on(CategoryActions.updateCategory, state => ({
    ...state,
    updating: true,
    error: null
  })),

  on(CategoryActions.updateCategorySuccess, (state, { category }) => ({
    ...state,
    categories: state.categories.map(c => c.id === category.id ? category : c),
    selectedCategory: state.selectedCategory?.id === category.id ? category : state.selectedCategory,
    updating: false
  })),

  on(CategoryActions.updateCategoryFailure, (state, { error }) => ({
    ...state,
    updating: false,
    error
  })),

  on(CategoryActions.deleteCategory, state => ({
    ...state,
    deleting: true,
    error: null
  })),

  on(CategoryActions.deleteCategorySuccess, (state, { id }) => ({
    ...state,
    categories: state.categories.filter(c => c.id !== id),
    selectedCategory: state.selectedCategory?.id === id ? null : state.selectedCategory,
    deleting: false,
    totalElements: state.totalElements - 1
  })),

  on(CategoryActions.deleteCategoryFailure, (state, { error }) => ({
    ...state,
    deleting: false,
    error
  })),

  on(CategoryActions.clearCategoryError, state => ({
    ...state,
    error: null
  }))
);
