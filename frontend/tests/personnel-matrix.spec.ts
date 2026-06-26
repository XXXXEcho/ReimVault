// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import PersonnelMatrix from '../src/components/PersonnelMatrix.vue';
import type { PersonnelMatrix as PersonnelMatrixData } from '../src/api/stats';

const MATRIX: PersonnelMatrixData = {
  columns: [
    { batchId: 10, batchName: '2026年4月报销批次', monthLabel: '2026-04' },
    { batchId: 11, batchName: '2026年5月报销批次', monthLabel: '2026-05' }
  ],
  rows: [
    {
      employeeId: 1,
      employeeName: '张三',
      department: '研发部',
      cells: [{ amount: 100, count: 1 }, { amount: 200, count: 1 }],
      unassigned: { amount: 80, count: 2 },
      total: { amount: 380, count: 4 }
    }
  ],
  totals: {
    columnTotals: [{ amount: 100, count: 1 }, { amount: 200, count: 1 }],
    unassignedTotal: { amount: 80, count: 2 },
    grandTotal: { amount: 380, count: 4 }
  }
};

describe('PersonnelMatrix', () => {
  it('renders employee rows, monthly columns and amounts', () => {
    const wrapper = mount(PersonnelMatrix, { props: { matrix: MATRIX } });
    expect(wrapper.find('[data-test="matrix-row-1"]').text()).toContain('张三');
    expect(wrapper.find('[data-test="matrix-column-10"]').text()).toContain('2026年4月报销批次');
    expect(wrapper.find('[data-test="matrix-cell-1-10"]').text()).toContain('100.00');
    expect(wrapper.find('[data-test="matrix-cell-1-11"]').text()).toContain('200.00');
    expect(wrapper.find('[data-test="matrix-cell-1-unassigned"]').text()).toContain('80.00');
    expect(wrapper.find('[data-test="matrix-totals-row"]').text()).toContain('380.00');
  });

  it('shows placeholder for empty cells', () => {
    const data: PersonnelMatrixData = {
      ...MATRIX,
      rows: [
        {
          employeeId: 2,
          employeeName: '李四',
          department: '市场部',
          cells: [{ amount: 0, count: 0 }, { amount: 0, count: 0 }],
          unassigned: { amount: 0, count: 0 },
          total: { amount: 0, count: 0 }
        }
      ]
    };
    const wrapper = mount(PersonnelMatrix, { props: { matrix: data } });
    expect(wrapper.find('[data-test="matrix-cell-2-10"]').text()).toContain('—');
  });

  it('shows empty state when matrix has no rows', () => {
    const wrapper = mount(PersonnelMatrix, { props: { matrix: null } });
    expect(wrapper.find('[data-test="matrix-empty"]').exists()).toBe(true);
  });
});
