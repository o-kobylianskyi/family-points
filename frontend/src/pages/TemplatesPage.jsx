import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import TemplateFieldsEditor from '../components/TemplateFieldsEditor'
import { getWorkspaceMembers } from '../api/workspaceApi'
import { getPointTypes } from '../api/economyApi'
import TemplateDraftFieldsEditor from '../components/TemplateDraftFieldsEditor'
import { getTemplateCatalog, getPrivateTemplates, previewTemplateImport, importTemplates, updatePrivateTemplate, deletePrivateTemplate, getTemplateCreationDraft } from '../api/templateApi'

const texts = {
  uk: ['Бібліотека шаблонів','Глобальні рекомендації','Мої шаблони','Виберіть потрібні шаблони. Імпорт додає їх у бібліотеку, а не створює активні завдання чи нагороди.','Зараз глобальних рекомендацій немає.','Переглянути імпорт','Імпортувати вибране','Очистити вибір','Уже в бібліотеці','Буде додано','Зберегти','Видалити','Немає особистих шаблонів.','Назва','Визначення (JSON)','Збережено','Виберіть шаблон для редагування','Видалити цей шаблон із бібліотеки?','Шаблони імпортовано','Не вибрано жодного шаблону.','Тільки адміністратор сім’ї може змінювати бібліотеку.','Повторно використовувати їх можна буде через окреме створення активного об’єкта.'],
  de: ['Vorlagenbibliothek','Globale Vorschläge','Meine Vorlagen','Vorlagen einzeln auswählen. Der Import speichert sie nur in der Bibliothek und erstellt keine aktiven Aufgaben oder Belohnungen.','Noch keine globalen Vorschläge.','Import prüfen','Auswahl importieren','Auswahl löschen','Bereits vorhanden','Wird hinzugefügt','Speichern','Löschen','Noch keine privaten Vorlagen.','Titel','Definition (JSON)','Gespeichert','Vorlage zum Bearbeiten auswählen','Diese Vorlage aus der Bibliothek löschen?','Vorlagen importiert','Keine Vorlagen ausgewählt.','Nur Familienadministratoren dürfen Vorlagen verwalten.','Aktive Aufgaben und Belohnungen werden später separat erstellt.'],
  en: ['Template library','Global suggestions','My templates','Select templates individually. Import saves them to your private library, not as active tasks or rewards.','No global suggestions yet.','Preview import','Import selected','Clear selection','Already imported','To be imported','Save','Delete','No private templates yet.','Title','Definition (JSON)','Saved','Select a template to edit','Delete this template from the library?','Templates imported','No templates selected.','Only workspace administrators can manage templates.','Create active tasks and rewards separately later.'],
  ru: ['Библиотека шаблонов','Глобальные рекомендации','Мои шаблоны','Выбирайте шаблоны отдельно. Импорт добавляет их в библиотеку без создания активных задач и наград.','Пока нет глобальных рекомендаций.','Предпросмотр импорта','Импортировать выбранные','Очистить выбор','Уже импортированы','Будут добавлены','Сохранить','Удалить','Пока нет личных шаблонов.','Название','Определение (JSON)','Сохранено','Выберите шаблон для редактирования','Удалить шаблон из библиотеки?','Шаблоны импортированы','Шаблоны не выбраны.','Только администратор семьи может менять библиотеку.','Активные задачи и награды создаются отдельно.']
}
const requestErrors = {
  uk: {
    INVALID_DEFINITION: 'Визначення шаблону має бути коректним JSON-об’єктом (не більше 30 000 символів).',
    NETWORK: 'Немає зв’язку із сервером. Перевірте підключення та повторіть спробу.',
    400: 'Некоректні дані шаблону або вибору. Перевірте введені значення.',
    401: 'Сеанс завершився. Увійдіть знову.',
    403: 'Недостатньо прав для керування шаблонами цієї сім’ї.',
    404: 'Шаблон або сім’ю не знайдено. Оновіть сторінку.',
    409: 'Дані змінилися або шаблон уже імпортовано. Оновіть список і повторіть спробу.',
    413: 'Розмір шаблону перевищує допустимий.',
    422: 'Шаблон містить непідтримувані параметри або залежності.',
    DEFAULT: 'Не вдалося виконати операцію із шаблонами. Повторіть спробу.'
  },
  ru: {
    INVALID_DEFINITION: 'Определение шаблона должно быть корректным JSON-объектом (не более 30 000 символов).',
    NETWORK: 'Нет связи с сервером. Проверьте подключение и повторите попытку.',
    400: 'Некорректные данные шаблона или выбора. Проверьте значения.',
    401: 'Сеанс завершён. Войдите снова.',
    403: 'Недостаточно прав для управления шаблонами этой семьи.',
    404: 'Шаблон или семья не найдены. Обновите страницу.',
    409: 'Данные изменились или шаблон уже импортирован. Обновите список.',
    413: 'Размер шаблона превышает допустимый.',
    422: 'Шаблон содержит неподдерживаемые параметры или зависимости.',
    DEFAULT: 'Не удалось выполнить операцию с шаблонами. Повторите попытку.'
  },
  de: {
    INVALID_DEFINITION: 'Die Vorlage muss ein gültiges JSON-Objekt sein (maximal 30.000 Zeichen).',
    NETWORK: 'Keine Verbindung zum Server. Bitte Verbindung prüfen und erneut versuchen.',
    400: 'Ungültige Vorlagen- oder Auswahldaten. Bitte Eingaben prüfen.',
    401: 'Die Sitzung ist abgelaufen. Bitte erneut anmelden.',
    403: 'Keine Berechtigung, Vorlagen dieser Familie zu verwalten.',
    404: 'Vorlage oder Familie nicht gefunden. Bitte Seite aktualisieren.',
    409: 'Daten wurden geändert oder die Vorlage wurde bereits importiert. Bitte Liste aktualisieren.',
    413: 'Die Vorlage ist zu groß.',
    422: 'Die Vorlage enthält nicht unterstützte Einstellungen oder Abhängigkeiten.',
    DEFAULT: 'Die Vorlagenaktion konnte nicht ausgeführt werden. Bitte erneut versuchen.'
  },
  en: {
    INVALID_DEFINITION: 'Template definition must be a valid JSON object (maximum 30,000 characters).',
    NETWORK: 'Cannot connect to the server. Check your connection and try again.',
    400: 'Invalid template or selection data. Check the entered values.',
    401: 'Session expired. Please sign in again.',
    403: 'You do not have permission to manage templates in this family.',
    404: 'Template or family not found. Refresh the page.',
    409: 'Data changed or template was already imported. Refresh the list and retry.',
    413: 'Template size exceeds the limit.',
    422: 'Template contains unsupported settings or dependencies.',
    DEFAULT: 'Template operation failed. Please try again.'
  }
}
function localizedTemplateError(error, language) {
  const messages = requestErrors[language] || requestErrors.en
  if (error?.message === 'INVALID_DEFINITION') return messages.INVALID_DEFINITION
  if (typeof error?.status === 'number') return messages[error.status] || messages.DEFAULT
  if (error instanceof TypeError || error?.name === 'NetworkError') return messages.NETWORK
  return messages.DEFAULT
}
const timeErrors = {
  uk: ['Вкажіть дату й час у форматі ISO-8601.', 'Час завершення має бути пізнішим за час початку.'],
  ru: ['Укажите дату и время в формате ISO-8601.', 'Время окончания должно быть позже начала.'],
  de: ['Datum und Uhrzeit müssen dem ISO-8601-Format entsprechen.', 'Die Endzeit muss nach der Startzeit liegen.'],
  en: ['Date and time must use ISO-8601 format.', 'End time must be later than start time.'],
}
export default function TemplatesPage() {
  const { i18n } = useTranslation()
  const lang = i18n.language.split('-')[0]
  const l = texts[lang] || texts.uk
  const timeErrorTexts = timeErrors[lang] || timeErrors.en
  const { currentUser, getAccessToken } = useAuth()
  const workspaceId = currentUser?.workspaceId
  const [catalog, setCatalog] = useState({ templates: [], packs: [] })
  const [privateItems, setPrivateItems] = useState([])
  const [selected, setSelected] = useState([])
  const [packFilter, setPackFilter] = useState('ALL')
  const [preview, setPreview] = useState(null)
  const [previewSelection, setPreviewSelection] = useState(null)
  const [editing, setEditing] = useState(null)
  const [creationDraft, setCreationDraft] = useState(null)
  const [draftText, setDraftText] = useState('')
  const [draftMembers, setDraftMembers] = useState([])
  const [draftPointTypes, setDraftPointTypes] = useState([])
  const [title, setTitle] = useState('')
  const [definition, setDefinition] = useState('')
  const [busy, setBusy] = useState(false)
  const actionLock = useRef(false)
  const selectionRevision = useRef(0)
  const workspaceRef = useRef(workspaceId)
  workspaceRef.current = workspaceId
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  useEffect(() => {
    let alive = true
    setCatalog({ templates: [], packs: [] }); setPrivateItems([]); setSelected([]); setPackFilter('ALL'); setPreview(null); setPreviewSelection(null); setEditing(null); setCreationDraft(null); setDraftText(''); setDraftMembers([]); setDraftPointTypes([]); setError(''); setNotice('')
    if (!workspaceId) return () => { alive = false }
    Promise.all([getTemplateCatalog(getAccessToken()), getPrivateTemplates(getAccessToken(), workspaceId)])
      .then(([a,b]) => { if (alive) { setCatalog(a || { templates: [], packs: [] }); setPrivateItems(Array.isArray(b) ? b : []) } })
      .catch(e => { if (alive) setError(e) })
    return () => { alive = false }
  }, [workspaceId, getAccessToken])
  const importedCodes = new Set(privateItems.map(item => item.catalogCode).filter(Boolean))
  const packItems = packFilter === 'ALL'
    ? (catalog.templates || [])
    : (catalog.templates || []).filter(item => (catalog.packs || []).find(pack => pack.code === packFilter)?.templateCodes?.includes(item.code))
  const visibleNewCount = packItems.filter(item => !importedCodes.has(item.code)).length
  const selectedNewCodes = selected.filter(code => !importedCodes.has(code))
  const selectVisible = checked => {
    selectionRevision.current += 1
    const codes = packItems.filter(item => !importedCodes.has(item.code)).map(item => item.code)
    setSelected(current => checked ? [...new Set([...current, ...codes])] : current.filter(code => !codes.includes(code)))
    setPreview(null)
    setPreviewSelection(null)
  }
  const toggle = code => { if (importedCodes.has(code)) return; selectionRevision.current += 1; setSelected(v => v.includes(code) ? v.filter(x=>x!==code) : [...v,code]); setPreview(null); setPreviewSelection(null) }
  const execute = async action => {
    if (actionLock.current) return
    actionLock.current = true
    const originWorkspace = workspaceId
    setBusy(true); setError(''); setNotice('')
    try { await action() } catch(e) { if (workspaceRef.current === originWorkspace) setError(e) } finally {
      actionLock.current = false
      if (workspaceRef.current === originWorkspace) setBusy(false)
    }
  }
  const refresh = async () => { const id = workspaceId; const items = await getPrivateTemplates(getAccessToken(), id); if (workspaceRef.current === id) setPrivateItems(items) }
  const chooseEdit = item => { setCreationDraft(null); setDraftText(''); setDraftMembers([]); setDraftPointTypes([]); setEditing(item); setTitle(item.title); setDefinition(item.definitionJson); setError('') }
  const validDefinition = (() => { try { return definition.length <= 30000 && !Array.isArray(JSON.parse(definition)) && JSON.parse(definition) !== null && typeof JSON.parse(definition) === 'object' } catch { return false } })()
  const validDraft = (() => { try { const value = JSON.parse(draftText); return value !== null && typeof value === 'object' && !Array.isArray(value) && draftText.length <= 30000 } catch { return false } })()
  const draftWarnings = (() => {
    if (!creationDraft || !validDraft) return []
    const data = JSON.parse(draftText)
    const warnings = []
    const local = {
      uk: {title:'Вкажіть назву.', member:'Оберіть учасника цієї сім’ї.',currency:'Оберіть активну валюту цієї сім’ї.',duration:'Тривалість часової нагороди має бути більшою за нуль.',start:'Укажіть дату початку завдання.'},
      ru: {title:'Укажите название.',member:'Выберите участника этой семьи.',currency:'Выберите активную валюту этой семьи.',duration:'Длительность временной награды должна быть больше нуля.',start:'Укажите дату начала задачи.'},
      de: {title:'Bitte einen Titel eingeben.',member:'Bitte ein Mitglied dieser Familie auswählen.',currency:'Bitte eine aktive Währung dieser Familie auswählen.',duration:'Die Dauer einer zeitbasierten Belohnung muss positiv sein.',start:'Bitte ein Startdatum angeben.'},
      en: {title:'Enter a title.',member:'Select a member of this family.',currency:'Select an active currency of this family.',duration:'Timed reward duration must be greater than zero.',start:'Enter a task start date.'}
    }[lang] || {title:'Enter a title.',member:'Select a member of this family.',currency:'Select an active currency of this family.',duration:'Duration must be positive.',start:'Enter a start date.'}
    if (!String(data.title ?? '').trim()) warnings.push(local.title)
    if (creationDraft.kind === 'TASK') {
      if ((!data.assignmentPolicy || data.assignmentPolicy === 'SINGLE_MEMBER') && !draftMembers.some(member => String(member.id) === String(data.assignedMemberId))) warnings.push(local.member)
      if (!data.startDate) warnings.push(local.start)
    }
    if (creationDraft.kind === 'REWARD') {
      if (data.rewardKind === 'TIME_BASED' && (!Number.isInteger(data.defaultDurationMinutes) || data.defaultDurationMinutes <= 0)) warnings.push(local.duration)
      if (data.pointTypeId == null) warnings.push(local.currency)
    }
    const ids = creationDraft.kind === 'TASK' ? ['rewardPointTypeId','penaltyPointTypeId'] : ['pointTypeId']
    for (const key of ids) {
      if (data[key] != null && !draftPointTypes.some(type => String(type.id) === String(data[key]))) warnings.push(local.currency)
    }
    // A recommendation with no price is incomplete, not a free reward.
    if (creationDraft.kind === 'REWARD' && (data.priceAmount == null || !Number.isInteger(data.priceAmount) || data.priceAmount < 0)) {
      warnings.push(({uk:'Вкажіть ціну цілим невід’ємним числом. Порожня ціна не означає безкоштовно.',ru:'Укажите цену целым неотрицательным числом. Пустая цена не означает бесплатно.',de:'Preis als nichtnegative ganze Zahl angeben. Ein leerer Preis bedeutet nicht kostenlos.',en:'Enter a nonnegative whole-number price. An empty price does not mean free.'})[lang] || 'Enter a valid price.')
    }
    if (creationDraft.kind === 'TASK' && !data.recurrenceType) {
      warnings.push(({uk:'Оберіть тип повторення.',ru:'Выберите тип повторения.',de:'Bitte Wiederholungsart auswählen.',en:'Select a recurrence type.'})[lang] || 'Select recurrence.')
    }
    if (creationDraft.kind === 'TASK' && data.startDate && !/^\\d{4}-\\d{2}-\\d{2}$/.test(String(data.startDate))) {
      warnings.push(({uk:'Дата початку повинна мати формат РРРР-ММ-ДД.',ru:'Дата начала должна быть в формате ГГГГ-ММ-ДД.',de:'Startdatum im Format JJJJ-MM-TT angeben.',en:'Start date must use YYYY-MM-DD format.'})[lang] || 'Invalid date.')
    }
    return [...new Set(warnings)]
  })()
  const availabilityError = (() => {
    if (!validDefinition || editing?.kind !== 'REWARD') return ''
    const value = JSON.parse(definition)
    const start = value.availabilityStartsAt
    const end = value.availabilityEndsAt
    const isIso = s => typeof s === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})?$/.test(s) && !Number.isNaN(Date.parse(s))
    const invalid = [start, end].some(s => s != null && s !== '' && !isIso(s))
    if (invalid) return timeErrorTexts[0]
    if (start && end && Date.parse(end) <= Date.parse(start)) return timeErrorTexts[1]
    return ''
  })()
  if (!workspaceId) return <div className="page-loading">...</div>
  return <div className="templates-page">
    <h1>{l[0]}</h1>
    <p>{l[3]}</p>
    {error && <div className="error-message" role="alert">{localizedTemplateError(error, lang)}</div>}
    {notice && <p role="status">{notice}</p>}
    <section className="settings-section">
      <h2>{l[1]}</h2>
      {!(catalog.templates || []).length && <p>{l[4]}</p>}
      {(catalog.packs || []).length > 0 && <label style={{display:'flex', gap:12, alignItems:'center', margin:'12px 0'}}>
        <span>{({uk:'Набір',ru:'Набор',de:'Paket',en:'Pack'})[lang] || 'Pack'}</span>
        <select value={packFilter} onChange={e => setPackFilter(e.target.value)} disabled={busy}>
          <option value="ALL">{({uk:'Усі шаблони',ru:'Все шаблоны',de:'Alle Vorlagen',en:'All templates'})[lang] || 'All templates'}</option>
          {(catalog.packs || []).map(pack => <option key={pack.code} value={pack.code}>{pack.title?.[lang] || pack.title?.en || pack.code}</option>)}
        </select>
      </label>}
      {packItems.length > 0 && <div className="modal-actions">
        <button type="button" className="secondary-button" disabled={busy} onClick={() => selectVisible(true)}>{({uk:'Вибрати показані',ru:'Выбрать показанные',de:'Sichtbare auswählen',en:'Select visible'})[lang] || 'Select visible'}</button>
        <button type="button" className="secondary-button" disabled={busy} onClick={() => selectVisible(false)}>{({uk:'Зняти вибір показаних',ru:'Снять выбор показанных',de:'Sichtbare abwählen',en:'Deselect visible'})[lang] || 'Deselect visible'}</button>
      </div>}
      <div className="template-grid">
        {packItems.map(item => <label key={item.code} className="template-card">
          <input type="checkbox" checked={selected.includes(item.code) || importedCodes.has(item.code)} onChange={()=>toggle(item.code)} disabled={busy || importedCodes.has(item.code)}/>
          <span><strong>{item.title?.[i18n.language.split('-')[0]] || item.title?.en || item.code}</strong> <small>{item.kind}</small>{item.kind === 'REWARD' && Number.isFinite(Number(item.definition?.durationMinutes)) && Number(item.definition?.durationMinutes) > 0 && <small style={{display:'block', opacity:.85}}>{({uk:'Тривалість',ru:'Продолжительность',de:'Dauer',en:'Duration'})[lang] || 'Duration'}: {item.definition.durationMinutes} {({uk:'хв',ru:'мин',de:'Min.',en:'min'})[lang] || 'min'}</small>}{importedCodes.has(item.code) && <small style={{display:'block', opacity:.8}}>{({uk:'Уже в бібліотеці',ru:'Уже в библиотеке',de:'Bereits in der Bibliothek',en:'Already in library'})[lang] || 'Already in library'}</small>}</span>
        </label>)}
      </div>
      <p>{selectedNewCodes.length} / {packItems.length} · {({uk:'Нових доступно',ru:'Новых доступно',de:'Neue verfügbar',en:'New available'})[lang] || 'New available'}: {visibleNewCount}</p>
      <div className="modal-actions">
        <button type="button" className="secondary-button" disabled={busy || !selected.length} onClick={()=>{selectionRevision.current += 1;setSelected([]);setPreview(null);setPreviewSelection(null)}}>{l[7]}</button>
        <button type="button" className="secondary-button" disabled={busy || !selectedNewCodes.length} onClick={()=>execute(async()=>{const revision=selectionRevision.current;const origin=workspaceId;const requestedCodes = [...selectedNewCodes];const result=await previewTemplateImport(getAccessToken(),workspaceId,requestedCodes);if(workspaceRef.current===origin && selectionRevision.current===revision){setPreview(result);setPreviewSelection(requestedCodes)}})}>{l[5]}</button>
      </div>
      {preview && <div className="settings-section">
        <p>{l[8]}: {(preview.alreadyImported || []).join(', ') || '—'}</p>
        <p>{l[9]}: {(preview.toImport || []).join(', ') || '—'}</p>
        <button className="primary-button" type="button" disabled={busy || !(preview.toImport || []).length || !previewSelection || previewSelection.join('|') !== selectedNewCodes.join('|')} onClick={()=>execute(async()=>{
          await importTemplates(getAccessToken(),workspaceId,previewSelection)
          await refresh(); if (workspaceRef.current === workspaceId) { setPreview(null); setPreviewSelection(null); setSelected([]); setNotice(l[18]) }
        })}>{l[6]}</button>
      </div>}
    </section>
    <section className="settings-section">
      <h2>{l[2]}</h2><p>{l[21]}</p>
      {!privateItems.length && <p>{l[12]}</p>}
      <div className="template-grid">
        {privateItems.map(item => <button key={item.id} type="button" className="secondary-button" onClick={()=>chooseEdit(item)}>
          {item.title} · {item.kind}
        </button>)}
      </div>
      {editing && ['TASK','REWARD'].includes(editing.kind) && <div className="modal-actions">
        <button type="button" className="secondary-button" disabled={busy} onClick={() => execute(async () => {
          const origin = workspaceId
          const itemId = editing.id
          const token = getAccessToken()
          const [draft, members, types] = await Promise.all([
            getTemplateCreationDraft(token, origin, itemId),
            getWorkspaceMembers(token, origin),
            getPointTypes(token, origin)
          ])
          if (workspaceRef.current === origin && editing?.id === itemId) { setCreationDraft(draft); setDraftText(JSON.stringify(draft.request, null, 2)); setDraftMembers(Array.isArray(members) ? members : []); setDraftPointTypes(Array.isArray(types) ? types.filter(type => type.active !== false) : []) }
        })}>{({uk:'Переглянути чернетку створення',ru:'Просмотреть черновик создания',de:'Erstellungsentwurf prüfen',en:'Preview creation draft'})[lang] || 'Preview creation draft'}</button>
      </div>}
      {creationDraft && editing && creationDraft.templateId === editing.id && <div className="settings-section">
        <h3>{({uk:'Чернетка (без створення завдання чи нагороди)',ru:'Черновик (без создания задачи или награды)',de:'Entwurf (ohne Aufgabe oder Belohnung anzulegen)',en:'Draft (no task or reward created)'})[lang] || 'Draft preview'}</h3>
        <p>{({uk:'Перевірте тривалість, валюту та призначення. Цей перегляд нічого не активує.',ru:'Проверьте длительность, валюту и назначение. Просмотр ничего не активирует.',de:'Bitte Dauer, Währung und Zuweisung prüfen. Diese Ansicht aktiviert nichts.',en:'Check duration, currency and assignment. This preview activates nothing.'})[lang] || 'Preview only'}</p>
        <TemplateDraftFieldsEditor kind={creationDraft.kind} value={draftText} onChange={setDraftText} language={lang} disabled={busy} members={draftMembers} pointTypes={draftPointTypes}/>
        <details><summary>{({uk:'Розширені параметри JSON',ru:'Расширенные параметры JSON',de:'Erweiterte JSON-Parameter',en:'Advanced JSON parameters'})[lang] || 'Advanced JSON'}</summary>
        <label style={{display:'block'}}>{({uk:'Параметри чернетки (JSON)',ru:'Параметры черновика (JSON)',de:'Entwurfsparameter (JSON)',en:'Draft parameters (JSON)'})[lang] || 'Draft JSON'}
          <textarea rows={10} value={draftText} onChange={e => setDraftText(e.target.value)} disabled={busy} spellCheck={false} style={{width:'100%'}}/>
        </label>
        </details>
        {!validDraft && <p className="error-message" role="alert">{({uk:'Потрібен коректний JSON-об’єкт (до 30 000 символів).',ru:'Нужен корректный JSON-объект (до 30 000 символов).',de:'Gültiges JSON-Objekt erforderlich (bis 30.000 Zeichen).',en:'A valid JSON object is required (up to 30,000 characters).'})[lang] || 'Invalid JSON'}</p>}
        {validDraft && draftWarnings.length > 0 && <div className="error-message" role="alert"><ul>{draftWarnings.map(message => <li key={message}>{message}</li>)}</ul></div>}
        {validDraft && draftWarnings.length === 0 && <p role="status">{({uk:'Попередні перевірки пройдені. Це не підтвердження Backend і не створення.',ru:'Предварительные проверки пройдены. Это не подтверждение Backend и не создание.',de:'Vorprüfungen bestanden. Keine Backend-Bestätigung und keine Erstellung.',en:'Preliminary checks passed. This is not Backend approval or creation.'})[lang] || 'Preliminary checks passed.'}</p>}
        <p role="status">{({uk:'Зміни лише у браузері. Для створення потрібне окреме підтвердження; активація тут недоступна.',ru:'Изменения только в браузере. Создание требует отдельного подтверждения; активация здесь недоступна.',de:'Änderungen nur im Browser. Erstellung erfordert separate Bestätigung; hier keine Aktivierung.',en:'Changes stay in your browser. Creation requires separate confirmation; activation is unavailable here.'})[lang] || 'Not activated'}</p>
      </div>}
      {editing && <form onSubmit={e=>{e.preventDefault();execute(async()=>{
        if (!validDefinition) throw new Error('INVALID_DEFINITION')
        if (availabilityError) throw new Error('INVALID_DEFINITION')
        await updatePrivateTemplate(getAccessToken(),workspaceId,editing.id,{title,definitionJson:definition,kind:editing.kind})
        await refresh();if (workspaceRef.current === workspaceId) setNotice(l[15])
      })}}>
        <h3>{l[16]}</h3>
        <label>{l[13]}<input required maxLength={150} value={title} onChange={e=>setTitle(e.target.value)}/></label>
        {availabilityError && <p className="error-message" role="alert">{availabilityError}</p>}
        <TemplateFieldsEditor kind={editing.kind} definition={definition} onChange={setDefinition} language={i18n.language.split('-')[0]} disabled={busy}/>
        <details>
          <summary>{l[14]} — JSON</summary>
          <label>{l[14]}<textarea required rows={8} value={definition} onChange={e=>setDefinition(e.target.value)} disabled={busy}/></label>
        </details>
        <div className="modal-actions">
          <button className="primary-button" disabled={busy || !validDefinition || Boolean(availabilityError) || !title.trim()} type="submit">{l[10]}</button>
          <button className="secondary-button" disabled={busy} type="button" onClick={()=>{if(window.confirm(l[17]))execute(async()=>{
            await deletePrivateTemplate(getAccessToken(),workspaceId,editing.id);await refresh();if (workspaceRef.current === workspaceId) setEditing(null)
          })}}>{l[11]}</button>
        </div>
      </form>}
    </section>
  </div>
}
