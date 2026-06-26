// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import { createMemoryHistory, createRouter } from 'vue-router';
import { createPinia, setActivePinia } from 'pinia';
import AppShell from '../src/layouts/AppShell.vue';
import { useAuthStore } from '../src/stores/auth';

const routes = [
  { path: '/reimbursements', component: { template: '<div>我的报销页</div>' }, meta: { title: '我的报销', description: '提交和管理自己的报销材料' } },
  { path: '/admin/reimbursements', component: { template: '<div>报销工作台页</div>' }, meta: { title: '报销工作台', description: '集中处理员工提交的材料' } }
];

describe('AppShell', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('shows employee navigation without admin-only destinations', async () => {
    const router = createRouter({ history: createMemoryHistory(), routes });
    await router.push('/reimbursements');
    const auth = useAuthStore();
    auth.user = { id: 1, username: 'employee', displayName: '员工一', department: '研发部', role: 'EMPLOYEE' };

    const wrapper = mount(AppShell, { global: { plugins: [router] } });
    await flushPromises();

    expect(wrapper.text()).toContain('我的报销');
    expect(wrapper.text()).not.toContain('报销工作台');
    expect(wrapper.text()).toContain('员工一');
    expect(wrapper.text()).toContain('EMPLOYEE');
  });

  it('shows admin navigation and current page description', async () => {
    const router = createRouter({ history: createMemoryHistory(), routes });
    await router.push('/admin/reimbursements');
    const auth = useAuthStore();
    auth.user = { id: 2, username: 'admin', displayName: '系统管理员', department: '财务部', role: 'ADMIN' };

    const wrapper = mount(AppShell, { global: { plugins: [router] } });
    await flushPromises();

    expect(wrapper.text()).toContain('报销工作台');
    expect(wrapper.text()).toContain('集中处理员工提交的材料');
    expect(wrapper.text()).toContain('批次管理');
    expect(wrapper.text()).toContain('系统管理员');
    expect(wrapper.text()).toContain('ADMIN');
  });
});
