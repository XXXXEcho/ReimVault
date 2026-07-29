<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import EmptyState from '../../components/EmptyState.vue';
import MaterialPreviewer from '../../components/MaterialPreviewer.vue';
import MetricCard from '../../components/MetricCard.vue';
import RecordDrawer from '../../components/RecordDrawer.vue';
import WorkbenchFilters from '../../components/WorkbenchFilters.vue';
import WorkbenchRecordTable from '../../components/WorkbenchRecordTable.vue';
import {
  listReimbursements,
  submitReimbursement,
  type EmployeeReimbursementFilters,
  type ReimbursementRecord,
  type ReimbursementStatus
} from '../../api/reimbursements';

const records = ref<ReimbursementRecord[]>([]);
const selected = ref<ReimbursementRecord | null>(null);
const previewAttachmentId = ref<number | null>(null);
const filters = ref({ categoryId: '', status: '', from: '', to: '', keyword: '' });

function params(): EmployeeReimbursementFilters {
  const next: EmployeeReimbursementFilters = {};
  if (filters.value.categoryId) next.categoryId = Number(filters.value.categoryId);
  if (filters.value.status) next.status = filters.value.status as ReimbursementStatus;
  if (filters.value.from) next.from = filters.value.from;
  if (filters.value.to) next.to = filters.value.to;
  if (filters.value.keyword) next.keyword = filters.value.keyword;
  return next;
}

async function load() {
  const response = await listReimbursements(params());
  records.value = response.data;
}

async function refreshSelected(record: ReimbursementRecord) {
  await load();
  selected.value = records.value.find((item) => item.id === record.id) ?? record;
}

function resetFilters() {
  filters.value = { categoryId: '', status: '', from: '', to: '', keyword: '' };
  void load();
}

function closeDrawer() {
  selected.value = null;
  previewAttachmentId.value = null;
}

function hasPaymentVoucher(record: ReimbursementRecord) {
  return (record.attachments ?? []).some((attachment) => attachment.type === 'PAYMENT_VOUCHER');
}

function apiErrorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '操作失败';
}

async function submitRecord(record: ReimbursementRecord) {
  if (import.meta.env.MODE !== 'test') {
    try {
      await ElMessageBox.confirm(`确认提交 ${Number(record.amount).toFixed(2)} 元的报销？提交后将进入报销专员处理。`, '提交报销', {
        confirmButtonText: '确认提交',
        cancelButtonText: '取消',
        type: 'info'
      });
    } catch {
      return;
    }
  }
  try {
    await submitReimbursement(record.id);
    ElMessage.success('报销已提交');
    selected.value = null;
    await load();
  } catch (err) {
    ElMessage.error(apiErrorMessage(err));
  }
}

const previewAttachments = computed(() => selected.value?.attachments ?? []);

const metrics = computed(() => ({
  draft: records.value.filter((record) => record.status === 'DRAFT').length,
  submitted: records.value.filter((record) => record.status === 'SUBMITTED' && !record.reimbursedAt).length,
  reimbursed: records.value.filter((record) => record.reimbursedAt).length,
  archived: records.value.filter((record) => record.status === 'ARCHIVED').length,
  incomplete: records.value.filter((record) => !hasPaymentVoucher(record)).length
}));

onMounted(load);
</script>

<template>
  <section class="workbench-page">
    <header class="workbench-page__header">
      <div>
        <p class="eyebrow">Employee Workbench</p>
        <h1>我的报销</h1>
      </div>
      <RouterLink class="primary-action" to="/reimbursements/new">新建报销</RouterLink>
    </header>

    <div class="metrics-grid">
      <MetricCard title="草稿" :value="metrics.draft" />
      <MetricCard title="待报销" :value="metrics.submitted" tone="success" />
      <MetricCard title="已报销" :value="metrics.reimbursed" />
      <MetricCard title="已归档" :value="metrics.archived" />
      <MetricCard title="材料不完整" :value="metrics.incomplete" tone="danger" />
    </div>

    <WorkbenchFilters v-model="filters" @apply="load" @reset="resetFilters" />

    <WorkbenchRecordTable v-if="records.length" :records="records" @open="selected = $event; previewAttachmentId = null" @submit="submitRecord" />
    <EmptyState v-else title="暂无报销记录" description="创建第一条报销后，它会显示在这里。">
      <template #action>
        <RouterLink class="primary-action" to="/reimbursements/new">新建报销</RouterLink>
      </template>
    </EmptyState>

    <RecordDrawer
      v-if="selected"
      :record="selected"
      :role="'EMPLOYEE'"
      @close="closeDrawer"
      @saved="refreshSelected"
      @submitted="refreshSelected"
      @preview="previewAttachmentId = $event"
    />
    <MaterialPreviewer
      v-if="previewAttachmentId"
      :attachments="previewAttachments"
      :active-id="previewAttachmentId"
      @close="previewAttachmentId = null"
    />
  </section>
</template>

<style scoped>
.workbench-page {
  display: grid;
  gap: var(--space-5);
}

.workbench-page__header {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  align-items: center;
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

.primary-action {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
}
</style>
