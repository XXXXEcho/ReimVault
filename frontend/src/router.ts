import { createRouter, createWebHistory, type RouterHistory } from 'vue-router';
import { useAuthStore } from './stores/auth';
import LoginView from './views/LoginView.vue';
import ReimbursementListView from './views/employee/ReimbursementListView.vue';
import ReimbursementEditView from './views/employee/ReimbursementEditView.vue';
import UserAdminView from './views/admin/UserAdminView.vue';
import CategoryAdminView from './views/admin/CategoryAdminView.vue';
import ReimbursementAdminView from './views/admin/ReimbursementAdminView.vue';
import ReimbursementRecordDetailView from './views/ReimbursementRecordDetailView.vue';
import BatchAdminView from './views/admin/BatchAdminView.vue';
import StatsAdminView from './views/admin/StatsAdminView.vue';
import OaAdminView from './views/admin/OaAdminView.vue';

const routes = [
  { path: '/', redirect: '/reimbursements' },
  { path: '/login', component: LoginView, meta: { title: '登录', description: '进入报销材料管理系统' } },
  { path: '/reimbursements', component: ReimbursementListView, meta: { title: '我的报销', description: '提交和查看自己的报销材料' } },
  { path: '/reimbursements/new', component: ReimbursementEditView, meta: { title: '新建报销', description: '创建草稿并上传材料' } },
  { path: '/reimbursements/:id/detail', component: ReimbursementRecordDetailView, meta: { title: '报销详情', description: '查看和维护单条报销材料' } },
  { path: '/reimbursements/:id', component: ReimbursementEditView, meta: { title: '编辑报销', description: '维护草稿材料' } },
  { path: '/admin/users', component: UserAdminView, meta: { title: '用户管理', description: '维护员工和管理员账号', requiresAdmin: true } },
  { path: '/admin/categories', component: CategoryAdminView, meta: { title: '分类管理', description: '维护报销用途分类', requiresManagement: true } },
  { path: '/admin/reimbursements', component: ReimbursementAdminView, meta: { title: '报销工作台', description: '集中处理员工提交的材料', requiresManagement: true } },
  { path: '/admin/reimbursements/:id', component: ReimbursementRecordDetailView, meta: { title: '报销详情', description: '核对单条报销材料', requiresManagement: true } },
  { path: '/admin/oa', component: OaAdminView, meta: { title: '经费编码', description: '维护 OA 经费编码', requiresManagement: true } },
  { path: '/admin/batches', component: BatchAdminView, meta: { title: '批次管理', description: '整理、导出和归档材料', requiresManagement: true } },
  { path: '/admin/stats', component: StatsAdminView, meta: { title: '统计', description: '查看报销金额和处理进度', requiresManagement: true } }
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
