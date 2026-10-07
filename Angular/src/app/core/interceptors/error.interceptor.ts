import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);

  return next(req).pipe(
    catchError(error => {
      // 401 Unauthorized is handled by auth flows/guards, skip toast
      if (error.status === 401) {
        return throwError(() => error);
      }

      let message = '';

      if (error.status === 0) {
        message = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.';
      } else if (error.error?.message && typeof error.error.message === 'string') {
        message = error.error.message;
      } else if (error.error?.error && typeof error.error.error === 'string') {
        message = error.error.error;
      } else if (error.status === 403) {
        message = 'Bạn không có quyền thực hiện thao tác này.';
      } else if (error.status === 404) {
        // Avoid noisy toast on background GET 404s
        if (req.method !== 'GET') {
          message = 'Không tìm thấy dữ liệu yêu cầu.';
        }
      } else if (error.status >= 500) {
        message = 'Hệ thống đang gặp sự cố. Vui lòng thử lại sau.';
      } else if (error.statusText && error.statusText !== 'OK') {
        message = error.statusText;
      }

      // Filter out invalid/spurious messages such as "OK"
      if (message && message.trim() !== 'OK') {
        snackBar.open(message, 'Đóng', {
          duration: 4000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
      }

      return throwError(() => error);
    })
  );
};
