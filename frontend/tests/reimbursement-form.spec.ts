// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import ReimbursementForm from '../src/components/ReimbursementForm.vue';
import http from '../src/api/http';
import { deleteAttachment } from '../src/api/reimbursements';

vi.mock('../src/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn()
  }
}));

describe('reimbursement form', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(URL, 'createObjectURL', { value: vi.fn((file: File) => `blob:${file.name}`), configurable: true });
    Object.defineProperty(URL, 'revokeObjectURL', { value: vi.fn(), configurable: true });
    vi.mocked(http.get).mockResolvedValue({ data: [{ id: 7, name: '差旅', enabled: true, sortOrder: 1, remark: '' }] });
  });

  it('renders required fields and required/optional attachment labels', async () => {
    const wrapper = mount(ReimbursementForm, {
      global: { stubs: ['el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-select', 'el-option', 'el-date-picker', 'el-button', 'el-upload'] }
    });
    await Promise.resolve();

    expect(wrapper.text()).toContain('金额');
    expect(wrapper.text()).toContain('用途分类');
    expect(wrapper.text()).toContain('用途说明');
    expect(wrapper.text()).toContain('支付时间');
    expect(wrapper.text()).toContain('支付凭证（必填）');
    expect(wrapper.text()).toContain('订单截图（选填）');
    expect(wrapper.text()).toContain('发票（选填）');
  });

  it('does not save when amount, category, purpose, or payment time is missing', async () => {
    const wrapper = mount(ReimbursementForm, {
      global: { stubs: ['el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-select', 'el-option', 'el-date-picker', 'el-button', 'el-upload'] }
    });
    await Promise.resolve();

    await wrapper.find('[data-test="save-draft"]').trigger('click');

    expect(http.post).not.toHaveBeenCalledWith('/reimbursements', expect.anything());
    expect(wrapper.text()).toContain('请填写金额、用途分类、用途说明和支付时间');
  });

  it('previews selected image files before saving', async () => {
    const wrapper = mount(ReimbursementForm, {
      global: { stubs: ['el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-select', 'el-option', 'el-date-picker', 'el-button', 'el-upload'] }
    });
    await Promise.resolve();

    const paymentFile = new File(['payment'], 'payment.png', { type: 'image/png' });
    const input = wrapper.find('[data-test="payment-voucher-files"]').element as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [paymentFile], configurable: true });
    await wrapper.find('[data-test="payment-voucher-files"]').trigger('change');
    await flushPromises();

    const preview = wrapper.find('img[alt="payment.png"]');
    expect(preview.exists()).toBe(true);
    expect(preview.attributes('src')).toBe('blob:payment.png');
  });

  it('requires a selected payment voucher before submitting a new reimbursement', async () => {
    const wrapper = mount(ReimbursementForm, {
      global: { stubs: ['el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-select', 'el-option', 'el-date-picker', 'el-button', 'el-upload'] }
    });
    await Promise.resolve();

    await wrapper.find('[aria-label="金额"]').setValue('88');
    await wrapper.find('[aria-label="用途分类"]').setValue('7');
    await wrapper.find('[aria-label="用途说明"]').setValue('客户拜访');
    await wrapper.find('[aria-label="支付时间"]').setValue('2026-05-21T10:00:00');
    await wrapper.find('[data-test="submit-reimbursement"]').trigger('click');
    await Promise.resolve();

    expect(http.post).not.toHaveBeenCalledWith('/reimbursements', expect.anything());
    expect(wrapper.text()).toContain('请先选择至少一张支付凭证');
  });

  it('saves a draft before submitting a new reimbursement with a selected payment voucher', async () => {
    vi.mocked(http.post)
      .mockResolvedValueOnce({ data: { id: 42, amount: 88, categoryId: 7, purpose: '客户拜访', paymentTime: '2026-05-21T10:00:00', status: 'DRAFT' } })
      .mockResolvedValueOnce({ data: { id: 1 } })
      .mockResolvedValueOnce({ data: { id: 42, status: 'SUBMITTED' } });

    const wrapper = mount(ReimbursementForm, {
      global: { stubs: ['el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-select', 'el-option', 'el-date-picker', 'el-button', 'el-upload'] }
    });
    await Promise.resolve();

    const paymentFile = new File(['payment'], 'payment.png', { type: 'image/png' });
    const input = wrapper.find('[data-test="payment-voucher-files"]').element as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [paymentFile], configurable: true });
    await wrapper.find('[data-test="payment-voucher-files"]').trigger('change');
    await wrapper.find('[aria-label="金额"]').setValue('88');
    await wrapper.find('[aria-label="用途分类"]').setValue('7');
    await wrapper.find('[aria-label="用途说明"]').setValue('客户拜访');
    await wrapper.find('[aria-label="支付时间"]').setValue('2026-05-21T10:00:00');
    await wrapper.find('[data-test="submit-reimbursement"]').trigger('click');
    await flushPromises();

    expect(http.post).toHaveBeenNthCalledWith(1, '/reimbursements', {
      amount: 88,
      categoryId: 7,
      purpose: '客户拜访',
      paymentTime: new Date('2026-05-21T10:00:00').toISOString()
    });
    expect(http.post).toHaveBeenNthCalledWith(3, '/reimbursements/42/submit');
  });

  it('patches edited existing draft before submitting it', async () => {
    vi.mocked(http.get).mockImplementation((url: string) => {
      if (url === '/categories') return Promise.resolve({ data: [{ id: 7, name: '差旅', enabled: true, sortOrder: 1, remark: '' }] });
      if (url === '/reimbursements/9') {
        return Promise.resolve({ data: { id: 9, amount: 88, categoryId: 7, purpose: '旧用途', paymentTime: '2026-05-21T02:00:00.000Z', status: 'DRAFT' } });
      }
      return Promise.resolve({ data: [] });
    });
    vi.mocked(http.patch).mockResolvedValue({ data: { id: 9, amount: 99, categoryId: 7, purpose: '新用途', paymentTime: '2026-05-21T10:00:00', status: 'DRAFT' } });
    vi.mocked(http.post).mockResolvedValue({ data: { id: 9, status: 'SUBMITTED' } });

    const wrapper = mount(ReimbursementForm, {
      props: { id: 9 },
      global: { stubs: ['el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-select', 'el-option', 'el-date-picker', 'el-button', 'el-upload'] }
    });
    await flushPromises();

    expect((wrapper.find('[aria-label="支付时间"]').element as HTMLInputElement).value).toBe('2026-05-21T10:00');
    await wrapper.find('[aria-label="金额"]').setValue('99');
    await wrapper.find('[aria-label="用途说明"]').setValue('新用途');
    await wrapper.find('[data-test="submit-reimbursement"]').trigger('click');
    await Promise.resolve();

    expect(http.patch).toHaveBeenCalledWith('/reimbursements/9', {
      amount: 99,
      categoryId: 7,
      purpose: '新用途',
      paymentTime: new Date('2026-05-21T10:00:00').toISOString()
    });
    expect(http.post).toHaveBeenCalledWith('/reimbursements/9/submit');
  });

  it('previews uploaded image attachments immediately when editing an existing draft', async () => {
    vi.mocked(http.get).mockImplementation((url: string) => {
      if (url === '/categories') return Promise.resolve({ data: [{ id: 7, name: '差旅', enabled: true, sortOrder: 1, remark: '' }] });
      if (url === '/reimbursements/9') {
        return Promise.resolve({ data: { id: 9, amount: 88, categoryId: 7, purpose: '旧用途', paymentTime: '2026-05-21T10:00:00', status: 'DRAFT', attachments: [] } });
      }
      return Promise.resolve({ data: [] });
    });
    vi.mocked(http.post).mockResolvedValue({ data: { id: 31, type: 'PAYMENT_VOUCHER', originalFilename: 'uploaded.png', contentType: 'image/png', sizeBytes: 1024, createdAt: '2026-05-21T10:00:00Z' } });

    const wrapper = mount(ReimbursementForm, {
      props: { id: 9 },
      global: { stubs: ['el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-select', 'el-option', 'el-date-picker', 'el-button', 'el-upload'] }
    });
    await flushPromises();

    const paymentFile = new File(['payment'], 'uploaded.png', { type: 'image/png' });
    const input = wrapper.find('[data-test="payment-voucher-files"]').element as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [paymentFile], configurable: true });
    await wrapper.find('[data-test="payment-voucher-files"]').trigger('change');
    await flushPromises();

    const preview = wrapper.find('img[alt="uploaded.png"]');
    expect(preview.exists()).toBe(true);
    expect(preview.attributes('src')).toBe('/api/attachments/31');
  });

  it('opens and closes a large image preview from an attachment thumbnail', async () => {
    vi.mocked(http.get).mockImplementation((url: string) => {
      if (url === '/categories') return Promise.resolve({ data: [{ id: 7, name: '差旅', enabled: true, sortOrder: 1, remark: '' }] });
      if (url === '/reimbursements/9') {
        return Promise.resolve({ data: {
          id: 9,
          amount: 88,
          categoryId: 7,
          purpose: '旧用途',
          paymentTime: '2026-05-21T10:00:00',
          status: 'DRAFT',
          attachments: [
            { id: 11, type: 'PAYMENT_VOUCHER', originalFilename: '支付截图.png', contentType: 'image/png', sizeBytes: 1024, createdAt: '2026-05-21T10:00:00Z' }
          ]
        } });
      }
      return Promise.resolve({ data: [] });
    });

    const wrapper = mount(ReimbursementForm, {
      props: { id: 9 },
      global: { stubs: ['el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-select', 'el-option', 'el-date-picker', 'el-button', 'el-upload'] }
    });
    await flushPromises();

    await wrapper.find('img[alt="支付截图.png"]').trigger('click');

    const dialog = wrapper.find('[role="dialog"]');
    expect(dialog.exists()).toBe(true);
    expect(dialog.find('img[alt="支付截图.png"]').attributes('src')).toBe('/api/attachments/11');

    await dialog.find('[aria-label="关闭大图预览"]').trigger('click');

    expect(wrapper.find('[role="dialog"]').exists()).toBe(false);
  });

  it('previews existing image attachments and links non-image attachments', async () => {
    vi.mocked(http.get).mockImplementation((url: string) => {
      if (url === '/categories') return Promise.resolve({ data: [{ id: 7, name: '差旅', enabled: true, sortOrder: 1, remark: '' }] });
      if (url === '/reimbursements/9') {
        return Promise.resolve({ data: {
          id: 9,
          amount: 88,
          categoryId: 7,
          purpose: '旧用途',
          paymentTime: '2026-05-21T10:00:00',
          status: 'DRAFT',
          attachments: [
            { id: 11, type: 'PAYMENT_VOUCHER', originalFilename: '支付截图.png', contentType: 'image/png', sizeBytes: 1024, createdAt: '2026-05-21T10:00:00Z' },
            { id: 12, type: 'INVOICE', originalFilename: '发票.pdf', contentType: 'application/pdf', sizeBytes: 2048, createdAt: '2026-05-21T10:00:00Z' }
          ]
        } });
      }
      return Promise.resolve({ data: [] });
    });

    const wrapper = mount(ReimbursementForm, {
      props: { id: 9 },
      global: { stubs: ['el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-select', 'el-option', 'el-date-picker', 'el-button', 'el-upload'] }
    });
    await flushPromises();

    const preview = wrapper.find('img[alt="支付截图.png"]');
    expect(preview.exists()).toBe(true);
    expect(preview.attributes('src')).toBe('/api/attachments/11');

    const link = wrapper.find('a[href="/api/attachments/12"]');
    expect(link.text()).toContain('发票.pdf');
  });

  it('shows backend submit errors for an existing draft without requiring a new payment voucher selection', async () => {
    vi.mocked(http.get).mockImplementation((url: string) => {
      if (url === '/categories') return Promise.resolve({ data: [{ id: 7, name: '差旅', enabled: true, sortOrder: 1, remark: '' }] });
      if (url === '/reimbursements/9') {
        return Promise.resolve({ data: { id: 9, amount: 88, categoryId: 7, purpose: '旧用途', paymentTime: '2026-05-21T10:00:00', status: 'DRAFT' } });
      }
      return Promise.resolve({ data: [] });
    });
    vi.mocked(http.patch).mockResolvedValue({ data: { id: 9, amount: 88, categoryId: 7, purpose: '旧用途', paymentTime: '2026-05-21T10:00:00', status: 'DRAFT' } });
    vi.mocked(http.post).mockRejectedValue({ response: { data: { message: '至少上传一张支付凭证' } } });

    const wrapper = mount(ReimbursementForm, {
      props: { id: 9 },
      global: { stubs: ['el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-select', 'el-option', 'el-date-picker', 'el-button', 'el-upload'] }
    });
    await Promise.resolve();
    await Promise.resolve();

    await wrapper.find('[data-test="submit-reimbursement"]').trigger('click');
    await flushPromises();

    expect(http.patch).toHaveBeenCalledWith('/reimbursements/9', expect.anything());
    expect(http.post).toHaveBeenCalledWith('/reimbursements/9/submit');
    expect(wrapper.text()).toContain('至少上传一张支付凭证');
  });

  it('previews attachments immediately after uploading selected files', async () => {
    vi.mocked(http.post)
      .mockResolvedValueOnce({ data: { id: 42, amount: 88, categoryId: 7, purpose: '客户拜访', paymentTime: '2026-05-21T10:00:00', status: 'DRAFT' } })
      .mockResolvedValueOnce({ data: { id: 21, type: 'PAYMENT_VOUCHER', originalFilename: 'payment.png', contentType: 'image/png', sizeBytes: 1024, createdAt: '2026-05-21T10:00:00Z' } });

    const wrapper = mount(ReimbursementForm, {
      global: { stubs: ['el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-select', 'el-option', 'el-date-picker', 'el-button', 'el-upload'] }
    });
    await Promise.resolve();

    const paymentFile = new File(['payment'], 'payment.png', { type: 'image/png' });
    const input = wrapper.find('[data-test="payment-voucher-files"]').element as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [paymentFile], configurable: true });
    await wrapper.find('[data-test="payment-voucher-files"]').trigger('change');
    await wrapper.find('[aria-label="金额"]').setValue('88');
    await wrapper.find('[aria-label="用途分类"]').setValue('7');
    await wrapper.find('[aria-label="用途说明"]').setValue('客户拜访');
    await wrapper.find('[aria-label="支付时间"]').setValue('2026-05-21T10:00:00');
    await wrapper.find('[data-test="save-draft"]').trigger('click');
    await flushPromises();

    const preview = wrapper.find('img[alt="payment.png"]');
    expect(preview.exists()).toBe(true);
    expect(preview.attributes('src')).toBe('/api/attachments/21');
  });

  it('uploads selected attachments after creating a draft', async () => {
    vi.mocked(http.post)
      .mockResolvedValueOnce({ data: { id: 42, amount: 88, categoryId: 7, purpose: '客户拜访', paymentTime: '2026-05-21T10:00:00', status: 'DRAFT' } })
      .mockResolvedValue({ data: { id: 1 } });

    const wrapper = mount(ReimbursementForm, {
      global: { stubs: ['el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-select', 'el-option', 'el-date-picker', 'el-button', 'el-upload'] }
    });
    await Promise.resolve();

    const paymentFile = new File(['payment'], 'payment.png', { type: 'image/png' });
    const orderFile = new File(['order'], 'order.png', { type: 'image/png' });
    const invoiceFile = new File(['invoice'], 'invoice.pdf', { type: 'application/pdf' });

    for (const [selector, file] of [
      ['[data-test="payment-voucher-files"]', paymentFile],
      ['[data-test="order-screenshot-files"]', orderFile],
      ['[data-test="invoice-files"]', invoiceFile]
    ] as const) {
      const input = wrapper.find(selector).element as HTMLInputElement;
      Object.defineProperty(input, 'files', { value: [file], configurable: true });
      await wrapper.find(selector).trigger('change');
    }

    await wrapper.find('[aria-label="金额"]').setValue('88');
    await wrapper.find('[aria-label="用途分类"]').setValue('7');
    await wrapper.find('[aria-label="用途说明"]').setValue('客户拜访');
    await wrapper.find('[aria-label="支付时间"]').setValue('2026-05-21T10:00:00');
    await wrapper.find('[data-test="save-draft"]').trigger('click');

    expect(http.post).toHaveBeenNthCalledWith(2, '/reimbursements/42/attachments?type=PAYMENT_VOUCHER', expect.any(FormData));
    expect(http.post).toHaveBeenNthCalledWith(3, '/reimbursements/42/attachments?type=ORDER_SCREENSHOT', expect.any(FormData));
    expect(http.post).toHaveBeenNthCalledWith(4, '/reimbursements/42/attachments?type=INVOICE', expect.any(FormData));
  });

  it('deletes attachments through the Task 13 attachment endpoint', () => {
    deleteAttachment(77);

    expect(http.delete).toHaveBeenCalledWith('/attachments/77');
  });
});
