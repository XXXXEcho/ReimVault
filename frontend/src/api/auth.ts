import http from './http';

export type Role = 'ADMIN' | 'EMPLOYEE';

export interface CurrentUser {
  id: number;
  username: string;
  displayName: string;
  department: string;
  role: Role;
}

export function login(payload: { username: string; password: string }) {
  return http.post<CurrentUser>('/auth/login', payload);
}

export function getCurrentUser() {
  return http.get<CurrentUser>('/auth/me');
}
