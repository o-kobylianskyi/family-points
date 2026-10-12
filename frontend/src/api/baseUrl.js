/**
 * Browser API origin.
 * A deployment may provide VITE_API_URL at build time. If omitted, production
 * uses the current HTTPS origin (Nginx proxies /auth, /api and /workspaces),
 * while Vite development preserves the existing remote/local backend routing.
 */
function normalizeBaseUrl(url) {
  return url.replace(/\/+$/, '')
}

const configured = import.meta.env.VITE_API_URL?.trim()

export const API_URL = configured
  ? normalizeBaseUrl(configured)
  : import.meta.env.DEV
    ? window.location.hostname === 'dev.family-point.com'
      ? 'https://api-dev.family-point.com:8443'
      : `http://${window.location.hostname}:8080`
    : window.location.origin
