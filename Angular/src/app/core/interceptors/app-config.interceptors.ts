/**
 * HTTP Interceptor Configuration
 * Add these interceptors to your app.config.ts in the correct order
 */

import { HttpInterceptorFn } from '@angular/common/http';
import { authInterceptor } from './auth.interceptor';
import { errorInterceptor } from './error.interceptor';
import { cacheInterceptor } from './cache.interceptor';
import { loadingInterceptor } from './loading.interceptor';
import { loggingInterceptor } from './logging.interceptor';

/**
 * Ordered list of HTTP interceptors
 * Order matters! Interceptors execute in the order provided
 *
 * 1. authInterceptor - Adds JWT token to all requests
 * 2. cacheInterceptor - Caches GET requests for 5 minutes
 * 3. loadingInterceptor - Shows/hides loading spinner
 * 4. loggingInterceptor - Logs requests/responses in development
 * 5. errorInterceptor - Displays error messages to user
 */
export const httpInterceptors: HttpInterceptorFn[] = [
  authInterceptor,
  cacheInterceptor,
  loadingInterceptor,
  loggingInterceptor,
  errorInterceptor
];

/**
 * Usage in app.config.ts:
 *
 * import { provideHttpClient, withInterceptors } from '@angular/common/http';
 * import { httpInterceptors } from '@core/interceptors/app-config.interceptors';
 *
 * export const appConfig: ApplicationConfig = {
 *   providers: [
 *     provideHttpClient(
 *       withInterceptors(httpInterceptors)
 *     ),
 *     // ... other providers
 *   ]
 * };
 */
