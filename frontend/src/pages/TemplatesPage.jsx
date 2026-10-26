import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { getTemplateCatalog, getPrivateTemplates, previewTemplateImport, importTemplates, updatePrivateTemplate, deletePrivateTemplate } from '../api/templateApi'

const texts = {
  uk: ['Бібліотека шаблонів','Глобальні рекомендації','Мої шаблони','Виберіть потрібні шаблони. Імпорт додає їх у бібліотеку, а не створює активні завдання чи нагороди.','Зараз глобальних рекомендацій немає.','Переглянути імпорт','Імпортувати вибране','Очистити вибір','Уже в бібліотеці','Буде додано','Зберегти','Видалити','Немає особистих шаблонів.','Назва','Визначення (JSON)','Збережено','Виберіть шаблон для редагування','Видалити цей шаблон із бібліотеки?','Шаблони імпортовано','Не вибрано жодного шаблону.','Тільки адміністратор сім’ї може змінювати бібліотеку.','Повторно використовувати їх можна буде через окреме створення активного об’єкта.'],
  de: ['Vorlagenbibliothek','Globale Vorschläge','Meine Vorlagen','Vorlagen einzeln auswählen. Der Import speichert sie nur in der Bibliothek und erstellt keine aktiven Aufgaben oder Belohnungen.','Noch keine globalen Vorschläge.','Import prüfen','Auswahl importieren','Auswahl löschen','Bereits vorhanden','Wird hinzugefügt','Speichern','Löschen','Noch keine privaten Vorlagen.','Titel','Definition (JSON)','Gespeichert','Vorlage zum Bearbeiten auswählen','Diese Vorlage aus der Bibliothek löschen?','Vorlagen importiert','Keine Vorlagen ausgewählt.','Nur Familienadministratoren dürfen Vorlagen verwalten.','Aktive Aufgaben und Belohnungen werden später separat erstellt.'],
  en: ['Template library','Global suggestions','My templates','Select templates individually. Import saves them to your private library, not as active tasks or rewards.','No global suggestions yet.','Preview import','Import selected','Clear selection','Already imported','To be imported','Save','Delete','No private templates yet.','Title','Definition (JSON)','Saved','Select a template to edit','Delete this template from the library?','Templates imported','No templates selected.','Only workspace administrators can manage templates.','Create active tasks and rewards separately later.'],
  ru: ['Библиотека шаблонов','Глобальные рекомендации','Мои шаблоны','Выбирайте шаблоны отдельно. Импорт добавляет их в библиотеку без создания активных задач и наград.','Пока нет глобальных рекомендаций.','Предпросмотр импорта','Импортировать выбранные','Очистить выбор','Уже импортированы','Будут добавлены','Сохранить','Удалить','Пока нет личных шаблонов.','Название','Определение (JSON)','Сохранено','Выберите шаблон для редактирования','Удалить шаблон из библиотеки?','Шаблоны импортированы','Шаблоны не выбраны.','Только администратор семьи может менять библиотеку.','Активные задачи и награды создаются отдельно.']
}
export default function TemplatesPage() {
  const { i18n } = useTranslation()
  const l = texts[i18n.language.split('-')[0]] || texts.uk
  const { currentUser, getAccessToken } = useAuth()
  const workspaceId = currentUser?.workspaceId
  const [catalog, setCatalog] = useState({ templates: [], packs: [] })
  const [privateItems, setPrivateItems] = useState([])
  const [selected, setSelected] = useState([])
  const [preview, setPreview] = useState(null)
  const [editing, setEditing] = useState(null)
  const [title, setTitle] = useState('')
  const [definition, setDefinition] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  useEffect(() => {
    let alive = true
    setCatalog({ templates: [], packs: [] }); setPrivateItems([]); setSelected([]); setPreview(null); setEditing(null); setError('')
    if (!workspaceId) return () => { alive = false }
    Promise.all([getTemplateCatalog(getAccessToken()), getPrivateTemplates(getAccessToken(), workspaceId)])
      .then(([a,b]) => { if (alive) { setCatalog(a || { templates: [], packs: [] }); setPrivateItems(Array.isArray(b) ? b : []) } })
      .catch(e => { if (alive) setError(e.message) })
    return () => { alive = false }
  }, [workspaceId, getAccessToken])
  const toggle = code => { setSelected(v => v.includes(code) ? v.filter(x=>x!==code) : [...v,code]); setPreview(null) }
  const execute = async action => {
    if (busy) return
    setBusy(true); setError(''); setNotice('')
    try { await action() } catch(e) { setError(e.message) } finally { setBusy(false) }
  }
  const refresh = async () => setPrivateItems(await getPrivateTemplates(getAccessToken(), workspaceId))
  const chooseEdit = item => { setEditing(item); setTitle(item.title); setDefinition(item.definitionJson); setError('') }
  if (!workspaceId) return <div className="page-loading">...</div>
  return <div className="templates-page">
    <h1>{l[0]}</h1>
    <p>{l[3]}</p>
    {error && <div className="error-message" role="alert">{error}</div>}
    {notice && <p role="status">{notice}</p>}
    <section className="settings-section">
      <h2>{l[1]}</h2>
      {!(catalog.templates || []).length && <p>{l[4]}</p>}
      <div className="template-grid">
        {(catalog.templates || []).map(item => <label key={item.code} className="template-card">
          <input type="checkbox" checked={selected.includes(item.code)} onChange={()=>toggle(item.code)} disabled={busy}/>
          <span><strong>{item.title?.[i18n.language.split('-')[0]] || item.title?.en || item.code}</strong> <small>{item.kind}</small></span>
        </label>)}
      </div>
      <p>{selected.length} / {(catalog.templates || []).length}</p>
      <div className="modal-actions">
        <button type="button" className="secondary-button" disabled={busy || !selected.length} onClick={()=>{setSelected([]);setPreview(null)}}>{l[7]}</button>
        <button type="button" className="secondary-button" disabled={busy || !selected.length} onClick={()=>execute(async()=>setPreview(await previewTemplateImport(getAccessToken(),workspaceId,selected)))}>{l[5]}</button>
      </div>
      {preview && <div className="settings-section">
        <p>{l[8]}: {(preview.alreadyImported || []).join(', ') || '—'}</p>
        <p>{l[9]}: {(preview.toImport || []).join(', ') || '—'}</p>
        <button className="primary-button" type="button" disabled={busy || !(preview.toImport || []).length} onClick={()=>execute(async()=>{
          await importTemplates(getAccessToken(),workspaceId,selected)
          await refresh(); setPreview(null);setSelected([]);setNotice(l[17])
        })}>{l[6]}</button>
      </div>}
    </section>
    <section className="settings-section">
      <h2>{l[2]}</h2><p>{l[20]}</p>
      {!privateItems.length && <p>{l[12]}</p>}
      <div className="template-grid">
        {privateItems.map(item => <button key={item.id} type="button" className="secondary-button" onClick={()=>chooseEdit(item)}>
          {item.title} · {item.kind}
        </button>)}
      </div>
      {editing && <form onSubmit={e=>{e.preventDefault();execute(async()=>{
        JSON.parse(definition)
        await updatePrivateTemplate(getAccessToken(),workspaceId,editing.id,{title,definitionJson:definition,kind:editing.kind})
        await refresh();setNotice(l[14])
      })}}>
        <h3>{l[15]}</h3>
        <label>{l[13]}<input required maxLength={150} value={title} onChange={e=>setTitle(e.target.value)}/></label>
        <label>{l[14]}<textarea required rows={8} value={definition} onChange={e=>setDefinition(e.target.value)}/></label>
        <div className="modal-actions">
          <button className="primary-button" disabled={busy} type="submit">{l[10]}</button>
          <button className="secondary-button" disabled={busy} type="button" onClick={()=>{if(window.confirm(l[16]))execute(async()=>{
            await deletePrivateTemplate(getAccessToken(),workspaceId,editing.id);await refresh();setEditing(null)
          })}}>{l[11]}</button>
        </div>
      </form>}
    </section>
  </div>
}
