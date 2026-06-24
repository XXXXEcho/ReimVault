// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import UserAdminView from '../src/views/admin/UserAdminView.vue';
import CategoryAdminView from '../src/views/admin/CategoryAdminView.vue';
import ReimbursementAdminView from '../src/views/admin/ReimbursementAdminView.vue';
import BatchAdminView from '../src/views/admin/BatchAdminView.vue';
import http from '../src/api/http';
import { useAuthStore } from '../src/stores/auth';

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
    setActivePinia(createPinia());
    const auth = useAuthStore();
    auth.user = { id: 1, username: 'admin', displayName: '管理员', department: '管理部', role: 'ADMIN' };
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

  it('shows a success message after saving a user', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: { id: 1 } });
    const wrapper = mount(UserAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-drawer', 'el-form', 'el-form-item', 'el-input', 'el-select', 'el-option', 'el-switch', 'el-button'] } });

    await wrapper.find('[aria-label="用户名"]').setValue('zhangsan');
    await wrapper.find('[aria-label="姓名"]').setValue('张三');
    await wrapper.find('[aria-label="部门"]').setValue('销售部');
    await wrapper.find('[aria-label="密码"]').setValue('secret');
    await wrapper.find('[data-test="create-user"]').trigger('click');
    await vi.waitFor(() => expect(wrapper.text()).toContain('用户保存成功'));

    expect(wrapper.find('[role="status"]').text()).toContain('用户保存成功');
  });

  it('shows backend errors when saving a user fails', async () => {
    vi.mocked(http.post).mockRejectedValue({ response: { data: { message: '用户名已存在' } } });
    const wrapper = mount(UserAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-drawer', 'el-form', 'el-form-item', 'el-input', 'el-select', 'el-option', 'el-switch', 'el-button'] } });

    await wrapper.find('[aria-label="用户名"]').setValue('zhangsan');
    await wrapper.find('[aria-label="姓名"]').setValue('张三');
    await wrapper.find('[aria-label="部门"]').setValue('销售部');
    await wrapper.find('[aria-label="密码"]').setValue('secret');
    await wrapper.find('[data-test="create-user"]').trigger('click');
    await vi.waitFor(() => expect(wrapper.text()).toContain('用户名已存在'));

    expect(wrapper.find('[role="alert"]').text()).toContain('用户名已存在');
  });

  it('sends edited user profile and password to the admin users endpoint', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: [{ id: 7, username: 'lisi', displayName: '李四', department: '销售部', role: 'EMPLOYEE', enabled: true }]
    });
    vi.mocked(http.patch).mockResolvedValue({ data: { id: 7 } });
    const wrapper = mount(UserAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-drawer', 'el-form', 'el-form-item', 'el-input', 'el-select', 'el-option', 'el-switch', 'el-button'] } });
    await vi.waitFor(() => expect(wrapper.text()).toContain('lisi'));

    await wrapper.find('tbody button').trigger('click');
    await wrapper.find('[aria-label="姓名"]').setValue('李四改');
    await wrapper.find('[aria-label="部门"]').setValue('财务部');
    await wrapper.find('[aria-label="密码"]').setValue('new-secret');
    await wrapper.find('[aria-label="角色"]').setValue('ADMIN');
    await wrapper.find('[aria-label="启用"]').setValue(false);
    await wrapper.find('[data-test="create-user"]').trigger('click');

    expect(http.patch).toHaveBeenCalledWith('/admin/users/7', {
      displayName: '李四改',
      department: '财务部',
      password: 'new-secret',
      role: 'ADMIN',
      enabled: false
    });
  });

  it('resets edited user form before creating another user', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: [{ id: 7, username: 'lisi', displayName: '李四', department: '销售部', role: 'EMPLOYEE', enabled: true }]
    });
    vi.mocked(http.post).mockResolvedValue({ data: { id: 8 } });
    const wrapper = mount(UserAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-drawer', 'el-form', 'el-form-item', 'el-input', 'el-select', 'el-option', 'el-switch', 'el-button'] } });
    await vi.waitFor(() => expect(wrapper.text()).toContain('lisi'));

    await wrapper.find('tbody button').trigger('click');
    await wrapper.find('[data-test="new-user"]').trigger('click');
    await wrapper.find('[aria-label="用户名"]').setValue('wangwu');
    await wrapper.find('[aria-label="姓名"]').setValue('王五');
    await wrapper.find('[aria-label="部门"]').setValue('财务部');
    await wrapper.find('[aria-label="密码"]').setValue('secret');
    await wrapper.find('[data-test="create-user"]').trigger('click');

    expect(http.post).toHaveBeenCalledWith('/admin/users', {
      username: 'wangwu',
      displayName: '王五',
      department: '财务部',
      password: 'secret',
      role: 'EMPLOYEE'
    });
    expect(http.patch).not.toHaveBeenCalled();
    await vi.waitFor(() => expect(wrapper.text()).toContain('用户保存成功'));
    expect(wrapper.find('[role="status"]').text()).toContain('用户保存成功');
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

  it('shows a success message after saving a category', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: { id: 2 } });
    const wrapper = mount(CategoryAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-switch', 'el-button'] } });

    await wrapper.find('[aria-label="分类名称"]').setValue('办公用品');
    await wrapper.find('[aria-label="排序"]').setValue('3');
    await wrapper.find('[data-test="create-category"]').trigger('click');
    await vi.waitFor(() => expect(wrapper.text()).toContain('分类保存成功'));

    expect(wrapper.find('[role="status"]').text()).toContain('分类保存成功');
  });

  it('resets edited category form before creating another category', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: [{ id: 3, name: '交通费', enabled: false, sortOrder: 9, remark: '旧分类' }]
    });
    vi.mocked(http.post).mockResolvedValue({ data: { id: 4 } });
    const wrapper = mount(CategoryAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-switch', 'el-button'] } });
    await vi.waitFor(() => expect(wrapper.text()).toContain('交通费'));

    await wrapper.find('tbody button').trigger('click');
    await wrapper.find('[data-test="new-category"]').trigger('click');
    await wrapper.find('[aria-label="分类名称"]').setValue('差旅费');
    await wrapper.find('[aria-label="排序"]').setValue('1');
    await wrapper.find('[aria-label="备注"]').setValue('新分类');
    await wrapper.find('[data-test="create-category"]').trigger('click');

    expect(http.post).toHaveBeenCalledWith('/admin/categories', {
      name: '差旅费',
      enabled: true,
      sortOrder: 1,
      remark: '新分类'
    });
    expect(http.patch).not.toHaveBeenCalled();
    await vi.waitFor(() => expect(wrapper.text()).toContain('分类保存成功'));
    expect(wrapper.find('[role="status"]').text()).toContain('分类保存成功');
  });

  it('passes employee, category, status, and payment date filters to admin reimbursement API', async () => {
    const wrapper = mount(ReimbursementAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-form', 'el-form-item', 'el-input', 'el-select', 'el-option', 'el-button'] } });

    await wrapper.find('[aria-label="员工ID"]').setValue('11');
    await wrapper.find('[aria-label="分类ID"]').setValue('7');
    await wrapper.find('[aria-label="状态"]').setValue('ARCHIVED');
    await wrapper.find('[aria-label="开始日期"]').setValue('2026-05-01');
    await wrapper.find('[aria-label="结束日期"]').setValue('2026-05-31');
    await wrapper.find('form.inline-form').trigger('submit');

    expect(http.get).toHaveBeenCalledWith('/admin/reimbursements', {
      params: {
        employeeId: 11,
        categoryId: 7,
        status: 'ARCHIVED',
        from: '2026-05-01',
        to: '2026-05-31',
        reimbursed: undefined
      }
    });
  });

  it('previews filtered export records before enabling downloads', async () => {
    vi.mocked(http.get).mockImplementation((url: string) => {
      if (url === '/admin/batches') return Promise.resolve({ data: [{ id: 5, name: '五月批次', description: '月度报销', archivedAt: null, items: [] }] });
      if (url === '/oa-numbers') return Promise.resolve({ data: [{ id: 1, number: 'JF-001' }] });
      if (url === '/admin/batches/export/preview') return Promise.resolve({ data: [{ id: 88, employeeId: 2, employeeName: '员工一', amount: 128, categoryId: 3, categoryName: '办公用品', purpose: '五月命中', paymentTime: '2026-05-10T10:00:00Z', status: 'SUBMITTED', adminRemark: '', submittedAt: '2026-05-11T10:00:00Z', archivedAt: null, reimbursedAt: null, batchId: null, batchName: null, oaId: 1, oaNumber: 'JF-001' }] });
      if (url === '/admin/batches/5') return Promise.resolve({ data: { id: 5, name: '五月批次', description: '月度报销', archivedAt: null, items: [] } });
      return Promise.resolve({ data: [] });
    });

    const wrapper = mount(BatchAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-form', 'el-form-item', 'el-input', 'el-button'] } });
    await vi.waitFor(() => expect(wrapper.text()).toContain('JF-001'));

    const page = wrapper.text();
    expect(page).toContain('先查询并核对命中记录，再导出 Excel 或附件压缩包。');
    expect(page.indexOf('批量导出')).toBeLessThan(page.indexOf('创建批次'));
    expect(page.indexOf('创建批次')).toBeLessThan(page.indexOf('批次列表'));
    expect(page.indexOf('批次列表')).toBeLessThan(page.indexOf('高级操作：按 ID 维护批次'));
    expect(page).toContain('请选择筛选条件后查询结果');
    expect(wrapper.find('[data-test="export-filtered-excel"]').attributes('disabled')).toBeDefined();
    expect(wrapper.find('[data-test="export-filtered-attachments"]').attributes('disabled')).toBeDefined();

    await wrapper.find('input[type="checkbox"]').setValue(true);
    expect(wrapper.find('[data-test="export-filtered-excel"]').attributes('disabled')).toBeDefined();
    await wrapper.find('[data-test="preview-filtered-export"]').trigger('click');

    await vi.waitFor(() => expect(wrapper.text()).toContain('查询结果：1 条'));
    expect(wrapper.text()).toContain('员工一');
    expect(wrapper.text()).toContain('五月命中');
    expect(wrapper.text()).toContain('JF-001');
    expect(wrapper.find('[data-test="export-filtered-excel"]').attributes('disabled')).toBeUndefined();
    expect(wrapper.find('[data-test="export-filtered-attachments"]').attributes('disabled')).toBeUndefined();

    await wrapper.find('[aria-label="批次ID"]').setValue('5');
    await wrapper.find('[data-test="load-batch"]').trigger('click');
    await vi.waitFor(() => expect(wrapper.text()).toContain('当前批次：五月批次'));
    expect(wrapper.text()).toContain('导出当前批次 Excel');
    expect(wrapper.text()).toContain('下载当前批次附件压缩包');
    expect(wrapper.text()).toContain('归档当前批次');
  });

  it('shows filtered export preview errors without leaving the page stuck', async () => {
    vi.mocked(http.get).mockImplementation((url: string) => {
      if (url === '/admin/batches') return Promise.resolve({ data: [] });
      if (url === '/oa-numbers') return Promise.resolve({ data: [{ id: 1, number: 'JF-001' }] });
      if (url === '/admin/batches/export/preview') return Promise.reject({ response: { data: { message: '请先登录' } } });
      return Promise.resolve({ data: [] });
    });

    const wrapper = mount(BatchAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-form', 'el-form-item', 'el-input', 'el-button'] } });
    await vi.waitFor(() => expect(wrapper.text()).toContain('JF-001'));

    await wrapper.find('input[type="checkbox"]').setValue(true);
    await wrapper.find('[data-test="preview-filtered-export"]').trigger('click');

    await vi.waitFor(() => expect(wrapper.text()).toContain('请先登录'));
    expect(wrapper.find('[data-test="preview-filtered-export"]').text()).toBe('查询结果');
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
      if (url === '/admin/batches/5') return Promise.resolve({ data: { id: 5, name: '五月批次', items: [] } });
      if (url === '/admin/batches/5/export/excel') return Promise.resolve({ data: new Blob(['excel']) });
      if (url === '/admin/batches/5/export/attachments') return Promise.resolve({ data: new Blob(['zip']) });
      return Promise.resolve({ data: [] });
    });
    vi.mocked(http.post).mockResolvedValue({ data: {} });

    const wrapper = mount(BatchAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-form', 'el-form-item', 'el-input', 'el-button'] } });
    await wrapper.find('[aria-label="批次ID"]').setValue('5');
    await wrapper.find('[data-test="load-batch"]').trigger('click');
    await wrapper.find('[aria-label="报销记录ID"]').setValue('99');
    await wrapper.find('[data-test="add-record"]').trigger('click');
    await wrapper.find('[data-test="export-excel"]').trigger('click');
    await wrapper.find('[data-test="export-attachments"]').trigger('click');

    expect(http.post).toHaveBeenCalledWith('/admin/batches/5/items/99');
    expect(http.get).toHaveBeenCalledWith('/admin/batches/5/export/excel', { responseType: 'blob' });
    expect(http.get).toHaveBeenCalledWith('/admin/batches/5/export/attachments', { responseType: 'blob' });
    expect(createObjectURL).toHaveBeenCalledTimes(2);
    expect(click).toHaveBeenCalledTimes(2);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:export');
  });

  it('archives the loaded batch through the archive endpoint', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
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
