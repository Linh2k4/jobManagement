import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { tap } from 'rxjs/operators';

/**
 * Cache Interceptor
 * Implements 5-minute TTL caching for KPI endpoints
 * Uses browser's localStorage for persistence across requests
 */
export const cacheInterceptor: HttpInterceptorFn = (req, next) => {
  const cacheKey = `http-cache-${req.method}-${req.url}`;
  const cacheExpiryKey = `http-cache-expiry-${req.method}-${req.url}`;

  // Only cache GET requests
  if (req.method !== 'GET') {
    return next(req);
  }

  // Don't cache KPI invalidation requests
  if (req.url.includes('/invalidate-cache')) {
    return next(req);
  }

  // Check if cached response exists and is still valid
  try {
    const cached = localStorage.getItem(cacheKey);
    const expiry = localStorage.getItem(cacheExpiryKey);

    if (cached && expiry && new Date().getTime() < parseInt(expiry, 10)) {
      const cachedResponse = JSON.parse(cached);
      return new Promise(resolve => {
        setTimeout(() => {
          resolve(new HttpResponse({
            body: cachedResponse,
            status: 200,
            statusText: 'OK (cached)',
            url: req.url
          }));
        }, 0);
      }) as any;
    }
  } catch (e) {
    // Silently fail on cache retrieval errors
  }

  // Make the request and cache the response
  return next(req).pipe(
    tap(event => {
      if (event instanceof HttpResponse) {
        try {
          // Cache for 5 minutes (300,000 ms)
          localStorage.setItem(cacheKey, JSON.stringify(event.body));
          localStorage.setItem(
            cacheExpiryKey,
            (new Date().getTime() + 5 * 60 * 1000).toString()
          );
        } catch (e) {
          // Silently fail on cache storage errors (quota exceeded, etc)
        }
      }
    })
  );
};

/**
 * Clear KPI cache
 * Call this when KPI is invalidated due to task status changes
 */
export function clearKpiCache(): void {
  const keys = Object.keys(localStorage).filter(key => key.includes('/kpi'));
  keys.forEach(key => {
    localStorage.removeItem(key);
    localStorage.removeItem(key.replace('http-cache-', 'http-cache-expiry-'));
  });
}
