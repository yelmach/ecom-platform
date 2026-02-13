export type UserRole = 'CLIENT' | 'SELLER';

export interface User {
  userId: string;
  username: string;
  email: string;
  role: UserRole;
  avatarUrl: string | null;
}

export interface UpdateUserRequest {
  username?: string;
  email?: string;
  password?: string;
  role?: UserRole;
  avatarUrl?: string;
}
