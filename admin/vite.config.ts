import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [vue(), tailwindcss()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      strictPort: true,
      // Docker 挂载卷在部分文件系统上收不到 inotify 事件，开启轮询保证热更新
      watch: { usePolling: true, interval: 300 },
      proxy: {
        // 关闭 mock 后，前端请求 /api 会被代理到后端网关
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
          ws: true,
        },
      },
    },
    preview: {
      host: '0.0.0.0',
      port: 4173,
      strictPort: true,
    },
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      sourcemap: false,
      chunkSizeWarningLimit: 1800,
      rollupOptions: {
        output: {
          entryFileNames: 'assets/js/[name]-[hash].js',
          chunkFileNames: 'assets/js/[name]-[hash].js',
          assetFileNames: 'assets/[ext]/[name]-[hash].[ext]',
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
  }
})
