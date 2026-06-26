import http from './http';
import type { Role } from './auth';

export interface UserRecord {
  id: number;
  username: string;
  displayName: string;
  department: string;
  role: Role;
  enabled: boolean;
}

export interface CreateUserInput {
  username: string;
  displayName: string;
  department: string;
  password: string;
  role: Role;
}

export interface UpdateUserInput {
  displayName: string;
  department: string;
  role: Role;
  enabled: boolean;
}

export function listUsers() {
  return http.get<UserRecord[]>('/admin/users');
}

export function searchEmployees(keyword = '') {
  return http.get<UserRecord[]>('/admin/employees', { params: keyword ? { keyword } : {} });
}

export function createUser(payload: CreateUserInput) {
  return http.post<UserRecord>('/admin/users', payload);
}

export function updateUser(id: number, payload: UpdateUserInput) {
  return http.patch<UserRecord>(`/admin/users/${id}`, payload);
}
