<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { createUser, deleteUser, listUsers, updateUser, type UserRecord } from '../../api/users';
import type { Role } from '../../api/auth';

const users = ref<UserRecord[]>([]);
const editingId = ref<number | null>(null);
const notice = ref<{ type: 'success' | 'error'; text: string } | null>(null);
const form = reactive({ username: '', displayName: '', department: '', password: '', role: 'EMPLOYEE' as Role, enabled: true });

function resetForm() {
  editingId.value = null;
  Object.assign(form, {
    username: '',
    displayName: '',
    department: '',
    password: '',
    role: 'EMPLOYEE' as Role,
    enabled: true
  });
}

async function load() {
  const response = await listUsers();
  users.value = response.data;
}

function edit(user: UserRecord) {
  editingId.value = user.id;
  Object.assign(form, { ...user, password: '' });
}

function errorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '操作失败';
}

async function save() {
  notice.value = null;
  try {
    if (editingId.value) {
      const payload = { displayName: form.displayName, department: form.department, role: form.role, enabled: form.enabled, ...(form.password.trim() ? { password: form.password } : {}) };
      await updateUser(editingId.value, payload);
    } else {
      await createUser({ username: form.username, displayName: form.displayName, department: form.department, password: form.password, role: form.role });
    }
    await load();
    notice.value = { type: 'success', text: '用户保存成功' };
  } catch (err) {
    notice.value = { type: 'error', text: errorMessage(err) };
  }
}

async function remove(id: number) {
  if (!confirm('确定要删除该用户吗？')) return;
  try {
    await deleteUser(id);
    await load();
    notice.value = { type: 'success', text: '用户已删除' };
  } catch (err) {
    notice.value = { type: 'error', text: errorMessage(err) };
  }
}

onMounted(load);
</script>

<template>
  <section>
    <div class="page-header">
      <h1>用户管理</h1>
      <button data-test="new-user" type="button" @click="resetForm">新增用户</button>
    </div>
    <p v-if="notice" :class="['notice', notice.type]" :role="notice.type === 'error' ? 'alert' : 'status'">{{ notice.text }}</p>
    <form class="inline-form" @submit.prevent="save">
      <input aria-label="用户名" v-model="form.username" placeholder="用户名" />
      <input aria-label="姓名" v-model="form.displayName" placeholder="姓名" />
      <input aria-label="部门" v-model="form.department" placeholder="部门" />
      <input aria-label="密码" v-model="form.password" placeholder="密码" type="password" />
      <select aria-label="角色" v-model="form.role"><option value="EMPLOYEE">员工</option><option value="SPECIALIST">报销专员</option><option value="ADMIN">管理员</option></select>
      <label class="checkbox-label"><input aria-label="启用" type="checkbox" v-model="form.enabled" />启用</label>
      <button data-test="create-user" type="button" @click="save">保存用户</button>
    </form>
    <table>
      <thead><tr><th>用户名</th><th>姓名</th><th>部门</th><th>角色</th><th>启用</th><th>操作</th></tr></thead>
      <tbody><tr v-for="user in users" :key="user.id"><td>{{ user.username }}</td><td>{{ user.displayName }}</td><td>{{ user.department }}</td><td>{{ { EMPLOYEE: '员工', SPECIALIST: '报销专员', ADMIN: '管理员' }[user.role] ?? user.role }}</td><td>{{ user.enabled ? '是' : '否' }}</td><td class="row-actions"><button @click="edit(user)">编辑</button><button class="btn-danger" @click="remove(user.id)">删除</button></td></tr></tbody>
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
