import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': process.env.VITE_API_TARGET ?? 'http://127.0.0.1:8080'
    }
  },
  test: {
    environment: 'jsdom',
    globals: true
  }
});
