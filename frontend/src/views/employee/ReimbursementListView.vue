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
          <td><span class="status-tag" :class="record.status.toLowerCase()">{{ record.status }}</span></td>
          <td>{{ record.submittedAt }}</td>
          <td class="row-actions">
            <RouterLink v-if="record.status === 'DRAFT'" :to="`/reimbursements/${record.id}`">编辑</RouterLink>
            <RouterLink v-else :to="`/reimbursements/${record.id}`" class="link-view">查看</RouterLink>
            <button v-if="record.status === 'DRAFT'" class="btn-danger" @click="remove(record.id)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.toolbar { margin-bottom: 18px; }
.status-tag { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 11px; font-weight: 800; letter-spacing: .4px; }
.status-tag.submitted { background: #dbeafe; color: #1d4ed8; }
.status-tag.archived { background: #f3f4f6; color: #6b7280; }
.status-tag.draft { background: #fef3c7; color: #b45309; }
.row-actions { display: flex; gap: 6px; align-items: center; }
.link-view { display: inline-flex; align-items: center; min-height: 34px; padding: 0 12px; border-radius: 8px; background: #f0f4ff; color: #2563eb; font-size: 12px; font-weight: 700; text-decoration: none; transition: background 160ms ease; }
.link-view:hover { background: #2563eb; color: #fff; }
.btn-danger { min-height: 34px; padding: 0 12px; border: 1px solid #fca5a5; border-radius: 8px; background: #fff; color: #dc2626; font-size: 12px; font-weight: 700; cursor: pointer; transition: background 160ms ease, color 160ms ease; }
.btn-danger:hover { background: #dc2626; color: #fff; }
</style>
