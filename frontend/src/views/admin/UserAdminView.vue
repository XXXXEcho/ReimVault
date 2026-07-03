<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { createUser, deleteUser, listUsers, updateUser, type UserRecord } from '../../api/users';
import type { Role } from '../../api/auth';

const users = ref<UserRecord[]>([]);
const editingId = ref<number | null>(null);
const notice = ref<{ type: 'success' | 'error'; text: string } | null>(null);
const form = reactive({ username: '', displayName: '', department: '', password: '', role: 'EMPLOYEE' as Role, enabled: true });

const ROLE_LABEL: Record<Role, string> = { EMPLOYEE: '员工', SPECIALIST: '报销专员', ADMIN: '管理员' };
const ROLE_TAG: Record<Role, string> = { ADMIN: 'tag--info', SPECIALIST: 'tag--warning', EMPLOYEE: 'tag--muted' };

function roleLabel(role: Role) {
  return ROLE_LABEL[role] ?? role;
}

function roleTagClass(role: Role) {
  return ROLE_TAG[role] ?? 'tag--muted';
}

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
  <section class="page">
    <header class="page__head">
      <div>
        <p class="eyebrow">系统管理</p>
        <h1>用户管理</h1>
        <p class="page__desc">维护员工、报销专员和管理员账号。</p>
      </div>
      <button class="ghost-btn" data-test="new-user" type="button" @click="resetForm">新增用户</button>
    </header>

    <p v-if="notice" :class="['notice', notice.type]" :role="notice.type === 'error' ? 'alert' : 'status'">{{ notice.text }}</p>

    <form class="enterprise-card form-card" @submit.prevent="save">
      <div class="field">
        <label>用户名</label>
        <input class="field-input" aria-label="用户名" v-model="form.username" placeholder="登录用户名" />
      </div>
      <div class="field">
        <label>姓名</label>
        <input class="field-input" aria-label="姓名" v-model="form.displayName" placeholder="显示姓名" />
      </div>
      <div class="field">
        <label>部门</label>
        <input class="field-input" aria-label="部门" v-model="form.department" placeholder="所在部门" />
      </div>
      <div class="field">
        <label>{{ editingId ? '新密码（留空不改）' : '密码' }}</label>
        <input class="field-input" aria-label="密码" v-model="form.password" type="password" placeholder="登录密码" />
      </div>
      <div class="field field--narrow">
        <label>角色</label>
        <select class="field-input" aria-label="角色" v-model="form.role">
          <option value="EMPLOYEE">员工</option>
          <option value="SPECIALIST">报销专员</option>
          <option value="ADMIN">管理员</option>
        </select>
      </div>
      <label class="checkbox-label">
        <input aria-label="启用" type="checkbox" v-model="form.enabled" />
        <span>启用</span>
      </label>
      <button class="primary-btn" data-test="create-user" type="button" @click="save">{{ editingId ? '更新用户' : '保存用户' }}</button>
    </form>

    <div class="enterprise-card table-card">
      <table class="data-table">
        <thead><tr><th>用户名</th><th>姓名</th><th>部门</th><th>角色</th><th class="col-state">启用</th><th class="col-actions">操作</th></tr></thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.username }}</td>
            <td>{{ user.displayName }}</td>
            <td>{{ user.department }}</td>
            <td><span :class="['tag', roleTagClass(user.role)]">{{ roleLabel(user.role) }}</span></td>
            <td><span :class="['tag', user.enabled ? 'tag--success' : 'tag--muted']">{{ user.enabled ? '启用' : '停用' }}</span></td>
            <td class="row-actions">
              <button class="ghost-btn" @click="edit(user)">编辑</button>
              <button class="danger-btn" @click="remove(user.id)">删除</button>
            </td>
          </tr>
          <tr v-if="!users.length"><td colspan="6" class="empty">暂无用户</td></tr>
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
.field { display: grid; gap: 6px; flex: 1; min-width: 160px; }
.field--narrow { max-width: 140px; }
.field label { color: var(--color-text-muted); font-size: 0.8125rem; font-weight: 700; }
.checkbox-label { display: inline-flex; align-items: center; gap: 8px; min-height: 40px; color: var(--color-text); font-weight: 700; cursor: pointer; }
.table-card { padding: var(--space-2); overflow-x: auto; }
.col-state, .col-actions { width: 1%; white-space: nowrap; }
.row-actions { display: flex; gap: var(--space-2); justify-content: flex-end; }
.empty { text-align: center; color: var(--color-text-subtle); padding: var(--space-10) !important; }
</style>
