// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import MaterialPreviewer from '../src/components/MaterialPreviewer.vue';
import type { AttachmentRecord } from '../src/api/reimbursements';

const attachments: AttachmentRecord[] = [
  { id: 1, type: 'PAYMENT_VOUCHER', originalFilename: 'pay.png', contentType: 'image/png', sizeBytes: 11, createdAt: '2026-05-21T03:00:00Z' },
  { id: 2, type: 'INVOICE', originalFilename: 'invoice.pdf', contentType: 'application/pdf', sizeBytes: 22, createdAt: '2026-05-21T03:00:00Z' }
];

describe('MaterialPreviewer', () => {
  it('renders image preview and switches to next attachment', async () => {
    const wrapper = mount(MaterialPreviewer, { props: { attachments, activeId: 1 } });
    expect(wrapper.find('img').attributes('src')).toBe('/api/attachments/1');
    await wrapper.find('[data-test="next-preview"]').trigger('click');
    expect(wrapper.find('object').attributes('data')).toBe('/api/attachments/2');
  });

  it('emits close and shows download link', async () => {
    const wrapper = mount(MaterialPreviewer, { props: { attachments, activeId: 1 } });
    expect(wrapper.find('[data-test="download-active"]').attributes('href')).toBe('/api/attachments/1');
    await wrapper.find('[data-test="close-preview"]').trigger('click');
    expect(wrapper.emitted('close')).toHaveLength(1);
  });
});
