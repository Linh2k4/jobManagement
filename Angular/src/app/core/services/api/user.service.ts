import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, ApiResponse } from '../api.service';
import { User } from '../../models';

export interface UserInfo extends User {
  isActive: boolean;
  groupName?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  constructor(private api: ApiService) {}

  /** GET /users — Manager only */
  listUsers(): Observable<ApiResponse<UserInfo[]>> {
    return this.api.get<UserInfo[]>('users');
  }

  /** DELETE /users/{id} — Manager only, deactivates the account */
  deactivateUser(userId: number): Observable<ApiResponse<void>> {
    return this.api.delete<void>(`users/${userId}`);
  }

  /** PATCH /users/{id}/activate — Manager only, reactivates the account */
  activateUser(userId: number): Observable<ApiResponse<void>> {
    return this.api.patch<void>(`users/${userId}/activate`, {});
  }

  /** GET /users/{id} — Manager only */
  getUser(userId: number): Observable<ApiResponse<UserInfo>> {
    return this.api.get<UserInfo>(`users/${userId}`);
  }

  /** POST /users/register — Manager only */
  createUser(data: { email: string; fullName: string; password: string; role: string }): Observable<ApiResponse<UserInfo>> {
    return this.api.post<UserInfo>('users/register', data);
  }

  /** PUT /users/{id} — Manager only, edits fullName/role of another user */
  updateUser(userId: number, data: { fullName?: string; role?: string }): Observable<ApiResponse<UserInfo>> {
    return this.api.put<UserInfo>(`users/${userId}`, data);
  }
}
