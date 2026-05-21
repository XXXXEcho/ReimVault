// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import RecordDrawer from '../src/components/RecordDrawer.vue';
import http from '../src/api/http';
import type { ReimbursementRecord } from '../src/api/reimbursements';

vi.mock('../src/api/http', () => ({
  default: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() }
}));

const draft: ReimbursementRecord = { id: 1, employeeId: 2, employeeName: '员工一', amount: 123.45, categoryId: 1, categoryName: '差旅费', purpose: '客户拜访', paymentTime: '2026-05-21T02:30:00Z', status: 'DRAFT', adminRemark: null, submittedAt: null, archivedAt: null, attachments: [] };
const submitted: ReimbursementRecord = { ...draft, id: 2, status: 'SUBMITTED', submittedAt: '2026-05-21T03:00:00Z', attachments: [{ id: 9, type: 'PAYMENT_VOUCHER', originalFilename: 'pay.png', contentType: 'image/png', sizeBytes: 11, createdAt: '2026-05-21T03:00:00Z' }] };

describe('RecordDrawer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(http.get).mockResolvedValue({ data: [{ id: 1, name: '差旅费', enabled: true, sortOrder: 1, remark: '' }] });
  });

  it('edits and saves draft fields inside the drawer', async () => {
    vi.mocked(http.patch).mockResolvedValue({ data: { ...draft, amount: 200, purpose: '新用途' } });
    const wrapper = mount(RecordDrawer, { props: { record: draft, role: 'EMPLOYEE' } });
    await flushPromises();

    await wrapper.find('[aria-label="金额"]').setValue('200');
    await wrapper.find('[aria-label="用途说明"]').setValue('新用途');
    await wrapper.find('[data-test="save-draft"]').trigger('click');

    expect(http.patch).toHaveBeenCalledWith('/reimbursements/1', expect.objectContaining({ amount: 200, purpose: '新用途' }));
    expect(wrapper.emitted('saved')).toHaveLength(1);
  });

  it('renders submitted employee record as read-only', async () => {
    const wrapper = mount(RecordDrawer, { props: { record: submitted, role: 'EMPLOYEE' } });
    await flushPromises();

    expect(wrapper.find('[aria-label="金额"]').attributes('disabled')).toBeDefined();
    expect(wrapper.text()).not.toContain('提交');
  });

  it('allows admin to save remark for submitted record', async () => {
    vi.mocked(http.patch).mockResolvedValue({ data: { ...submitted, adminRemark: '已核对' } });
    const wrapper = mount(RecordDrawer, { props: { record: submitted, role: 'ADMIN' } });
    await flushPromises();

    await wrapper.find('[aria-label="管理员备注"]').setValue('已核对');
    await wrapper.find('[data-test="save-remark"]').trigger('click');

    expect(http.patch).toHaveBeenCalledWith('/admin/reimbursements/2/remark', { adminRemark: '已核对' });
  });
});
