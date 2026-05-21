import { defineStore } from 'pinia';
import { getCurrentUser, login, type CurrentUser } from '../api/auth';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as CurrentUser | null
  }),
  actions: {
    async login(username: string, password: string) {
      const response = await login({ username, password });
      this.user = response.data;
      return this.user;
    },
    async loadCurrentUser() {
      const response = await getCurrentUser();
      this.user = response.data;
      return this.user;
    },
    logout() {
      this.user = null;
    }
  }
});
