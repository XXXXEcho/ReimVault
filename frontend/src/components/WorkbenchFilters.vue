<script setup lang="ts">
const filters = defineModel<{
  employeeId?: string;
  categoryId?: string;
  status?: string;
  from?: string;
  to?: string;
  keyword?: string;
}>({ required: true });

const props = defineProps<{
  admin?: boolean;
}>();

const emit = defineEmits<{
  apply: [];
  reset: [];
}>();

function updateFilter(key: keyof typeof filters.value, value: string) {
  filters.value = { ...filters.value, [key]: value };
}

function fieldValue(form: HTMLFormElement, ariaLabel: string) {
  return (form.querySelector(`[aria-label="${ariaLabel}"]`) as HTMLInputElement | HTMLSelectElement | null)?.value ?? '';
}

function applyForm(form: HTMLFormElement) {
  filters.value = {
    ...filters.value,
    employeeId: props.admin ? fieldValue(form, '员工ID') : filters.value.employeeId,
    categoryId: fieldValue(form, '分类ID'),
    status: fieldValue(form, '状态'),
    from: fieldValue(form, '开始日期'),
    to: fieldValue(form, '结束日期'),
    keyword: fieldValue(form, '关键词')
  };
  emit('apply');
}

function submitFilters(event: SubmitEvent) {
  applyForm(event.currentTarget as HTMLFormElement);
}

function clickApply(event: MouseEvent) {
  applyForm((event.currentTarget as HTMLButtonElement).form as HTMLFormElement);
}
</script>

<template>
  <form class="filters workbench-filters enterprise-card" @submit.prevent="submitFilters">
    <input
      v-if="props.admin"
      :value="filters.employeeId"
      aria-label="员工ID"
      type="number"
      min="1"
      placeholder="员工ID"
      @input="updateFilter('employeeId', ($event.target as HTMLInputElement).value)"
    />
    <input
      :value="filters.categoryId"
      aria-label="分类ID"
      type="number"
      min="1"
      placeholder="分类ID"
      @input="updateFilter('categoryId', ($event.target as HTMLInputElement).value)"
    />
    <select :value="filters.status" aria-label="状态" @change="updateFilter('status', ($event.target as HTMLSelectElement).value)">
      <option value="">全部状态</option>
      <option value="DRAFT">草稿</option>
      <option value="SUBMITTED">已提交</option>
      <option value="ARCHIVED">已归档</option>
    </select>
    <input :value="filters.from" aria-label="开始日期" type="date" @input="updateFilter('from', ($event.target as HTMLInputElement).value)" />
    <input :value="filters.to" aria-label="结束日期" type="date" @input="updateFilter('to', ($event.target as HTMLInputElement).value)" />
    <input :value="filters.keyword" aria-label="关键词" type="search" placeholder="关键词" @input="updateFilter('keyword', ($event.target as HTMLInputElement).value)" />
    <div class="workbench-filters__actions">
      <button data-test="apply-filters" type="submit" @click.prevent="clickApply">筛选</button>
      <button type="button" @click="emit('reset')">重置</button>
    </div>
  </form>
</template>

<style scoped>
.workbench-filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--space-3);
  align-items: end;
  padding: var(--space-4);
}

.workbench-filters input,
.workbench-filters select,
.workbench-filters button {
  min-height: 44px;
}

.workbench-filters__actions {
  display: flex;
  gap: var(--space-2);
}

.workbench-filters__actions button {
  flex: 1;
}
</style>
