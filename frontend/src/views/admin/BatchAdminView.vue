<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { addBatchItem, archiveBatch, createBatch, exportBatchAttachments, exportBatchExcel, exportFilteredAttachments, exportFilteredExcel, getBatch, listBatches, removeBatchItem, type Batch } from '../../api/batches';
import { formatTime } from '../../api/reimbursements';
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

async function doExportFilteredExcel() {
  const response = await exportFilteredExcel(selectedOaIds.value, selectedMonths.value);
  downloadBlob(response.data, 'export.xlsx');
}

async function doExportFilteredAttachments() {
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
    <form class="inline-form" @submit.prevent="saveBatch">
      <input aria-label="批次名称" v-model="form.name" placeholder="批次名称" />
      <input aria-label="批次描述" v-model="form.description" placeholder="批次描述" />
      <button type="submit">创建批次</button>
    </form>

    <div class="filter-section">
      <strong>筛选导出</strong>
      <div class="filter-row">
        <div class="multi-select-wrapper">
          <span class="multi-label">经费编码</span>
          <div class="multi-dropdown">
            <label v-for="oa in oaNumbers" :key="oa.id" class="multi-option">
              <input type="checkbox" :value="oa.id" v-model="selectedOaIds" />
              {{ oa.number }}
            </label>
            <p v-if="!oaNumbers.length" class="empty-hint">暂无经费编码</p>
          </div>
        </div>
        <div class="multi-select-wrapper">
          <span class="multi-label">月份</span>
          <div class="multi-dropdown">
            <label v-for="m in monthOptions" :key="m" class="multi-option">
              <input type="checkbox" :value="m" v-model="selectedMonths" />
              {{ m }}
            </label>
          </div>
        </div>
      </div>
      <div class="filter-actions">
        <button type="button" class="btn-export" @click="doExportFilteredExcel">导出 Excel</button>
        <button type="button" class="btn-export" @click="doExportFilteredAttachments">导出附件</button>
      </div>
    </div>

    <form class="inline-form" @submit.prevent="loadBatch()">
      <input aria-label="批次ID" v-model="batchId" type="number" placeholder="批次ID" />
      <button data-test="load-batch" type="submit">加载批次</button>
      <input aria-label="报销记录ID" v-model="recordId" type="number" placeholder="报销记录ID" />
      <button data-test="add-record" type="button" @click="addRecord">加入批次</button>
      <button data-test="export-excel" type="button" class="btn-secondary" @click="exportExcel">导出 Excel</button>
      <button data-test="export-attachments" type="button" class="btn-secondary" @click="exportAttachments">导出附件</button>
      <button data-test="archive-batch" type="button" class="btn-danger" @click="archiveCurrent">归档</button>
    </form>
    <h2 class="section-title">批次列表</h2>
    <table>
      <thead><tr><th>ID</th><th>名称</th><th>说明</th><th>归档时间</th><th>操作</th></tr></thead>
      <tbody><tr v-for="batch in batches" :key="batch.id"><td>{{ batch.id }}</td><td>{{ batch.name }}</td><td>{{ batch.description }}</td><td>{{ formatTime(batch.archivedAt) }}</td><td><button @click="loadBatch(batch.id)">查看</button></td></tr></tbody>
    </table>
    <template v-if="current">
      <h2 class="section-title">批次明细 — {{ current.name }}</h2>
      <table>
        <thead><tr><th>记录ID</th><th>员工</th><th>分类</th><th>经费编码</th><th>操作</th></tr></thead>
        <tbody><tr v-for="item in current.items" :key="item.id"><td>{{ item.recordId }}</td><td>{{ item.employeeName }}</td><td>{{ item.categoryName }}</td><td>{{ item.oaNumber || '—' }}</td><td><button class="btn-danger" @click="removeRecord(item.recordId)">移除</button></td></tr></tbody>
      </table>
    </template>
  </section>
</template>

<style scoped>
.inline-form { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 18px; }
.section-title { margin: 22px 0 12px; font-size: 16px; color: #1e293b; }
.filter-section { padding: 16px; margin-bottom: 18px; border: 1px solid #e5e7eb; border-radius: 14px; background: #f8fafc; }
.filter-section > strong { display: block; margin-bottom: 12px; color: #1e293b; font-size: 14px; }
.filter-row { display: flex; gap: 16px; flex-wrap: wrap; }
.multi-select-wrapper { min-width: 160px; }
.multi-label { display: block; margin-bottom: 4px; color: #64748b; font-size: 12px; font-weight: 700; }
.multi-dropdown { max-height: 180px; overflow-y: auto; padding: 6px; border: 1px solid #dbe3ef; border-radius: 8px; background: #fff; }
.multi-option { display: flex; align-items: center; gap: 6px; padding: 4px 6px; font-size: 13px; cursor: pointer; border-radius: 4px; }
.multi-option:hover { background: #f0f4ff; }
.empty-hint { margin: 4px 0; color: #94a3b8; font-size: 12px; }
.filter-actions { display: flex; gap: 10px; margin-top: 12px; }
.btn-export { min-height: 36px; padding: 0 16px; border: 1px solid #93c5fd; border-radius: 8px; background: #fff; color: #2563eb; font-size: 13px; font-weight: 700; cursor: pointer; }
.btn-export:hover { background: #2563eb; color: #fff; }
.btn-secondary { background: #f0f4ff !important; color: #2563eb !important; font-size: 12px !important; }
.btn-secondary:hover { background: #2563eb !important; color: #fff !important; }
.btn-danger { min-height: 34px; padding: 0 12px; border: 1px solid #fca5a5; border-radius: 8px; background: #fff; color: #dc2626; font-size: 12px; font-weight: 700; cursor: pointer; transition: background 160ms ease, color 160ms ease; }
.btn-danger:hover { background: #dc2626; color: #fff; }
</style>
