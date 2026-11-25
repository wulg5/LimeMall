import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

/**
 * Vite 开发配置：
 * - 别名 @ -> src
 * - host: true 同时监听 IPv4/IPv6，保证 localhost 与 127.0.0.1 均可访问
 * - 代理 /api 与 /uploads 到后端（18080），规避开发期跨域
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: true,
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:18080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
        // 同源代理：剥离 Origin 头，避免浏览器对 POST 携带的 Origin 被后端 CORS 误拦
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'))
        }
      },
      '/uploads': {
        target: 'http://127.0.0.1:18080',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'))
        }
      }
    }
  }
})
