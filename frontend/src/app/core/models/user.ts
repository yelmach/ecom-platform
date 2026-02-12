export type UserRole = 'CLIENT' | 'SELLER';

export interface User {
  username: string;
  email: string;
  role: UserRole;
  avatarUrl: string | null;
}
