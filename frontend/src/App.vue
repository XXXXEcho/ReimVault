<script setup lang="ts">
import { useAuthStore } from './stores/auth';

const auth = useAuthStore();
</script>

<template>
  <el-container class="app-shell">
    <el-header>
      <strong>报销材料管理系统</strong>
      <nav>
        <RouterLink to="/reimbursements">我的报销</RouterLink>
        <template v-if="auth.user?.role === 'ADMIN'">
          <RouterLink to="/admin/reimbursements">报销管理</RouterLink>
          <RouterLink to="/admin/batches">批次管理</RouterLink>
          <RouterLink to="/admin/users">用户管理</RouterLink>
          <RouterLink to="/admin/categories">分类管理</RouterLink>
        </template>
      </nav>
    </el-header>
    <el-main><RouterView /></el-main>
  </el-container>
</template>

<style scoped>
.app-shell { min-height: 100vh; }
.el-header { display: flex; align-items: center; gap: 24px; border-bottom: 1px solid #ddd; }
nav { display: flex; gap: 12px; }
a { color: #1f66d1; text-decoration: none; }
</style>
