import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);

  return next(req).pipe(
    catchError(error => {
      let message = 'An error occurred';

      if (error.status === 0) {
        message = 'Network error - unable to reach server';
      } else if (error.error?.message) {
        message = error.error.message;
      } else if (error.statusText) {
        message = error.statusText;
      }

      snackBar.open(message, 'Close', { duration: 5000 });

      return throwError(() => error);
    })
  );
};
