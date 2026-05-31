import http from './http';

export type ReimbursementStatus = 'DRAFT' | 'SUBMITTED' | 'ARCHIVED';

export function statusLabel(status: ReimbursementStatus, reimbursedAt?: string | null) {
  if (status === 'SUBMITTED' && reimbursedAt) return '已报销';
  return { DRAFT: '未提交', SUBMITTED: '已提交未报销', ARCHIVED: '已报销' }[status];
}

export function formatTime(iso: string | null | undefined) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

export interface ReimbursementInput {
  amount: number;
  categoryId: number;
  purpose: string;
  paymentTime: string;
  oaId: number | null;
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
  reimbursedAt: string | null;
  batchId: number | null;
  batchName: string | null;
  oaId: number | null;
  oaNumber: string;
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

export function withdrawReimbursement(id: number) {
  return http.post<ReimbursementRecord>(`/reimbursements/${id}/withdraw`);
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
  reimbursed?: boolean;
  oaId?: number;
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

export function rejectReimbursement(id: number) {
  return http.post<ReimbursementRecord>(`/admin/reimbursements/${id}/reject`);
}

export function markReimbursed(id: number) {
  return http.post<ReimbursementRecord>(`/admin/reimbursements/${id}/reimburse`);
}

export function unreimburse(id: number) {
  return http.post<ReimbursementRecord>(`/admin/reimbursements/${id}/unreimburse`);
}

export function updateOaNumber(id: number, oaId: number | null) {
  return http.patch<ReimbursementRecord>(`/admin/reimbursements/${id}/oa-number`, { oaId });
}
