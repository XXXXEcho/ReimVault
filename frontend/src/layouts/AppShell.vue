<script setup lang="ts">
import { computed, inject } from 'vue';
import { RouterLink, RouterView, routeLocationKey } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();
const route = inject(routeLocationKey, null);
const routeMeta = computed(() => route?.meta ?? {});

const employeeNav = [
  { to: '/reimbursements', label: '我的报销', icon: 'document' }
];

const adminNav = [
  { to: '/admin/reimbursements', label: '报销工作台', icon: 'dashboard' },
  { to: '/admin/oa', label: '经费编码', icon: 'tags' },
  { to: '/admin/batches', label: '批次管理', icon: 'archive' },
  { to: '/admin/stats', label: '统计', icon: 'dashboard' },
  { to: '/admin/users', label: '用户管理', icon: 'users', adminOnly: true },
  { to: '/admin/categories', label: '分类管理', icon: 'tags' }
];

const navigationItems = computed(() => (
  auth.user?.role === 'ADMIN'
    ? [...employeeNav, ...adminNav]
    : auth.user?.role === 'SPECIALIST'
      ? [...employeeNav, ...adminNav.filter((item) => !item.adminOnly)]
      : employeeNav
));

const pageTitle = computed(() => String(routeMeta.value.title ?? '报销材料管理系统'));
const pageDescription = computed(() => String(routeMeta.value.description ?? '企业报销材料提交、整理与归档'));
</script>

<template>
  <div class="app-shell">
    <aside class="app-shell__sidebar" aria-label="主导航">
      <RouterLink class="app-shell__brand" to="/reimbursements" aria-label="报销材料管理系统首页">
        <span class="app-shell__brand-mark" aria-hidden="true">
          <svg viewBox="0 0 24 24" role="img" focusable="false">
            <path d="M7 3h7l4 4v14H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z" />
            <path d="M14 3v5h5" />
            <path d="M8.5 13h7" />
            <path d="M8.5 17h5" />
          </svg>
        </span>
        <span>
          <strong>报销系统</strong>
          <small>Workbench</small>
        </span>
      </RouterLink>

      <nav class="app-shell__nav">
        <RouterLink
          v-for="item in navigationItems"
          :key="item.to"
          class="app-shell__nav-link"
          active-class="app-shell__nav-link--active"
          :to="item.to"
        >
          <span class="app-shell__nav-icon" aria-hidden="true">
            <svg v-if="item.icon === 'document'" viewBox="0 0 24 24" role="img" focusable="false">
              <path d="M7 3h7l4 4v14H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z" />
              <path d="M14 3v5h5" />
              <path d="M8 12h8" />
              <path d="M8 16h6" />
            </svg>
            <svg v-else-if="item.icon === 'dashboard'" viewBox="0 0 24 24" role="img" focusable="false">
              <path d="M4 13h6v7H4z" />
              <path d="M14 4h6v16h-6z" />
              <path d="M4 4h6v5H4z" />
            </svg>
            <svg v-else-if="item.icon === 'archive'" viewBox="0 0 24 24" role="img" focusable="false">
              <path d="M4 7h16" />
              <path d="M6 7v12h12V7" />
              <path d="M8 3h8l2 4H6z" />
              <path d="M10 12h4" />
            </svg>
            <svg v-else-if="item.icon === 'users'" viewBox="0 0 24 24" role="img" focusable="false">
              <path d="M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" />
              <path d="M3 21a6 6 0 0 1 12 0" />
              <path d="M17 11a3 3 0 0 0 0-6" />
              <path d="M18 21a5 5 0 0 0-3-4.6" />
            </svg>
            <svg v-else viewBox="0 0 24 24" role="img" focusable="false">
              <path d="M4 7h16" />
              <path d="M4 12h16" />
              <path d="M4 17h16" />
              <path d="M8 5v4" />
              <path d="M16 10v4" />
              <path d="M11 15v4" />
            </svg>
          </span>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <div class="app-shell__main">
      <header class="app-shell__topbar">
        <div class="app-shell__heading">
          <p class="app-shell__eyebrow">报销材料管理</p>
          <h1>{{ pageTitle }}</h1>
          <p>{{ pageDescription }}</p>
        </div>
        <div v-if="auth.user" class="app-shell__user-pill" aria-label="当前用户">
          <span class="app-shell__avatar" aria-hidden="true">{{ auth.user.displayName.slice(0, 1) }}</span>
          <span class="app-shell__user-name">{{ auth.user.displayName }}</span>
          <span class="app-shell__role">{{ auth.user.role }}</span>
        </div>
      </header>

      <main class="app-shell__content" tabindex="-1">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: grid;
  min-height: 100vh;
  grid-template-columns: 264px minmax(0, 1fr);
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.12), transparent 34rem),
    var(--color-background);
}

.app-shell__sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  padding: var(--space-6) var(--space-4);
  border-right: 1px solid var(--color-border);
  background: rgba(255, 255, 255, 0.92);
}

.app-shell__brand {
  display: flex;
  min-height: 56px;
  align-items: center;
  gap: var(--space-3);
  padding: 0 var(--space-3);
  border-radius: var(--radius-lg);
  color: var(--color-text);
  text-decoration: none;
}

.app-shell__brand strong,
.app-shell__brand small {
  display: block;
}

.app-shell__brand small {
  color: var(--color-text-muted);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.app-shell__brand-mark,
.app-shell__avatar {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
}

.app-shell__brand-mark {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: white;
}

.app-shell__brand-mark svg,
.app-shell__nav-icon svg {
  width: 22px;
  height: 22px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.app-shell__nav {
  display: grid;
  gap: var(--space-2);
  margin-top: var(--space-8);
}

.app-shell__nav-link {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: var(--space-3);
  border-radius: var(--radius-md);
  padding: var(--space-3);
  color: var(--color-text-muted);
  font-weight: 600;
  text-decoration: none;
  transition: background-color 180ms ease, color 180ms ease, box-shadow 180ms ease;
}

.app-shell__nav-link:hover,
.app-shell__nav-link--active {
  background: var(--color-primary-soft);
  color: var(--color-primary-strong);
}

.app-shell__nav-link--active {
  box-shadow: inset 3px 0 0 var(--color-primary);
}

.app-shell__nav-link:focus-visible,
.app-shell__brand:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--color-focus) 72%, white);
  outline-offset: 3px;
}

.app-shell__nav-icon {
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
}

.app-shell__main {
  min-width: 0;
}

.app-shell__topbar {
  display: flex;
  min-height: 112px;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
  padding: var(--space-6) var(--space-8);
  border-bottom: 1px solid var(--color-border);
  background: rgba(245, 247, 251, 0.82);
}

.app-shell__heading h1,
.app-shell__heading p {
  margin: 0;
}

.app-shell__heading h1 {
  margin-top: var(--space-1);
  font-size: clamp(24px, 3vw, 34px);
  line-height: 1.15;
}

.app-shell__heading p:not(.app-shell__eyebrow) {
  margin-top: var(--space-2);
  color: var(--color-text-muted);
}

.app-shell__eyebrow {
  color: var(--color-primary-strong);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.app-shell__user-pill {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  gap: var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: 999px;
  padding: var(--space-1) var(--space-3) var(--space-1) var(--space-1);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  white-space: nowrap;
}

.app-shell__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-primary-soft);
  color: var(--color-primary-strong);
  font-weight: 800;
}

.app-shell__user-name {
  font-weight: 700;
}

.app-shell__role {
  border-radius: 999px;
  padding: 2px var(--space-2);
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 700;
}

.app-shell__content {
  min-width: 0;
  padding: var(--space-8);
}

@media (max-width: 767px) {
  .app-shell {
    display: block;
  }

  .app-shell__sidebar {
    position: static;
    height: auto;
    padding: var(--space-4);
  }

  .app-shell__brand {
    padding: 0;
  }

  .app-shell__nav {
    display: flex;
    gap: var(--space-2);
    margin-top: var(--space-4);
    overflow-x: auto;
    padding-bottom: var(--space-1);
  }

  .app-shell__nav-link {
    flex: 0 0 auto;
  }

  .app-shell__topbar {
    min-height: auto;
    align-items: flex-start;
    flex-direction: column;
    gap: var(--space-4);
    padding: var(--space-5) var(--space-4);
  }

  .app-shell__content {
    padding: var(--space-4);
  }
}
</style>
