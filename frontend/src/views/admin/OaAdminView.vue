<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { listOaNumbers, createOaNumber, updateOaNumber, deleteOaNumber, type OaNumber } from '../../api/oa';

const items = ref<OaNumber[]>([]);
const newNumber = ref('');
const editing = reactive<Record<number, string>>({});
const editingId = ref<number | null>(null);
const notice = ref<{ type: 'success' | 'error'; text: string } | null>(null);

function errorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '操作失败';
}

async function load() {
  const response = await listOaNumbers();
  items.value = response.data;
}

async function add() {
  if (!newNumber.value.trim()) return;
  notice.value = null;
  try {
    await createOaNumber(newNumber.value.trim());
    newNumber.value = '';
    notice.value = { type: 'success', text: '新增成功' };
    await load();
  } catch (err) {
    notice.value = { type: 'error', text: errorMessage(err) };
  }
}

function startEdit(item: OaNumber) {
  editingId.value = item.id;
  editing[item.id] = item.number;
}

async function saveEdit(id: number) {
  notice.value = null;
  try {
    await updateOaNumber(id, editing[id]);
    editingId.value = null;
    notice.value = { type: 'success', text: '修改成功' };
    await load();
  } catch (err) {
    notice.value = { type: 'error', text: errorMessage(err) };
  }
}

async function remove(id: number) {
  if (!confirm('确定删除此经费编码？')) return;
  notice.value = null;
  try {
    await deleteOaNumber(id);
    notice.value = { type: 'success', text: '已删除' };
    await load();
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
        <h1>经费编码</h1>
        <p class="page__desc">维护可用于报销关联的 OA 经费编码。</p>
      </div>
    </header>

    <p v-if="notice" :class="['notice', notice.type]" :role="notice.type === 'error' ? 'alert' : 'status'">{{ notice.text }}</p>

    <form class="enterprise-card form-card" @submit.prevent="add">
      <input class="field-input" aria-label="OA编号" v-model="newNumber" placeholder="输入新经费编码，如 30280501" />
      <button type="submit" class="primary-btn">新增经费编码</button>
    </form>

    <div class="enterprise-card table-card">
      <table class="data-table">
        <thead><tr><th>经费编码</th><th class="col-actions">操作</th></tr></thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td>
              <template v-if="editingId === item.id">
                <input class="field-input edit-input" v-model="editing[item.id]" @keyup.enter="saveEdit(item.id)" />
              </template>
              <template v-else>{{ item.number }}</template>
            </td>
            <td class="row-actions">
              <template v-if="editingId === item.id">
                <button class="primary-btn" @click="saveEdit(item.id)">保存</button>
                <button class="ghost-btn" @click="editingId = null">取消</button>
              </template>
              <template v-else>
                <button class="ghost-btn" @click="startEdit(item)">编辑</button>
                <button class="danger-btn" @click="remove(item.id)">删除</button>
              </template>
            </td>
          </tr>
          <tr v-if="!items.length"><td colspan="2" class="empty">暂无经费编码</td></tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.page { display: grid; gap: var(--space-5); }
.page__head h1 { margin: 0; font-size: 1.5rem; }
.page__desc { margin: 6px 0 0; color: var(--color-text-muted); font-size: 0.875rem; }
.eyebrow { margin: 0 0 4px; color: var(--color-text-subtle); font-size: 0.8rem; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; }
.notice { margin: 0; padding: var(--space-3) var(--space-4); border-radius: var(--radius-md); font-weight: 700; }
.notice.success { background: var(--color-success-soft); color: #166534; }
.notice.error { background: var(--color-danger-soft); color: #991b1b; }
.form-card { display: flex; flex-wrap: wrap; gap: var(--space-3); align-items: center; padding: var(--space-4) var(--space-5); }
.form-card .field-input { flex: 1; min-width: 220px; }
.table-card { padding: var(--space-2); overflow-x: auto; }
.col-actions { width: 1%; white-space: nowrap; }
.row-actions { display: flex; gap: var(--space-2); justify-content: flex-end; }
.edit-input { min-width: 220px; }
.empty { text-align: center; color: var(--color-text-subtle); padding: var(--space-10) !important; }
</style>
