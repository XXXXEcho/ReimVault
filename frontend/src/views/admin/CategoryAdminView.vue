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
  <section>
    <div class="page-header">
      <h1>分类管理</h1>
      <button data-test="new-category" type="button" @click="resetForm">新增分类</button>
    </div>
    <p v-if="notice" :class="['notice', notice.type]" :role="notice.type === 'error' ? 'alert' : 'status'">{{ notice.text }}</p>
    <form class="inline-form" @submit.prevent="save">
      <input aria-label="分类名称" v-model="form.name" placeholder="分类名称" />
      <input aria-label="排序" v-model="form.sortOrder" type="number" placeholder="排序" />
      <input aria-label="备注" v-model="form.remark" placeholder="备注" />
      <label class="checkbox-label"><input aria-label="启用" type="checkbox" v-model="form.enabled" />启用</label>
      <button data-test="create-category" type="button" @click="save">保存分类</button>
    </form>
    <table>
      <thead><tr><th>名称</th><th>启用</th><th>排序</th><th>备注</th><th>操作</th></tr></thead>
      <tbody><tr v-for="category in categories" :key="category.id"><td>{{ category.name }}</td><td>{{ category.enabled ? '是' : '否' }}</td><td>{{ category.sortOrder }}</td><td>{{ category.remark }}</td><td class="row-actions"><button @click="edit(category)">编辑</button><button class="btn-danger" @click="remove(category.id)">删除</button></td></tr></tbody>
    </table>
  </section>
</template>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 18px; }
.page-header h1 { margin: 0; }
.inline-form { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 18px; }
.checkbox-label { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; color: #475569; cursor: pointer; }
.notice { margin: 0 0 14px; padding: 10px 14px; border-radius: 10px; font-size: 13px; font-weight: 700; }
.notice.success { background: #dcfce7; color: #166534; }
.notice.error { background: #fee2e2; color: #991b1b; }
.row-actions { display: flex; gap: 6px; align-items: center; }
.btn-danger { min-height: 34px; padding: 0 12px; border: 1px solid #fca5a5; border-radius: 8px; background: #fff; color: #dc2626; font-size: 12px; font-weight: 700; cursor: pointer; transition: background 160ms ease, color 160ms ease; }
.btn-danger:hover { background: #dc2626; color: #fff; }
</style>
