<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { listReimbursements, type ReimbursementRecord } from '../../api/reimbursements';

const records = ref<ReimbursementRecord[]>([]);

onMounted(async () => {
  const response = await listReimbursements();
  records.value = response.data;
});
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
          <td><RouterLink :to="`/reimbursements/${record.id}`">编辑</RouterLink></td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.toolbar { margin-bottom: 16px; }
table { width: 100%; border-collapse: collapse; }
th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
</style>
