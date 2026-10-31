import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'

import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

const version = JSON.parse(readFileSync(new URL('./package.json', import.meta.url), 'utf8')).version
const gitSha = (() => {
  try { return execFileSync('git', ['rev-parse', '--short=12', 'HEAD'], { cwd: new URL('..', import.meta.url), encoding: 'utf8' }).trim() }
  catch { return 'unknown' }
})()
const buildInfo = {
  version,
  commit: process.env.VITE_APP_COMMIT || process.env.GITHUB_SHA?.slice(0, 12) || gitSha,
  builtAt: new Date().toISOString(),
  channel: process.env.VITE_APP_CHANNEL || (process.env.NODE_ENV === 'production' ? 'BUILD' : 'DEV'),
}

export default defineConfig({
  plugins: [react()],
  define: { __FP_BUILD_INFO__: JSON.stringify(buildInfo) },

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
