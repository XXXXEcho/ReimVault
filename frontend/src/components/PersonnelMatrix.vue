<script setup lang="ts">
import type { MatrixCell, PersonnelMatrix } from '../api/stats';

const props = defineProps<{
  matrix: PersonnelMatrix | null;
}>();

const money = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY', minimumFractionDigits: 2 });

function formatAmount(cell: MatrixCell) {
  return cell.count > 0 ? money.format(Number(cell.amount)) : '—';
}

function formatCount(cell: MatrixCell) {
  return cell.count > 0 ? `${cell.count} 笔` : '';
}
</script>

<template>
  <section class="matrix-panel enterprise-card">
    <header class="matrix-panel__head">
      <h2>个人月度报销矩阵</h2>
      <p class="matrix-panel__desc">每位员工在各月度批次中的报销金额；未归入月度批次的记录单列展示。</p>
    </header>

    <p v-if="!props.matrix || props.matrix.rows.length === 0" class="matrix-empty" data-test="matrix-empty">
      当前筛选范围下暂无报销记录。
    </p>

    <div v-else class="matrix-table-wrap">
      <table class="matrix-table">
        <thead>
          <tr>
            <th class="matrix-table__name-col">员工</th>
            <th class="matrix-table__name-col">部门</th>
            <th
              v-for="column in props.matrix.columns"
              :key="column.batchId"
              :data-test="`matrix-column-${column.batchId}`"
            >{{ column.batchName }}</th>
            <th>未入批次</th>
            <th>合计</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in props.matrix.rows"
            :key="row.employeeId"
            :data-test="`matrix-row-${row.employeeId}`"
          >
            <td class="matrix-table__name-col matrix-table__employee">{{ row.employeeName }}</td>
            <td class="matrix-table__name-col">{{ row.department }}</td>
            <td
              v-for="(cell, index) in row.cells"
              :key="index"
              :data-test="`matrix-cell-${row.employeeId}-${props.matrix.columns[index].batchId}`"
            >
              <span class="matrix-amount">{{ formatAmount(cell) }}</span>
              <small class="matrix-count">{{ formatCount(cell) }}</small>
            </td>
            <td :data-test="`matrix-cell-${row.employeeId}-unassigned`">
              <span class="matrix-amount">{{ formatAmount(row.unassigned) }}</span>
              <small class="matrix-count">{{ formatCount(row.unassigned) }}</small>
            </td>
            <td>
              <span class="matrix-amount matrix-amount--strong">{{ formatAmount(row.total) }}</span>
              <small class="matrix-count">{{ formatCount(row.total) }}</small>
            </td>
          </tr>
        </tbody>
        <tfoot>
          <tr data-test="matrix-totals-row">
            <th class="matrix-table__name-col">合计</th>
            <td class="matrix-table__name-col"></td>
            <td v-for="(cell, index) in props.matrix.totals.columnTotals" :key="index">
              <span class="matrix-amount matrix-amount--strong">{{ formatAmount(cell) }}</span>
            </td>
            <td>
              <span class="matrix-amount matrix-amount--strong">{{ formatAmount(props.matrix.totals.unassignedTotal) }}</span>
            </td>
            <td>
              <span class="matrix-amount matrix-amount--grand">{{ formatAmount(props.matrix.totals.grandTotal) }}</span>
            </td>
          </tr>
        </tfoot>
      </table>
    </div>
  </section>
</template>

<style scoped>
.matrix-panel {
  padding: var(--space-5);
}

.matrix-panel__head {
  margin-bottom: var(--space-4);
}

.matrix-panel__head h2 {
  margin: 0 0 var(--space-1);
  font-size: 1rem;
  color: var(--color-text);
}

.matrix-panel__desc {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.matrix-empty {
  margin: 0;
  padding: var(--space-4) 0;
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

.matrix-table-wrap {
  overflow-x: auto;
}

.matrix-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 640px;
}

.matrix-table th,
.matrix-table td {
  border-bottom: 1px solid var(--color-border);
  padding: var(--space-3);
  text-align: right;
  vertical-align: top;
  white-space: nowrap;
}

.matrix-table thead th {
  background: var(--color-surface-muted);
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-muted);
}

.matrix-table__name-col {
  text-align: left;
}

.matrix-table__employee {
  font-weight: 700;
  color: var(--color-text);
}

.matrix-amount {
  display: block;
  color: var(--color-text);
  font-variant-numeric: tabular-nums;
}

.matrix-count {
  color: var(--color-text-muted);
  font-size: 0.75rem;
}

.matrix-amount--strong {
  font-weight: 700;
}

.matrix-amount--grand {
  color: var(--color-primary);
  font-weight: 800;
}

.matrix-table tfoot th,
.matrix-table tfoot td {
  border-top: 2px solid var(--color-border);
  background: var(--color-surface-muted);
  font-weight: 700;
}
</style>
