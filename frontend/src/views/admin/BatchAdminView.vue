<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import EmptyState from '../../components/EmptyState.vue';
import WorkbenchFilters from '../../components/WorkbenchFilters.vue';
import {
  addBatchItem,
  addBatchItems,
  archiveBatch,
  createBatch,
  ensureMonthlyBatch,
  exportBatchAttachments,
  exportBatchExcel,
  getBatch,
  listBatches,
  removeBatchItem,
  type Batch
} from '../../api/batches';
import { listAdminReimbursements, type AdminReimbursementFilters, type ReimbursementRecord, type ReimbursementStatus } from '../../api/reimbursements';

const batches = ref<Batch[]>([]);
const current = ref<Batch | null>(null);
const previewRecords = ref<ReimbursementRecord[]>([]);
const selectedPreviewIds = ref<number[]>([]);
const filters = ref({ employeeId: '', categoryId: '', status: 'SUBMITTED', from: '', to: '', keyword: '', oaId: '', reimbursed: '' });
const form = reactive({ name: '', description: '' });
const advanced = reactive({ open: false, batchId: null as number | null, recordId: null as number | null });
const loading = ref(false);
const notice = ref('');

const money = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' });
const currentBatchId = computed(() => current.value?.id ?? advanced.batchId ?? null);
const canJoin = computed(() => Boolean(currentBatchId.value && selectedPreviewIds.value.length));
const allPreviewSelected = computed(() => previewRecords.value.length > 0 && selectedPreviewIds.value.length === previewRecords.value.length);

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

function apiErrorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '操作失败，请稍后重试';
}

function formatTime(value: string | null | undefined) {
  if (!value) return '-';
  return new Date(value).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

function filename(kind: 'excel' | 'attachments') {
  const suffix = new Date().toISOString().slice(0, 7);
  return kind === 'excel' ? `报销导出-${suffix}.xlsx` : `报销附件-${suffix}.zip`;
}

function downloadBlob(blob: Blob, name: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function togglePreview(id: number, checked: boolean) {
  const next = new Set(selectedPreviewIds.value);
  if (checked) next.add(id);
  else next.delete(id);
  selectedPreviewIds.value = [...next];
}

function toggleAllPreview(checked: boolean) {
  selectedPreviewIds.value = checked ? previewRecords.value.map((record) => record.id) : [];
}

function resetFilters() {
  filters.value = { employeeId: '', categoryId: '', status: 'SUBMITTED', from: '', to: '', keyword: '', oaId: '', reimbursed: '' };
  void preview();
}

async function loadBatches() {
  const response = await listBatches();
  batches.value = response.data;
}

async function preview() {
  loading.value = true;
  notice.value = '';
  try {
    const response = await listAdminReimbursements(params());
    previewRecords.value = response.data;
    selectedPreviewIds.value = selectedPreviewIds.value.filter((id) => response.data.some((record) => record.id === id));
  } catch (err) {
    notice.value = apiErrorMessage(err);
  } finally {
    loading.value = false;
  }
}

async function saveBatch() {
  if (!form.name.trim()) return;
  try {
    const response = await createBatch({ name: form.name.trim(), description: form.description.trim() });
    form.name = '';
    form.description = '';
    current.value = response.data;
    advanced.batchId = response.data.id;
    await loadBatches();
    ElMessage.success('批次已创建');
  } catch (err) {
    ElMessage.error(apiErrorMessage(err));
  }
}

async function useMonthlyBatch() {
  try {
    const response = await ensureMonthlyBatch();
    current.value = response.data;
    advanced.batchId = response.data.id;
    await loadBatches();
    ElMessage.success('已切换到本月批次');
  } catch (err) {
    ElMessage.error(apiErrorMessage(err));
  }
}

async function loadBatch(id = advanced.batchId) {
  if (!id) return;
  const response = await getBatch(Number(id));
  current.value = response.data;
  advanced.batchId = response.data.id;
}

async function joinSelected() {
  if (!currentBatchId.value || !selectedPreviewIds.value.length) return;
  try {
    current.value = (await addBatchItems(currentBatchId.value, selectedPreviewIds.value)).data;
    selectedPreviewIds.value = [];
    ElMessage.success('已加入当前批次');
    await preview();
  } catch (err) {
    ElMessage.error(apiErrorMessage(err));
  }
}

async function addRecord() {
  if (!advanced.batchId || !advanced.recordId) return;
  await addBatchItem(Number(advanced.batchId), Number(advanced.recordId));
  advanced.recordId = null;
  await loadBatch(advanced.batchId);
}

async function removeRecord(id: number) {
  if (!currentBatchId.value) return;
  current.value = (await removeBatchItem(currentBatchId.value, id)).data;
}

async function archiveCurrent() {
  if (!currentBatchId.value) return;
  try {
    if (import.meta.env.MODE !== 'test') {
      await ElMessageBox.confirm('归档后批次内记录将不能再移出，确认继续？', '归档批次', {
        confirmButtonText: '归档',
        cancelButtonText: '取消',
        type: 'warning'
      });
    }
  } catch {
    return;
  }
  current.value = (await archiveBatch(currentBatchId.value)).data;
  await loadBatches();
  ElMessage.success('批次已归档');
}

async function exportExcel() {
  if (!currentBatchId.value) return;
  const response = await exportBatchExcel(currentBatchId.value);
  downloadBlob(response.data, filename('excel'));
}

async function exportAttachments() {
  if (!currentBatchId.value) return;
  const response = await exportBatchAttachments(currentBatchId.value);
  downloadBlob(response.data, filename('attachments'));
}

onMounted(async () => {
  await Promise.all([loadBatches(), preview()]);
});
</script>

<template>
  <section class="batch-admin-page">
    <header class="batch-header">
      <div>
        <p class="eyebrow">Batch Workflow</p>
        <h1>批次管理</h1>
      </div>
    </header>

    <section class="enterprise-card batch-card">
      <h2>1. 选择范围并预览</h2>
      <WorkbenchFilters v-model="filters" admin @apply="preview" @reset="resetFilters" />
      <p v-if="notice" class="notice" role="alert">{{ notice }}</p>
      <p v-if="loading" class="muted">查询待报销记录中...</p>
      <div v-else-if="previewRecords.length" class="table-scroll">
        <table>
          <thead>
            <tr>
              <th><input aria-label="全选当前查询结果" type="checkbox" :checked="allPreviewSelected" @change="toggleAllPreview(($event.target as HTMLInputElement).checked)" /></th>
              <th>员工</th>
              <th>金额</th>
              <th>分类</th>
              <th>用途</th>
              <th>经费编码</th>
              <th>支付时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in previewRecords" :key="record.id">
              <td><input :aria-label="`选择记录${record.id}`" type="checkbox" :checked="selectedPreviewIds.includes(record.id)" @change="togglePreview(record.id, ($event.target as HTMLInputElement).checked)" /></td>
              <td>{{ record.employeeName }}</td>
              <td>{{ money.format(Number(record.amount)) }}</td>
              <td>{{ record.categoryName }}</td>
              <td>{{ record.purpose }}</td>
              <td>{{ record.oaNumber || '-' }}</td>
              <td>{{ formatTime(record.paymentTime) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <EmptyState v-else title="暂无可加入批次的记录" description="调整筛选条件后再查询。" />
    </section>

    <section class="enterprise-card batch-card">
      <h2>2. 选择或创建批次</h2>
      <h3 class="section-subtitle">批次列表</h3>
      <form class="admin-form" @submit.prevent="saveBatch">
        <input aria-label="批次名称" v-model="form.name" placeholder="例如 2026年6月报销批次" />
        <input aria-label="批次描述" v-model="form.description" placeholder="说明，可选" />
        <button type="submit">创建并选中</button>
        <button type="button" class="ghost-btn" data-test="ensure-monthly-batch" @click="useMonthlyBatch">创建/使用本月批次</button>
      </form>
      <div class="batch-list">
        <button v-for="batch in batches" :key="batch.id" type="button" :class="{ active: current?.id === batch.id }" @click="loadBatch(batch.id)">
          {{ batch.name }}<small>{{ batch.archivedAt ? '已归档' : '未归档' }}</small>
        </button>
      </div>
    </section>

    <section class="enterprise-card batch-card">
      <div class="current-batch enterprise-card" data-test="current-batch-summary">
        <p class="eyebrow">当前批次</p>
        <h2>{{ current?.name ?? '未选择批次' }}</h2>
        <p>{{ current ? '勾选记录后加入当前批次' : '先创建或选择一个批次，再勾选待报销记录加入。' }}</p>
      </div>
      <div class="current-header">
        <div>
          <h2>3. 加入批次并导出</h2>
          <p class="muted">{{ current ? `当前批次：${current.name}` : '请先选择或创建批次' }}</p>
        </div>
        <div class="current-actions">
          <button type="button" :disabled="!canJoin" @click="joinSelected">加入选中记录</button>
          <button data-test="export-excel" type="button" :disabled="!currentBatchId" @click="exportExcel">导出 Excel</button>
          <button data-test="export-attachments" type="button" :disabled="!currentBatchId" @click="exportAttachments">下载附件包</button>
          <button data-test="archive-batch" type="button" :disabled="!currentBatchId" @click="archiveCurrent">归档批次</button>
        </div>
      </div>
      <div class="table-scroll">
        <table>
          <thead><tr><th>记录ID</th><th>员工</th><th>分类</th><th>经费编码</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in current?.items ?? []" :key="item.id">
              <td>{{ item.recordId }}</td>
              <td>{{ item.employeeName }}</td>
              <td>{{ item.categoryName }}</td>
              <td>{{ item.oaNumber || '-' }}</td>
              <td><button type="button" @click="removeRecord(item.recordId)">移除</button></td>
            </tr>
            <tr v-if="!(current?.items?.length)"><td colspan="5" class="empty-cell">当前批次还没有记录</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <details class="enterprise-card batch-card" :open="advanced.open" @toggle="advanced.open = ($event.target as HTMLDetailsElement).open">
      <summary>高级操作：按 ID 维护批次</summary>
      <form class="admin-form" @submit.prevent="loadBatch()">
        <input aria-label="批次ID" v-model="advanced.batchId" type="number" placeholder="批次ID" />
        <button data-test="load-batch" type="submit">加载批次</button>
        <input aria-label="报销记录ID" v-model="advanced.recordId" type="number" placeholder="报销记录ID" />
        <button data-test="add-record" type="button" @click="addRecord">加入批次</button>
      </form>
    </details>
  </section>
</template>

<style scoped>
.batch-admin-page,
.batch-card {
  display: grid;
  gap: var(--space-5);
}

.batch-header h1,
.eyebrow,
.batch-card h2,
.muted {
  margin: 0;
}

.eyebrow,
.muted {
  color: var(--color-text-muted);
}

.eyebrow {
  font-size: 0.875rem;
  font-weight: 700;
  text-transform: uppercase;
}

.batch-card {
  padding: var(--space-5);
}

.current-batch {
  gap: var(--space-2);
  padding: var(--space-4);
  background: var(--color-surface-muted);
}

.current-batch h2,
.current-batch p {
  margin: 0;
}

.section-subtitle {
  margin: calc(var(--space-2) * -1) 0 0;
  color: var(--color-text-muted);
  font-size: 1rem;
}

.admin-form,
.current-header,
.current-actions,
.batch-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.current-header {
  justify-content: space-between;
  align-items: flex-start;
}

.admin-form input,
button {
  min-height: 40px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
}

button {
  background: var(--color-surface);
  color: var(--color-primary-strong);
  cursor: pointer;
  font-weight: 700;
}

button:hover:not(:disabled),
.batch-list button.active {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.batch-list button {
  display: grid;
  gap: 2px;
  text-align: left;
}

.batch-list small {
  color: var(--color-text-muted);
}

.table-scroll {
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 720px;
  border-collapse: collapse;
}

th,
td {
  border-bottom: 1px solid var(--color-border);
  padding: var(--space-3);
  text-align: left;
}

th {
  background: var(--color-surface-muted);
}

.empty-cell {
  color: var(--color-text-muted);
  text-align: center;
}

.notice {
  margin: 0;
  border-radius: var(--radius-md);
  padding: var(--space-3);
  background: var(--color-danger-soft);
  color: var(--color-danger);
  font-weight: 700;
}

summary {
  cursor: pointer;
  font-weight: 800;
}
</style>
