// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { useAuthStore } from '../src/stores/auth';
import { createMemoryHistory } from 'vue-router';
import { createAppRouter } from '../src/router';
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

async function mountWithRouter(component: typeof EmployeeWorkbench | typeof AdminWorkbench, path: string) {
  const router = createAppRouter(createMemoryHistory());
  await router.push(path);
  await router.isReady();
  return { wrapper: mount(component, { global: { plugins: [router], stubs: ['RouterLink'] } }), router };
}

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

  it('renders employee metrics and opens a detail page by row click', async () => {
    const { wrapper, router } = await mountWithRouter(EmployeeWorkbench, '/reimbursements');
    await flushPromises();

    expect(wrapper.text()).toContain('草稿');
    expect(wrapper.text()).toContain('待报销');
    expect(wrapper.text()).toContain('材料不完整');
    await wrapper.find('[data-test="record-row-1"]').trigger('click');
    await flushPromises();
    expect(router.currentRoute.value.path).toBe('/reimbursements/1/detail');
  });

  it('uses the detail page for drafts in my-reimbursements', async () => {
    const { wrapper, router } = await mountWithRouter(EmployeeWorkbench, '/reimbursements');
    await flushPromises();

    await wrapper.find('[data-test="record-row-1"]').trigger('click');
    await flushPromises();

    expect(router.currentRoute.value.path).toBe('/reimbursements/1/detail');
  });

  it('shows a submit button on draft rows with a payment voucher in my-reimbursements', async () => {
    const draftWithVoucher = {
      ...records[0],
      attachments: [{ id: 9, type: 'PAYMENT_VOUCHER', originalFilename: 'draft-pay.png', contentType: 'image/png', sizeBytes: 12, createdAt: '2026-05-21T03:00:00Z' }]
    };
    vi.mocked(http.get).mockImplementation((url: string) => {
      if (url === '/oa-numbers' || url === '/admin/oa-numbers') return Promise.resolve({ data: [] });
      if (url === '/categories' || url === '/admin/categories') return Promise.resolve({ data: [{ id: 1, name: '差旅费', enabled: true, sortOrder: 1, remark: '' }] });
      if (url === '/admin/employees') return Promise.resolve({ data: [{ id: 2, username: 'emp', displayName: '员工一', department: '研发部', role: 'EMPLOYEE', enabled: true }] });
      return Promise.resolve({ data: [draftWithVoucher, records[1]] });
    });
    vi.mocked(http.post).mockResolvedValue({ data: { ...draftWithVoucher, status: 'SUBMITTED' } });
    const { wrapper } = await mountWithRouter(EmployeeWorkbench, '/reimbursements');
    await flushPromises();

    const submitBtn = wrapper.find('[data-test="submit-row-1"]');
    expect(submitBtn.exists()).toBe(true);
    await submitBtn.trigger('click');
    await flushPromises();

    expect(http.post).toHaveBeenCalledWith('/reimbursements/1/submit');
  });

  it('opens submitted employee records as a dedicated page', async () => {
    const { wrapper, router } = await mountWithRouter(EmployeeWorkbench, '/reimbursements');
    await flushPromises();
    await wrapper.find('[data-test="record-row-2"]').trigger('click');
    await flushPromises();
    expect(router.currentRoute.value.path).toBe('/reimbursements/2/detail');
  });

  it('keeps the list focused on navigation rather than embedding a preview dialog', async () => {
    const { wrapper, router } = await mountWithRouter(EmployeeWorkbench, '/reimbursements');
    await flushPromises();
    await wrapper.find('[data-test="record-row-2"]').trigger('click');
    await flushPromises();
    expect(router.currentRoute.value.path).toBe('/reimbursements/2/detail');
  });

  it('navigates to the selected employee record once', async () => {
    const { wrapper, router } = await mountWithRouter(EmployeeWorkbench, '/reimbursements');
    await flushPromises();
    await wrapper.find('[data-test="record-row-2"]').trigger('click');
    await flushPromises();
    expect(router.currentRoute.value.fullPath).toBe('/reimbursements/2/detail');
  });

  it('opens a dedicated detail page from the admin workbench', async () => {
    const { wrapper, router } = await mountWithRouter(AdminWorkbench, '/admin/reimbursements');
    await flushPromises();
    await wrapper.find('[data-test="record-row-2"]').trigger('click');
    await flushPromises();
    expect(router.currentRoute.value.path).toBe('/admin/reimbursements/2');
  });

  it('renders admin metrics, passes filters, and opens a detail page by row click', async () => {
    const { wrapper, router } = await mountWithRouter(AdminWorkbench, '/admin/reimbursements');
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
    await flushPromises();
    expect(router.currentRoute.value.path).toBe('/admin/reimbursements/2');
  });
});
