import http from './http';

export type ReimbursementStatus = 'DRAFT' | 'SUBMITTED' | 'ARCHIVED';
export type AttachmentType = 'PAYMENT_VOUCHER' | 'ORDER_SCREENSHOT' | 'INVOICE';
export type BulkReimbursementAction = 'REIMBURSE' | 'UNREIMBURSE' | 'REJECT' | 'ARCHIVE';

export function statusLabel(status: ReimbursementStatus, reimbursedAt?: string | null) {
  if (status === 'SUBMITTED' && reimbursedAt) return '已报销';
  return { DRAFT: '草稿', SUBMITTED: '待报销', ARCHIVED: '已归档' }[status];
}

export function formatTime(iso: string | null | undefined) {
  if (!iso) return '-';
  return new Date(iso).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

export interface ReimbursementInput {
  amount: number;
  categoryId: number;
  purpose: string;
  paymentTime: string;
  oaId?: number | null;
}

export interface AttachmentRecord {
  id: number;
  type: AttachmentType;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  createdAt: string;
}

export type ReimbursementAttachment = AttachmentRecord;

export interface ReimbursementRecord extends ReimbursementInput {
  id: number;
  employeeId: number;
  employeeName: string;
  categoryName: string;
  status: ReimbursementStatus;
  adminRemark: string | null;
  submittedAt: string | null;
  archivedAt: string | null;
  reimbursedAt?: string | null;
  batchId?: number | null;
  batchName?: string | null;
  oaId?: number | null;
  oaNumber?: string | null;
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
  reimbursed?: boolean;
  oaId?: number;
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

export function withdrawReimbursement(id: number) {
  return http.post<ReimbursementRecord>(`/reimbursements/${id}/withdraw`);
}

export function deleteReimbursement(id: number) {
  return http.delete(`/reimbursements/${id}`);
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

export function archiveRecords(ids: number[]) {
  return http.post('/admin/reimbursements/archive', { ids });
}

export function bulkUpdateReimbursements(ids: number[], action: BulkReimbursementAction) {
  return http.post<ReimbursementRecord[]>('/admin/reimbursements/bulk-action', { ids, action });
}

export function updateOaNumber(id: number, oaId: number | null) {
  return http.patch<ReimbursementRecord>(`/admin/reimbursements/${id}/oa-number`, { oaId });
}
