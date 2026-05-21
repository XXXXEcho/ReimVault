import http from './http';

export interface Category {
  id: number;
  name: string;
  enabled: boolean;
  sortOrder: number;
  remark: string;
}

export type CategoryInput = Omit<Category, 'id'>;

export function listCategories() {
  return http.get<Category[]>('/categories');
}

export function listAdminCategories() {
  return http.get<Category[]>('/admin/categories');
}

export function createCategory(payload: CategoryInput) {
  return http.post<Category>('/admin/categories', payload);
}

export function updateCategory(id: number, payload: CategoryInput) {
  return http.patch<Category>(`/admin/categories/${id}`, payload);
}
