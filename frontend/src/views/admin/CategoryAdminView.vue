<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { createCategory, listAdminCategories, updateCategory, type Category } from '../../api/categories';

const categories = ref<Category[]>([]);
const editingId = ref<number | null>(null);
const form = reactive({ name: '', enabled: true, sortOrder: 0, remark: '' });

async function load() {
  const response = await listAdminCategories();
  categories.value = response.data;
}

function edit(category: Category) {
  editingId.value = category.id;
  Object.assign(form, category);
}

async function save() {
  const payload = { name: form.name, enabled: form.enabled, sortOrder: Number(form.sortOrder), remark: form.remark };
  if (editingId.value) await updateCategory(editingId.value, payload);
  else await createCategory(payload);
  await load();
}

onMounted(load);
</script>

<template>
  <section>
    <h1>分类管理</h1>
    <form class="admin-form" @submit.prevent="save">
      <input aria-label="分类名称" v-model="form.name" placeholder="分类名称" />
      <input aria-label="排序" v-model="form.sortOrder" type="number" placeholder="排序" />
      <input aria-label="备注" v-model="form.remark" placeholder="备注" />
      <label><input aria-label="启用" type="checkbox" v-model="form.enabled" />启用</label>
      <button data-test="create-category" type="button" @click="save">保存分类</button>
    </form>
    <table>
      <thead><tr><th>名称</th><th>启用</th><th>排序</th><th>备注</th><th>操作</th></tr></thead>
      <tbody><tr v-for="category in categories" :key="category.id"><td>{{ category.name }}</td><td>{{ category.enabled ? '是' : '否' }}</td><td>{{ category.sortOrder }}</td><td>{{ category.remark }}</td><td><button @click="edit(category)">编辑</button></td></tr></tbody>
    </table>
  </section>
</template>

<style scoped>
.admin-form { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
table { width: 100%; border-collapse: collapse; }
th, td { border: 1px solid #ddd; padding: 8px; }
</style>
