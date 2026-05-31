<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { listAdminReimbursements, rejectReimbursement, updateAdminRemark, updateOaNumber as updateOa, markReimbursed, unreimburse, type ReimbursementRecord, type ReimbursementStatus, statusLabel, formatTime } from '../../api/reimbursements';
import { listOaNumbers, type OaNumber } from '../../api/oa';
import { addBatchItem, ensureMonthlyBatch, type Batch } from '../../api/batches';

const records = ref<ReimbursementRecord[]>([]);
const oaNumbers = ref<OaNumber[]>([]);
const filters = reactive({ employeeId: '', categoryId: '', status: 'SUBMITTED', from: '', to: '', reimbursed: false });
const remarks = reactive<Record<number, string>>({});
const oaIds = reactive<Record<number, number | null>>({});
const monthlyBatch = ref<Batch | null>(null);
const notice = ref<{ type: 'success' | 'error'; text: string } | null>(null);

function filterParams() {
  return {
    employeeId: filters.employeeId ? Number(filters.employeeId) : undefined,
    categoryId: filters.categoryId ? Number(filters.categoryId) : undefined,
    status: (filters.status || undefined) as ReimbursementStatus | undefined,
    from: filters.from || undefined,
    to: filters.to || undefined,
    reimbursed: filters.reimbursed || undefined
  };
}

function errorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '操作失败';
}

async function load() {
  const response = await listAdminReimbursements(filterParams());
  records.value = response.data;
  for (const record of records.value) {
    remarks[record.id] = record.adminRemark ?? '';
    oaIds[record.id] = record.oaId ?? null;
  }
}

async function saveRemark(id: number) {
  await updateAdminRemark(id, remarks[id] ?? '');
  await load();
}

async function saveOaNumber(id: number) {
  await updateOa(id, oaIds[id] ?? null);
  await load();
}

async function initMonthlyBatch() {
  try {
    const response = await ensureMonthlyBatch();
    monthlyBatch.value = response.data;
  } catch {
    // batch may already exist, ignore
  }
}

async function addToMonthlyBatch(recordId: number) {
  if (!monthlyBatch.value) return;
  notice.value = null;
  try {
    await addBatchItem(monthlyBatch.value.id, recordId);
    notice.value = { type: 'success', text: '已加入月度批次' };
    await load();
  } catch (err) {
    notice.value = { type: 'error', text: errorMessage(err) };
  }
}

async function rejectRecord(id: number) {
  if (!confirm('确定要打回这条记录吗？打回后员工可重新编辑。')) return;
  notice.value = null;
  try {
    await rejectReimbursement(id);
    notice.value = { type: 'success', text: '已打回' };
    await load();
  } catch (err) {
    notice.value = { type: 'error', text: errorMessage(err) };
  }
}

async function markAsReimbursed(id: number) {
  notice.value = null;
  try {
    await markReimbursed(id);
    notice.value = { type: 'success', text: '已标记为报销完成' };
    await load();
  } catch (err) {
    notice.value = { type: 'error', text: errorMessage(err) };
  }
}

async function undoReimbursed(id: number) {
  notice.value = null;
  try {
    await unreimburse(id);
    notice.value = { type: 'success', text: '已撤销报销' };
    await load();
  } catch (err) {
    notice.value = { type: 'error', text: errorMessage(err) };
  }
}

onMounted(async () => {
  const oaResponse = await listOaNumbers();
  oaNumbers.value = oaResponse.data;
  await Promise.all([load(), initMonthlyBatch()]);
});
</script>

<template>
  <section>
    <h1>报销管理</h1>
    <p v-if="notice" :class="['notice', notice.type]" :role="notice.type === 'error' ? 'alert' : 'status'">{{ notice.text }}</p>
    <form class="inline-form" @submit.prevent="load">
      <input aria-label="员工ID" v-model="filters.employeeId" type="number" min="1" placeholder="员工ID" />
      <input aria-label="分类ID" v-model="filters.categoryId" type="number" min="1" placeholder="分类ID" />
      <select aria-label="状态" v-model="filters.status"><option value="SUBMITTED">已提交未报销</option><option value="ARCHIVED">已报销</option><option value="DRAFT">未提交</option></select>
      <input aria-label="开始日期" v-model="filters.from" type="date" />
      <input aria-label="结束日期" v-model="filters.to" type="date" />
      <button type="button" :class="{ active: filters.reimbursed }" @click="filters.reimbursed = !filters.reimbursed; load()">{{ filters.reimbursed ? '显示全部' : '只看已报销' }}</button>
      <button type="submit">筛选</button>
    </form>
    <div class="table-scroll">
    <table>
      <thead><tr><th>员工</th><th>金额</th><th>用途分类</th><th>用途说明</th><th>支付时间</th><th>经费编码</th><th>状态</th><th>报销时间</th><th>批次</th><th>备注</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="record in records" :key="record.id">
          <td>{{ record.employeeName }}</td>
          <td>{{ record.amount }}</td>
          <td>{{ record.categoryName }}</td>
          <td>{{ record.purpose }}</td>
          <td>{{ formatTime(record.paymentTime) }}</td>
          <td><select class="oa-select" :aria-label="`经费编码${record.id}`" v-model.number="oaIds[record.id]"><option :value="null">未分配</option><option v-for="oa in oaNumbers" :key="oa.id" :value="oa.id">{{ oa.number }}</option></select><button class="btn-oa-save" @click="saveOaNumber(record.id)">保存</button></td>
          <td><span class="status-tag" :class="record.reimbursedAt ? 'reimbursed' : record.status.toLowerCase()">{{ statusLabel(record.status, record.reimbursedAt) }}</span></td>
          <td>{{ formatTime(record.reimbursedAt) }}</td>
          <td>{{ record.batchName ?? '—' }}</td>
          <td><input class="remark-input" :aria-label="`备注${record.id}`" v-model="remarks[record.id]" /></td>
          <td class="row-actions">
            <RouterLink :to="`/admin/reimbursements/${record.id}`" class="link-view">查看</RouterLink>
            <button @click="saveRemark(record.id)">保存备注</button>
            <button v-if="!record.batchId && record.status === 'SUBMITTED' && monthlyBatch" class="btn-secondary" @click="addToMonthlyBatch(record.id)">加入月度批次</button>
            <button v-if="record.status === 'SUBMITTED' && !record.batchId" class="btn-warning" @click="rejectRecord(record.id)">打回</button>
            <button v-if="record.status === 'SUBMITTED' && !record.reimbursedAt" class="btn-success" @click="markAsReimbursed(record.id)">已报销</button>
            <button v-if="record.reimbursedAt" class="btn-undo" @click="undoReimbursed(record.id)">撤销报销</button>
          </td>
        </tr>
      </tbody>
    </table>
    </div>
  </section>
</template>

<style scoped>
.inline-form { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 18px; }
.notice { margin: 0 0 14px; padding: 10px 14px; border-radius: 10px; font-size: 13px; font-weight: 700; }
.notice.success { background: #dcfce7; color: #166534; }
.notice.error { background: #fee2e2; color: #991b1b; }
.status-tag { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 11px; font-weight: 800; letter-spacing: .4px; }
.status-tag.submitted { background: #dbeafe; color: #1d4ed8; }
.status-tag.reimbursed { background: #dcfce7; color: #166534; }
.status-tag.archived { background: #f3f4f6; color: #6b7280; }
.status-tag.draft { background: #fef3c7; color: #b45309; }
.table-scroll { overflow-x: auto; }
.remark-input { width: 100%; min-width: 120px; }
.oa-select { min-width: 80px; }
.btn-oa-save { margin-left: 4px; min-height: 30px; padding: 0 8px; border: 1px solid #c4b5fd; border-radius: 6px; background: #fff; color: #7c3aed; font-size: 11px; font-weight: 700; cursor: pointer; }
.btn-oa-save:hover { background: #7c3aed; color: #fff; }
.row-actions { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
.btn-secondary { background: #f0f4ff !important; color: #2563eb !important; font-size: 12px !important; }
.btn-secondary:hover { background: #2563eb !important; color: #fff !important; }
.link-view { display: inline-flex; align-items: center; min-height: 34px; padding: 0 12px; border-radius: 8px; background: #f0f4ff; color: #2563eb; font-size: 12px; font-weight: 700; text-decoration: none; transition: background 160ms ease; }
.link-view:hover { background: #2563eb; color: #fff; }
.btn-warning { min-height: 34px; padding: 0 12px; border: 1px solid #fcd34d; border-radius: 8px; background: #fff; color: #b45309; font-size: 12px; font-weight: 700; cursor: pointer; transition: background 160ms ease, color 160ms ease; }
.btn-warning:hover { background: #b45309; color: #fff; }
.btn-success { min-height: 34px; padding: 0 12px; border: 1px solid #86efac; border-radius: 8px; background: #fff; color: #16a34a; font-size: 12px; font-weight: 700; cursor: pointer; transition: background 160ms ease, color 160ms ease; }
.btn-success:hover { background: #16a34a; color: #fff; }
.btn-undo { min-height: 34px; padding: 0 12px; border: 1px solid #fca5a5; border-radius: 8px; background: #fff; color: #dc2626; font-size: 12px; font-weight: 700; cursor: pointer; transition: background 160ms ease, color 160ms ease; }
.btn-undo:hover { background: #dc2626; color: #fff; }
</style>
