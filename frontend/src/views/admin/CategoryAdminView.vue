<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { createCategory, deleteCategory, listAdminCategories, updateCategory, type Category } from '../../api/categories';

const categories = ref<Category[]>([]);
const editingId = ref<number | null>(null);
const notice = ref<{ type: 'success' | 'error'; text: string } | null>(null);
const form = reactive({ name: '', enabled: true, sortOrder: 0, remark: '' });

function resetForm() {
  editingId.value = null;
  Object.assign(form, { name: '', enabled: true, sortOrder: 0, remark: '' });
}

function errorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '操作失败';
}

async function load() {
  const response = await listAdminCategories();
  categories.value = response.data;
}

function edit(category: Category) {
  editingId.value = category.id;
  Object.assign(form, category);
}

async function save() {
  notice.value = null;
  const payload = { name: form.name, enabled: form.enabled, sortOrder: Number(form.sortOrder), remark: form.remark };
  try {
    if (editingId.value) await updateCategory(editingId.value, payload);
    else await createCategory(payload);
    await load();
    notice.value = { type: 'success', text: '分类保存成功' };
  } catch (err) {
    notice.value = { type: 'error', text: errorMessage(err) };
  }
}

async function remove(id: number) {
  if (!confirm('确定要删除该分类吗？')) return;
  try {
    await deleteCategory(id);
    await load();
    notice.value = { type: 'success', text: '分类已删除' };
  } catch (err) {
    notice.value = { type: 'error', text: errorMessage(err) };
  }
}

onMounted(load);
</script>

<template>
  <section class="page">
    <header class="page__head">
      <div>
        <p class="eyebrow">基础数据</p>
        <h1>分类管理</h1>
        <p class="page__desc">维护员工报销时可选的用途分类。</p>
      </div>
      <button class="ghost-btn" data-test="new-category" type="button" @click="resetForm">新增分类</button>
    </header>

    <p v-if="notice" :class="['notice', notice.type]" :role="notice.type === 'error' ? 'alert' : 'status'">{{ notice.text }}</p>

    <form class="enterprise-card form-card" @submit.prevent="save">
      <div class="field">
        <label>分类名称</label>
        <input class="field-input" aria-label="分类名称" v-model="form.name" placeholder="如：差旅费" />
      </div>
      <div class="field field--narrow">
        <label>排序</label>
        <input class="field-input" aria-label="排序" v-model="form.sortOrder" type="number" placeholder="排序" />
      </div>
      <div class="field">
        <label>备注</label>
        <input class="field-input" aria-label="备注" v-model="form.remark" placeholder="备注（选填）" />
      </div>
      <label class="checkbox-label">
        <input aria-label="启用" type="checkbox" v-model="form.enabled" />
        <span>启用</span>
      </label>
      <button class="primary-btn" data-test="create-category" type="button" @click="save">{{ editingId ? '更新分类' : '保存分类' }}</button>
    </form>

    <div class="enterprise-card table-card">
      <table class="data-table">
        <thead><tr><th>名称</th><th class="col-state">启用</th><th class="col-sort">排序</th><th>备注</th><th class="col-actions">操作</th></tr></thead>
        <tbody>
          <tr v-for="category in categories" :key="category.id">
            <td>{{ category.name }}</td>
            <td><span :class="['tag', category.enabled ? 'tag--success' : 'tag--muted']">{{ category.enabled ? '启用' : '停用' }}</span></td>
            <td>{{ category.sortOrder }}</td>
            <td>{{ category.remark || '—' }}</td>
            <td class="row-actions">
              <button class="ghost-btn" @click="edit(category)">编辑</button>
              <button class="danger-btn" @click="remove(category.id)">删除</button>
            </td>
          </tr>
          <tr v-if="!categories.length"><td colspan="5" class="empty">暂无分类</td></tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.page { display: grid; gap: var(--space-5); }
.page__head { display: flex; align-items: flex-start; justify-content: space-between; gap: var(--space-4); }
.page__head h1 { margin: 0; font-size: 1.5rem; }
.page__desc { margin: 6px 0 0; color: var(--color-text-muted); font-size: 0.875rem; }
.eyebrow { margin: 0 0 4px; color: var(--color-text-subtle); font-size: 0.8rem; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; }
.notice { margin: 0; padding: var(--space-3) var(--space-4); border-radius: var(--radius-md); font-weight: 700; }
.notice.success { background: var(--color-success-soft); color: #166534; }
.notice.error { background: var(--color-danger-soft); color: #991b1b; }
.form-card { display: flex; flex-wrap: wrap; gap: var(--space-3); align-items: flex-end; padding: var(--space-4) var(--space-5); }
.field { display: grid; gap: 6px; flex: 1; min-width: 180px; }
.field--narrow { max-width: 120px; }
.field label { color: var(--color-text-muted); font-size: 0.8125rem; font-weight: 700; }
.checkbox-label { display: inline-flex; align-items: center; gap: 8px; min-height: 40px; color: var(--color-text); font-weight: 700; cursor: pointer; }
.table-card { padding: var(--space-2); overflow-x: auto; }
.col-state, .col-sort { width: 1%; white-space: nowrap; }
.col-actions { width: 1%; white-space: nowrap; }
.row-actions { display: flex; gap: var(--space-2); justify-content: flex-end; }
.empty { text-align: center; color: var(--color-text-subtle); padding: var(--space-10) !important; }
</style>
