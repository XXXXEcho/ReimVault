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

const MATRIX = {
  columns: [
    { batchId: 10, batchName: '2026年4月报销批次', monthLabel: '2026-04' },
    { batchId: 11, batchName: '2026年5月报销批次', monthLabel: '2026-05' }
  ],
  rows: [
    {
      employeeId: 1,
      employeeName: '张三',
      department: '研发部',
      cells: [{ amount: 100, count: 1 }, { amount: 200, count: 1 }],
      unassigned: { amount: 80, count: 2 },
      total: { amount: 380, count: 4 }
    }
  ],
  totals: {
    columnTotals: [{ amount: 100, count: 1 }, { amount: 200, count: 1 }],
    unassignedTotal: { amount: 80, count: 2 },
    grandTotal: { amount: 380, count: 4 }
  }
};

const EMPLOYEES = [
  { id: 1, username: 'zhangsan', displayName: '张三', department: '研发部', role: 'EMPLOYEE', enabled: true }
];

describe('stats view', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    const auth = useAuthStore();
    auth.user = { id: 1, username: 'admin', displayName: '管理员', department: '管理部', role: 'ADMIN' };
    vi.clearAllMocks();
    vi.mocked(http.get).mockImplementation((url: string) => {
      if (url === '/oa-numbers') return Promise.resolve({ data: [{ id: 1, number: '绿洲计划经费 30280501' }] });
      if (url === '/admin/batches') return Promise.resolve({ data: [{ id: 2, name: '2026年6月报销批次', description: '', createdAt: '', archivedAt: null, items: [] }] });
      if (url === '/admin/employees') return Promise.resolve({ data: EMPLOYEES });
      if (url === '/admin/reimbursements/stats/personnel-matrix') return Promise.resolve({ data: MATRIX });
      if (url === '/admin/reimbursements/stats') return Promise.resolve({ data: STATS });
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

  it('renders personnel matrix and refetches when an employee is selected', async () => {
    const wrapper = mount(StatsAdminView, { global: { stubs: ['el-button'] } });
    await vi.waitFor(() => expect(wrapper.find('[data-test="matrix-row-1"]').exists()).toBe(true));

    expect(wrapper.find('[data-test="matrix-column-10"]').text()).toContain('2026年4月报销批次');
    expect(wrapper.find('[data-test="matrix-cell-1-10"]').text()).toContain('100.00');
    expect(wrapper.find('[data-test="matrix-cell-1-unassigned"]').text()).toContain('80.00');
    expect(wrapper.find('[data-test="matrix-totals-row"]').text()).toContain('380.00');

    vi.mocked(http.get).mockClear();
    await wrapper.find('[data-test="employee-filter-group"] input[type="checkbox"]').setValue(true);

    await vi.waitFor(() => {
      const matrixCalls = vi.mocked(http.get).mock.calls.filter((c) => (c[0] as string).endsWith('/personnel-matrix'));
      const last = matrixCalls[matrixCalls.length - 1];
      expect(last).toBeTruthy();
      const params = (last![1] as { params?: URLSearchParams })?.params;
      expect(params?.get('employeeIds')).toBe('1');
    });
  });
});
