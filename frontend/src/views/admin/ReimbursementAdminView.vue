<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { listAdminReimbursements, updateAdminRemark, type ReimbursementRecord, type ReimbursementStatus } from '../../api/reimbursements';

const records = ref<ReimbursementRecord[]>([]);
const filters = reactive({ employeeId: '', categoryId: '', status: 'SUBMITTED', from: '', to: '' });
const remarks = reactive<Record<number, string>>({});

function filterParams() {
  return {
    employeeId: filters.employeeId ? Number(filters.employeeId) : undefined,
    categoryId: filters.categoryId ? Number(filters.categoryId) : undefined,
    status: (filters.status || undefined) as ReimbursementStatus | undefined,
    from: filters.from || undefined,
    to: filters.to || undefined
  };
}

async function load() {
  const response = await listAdminReimbursements(filterParams());
  records.value = response.data;
  for (const record of records.value) remarks[record.id] = record.adminRemark ?? '';
}

async function saveRemark(id: number) {
  await updateAdminRemark(id, remarks[id] ?? '');
  await load();
}

onMounted(load);
</script>

<template>
  <section>
    <h1>报销管理</h1>
    <form class="filters" @submit.prevent="load">
      <input aria-label="员工ID" v-model="filters.employeeId" type="number" min="1" placeholder="员工ID" />
      <input aria-label="分类ID" v-model="filters.categoryId" type="number" min="1" placeholder="分类ID" />
      <select aria-label="状态" v-model="filters.status"><option value="SUBMITTED">已提交</option><option value="ARCHIVED">已归档</option><option value="DRAFT">草稿</option></select>
      <input aria-label="开始日期" v-model="filters.from" type="date" />
      <input aria-label="结束日期" v-model="filters.to" type="date" />
      <button type="submit">筛选</button>
    </form>
    <table>
      <thead><tr><th>员工</th><th>金额</th><th>用途分类</th><th>用途说明</th><th>支付时间</th><th>状态</th><th>管理员备注</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="record in records" :key="record.id">
          <td>{{ record.employeeName }}</td><td>{{ record.amount }}</td><td>{{ record.categoryName }}</td><td>{{ record.purpose }}</td><td>{{ record.paymentTime }}</td><td>{{ record.status }}</td>
          <td><input :aria-label="`备注${record.id}`" v-model="remarks[record.id]" /></td>
          <td><button @click="saveRemark(record.id)">保存备注</button></td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.filters { display: flex; gap: 10px; margin-bottom: 16px; }
table { width: 100%; border-collapse: collapse; }
th, td { border: 1px solid #ddd; padding: 8px; }
</style>
