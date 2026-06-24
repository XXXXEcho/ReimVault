import { createRouter, createWebHistory, type RouterHistory } from 'vue-router';
import { useAuthStore } from './stores/auth';
import LoginView from './views/LoginView.vue';
import ReimbursementListView from './views/employee/ReimbursementListView.vue';
import ReimbursementEditView from './views/employee/ReimbursementEditView.vue';
import UserAdminView from './views/admin/UserAdminView.vue';
import CategoryAdminView from './views/admin/CategoryAdminView.vue';
import ReimbursementAdminView from './views/admin/ReimbursementAdminView.vue';
import ReimbursementDetailView from './views/admin/ReimbursementDetailView.vue';
import BatchAdminView from './views/admin/BatchAdminView.vue';
import StatsAdminView from './views/admin/StatsAdminView.vue';
import OaAdminView from './views/admin/OaAdminView.vue';

const routes = [
  { path: '/', redirect: '/reimbursements' },
  { path: '/login', component: LoginView },
  { path: '/reimbursements', component: ReimbursementListView },
  { path: '/reimbursements/new', component: ReimbursementEditView },
  { path: '/reimbursements/:id', component: ReimbursementEditView },
  { path: '/admin/users', component: UserAdminView, meta: { requiresAdmin: true } },
  { path: '/admin/categories', component: CategoryAdminView, meta: { requiresManagement: true } },
  { path: '/admin/reimbursements', component: ReimbursementAdminView, meta: { requiresManagement: true } },
  { path: '/admin/reimbursements/:id', component: ReimbursementDetailView, meta: { requiresManagement: true } },
  { path: '/admin/oa', component: OaAdminView, meta: { requiresManagement: true } },
  { path: '/admin/batches', component: BatchAdminView, meta: { requiresManagement: true } },
  { path: '/admin/stats', component: StatsAdminView, meta: { requiresManagement: true } }
];

export function createAppRouter(history: RouterHistory = createWebHistory()) {
  const router = createRouter({ history, routes });

  router.beforeEach(async (to) => {
    if (to.path === '/login') return true;
    const auth = useAuthStore();
    if (!auth.user) {
      try {
        await auth.loadCurrentUser();
      } catch {
        return '/login';
      }
    }
    if (to.meta.requiresAdmin && auth.user?.role !== 'ADMIN') return '/reimbursements';
    if (to.meta.requiresManagement && auth.user?.role !== 'ADMIN' && auth.user?.role !== 'SPECIALIST') return '/reimbursements';
    return true;
  });

  return router;
}

export default createAppRouter();
