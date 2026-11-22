import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { applyTheme } from '../utils/theme'

const KEY = 'familypoints.theme.preference'
const MODES = ['system', 'light', 'dark']

function savedMode() {
  try {
    const value = localStorage.getItem(KEY)
    return MODES.includes(value) ? value : 'system'
  } catch { return 'system' }
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
  const { i18n } = useTranslation()
  const language = (i18n.resolvedLanguage || i18n.language || 'en').split('-')[0]
  const labels = ({
    uk: { title:'Оформлення', hint:'Тема FamilyPoints зберігається в цьому браузері.', group:'Тема застосунку', system:'Як у системі', light:'Світла', dark:'Темна' },
    ru: { title:'Оформление', hint:'Тема FamilyPoints сохраняется в этом браузере.', group:'Тема приложения', system:'Как в системе', light:'Светлая', dark:'Тёмная' },
    de: { title:'Darstellung', hint:'Das FamilyPoints-Design wird in diesem Browser gespeichert.', group:'Anwendungsdesign', system:'Wie im System', light:'Hell', dark:'Dunkel' },
    en: { title:'Appearance', hint:'The FamilyPoints theme is saved in this browser.', group:'Application theme', system:'System', light:'Light', dark:'Dark' },
  })[language] || { title:'Appearance', hint:'The FamilyPoints theme is saved in this browser.', group:'Application theme', system:'System', light:'Light', dark:'Dark' }
  const [mode, setMode] = useState(savedMode)
  const choose = (next) => {
    setMode(next)
    try { localStorage.setItem(KEY, next) } catch { /* private storage */ }
    applyTheme(next)
  }
  return <section className="settings-section">
    <h2>{labels.title}</h2>
    <p>{labels.hint}</p>
    <div className="fp-theme-options" role="group" aria-label={labels.group}>
      {[
        ['system', labels.system],
        ['light', labels.light],
        ['dark', labels.dark],
      ].map(([value, label]) => <label key={value}>
        <input type="radio" name="fp-theme" value={value}
          checked={mode === value} onChange={() => choose(value)} />
        <span>{label}</span>
      </label>)}
    </div>
  </section>
}
