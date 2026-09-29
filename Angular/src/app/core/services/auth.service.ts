import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { AuthStore } from '../stores/auth.store';
import { ApiService, ApiResponse } from './api.service';
import { User } from '../models';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  // Backend follows OAuth2 (RFC 6749) token response naming for this endpoint specifically
  access_token: string;
  refresh_token?: string;
  token_type?: string;
  expires_in?: number;
  user: User;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(
    private apiService: ApiService,
    private authStore: AuthStore
  ) {}

  login(email: string, password: string): Observable<ApiResponse<AuthResponse>> {
    return this.apiService.post<AuthResponse>('auth/login', { email, password })
      .pipe(
        tap(response => {
          if (response.data) {
            this.authStore.setUser(response.data.user);
            this.authStore.setToken(response.data.access_token);
            this.authStore.setRefreshToken(response.data.refresh_token ?? null);
          }
        })
      );
  }

  register(data: any): Observable<ApiResponse<AuthResponse>> {
    return this.apiService.post<AuthResponse>('auth/register', data);
  }

  logout(): void {
    this.authStore.clear();
  }

  refreshToken(): Observable<ApiResponse<AuthResponse>> {
    return this.apiService.post<AuthResponse>('auth/refresh', {
      refreshToken: this.authStore.refreshTokenValue()
    })
      .pipe(
        tap(response => {
          if (response.data) {
            this.authStore.setToken(response.data.access_token);
            this.authStore.setRefreshToken(response.data.refresh_token ?? null);
          }
        })
      );
  }
}
