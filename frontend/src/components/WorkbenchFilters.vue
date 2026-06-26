<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { listAdminCategories, listCategories, type Category } from '../api/categories';
import { listOaNumbers, type OaNumber } from '../api/oa';
import { searchEmployees, type UserRecord } from '../api/users';

const filters = defineModel<{
  employeeId?: string;
  categoryId?: string;
  status?: string;
  from?: string;
  to?: string;
  keyword?: string;
  oaId?: string;
  reimbursed?: string;
}>({ required: true });

const props = defineProps<{
  admin?: boolean;
}>();

const emit = defineEmits<{
  apply: [];
  reset: [];
}>();

const categories = ref<Category[]>([]);
const oaNumbers = ref<OaNumber[]>([]);
const employees = ref<UserRecord[]>([]);
const employeeKeyword = ref('');

function updateFilter(key: keyof typeof filters.value, value: string) {
  filters.value = { ...filters.value, [key]: value };
}

function updateEmployeeKeyword(value: string) {
  employeeKeyword.value = value;
  if (/^\d+$/.test(value)) updateFilter('employeeId', value);
}

function fieldValue(form: HTMLFormElement, ariaLabel: string) {
  return (form.querySelector(`[aria-label="${ariaLabel}"]`) as HTMLInputElement | HTMLSelectElement | null)?.value ?? '';
}

function applyForm(form: HTMLFormElement) {
  filters.value = {
    ...filters.value,
    employeeId: props.admin ? filters.value.employeeId : filters.value.employeeId,
    categoryId: fieldValue(form, '分类ID'),
    status: fieldValue(form, '状态'),
    from: fieldValue(form, '开始日期'),
    to: fieldValue(form, '结束日期'),
    keyword: fieldValue(form, '关键词'),
    oaId: props.admin ? fieldValue(form, '经费编码') : filters.value.oaId,
    reimbursed: props.admin ? fieldValue(form, '报销状态') : filters.value.reimbursed
  };
  emit('apply');
}

function submitFilters(event: SubmitEvent) {
  applyForm(event.currentTarget as HTMLFormElement);
}

function clickApply(event: MouseEvent) {
  applyForm((event.currentTarget as HTMLButtonElement).form as HTMLFormElement);
}

async function loadEmployees() {
  if (!props.admin) return;
  const response = await searchEmployees(employeeKeyword.value);
  employees.value = response.data;
}

onMounted(async () => {
  const [response, oaResponse] = await Promise.all([
    props.admin ? listAdminCategories() : listCategories(),
    props.admin ? listOaNumbers() : Promise.resolve({ data: [] as OaNumber[] })
  ]);
  categories.value = response.data;
  oaNumbers.value = oaResponse.data;
  await loadEmployees();
});
</script>

<template>
  <form class="filters workbench-filters enterprise-card" @submit.prevent="submitFilters">
    <label v-if="props.admin" class="workbench-filters__field workbench-filters__span">
      <span>员工</span>
      <div class="workbench-filters__employee">
        <input :value="employeeKeyword" aria-label="员工ID" type="search" placeholder="输入姓名搜索" @input="updateEmployeeKeyword(($event.target as HTMLInputElement).value)" @change="loadEmployees" />
        <select :value="filters.employeeId" aria-label="员工" @change="updateFilter('employeeId', ($event.target as HTMLSelectElement).value)">
          <option value="">全部员工</option>
          <option v-for="employee in employees" :key="employee.id" :value="employee.id">
            {{ employee.displayName }} · {{ employee.department || employee.username }}
          </option>
        </select>
      </div>
    </label>
    <label class="workbench-filters__field">
      <span>用途分类</span>
      <select :value="filters.categoryId" aria-label="分类ID" @change="updateFilter('categoryId', ($event.target as HTMLSelectElement).value)">
        <option value="">全部分类</option>
        <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
      </select>
    </label>
    <label class="workbench-filters__field">
      <span>状态</span>
      <select :value="filters.status" aria-label="状态" @change="updateFilter('status', ($event.target as HTMLSelectElement).value)">
        <option value="">全部状态</option>
        <option value="DRAFT">草稿</option>
        <option value="SUBMITTED">待报销</option>
        <option value="ARCHIVED">已归档</option>
      </select>
    </label>
    <label v-if="props.admin" class="workbench-filters__field">
      <span>经费编码</span>
      <select :value="filters.oaId" aria-label="经费编码" @change="updateFilter('oaId', ($event.target as HTMLSelectElement).value)">
        <option value="">全部编码</option>
        <option v-for="oa in oaNumbers" :key="oa.id" :value="oa.id">{{ oa.number }}</option>
      </select>
    </label>
    <label v-if="props.admin" class="workbench-filters__field">
      <span>报销状态</span>
      <select :value="filters.reimbursed" aria-label="报销状态" @change="updateFilter('reimbursed', ($event.target as HTMLSelectElement).value)">
        <option value="">全部</option>
        <option value="false">未报销</option>
        <option value="true">已报销</option>
      </select>
    </label>
    <label class="workbench-filters__field">
      <span>开始日期</span>
      <input :value="filters.from" aria-label="开始日期" type="date" @input="updateFilter('from', ($event.target as HTMLInputElement).value)" />
    </label>
    <label class="workbench-filters__field">
      <span>结束日期</span>
      <input :value="filters.to" aria-label="结束日期" type="date" @input="updateFilter('to', ($event.target as HTMLInputElement).value)" />
    </label>
    <label class="workbench-filters__field">
      <span>关键词</span>
      <input :value="filters.keyword" aria-label="关键词" type="search" placeholder="用途、备注" @input="updateFilter('keyword', ($event.target as HTMLInputElement).value)" />
    </label>
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

.workbench-filters__field {
  display: grid;
  gap: var(--space-2);
}

.workbench-filters__field span {
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  font-weight: 700;
}

.workbench-filters__span {
  grid-column: span 2;
}

.workbench-filters__employee {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) minmax(160px, 1fr);
  gap: var(--space-2);
}

.workbench-filters__actions {
  display: flex;
  gap: var(--space-2);
}

.workbench-filters__actions button {
  flex: 1;
}

@media (max-width: 640px) {
  .workbench-filters__span {
    grid-column: auto;
  }

  .workbench-filters__employee {
    grid-template-columns: 1fr;
  }
}
</style>
