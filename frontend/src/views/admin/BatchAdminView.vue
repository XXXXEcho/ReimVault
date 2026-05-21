<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { addBatchItem, archiveBatch, createBatch, exportBatchAttachments, exportBatchExcel, getBatch, listBatches, removeBatchItem, type Batch } from '../../api/batches';

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
  await loadBatch(batchId.value);
}

async function removeRecord(id: number) {
  if (!batchId.value) return;
  await removeBatchItem(Number(batchId.value), id);
  await loadBatch(batchId.value);
}

async function archiveCurrent() {
  if (!batchId.value) return;
  await archiveBatch(Number(batchId.value));
  await loadBatch(batchId.value);
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
  <section class="batch-admin-page">
    <h1>批次管理</h1>

    <section class="enterprise-card batch-card" aria-labelledby="create-batch-title">
      <h2 id="create-batch-title">创建批次</h2>
      <form class="admin-form" @submit.prevent="saveBatch">
        <input aria-label="批次名称" v-model="form.name" placeholder="批次名称" />
        <input aria-label="批次描述" v-model="form.description" placeholder="批次描述" />
        <button type="submit">创建批次</button>
      </form>
    </section>

    <section class="enterprise-card batch-card" aria-labelledby="batch-actions-title">
      <h2 id="batch-actions-title">批次操作</h2>
      <form class="admin-form" @submit.prevent="loadBatch()">
        <input aria-label="批次ID" v-model="batchId" type="number" placeholder="批次ID" />
        <button data-test="load-batch" type="submit">加载批次</button>
        <input aria-label="报销记录ID" v-model="recordId" type="number" placeholder="报销记录ID" />
        <button data-test="add-record" type="button" @click="addRecord">加入批次</button>
        <button data-test="export-excel" type="button" @click="exportExcel">导出 Excel</button>
        <button data-test="export-attachments" type="button" @click="exportAttachments">导出附件</button>
        <button data-test="archive-batch" type="button" @click="archiveCurrent">归档</button>
      </form>
    </section>

    <section class="enterprise-card batch-card" aria-labelledby="batch-list-title">
      <h2 id="batch-list-title">批次列表</h2>
      <div class="table-scroll">
        <table>
          <thead><tr><th>ID</th><th>名称</th><th>说明</th><th>归档时间</th><th>操作</th></tr></thead>
          <tbody><tr v-for="batch in batches" :key="batch.id"><td>{{ batch.id }}</td><td>{{ batch.name }}</td><td>{{ batch.description }}</td><td>{{ batch.archivedAt }}</td><td><button @click="loadBatch(batch.id)">查看</button></td></tr></tbody>
        </table>
      </div>
    </section>

    <section class="enterprise-card batch-card" aria-labelledby="batch-detail-title">
      <h2 id="batch-detail-title">批次明细</h2>
      <div class="table-scroll">
        <table>
          <thead><tr><th>记录ID</th><th>员工</th><th>分类</th><th>操作</th></tr></thead>
          <tbody><tr v-for="item in current?.items ?? []" :key="item.id"><td>{{ item.recordId }}</td><td>{{ item.employeeName }}</td><td>{{ item.categoryName }}</td><td><button @click="removeRecord(item.recordId)">移除</button></td></tr></tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<style scoped>
.batch-admin-page {
  display: grid;
  gap: var(--space-5);
}

.batch-card {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-5);
}

.batch-card h2 {
  margin: 0;
}

.admin-form {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

input {
  min-height: 40px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
}

button {
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  color: var(--color-primary-strong);
  background: var(--color-surface);
  cursor: pointer;
}

button:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
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
  border: 1px solid var(--color-border);
  padding: var(--space-2) var(--space-3);
}

th {
  background: var(--color-surface-muted);
}
</style>
