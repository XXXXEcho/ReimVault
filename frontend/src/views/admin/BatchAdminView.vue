<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { addBatchItem, archiveBatch, createBatch, exportBatchAttachments, exportBatchExcel, getBatch, listBatches, removeBatchItem, type Batch } from '../../api/batches';
import { formatTime } from '../../api/reimbursements';

const batches = ref<Batch[]>([]);
const current = ref<Batch | null>(null);
const form = reactive({ name: '', description: '' });
const batchId = ref<number | null>(null);
const recordId = ref<number | null>(null);

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

onMounted(loadBatches);
</script>

<template>
  <section>
    <h1>批次管理</h1>
    <form class="inline-form" @submit.prevent="saveBatch">
      <input aria-label="批次名称" v-model="form.name" placeholder="批次名称" />
      <input aria-label="批次描述" v-model="form.description" placeholder="批次描述" />
      <button type="submit">创建批次</button>
    </form>
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
        <thead><tr><th>记录ID</th><th>员工</th><th>分类</th><th>操作</th></tr></thead>
        <tbody><tr v-for="item in current.items" :key="item.id"><td>{{ item.recordId }}</td><td>{{ item.employeeName }}</td><td>{{ item.categoryName }}</td><td><button class="btn-danger" @click="removeRecord(item.recordId)">移除</button></td></tr></tbody>
      </table>
    </template>
  </section>
</template>

<style scoped>
.inline-form { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 18px; }
.section-title { margin: 22px 0 12px; font-size: 16px; color: #1e293b; }
.btn-secondary { background: #f0f4ff !important; color: #2563eb !important; font-size: 12px !important; }
.btn-secondary:hover { background: #2563eb !important; color: #fff !important; }
.btn-danger { min-height: 34px; padding: 0 12px; border: 1px solid #fca5a5; border-radius: 8px; background: #fff; color: #dc2626; font-size: 12px; font-weight: 700; cursor: pointer; transition: background 160ms ease, color 160ms ease; }
.btn-danger:hover { background: #dc2626; color: #fff; }
</style>
