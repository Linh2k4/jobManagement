import { Category } from '../../core/models';

export interface CategoryState {
  categories: Category[];
  selectedCategory: Category | null;
  systemCategories: Category[];
  customCategories: Category[];
  page: number;
  pageSize: number;
  totalElements: number;
  loading: boolean;
  loadingCategory: boolean;
  creating: boolean;
  updating: boolean;
  deleting: boolean;
  error: string | null;
}

export const initialCategoryState: CategoryState = {
  categories: [],
  selectedCategory: null,
  systemCategories: [],
  customCategories: [],
  page: 0,
  pageSize: 20,
  totalElements: 0,
  loading: false,
  loadingCategory: false,
  creating: false,
  updating: false,
  deleting: false,
  error: null
};
