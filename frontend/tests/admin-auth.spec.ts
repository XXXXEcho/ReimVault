// @vitest-environment jsdom
import { describe, expect, it, beforeEach } from 'vitest';
import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { createMemoryHistory, type Router } from 'vue-router';
import App from '../src/App.vue';
import { createAppRouter } from '../src/router';
import { useAuthStore } from '../src/stores/auth';
import type { CurrentUser } from '../src/api/auth';

const employeeUser: CurrentUser = { id: 1, username: 'employee', displayName: '员工', department: '研发部', role: 'EMPLOYEE' };
const adminUser: CurrentUser = { id: 2, username: 'admin', displayName: '管理员', department: '财务部', role: 'ADMIN' };

function createTestRouter() {
  return createAppRouter(createMemoryHistory());
}

function mountAppWithUser(user: CurrentUser, router: Router = createTestRouter()) {
  const pinia = createPinia();
  setActivePinia(pinia);
  const auth = useAuthStore();
  auth.user = user;

  const wrapper = mount(App, {
    global: {
      plugins: [pinia, router],
      stubs: { RouterLink: RouterLinkStub, RouterView: true }
    }
  });

  return { auth, router, wrapper };
}

describe('admin authorization UI and routes', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('shows admin navigation links only to administrators', () => {
    const employeeWrapper = mountAppWithUser(employeeUser).wrapper;
    expect(employeeWrapper.text()).toContain('我的报销');
    expect(employeeWrapper.text()).not.toContain('报销管理');
    expect(employeeWrapper.text()).not.toContain('批次管理');
    employeeWrapper.unmount();

    const adminWrapper = mountAppWithUser(adminUser).wrapper;
    expect(adminWrapper.text()).toContain('报销管理');
    expect(adminWrapper.text()).toContain('批次管理');
    expect(adminWrapper.text()).toContain('用户管理');
    expect(adminWrapper.text()).toContain('分类管理');
  });

  it('renders the SaaS workspace shell for signed-in users', async () => {
    const { auth, router, wrapper } = mountAppWithUser(adminUser);

    expect(wrapper.find('[aria-label="主导航"]').exists()).toBe(true);
    expect(wrapper.find('.brand-title').text()).toBe('报销归档通');
    expect(wrapper.find('.brand-subtitle').text()).toBe('ReimVault');
    expect(wrapper.find('.topbar-user').text()).toContain('管理员');
    expect(wrapper.find('.topbar-user').text()).toContain('财务部');

    await router.push('/admin/reimbursements');
    await wrapper.find('[aria-label="退出登录"]').trigger('click');
    await flushPromises();

    expect(auth.user).toBeNull();
    expect(router.currentRoute.value.path).toBe('/login');
  });

  it('redirects employees away from admin routes', async () => {
    setActivePinia(createPinia());
    useAuthStore().user = employeeUser;
    const router = createTestRouter();

    await router.push('/admin/reimbursements');

    expect(router.currentRoute.value.path).toBe('/reimbursements');
  });
});
