import { Injectable, signal } from '@angular/core';

/**
 * Loading Service
 * Manages global loading state for HTTP requests
 * Used by LoadingInterceptor to show/hide loading spinner
 */
@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  private activeRequests = 0;
  public isLoading = signal<boolean>(false);

  show(): void {
    this.activeRequests++;
    this.isLoading.set(true);
  }

  hide(): void {
    this.activeRequests = Math.max(0, this.activeRequests - 1);
    if (this.activeRequests === 0) {
      this.isLoading.set(false);
    }
  }

  reset(): void {
    this.activeRequests = 0;
    this.isLoading.set(false);
  }
}
