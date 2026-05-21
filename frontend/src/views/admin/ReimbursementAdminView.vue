<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import EmptyState from '../../components/EmptyState.vue';
import MetricCard from '../../components/MetricCard.vue';
import RecordDrawer from '../../components/RecordDrawer.vue';
import WorkbenchFilters from '../../components/WorkbenchFilters.vue';
import WorkbenchRecordTable from '../../components/WorkbenchRecordTable.vue';
import {
  listAdminReimbursements,
  type AdminReimbursementFilters,
  type ReimbursementRecord,
  type ReimbursementStatus
} from '../../api/reimbursements';

const records = ref<ReimbursementRecord[]>([]);
const selected = ref<ReimbursementRecord | null>(null);
const filters = ref({ employeeId: '', categoryId: '', status: 'SUBMITTED', from: '', to: '', keyword: '' });

function params(): AdminReimbursementFilters {
  const next: AdminReimbursementFilters = {};
  if (filters.value.employeeId) next.employeeId = Number(filters.value.employeeId);
  if (filters.value.categoryId) next.categoryId = Number(filters.value.categoryId);
  if (filters.value.status) next.status = filters.value.status as ReimbursementStatus;
  if (filters.value.from) next.from = filters.value.from;
  if (filters.value.to) next.to = filters.value.to;
  if (filters.value.keyword) next.keyword = filters.value.keyword;
  return next;
}

async function load() {
  const response = await listAdminReimbursements(params());
  records.value = response.data;
}

async function refreshSelected(record: ReimbursementRecord) {
  await load();
  selected.value = records.value.find((item) => item.id === record.id) ?? record;
}

function hasPaymentVoucher(record: ReimbursementRecord) {
  return (record.attachments ?? []).some((attachment) => attachment.type === 'PAYMENT_VOUCHER');
}

const metrics = computed(() => ({
  draft: records.value.filter((record) => record.status === 'DRAFT').length,
  submitted: records.value.filter((record) => record.status === 'SUBMITTED').length,
  archived: records.value.filter((record) => record.status === 'ARCHIVED').length,
  incomplete: records.value.filter((record) => !hasPaymentVoucher(record)).length
}));

function resetFilters() {
  filters.value = { employeeId: '', categoryId: '', status: 'SUBMITTED', from: '', to: '', keyword: '' };
  void load();
}

function openDrawer(record: ReimbursementRecord) {
  selected.value = record;
}

onMounted(load);
</script>

<template>
  <section class="workbench-page">
    <header class="workbench-page__header">
      <div>
        <p class="eyebrow">Admin Workbench</p>
        <h1>报销管理</h1>
      </div>
    </header>

    <div class="metrics-grid">
      <MetricCard title="草稿" :value="metrics.draft" />
      <MetricCard title="已提交" :value="metrics.submitted" tone="success" />
      <MetricCard title="已归档" :value="metrics.archived" />
      <MetricCard title="材料不完整" :value="metrics.incomplete" tone="danger" />
    </div>

    <WorkbenchFilters v-model="filters" admin @apply="load" @reset="resetFilters" />

    <WorkbenchRecordTable v-if="records.length" :records="records" admin @open="openDrawer" />
    <EmptyState v-else title="暂无待处理记录" description="符合筛选条件的报销会显示在这里。" />

    <RecordDrawer
      v-if="selected"
      :record="selected"
      :role="'ADMIN'"
      @close="selected = null"
      @saved="refreshSelected"
      @submitted="refreshSelected"
    />
  </section>
</template>

<style scoped>
.workbench-page {
  display: grid;
  gap: var(--space-5);
}

.workbench-page__header h1,
.eyebrow {
  margin: 0;
}

.eyebrow {
  color: var(--color-text-muted);
  font-size: 0.875rem;
  font-weight: 700;
  text-transform: uppercase;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: var(--space-4);
}

</style>
