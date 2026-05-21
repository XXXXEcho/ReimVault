<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { createUser, listUsers, updateUser, type UserRecord } from '../../api/users';
import type { Role } from '../../api/auth';

const users = ref<UserRecord[]>([]);
const editingId = ref<number | null>(null);
const form = reactive({ username: '', displayName: '', department: '', password: '', role: 'EMPLOYEE' as Role, enabled: true });

async function load() {
  const response = await listUsers();
  users.value = response.data;
}

function edit(user: UserRecord) {
  editingId.value = user.id;
  Object.assign(form, { ...user, password: '' });
}

async function save() {
  if (editingId.value) {
    await updateUser(editingId.value, { displayName: form.displayName, department: form.department, role: form.role, enabled: form.enabled });
  } else {
    await createUser({ username: form.username, displayName: form.displayName, department: form.department, password: form.password, role: form.role });
  }
  await load();
}

onMounted(load);
</script>

<template>
  <section>
    <h1>用户管理</h1>
    <form class="admin-form" @submit.prevent="save">
      <input aria-label="用户名" v-model="form.username" placeholder="用户名" />
      <input aria-label="姓名" v-model="form.displayName" placeholder="姓名" />
      <input aria-label="部门" v-model="form.department" placeholder="部门" />
      <input aria-label="密码" v-model="form.password" placeholder="密码" type="password" />
      <select aria-label="角色" v-model="form.role"><option value="EMPLOYEE">员工</option><option value="ADMIN">管理员</option></select>
      <label><input aria-label="启用" type="checkbox" v-model="form.enabled" />启用</label>
      <button data-test="create-user" type="button" @click="save">保存用户</button>
    </form>
    <table>
      <thead><tr><th>用户名</th><th>姓名</th><th>部门</th><th>角色</th><th>启用</th><th>操作</th></tr></thead>
      <tbody><tr v-for="user in users" :key="user.id"><td>{{ user.username }}</td><td>{{ user.displayName }}</td><td>{{ user.department }}</td><td>{{ user.role }}</td><td>{{ user.enabled ? '是' : '否' }}</td><td><button @click="edit(user)">编辑</button></td></tr></tbody>
    </table>
  </section>
</template>

<style scoped>
.admin-form { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
table { width: 100%; border-collapse: collapse; }
th, td { border: 1px solid #ddd; padding: 8px; }
</style>
