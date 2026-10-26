import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import TemplateFieldsEditor from '../components/TemplateFieldsEditor'
import { getTemplateCatalog, getPrivateTemplates, previewTemplateImport, importTemplates, updatePrivateTemplate, deletePrivateTemplate } from '../api/templateApi'

const texts = {
  uk: ['Бібліотека шаблонів','Глобальні рекомендації','Мої шаблони','Виберіть потрібні шаблони. Імпорт додає їх у бібліотеку, а не створює активні завдання чи нагороди.','Зараз глобальних рекомендацій немає.','Переглянути імпорт','Імпортувати вибране','Очистити вибір','Уже в бібліотеці','Буде додано','Зберегти','Видалити','Немає особистих шаблонів.','Назва','Визначення (JSON)','Збережено','Виберіть шаблон для редагування','Видалити цей шаблон із бібліотеки?','Шаблони імпортовано','Не вибрано жодного шаблону.','Тільки адміністратор сім’ї може змінювати бібліотеку.','Повторно використовувати їх можна буде через окреме створення активного об’єкта.'],
  de: ['Vorlagenbibliothek','Globale Vorschläge','Meine Vorlagen','Vorlagen einzeln auswählen. Der Import speichert sie nur in der Bibliothek und erstellt keine aktiven Aufgaben oder Belohnungen.','Noch keine globalen Vorschläge.','Import prüfen','Auswahl importieren','Auswahl löschen','Bereits vorhanden','Wird hinzugefügt','Speichern','Löschen','Noch keine privaten Vorlagen.','Titel','Definition (JSON)','Gespeichert','Vorlage zum Bearbeiten auswählen','Diese Vorlage aus der Bibliothek löschen?','Vorlagen importiert','Keine Vorlagen ausgewählt.','Nur Familienadministratoren dürfen Vorlagen verwalten.','Aktive Aufgaben und Belohnungen werden später separat erstellt.'],
  en: ['Template library','Global suggestions','My templates','Select templates individually. Import saves them to your private library, not as active tasks or rewards.','No global suggestions yet.','Preview import','Import selected','Clear selection','Already imported','To be imported','Save','Delete','No private templates yet.','Title','Definition (JSON)','Saved','Select a template to edit','Delete this template from the library?','Templates imported','No templates selected.','Only workspace administrators can manage templates.','Create active tasks and rewards separately later.'],
  ru: ['Библиотека шаблонов','Глобальные рекомендации','Мои шаблоны','Выбирайте шаблоны отдельно. Импорт добавляет их в библиотеку без создания активных задач и наград.','Пока нет глобальных рекомендаций.','Предпросмотр импорта','Импортировать выбранные','Очистить выбор','Уже импортированы','Будут добавлены','Сохранить','Удалить','Пока нет личных шаблонов.','Название','Определение (JSON)','Сохранено','Выберите шаблон для редактирования','Удалить шаблон из библиотеки?','Шаблоны импортированы','Шаблоны не выбраны.','Только администратор семьи может менять библиотеку.','Активные задачи и награды создаются отдельно.']
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
    setCatalog({ templates: [], packs: [] }); setPrivateItems([]); setSelected([]); setPackFilter('ALL'); setPreview(null); setPreviewSelection(null); setEditing(null); setError('')
    if (!workspaceId) return () => { alive = false }
    Promise.all([getTemplateCatalog(getAccessToken()), getPrivateTemplates(getAccessToken(), workspaceId)])
      .then(([a,b]) => { if (alive) { setCatalog(a || { templates: [], packs: [] }); setPrivateItems(Array.isArray(b) ? b : []) } })
      .catch(e => { if (alive) setError(e.message) })
    return () => { alive = false }
  }, [workspaceId, getAccessToken])
  const importedCodes = new Set(privateItems.map(item => item.catalogCode).filter(Boolean))
  const visibleNewCount = (catalog.templates || []).filter(item => !importedCodes.has(item.code)).length
  const packItems = packFilter === 'ALL'
    ? (catalog.templates || [])
    : (catalog.templates || []).filter(item => (catalog.packs || []).find(pack => pack.code === packFilter)?.templateCodes?.includes(item.code))
  const selectVisible = checked => {
    selectionRevision.current += 1
    const codes = packItems.map(item => item.code)
    setSelected(current => checked ? [...new Set([...current, ...codes])] : current.filter(code => !codes.includes(code)))
    setPreview(null)
    setPreviewSelection(null)
  }
  const toggle = code => { selectionRevision.current += 1; setSelected(v => v.includes(code) ? v.filter(x=>x!==code) : [...v,code]); setPreview(null); setPreviewSelection(null) }
  const execute = async action => {
    if (actionLock.current) return
    actionLock.current = true
    const originWorkspace = workspaceId
    setBusy(true); setError(''); setNotice('')
    try { await action() } catch(e) { if (workspaceRef.current === originWorkspace) setError(e.message) } finally {
      actionLock.current = false
      if (workspaceRef.current === originWorkspace) setBusy(false)
    }
  }
  const refresh = async () => { const id = workspaceId; const items = await getPrivateTemplates(getAccessToken(), id); if (workspaceRef.current === id) setPrivateItems(items) }
  const chooseEdit = item => { setEditing(item); setTitle(item.title); setDefinition(item.definitionJson); setError('') }
  const validDefinition = (() => { try { return definition.length <= 30000 && !Array.isArray(JSON.parse(definition)) && JSON.parse(definition) !== null && typeof JSON.parse(definition) === 'object' } catch { return false } })()
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
    {error && <div className="error-message" role="alert">{error}</div>}
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
          <input type="checkbox" checked={selected.includes(item.code)} onChange={()=>toggle(item.code)} disabled={busy}/>
          <span><strong>{item.title?.[i18n.language.split('-')[0]] || item.title?.en || item.code}</strong> <small>{item.kind}</small>{importedCodes.has(item.code) && <small style={{display:'block', opacity:.8}}>{({uk:'Уже в бібліотеці',ru:'Уже в библиотеке',de:'Bereits in der Bibliothek',en:'Already in library'})[lang] || 'Already in library'}</small>}</span>
        </label>)}
      </div>
      <p>{selected.length} / {(catalog.templates || []).length} · {({uk:'Нових доступно',ru:'Новых доступно',de:'Neue verfügbar',en:'New available'})[lang] || 'New available'}: {visibleNewCount}</p>
      <div className="modal-actions">
        <button type="button" className="secondary-button" disabled={busy || !selected.length} onClick={()=>{selectionRevision.current += 1;setSelected([]);setPreview(null);setPreviewSelection(null)}}>{l[7]}</button>
        <button type="button" className="secondary-button" disabled={busy || !selected.length || !selected.some(code => !importedCodes.has(code))} onClick={()=>execute(async()=>{const revision=selectionRevision.current;const origin=workspaceId;const result=await previewTemplateImport(getAccessToken(),workspaceId,selected);if(workspaceRef.current===origin && selectionRevision.current===revision){setPreview(result);setPreviewSelection([...selected])}})}>{l[5]}</button>
      </div>
      {preview && <div className="settings-section">
        <p>{l[8]}: {(preview.alreadyImported || []).join(', ') || '—'}</p>
        <p>{l[9]}: {(preview.toImport || []).join(', ') || '—'}</p>
        <button className="primary-button" type="button" disabled={busy || !(preview.toImport || []).length || !previewSelection || previewSelection.join('|') !== selected.join('|')} onClick={()=>execute(async()=>{
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
      {editing && <form onSubmit={e=>{e.preventDefault();execute(async()=>{
        if (!validDefinition) throw new Error('Template definition must be a valid JSON object (max 30,000 characters)')
        await updatePrivateTemplate(getAccessToken(),workspaceId,editing.id,{title,definitionJson:definition,kind:editing.kind})
        await refresh();setNotice(l[15])
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
            await deletePrivateTemplate(getAccessToken(),workspaceId,editing.id);await refresh();setEditing(null)
          })}}>{l[11]}</button>
        </div>
      </form>}
    </section>
  </div>
}
