// @vitest-environment jsdom
import { afterEach, describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import MaterialPreviewer from '../src/components/MaterialPreviewer.vue';
import type { AttachmentRecord } from '../src/api/reimbursements';

const attachments: AttachmentRecord[] = [
  { id: 1, type: 'PAYMENT_VOUCHER', originalFilename: 'pay.png', contentType: 'image/png', sizeBytes: 11, createdAt: '2026-05-21T03:00:00Z' },
  { id: 2, type: 'INVOICE', originalFilename: 'invoice.pdf', contentType: 'application/pdf', sizeBytes: 22, createdAt: '2026-05-21T03:00:00Z' },
  { id: 3, type: 'ORDER_SCREENSHOT', originalFilename: 'archive.zip', contentType: 'application/zip', sizeBytes: 33, createdAt: '2026-05-21T03:00:00Z' }
];

afterEach(() => {
  document.body.innerHTML = '';
});

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

  it('wraps previous navigation from first attachment to last attachment', async () => {
    const wrapper = mount(MaterialPreviewer, { props: { attachments, activeId: 1 } });
    await wrapper.find('[data-test="previous-preview"]').trigger('click');
    expect(wrapper.text()).toContain('archive.zip');
    expect(wrapper.find('[data-test="download-active"]').attributes('href')).toBe('/api/attachments/3');
  });

  it('renders fallback text and download link for unsupported files', () => {
    const wrapper = mount(MaterialPreviewer, { props: { attachments, activeId: 3 } });
    const download = wrapper.find('[data-test="download-active"]');
    const fallbackDownload = wrapper.find('.material-previewer__fallback a');

    expect(wrapper.text()).toContain('该文件类型暂不支持在线预览，请下载后查看。');
    expect(download.attributes('href')).toBe('/api/attachments/3');
    expect(download.attributes('download')).toBe('archive.zip');
    expect(fallbackDownload.attributes('href')).toBe('/api/attachments/3');
    expect(fallbackDownload.attributes('download')).toBe('archive.zip');
  });

  it('supports Escape close and arrow key navigation with wrapping', async () => {
    const wrapper = mount(MaterialPreviewer, { props: { attachments, activeId: 1 } });
    const dialog = wrapper.find('[aria-label="材料预览"]');

    await dialog.trigger('keydown', { key: 'ArrowRight' });
    expect(wrapper.find('object').attributes('data')).toBe('/api/attachments/2');

    await dialog.trigger('keydown', { key: 'ArrowLeft' });
    expect(wrapper.find('img').attributes('src')).toBe('/api/attachments/1');

    await dialog.trigger('keydown', { key: 'ArrowLeft' });
    expect(wrapper.text()).toContain('archive.zip');

    await dialog.trigger('keydown', { key: 'Escape' });
    expect(wrapper.emitted('close')).toHaveLength(1);
  });

  it('labels PDF objects with the active filename', () => {
    const wrapper = mount(MaterialPreviewer, { props: { attachments, activeId: 2 } });
    const object = wrapper.find('object');

    expect(object.attributes('title')).toBe('invoice.pdf');
    expect(object.attributes('aria-label')).toBe('invoice.pdf');
    expect(wrapper.find('object a').attributes('download')).toBe('invoice.pdf');
  });

  it('adds a download attribute to the active download link', () => {
    const wrapper = mount(MaterialPreviewer, { props: { attachments, activeId: 1 } });

    expect(wrapper.find('[data-test="download-active"]').attributes('download')).toBe('pay.png');
  });

  it('restores focus when the previewer unmounts', async () => {
    const opener = document.createElement('button');
    opener.textContent = 'open preview';
    document.body.appendChild(opener);
    opener.focus();

    const wrapper = mount(MaterialPreviewer, { props: { attachments, activeId: 1 }, attachTo: document.body });
    await wrapper.vm.$nextTick();
    expect(document.activeElement).toBe(wrapper.find('[data-test="close-preview"]').element);

    wrapper.unmount();

    expect(document.activeElement).toBe(opener);
  });

  it('keeps tab focus inside previewer controls', async () => {
    const host = document.createElement('div');
    const before = document.createElement('button');
    const after = document.createElement('button');
    before.textContent = 'before';
    after.textContent = 'after';
    host.appendChild(before);
    document.body.appendChild(host);

    const wrapper = mount(MaterialPreviewer, { props: { attachments, activeId: 1 }, attachTo: host });
    host.appendChild(after);
    await wrapper.vm.$nextTick();

    const close = wrapper.find<HTMLButtonElement>('[data-test="close-preview"]').element;
    const next = wrapper.find<HTMLButtonElement>('[data-test="next-preview"]').element;
    const dialog = wrapper.find('[aria-label="材料预览"]');

    close.focus();
    await dialog.trigger('keydown', { key: 'Tab', shiftKey: true });
    expect(document.activeElement).toBe(next);

    await dialog.trigger('keydown', { key: 'Tab' });
    expect(document.activeElement).toBe(close);
    expect(document.activeElement).not.toBe(before);
    expect(document.activeElement).not.toBe(after);
  });
});
