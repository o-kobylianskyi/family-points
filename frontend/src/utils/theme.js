// Shared theme DOM utility is not a React component; keep it outside
// component modules so Fast Refresh can track component boundaries correctly.
export function applyTheme(mode) {
  const systemDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches
  document.documentElement.dataset.theme = mode === 'system'
    ? (systemDark ? 'dark' : 'light')
    : mode
}
