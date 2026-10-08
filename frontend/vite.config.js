
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],

  resolve: {
    dedupe: [
      'react',
      'react-dom',
    ],
  },

  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,

    // Localhost/IP addresses are allowed by Vite automatically.
    // Explicitly allow the DEV hostname used by the reverse proxy.
    allowedHosts: ['dev.family-point.com'],

    // HMR поки залишаємо локальним.
    // Для віддаленого DEV налаштуємо окремо.
  },
})
