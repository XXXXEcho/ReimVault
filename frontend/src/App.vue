<script setup lang="ts">
import { useRouter } from 'vue-router';
import AppBrand from './components/AppBrand.vue';
import { useAuthStore } from './stores/auth';

const auth = useAuthStore();
const router = useRouter();

const navigationItems = [
  { to: '/reimbursements', label: '我的报销' },
  { to: '/admin/reimbursements', label: '报销管理', adminOnly: true },
  { to: '/admin/batches', label: '批次管理', adminOnly: true },
  { to: '/admin/users', label: '用户管理', adminOnly: true },
  { to: '/admin/categories', label: '分类管理', adminOnly: true }
];

function canShow(item: { adminOnly?: boolean }) {
  return !item.adminOnly || auth.user?.role === 'ADMIN';
}

async function logout() {
  auth.logout();
  await router.push('/login');
}
</script>

<template>
  <div v-if="!auth.user" class="auth-shell">
    <AppBrand class="auth-brand" />
    <RouterView />
  </div>

  <div v-else class="workspace-shell">
    <aside class="workspace-sidebar" aria-label="主导航">
      <AppBrand class="sidebar-brand" />

      <nav class="sidebar-nav">
        <template v-for="item in navigationItems" :key="item.to">
          <RouterLink v-if="canShow(item)" :to="item.to">
            {{ item.label }}
          </RouterLink>
        </template>
      </nav>
    </aside>

    <section class="workspace-main">
      <header class="workspace-topbar">
        <div>
          <p class="topbar-eyebrow">企业报销材料工作台</p>
          <h1>材料齐备，归档无忧</h1>
        </div>
        <div class="topbar-user">
          <span>{{ auth.user.displayName }}</span>
          <small>{{ auth.user.department }}</small>
          <button type="button" aria-label="退出登录" @click="logout">退出</button>
        </div>
      </header>
      <main class="workspace-content">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<style scoped>
:global(body) {
  margin: 0;
  min-width: 320px;
  background: #f4f7fb;
  color: #111827;
  font-family: Inter, "Microsoft YaHei", "PingFang SC", Arial, sans-serif;
}

:global(*) {
  box-sizing: border-box;
}

.auth-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.18), transparent 32%),
    linear-gradient(135deg, #0f172a 0%, #1e3a8a 48%, #f4f7fb 48%);
}

.auth-brand {
  position: fixed;
  top: 28px;
  left: 32px;
  color: #fff;
}

.workspace-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
}

.workspace-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  padding: 24px 18px;
  background: #0f172a;
  color: #e5e7eb;
  box-shadow: 12px 0 28px rgba(15, 23, 42, 0.12);
}

.sidebar-nav {
  display: grid;
  gap: 8px;
  margin-top: 32px;
}

.sidebar-nav a {
  display: flex;
  align-items: center;
  min-height: 44px;
  padding: 0 14px;
  border-radius: 12px;
  color: #cbd5e1;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: background 180ms ease, color 180ms ease;
}

.sidebar-nav a:hover,
.sidebar-nav a.router-link-active {
  background: rgba(59, 130, 246, 0.18);
  color: #fff;
}

.workspace-main {
  min-width: 0;
  display: grid;
  grid-template-rows: auto 1fr;
}

.workspace-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 32px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.workspace-topbar h1,
.topbar-eyebrow {
  margin: 0;
}

.workspace-topbar h1 {
  margin-top: 4px;
  font-size: 22px;
  line-height: 1.3;
}

.topbar-eyebrow {
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.topbar-user {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 44px;
  padding: 8px 10px 8px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.topbar-user span {
  font-weight: 700;
}

.topbar-user small {
  color: #64748b;
}

.topbar-user button,
.workspace-content :deep(button),
.workspace-content :deep(.el-button--primary) {
  min-height: 38px;
  border: 0;
  border-radius: 10px;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
  font-weight: 700;
}

.topbar-user button {
  padding: 0 14px;
  background: #0f172a;
}

.workspace-content {
  min-width: 0;
  padding: 28px 32px 40px;
}

.workspace-content :deep(> section),
.auth-shell :deep(.login-card),
.workspace-content :deep(.reimbursement-form) {
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.07);
}

.workspace-content :deep(> section) {
  padding: 24px;
}

.auth-shell :deep(.login-card),
.workspace-content :deep(.reimbursement-form) {
  padding: 28px;
}

.workspace-content :deep(h1),
.auth-shell :deep(h1) {
  margin-top: 0;
  color: #0f172a;
  font-size: 22px;
}

.workspace-content :deep(.toolbar),
.workspace-content :deep(.filters),
.workspace-content :deep(.actions) {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.workspace-content :deep(.toolbar a) {
  display: inline-flex;
  align-items: center;
  min-height: 40px;
  padding: 0 14px;
  border-radius: 10px;
  background: #2563eb;
  color: #fff;
  font-weight: 700;
  text-decoration: none;
}

.workspace-content :deep(input),
.workspace-content :deep(select),
.auth-shell :deep(input) {
  min-height: 40px;
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  background: #fff;
  color: #111827;
}

.workspace-content :deep(input:focus),
.workspace-content :deep(select:focus),
.workspace-content :deep(button:focus-visible),
.auth-shell :deep(input:focus),
.auth-shell :deep(button:focus-visible),
.sidebar-nav a:focus-visible {
  outline: 3px solid rgba(37, 99, 235, 0.28);
  outline-offset: 2px;
}

.workspace-content :deep(table) {
  width: 100%;
  overflow: hidden;
  border-collapse: separate;
  border-spacing: 0;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #fff;
}

.workspace-content :deep(th),
.workspace-content :deep(td) {
  border: 0;
  border-bottom: 1px solid #edf2f7;
  padding: 11px 12px;
  text-align: left;
  font-size: 13px;
}

.workspace-content :deep(th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 800;
}

.workspace-content :deep(td) {
  color: #1f2937;
}

.workspace-content :deep(tr:last-child td) {
  border-bottom: 0;
}

@media (max-width: 860px) {
  .workspace-shell {
    grid-template-columns: 1fr;
  }

  .workspace-sidebar {
    position: static;
    height: auto;
  }

  .sidebar-nav,
  .workspace-topbar,
  .topbar-user {
    align-items: stretch;
  }

  .workspace-topbar,
  .topbar-user {
    flex-direction: column;
  }

  .workspace-content {
    padding: 18px;
  }
}
</style>
