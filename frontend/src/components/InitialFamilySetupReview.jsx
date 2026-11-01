import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { getTemplateCatalog } from '../api/templateApi'
import { getSetupState, reviewSetupCatalog, setSetupStep, finalizeSetupImport, getSetupFinalizeReceipt } from '../api/setupApi'
import { templateCategoryLabel } from '../utils/templateCategoryLabels'

// Read-only setup review. Do not enable finalize/activation until full saga UX is approved.
const words = {
  uk: ['Початкове налаштування сім’ї', 'Налаштування недоступне або вимкнене.', 'Тип', 'Категорія', 'Завдання', 'Нагороди', 'Усі категорії', 'Переглянути вибране', 'Вибрано', 'Буде імпортовано', 'Уже імпортовано', 'Це лише перегляд; імпорт та активація не виконуються.', 'Не вдалося перевірити вибір. Оновіть сторінку.'],
  ru: ['Первоначальная настройка семьи', 'Настройка недоступна или отключена.', 'Тип', 'Категория', 'Задачи', 'Награды', 'Все категории', 'Просмотреть выбранное', 'Выбрано', 'Будет импортировано', 'Уже импортировано', 'Это только просмотр, без импорта и активации.', 'Не удалось проверить выбор. Обновите страницу.'],
  de: ['Ersteinrichtung der Familie', 'Einrichtung nicht verfügbar oder deaktiviert.', 'Typ', 'Kategorie', 'Aufgaben', 'Belohnungen', 'Alle Kategorien', 'Auswahl prüfen', 'Ausgewählt', 'Zu importieren', 'Bereits importiert', 'Nur Vorschau, kein Import und keine Aktivierung.', 'Auswahl konnte nicht geprüft werden. Seite aktualisieren.'],
  en: ['Initial family setup', 'Setup is unavailable or disabled.', 'Type', 'Category', 'Tasks', 'Rewards', 'All categories', 'Review selection', 'Selected', 'To import', 'Already imported', 'Read-only preview: no import or activation.', 'Could not review selection. Refresh the page.'],
}
const setupSteps = {
  uk: { MEMBERS:'Учасники',MEMBER_GROUPS:'Групи учасників',WORK_NODE_GROUPS:'Папки завдань',CATALOG_SELECTION:'Вибір шаблонів',TASK_REVIEW:'Перевірка завдань',REWARD_REVIEW:'Перевірка нагород',FINAL_REVIEW:'Підсумкова перевірка' },
  ru: { MEMBERS:'Участники',MEMBER_GROUPS:'Группы участников',WORK_NODE_GROUPS:'Папки задач',CATALOG_SELECTION:'Выбор шаблонов',TASK_REVIEW:'Проверка задач',REWARD_REVIEW:'Проверка наград',FINAL_REVIEW:'Итоговая проверка' },
  de: { MEMBERS:'Mitglieder',MEMBER_GROUPS:'Mitgliedergruppen',WORK_NODE_GROUPS:'Aufgabenordner',CATALOG_SELECTION:'Vorlagenauswahl',TASK_REVIEW:'Aufgaben prüfen',REWARD_REVIEW:'Belohnungen prüfen',FINAL_REVIEW:'Abschlussprüfung' },
  en: { MEMBERS:'Members',MEMBER_GROUPS:'Member groups',WORK_NODE_GROUPS:'Task folders',CATALOG_SELECTION:'Template selection',TASK_REVIEW:'Task review',REWARD_REVIEW:'Reward review',FINAL_REVIEW:'Final review' }
}
const setupStates = {
  uk: { COMPLETE:'Виконано',COMPLETED:'Завершено',SKIPPED:'Пропущено',PENDING:'Очікує',IN_PROGRESS:'У процесі' },
  ru: { COMPLETE:'Выполнено',COMPLETED:'Завершено',SKIPPED:'Пропущено',PENDING:'Ожидает',IN_PROGRESS:'В процессе' },
  de: { COMPLETE:'Erledigt',COMPLETED:'Abgeschlossen',SKIPPED:'Übersprungen',PENDING:'Ausstehend',IN_PROGRESS:'In Bearbeitung' },
  en: { COMPLETE:'Complete',COMPLETED:'Completed',SKIPPED:'Skipped',PENDING:'Pending',IN_PROGRESS:'In progress' }
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
  const [errorStatus, setErrorStatus] = useState(null)
  const [pending, setPending] = useState(null)
  const [confirmed, setConfirmed] = useState(false)
  const [finished, setFinished] = useState(null)
  const seq = useRef(0)
  const activeScope = useRef(null)
  const accountId = currentUser?.accountId ?? currentUser?.userId ?? currentUser?.id ?? currentUser?.workspaceMemberId ?? currentUser?.username
  const storageKey = accountId && workspaceId ? `fp:setup:pending:${accountId}:${workspaceId}` : null
  activeScope.current = storageKey
  useEffect(() => {
    setPending(null); setConfirmed(false); setFinished(null)
    if (!storageKey) return
    try {
      const value = JSON.parse(sessionStorage.getItem(storageKey) || 'null')
      if (value && String(value.workspaceId) === String(workspaceId) && String(value.accountId) === String(accountId) &&
          Number.isSafeInteger(value.epoch) && Number.isSafeInteger(value.revision) &&
          typeof value.operationId === 'string' && Array.isArray(value.codes)) setPending(value)
      const shared = readShared()
      if (shared && String(shared.workspaceId) === String(workspaceId) && String(shared.accountId) === String(accountId)) setPending(shared)
      else if (shared) setPending({ blocked: true })
    } catch { setPending({ blocked: true }) }
  }, [storageKey])
  // Shared pending receipt marker coordinates tabs. Backend revision/epoch remain authoritative.
  const sharedKey = storageKey ? storageKey + ':shared' : null
  const readShared = () => {
    try { return sharedKey ? JSON.parse(localStorage.getItem(sharedKey) || 'null') : null }
    catch { return { blocked: true } } // fail closed on malformed shared state
  }
  const persist = value => {
    if (sessionStorage.getItem(storageKey) || readShared()) throw new Error('PENDING_SETUP_ALREADY_EXISTS')
    localStorage.setItem(sharedKey, JSON.stringify(value))
    try { sessionStorage.setItem(storageKey, JSON.stringify(value)) }
    catch (error) { localStorage.removeItem(sharedKey); throw error }
    setPending(value)
  }
  const clear = operation => {
    // Clear only the exact completed operation. Never erase another tab's marker.
    const shared = readShared()
    if (shared?.operationId === operation.operationId) localStorage.removeItem(sharedKey)
    try {
      const saved = JSON.parse(sessionStorage.getItem(storageKey) || 'null')
      if (saved?.operationId === operation.operationId) sessionStorage.removeItem(storageKey)
    } catch { /* Malformed data remains blocked, never silently replaced. */ }
    setPending(null)
  }
  useEffect(() => {
    if (!sharedKey) return
    const sync = event => {
      if (event.key !== sharedKey) return
      setConfirmed(false)
      if (event.newValue) {
        try { setPending(JSON.parse(event.newValue)) } catch { setPending({ blocked: true }) }
      } else setPending(null)
    }
    window.addEventListener('storage', sync)
    return () => window.removeEventListener('storage', sync)
  }, [sharedKey])
  useEffect(() => {
    const id = ++seq.current
    setSetup(null); setCatalog([]); setPacks([]); setPack('ALL'); setSelected([]); setReview(null); setConfirmed(false); setError(false); setUnavailable(false); setBusy(false); setFinished(null)
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
    if (!setup || busy || pending || readShared() || setup.status === 'COMPLETED') return
    const id = ++seq.current
    const origin = workspaceId
    setBusy(true); setError(false); setReview(null); setConfirmed(false)
    try {
      const result = await setSetupStep(getAccessToken(), origin, step, status, setup.revision)
      if (id === seq.current) setSetup(result)
    } catch (e) { if (id === seq.current) { setError(true); setErrorStatus(e?.status) } }
    finally { if (id === seq.current) setBusy(false) }
  }
  const preview = async () => {
    if (!setup || busy || pending || readShared() || !selected.length) return
    const id = ++seq.current
    const workspace = workspaceId
    const codes = catalog.filter(item => selected.includes(item.code)).map(item => item.code)
    setBusy(true); setReview(null); setConfirmed(false); setError(false)
    try {
      const result = await reviewSetupCatalog(getAccessToken(), workspace, setup.revision, codes)
      if (seq.current === id) setReview(result)
    } catch { if (seq.current === id) setError(true) }
    finally { if (seq.current === id) setBusy(false) }
  }
  const recover = async () => {
    if (!pending?.operationId || !Number.isSafeInteger(pending.epoch) || busy) return
    const scope = storageKey
    const operation = pending
    setBusy(true); setError(false)
    try {
      const result = await getSetupFinalizeReceipt(getAccessToken(), operation.workspaceId, operation.operationId)
      if (activeScope.current !== scope) return
      if (result.dataEpoch !== operation.epoch) throw new Error('EPOCH_MISMATCH')
      const refreshed = await getSetupState(getAccessToken(), operation.workspaceId)
      if (activeScope.current !== scope || refreshed.dataEpoch !== operation.epoch) return
      clear(operation); setFinished(result); setSetup(refreshed)
    } catch (e) { if (activeScope.current === scope) { setError(true); setErrorStatus(e?.status) } }
    finally { if (activeScope.current === scope) setBusy(false) }
  }
  const finalize = async () => {
    if (!confirmed || pending || busy || !storageKey || !setup || !review ||
        sessionStorage.getItem(storageKey) || readShared() ||
        review.revision !== setup.revision || review.dataEpoch !== setup.dataEpoch ||
        setup.status === 'COMPLETED' || !window.crypto?.randomUUID ||
        !Object.values(setup.steps || {}).every(v => v === 'COMPLETE' || v === 'SKIPPED')) return
    const codes = catalog.filter(item => selected.includes(item.code)).map(item => item.code)
    if (JSON.stringify(codes) !== JSON.stringify(review.selectedCodes)) return
    const scope = storageKey
    const operation = { accountId, workspaceId, epoch: setup.dataEpoch, revision: setup.revision,
      codes, operationId: window.crypto.randomUUID() }
    try { persist(operation) } catch { setError(true); return }
    setBusy(true); setError(false)
    try {
      const result = await finalizeSetupImport(getAccessToken(), operation.workspaceId, operation.operationId, operation.revision, operation.codes)
      if (activeScope.current !== scope) return
      if (result.dataEpoch !== operation.epoch) throw new Error('EPOCH_MISMATCH')
      const refreshed = await getSetupState(getAccessToken(), operation.workspaceId)
      if (activeScope.current !== scope || refreshed.dataEpoch !== operation.epoch) return
      clear(operation); setFinished(result); setSetup(refreshed)
    } catch { if (activeScope.current === scope) setError(true) }
    finally { if (activeScope.current === scope) setBusy(false) }
  }
  return <section className="settings-section" aria-label="Initial family setup">
    <h2>{w[0]}</h2>
    <p>{w[11]}</p>
    {unavailable && <p role="status">{w[1]}</p>}
    {error && <p role="alert">{errorStatus === 409 ? ({uk:'Дані налаштування змінилися. Оновіть сторінку та перевірте стан операції.',ru:'Данные изменились. Обновите страницу и проверьте операцию.',de:'Einrichtungsdaten geändert. Seite aktualisieren und Vorgang prüfen.',en:'Setup changed. Refresh and check operation status.'})[lang] : errorStatus === 403 ? ({uk:'Недостатньо прав для налаштування.',ru:'Недостаточно прав для настройки.',de:'Keine Berechtigung für die Einrichtung.',en:'Not authorized to manage setup.'})[lang] : w[12]}</p>}
    {setup && <>
      <p>{(setupStates[lang] || setupStates.en)[setup.status] || setup.status} · {Object.values(setup.steps || {}).filter(x => x === 'COMPLETE' || x === 'SKIPPED').length}/{Object.keys(setup.steps || {}).length}</p>
      <div className="template-toolbar">
        <label>{w[2]}<select value={kind} onChange={e=>{setKind(e.target.value);setCategory('ALL');setReview(null);setConfirmed(false);seq.current++}}><option value="TASK">{w[4]}</option><option value="REWARD">{w[5]}</option></select></label>
        <label>{({uk:'Набір',ru:'Набор',de:'Paket',en:'Pack'})[lang] || 'Pack'}<select value={pack} onChange={e=>{setPack(e.target.value);setCategory('ALL');setReview(null);setConfirmed(false);seq.current++}}><option value="ALL">{w[6]}</option>{packs.map(p=><option value={p.code} key={p.code}>{p.title?.[lang] || p.title?.en || p.code}</option>)}</select></label>
        <label>{w[3]}<select value={category} onChange={e=>{setCategory(e.target.value);setReview(null);setConfirmed(false);seq.current++}}><option value="ALL">{w[6]}</option>{categories.map(c=><option key={c} value={c}>{templateCategoryLabel(c, lang)}</option>)}</select></label>
      </div>
      <div style={{maxHeight:260,overflowY:'auto'}}>
        {items.map(item=><label key={item.code} style={{display:'flex',gap:8,padding:'4px 0'}}>
          <input type="checkbox" disabled={busy || setup.status === 'COMPLETED'} checked={selected.includes(item.code)} onChange={()=>{setSelected(v=>v.includes(item.code)?v.filter(c=>c!==item.code):[...v,item.code]);setReview(null);setConfirmed(false);seq.current++}}/>
          <span>{item.title?.[lang] || item.title?.en || item.code}</span>
        </label>)}
      </div>
      <p>{w[8]}: {selected.length}</p>
      <div>
        <strong>{({uk:'Кроки налаштування',ru:'Шаги настройки',de:'Einrichtungsschritte',en:'Setup steps'})[lang] || 'Setup steps'}</strong>
        {Object.entries(setup.steps || {}).map(([step, value]) => <div key={step} style={{display:'flex',alignItems:'center',gap:8,flexWrap:'wrap',padding:'5px 0'}}>
          <span>{(setupSteps[lang] || setupSteps.en)[step] || step} — {(setupStates[lang] || setupStates.en)[value] || value}</span>
          {setup.status !== 'COMPLETED' && <>
            <button className="secondary-button" type="button" disabled={busy || !!pending} onClick={()=>setStep(step,'COMPLETE')}>{({uk:'Позначити виконаним',ru:'Отметить выполненным',de:'Als erledigt markieren',en:'Mark complete'})[lang]}</button>
            <button className="secondary-button" type="button" disabled={busy || !!pending} onClick={()=>setStep(step,'SKIPPED')}>{({uk:'Пропустити',ru:'Пропустить',de:'Überspringen',en:'Skip'})[lang]}</button>
          </>}
        </div>)}
      </div>
      <button type="button" className="secondary-button" disabled={!selected.length || busy || !!pending || setup.status === 'COMPLETED'} onClick={preview}>{w[7]}</button>
      {review && <p role="status">{w[9]}: {review.toImportCount} · {w[10]}: {review.alreadyImportedCount}</p>}
      {pending && <div role="status"><p>{({uk:'Результат попередньої операції невідомий. Новий імпорт заблоковано.',ru:'Результат предыдущей операции неизвестен. Новый импорт заблокирован.',de:'Vorheriger Vorgang unklar. Neuer Import gesperrt.',en:'Previous result unknown. New import blocked.'})[lang]}</p><button type="button" className="secondary-button" disabled={busy || !pending?.operationId} onClick={recover}>{({uk:'Перевірити квитанцію',ru:'Проверить квитанцию',de:'Beleg prüfen',en:'Check receipt'})[lang]}</button></div>}
      {review && !pending && setup.status !== 'COMPLETED' && <div>
        <label><input type="checkbox" checked={confirmed} disabled={busy} onChange={e=>setConfirmed(e.target.checked)} />{({uk:'Підтверджую імпорт приватних шаблонів і завершення налаштування без активації.',ru:'Подтверждаю импорт и завершение без активации.',de:'Import und Abschluss ohne Aktivierung bestätigen.',en:'Confirm private template import and setup completion without activation.'})[lang]}</label>
        <button type="button" className="secondary-button" disabled={busy || !confirmed || !storageKey || !Object.values(setup.steps || {}).every(v=>v==='COMPLETE'||v==='SKIPPED')} onClick={finalize}>{({uk:'Завершити й імпортувати',ru:'Завершить и импортировать',de:'Abschließen und importieren',en:'Finalize and import'})[lang]}</button>
      </div>}
      {finished && <p role="status">{({uk:'Імпорт завершено',ru:'Импорт завершён',de:'Import abgeschlossen',en:'Import completed'})[lang]}</p>}
    </>}
  </section>
}
