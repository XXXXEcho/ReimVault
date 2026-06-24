// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import StatsAdminView from '../src/views/admin/StatsAdminView.vue';
import http from '../src/api/http';
import { useAuthStore } from '../src/stores/auth';

vi.mock('../src/api/http', () => ({
  default: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() }
}));

const STATS = {
  totalCount: 5,
  totalAmount: 460,
  reimbursedCount: 2,
  reimbursedAmount: 230,
  unreimbursedCount: 2,
  unreimbursedAmount: 180,
  draftCount: 1,
  draftAmount: 50
};

describe('stats view', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    const auth = useAuthStore();
    auth.user = { id: 1, username: 'admin', displayName: '管理员', department: '管理部', role: 'ADMIN' };
    vi.clearAllMocks();
    vi.mocked(http.get).mockImplementation((url: string) => {
      if (url === '/oa-numbers') return Promise.resolve({ data: [{ id: 1, number: '绿洲计划经费 30280501' }] });
      if (url === '/admin/batches') return Promise.resolve({ data: [{ id: 2, name: '2026年6月报销批次', description: '', createdAt: '', archivedAt: null, items: [] }] });
      if (url.startsWith('/admin/reimbursements/stats')) return Promise.resolve({ data: STATS });
      return Promise.resolve({ data: [] });
    });
  });

  it('summarizes the active scope above the result cards', async () => {
    const wrapper = mount(StatsAdminView, { global: { stubs: ['el-button'] } });
    await vi.waitFor(() => expect(wrapper.text()).toContain('460.00'));
    expect(wrapper.find('[data-test="scope-summary"]').text()).toContain('全部记录');

    await wrapper.find('[data-test="oa-filter-group"] input[type="checkbox"]').setValue(true);
    expect(wrapper.find('[data-test="scope-summary"]').text()).toContain('绿洲计划经费');
  });

  it('renders default amount metrics and switches metric set via preset', async () => {
    const wrapper = mount(StatsAdminView, { global: { stubs: ['el-button'] } });
    await vi.waitFor(() => expect(wrapper.text()).toContain('460.00'));

    expect(wrapper.find('[data-test="metric-totalAmount"]').text()).toContain('460.00');
    expect(wrapper.find('[data-test="metric-reimbursedAmount"]').text()).toContain('230.00');
    expect(wrapper.find('[data-test="metric-totalCount"]').exists()).toBe(false);

    await wrapper.find('[data-test="preset-progress"]').trigger('click');
    expect(wrapper.find('[data-test="metric-reimbursedCount"]').text()).toContain('2');
    expect(wrapper.find('[data-test="metric-totalCount"]').text()).toContain('5');
    expect(wrapper.find('[data-test="metric-reimbursedAmount"]').exists()).toBe(false);
  });

  it('refetches stats scoped by fund code when one is selected', async () => {
    const wrapper = mount(StatsAdminView, { global: { stubs: ['el-button'] } });
    await vi.waitFor(() => expect(wrapper.text()).toContain('绿洲计划经费'));
    vi.mocked(http.get).mockClear();

    await wrapper.find('[data-test="oa-filter-group"] input[type="checkbox"]').setValue(true);

    await vi.waitFor(() => {
      const statsCalls = vi.mocked(http.get).mock.calls.filter((c) => (c[0] as string).startsWith('/admin/reimbursements/stats'));
      const last = statsCalls[statsCalls.length - 1];
      expect(last).toBeTruthy();
      const params = (last![1] as { params?: URLSearchParams })?.params;
      expect(params?.get('oaIds')).toBe('1');
    });
  });
});
