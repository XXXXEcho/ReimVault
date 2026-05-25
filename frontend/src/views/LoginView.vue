<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useAuthStore();
const form = reactive({ username: '', password: '' });
const error = ref('');

async function submit() {
  error.value = '';
  try {
    const user = await auth.login(form.username, form.password);
    await router.push(user.role === 'ADMIN' ? '/admin/reimbursements' : '/reimbursements');
  } catch (err) {
    if (typeof err === 'object' && err && 'response' in err) {
      const response = (err as { response?: { data?: { message?: string } } }).response;
      if (response?.data?.message) {
        error.value = response.data.message;
        return;
      }
    }
    error.value = '用户名或密码错误';
  }
}
</script>

<template>
  <div class="login-card">
    <h1>登录</h1>
    <form @submit.prevent="submit">
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <label>用户名<input aria-label="用户名" v-model="form.username" autocomplete="username" /></label>
      <label>密码<input aria-label="密码" v-model="form.password" type="password" autocomplete="current-password" /></label>
      <button class="primary-action" type="submit">登录</button>
    </form>
  </div>
</template>

<style scoped>
.login-card { max-width: 380px; margin: 0 auto; }
form { display: grid; gap: 18px; }
label { display: grid; gap: 6px; font-size: 13px; font-weight: 700; color: #374151; }
.primary-action { min-height: 44px; border: 0; border-radius: 12px; background: #2563eb; color: #fff; cursor: pointer; font-size: 15px; font-weight: 700; transition: background 160ms ease; }
.primary-action:hover { background: #1d4ed8; }
.error { margin: 0; padding: 10px 14px; border-radius: 10px; background: #fee2e2; color: #991b1b; font-size: 13px; font-weight: 700; }
</style>
