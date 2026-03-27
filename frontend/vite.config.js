import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const devServerPort = Number(env.VITE_DEV_SERVER_PORT || 5173)

  return {
    plugins: [vue()],
    server: {
      port: Number.isFinite(devServerPort) ? devServerPort : 5173,
      host: '0.0.0.0',
      proxy: {
        '/api': {
          target: env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8080',
          changeOrigin: true
        }
      }
    }
  }
})
