export type CategoryTier = 'SYSTEM' | 'CUSTOM';
export type FieldType = 'TEXT' | 'TEXTAREA' | 'NUMBER' | 'DATE' | 'DATETIME' | 'SELECT' | 'MULTISELECT' | 'CHECKBOX';
export type CategoryStatus = 'ACTIVE' | 'INACTIVE';

export interface Category {
  id: number;
  code: string; // Auto-generated from name, immutable
  name: string;
  description?: string;
  icon?: string;
  color?: string;
  tier: CategoryTier; // SYSTEM (Manager only) or CUSTOM (Lead-owned)
  ownerId?: number; // Null if SYSTEM, Lead ID if CUSTOM
  ownerName?: string;
  allowedTaskTypes: ('FAST' | 'OFTEN' | 'MULTI_STEP')[];
  extraFields: ExtraField[];
  status: CategoryStatus;
  taskCount: number; // Read-only, number of tasks using this category
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface ExtraField {
  id: number;
  label: string;
  fieldType: FieldType;
  required: boolean;
  placeholder?: string;
  defaultValue?: any;
  options?: string[]; // For SELECT/MULTISELECT types
  sortOrder: number;
  visibleInList: boolean; // Show as column in task list
  createdAt: string;
}

export interface CreateCategoryRequest {
  name: string;
  description?: string;
  icon?: string;
  color?: string;
  tier: CategoryTier;
  allowedTaskTypes: ('FAST' | 'OFTEN' | 'MULTI_STEP')[];
}

export interface UpdateCategoryRequest {
  name?: string;
  description?: string;
  icon?: string;
  color?: string;
  allowedTaskTypes?: ('FAST' | 'OFTEN' | 'MULTI_STEP')[];
  sortOrder?: number;
}

export interface AddExtraFieldRequest {
  label: string;
  fieldType: FieldType;
  required: boolean;
  placeholder?: string;
  defaultValue?: any;
  options?: string[]; // For SELECT/MULTISELECT
  sortOrder: number;
  visibleInList?: boolean;
}

export interface UpdateExtraFieldRequest {
  label?: string;
  fieldType?: FieldType; // Rejected server-side if the category already has tasks
  placeholder?: string;
  defaultValue?: any;
  options?: string[]; // For SELECT/MULTISELECT
  sortOrder?: number;
  visibleInList?: boolean;
  required?: boolean; // Only if not locked by existing data
}

export interface CategoryResponse {
  data: Category;
  success: boolean;
}

export interface CategoryListResponse {
  data: Category[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  success: boolean;
}

export interface CategoryFilter {
  tier?: CategoryTier;
  status?: CategoryStatus;
  ownerId?: number;
  searchText?: string;
  allowedTaskType?: 'FAST' | 'OFTEN' | 'MULTI_STEP';
}
