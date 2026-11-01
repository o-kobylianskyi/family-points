import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { getTemplateCatalog, getPrivateTemplates, previewTemplateImport, importTemplates } from '../api/templateApi'
import { templateCategoryLabel } from '../utils/templateCategoryLabels'
import './FamilySetupPage.css'

// This is an explicit, non-destructive onboarding entry. It does not infer that an
// existing family is empty and never creates active tasks/rewards on page load.
const copy = {
  uk: ['Налаштування сім’ї', 'Групи', 'Вибір шаблонів', 'Перевірка', 'Імпорт', 'Групи учасників і групи завдань — різні сутності. Налаштуйте їх окремо.', 'Перейти до учасників і груп', 'Завдання', 'Нагороди', 'Усі набори', 'Пошук', 'Обрано', 'Уже імпортовано', 'Перевірити імпорт', 'Підтвердити імпорт', 'Імпорт не створює активних завдань чи нагород і не змінює баланс.', 'Перейти до бібліотеки та налаштувати завдання/нагороди', 'Назад', 'Далі', 'Завантаження…', 'Повторити', 'Не вдалося отримати дані. Перевірте підключення або права доступу.', 'Виберіть хоча б один новий шаблон.', 'Імпорт завершено. Для активації створіть завдання чи нагороду окремо.', 'Пропустити імпорт', 'Кількість нових', 'Вибрати всі знайдені', 'Очистити вибір', 'Усі категорії', 'Перевірка потребує оновлення після зміни вибору.', 'Повернутися до шаблонів'],
  ru: ['Настройка семьи', 'Группы', 'Выбор шаблонов', 'Проверка', 'Импорт', 'Группы участников и группы задач — разные сущности. Настраивайте их отдельно.', 'Перейти к участникам и группам', 'Задачи', 'Награды', 'Все наборы', 'Поиск', 'Выбрано', 'Уже импортировано', 'Проверить импорт', 'Подтвердить импорт', 'Импорт не создаёт активных задач или наград и не меняет баланс.', 'Перейти к библиотеке и настроить задачи/награды', 'Назад', 'Далее', 'Загрузка…', 'Повторить', 'Не удалось загрузить данные. Проверьте подключение и права.', 'Выберите хотя бы один новый шаблон.', 'Импорт завершён. Активация выполняется отдельно.', 'Пропустить импорт', 'Новых', 'Выбрать найденные', 'Очистить выбор', 'Все категории', 'После изменения выбора проверьте план заново.', 'Вернуться к шаблонам'],
  de: ['Familie einrichten', 'Gruppen', 'Vorlagen wählen', 'Prüfung', 'Import', 'Mitgliedergruppen und Aufgabengruppen sind verschieden und werden getrennt verwaltet.', 'Mitglieder und Gruppen öffnen', 'Aufgaben', 'Belohnungen', 'Alle Pakete', 'Suche', 'Ausgewählt', 'Bereits importiert', 'Import prüfen', 'Import bestätigen', 'Der Import aktiviert keine Aufgaben oder Belohnungen und ändert keine Salden.', 'Vorlagenbibliothek und Aktivierung öffnen', 'Zurück', 'Weiter', 'Laden…', 'Erneut versuchen', 'Daten konnten nicht geladen werden. Verbindung und Rechte prüfen.', 'Mindestens eine neue Vorlage wählen.', 'Import abgeschlossen. Aktivierung erfolgt separat.', 'Import überspringen', 'Neu', 'Gefundene auswählen', 'Auswahl löschen', 'Alle Kategorien', 'Nach Änderungen muss die Auswahl erneut geprüft werden.', 'Zur Bibliothek zurück'],
  en: ['Set up your family', 'Groups', 'Choose templates', 'Review', 'Import', 'Member groups and task groups are separate entities. Configure them independently.', 'Open members and groups', 'Tasks', 'Rewards', 'All packs', 'Search', 'Selected', 'Already imported', 'Preview import', 'Confirm import', 'Import does not activate tasks or rewards or change balances.', 'Open template library to configure tasks and rewards', 'Back', 'Next', 'Loading…', 'Retry', 'Could not load data. Check connectivity and permissions.', 'Select at least one new template.', 'Import completed. Create active tasks or rewards separately.', 'Skip import', 'New items', 'Select matching', 'Clear selection', 'All categories', 'Selection changed: review the import again.', 'Back to templates']
}
const nameOf = (item, lang) => item?.title?.[lang] || item?.title?.en || item?.code || ''
const categoryOf = item => item?.suggested?.category || item?.definition?.category || 'other'
// Draft choices are session-only and scoped to the authenticated account + family.
// No preview, credential, or permission is persisted; all imports require fresh server review.
const draftKey = (accountId, workspaceId) => `fp:onboarding:v1:${accountId}:${workspaceId}`
function readDraft(key) {
  try {
    const value = JSON.parse(sessionStorage.getItem(key) || 'null')
    return value && Array.isArray(value.codes) && value.codes.length <= 142 &&
      value.codes.every(code => typeof code === 'string' && code.length < 100)
      ? [...new Set(value.codes)] : []
  } catch { return [] }
}
function saveDraft(key, codes) {
  try {
    if (codes.length) sessionStorage.setItem(key, JSON.stringify({ codes }))
    else sessionStorage.removeItem(key)
  } catch { /* Session storage is optional; manual selection still works. */ }
}
const validCatalog = value => value && Array.isArray(value.templates) && Array.isArray(value.packs) &&
  value.templates.every(item => typeof item?.code === 'string' && ['TASK', 'REWARD'].includes(item.kind))
export default function FamilySetupPage() {
  const { i18n } = useTranslation()
  const lang = i18n.language.split('-')[0]
  const t = copy[lang] || copy.en
  const { currentUser, getAccessToken } = useAuth()
  const workspaceId = currentUser?.workspaceId
  const accountId = currentUser?.accountId ?? currentUser?.userId ?? currentUser?.id
  const draftStorageKey = workspaceId && accountId != null ? draftKey(accountId, workspaceId) : null
  const [step, setStep] = useState(0)
  const [catalog, setCatalog] = useState({ templates: [], packs: [] })
  const [owned, setOwned] = useState([])
  const [selected, setSelected] = useState([])
  const [kind, setKind] = useState('TASK')
  const [pack, setPack] = useState('ALL')
  const [category, setCategory] = useState('ALL')
  const [search, setSearch] = useState('')
  const [preview, setPreview] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [complete, setComplete] = useState(false)
  const generation = useRef(0)
  const revision = useRef(0)
  const lock = useRef(false)
  const workspaceRef = useRef(workspaceId)
  const identityRef = useRef(draftStorageKey)
  if (workspaceRef.current !== workspaceId || identityRef.current !== draftStorageKey) {
    workspaceRef.current = workspaceId; identityRef.current = draftStorageKey
    generation.current++; revision.current++; lock.current = false
  }
  const load = async (id, request, preserveError = false) => {
    setBusy(true); if (!preserveError) setError(''); setPreview(null)
    try {
      const [nextCatalog, nextOwned] = await Promise.all([
        getTemplateCatalog(getAccessToken()), getPrivateTemplates(getAccessToken(), id)
      ])
      if (!validCatalog(nextCatalog) || !Array.isArray(nextOwned)) throw new Error('INVALID_TEMPLATE_CATALOG')
      if (generation.current === request && workspaceRef.current === id) {
        setCatalog(nextCatalog); setOwned(nextOwned)
      }
    } catch {
      if (generation.current === request && workspaceRef.current === id) setError(t[21])
    } finally {
      if (generation.current === request && workspaceRef.current === id) setBusy(false)
    }
  }
  useEffect(() => {
    const request = ++generation.current
    setStep(0); setSelected(draftStorageKey ? readDraft(draftStorageKey) : []); setOwned([]); setCatalog({ templates: [], packs: [] })
    setPack('ALL'); setKind('TASK'); setCategory('ALL'); setSearch('')
    setComplete(false); setPreview(null)
    if (workspaceId) load(workspaceId, request)
    return () => { generation.current++ }
  }, [workspaceId, draftStorageKey])
  const ownedCodes = new Set(owned.map(item => item.catalogCode).filter(Boolean))
  const available = catalog.templates.filter(item => !ownedCodes.has(item.code))
  const filtered = catalog.templates.filter(item =>
    item.kind === kind &&
    (pack === 'ALL' || catalog.packs.find(group => group.code === pack)?.templateCodes?.includes(item.code)) &&
    (category === 'ALL' || categoryOf(item) === category) &&
    [nameOf(item, lang), item.code].join(' ').toLocaleLowerCase().includes(search.trim().toLocaleLowerCase())
  )
  const categories = [...new Set(catalog.templates.filter(item => item.kind === kind).map(categoryOf))].sort()
  const selectedNew = selected.filter(code => available.some(item => item.code === code))
  const choose = updater => {
    invalidate()
    setSelected(old => {
      const codes = typeof updater === 'function' ? updater(old) : updater
      if (draftStorageKey) saveDraft(draftStorageKey, codes)
      return codes
    })
  }
  const invalidate = () => { revision.current++; setPreview(null); setError('') }
  const toggle = code => choose(old => old.includes(code) ? old.filter(value => value !== code) : [...old, code])
  const previewSelection = async () => {
    if (lock.current || !selectedNew.length) { if (!selectedNew.length) setError(t[22]); return }
    lock.current = true
    const request = generation.current, revisionAtStart = revision.current, id = workspaceId
    const codes = [...selectedNew]
    setBusy(true); setError(''); setPreview(null)
    try {
      const result = await previewTemplateImport(getAccessToken(), id, codes)
      if (request !== generation.current || id !== workspaceRef.current || revisionAtStart !== revision.current) return
      if (!result || (result.workspaceId != null && String(result.workspaceId) !== String(id)) ||
          !Array.isArray(result.toImport) || !Array.isArray(result.alreadyImported) ||
          result.toImport.some(code => !codes.includes(code)) ||
          result.alreadyImported.some(code => !codes.includes(code)) ||
          new Set([...result.toImport, ...result.alreadyImported]).size !== codes.length)
        throw new Error('INVALID_IMPORT_PREVIEW')
      setPreview({ result, codes })
      setStep(2)
    } catch {
      if (request === generation.current && id === workspaceRef.current) setError(t[21])
    } finally {
      lock.current = false
      if (request === generation.current && id === workspaceRef.current) setBusy(false)
    }
  }
  const confirm = async () => {
    if (lock.current || !preview || !preview.codes.length || !preview.result.toImport.length ||
        preview.codes.join('|') !== selectedNew.join('|')) return
    lock.current = true
    const id = workspaceId, request = generation.current, codes = [...preview.codes]
    setBusy(true); setError('')
    try {
      await importTemplates(getAccessToken(), id, preview.result.toImport)
      if (request !== generation.current || workspaceRef.current !== id) return
      // Re-read authoritative server state. Never infer imported records from a POST alone.
      const latest = await getPrivateTemplates(getAccessToken(), id)
      if (!Array.isArray(latest)) throw new Error('INVALID_IMPORT_RESULT')
      if (request !== generation.current || workspaceRef.current !== id) return
      setOwned(latest); choose([]); setComplete(true); setStep(3)
    } catch {
      if (request === generation.current && workspaceRef.current === id) {
        setPreview(null); setStep(1); setError(t[21])
        // An ambiguous network failure may have committed. Require fresh server reconciliation.
        await load(id, request, true)
      }
    } finally {
      lock.current = false
      if (request === generation.current && workspaceRef.current === id) setBusy(false)
    }
  }
  if (!workspaceId) return null
  return <main className="family-setup settings-section">
    <h1>{t[0]}</h1>
    <nav className="family-setup-steps" aria-label={t[0]}>{[t[1], t[2], t[3], t[4]].map((label, index) =>
      <span key={label} aria-current={step === index ? 'step' : undefined} className={step === index ? 'active' : ''}>{index + 1}. {label}</span>)}</nav>
    {error && <p role="alert" className="error-message">{error}</p>}
    {busy && <p role="status">{t[19]}</p>}
    {step === 0 && <section><h2>{t[1]}</h2><p>{t[5]}</p>
      <Link to="/members" className="secondary-button">{t[6]}</Link>
      <p>{t[15]}</p><button type="button" onClick={() => setStep(1)}>{t[18]}</button>
    </section>}
    {step === 1 && <section><h2>{t[2]}</h2>
      <div className="family-setup-filters">
        <label>{t[7]} / {t[8]}<select value={kind} onChange={e => {setKind(e.target.value); setCategory('ALL')}}><option value="TASK">{t[7]}</option><option value="REWARD">{t[8]}</option></select></label>
        <label>{t[9]}<select value={pack} onChange={e => setPack(e.target.value)}><option value="ALL">{t[9]}</option>{catalog.packs.map(item => <option key={item.code} value={item.code}>{nameOf(item, lang)}</option>)}</select></label>
        <label>{t[28]}<select value={category} onChange={e => setCategory(e.target.value)}><option value="ALL">{t[28]}</option>{categories.map(code => <option key={code} value={code}>{templateCategoryLabel(code, lang)}</option>)}</select></label>
        <label>{t[10]}<input type="search" value={search} onChange={e => setSearch(e.target.value)}/></label>
      </div>
      <div className="family-setup-actions"><button type="button" disabled={busy} onClick={() => {invalidate(); choose(old => [...new Set([...old, ...filtered.filter(item => !ownedCodes.has(item.code)).map(item => item.code)])])}}>{t[26]}</button>
        <button type="button" disabled={busy} onClick={() => choose([])}>{t[27]}</button></div>
      <div className="family-setup-catalog">{filtered.map(item => <label key={item.code} className="family-setup-item">
        <input type="checkbox" checked={selected.includes(item.code) || ownedCodes.has(item.code)} disabled={busy || ownedCodes.has(item.code)} onChange={() => toggle(item.code)}/>
        <span><strong>{nameOf(item, lang)}</strong><small>{templateCategoryLabel(categoryOf(item), lang)}{ownedCodes.has(item.code) ? ' · ' + t[12] : ''}</small></span>
      </label>)}</div>
      <p role="status">{t[11]}: {selectedNew.length} · {t[12]}: {ownedCodes.size}</p>
      <p>{t[15]}</p>
      <div className="family-setup-actions"><button type="button" onClick={() => setStep(0)}>{t[17]}</button>
        <button type="button" disabled={busy || !selectedNew.length} onClick={previewSelection}>{t[13]}</button>
        <button type="button" disabled={busy} onClick={() => setStep(3)}>{t[24]}</button></div>
    </section>}
    {step === 2 && <section><h2>{t[3]}</h2><p>{t[15]}</p>
      <p>{t[25]}: {preview?.result?.toImport?.length || 0} · {t[12]}: {preview?.result?.alreadyImported?.length || 0}</p>
      <ul>{preview?.codes.map(code => <li key={code}>{nameOf(catalog.templates.find(item => item.code === code), lang)}</li>)}</ul>
      {preview?.result?.warnings?.length > 0 && <ul>{preview.result.warnings.map((warning, index) => <li key={index}>{String(warning)}</li>)}</ul>}
      <div className="family-setup-actions"><button type="button" disabled={busy} onClick={() => {setPreview(null);setStep(1)}}>{t[17]}</button>
        <button type="button" disabled={busy || !preview?.result?.toImport?.length} onClick={confirm}>{t[14]}</button></div>
    </section>}
    {step === 3 && <section><h2>{t[4]}</h2><p role="status">{complete ? t[23] : t[15]}</p>
      <Link to="/templates">{t[16]}</Link>
      <p><Link to="/tasks">{t[7]}</Link> · <Link to="/rewards">{t[8]}</Link></p>
      <button type="button" onClick={() => {setStep(1); setComplete(false)}}>{t[30]}</button>
    </section>}
    {error && <button type="button" disabled={busy} onClick={() => load(workspaceId, ++generation.current)}>{t[20]}</button>}
  </main>
}
