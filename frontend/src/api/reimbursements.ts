import http from './http';

export type ReimbursementStatus = 'DRAFT' | 'SUBMITTED' | 'ARCHIVED';
export type AttachmentType = 'PAYMENT_VOUCHER' | 'ORDER_SCREENSHOT' | 'INVOICE';

export interface ReimbursementInput {
  amount: number;
  categoryId: number;
  purpose: string;
  paymentTime: string;
}

export interface AttachmentRecord {
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
  adminRemark: string | null;
  submittedAt: string | null;
  archivedAt: string | null;
  attachments: AttachmentRecord[];
}

export interface EmployeeReimbursementFilters {
  categoryId?: number;
  status?: ReimbursementStatus;
  from?: string;
  to?: string;
  keyword?: string;
}

export interface AdminReimbursementFilters extends EmployeeReimbursementFilters {
  employeeId?: number;
}

export function listReimbursements(params: EmployeeReimbursementFilters = {}) {
  return http.get<ReimbursementRecord[]>('/reimbursements', { params });
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

export function uploadAttachment(recordId: number, type: AttachmentType, file: File) {
  const form = new FormData();
  form.append('file', file);
  return http.post<AttachmentRecord>(`/reimbursements/${recordId}/attachments?type=${type}`, form);
}

export function deleteAttachment(attachmentId: number) {
  return http.delete(`/attachments/${attachmentId}`);
}

export function listAdminReimbursements(params: AdminReimbursementFilters = { status: 'SUBMITTED' }) {
  return http.get<ReimbursementRecord[]>('/admin/reimbursements', { params });
}

export function updateAdminRemark(id: number, adminRemark: string) {
  return http.patch<ReimbursementRecord>(`/admin/reimbursements/${id}/remark`, { adminRemark });
}
