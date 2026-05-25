import http from './http';

export interface BatchItem {
  id: number;
  recordId: number;
  employeeName: string;
  categoryName: string;
}

export interface Batch {
  id: number;
  name: string;
  description: string;
  createdAt: string;
  archivedAt: string | null;
  items: BatchItem[];
}

export function listBatches() {
  return http.get<Batch[]>('/admin/batches');
}

export function createBatch(payload: { name: string; description: string }) {
  return http.post<Batch>('/admin/batches', payload);
}

export function getBatch(id: number) {
  return http.get<Batch>(`/admin/batches/${id}`);
}

export function addBatchItem(batchId: number, recordId: number) {
  return http.post(`/admin/batches/${batchId}/items/${recordId}`);
}

export function removeBatchItem(batchId: number, recordId: number) {
  return http.delete(`/admin/batches/${batchId}/items/${recordId}`);
}

export function archiveBatch(batchId: number) {
  return http.post(`/admin/batches/${batchId}/archive`);
}

export function exportBatchExcel(batchId: number) {
  return http.get<Blob>(`/admin/batches/${batchId}/export/excel`, { responseType: 'blob' });
}

export function exportBatchAttachments(batchId: number) {
  return http.get<Blob>(`/admin/batches/${batchId}/export/attachments`, { responseType: 'blob' });
}

export function ensureMonthlyBatch() {
  return http.post<Batch>('/admin/batches/monthly');
}
