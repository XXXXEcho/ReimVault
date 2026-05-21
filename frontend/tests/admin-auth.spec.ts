// @vitest-environment jsdom
import { describe, expect, it, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import App from '../src/App.vue';
import router from '../src/router';
import { useAuthStore } from '../src/stores/auth';

describe('admin authorization UI and routes', () => {
  beforeEach(async () => {
    setActivePinia(createPinia());
    await router.push('/reimbursements');
  });

  it('shows admin navigation links only to administrators', async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    useAuthStore().user = { id: 1, username: 'employee', displayName: '员工', department: '研发部', role: 'EMPLOYEE' };

    const employeeWrapper = mount(App, { global: { plugins: [pinia], stubs: { RouterLink: { template: '<a><slot /></a>' }, RouterView: true } } });
    expect(employeeWrapper.text()).toContain('我的报销');
    expect(employeeWrapper.text()).not.toContain('报销工作台');
    expect(employeeWrapper.text()).not.toContain('批次管理');

    const adminPinia = createPinia();
    setActivePinia(adminPinia);
    useAuthStore().user = { id: 2, username: 'admin', displayName: '管理员', department: '财务部', role: 'ADMIN' };
    const adminWrapper = mount(App, { global: { plugins: [adminPinia], stubs: { RouterLink: { template: '<a><slot /></a>' }, RouterView: true } } });

    expect(adminWrapper.text()).toContain('报销工作台');
    expect(adminWrapper.text()).toContain('批次管理');
    expect(adminWrapper.text()).toContain('用户管理');
    expect(adminWrapper.text()).toContain('分类管理');
  });

  it('redirects employees away from admin routes', async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    useAuthStore().user = { id: 1, username: 'employee', displayName: '员工', department: '研发部', role: 'EMPLOYEE' };

    await router.push('/admin/reimbursements');

    expect(router.currentRoute.value.path).toBe('/reimbursements');
  });
});
