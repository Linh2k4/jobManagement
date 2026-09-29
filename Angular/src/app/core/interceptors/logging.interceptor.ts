import { HttpInterceptorFn } from '@angular/common/http';
import { HttpResponse } from '@angular/common/http';
import { tap } from 'rxjs/operators';

/**
 * Logging Interceptor
 * Logs all HTTP requests and responses in development mode
 * Helps with debugging API integration issues
 */
export const loggingInterceptor: HttpInterceptorFn = (req, next) => {
  const isDev = !this || typeof window !== 'undefined'; // Check if in browser

  if (isDev) {
    const startTime = new Date().getTime();

    return next(req).pipe(
      tap(
        (event) => {
          if (event instanceof HttpResponse) {
            const elapsedTime = new Date().getTime() - startTime;
            console.log(
              `%c[HTTP ${req.method}] ${req.url} - ${event.status} ${event.statusText} (${elapsedTime}ms)`,
              'color: green; font-weight: bold'
            );

            // Log response body in development for debugging
            if (req.method !== 'GET') {
              console.log('%cRequest body:', 'color: blue', req.body);
              console.log('%cResponse body:', 'color: blue', event.body);
            }
          }
        },
        (error) => {
          const elapsedTime = new Date().getTime() - startTime;
          console.error(
            `%c[HTTP ERROR] ${req.method} ${req.url} - ${error.status} ${error.statusText} (${elapsedTime}ms)`,
            'color: red; font-weight: bold'
          );
          console.error('%cError details:', 'color: red', error);
          console.error('%cRequest body:', 'color: red', req.body);
        }
      )
    );
  }

  return next(req);
};
