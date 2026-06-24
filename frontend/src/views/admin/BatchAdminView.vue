<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { addBatchItem, archiveBatch, createBatch, exportBatchAttachments, exportBatchExcel, exportFilteredAttachments, exportFilteredExcel, getBatch, listBatches, previewFilteredExport, removeBatchItem, type Batch } from '../../api/batches';
import { formatTime, statusLabel, type ReimbursementRecord } from '../../api/reimbursements';
import { listOaNumbers, type OaNumber } from '../../api/oa';

const batches = ref<Batch[]>([]);
const current = ref<Batch | null>(null);
const oaNumbers = ref<OaNumber[]>([]);
const form = reactive({ name: '', description: '' });
const batchId = ref<number | null>(null);
const recordId = ref<number | null>(null);
const selectedOaIds = ref<number[]>([]);
const selectedMonths = ref<string[]>([]);
const monthOptions = ref<string[]>([]);
const previewRecords = ref<ReimbursementRecord[]>([]);
const previewLoaded = ref(false);
const previewLoading = ref(false);
const previewError = ref('');
const hasExportFilter = computed(() => selectedOaIds.value.length > 0 || selectedMonths.value.length > 0);
const canExportFiltered = computed(() => previewLoaded.value && previewRecords.value.length > 0);

async function loadBatches() {
  const response = await listBatches();
  batches.value = response.data;
}

async function saveBatch() {
  await createBatch({ name: form.name, description: form.description });
  form.name = '';
  form.description = '';
  await loadBatches();
}

async function loadBatch(id = batchId.value) {
  if (!id) return;
  const response = await getBatch(Number(id));
  current.value = response.data;
  batchId.value = response.data.id;
}

async function addRecord() {
  if (!batchId.value || !recordId.value) return;
  await addBatchItem(Number(batchId.value), Number(recordId.value));
  recordId.value = null;
  await loadBatch(batchId.value);
}

async function removeRecord(id: number) {
  if (!batchId.value) return;
  await removeBatchItem(Number(batchId.value), id);
  await loadBatch(batchId.value);
}

async function archiveCurrent() {
  if (!batchId.value || !confirm('确定要归档此批次？归档后不可撤销。')) return;
  await archiveBatch(Number(batchId.value));
  await loadBatch(batchId.value);
  await loadBatches();
}

function errorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '操作失败';
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

async function exportExcel() {
  if (!batchId.value) return;
  const response = await exportBatchExcel(Number(batchId.value));
  downloadBlob(response.data, `batch-${batchId.value}.xlsx`);
}

async function exportAttachments() {
  if (!batchId.value) return;
  const response = await exportBatchAttachments(Number(batchId.value));
  downloadBlob(response.data, `batch-${batchId.value}-attachments.zip`);
}

function clearExportPreview() {
  previewLoaded.value = false;
  previewRecords.value = [];
  previewError.value = '';
}

async function previewExport() {
  if (!hasExportFilter.value) return;
  previewLoading.value = true;
  previewError.value = '';
  try {
    const response = await previewFilteredExport(selectedOaIds.value, selectedMonths.value);
    previewRecords.value = response.data;
    previewLoaded.value = true;
  } catch (err) {
    previewRecords.value = [];
    previewLoaded.value = false;
    previewError.value = errorMessage(err);
  } finally {
    previewLoading.value = false;
  }
}

async function doExportFilteredExcel() {
  if (!canExportFiltered.value) return;
  const response = await exportFilteredExcel(selectedOaIds.value, selectedMonths.value);
  downloadBlob(response.data, 'export.xlsx');
}

async function doExportFilteredAttachments() {
  if (!canExportFiltered.value) return;
  const response = await exportFilteredAttachments(selectedOaIds.value, selectedMonths.value);
  downloadBlob(response.data, 'export-attachments.zip');
}

onMounted(async () => {
  await loadBatches();
  const oaResponse = await listOaNumbers();
  oaNumbers.value = oaResponse.data;
  const now = new Date();
  const months: string[] = [];
  for (let i = 0; i < 12; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    months.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }
  monthOptions.value = months;
});
</script>

<template>
  <section>
    <h1>批次管理</h1>

    <section class="page-section filter-section">
      <div class="export-layout">
        <div>
          <h2 class="section-title">批量导出</h2>
          <p class="section-desc">先查询并核对命中记录，再导出 Excel 或附件压缩包。</p>
          <div class="filter-row">
            <div class="multi-select-wrapper">
              <span class="multi-label">经费编码</span>
              <div class="multi-dropdown">
                <label v-for="oa in oaNumbers" :key="oa.id" class="multi-option">
                  <input type="checkbox" :value="oa.id" v-model="selectedOaIds" @change="clearExportPreview" />
                  {{ oa.number }}
                </label>
                <p v-if="!oaNumbers.length" class="empty-hint">暂无经费编码</p>
              </div>
            </div>
            <div class="multi-select-wrapper">
              <span class="multi-label">月份</span>
              <div class="multi-dropdown">
                <label v-for="m in monthOptions" :key="m" class="multi-option">
                  <input type="checkbox" :value="m" v-model="selectedMonths" @change="clearExportPreview" />
                  {{ m }}
                </label>
              </div>
            </div>
          </div>
        </div>
        <aside class="export-summary">
          <p v-if="previewError" class="export-error">{{ previewError }}</p>
          <p v-else-if="!hasExportFilter" class="export-hint">请选择筛选条件后查询结果</p>
          <p v-else-if="!previewLoaded" class="export-hint">请先查询并确认导出范围</p>
          <p v-else class="export-count">查询结果：{{ previewRecords.length }} 条</p>
          <button data-test="preview-filtered-export" type="button" class="btn-preview" :disabled="!hasExportFilter || previewLoading" @click="previewExport">{{ previewLoading ? '查询中...' : '查询结果' }}</button>
          <div class="filter-actions">
            <button data-test="export-filtered-excel" type="button" class="btn-export" :disabled="!canExportFiltered" @click="doExportFilteredExcel">导出当前结果 Excel</button>
            <button data-test="export-filtered-attachments" type="button" class="btn-export" :disabled="!canExportFiltered" @click="doExportFilteredAttachments">下载当前结果附件包</button>
          </div>
        </aside>
      </div>
      <div v-if="previewLoaded" class="preview-panel">
        <p v-if="!previewRecords.length" class="empty-hint">没有匹配记录，请调整筛选条件。</p>
        <table v-else>
          <thead><tr><th>员工</th><th>金额</th><th>分类</th><th>用途</th><th>经费编码</th><th>支付时间</th><th>状态</th></tr></thead>
          <tbody><tr v-for="record in previewRecords" :key="record.id"><td>{{ record.employeeName }}</td><td>{{ record.amount }}</td><td>{{ record.categoryName }}</td><td>{{ record.purpose }}</td><td>{{ record.oaNumber || '—' }}</td><td>{{ formatTime(record.paymentTime) }}</td><td>{{ statusLabel(record.status, record.reimbursedAt) }}</td></tr></tbody>
        </table>
      </div>
    </section>

    <section class="page-section">
      <h2 class="section-title">创建批次</h2>
      <form class="inline-form" @submit.prevent="saveBatch">
        <input aria-label="批次名称" v-model="form.name" placeholder="批次名称" />
        <input aria-label="批次描述" v-model="form.description" placeholder="批次描述" />
        <button type="submit">创建批次</button>
      </form>
    </section>

    <section class="page-section">
      <h2 class="section-title">批次列表</h2>
      <table>
        <thead><tr><th>ID</th><th>名称</th><th>说明</th><th>归档时间</th><th>操作</th></tr></thead>
        <tbody><tr v-for="batch in batches" :key="batch.id"><td>{{ batch.id }}</td><td>{{ batch.name }}</td><td>{{ batch.description }}</td><td>{{ formatTime(batch.archivedAt) }}</td><td><button @click="loadBatch(batch.id)">查看详情</button></td></tr></tbody>
      </table>
    </section>

    <section v-if="current" class="page-section current-section">
      <div class="current-header">
        <h2 class="section-title">当前批次：{{ current.name }}</h2>
        <div class="current-actions">
          <button data-test="export-excel" type="button" class="btn-secondary" @click="exportExcel">导出当前批次 Excel</button>
          <button data-test="export-attachments" type="button" class="btn-secondary" @click="exportAttachments">下载当前批次附件压缩包</button>
          <button data-test="archive-batch" type="button" class="btn-danger" @click="archiveCurrent">归档当前批次</button>
        </div>
      </div>
      <table>
        <thead><tr><th>记录ID</th><th>员工</th><th>分类</th><th>经费编码</th><th>操作</th></tr></thead>
        <tbody><tr v-for="item in current.items" :key="item.id"><td>{{ item.recordId }}</td><td>{{ item.employeeName }}</td><td>{{ item.categoryName }}</td><td>{{ item.oaNumber || '—' }}</td><td><button class="btn-danger" @click="removeRecord(item.recordId)">移除</button></td></tr></tbody>
      </table>
    </section>

    <section class="page-section advanced-section">
      <h2 class="section-title">高级操作：按 ID 维护批次</h2>
      <form class="inline-form" @submit.prevent="loadBatch()">
        <input aria-label="批次ID" v-model="batchId" type="number" placeholder="批次ID" />
        <button data-test="load-batch" type="button" @click="loadBatch()">加载批次</button>
        <input aria-label="报销记录ID" v-model="recordId" type="number" placeholder="报销记录ID" />
        <button data-test="add-record" type="button" @click="addRecord">加入批次</button>
      </form>
    </section>
  </section>
</template>

<style scoped>
.inline-form { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 0; }
.page-section { padding: 16px; margin-bottom: 18px; border: 1px solid #e5e7eb; border-radius: 14px; background: #fff; }
.section-title { margin: 0 0 12px; font-size: 16px; color: #1e293b; }
.section-desc { margin: -4px 0 14px; color: #64748b; font-size: 13px; }
.filter-section { background: linear-gradient(135deg, #f8fafc 0%, #eff6ff 100%); border-color: #bfdbfe; }
.export-layout { display: grid; grid-template-columns: minmax(0, 1fr) 280px; gap: 18px; align-items: start; }
.filter-row { display: flex; gap: 16px; flex-wrap: wrap; }
.multi-select-wrapper { min-width: 180px; flex: 1; }
.multi-label { display: block; margin-bottom: 6px; color: #334155; font-size: 12px; font-weight: 800; }
.multi-dropdown { max-height: 172px; overflow-y: auto; padding: 8px; border: 1px solid #dbe3ef; border-radius: 12px; background: rgba(255,255,255,.9); box-shadow: inset 0 1px 0 rgba(255,255,255,.7); }
.multi-option { display: flex; align-items: center; gap: 8px; padding: 7px 8px; font-size: 13px; cursor: pointer; border-radius: 8px; color: #334155; }
.multi-option:hover { background: #eaf2ff; }
.empty-hint { margin: 4px 0; color: #94a3b8; font-size: 12px; }
.export-summary { padding: 14px; border: 1px solid #dbeafe; border-radius: 14px; background: #fff; }
.export-hint { margin: 0 0 12px; color: #b45309; font-size: 12px; font-weight: 700; }
.export-error { margin: 0 0 12px; color: #dc2626; font-size: 12px; font-weight: 800; }
.export-count { margin: 0 0 12px; color: #166534; font-size: 14px; font-weight: 800; }
.filter-actions { display: flex; gap: 10px; margin-top: 12px; flex-wrap: wrap; }
.preview-panel { max-height: 420px; margin-top: 16px; padding-top: 16px; border-top: 1px solid #dbeafe; overflow: auto; }
.current-header { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; flex-wrap: wrap; margin-bottom: 12px; }
.current-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.advanced-section { background: #f8fafc; border-style: dashed; }
.btn-preview { width: 100%; min-height: 38px; padding: 0 16px; border: 0; border-radius: 10px; background: #2563eb; color: #fff; font-size: 13px; font-weight: 800; cursor: pointer; }
.btn-preview:disabled { background: #cbd5e1; cursor: not-allowed; }
.btn-export { min-height: 36px; padding: 0 16px; border: 1px solid #93c5fd; border-radius: 8px; background: #fff; color: #2563eb; font-size: 13px; font-weight: 700; cursor: pointer; }
.btn-export:hover:not(:disabled) { background: #2563eb; color: #fff; }
.btn-export:disabled { border-color: #cbd5e1; color: #94a3b8; cursor: not-allowed; }
@media (max-width: 900px) { .export-layout { grid-template-columns: 1fr; } }
.btn-secondary { background: #f0f4ff !important; color: #2563eb !important; font-size: 12px !important; }
.btn-secondary:hover { background: #2563eb !important; color: #fff !important; }
.btn-danger { min-height: 34px; padding: 0 12px; border: 1px solid #fca5a5; border-radius: 8px; background: #fff; color: #dc2626; font-size: 12px; font-weight: 700; cursor: pointer; transition: background 160ms ease, color 160ms ease; }
.btn-danger:hover { background: #dc2626; color: #fff; }
</style>
