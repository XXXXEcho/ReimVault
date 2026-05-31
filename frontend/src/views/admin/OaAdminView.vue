<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { listAdminReimbursements, updateOaNumber, type ReimbursementRecord, type ReimbursementStatus, formatTime } from '../../api/reimbursements';

const records = ref<ReimbursementRecord[]>([]);
const oaNumbers = reactive<Record<number, string>>({});
const notice = ref<{ type: 'success' | 'error'; text: string } | null>(null);
const statusFilter = ref<ReimbursementStatus | ''>('');
const oaFilter = ref<'' | 'yes' | 'no'>('');

function errorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '操作失败';
}

async function load() {
  const response = await listAdminReimbursements({
    status: (statusFilter.value || undefined) as ReimbursementStatus | undefined
  });
  let list = response.data;
  if (oaFilter.value === 'yes') list = list.filter(r => r.oaNumber);
  else if (oaFilter.value === 'no') list = list.filter(r => !r.oaNumber);
  records.value = list;
  for (const r of records.value) oaNumbers[r.id] = r.oaNumber ?? '';
}

async function save(id: number) {
  notice.value = null;
  try {
    await updateOaNumber(id, oaNumbers[id] ?? '');
    notice.value = { type: 'success', text: '保存成功' };
    await load();
  } catch (err) {
    notice.value = { type: 'error', text: errorMessage(err) };
  }
}

async function clear(id: number) {
  notice.value = null;
  try {
    await updateOaNumber(id, '');
    notice.value = { type: 'success', text: '已删除' };
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
      <select aria-label="状态" v-model="statusFilter">
        <option value="">全部状态</option>
        <option value="SUBMITTED">已提交未报销</option>
        <option value="ARCHIVED">已报销</option>
      </select>
      <select aria-label="OA编号" v-model="oaFilter">
        <option value="">全部</option>
        <option value="no">未分配</option>
        <option value="yes">已分配</option>
      </select>
      <button type="submit">查询</button>
    </form>
    <div class="table-scroll">
      <table>
        <thead><tr><th>ID</th><th>员工</th><th>金额</th><th>用途</th><th>支付时间</th><th>OA编号</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="r in records" :key="r.id">
            <td>{{ r.id }}</td>
            <td>{{ r.employeeName }}</td>
            <td>{{ r.amount }}</td>
            <td>{{ r.purpose }}</td>
            <td>{{ formatTime(r.paymentTime) }}</td>
            <td><input class="oa-input" :aria-label="`OA${r.id}`" v-model="oaNumbers[r.id]" placeholder="输入OA编号" /></td>
            <td class="row-actions">
              <button @click="save(r.id)">保存</button>
              <button v-if="r.oaNumber" class="btn-danger" @click="clear(r.id)">删除</button>
            </td>
          </tr>
          <tr v-if="!records.length"><td colspan="7" class="empty">无记录</td></tr>
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
.row-actions { display: flex; gap: 6px; align-items: center; }
.btn-danger { min-height: 34px; padding: 0 12px; border: 1px solid #fca5a5; border-radius: 8px; background: #fff; color: #dc2626; font-size: 12px; font-weight: 700; cursor: pointer; transition: background 160ms ease, color 160ms ease; }
.btn-danger:hover { background: #dc2626; color: #fff; }
.empty { text-align: center; color: #94a3b8; padding: 32px 12px !important; }
</style>
