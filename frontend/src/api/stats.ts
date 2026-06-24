import http from './http';

export interface ReimbursementStats {
  totalCount: number;
  totalAmount: number;
  reimbursedCount: number;
  reimbursedAmount: number;
  unreimbursedCount: number;
  unreimbursedAmount: number;
  draftCount: number;
  draftAmount: number;
}

export function getStats(oaIds: number[], batchIds: number[]) {
  const params = new URLSearchParams();
  if (oaIds.length) params.set('oaIds', oaIds.join(','));
  if (batchIds.length) params.set('batchIds', batchIds.join(','));
  return http.get<ReimbursementStats>('/admin/reimbursements/stats', { params });
}
