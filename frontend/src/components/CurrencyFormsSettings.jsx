import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getPointTypes, getPointNameForms, savePointNameForms, createPointType, getAllPointTypes, updatePointType } from '../api/economyApi'
import { currencyQuantity } from '../utils/formatCurrencyAmount'

const languages = [['uk', 'Українська'], ['ru', 'Русский'], ['en', 'English'], ['de', 'Deutsch']]
const blank = { one: '', few: '', many: '' }

const defaults = {
  POINTS: {
    uk: ['бал', 'бали', 'балів'], ru: ['балл', 'балла', 'баллов'],
    en: ['point', 'points', 'points'], de: ['Punkt', 'Punkte', 'Punkte'],
  },
  COPPER: {
    uk: ['мідна монета', 'мідні монети', 'мідних монет'], ru: ['медная монета', 'медные монеты', 'медных монет'],
    en: ['copper coin', 'copper coins', 'copper coins'], de: ['Kupfermünze', 'Kupfermünzen', 'Kupfermünzen'],
  },
  SILVER: {
    uk: ['срібна монета', 'срібні монети', 'срібних монет'], ru: ['серебряная монета', 'серебряные монеты', 'серебряных монет'],
    en: ['silver coin', 'silver coins', 'silver coins'], de: ['Silbermünze', 'Silbermünzen', 'Silbermünzen'],
  },
  GOLD: {
    uk: ['золота монета', 'золоті монети', 'золотих монет'], ru: ['золотая монета', 'золотые монеты', 'золотых монет'],
    en: ['gold coin', 'gold coins', 'gold coins'], de: ['Goldmünze', 'Goldmünzen', 'Goldmünzen'],
  },
}

const labels = {
  uk: { heading: 'Відмінювання валют', intro: 'Задайте назви для різної кількості та мови.', currency: 'Валюта', language: 'Мова назв', choose: 'Оберіть валюту', one: 'Для 1', few: 'Для 2–4', many: 'Для 5, 11–14 та інших', save: 'Зберегти', saved: 'Збережено на сервері', recommended: 'Типові форми (не збережені)', unset: 'Не налаштовано', preview: 'Попередній перегляд', success: 'Форми збережено', loading: 'Завантаження…', retry: 'Не вдалося завантажити збережені форми.', noAccess: 'Для редагування потрібне право керування економікою.' },
  ru: { heading: 'Склонение валют', intro: 'Настройте названия для разных количеств и языков.', currency: 'Валюта', language: 'Язык названий', choose: 'Выберите валюту', one: 'Для 1', few: 'Для 2–4', many: 'Для 5, 11–14 и других', save: 'Сохранить', saved: 'Сохранено на сервере', recommended: 'Типовые формы (не сохранены)', unset: 'Не настроено', preview: 'Предпросмотр', success: 'Формы сохранены', loading: 'Загрузка…', retry: 'Не удалось загрузить сохранённые формы.', noAccess: 'Для изменения необходимо право управления экономикой.' },
  en: { heading: 'Currency word forms', intro: 'Configure quantity forms for each currency and language.', currency: 'Currency', language: 'Form language', choose: 'Select currency', one: 'For 1', few: 'For 2–4', many: 'For 5, 11–14 and others', save: 'Save', saved: 'Saved on server', recommended: 'Suggested forms (not saved)', unset: 'Not configured', preview: 'Preview', success: 'Forms saved', loading: 'Loading…', retry: 'Could not load saved forms.', noAccess: 'Economy management permission is needed to edit.' },
  de: { heading: 'Währungsformen', intro: 'Wortformen je Währung, Anzahl und Sprache verwalten.', currency: 'Währung', language: 'Sprache der Formen', choose: 'Währung wählen', one: 'Für 1', few: 'Für 2–4', many: 'Für 5, 11–14 und andere', save: 'Speichern', saved: 'Auf Server gespeichert', recommended: 'Vorschlag (nicht gespeichert)', unset: 'Nicht konfiguriert', preview: 'Vorschau', success: 'Formen gespeichert', loading: 'Laden…', retry: 'Gespeicherte Formen konnten nicht geladen werden.', noAccess: 'Zum Bearbeiten werden Rechte zur Wirtschaftsverwaltung benötigt.' },
}

const currencyNames = {
  POINTS: { uk: 'Бали', ru: 'Баллы', en: 'Points', de: 'Punkte' },
  COPPER: { uk: 'Мідь', ru: 'Медь', en: 'Copper', de: 'Kupfer' },
  SILVER: { uk: 'Срібло', ru: 'Серебро', en: 'Silver', de: 'Silber' },
  GOLD: { uk: 'Золото', ru: 'Золото', en: 'Gold', de: 'Gold' },
}
const localizedCurrency = (type, lang) => currencyNames[type.code?.toUpperCase()]?.[lang] || type.name

export default function CurrencyFormsSettings({ token, workspaceId, canManage }) {
  const { i18n } = useTranslation()
  const uiLang = i18n.language.split('-')[0]
  const l = labels[uiLang] || labels.uk
  const [types, setTypes] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [language, setLanguage] = useState(uiLang in labels ? uiLang : 'uk')
  const [forms, setForms] = useState([])
  const [draft, setDraft] = useState(blank)
  const [loading, setLoading] = useState(false)
  const [loaded, setLoaded] = useState(false)
  const [dirty, setDirty] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const [creating, setCreating] = useState(false)
  const [editName, setEditName] = useState('')
  const [editActive, setEditActive] = useState(true)
  const [editing, setEditing] = useState(false)
  const [newCurrencyName, setNewCurrencyName] = useState('')
  const [newCurrencyCode, setNewCurrencyCode] = useState('')
  const selected = types.find(type => String(type.id) === String(selectedId))
  const current = forms.find(form => form.language === language)
  const suggested = defaults[selected?.code?.toUpperCase()]?.[language]
  const status = current ? l.saved : suggested ? l.recommended : l.unset

  useEffect(() => {
    let alive = true
    setSelectedId('');
    (canManage ? getAllPointTypes(token, workspaceId) : getPointTypes(token, workspaceId))
      .then(data => { if (alive) { setTypes(data); if (data.length) setSelectedId(String(data[0].id)) } })
      .catch(e => { if (alive) setError(e.message) })
    return () => { alive = false }
  }, [token, workspaceId, canManage])

  useEffect(() => {
    let alive = true
    setForms([])
    setLoaded(false)
    setLoading(Boolean(selectedId))
    setError('')
    setNotice('')
    setDirty(false)
    if (selectedId) {
      getPointNameForms(token, workspaceId, selectedId)
        .then(data => { if (alive) { setForms(data); setLoaded(true) } })
        .catch(e => { if (alive) setError(e.message || l.retry) })
        .finally(() => { if (alive) setLoading(false) })
    }
    return () => { alive = false }
  }, [token, workspaceId, selectedId])

  useEffect(() => {
    const saved = forms.find(form => form.language === language)
    const preset = defaults[selected?.code?.toUpperCase()]?.[language]
    setDraft(saved ? { one: saved.one, few: saved.few, many: saved.many }
      : preset ? { one: preset[0], few: preset[1], many: preset[2] } : blank)
    setDirty(false)
  }, [forms, selectedId, language])

  useEffect(() => {
    setEditName(selected?.name || '')
    setEditActive(selected?.active ?? true)
  }, [selected?.id, selected?.name, selected?.active])

  async function saveCurrency(event) {
    event.preventDefault()
    if (!canManage || !selected || !editName.trim() || editing) return
    setEditing(true); setError(''); setNotice('')
    try {
      const updated = await updatePointType(token, workspaceId, selected.id, {
        name: editName.trim(), active: editActive, sortOrder: selected.sortOrder,
      })
      setTypes(previous => previous.map(type => type.id === updated.id ? updated : type))
      setNotice(uiLang === 'uk' ? 'Валюту оновлено' : uiLang === 'ru' ? 'Валюта обновлена' : uiLang === 'de' ? 'Währung aktualisiert' : 'Currency updated')
    } catch(e) { setError(e.message) }
    finally { setEditing(false) }
  }

  async function addCurrency(event) {
    event.preventDefault()
    if (!canManage || busy || !newCurrencyName.trim() || !/^[A-Z][A-Z0-9_]{1,49}$/.test(newCurrencyCode.trim().toUpperCase())) return
    setCreating(true); setError(''); setNotice('')
    try {
      const created = await createPointType(token, workspaceId, {
        name: newCurrencyName.trim(),
        code: newCurrencyCode.trim().toUpperCase(),
        sortOrder: Math.max(100, ...types.map(type => type.sortOrder + 10)),
      })
      const refreshed = await getPointTypes(token, workspaceId)
      setTypes(refreshed)
      setSelectedId(String(created.id))
      setNewCurrencyCode('')
      setNewCurrencyName('')
      setNotice('Валюту створено. Тепер налаштуйте її відмінювання.')
    } catch (e) { setError(e.message) }
    finally { setCreating(false) }
  }

  async function save(event) {
    event.preventDefault()
    if (!selected || !canManage || !loaded || Object.values(draft).some(value => !value.trim())) return
    setBusy(true); setError(''); setNotice('')
    try {
      await savePointNameForms(token, workspaceId, selected.id, language, draft)
      const updated = await getPointNameForms(token, workspaceId, selected.id)
      setForms(updated)
      setDirty(false)
      setNotice(l.success)
    } catch (e) { setError(e.message) }
    finally { setBusy(false) }
  }

  return <section className="settings-section currency-forms-section">
    <h2>{l.heading}</h2>
    <p>{l.intro}</p>
    {error && <div className="error-message" role="alert">{error}</div>}
    {notice && <div className="settings-success" role="status">{notice}</div>}
    {canManage && <form onSubmit={addCurrency} className="currency-forms-editor" style={{ marginBottom: '1rem' }}>
      <h3>{uiLang === 'ru' ? 'Добавить валюту' : uiLang === 'de' ? 'Währung hinzufügen' : uiLang === 'en' ? 'Add currency' : 'Додати валюту'}</h3>
      <div className="currency-forms-fields">
        <label><span>{uiLang === 'ru' ? 'Название' : uiLang === 'de' ? 'Name' : uiLang === 'en' ? 'Name' : 'Назва'}</span>
          <input required maxLength={100} value={newCurrencyName} disabled={creating}
            onChange={e => setNewCurrencyName(e.target.value)} placeholder="Кристали" />
        </label>
        <label><span>{uiLang === 'ru' ? 'Код (латиница)' : uiLang === 'de' ? 'Code (lateinisch)' : uiLang === 'en' ? 'Code (Latin letters)' : 'Код (латиниця)'}</span>
          <input required maxLength={50} pattern="[A-Za-z][A-Za-z0-9_]{1,49}" value={newCurrencyCode} disabled={creating}
            onChange={e => setNewCurrencyCode(e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, ''))} placeholder="CRYSTALS" />
        </label>
      </div>
      <button className="primary-button" type="submit" disabled={creating || busy || !newCurrencyName.trim() || !/^[A-Z][A-Z0-9_]{1,49}$/.test(newCurrencyCode)}>
        {creating ? l.loading : uiLang === 'ru' ? 'Создать валюту' : uiLang === 'de' ? 'Währung erstellen' : uiLang === 'en' ? 'Create currency' : 'Створити валюту'}
      </button>
    </form>}
    <div className="currency-forms-layout">
      <div className="currency-forms-picker">
        <label htmlFor="currency-type">{l.currency}</label>
        <select id="currency-type" value={selectedId} onChange={e => setSelectedId(e.target.value)}>
          {types.length === 0 && <option value="">{l.choose}</option>}
          {types.map(type => <option key={type.id} value={type.id}>
            {localizedCurrency(type, uiLang)} ({type.code})
          </option>)}
        </select>
        {selected && !selected.active && <div className="currency-forms-status">{uiLang === 'uk' ? 'Неактивна' : uiLang === 'ru' ? 'Неактивна' : uiLang === 'de' ? 'Inaktiv' : 'Inactive'}</div>}
        {selected && <div className="currency-forms-status">
          {loading ? l.loading : loaded ? status : l.retry}
        </div>}
      </div>
      {selected && canManage && <form className="currency-forms-editor" onSubmit={saveCurrency}>
        <h3>{uiLang === 'uk' ? 'Налаштування валюти' : uiLang === 'ru' ? 'Настройки валюты' : uiLang === 'de' ? 'Währungseinstellungen' : 'Currency settings'}</h3>
        <div className="currency-forms-fields">
          <label><span>{uiLang === 'uk' ? 'Назва' : uiLang === 'ru' ? 'Название' : uiLang === 'de' ? 'Name' : 'Name'}</span>
            <input maxLength={100} required disabled={editing} value={editName} onChange={e => setEditName(e.target.value)} />
          </label>
          <label><span>{uiLang === 'uk' ? 'Активна' : uiLang === 'ru' ? 'Активна' : uiLang === 'de' ? 'Aktiv' : 'Active'}</span>
            <input type="checkbox" disabled={editing} checked={editActive} onChange={e => setEditActive(e.target.checked)} />
          </label>
        </div>
        <button className="primary-button" type="submit" disabled={editing || !editName.trim() || (editName === selected.name && editActive === selected.active)}>
          {editing ? l.loading : l.save}
        </button>
      </form>}
      {selected && <form className="currency-forms-editor" onSubmit={save}>
        <label className="currency-forms-language" htmlFor="currency-language">{l.language}
          <select id="currency-language" value={language} onChange={e => setLanguage(e.target.value)} disabled={busy}>
            {languages.map(([code, label]) => <option key={code} value={code}>{label}</option>)}
          </select>
        </label>
        <div className="currency-forms-fields">
          {['one', 'few', 'many'].map((key, i) => <label key={key}>
            <span>{l[key]}</span>
            <input maxLength={100} required value={draft[key]} disabled={!canManage || !loaded || busy}
              onChange={e => { setDraft(prev => ({ ...prev, [key]: e.target.value })); setDirty(true); setNotice('') }} />
          </label>)}
        </div>
        <div className="currency-forms-preview">
          <strong>{l.preview}</strong>
          <div>{[1, 2, 5, 11, 21].map(n => <span key={n}>{currencyQuantity(n, {
            code: selected.code, name: localizedCurrency(selected, language),
            language, forms: [{ language, ...draft }],
          })}</span>)}</div>
        </div>
        {canManage
          ? <button className="primary-button currency-forms-save" type="submit"
              disabled={busy || !loaded || Object.values(draft).some(value => !value.trim())}>
              {busy ? l.loading : l.save}{dirty ? ' *' : ''}
            </button>
          : <p className="currency-forms-status">{l.noAccess}</p>}
      </form>}
    </div>
  </section>
}
