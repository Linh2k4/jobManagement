export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
  requestId?: string;
  errors?: Record<string, string>;
  status?: number;
}

export interface PagedResponse<T> {
  content: T[];
  pagination: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface ErrorResponse {
  status: number;
  message: string;
  path: string;
  errors?: Record<string, string>;
  timestamp?: string;
}
