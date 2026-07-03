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
  <div class="login-page">
    <div class="login-card enterprise-card">
      <div class="login-brand">
        <span class="login-brand__mark" aria-hidden="true">报</span>
        <div>
          <strong>报销归档通</strong>
          <small>ReimVault</small>
        </div>
      </div>
      <h1>欢迎登录</h1>
      <p class="login-sub">进入报销材料管理系统</p>
      <form @submit.prevent="submit">
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <label class="login-field">
          <span>用户名</span>
          <input class="field-input" aria-label="用户名" v-model="form.username" autocomplete="username" placeholder="请输入用户名" />
        </label>
        <label class="login-field">
          <span>密码</span>
          <input class="field-input" aria-label="密码" v-model="form.password" type="password" autocomplete="current-password" placeholder="请输入密码" />
        </label>
        <button class="primary-btn login-submit" type="submit">登录</button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: var(--space-6);
  background:
    radial-gradient(circle at 20% 10%, rgba(37, 99, 235, 0.14), transparent 36rem),
    radial-gradient(circle at 90% 90%, rgba(37, 99, 235, 0.08), transparent 30rem),
    var(--color-background);
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: var(--space-10) var(--space-8);
  border-radius: var(--radius-xl);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-6);
}

.login-brand__mark {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: #fff;
  font-weight: 800;
  font-size: 20px;
}

.login-brand strong {
  display: block;
  color: var(--color-text);
  font-size: 1.0625rem;
}

.login-brand small {
  color: var(--color-text-muted);
  font-size: 0.75rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.login-card h1 {
  margin: 0 0 var(--space-1);
  font-size: 1.5rem;
}

.login-sub {
  margin: 0 0 var(--space-6);
  color: var(--color-text-muted);
}

form {
  display: grid;
  gap: var(--space-4);
}

.login-field {
  display: grid;
  gap: 6px;
}

.login-field span {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-muted);
}

.login-submit {
  margin-top: var(--space-2);
  width: 100%;
  min-height: 46px;
  font-size: 15px;
}

.error {
  margin: 0;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--color-danger-soft);
  color: #991b1b;
  font-weight: 700;
}
</style>
