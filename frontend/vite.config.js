import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  // 将缓存放在项目目录，避免 Windows 对 node_modules 缓存目录的权限/占用问题。
  cacheDir: '.vite-cache',
  resolve: {
    alias: {
      // @ 指向 src，便于深路径引用
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    open: false,
    proxy: {
      // 测试环境：/api/v1 → 独立 Mock API
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      // Agent 服务独立运行，前端统一通过 /agent 访问，避免把服务地址写进组件。
      '/agent': {
        target: process.env.VITE_AGENT_TARGET || 'http://127.0.0.1:8000',
        changeOrigin: true,
        // Python Agent 的公开接口统一使用 /api 前缀。
        // 浏览器请求 /agent/chat，代理后实际访问 http://127.0.0.1:8000/api/chat。
        rewrite: (path) => path.replace(/^\/agent/, '/api'),
      },
    },
  },
})
