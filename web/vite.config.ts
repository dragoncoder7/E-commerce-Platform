import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
export default defineConfig(({ mode }) => ({
  plugins: [vue(), tailwindcss()],
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id: string) {
          if (!id.includes('node_modules')) return
          if (id.includes('@element-plus/icons-vue')) return 'element-icons'
          if (id.includes('element-plus')) return 'element-plus'
          if (id.includes('vue-router') || id.includes('pinia') || id.includes('/vue/')) {
            return 'vue-vendor'
          }
          return 'vendor'
        },
      },
    },
  },
  server: { port: 5174, strictPort: true, proxy: { '/api': { target: loadEnv(mode, process.cwd(), '').VITE_PROXY_TARGET || 'http://localhost:8080', changeOrigin: true } } },
}))
