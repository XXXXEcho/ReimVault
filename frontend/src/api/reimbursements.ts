import http from './http';

export type ReimbursementStatus = 'DRAFT' | 'SUBMITTED' | 'ARCHIVED';

export interface ReimbursementInput {
  amount: number;
  categoryId: number;
  purpose: string;
  paymentTime: string;
}

export interface ReimbursementAttachment {
  id: number;
  type: AttachmentType;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  createdAt: string;
}

export interface ReimbursementRecord extends ReimbursementInput {
  id: number;
  employeeId: number;
  employeeName: string;
  categoryName: string;
  status: ReimbursementStatus;
  adminRemark: string;
  submittedAt: string | null;
  archivedAt: string | null;
  batchId: number | null;
  batchName: string | null;
  attachments?: ReimbursementAttachment[];
}

export function listReimbursements() {
  return http.get<ReimbursementRecord[]>('/reimbursements');
}

export function createReimbursement(payload: ReimbursementInput) {
  return http.post<ReimbursementRecord>('/reimbursements', payload);
}

export function getReimbursement(id: number) {
  return http.get<ReimbursementRecord>(`/reimbursements/${id}`);
}

export function updateReimbursement(id: number, payload: ReimbursementInput) {
  return http.patch<ReimbursementRecord>(`/reimbursements/${id}`, payload);
}

export function submitReimbursement(id: number) {
  return http.post<ReimbursementRecord>(`/reimbursements/${id}/submit`);
}

export function deleteReimbursement(id: number) {
  return http.delete(`/reimbursements/${id}`);
}

export type AttachmentType = 'PAYMENT_VOUCHER' | 'ORDER_SCREENSHOT' | 'INVOICE';

export function uploadAttachment(recordId: number, type: AttachmentType, file: File) {
  const form = new FormData();
  form.append('file', file);
  return http.post(`/reimbursements/${recordId}/attachments?type=${type}`, form);
}

export function deleteAttachment(attachmentId: number) {
  return http.delete(`/attachments/${attachmentId}`);
}

export interface AdminReimbursementFilters {
  employeeId?: number;
  categoryId?: number;
  status?: ReimbursementStatus;
  from?: string;
  to?: string;
}

export function listAdminReimbursements(params: AdminReimbursementFilters = { status: 'SUBMITTED' }) {
  return http.get<ReimbursementRecord[]>('/admin/reimbursements', { params });
}

export function getAdminReimbursement(id: number) {
  return http.get<ReimbursementRecord>(`/admin/reimbursements/${id}`);
}

export function updateAdminRemark(id: number, adminRemark: string) {
  return http.patch<ReimbursementRecord>(`/admin/reimbursements/${id}/remark`, { adminRemark });
}
