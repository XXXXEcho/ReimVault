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

export interface MatrixCell {
  amount: number;
  count: number;
}

export interface MonthlyBatchColumn {
  batchId: number;
  batchName: string;
  monthLabel: string;
}

export interface EmployeeMatrixRow {
  employeeId: number;
  employeeName: string;
  department: string;
  cells: MatrixCell[];
  unassigned: MatrixCell;
  total: MatrixCell;
}

export interface MatrixTotals {
  columnTotals: MatrixCell[];
  unassignedTotal: MatrixCell;
  grandTotal: MatrixCell;
}

export interface PersonnelMatrix {
  columns: MonthlyBatchColumn[];
  rows: EmployeeMatrixRow[];
  totals: MatrixTotals;
}

function buildParams(oaIds: number[], batchIds: number[], employeeIds: number[]) {
  const params = new URLSearchParams();
  if (oaIds.length) params.set('oaIds', oaIds.join(','));
  if (batchIds.length) params.set('batchIds', batchIds.join(','));
  if (employeeIds.length) params.set('employeeIds', employeeIds.join(','));
  return params;
}

export function getStats(oaIds: number[], batchIds: number[], employeeIds: number[] = []) {
  return http.get<ReimbursementStats>('/admin/reimbursements/stats', { params: buildParams(oaIds, batchIds, employeeIds) });
}

export function getPersonnelMatrix(oaIds: number[], batchIds: number[], employeeIds: number[]) {
  return http.get<PersonnelMatrix>('/admin/reimbursements/stats/personnel-matrix', { params: buildParams(oaIds, batchIds, employeeIds) });
}
