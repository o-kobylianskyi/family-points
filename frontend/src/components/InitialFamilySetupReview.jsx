import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/authContextValue'
import { getTemplateCatalog } from '../api/templateApi'
import { getSetupState, reviewSetupCatalog, setSetupStep, finalizeSetupImport, getSetupFinalizeReceipt } from '../api/setupApi'
import { templateCategoryLabel } from '../utils/templateCategoryLabels'
import { validateSetupCatalog, selectSetupMatching } from '../utils/setupCatalogSelection'
import { validSetupFinalizeOperation, assertSetupFinalizeReceipt, reconcileSetupPending, assertSetupState, assertSetupReview, SETUP_STEP_CODES, reconcileSetupStepAfterFailure, assertSetupStepTransition, canChangeSetupStep, sameSetupFinalizeOperation, assertSetupCompletionAfterReceipt } from '../utils/setupFinalizeSafety'

// Seven-step V49 reviewed private-template import; no automatic activation or ledger writes.
const words = {
  uk: ['Початкове налаштування сім’ї', 'Налаштування недоступне або вимкнене.', 'Тип', 'Категорія', 'Завдання', 'Нагороди', 'Усі категорії', 'Переглянути вибране', 'Вибрано', 'Буде імпортовано', 'Уже імпортовано', 'Перегляд не змінює дані. Лише після окремого підтвердження імпортуються приватні шаблони та завершується налаштування; завдання й нагороди не активуються.', 'Не вдалося перевірити вибір. Оновіть сторінку.'],
  ru: ['Первоначальная настройка семьи', 'Настройка недоступна или отключена.', 'Тип', 'Категория', 'Задачи', 'Награды', 'Все категории', 'Просмотреть выбранное', 'Выбрано', 'Будет импортировано', 'Уже импортировано', 'Просмотр не изменяет данные. Только после отдельного подтверждения импортируются частные шаблоны и завершается настройка; задачи и награды не активируются.', 'Не удалось проверить выбор. Обновите страницу.'],
  de: ['Ersteinrichtung der Familie', 'Einrichtung nicht verfügbar oder deaktiviert.', 'Typ', 'Kategorie', 'Aufgaben', 'Belohnungen', 'Alle Kategorien', 'Auswahl prüfen', 'Ausgewählt', 'Zu importieren', 'Bereits importiert', 'Die Vorschau ändert nichts. Erst nach ausdrücklicher Bestätigung werden private Vorlagen importiert und die Einrichtung abgeschlossen; Aufgaben und Belohnungen bleiben inaktiv.', 'Auswahl konnte nicht geprüft werden. Seite aktualisieren.'],
  en: ['Initial family setup', 'Setup is unavailable or disabled.', 'Type', 'Category', 'Tasks', 'Rewards', 'All categories', 'Review selection', 'Selected', 'To import', 'Already imported', 'Preview does not change data. Explicit confirmation imports private templates and completes setup without activating tasks or rewards.', 'Could not review selection. Refresh the page.'],
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
  const { currentUser } = useAuth()
  const owner = currentUser?.accountId ?? currentUser?.userId ?? currentUser?.id ?? currentUser?.username
  return <InitialFamilySetupReviewContent key={`${owner}:${currentUser?.workspaceId}:${currentUser?.memberId}`} />
}

function InitialFamilySetupReviewContent() {
  const { i18n } = useTranslation()
  const lang = i18n.language.split('-')[0]
  const w = words[lang] || words.en
  const { currentUser, getAccessToken } = useAuth()
  const workspaceId = currentUser?.workspaceId
  const canManageSetup = currentUser?.permissions?.includes('MANAGE_WORKSPACE') || currentUser?.permissions?.includes('ADMIN_OVERRIDE')
  const [setup, setSetup] = useState(null)
  const [catalog, setCatalog] = useState([])
  const [packs, setPacks] = useState([])
  const [pack, setPack] = useState('ALL')
  const [unavailable, setUnavailable] = useState(false)
  const [loadingSetup, setLoadingSetup] = useState(false)
  const [loadAttempt, setLoadAttempt] = useState(0)
  const [kind, setKind] = useState('TASK')
  const [category, setCategory] = useState('ALL')
  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState([])
  const [review, setReview] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(false)
  const [errorStatus, setErrorStatus] = useState(null)
  const [confirmed, setConfirmed] = useState(false)
  const [finished, setFinished] = useState(null)
  const seq = useRef(0)
  const activeScope = useRef(null)
  const accountId = currentUser?.accountId ?? currentUser?.userId ?? currentUser?.id ?? currentUser?.workspaceMemberId ?? currentUser?.username
  const storageKey = accountId && workspaceId ? `fp:setup:pending:${accountId}:${workspaceId}` : null
  const sharedKey = storageKey ? storageKey + ':shared' : null
  const readShared = () => {
    try { return sharedKey ? JSON.parse(localStorage.getItem(sharedKey) || 'null') : null }
    catch { return { blocked: true } }
  }
  const [pending, setPending] = useState(() => {
    if (!storageKey) return null
    try {
      const session = JSON.parse(sessionStorage.getItem(storageKey) || 'null')
      const shared = readShared()
      return reconcileSetupPending(session, shared, workspaceId, accountId)
    } catch { return { blocked: true } }
  })
  useEffect(() => { activeScope.current = storageKey }, [storageKey])
  // Shared marker coordinates tabs; backend revision/epoch remain authoritative.
  const persist = value => {
    if (!validSetupFinalizeOperation(value, workspaceId, accountId)) throw new Error('INVALID_SETUP_FINALIZE_OPERATION')
    if (sessionStorage.getItem(storageKey) || readShared()) throw new Error('PENDING_SETUP_ALREADY_EXISTS')
    localStorage.setItem(sharedKey, JSON.stringify(value))
    try { sessionStorage.setItem(storageKey, JSON.stringify(value)) }
    catch (error) {
      // A competing tab may have replaced the marker meanwhile. Never erase it.
      if (sameSetupFinalizeOperation(readShared(), value)) localStorage.removeItem(sharedKey)
      throw error
    }
    setPending(value)
  }
  const clear = operation => {
    // A receipt can clear only its exact account/family/epoch/revision/ordered codes.
    // UUID equality alone is insufficient when browser storage conflicts.
    let saved
    try { saved = JSON.parse(sessionStorage.getItem(storageKey) || 'null') }
    catch { setPending({ blocked: true }); return false }
    const shared = readShared()
    if ((saved && !sameSetupFinalizeOperation(saved, operation)) ||
        (shared && !sameSetupFinalizeOperation(shared, operation))) {
      setPending({ blocked: true })
      return false
    }
    if (saved) sessionStorage.removeItem(storageKey)
    if (shared) localStorage.removeItem(sharedKey)
    // Storage is shared between tabs; a new marker must keep all writes blocked.
    let remaining
    try {
      remaining = reconcileSetupPending(
        JSON.parse(sessionStorage.getItem(storageKey) || 'null'),
        readShared(), workspaceId, accountId
      )
    } catch { remaining = { blocked: true } }
    setPending(remaining)
    return !remaining
  }
  useEffect(() => {
    if (!sharedKey) return
    const sync = event => {
      if (event.key !== sharedKey) return
      setConfirmed(false)
      try {
        const session = JSON.parse(sessionStorage.getItem(storageKey) || 'null')
        const shared = event.newValue ? JSON.parse(event.newValue) : null
        setPending(reconcileSetupPending(session, shared, workspaceId, accountId))
      } catch { setPending({ blocked: true }) }
    }
    window.addEventListener('storage', sync)
    return () => window.removeEventListener('storage', sync)
  }, [sharedKey, storageKey, workspaceId, accountId])
  useEffect(() => {
    const id = ++seq.current
    if (!workspaceId || !canManageSetup) return
    setLoadingSetup(true)
    setUnavailable(false)
    Promise.all([getSetupState(getAccessToken(), workspaceId), getTemplateCatalog(getAccessToken())])
      .then(([state, data]) => {
        const checked = validateSetupCatalog(data)
        const safeState = assertSetupState(state, workspaceId)
        if (seq.current === id) {
          setSetup(safeState); setCatalog(checked.templates); setPacks(checked.packs)
          setLoadingSetup(false)
        }
      })
      .catch(() => { if (seq.current === id) { setUnavailable(true); setLoadingSetup(false) } })
    const guard = seq
    return () => { if (guard.current === id) guard.current = id + 1 }
  }, [workspaceId, getAccessToken, canManageSetup, loadAttempt])
  // Catalog responses are portable localized DTOs. Invalid entries must never
  // become part of a finalize payload (no duplicate or blank catalog codes).
  const validCatalog = catalog.filter(item => item && typeof item.code === 'string' &&
    item.code.trim() && (item.kind === 'TASK' || item.kind === 'REWARD'))
  const catalogCodes = new Set(validCatalog.map(item => item.code))
  const cat = item => item?.definition?.category || item?.suggested?.category || 'other'
  const typed = validCatalog.filter(item => item.kind === kind &&
    (pack === 'ALL' || packs.find(p => p.code === pack)?.templateCodes?.includes(item.code)))
  const categories = [...new Set(typed.map(cat))].sort()
  const items = typed.filter(item => {
    if (category !== 'ALL' && cat(item) !== category) return false
    const term = search.trim().toLocaleLowerCase()
    if (!term) return true
    return [item.code, item.title?.[lang], item.title?.en, item.description?.[lang]]
      .filter(Boolean).join(' ').toLocaleLowerCase().includes(term)
  })
  const matchingCodes = items.map(item => item.code)
  const allMatchingSelected = matchingCodes.length > 0 && matchingCodes.every(code => selected.includes(code))
  const updateSelection = checked => {
    setSelected(previous => selectSetupMatching(previous, matchingCodes, checked, catalogCodes))
    setReview(null); setConfirmed(false); seq.current++
  }
  const setStep = async (step, status) => {
    if (!canManageSetup || !setup || busy || pending || readShared() || !canChangeSetupStep(setup, step, status)) return
    const id = ++seq.current
    const origin = workspaceId
    const before = setup
    setBusy(true); setError(false); setErrorStatus(null); setReview(null); setConfirmed(false)
    try {
      const result = await setSetupStep(getAccessToken(), origin, step, status, before.revision)
      if (id === seq.current) setSetup(assertSetupStepTransition(before, result, step, status))
    } catch (e) {
      if (id !== seq.current) return
      // A PATCH may have committed before the response was lost. Read once,
      // never issue a second PATCH or silently advance a new revision.
      try {
        const observed = assertSetupState(await getSetupState(getAccessToken(), origin), origin)
        if (id !== seq.current) return
        const outcome = reconcileSetupStepAfterFailure(before, observed, step, status)
        setSetup(observed)
        if (outcome !== 'MATCHED') { setError(true); setErrorStatus(409) }
      } catch {
        if (id === seq.current) { setError(true); setErrorStatus(e?.status) }
      }
    } finally { if (id === seq.current) setBusy(false) }
  }
  const preview = async () => {
    if (!canManageSetup || !setup || busy || pending || readShared() || !selected.length || selected.some(code => !catalogCodes.has(code))) return
    const id = ++seq.current
    const workspace = workspaceId
    const codes = [...new Set(validCatalog.filter(item => selected.includes(item.code)).map(item => item.code))]
    setBusy(true); setReview(null); setConfirmed(false); setError(false); setErrorStatus(null)
    try {
      const result = await reviewSetupCatalog(getAccessToken(), workspace, setup.revision, codes)
      if (seq.current === id) setReview(assertSetupReview(result, setup, codes))
    } catch (e) { if (seq.current === id) { setError(true); setErrorStatus(e?.status ?? null) } }
    finally { if (seq.current === id) setBusy(false) }
  }
  const recover = async () => {
    if (!canManageSetup || !pending?.operationId || !Number.isSafeInteger(pending.epoch) || busy) return
    const scope = storageKey
    const operation = pending
    setBusy(true); setError(false); setErrorStatus(null)
    try {
      const result = await getSetupFinalizeReceipt(getAccessToken(), operation.workspaceId, operation.operationId)
      if (activeScope.current !== scope) return
      assertSetupFinalizeReceipt(result, operation)
      const refreshed = assertSetupCompletionAfterReceipt(await getSetupState(getAccessToken(), operation.workspaceId), operation)
      if (activeScope.current !== scope) return
      if (!clear(operation)) { setError(true); return }
      setFinished(result); setSetup(refreshed)
    } catch (e) { if (activeScope.current === scope) { setError(true); setErrorStatus(e?.status) } }
    finally { if (activeScope.current === scope) setBusy(false) }
  }
  const finalize = async () => {
    if (!canManageSetup || !confirmed || pending || busy || !storageKey || !setup || !review ||
        sessionStorage.getItem(storageKey) || readShared() ||
        review.revision !== setup.revision || review.dataEpoch !== setup.dataEpoch ||
        setup.status === 'COMPLETED' || !window.crypto?.randomUUID ||
        !SETUP_STEP_CODES.every(code => ['COMPLETE','SKIPPED'].includes(setup.steps?.[code]))) return
    const codes = [...new Set(validCatalog.filter(item => selected.includes(item.code)).map(item => item.code))]
    try { assertSetupReview(review, setup, codes) } catch { setError(true); return }
    if (!codes.length || selected.some(code => !catalogCodes.has(code)) ||
        JSON.stringify(codes) !== JSON.stringify(review.selectedCodes)) return
    const scope = storageKey
    const operation = { accountId, workspaceId, epoch: setup.dataEpoch, revision: setup.revision,
      codes, operationId: window.crypto.randomUUID() }
    try { persist(operation) } catch { setError(true); return }
    setBusy(true); setError(false); setErrorStatus(null)
    try {
      const result = await finalizeSetupImport(getAccessToken(), operation.workspaceId, operation.operationId, operation.revision, operation.codes)
      if (activeScope.current !== scope) return
      assertSetupFinalizeReceipt(result, operation)
      const refreshed = assertSetupCompletionAfterReceipt(await getSetupState(getAccessToken(), operation.workspaceId), operation)
      if (activeScope.current !== scope) return
      if (!clear(operation)) { setError(true); return }
      setFinished(result); setSetup(refreshed)
    } catch { if (activeScope.current === scope) setError(true) }
    finally { if (activeScope.current === scope) setBusy(false) }
  }
  return <section id="initial-family-setup" className="settings-section" aria-label={w[0]}>
    <h2>{w[0]}</h2>
    <p>{w[11]}</p>
    {loadingSetup && <p role="status">{({uk:'Завантаження стану та каталогу…',ru:'Загрузка состояния и каталога…',de:'Einrichtung und Katalog werden geladen…',en:'Loading setup and catalog…'})[lang] || 'Loading setup and catalog…'}</p>}
    {unavailable && <div role="status">
      <p>{w[1]}</p>
      <button type="button" className="secondary-button" disabled={loadingSetup || busy}
        onClick={() => { setError(false); setErrorStatus(null); setLoadAttempt(value => value + 1) }}>
        {({uk:'Повторити завантаження',ru:'Повторить загрузку',de:'Erneut laden',en:'Retry loading'})[lang] || 'Retry loading'}
      </button>
    </div>}
    {error && <p role="alert">{errorStatus === 409 ? ({uk:'Дані налаштування змінилися. Оновіть сторінку та перевірте стан операції.',ru:'Данные изменились. Обновите страницу и проверьте операцию.',de:'Einrichtungsdaten geändert. Seite aktualisieren und Vorgang prüfen.',en:'Setup changed. Refresh and check operation status.'})[lang] : errorStatus === 403 ? ({uk:'Недостатньо прав для налаштування.',ru:'Недостаточно прав для настройки.',de:'Keine Berechtigung für die Einrichtung.',en:'Not authorized to manage setup.'})[lang] : w[12]}</p>}
    {setup && <>
      {setup.status === 'COMPLETED' && <p role="status">{({uk:'Початкове налаштування завершено. Новий імпорт недоступний.',ru:'Первоначальная настройка завершена. Повторный импорт недоступен.',de:'Ersteinrichtung abgeschlossen. Erneuter Import nicht verfügbar.',en:'Initial setup completed. New import is unavailable.'})[lang]}</p>}
      <p>{(setupStates[lang] || setupStates.en)[setup.status] || setup.status} · {SETUP_STEP_CODES.filter(code => ['COMPLETE','SKIPPED'].includes(setup.steps?.[code])).length}/{SETUP_STEP_CODES.length}</p>
      {setup.status !== 'COMPLETED' && <div className="template-toolbar">
        <label>{w[2]}<select value={kind} disabled={busy || !!pending} onChange={e=>{setKind(e.target.value);setCategory('ALL');setReview(null);setConfirmed(false);seq.current++}}><option value="TASK">{w[4]}</option><option value="REWARD">{w[5]}</option></select></label>
        <label>{({uk:'Набір',ru:'Набор',de:'Paket',en:'Pack'})[lang] || 'Pack'}<select value={pack} disabled={busy || !!pending} onChange={e=>{setPack(e.target.value);setCategory('ALL');setReview(null);setConfirmed(false);seq.current++}}><option value="ALL">{({uk:'Усі набори',ru:'Все наборы',de:'Alle Pakete',en:'All packs'})[lang] || 'All packs'}</option>{packs.map(p=><option value={p.code} key={p.code}>{p.title?.[lang] || p.title?.en || p.code}</option>)}</select></label>
        <label>{w[3]}<select value={category} disabled={busy || !!pending} onChange={e=>{setCategory(e.target.value);setReview(null);setConfirmed(false);seq.current++}}><option value="ALL">{w[6]}</option>{categories.map(c=><option key={c} value={c}>{templateCategoryLabel(c, lang)}</option>)}</select></label>
      </div>}
      {setup.status !== 'COMPLETED' && <div className="template-toolbar">
        <label>{({
          uk:'Пошук шаблонів',ru:'Поиск шаблонов',de:'Vorlagen suchen',en:'Search templates',
        })[lang]}
          <input type="search" value={search} maxLength={120} disabled={busy || !!pending}
            onChange={e=>{setSearch(e.target.value);setReview(null);setConfirmed(false);seq.current++}}
            placeholder={({uk:'Назва або код',ru:'Название или код',de:'Titel oder Code',en:'Title or code'})[lang]}/>
        </label>
      </div>}
      {setup.status !== 'COMPLETED' && <div className="template-toolbar">
        <button type="button" className="secondary-button"
          disabled={busy || !!pending || matchingCodes.length === 0}
          onClick={() => updateSelection(true)}>{({
            uk:'Вибрати всі знайдені',ru:'Выбрать все найденные',de:'Alle Treffer auswählen',en:'Select all matching',
          })[lang]}</button>
        <button type="button" className="secondary-button"
          disabled={busy || !!pending || !allMatchingSelected}
          onClick={() => updateSelection(false)}>{({
            uk:'Зняти вибір із знайдених',ru:'Снять выбор с найденных',de:'Trefferauswahl entfernen',en:'Deselect matching',
          })[lang]}</button>
        <span>{matchingCodes.length} / {catalog.length}</span>
      </div>}
      {setup.status !== 'COMPLETED' && <div className="initial-setup-template-list" role="group" aria-label={({uk:'Шаблони для вибору',ru:'Шаблоны для выбора',de:'Vorlagen zur Auswahl',en:'Templates to select'})[lang] || 'Templates to select'}>
        {items.map(item=><label key={item.code} className="initial-setup-template-option">
          <input type="checkbox" disabled={busy || !!pending || setup.status === 'COMPLETED'} checked={selected.includes(item.code)} onChange={()=>{setSelected(v=>selectSetupMatching(v,[item.code],!v.includes(item.code),catalogCodes));setReview(null);setConfirmed(false);seq.current++}}/>
          <span>{item.title?.[lang] || item.title?.en || item.code}</span>
        </label>)}
      </div>}
      {setup.status !== 'COMPLETED' && <p role="status" aria-live="polite" aria-atomic="true">{w[8]}: {selected.length}</p>}
      <div>
        <strong>{({uk:'Кроки налаштування',ru:'Шаги настройки',de:'Einrichtungsschritte',en:'Setup steps'})[lang] || 'Setup steps'}</strong>
        {SETUP_STEP_CODES.map(step => <div key={step} className="initial-setup-step">
          <span>{(setupSteps[lang] || setupSteps.en)[step] || step} — {(setupStates[lang] || setupStates.en)[setup.steps?.[step] || 'PENDING'] || (setup.steps?.[step] || 'PENDING')}</span>
          {setup.status !== 'COMPLETED' && <>
            <button className="secondary-button" type="button" disabled={busy || !!pending || !canManageSetup || !canChangeSetupStep(setup, step,'COMPLETE')} onClick={()=>setStep(step,'COMPLETE')}>{({uk:'Позначити виконаним',ru:'Отметить выполненным',de:'Als erledigt markieren',en:'Mark complete'})[lang]}</button>
            <button className="secondary-button" type="button" disabled={busy || !!pending || !canManageSetup || !canChangeSetupStep(setup, step,'SKIPPED')} onClick={()=>setStep(step,'SKIPPED')}>{({uk:'Пропустити',ru:'Пропустить',de:'Überspringen',en:'Skip'})[lang]}</button>
          </>}
        </div>)}
      </div>
      {setup.status !== 'COMPLETED' && <button type="button" className="secondary-button" disabled={!selected.length || busy || !!pending || setup.status === 'COMPLETED'} onClick={preview}>{w[7]}</button>}
      {review && <p role="status">{w[9]}: {review.toImportCount} · {w[10]}: {review.alreadyImportedCount}</p>}
      {pending && <div role="status"><p>{({uk:'Результат попередньої операції невідомий. Новий імпорт заблоковано.',ru:'Результат предыдущей операции неизвестен. Новый импорт заблокирован.',de:'Vorheriger Vorgang unklar. Neuer Import gesperrt.',en:'Previous result unknown. New import blocked.'})[lang]}</p><button type="button" className="secondary-button" disabled={busy || !pending?.operationId} onClick={recover}>{({uk:'Перевірити квитанцію',ru:'Проверить квитанцию',de:'Beleg prüfen',en:'Check receipt'})[lang]}</button></div>}
      {review && !pending && setup.status !== 'COMPLETED' && <div>
        <label><input type="checkbox" checked={confirmed} disabled={busy} onChange={e=>setConfirmed(e.target.checked)} />{({uk:'Підтверджую імпорт приватних шаблонів і завершення налаштування без активації.',ru:'Подтверждаю импорт и завершение без активации.',de:'Import und Abschluss ohne Aktivierung bestätigen.',en:'Confirm private template import and setup completion without activation.'})[lang]}</label>
        <button type="button" className="secondary-button" disabled={busy || !confirmed || !storageKey || !SETUP_STEP_CODES.every(code => ['COMPLETE','SKIPPED'].includes(setup.steps?.[code]))} onClick={finalize}>{({uk:'Завершити й імпортувати',ru:'Завершить и импортировать',de:'Abschließen und importieren',en:'Finalize and import'})[lang]}</button>
      </div>}
      {finished && <p role="status">{({uk:'Імпорт завершено',ru:'Импорт завершён',de:'Import abgeschlossen',en:'Import completed'})[lang]}</p>}
    </>}
  </section>
}
