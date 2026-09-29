import { Role } from './role.enum';

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UserAuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}
