// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { useAuthStore } from '../src/stores/auth';
import EmployeeWorkbench from '../src/views/employee/ReimbursementListView.vue';
import AdminWorkbench from '../src/views/admin/ReimbursementAdminView.vue';
import http from '../src/api/http';

vi.mock('../src/api/http', () => ({
  default: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() }
}));

const records = [
  { id: 1, employeeId: 2, employeeName: '员工一', amount: 123.45, categoryId: 1, categoryName: '差旅费', purpose: '客户拜访', paymentTime: '2026-05-21T02:30:00Z', status: 'DRAFT', adminRemark: null, submittedAt: null, archivedAt: null, attachments: [] },
  { id: 2, employeeId: 2, employeeName: '员工一', amount: 88, categoryId: 1, categoryName: '差旅费', purpose: '午餐', paymentTime: '2026-05-20T02:30:00Z', status: 'SUBMITTED', adminRemark: null, submittedAt: '2026-05-20T03:00:00Z', archivedAt: null, attachments: [{ id: 8, type: 'PAYMENT_VOUCHER', originalFilename: 'pay.png', contentType: 'image/png', sizeBytes: 11, createdAt: '2026-05-20T03:00:00Z' }] }
];

describe('workbench pages', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    useAuthStore().user = { id: 1, username: 'admin', displayName: '管理员', department: '管理部', role: 'ADMIN' };
    vi.clearAllMocks();
    vi.mocked(http.get).mockImplementation((url: string) => {
      if (url === '/oa-numbers' || url === '/admin/oa-numbers') return Promise.resolve({ data: [] });
      if (url === '/categories' || url === '/admin/categories') return Promise.resolve({ data: [{ id: 1, name: '差旅费', enabled: true, sortOrder: 1, remark: '' }] });
      if (url === '/admin/employees') return Promise.resolve({ data: [{ id: 2, username: 'emp', displayName: '员工一', department: '研发部', role: 'EMPLOYEE', enabled: true }] });
      return Promise.resolve({ data: records });
    });
  });

  it('renders employee metrics and opens drawer by row click', async () => {
    const wrapper = mount(EmployeeWorkbench, { global: { stubs: ['RouterLink'] } });
    await flushPromises();

    expect(wrapper.text()).toContain('草稿');
    expect(wrapper.text()).toContain('待报销');
    expect(wrapper.text()).toContain('材料不完整');
    await wrapper.find('[data-test="record-row-1"]').trigger('click');
    expect(wrapper.text()).toContain('记录详情');
    expect(wrapper.text()).toContain('客户拜访');
  });

  it('lets an admin edit their own draft in my-reimbursements', async () => {
    const wrapper = mount(EmployeeWorkbench, { global: { stubs: ['RouterLink'] } });
    await flushPromises();

    await wrapper.find('[data-test="record-row-1"]').trigger('click');
    await flushPromises();

    expect(wrapper.find('[aria-label="金额"]').attributes('disabled')).toBeUndefined();
    expect(wrapper.find('[data-test="save-draft"]').exists()).toBe(true);
  });

  it('reloads employee records and keeps selected record synced after drawer saved event', async () => {
    const refreshedRecords = records.map((record) => (record.id === 2 ? { ...record, purpose: '晚餐', attachments: [] } : record));
    let reimbursementCalls = 0;
    vi.mocked(http.get).mockImplementation((url: string) => {
      if (url === '/oa-numbers' || url === '/admin/oa-numbers') return Promise.resolve({ data: [] });
      if (url === '/categories') return Promise.resolve({ data: [{ id: 1, name: '差旅费', enabled: true, sortOrder: 1, remark: '' }] });
      reimbursementCalls += 1;
      return Promise.resolve({ data: reimbursementCalls === 1 ? records : refreshedRecords });
    });
    const wrapper = mount(EmployeeWorkbench, { global: { stubs: ['RouterLink'] } });
    await flushPromises();
    await wrapper.find('[data-test="record-row-2"]').trigger('click');
    expect(wrapper.text()).toContain('午餐');

    wrapper.findComponent({ name: 'RecordDrawer' }).vm.$emit('saved', records[1]);
    await flushPromises();

    expect(reimbursementCalls).toBe(2);
    expect(wrapper.text()).toContain('晚餐');
    expect(wrapper.text()).not.toContain('午餐');
  });

  it('opens material previewer from employee drawer preview event', async () => {
    const wrapper = mount(EmployeeWorkbench, { global: { stubs: ['RouterLink'] } });
    await flushPromises();
    await wrapper.find('[data-test="record-row-2"]').trigger('click');

    wrapper.findComponent({ name: 'RecordDrawer' }).vm.$emit('preview', 8);
    await flushPromises();

    expect(wrapper.find('[aria-label="材料预览"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="download-active"]').attributes('href')).toBe('/api/attachments/8');
  });

  it('removes employee previewer when preview close is emitted', async () => {
    const wrapper = mount(EmployeeWorkbench, { global: { stubs: ['RouterLink'] } });
    await flushPromises();
    await wrapper.find('[data-test="record-row-2"]').trigger('click');
    wrapper.findComponent({ name: 'RecordDrawer' }).vm.$emit('preview', 8);
    await flushPromises();

    wrapper.findComponent({ name: 'MaterialPreviewer' }).vm.$emit('close');
    await flushPromises();

    expect(wrapper.find('[aria-label="材料预览"]').exists()).toBe(false);
  });

  it('opens material previewer from admin drawer preview event', async () => {
    const wrapper = mount(AdminWorkbench);
    await flushPromises();
    await wrapper.find('[data-test="record-row-2"]').trigger('click');

    wrapper.findComponent({ name: 'RecordDrawer' }).vm.$emit('preview', 8);
    await flushPromises();

    expect(wrapper.find('[aria-label="材料预览"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="download-active"]').attributes('href')).toBe('/api/attachments/8');
  });

  it('renders admin metrics, passes filters, and opens drawer by row click', async () => {
    const wrapper = mount(AdminWorkbench);
    await flushPromises();

    expect(wrapper.text()).toContain('草稿');
    expect(wrapper.text()).toContain('待报销');
    expect(wrapper.text()).toContain('已归档');
    expect(wrapper.text()).toContain('材料不完整');

    await wrapper.find('[aria-label="员工ID"]').setValue('2');
    await wrapper.find('[aria-label="状态"]').setValue('SUBMITTED');
    await wrapper.find('[data-test="apply-filters"]').trigger('click');

    expect(http.get).toHaveBeenLastCalledWith('/admin/reimbursements', { params: expect.objectContaining({ employeeId: 2, status: 'SUBMITTED' }) });
    await wrapper.find('[data-test="record-row-2"]').trigger('click');
    expect(wrapper.text()).toContain('记录详情');
  });
});
