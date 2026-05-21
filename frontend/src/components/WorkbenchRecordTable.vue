<script setup lang="ts">
import MaterialCompleteness from './MaterialCompleteness.vue';
import StatusTag from './StatusTag.vue';
import type { AttachmentType, ReimbursementRecord } from '../api/reimbursements';

const props = defineProps<{
  records: ReimbursementRecord[];
  admin?: boolean;
}>();

const emit = defineEmits<{
  open: [record: ReimbursementRecord];
}>();

function attachmentCount(record: ReimbursementRecord, type: AttachmentType) {
  return (record.attachments ?? []).filter((attachment) => attachment.type === type).length;
}

function openFromKeyboard(event: KeyboardEvent, record: ReimbursementRecord) {
  if (event.key === 'Enter') emit('open', record);
}
</script>

<template>
  <div class="workbench-table-wrap enterprise-card">
    <table class="workbench-table">
      <thead>
        <tr>
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
          <td v-if="props.admin">{{ record.employeeName }}</td>
          <td>¥{{ record.amount }}</td>
          <td>{{ record.categoryName }}</td>
          <td>{{ record.purpose }}</td>
          <td>{{ record.paymentTime }}</td>
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
</style>
