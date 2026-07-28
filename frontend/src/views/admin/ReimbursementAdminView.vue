<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useAuthStore } from '../../stores/auth';
import EmptyState from '../../components/EmptyState.vue';
import MaterialPreviewer from '../../components/MaterialPreviewer.vue';
import MetricCard from '../../components/MetricCard.vue';
import RecordDrawer from '../../components/RecordDrawer.vue';
import WorkbenchFilters from '../../components/WorkbenchFilters.vue';
import WorkbenchRecordTable from '../../components/WorkbenchRecordTable.vue';
import {
  bulkUpdateReimbursements,
  listAdminReimbursements,
  type AdminReimbursementFilters,
  type BulkReimbursementAction,
  type ReimbursementRecord,
  type ReimbursementStatus
} from '../../api/reimbursements';

const auth = useAuthStore();
const records = ref<ReimbursementRecord[]>([]);
const selected = ref<ReimbursementRecord | null>(null);
const selectedIds = ref<number[]>([]);
const previewAttachmentId = ref<number | null>(null);
const filters = ref({ employeeId: '', categoryId: '', status: 'SUBMITTED', from: '', to: '', keyword: '', oaId: '', reimbursed: '' });
const loading = ref(false);
const error = ref('');

function params(): AdminReimbursementFilters {
  const next: AdminReimbursementFilters = {};
  if (filters.value.employeeId) next.employeeId = Number(filters.value.employeeId);
  if (filters.value.categoryId) next.categoryId = Number(filters.value.categoryId);
  if (filters.value.status) next.status = filters.value.status as ReimbursementStatus;
  if (filters.value.from) next.from = filters.value.from;
  if (filters.value.to) next.to = filters.value.to;
  if (filters.value.keyword) next.keyword = filters.value.keyword;
  if (filters.value.oaId) next.oaId = Number(filters.value.oaId);
  if (filters.value.reimbursed) next.reimbursed = filters.value.reimbursed === 'true';
  return next;
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const response = await listAdminReimbursements(params());
    records.value = response.data;
    selectedIds.value = selectedIds.value.filter((id) => records.value.some((record) => record.id === id));
  } catch (err) {
    error.value = apiErrorMessage(err);
  } finally {
    loading.value = false;
  }
}

async function refreshSelected(record: ReimbursementRecord) {
  await load();
  selected.value = records.value.find((item) => item.id === record.id) ?? record;
}

function hasPaymentVoucher(record: ReimbursementRecord) {
  return (record.attachments ?? []).some((attachment) => attachment.type === 'PAYMENT_VOUCHER');
}

const previewAttachments = computed(() => selected.value?.attachments ?? []);

const metrics = computed(() => ({
  draft: records.value.filter((record) => record.status === 'DRAFT').length,
  submitted: records.value.filter((record) => record.status === 'SUBMITTED' && !record.reimbursedAt).length,
  reimbursed: records.value.filter((record) => record.reimbursedAt).length,
  archived: records.value.filter((record) => record.status === 'ARCHIVED').length,
  incomplete: records.value.filter((record) => !hasPaymentVoucher(record)).length
}));

function resetFilters() {
  filters.value = { employeeId: '', categoryId: '', status: 'SUBMITTED', from: '', to: '', keyword: '', oaId: '', reimbursed: '' };
  void load();
}

function apiErrorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '操作失败，请稍后重试';
}

async function runBulkAction(action: BulkReimbursementAction, label: string) {
  if (!selectedIds.value.length) return;
  try {
    await ElMessageBox.confirm(`确认对 ${selectedIds.value.length} 条记录执行“${label}”？`, '批量处理确认', {
      confirmButtonText: label,
      cancelButtonText: '取消',
      type: action === 'REJECT' ? 'warning' : 'info'
    });
  } catch {
    return;
  }
  try {
    await bulkUpdateReimbursements(selectedIds.value, action);
    ElMessage.success(`${label}完成`);
    selectedIds.value = [];
    await load();
  } catch (err) {
    ElMessage.error(apiErrorMessage(err));
  }
}

function openDrawer(record: ReimbursementRecord) {
  selected.value = record;
  previewAttachmentId.value = null;
}

function closeDrawer() {
  selected.value = null;
  previewAttachmentId.value = null;
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
      <MetricCard title="待报销" :value="metrics.submitted" tone="success" />
      <MetricCard title="已报销" :value="metrics.reimbursed" />
      <MetricCard title="已归档" :value="metrics.archived" />
      <MetricCard title="材料不完整" :value="metrics.incomplete" tone="danger" />
    </div>

    <WorkbenchFilters v-model="filters" admin @apply="load" @reset="resetFilters" />

    <p v-if="error" class="notice notice--error" role="alert">
      {{ error }}
      <button type="button" @click="load">重试</button>
    </p>

    <div v-if="records.length" class="bulk-bar enterprise-card">
      <span>已选择 {{ selectedIds.length }} 条</span>
      <button type="button" :disabled="!selectedIds.length" @click="runBulkAction('REIMBURSE', '标记已报销')">标记已报销</button>
      <button type="button" :disabled="!selectedIds.length" @click="runBulkAction('UNREIMBURSE', '撤销报销')">撤销报销</button>
      <button type="button" :disabled="!selectedIds.length" @click="runBulkAction('ARCHIVE', '归档')">归档</button>
      <button type="button" :disabled="!selectedIds.length" @click="runBulkAction('REJECT', '打回')">打回</button>
      <button type="button" :disabled="!selectedIds.length" @click="selectedIds = []">清空选择</button>
    </div>

    <p v-if="loading" class="loading">加载报销记录中...</p>
    <WorkbenchRecordTable v-else-if="records.length" v-model:selected-ids="selectedIds" :records="records" admin @open="openDrawer" />
    <EmptyState v-else title="暂无待处理记录" description="符合筛选条件的报销会显示在这里。" />

    <RecordDrawer
      v-if="selected"
      :record="selected"
      :role="auth.user?.role ?? 'ADMIN'"
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

.bulk-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
}

.bulk-bar span {
  color: var(--color-text-muted);
  font-weight: 700;
}

.bulk-bar button,
.notice button {
  min-height: 36px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-primary-strong);
  cursor: pointer;
}

.bulk-bar button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.notice {
  margin: 0;
  border-radius: var(--radius-md);
  padding: var(--space-3);
  font-weight: 700;
}

.notice--error {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.loading {
  margin: 0;
  color: var(--color-text-muted);
  font-weight: 700;
}
</style>
