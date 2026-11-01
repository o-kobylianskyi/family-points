import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { getTemplateCatalog } from '../api/templateApi'
import { getSetupState, reviewSetupCatalog, setSetupStep } from '../api/setupApi'
import { templateCategoryLabel } from '../utils/templateCategoryLabels'

// Read-only setup review. Do not enable finalize/activation until full saga UX is approved.
const words = {
  uk: ['Початкове налаштування сім’ї', 'Налаштування недоступне або вимкнене.', 'Тип', 'Категорія', 'Завдання', 'Нагороди', 'Усі категорії', 'Переглянути вибране', 'Вибрано', 'Буде імпортовано', 'Уже імпортовано', 'Це лише перегляд; імпорт та активація не виконуються.', 'Не вдалося перевірити вибір. Оновіть сторінку.'],
  ru: ['Первоначальная настройка семьи', 'Настройка недоступна или отключена.', 'Тип', 'Категория', 'Задачи', 'Награды', 'Все категории', 'Просмотреть выбранное', 'Выбрано', 'Будет импортировано', 'Уже импортировано', 'Это только просмотр, без импорта и активации.', 'Не удалось проверить выбор. Обновите страницу.'],
  de: ['Ersteinrichtung der Familie', 'Einrichtung nicht verfügbar oder deaktiviert.', 'Typ', 'Kategorie', 'Aufgaben', 'Belohnungen', 'Alle Kategorien', 'Auswahl prüfen', 'Ausgewählt', 'Zu importieren', 'Bereits importiert', 'Nur Vorschau, kein Import und keine Aktivierung.', 'Auswahl konnte nicht geprüft werden. Seite aktualisieren.'],
  en: ['Initial family setup', 'Setup is unavailable or disabled.', 'Type', 'Category', 'Tasks', 'Rewards', 'All categories', 'Review selection', 'Selected', 'To import', 'Already imported', 'Read-only preview: no import or activation.', 'Could not review selection. Refresh the page.'],
}
export default function InitialFamilySetupReview() {
  const { i18n } = useTranslation()
  const lang = i18n.language.split('-')[0]
  const w = words[lang] || words.en
  const { currentUser, getAccessToken } = useAuth()
  const workspaceId = currentUser?.workspaceId
  const [setup, setSetup] = useState(null)
  const [catalog, setCatalog] = useState([])
  const [packs, setPacks] = useState([])
  const [pack, setPack] = useState('ALL')
  const [unavailable, setUnavailable] = useState(false)
  const [kind, setKind] = useState('TASK')
  const [category, setCategory] = useState('ALL')
  const [selected, setSelected] = useState([])
  const [review, setReview] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(false)
  const seq = useRef(0)
  useEffect(() => {
    const id = ++seq.current
    setSetup(null); setCatalog([]); setPacks([]); setPack('ALL'); setSelected([]); setReview(null); setError(false); setUnavailable(false)
    if (!workspaceId) return
    Promise.all([getSetupState(getAccessToken(), workspaceId), getTemplateCatalog(getAccessToken())])
      .then(([state, data]) => { if (seq.current === id) { setSetup(state); setCatalog(data?.templates || []); setPacks(data?.packs || []) } })
      .catch(() => { if (seq.current === id) setUnavailable(true) })
    return () => { seq.current++ }
  }, [workspaceId, getAccessToken])
  const cat = item => item?.definition?.category || item?.suggested?.category || 'other'
  const typed = catalog.filter(item => item.kind === kind && (pack === 'ALL' || packs.find(p => p.code === pack)?.templateCodes?.includes(item.code)))
  const categories = [...new Set(typed.map(cat))].sort()
  const items = typed.filter(item => category === 'ALL' || cat(item) === category)
  const setStep = async (step, status) => {
    if (!setup || busy || setup.status === 'COMPLETED') return
    const id = ++seq.current
    const origin = workspaceId
    setBusy(true); setError(false); setReview(null)
    try {
      const result = await setSetupStep(getAccessToken(), origin, step, status, setup.revision)
      if (id === seq.current) setSetup(result)
    } catch { if (id === seq.current) setError(true) }
    finally { if (id === seq.current) setBusy(false) }
  }
  const preview = async () => {
    if (!setup || busy || !selected.length) return
    const id = ++seq.current
    const workspace = workspaceId
    const codes = catalog.filter(item => selected.includes(item.code)).map(item => item.code)
    setBusy(true); setReview(null); setError(false)
    try {
      const result = await reviewSetupCatalog(getAccessToken(), workspace, setup.revision, codes)
      if (seq.current === id) setReview(result)
    } catch { if (seq.current === id) setError(true) }
    finally { if (seq.current === id) setBusy(false) }
  }
  return <section className="settings-section" aria-label="Initial family setup">
    <h2>{w[0]}</h2>
    <p>{w[11]}</p>
    {unavailable && <p role="status">{w[1]}</p>}
    {error && <p role="alert">{w[12]}</p>}
    {setup && <>
      <p>{setup.status} · {Object.values(setup.steps || {}).filter(x => x === 'COMPLETE' || x === 'SKIPPED').length}/{Object.keys(setup.steps || {}).length}</p>
      <div className="template-toolbar">
        <label>{w[2]}<select value={kind} onChange={e=>{setKind(e.target.value);setCategory('ALL');setReview(null)}}><option value="TASK">{w[4]}</option><option value="REWARD">{w[5]}</option></select></label>
        <label>{({uk:'Набір',ru:'Набор',de:'Paket',en:'Pack'})[lang] || 'Pack'}<select value={pack} onChange={e=>{setPack(e.target.value);setCategory('ALL');setReview(null)}}><option value="ALL">{w[6]}</option>{packs.map(p=><option value={p.code} key={p.code}>{p.title?.[lang] || p.title?.en || p.code}</option>)}</select></label>
        <label>{w[3]}<select value={category} onChange={e=>{setCategory(e.target.value);setReview(null)}}><option value="ALL">{w[6]}</option>{categories.map(c=><option key={c} value={c}>{templateCategoryLabel(c, lang)}</option>)}</select></label>
      </div>
      <div style={{maxHeight:260,overflowY:'auto'}}>
        {items.map(item=><label key={item.code} style={{display:'flex',gap:8,padding:'4px 0'}}>
          <input type="checkbox" disabled={busy || setup.status === 'COMPLETED'} checked={selected.includes(item.code)} onChange={()=>{setSelected(v=>v.includes(item.code)?v.filter(c=>c!==item.code):[...v,item.code]);setReview(null)}}/>
          <span>{item.title?.[lang] || item.title?.en || item.code}</span>
        </label>)}
      </div>
      <p>{w[8]}: {selected.length}</p>
      <div>
        <strong>{({uk:'Кроки налаштування',ru:'Шаги настройки',de:'Einrichtungsschritte',en:'Setup steps'})[lang] || 'Setup steps'}</strong>
        {Object.entries(setup.steps || {}).map(([step, value]) => <div key={step} style={{display:'flex',alignItems:'center',gap:8,flexWrap:'wrap',padding:'5px 0'}}>
          <span>{step} — {value}</span>
          {setup.status !== 'COMPLETED' && <>
            <button className="secondary-button" type="button" disabled={busy} onClick={()=>setStep(step,'COMPLETE')}>{({uk:'Позначити виконаним',ru:'Отметить выполненным',de:'Als erledigt markieren',en:'Mark complete'})[lang]}</button>
            <button className="secondary-button" type="button" disabled={busy} onClick={()=>setStep(step,'SKIPPED')}>{({uk:'Пропустити',ru:'Пропустить',de:'Überspringen',en:'Skip'})[lang]}</button>
          </>}
        </div>)}
      </div>
      <button type="button" className="secondary-button" disabled={!selected.length || busy || setup.status === 'COMPLETED'} onClick={preview}>{w[7]}</button>
      {review && <p role="status">{w[9]}: {review.toImportCount} · {w[10]}: {review.alreadyImportedCount}</p>}
    </>}
  </section>
}
