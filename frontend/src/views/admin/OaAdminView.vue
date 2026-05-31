<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { listAdminReimbursements, updateOaNumber, type ReimbursementRecord, type ReimbursementStatus, statusLabel, formatTime } from '../../api/reimbursements';

const records = ref<ReimbursementRecord[]>([]);
const oaNumbers = reactive<Record<number, string>>({});
const notice = ref<{ type: 'success' | 'error'; text: string } | null>(null);
const filters = reactive({ status: 'SUBMITTED' as ReimbursementStatus | '', oaFilter: '' as string });

function errorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '操作失败';
}

async function load() {
  const response = await listAdminReimbursements({
    status: (filters.status || undefined) as ReimbursementStatus | undefined
  });
  let list = response.data;
  if (filters.oaFilter === 'assigned') list = list.filter(r => r.oaNumber);
  else if (filters.oaFilter === 'unassigned') list = list.filter(r => !r.oaNumber);
  records.value = list;
  for (const record of records.value) oaNumbers[record.id] = record.oaNumber ?? '';
}

async function saveOaNumber(id: number) {
  notice.value = null;
  try {
    await updateOaNumber(id, oaNumbers[id] ?? '');
    notice.value = { type: 'success', text: 'OA编号已保存' };
    await load();
  } catch (err) {
    notice.value = { type: 'error', text: errorMessage(err) };
  }
}

onMounted(load);
</script>

<template>
  <section>
    <h1>OA编号管理</h1>
    <p v-if="notice" :class="['notice', notice.type]" :role="notice.type === 'error' ? 'alert' : 'status'">{{ notice.text }}</p>
    <form class="inline-form" @submit.prevent="load">
      <select aria-label="状态" v-model="filters.status">
        <option value="">全部状态</option>
        <option value="SUBMITTED">已提交未报销</option>
        <option value="ARCHIVED">已报销</option>
        <option value="DRAFT">未提交</option>
      </select>
      <select aria-label="OA编号筛选" v-model="filters.oaFilter">
        <option value="">全部记录</option>
        <option value="unassigned">未分配OA</option>
        <option value="assigned">已分配OA</option>
      </select>
      <button type="submit">筛选</button>
    </form>
    <div class="table-scroll">
      <table>
        <thead><tr><th>ID</th><th>员工</th><th>金额</th><th>用途</th><th>状态</th><th>支付时间</th><th>OA编号</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="record in records" :key="record.id">
            <td>{{ record.id }}</td>
            <td>{{ record.employeeName }}</td>
            <td>{{ record.amount }}</td>
            <td>{{ record.purpose }}</td>
            <td><span class="status-tag" :class="record.reimbursedAt ? 'reimbursed' : record.status.toLowerCase()">{{ statusLabel(record.status, record.reimbursedAt) }}</span></td>
            <td>{{ formatTime(record.paymentTime) }}</td>
            <td><input class="oa-input" :aria-label="`OA${record.id}`" v-model="oaNumbers[record.id]" placeholder="输入OA编号" /></td>
            <td class="row-actions">
              <RouterLink :to="`/admin/reimbursements/${record.id}`" class="link-view">查看</RouterLink>
              <button @click="saveOaNumber(record.id)">保存</button>
            </td>
          </tr>
          <tr v-if="!records.length"><td colspan="8" class="empty">无记录</td></tr>
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
.table-scroll { overflow-x: auto; }
.oa-input { width: 100%; min-width: 140px; }
.status-tag { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 11px; font-weight: 800; letter-spacing: .4px; white-space: nowrap; }
.status-tag.submitted { background: #dbeafe; color: #1d4ed8; }
.status-tag.reimbursed { background: #dcfce7; color: #166534; }
.status-tag.archived { background: #f3f4f6; color: #6b7280; }
.status-tag.draft { background: #fef3c7; color: #b45309; }
.row-actions { display: flex; gap: 6px; align-items: center; flex-wrap: nowrap; }
.link-view { display: inline-flex; align-items: center; min-height: 34px; padding: 0 12px; border-radius: 8px; background: #f0f4ff; color: #2563eb; font-size: 12px; font-weight: 700; text-decoration: none; transition: background 160ms ease; }
.link-view:hover { background: #2563eb; color: #fff; }
.empty { text-align: center; color: #94a3b8; padding: 32px 12px !important; }
</style>
