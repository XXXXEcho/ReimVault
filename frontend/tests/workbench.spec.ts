// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
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
    vi.clearAllMocks();
    vi.mocked(http.get).mockResolvedValue({ data: records });
  });

  it('renders employee metrics and opens drawer by row click', async () => {
    const wrapper = mount(EmployeeWorkbench, { global: { stubs: ['RouterLink'] } });
    await flushPromises();

    expect(wrapper.text()).toContain('草稿');
    expect(wrapper.text()).toContain('已提交');
    expect(wrapper.text()).toContain('材料不完整');
    await wrapper.find('[data-test="record-row-1"]').trigger('click');
    expect(wrapper.text()).toContain('记录详情');
    expect(wrapper.text()).toContain('客户拜访');
  });

  it('reloads employee records and keeps selected record synced after drawer saved event', async () => {
    const refreshedRecords = records.map((record) => (record.id === 2 ? { ...record, purpose: '晚餐', attachments: [] } : record));
    vi.mocked(http.get).mockResolvedValueOnce({ data: records }).mockResolvedValueOnce({ data: refreshedRecords });
    const wrapper = mount(EmployeeWorkbench, { global: { stubs: ['RouterLink'] } });
    await flushPromises();
    await wrapper.find('[data-test="record-row-2"]').trigger('click');
    expect(wrapper.text()).toContain('午餐');

    wrapper.findComponent({ name: 'RecordDrawer' }).vm.$emit('saved', records[1]);
    await flushPromises();

    expect(http.get).toHaveBeenCalledTimes(2);
    expect(wrapper.text()).toContain('晚餐');
    expect(wrapper.text()).not.toContain('午餐');
  });

  it('renders admin metrics, passes filters, and opens drawer by row click', async () => {
    const wrapper = mount(AdminWorkbench);
    await flushPromises();

    expect(wrapper.text()).toContain('草稿');
    expect(wrapper.text()).toContain('已提交');
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
