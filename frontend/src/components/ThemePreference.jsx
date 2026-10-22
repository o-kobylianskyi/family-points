import { useEffect, useState } from 'react'

const KEY = 'familypoints.theme.preference'
const MODES = ['system', 'light', 'dark']

function savedMode() {
  try {
    const value = localStorage.getItem(KEY)
    return MODES.includes(value) ? value : 'system'
  } catch { return 'system' }
}

export function applyTheme(mode) {
  const systemDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches
  document.documentElement.dataset.theme = mode === 'system'
    ? (systemDark ? 'dark' : 'light')
    : mode
}

export function ThemeInitializer() {
  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const refresh = () => applyTheme(savedMode())
    refresh()
    media.addEventListener?.('change', refresh)
    return () => media.removeEventListener?.('change', refresh)
  }, [])
  return null
}

export default function ThemePreference() {
  const [mode, setMode] = useState(savedMode)
  const choose = (next) => {
    setMode(next)
    try { localStorage.setItem(KEY, next) } catch { /* private storage */ }
    applyTheme(next)
  }
  return <section className="settings-section">
    <h2>Оформлення / Appearance</h2>
    <p>Виберіть тему FamilyPoints. Налаштування зберігається в цьому браузері.</p>
    <div className="fp-theme-options" role="group" aria-label="Тема застосунку">
      {[
        ['system', 'Як у системі / System'],
        ['light', 'Світла / Light'],
        ['dark', 'Темна / Dark'],
      ].map(([value, label]) => <label key={value}>
        <input type="radio" name="fp-theme" value={value}
          checked={mode === value} onChange={() => choose(value)} />
        <span>{label}</span>
      </label>)}
    </div>
  </section>
}
