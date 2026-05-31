import http from './http';

export interface OaNumber {
  id: number;
  number: string;
  createdAt: string;
}

export function listOaNumbers() {
  return http.get<OaNumber[]>('/admin/oa-numbers');
}

export function createOaNumber(number: string) {
  return http.post<OaNumber>('/admin/oa-numbers', { number });
}

export function updateOaNumber(id: number, number: string) {
  return http.put<OaNumber>(`/admin/oa-numbers/${id}`, { number });
}

export function deleteOaNumber(id: number) {
  return http.delete(`/admin/oa-numbers/${id}`);
}
