// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import MaterialList from '../src/components/MaterialList.vue';
import http from '../src/api/http';

vi.mock('../src/api/http', () => ({
  default: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() }
}));

const attachment = { id: 9, type: 'PAYMENT_VOUCHER', originalFilename: 'pay.png', contentType: 'image/png', sizeBytes: 11, createdAt: '2026-05-21T03:00:00Z' };

function mountList(status: 'DRAFT' | 'SUBMITTED' = 'DRAFT') {
  return mount(MaterialList, { props: { recordId: 1, status, attachments: [attachment] } });
}

describe('MaterialList', () => {
  beforeEach(() => vi.clearAllMocks());

  it('groups attachments and emits preview', async () => {
    const wrapper = mountList('SUBMITTED');
    expect(wrapper.text()).toContain('支付凭证');
    expect(wrapper.text()).toContain('pay.png');
    await wrapper.find('[data-test="preview-9"]').trigger('click');
    expect(wrapper.emitted('preview')?.[0]).toEqual([9]);
  });

  it('hides draft-only upload and delete controls for non-draft records while keeping preview and download', () => {
    const wrapper = mountList('SUBMITTED');

    expect(wrapper.find('[data-test="upload-PAYMENT_VOUCHER"]').exists()).toBe(false);
    expect(wrapper.find('[data-test="delete-9"]').exists()).toBe(false);
    expect(wrapper.find('[data-test="preview-9"]').exists()).toBe(true);
    expect(wrapper.find('a').text()).toContain('下载');
  });

  it('uses the attachment download endpoint for download links', () => {
    const wrapper = mountList('SUBMITTED');
    const href = wrapper.find('a').attributes('href') ?? '';

    expect(href === '/api/attachments/9' || href.endsWith('/api/attachments/9')).toBe(true);
    expect(wrapper.find('a').text()).toContain('下载');
  });

  it('distinguishes upload controls by material group accessible labels', () => {
    const wrapper = mount(MaterialList, { props: { recordId: 1, status: 'DRAFT', attachments: [] } });

    expect(wrapper.find('[aria-label="上传支付凭证"]').exists()).toBe(true);
  });

  it('uploads file for draft record', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: attachment });
    const wrapper = mount(MaterialList, { props: { recordId: 1, status: 'DRAFT', attachments: [] } });
    const file = new File(['pay'], 'pay.png', { type: 'image/png' });
    const input = wrapper.find('[data-test="upload-PAYMENT_VOUCHER"]').element as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [file], configurable: true });
    await wrapper.find('[data-test="upload-PAYMENT_VOUCHER"]').trigger('change');
    expect(http.post).toHaveBeenCalledWith('/reimbursements/1/attachments?type=PAYMENT_VOUCHER', expect.any(FormData));
    expect(wrapper.emitted('changed')).toHaveLength(1);
  });

  it('shows upload API errors without emitting changed', async () => {
    vi.mocked(http.post).mockRejectedValue({ response: { data: { message: '上传失败' } } });
    const wrapper = mount(MaterialList, { props: { recordId: 1, status: 'DRAFT', attachments: [] } });
    const file = new File(['pay'], 'pay.png', { type: 'image/png' });
    const input = wrapper.find('[data-test="upload-PAYMENT_VOUCHER"]').element as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [file], configurable: true });

    await wrapper.find('[data-test="upload-PAYMENT_VOUCHER"]').trigger('change');

    expect(wrapper.find('[role="alert"]').text()).toContain('上传失败');
    expect(wrapper.emitted('changed')).toBeUndefined();
  });

  it('confirms and deletes draft attachment', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(http.delete).mockResolvedValue({});
    const wrapper = mountList('DRAFT');
    await wrapper.find('[data-test="delete-9"]').trigger('click');
    expect(http.delete).toHaveBeenCalledWith('/attachments/9');
    expect(wrapper.emitted('changed')).toHaveLength(1);
  });

  it('shows delete API errors without emitting changed', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(http.delete).mockRejectedValue({ response: { data: { message: '删除失败' } } });
    const wrapper = mountList('DRAFT');

    await wrapper.find('[data-test="delete-9"]').trigger('click');

    expect(wrapper.find('[role="alert"]').text()).toContain('删除失败');
    expect(wrapper.emitted('changed')).toBeUndefined();
  });
});
