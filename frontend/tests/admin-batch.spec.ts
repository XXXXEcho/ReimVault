// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import UserAdminView from '../src/views/admin/UserAdminView.vue';
import CategoryAdminView from '../src/views/admin/CategoryAdminView.vue';
import ReimbursementAdminView from '../src/views/admin/ReimbursementAdminView.vue';
import BatchAdminView from '../src/views/admin/BatchAdminView.vue';
import http from '../src/api/http';

vi.mock('../src/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn()
  }
}));

describe('admin views', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(http.get).mockResolvedValue({ data: [] });
  });

  it('creates users through the admin users endpoint', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: { id: 1 } });
    const wrapper = mount(UserAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-drawer', 'el-form', 'el-form-item', 'el-input', 'el-select', 'el-option', 'el-switch', 'el-button'] } });

    await wrapper.find('[aria-label="用户名"]').setValue('zhangsan');
    await wrapper.find('[aria-label="姓名"]').setValue('张三');
    await wrapper.find('[aria-label="部门"]').setValue('销售部');
    await wrapper.find('[aria-label="密码"]').setValue('secret');
    await wrapper.find('[aria-label="角色"]').setValue('EMPLOYEE');
    await wrapper.find('[data-test="create-user"]').trigger('click');

    expect(http.post).toHaveBeenCalledWith('/admin/users', {
      username: 'zhangsan',
      displayName: '张三',
      department: '销售部',
      password: 'secret',
      role: 'EMPLOYEE'
    });
  });

  it('creates categories through the admin categories endpoint', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: { id: 2 } });
    const wrapper = mount(CategoryAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-switch', 'el-button'] } });

    await wrapper.find('[aria-label="分类名称"]').setValue('办公用品');
    await wrapper.find('[aria-label="排序"]').setValue('3');
    await wrapper.find('[aria-label="备注"]').setValue('常用');
    await wrapper.find('[data-test="create-category"]').trigger('click');

    expect(http.post).toHaveBeenCalledWith('/admin/categories', {
      name: '办公用品',
      enabled: true,
      sortOrder: 3,
      remark: '常用'
    });
  });

  it('passes employee, category, status, and payment date filters to admin reimbursement API', async () => {
    const wrapper = mount(ReimbursementAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-form', 'el-form-item', 'el-input', 'el-select', 'el-option', 'el-button'] } });

    await wrapper.find('[aria-label="员工ID"]').setValue('11');
    await wrapper.find('[aria-label="分类ID"]').setValue('7');
    await wrapper.find('[aria-label="状态"]').setValue('ARCHIVED');
    await wrapper.find('[aria-label="开始日期"]').setValue('2026-05-01');
    await wrapper.find('[aria-label="结束日期"]').setValue('2026-05-31');
    await wrapper.find('form.filters').trigger('submit');

    expect(http.get).toHaveBeenCalledWith('/admin/reimbursements', {
      params: {
        employeeId: 11,
        categoryId: 7,
        status: 'ARCHIVED',
        from: '2026-05-01',
        to: '2026-05-31'
      }
    });
  });

  it('adds selected records to a batch and downloads Excel and attachment exports', async () => {
    Object.defineProperty(URL, 'createObjectURL', { value: vi.fn(), configurable: true });
    Object.defineProperty(URL, 'revokeObjectURL', { value: vi.fn(), configurable: true });
    const createObjectURL = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:export');
    const revokeObjectURL = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const click = vi.fn();
    const originalCreateElement = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation((tagName: string) => {
      const element = originalCreateElement(tagName);
      if (tagName === 'a') element.click = click;
      return element;
    });
    vi.mocked(http.get).mockImplementation((url: string) => {
      if (url === '/admin/batches/1') return Promise.resolve({ data: { id: 1, name: '五月批次', items: [] } });
      if (url === '/admin/batches/1/export/excel') return Promise.resolve({ data: new Blob(['excel']) });
      if (url === '/admin/batches/1/export/attachments') return Promise.resolve({ data: new Blob(['zip']) });
      return Promise.resolve({ data: [] });
    });
    vi.mocked(http.post).mockResolvedValue({ data: {} });

    const wrapper = mount(BatchAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-form', 'el-form-item', 'el-input', 'el-button'] } });
    expect(wrapper.text()).toContain('批次列表');
    expect(wrapper.find('.enterprise-card').exists()).toBe(true);
    await wrapper.find('[aria-label="批次ID"]').setValue('1');
    await wrapper.find('[data-test="load-batch"]').trigger('click');
    await wrapper.find('[aria-label="报销记录ID"]').setValue('99');
    await wrapper.find('[data-test="add-record"]').trigger('click');
    await wrapper.find('[data-test="export-excel"]').trigger('click');
    await wrapper.find('[data-test="export-attachments"]').trigger('click');

    expect(http.post).toHaveBeenCalledWith('/admin/batches/1/items/99');
    expect(http.get).toHaveBeenCalledWith('/admin/batches/1/export/excel', { responseType: 'blob' });
    expect(http.get).toHaveBeenCalledWith('/admin/batches/1/export/attachments', { responseType: 'blob' });
    expect(createObjectURL).toHaveBeenCalledTimes(2);
    expect(click).toHaveBeenCalledTimes(2);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:export');
  });

  it('archives the loaded batch through the archive endpoint', async () => {
    vi.mocked(http.get).mockImplementation((url: string) => {
      if (url === '/admin/batches/5') return Promise.resolve({ data: { id: 5, name: '五月批次', items: [] } });
      return Promise.resolve({ data: [] });
    });
    vi.mocked(http.post).mockResolvedValue({ data: {} });

    const wrapper = mount(BatchAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-form', 'el-form-item', 'el-input', 'el-button'] } });
    await wrapper.find('[aria-label="批次ID"]').setValue('5');
    await wrapper.find('[data-test="load-batch"]').trigger('click');
    await wrapper.find('[data-test="archive-batch"]').trigger('click');

    expect(http.post).toHaveBeenCalledWith('/admin/batches/5/archive');
  });
});
