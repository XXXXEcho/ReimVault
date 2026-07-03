// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import LoginView from '../src/views/LoginView.vue';
import http from '../src/api/http';
import { useAuthStore } from '../src/stores/auth';

vi.mock('../src/api/http', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn()
  }
}));

const push = vi.fn();
vi.mock('vue-router', () => ({
  useRouter: () => ({ push })
}));

describe('auth flow', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  it('posts login credentials and stores the current user', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        id: 1,
        username: 'admin',
        displayName: '管理员',
        department: '财务部',
        role: 'ADMIN'
      }
    });

    const wrapper = mount(LoginView, {
      global: {
        plugins: [createPinia()],
        stubs: ['el-card', 'el-form', 'el-form-item', 'el-input', 'el-button']
      }
    });

    expect(wrapper.find('[aria-label="用户名"]').exists()).toBe(true);
    expect(wrapper.find('[aria-label="密码"]').exists()).toBe(true);
    expect(wrapper.find('button[type="submit"]').classes()).toContain('primary-btn');

    await wrapper.find('[aria-label="用户名"]').setValue('admin');
    await wrapper.find('[aria-label="密码"]').setValue('secret');
    await wrapper.find('form').trigger('submit');
    await flushPromises();

    expect(http.post).toHaveBeenCalledWith('/auth/login', { username: 'admin', password: 'secret' });
    expect(useAuthStore().user?.username).toBe('admin');
    expect(push).toHaveBeenCalledWith('/admin/reimbursements');
  });
});
