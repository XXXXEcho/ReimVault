// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import MaterialList from '../src/components/MaterialList.vue';
import http from '../src/api/http';

vi.mock('../src/api/http', () => ({
  default: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() }
}));

const attachment = { id: 9, type: 'PAYMENT_VOUCHER', originalFilename: 'pay.png', contentType: 'image/png', sizeBytes: 11, createdAt: '2026-05-21T03:00:00Z' };

describe('MaterialList', () => {
  beforeEach(() => vi.clearAllMocks());

  it('groups attachments and emits preview', async () => {
    const wrapper = mount(MaterialList, { props: { recordId: 1, status: 'SUBMITTED', attachments: [attachment] } });
    expect(wrapper.text()).toContain('支付凭证');
    expect(wrapper.text()).toContain('pay.png');
    await wrapper.find('[data-test="preview-9"]').trigger('click');
    expect(wrapper.emitted('preview')?.[0]).toEqual([9]);
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

  it('confirms and deletes draft attachment', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(http.delete).mockResolvedValue({});
    const wrapper = mount(MaterialList, { props: { recordId: 1, status: 'DRAFT', attachments: [attachment] } });
    await wrapper.find('[data-test="delete-9"]').trigger('click');
    expect(http.delete).toHaveBeenCalledWith('/attachments/9');
    expect(wrapper.emitted('changed')).toHaveLength(1);
  });
});
