// @vitest-environment jsdom
import { describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import StatusTag from '../src/components/StatusTag.vue';
import MetricCard from '../src/components/MetricCard.vue';
import MaterialCompleteness from '../src/components/MaterialCompleteness.vue';
import EmptyState from '../src/components/EmptyState.vue';
import ConfirmAction from '../src/components/ConfirmAction.vue';

describe('UI primitives', () => {
  it('renders status labels with semantic class', () => {
    const wrapper = mount(StatusTag, { props: { status: 'SUBMITTED' } });
    expect(wrapper.text()).toBe('待报销');
    expect(wrapper.classes()).toContain('status-submitted');
  });

  it('renders metric card title and value', () => {
    const wrapper = mount(MetricCard, { props: { title: '已提交', value: 8, tone: 'primary' } });
    expect(wrapper.text()).toContain('已提交');
    expect(wrapper.text()).toContain('8');
  });

  it('shows missing required payment voucher', () => {
    const wrapper = mount(MaterialCompleteness, { props: { paymentVoucherCount: 0, orderScreenshotCount: 1, invoiceCount: 0 } });
    expect(wrapper.text()).toContain('支付凭证缺失');
    expect(wrapper.classes()).toContain('is-incomplete');
  });

  it('renders empty state with action slot', () => {
    const wrapper = mount(EmptyState, { props: { title: '暂无记录', description: '创建第一条报销记录' }, slots: { action: '<button>新建报销</button>' } });
    expect(wrapper.text()).toContain('暂无记录');
    expect(wrapper.text()).toContain('新建报销');
  });

  it('confirms before emitting confirm action', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const wrapper = mount(ConfirmAction, { props: { message: '确认删除？' }, slots: { default: '删除' } });
    await wrapper.find('button').trigger('click');
    expect(wrapper.emitted('confirm')).toHaveLength(1);
    confirm.mockRestore();
  });

  it('does not emit when confirmation is canceled', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    const wrapper = mount(ConfirmAction, { props: { message: '确认删除？' }, slots: { default: '删除' } });
    await wrapper.find('button').trigger('click');
    expect(wrapper.emitted('confirm')).toBeUndefined();
    confirm.mockRestore();
  });

  it('renders a semantic button trigger', () => {
    const wrapper = mount(ConfirmAction, { props: { message: '确认归档？' }, slots: { default: '归档' } });
    const trigger = wrapper.get('button');
    expect(trigger.attributes('type')).toBe('button');
    expect(trigger.text()).toBe('归档');
  });
});
