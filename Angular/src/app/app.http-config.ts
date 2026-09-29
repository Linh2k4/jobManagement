import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { cacheInterceptor } from './core/interceptors/cache.interceptor';
import { loadingInterceptor } from './core/interceptors/loading.interceptor';
import { loggingInterceptor } from './core/interceptors/logging.interceptor';

/**
 * HTTP Configuration for Angular Application
 * Includes all interceptors and CORS handling
 */
export const httpConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(
      withInterceptors([
        authInterceptor,
        cacheInterceptor,
        loadingInterceptor,
        loggingInterceptor,
        errorInterceptor
      ]),
      withXsrfConfiguration({
        cookieName: 'XSRF-TOKEN',
        headerName: 'X-XSRF-TOKEN'
      })
    )
  ]
};

/**
 * HTTP Interceptor Execution Order:
 *
 * Request Flow:
 * 1. authInterceptor - Adds JWT Bearer token to Authorization header
 * 2. cacheInterceptor - Caches GET requests for 5 minutes (dev) / 10 minutes (prod)
 * 3. loadingInterceptor - Increments activeRequests counter, shows loading indicator
 * 4. loggingInterceptor - Logs request/response in development mode
 * 5. errorInterceptor - Displays error messages to user
 *
 * Response Flow (reverse order):
 * 5. errorInterceptor - Catches HTTP errors, logs them
 * 4. loggingInterceptor - Logs successful responses
 * 3. loadingInterceptor - Decrements activeRequests counter, hides loading
 * 2. cacheInterceptor - Caches successful responses
 * 1. authInterceptor - No response handling
 *
 * Backend API Configuration:
 * - Development: http://localhost:8080/api (via proxy)
 * - Production: https://api.jobmanagement.com/api
 * - Configure in src/environments/environment.ts
 */
