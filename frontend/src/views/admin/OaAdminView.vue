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
  <section>
    <h1>经费编码</h1>
    <p v-if="notice" :class="['notice', notice.type]" :role="notice.type === 'error' ? 'alert' : 'status'">{{ notice.text }}</p>
    <form class="inline-form" @submit.prevent="add">
      <input aria-label="OA编号" v-model="newNumber" placeholder="输入新经费编码" />
      <button type="submit">新增</button>
    </form>
    <table>
      <thead><tr><th>ID</th><th>经费编码</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td>{{ item.id }}</td>
          <td>
            <template v-if="editingId === item.id">
              <input class="edit-input" v-model="editing[item.id]" @keyup.enter="saveEdit(item.id)" />
            </template>
            <template v-else>{{ item.number }}</template>
          </td>
          <td class="row-actions">
            <template v-if="editingId === item.id">
              <button @click="saveEdit(item.id)">保存</button>
              <button @click="editingId = null">取消</button>
            </template>
            <template v-else>
              <button @click="startEdit(item)">编辑</button>
              <button class="btn-danger" @click="remove(item.id)">删除</button>
            </template>
          </td>
        </tr>
        <tr v-if="!items.length"><td colspan="3" class="empty">暂无经费编码</td></tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.inline-form { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 18px; }
.notice { margin: 0 0 14px; padding: 10px 14px; border-radius: 10px; font-size: 13px; font-weight: 700; }
.notice.success { background: #dcfce7; color: #166534; }
.notice.error { background: #fee2e2; color: #991b1b; }
.edit-input { min-width: 160px; }
.row-actions { display: flex; gap: 6px; align-items: center; }
.btn-danger { min-height: 34px; padding: 0 12px; border: 1px solid #fca5a5; border-radius: 8px; background: #fff; color: #dc2626; font-size: 12px; font-weight: 700; cursor: pointer; transition: background 160ms ease, color 160ms ease; }
.btn-danger:hover { background: #dc2626; color: #fff; }
.empty { text-align: center; color: #94a3b8; padding: 32px 12px !important; }
</style>
