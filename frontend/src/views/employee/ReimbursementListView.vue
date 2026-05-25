<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { deleteReimbursement, listReimbursements, type ReimbursementRecord } from '../../api/reimbursements';

const records = ref<ReimbursementRecord[]>([]);

async function load() {
  const response = await listReimbursements();
  records.value = response.data;
}

async function remove(id: number) {
  if (!confirm('确定要删除这条报销记录吗？')) return;
  await deleteReimbursement(id);
  await load();
}

onMounted(load);
</script>

<template>
  <section>
    <div class="toolbar"><RouterLink to="/reimbursements/new">新建报销</RouterLink></div>
    <table>
      <thead><tr><th>金额</th><th>用途分类</th><th>用途说明</th><th>支付时间</th><th>状态</th><th>提交时间</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="record in records" :key="record.id">
          <td>{{ record.amount }}</td>
          <td>{{ record.categoryName }}</td>
          <td>{{ record.purpose }}</td>
          <td>{{ record.paymentTime }}</td>
          <td>{{ record.status }}</td>
          <td>{{ record.submittedAt }}</td>
          <td class="actions">
            <RouterLink v-if="record.status === 'DRAFT'" :to="`/reimbursements/${record.id}`">编辑</RouterLink>
            <RouterLink v-else :to="`/reimbursements/${record.id}`">查看</RouterLink>
            <button v-if="record.status === 'DRAFT'" class="delete-btn" @click="remove(record.id)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.toolbar { margin-bottom: 16px; }
table { width: 100%; border-collapse: collapse; }
th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
.actions { display: flex; gap: 8px; align-items: center; }
.delete-btn { background: none; border: 0; color: #b00020; cursor: pointer; padding: 0; }
.delete-btn:hover { text-decoration: underline; }
</style>
