<script setup lang="ts">
import MaterialCompleteness from './MaterialCompleteness.vue';
import StatusTag from './StatusTag.vue';
import type { AttachmentType, ReimbursementRecord } from '../api/reimbursements';

const props = defineProps<{
  records: ReimbursementRecord[];
  admin?: boolean;
  selectedIds?: number[];
}>();

const emit = defineEmits<{
  open: [record: ReimbursementRecord];
  'update:selectedIds': [ids: number[]];
}>();

const money = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' });

function attachmentCount(record: ReimbursementRecord, type: AttachmentType) {
  return (record.attachments ?? []).filter((attachment) => attachment.type === type).length;
}

function openFromKeyboard(event: KeyboardEvent, record: ReimbursementRecord) {
  if (event.key === 'Enter') emit('open', record);
}

function formatTime(value: string | null | undefined) {
  if (!value) return '—';
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function toggle(id: number, checked: boolean) {
  const current = new Set(props.selectedIds ?? []);
  if (checked) current.add(id);
  else current.delete(id);
  emit('update:selectedIds', [...current]);
}

function toggleAll(checked: boolean) {
  emit('update:selectedIds', checked ? props.records.map((record) => record.id) : []);
}
</script>

<template>
  <div class="workbench-table-wrap enterprise-card">
    <table class="workbench-table">
      <thead>
        <tr>
          <th v-if="props.admin" class="workbench-table__select">
            <input
              aria-label="全选报销记录"
              type="checkbox"
              :checked="props.records.length > 0 && (props.selectedIds?.length ?? 0) === props.records.length"
              @click.stop
              @change="toggleAll(($event.target as HTMLInputElement).checked)"
            />
          </th>
          <th v-if="props.admin">员工</th>
          <th>金额</th>
          <th>用途分类</th>
          <th>用途说明</th>
          <th>支付时间</th>
          <th>状态</th>
          <th>材料</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="record in props.records"
          :key="record.id"
          :data-test="`record-row-${record.id}`"
          tabindex="0"
          class="workbench-table__row"
          @click="emit('open', record)"
          @keydown="openFromKeyboard($event, record)"
        >
          <td v-if="props.admin" class="workbench-table__select">
            <input
              :aria-label="`选择报销记录${record.id}`"
              type="checkbox"
              :checked="props.selectedIds?.includes(record.id)"
              @click.stop
              @change="toggle(record.id, ($event.target as HTMLInputElement).checked)"
            />
          </td>
          <td v-if="props.admin">{{ record.employeeName }}</td>
          <td>{{ money.format(Number(record.amount)) }}</td>
          <td>{{ record.categoryName }}</td>
          <td>{{ record.purpose }}</td>
          <td>{{ formatTime(record.paymentTime) }}</td>
          <td><StatusTag :status="record.status" /></td>
          <td>
            <MaterialCompleteness
              :payment-voucher-count="attachmentCount(record, 'PAYMENT_VOUCHER')"
              :order-screenshot-count="attachmentCount(record, 'ORDER_SCREENSHOT')"
              :invoice-count="attachmentCount(record, 'INVOICE')"
            />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.workbench-table-wrap {
  overflow-x: auto;
}

.workbench-table {
  width: 100%;
  border-collapse: collapse;
}

.workbench-table th,
.workbench-table td {
  border-bottom: 1px solid var(--color-border);
  padding: var(--space-3);
  text-align: left;
  vertical-align: top;
}

.workbench-table__row {
  min-height: 44px;
  cursor: pointer;
}

.workbench-table__row:hover,
.workbench-table__row:focus {
  background: var(--color-surface-muted);
  outline: 2px solid var(--color-primary-soft);
  outline-offset: -2px;
}

.workbench-table__select {
  width: 44px;
  text-align: center;
}

.workbench-table__select input {
  width: 18px;
  height: 18px;
}
</style>
