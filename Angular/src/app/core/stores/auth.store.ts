import { Injectable } from '@angular/core';
import { signal, computed } from '@angular/core';
import { User, Role } from '../models';
import { environment } from '../../../environments/environment';

const USER_KEY = 'auth_user';

function readStoredUser(): User | null {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) as User : null;
  } catch {
    return null;
  }
}

/**
 * Auth state as Angular signals. Token/user are persisted to localStorage so a
 * page reload (or a fresh tab on a deep link) doesn't silently log the user out —
 * only the in-memory signals are the source of truth at runtime, localStorage is
 * just how they survive a reload.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthStore {
  readonly user = signal<User | null>(readStoredUser());
  readonly token = signal<string | null>(localStorage.getItem(environment.tokenKey));
  readonly refreshTokenValue = signal<string | null>(localStorage.getItem(environment.refreshTokenKey));

  readonly isAuthenticated = computed(() => this.user() !== null && this.token() !== null);

  readonly isManager = computed(() => this.user()?.role === Role.MANAGER);

  readonly isLead = computed(() => this.user()?.role === Role.LEAD);

  readonly isMember = computed(() => this.user()?.role === Role.MEMBER);

  setUser(user: User | null) {
    this.user.set(user);
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
    else localStorage.removeItem(USER_KEY);
  }

  setToken(token: string | null) {
    this.token.set(token);
    if (token) localStorage.setItem(environment.tokenKey, token);
    else localStorage.removeItem(environment.tokenKey);
  }

  setRefreshToken(token: string | null) {
    this.refreshTokenValue.set(token);
    if (token) localStorage.setItem(environment.refreshTokenKey, token);
    else localStorage.removeItem(environment.refreshTokenKey);
  }

  clear() {
    this.user.set(null);
    this.token.set(null);
    this.refreshTokenValue.set(null);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(environment.tokenKey);
    localStorage.removeItem(environment.refreshTokenKey);
  }
}
