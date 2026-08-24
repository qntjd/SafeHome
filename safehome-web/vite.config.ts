import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    host: true, // 같은 Wi-Fi의 폰 등 다른 기기에서 접속할 수 있도록 네트워크에 노출
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})