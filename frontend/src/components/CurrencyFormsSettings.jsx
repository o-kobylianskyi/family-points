import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getPointTypes, getPointNameForms, savePointNameForms } from '../api/economyApi'
import { currencyQuantity } from '../utils/formatCurrencyAmount'

const languages = [['uk', 'Українська'], ['ru', 'Русский'], ['en', 'English'], ['de', 'Deutsch']]
const empty = { one: '', few: '', many: '' }

export default function CurrencyFormsSettings({ token, workspaceId, canManage }) {
  const { t, i18n } = useTranslation()
  const [types, setTypes] = useState([])
  const [selected, setSelected] = useState(null)
  const [language, setLanguage] = useState(i18n.language.split('-')[0])
  const [forms, setForms] = useState([])
  const [draft, setDraft] = useState(empty)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let live = true
    getPointTypes(token, workspaceId).then(data => { if (live) setTypes(data) })
      .catch(e => { if (live) setError(e.message) })
    return () => { live = false }
  }, [token, workspaceId])

  useEffect(() => {
    if (!selected) return
    let live = true
    getPointNameForms(token, workspaceId, selected.id)
      .then(data => { if (live) setForms(data) })
      .catch(e => { if (live) setError(e.message) })
    return () => { live = false }
  }, [token, workspaceId, selected?.id])

  useEffect(() => {
    const form = forms.find(f => f.language === language)
    setDraft(form ? { one: form.one, few: form.few, many: form.many } : empty)
  }, [forms, language, selected?.id])

  const copy = {
    uk: ['Відмінювання валют', 'Форми назв валют за кількістю', 'Зберегти', 'Назва для 1', 'Назва для 2–4', 'Назва для 5+', 'Збережено'],
    ru: ['Склонение валют', 'Формы названий валют по количеству', 'Сохранить', 'Название для 1', 'Название для 2–4', 'Название для 5+', 'Сохранено'],
    en: ['Currency word forms', 'Quantity-sensitive currency names', 'Save', 'Name for 1', 'Name for 2–4', 'Name for 5+', 'Saved'],
    de: ['Währungsformen', 'Wortformen je nach Menge', 'Speichern', 'Name für 1', 'Name für 2–4', 'Name für 5+', 'Gespeichert'],
  }[i18n.language.split('-')[0]] || ['Currency word forms', '', 'Save', 'One', 'Few', 'Many', 'Saved']

  async function save(event) {
    event.preventDefault()
    if (!selected || !canManage || Object.values(draft).some(v => !v.trim())) return
    setBusy(true); setError(''); setNotice('')
    try {
      await savePointNameForms(token, workspaceId, selected.id, language, draft)
      setForms(await getPointNameForms(token, workspaceId, selected.id))
      setNotice(copy[6])
    } catch (e) { setError(e.message) }
    finally { setBusy(false) }
  }

  const sample = [1, 2, 5, 11, 21]
  return <section className="settings-section">
    <h2>{copy[0]}</h2><p>{copy[1]}</p>
    {error && <p role="alert">{error}</p>}{notice && <p>{notice}</p>}
    <div className="settings-form">
      <select value={selected?.id || ''} onChange={e => {
        setForms([]); setSelected(types.find(type => type.id === Number(e.target.value)) || null)
      }}>
        <option value="">—</option>
        {types.map(type => <option key={type.id} value={type.id}>{type.name} ({type.code})</option>)}
      </select>
      {selected && <form onSubmit={save}>
        <select value={language} onChange={e => setLanguage(e.target.value)}>
          {languages.map(([code, label]) => <option key={code} value={code}>{label}</option>)}
        </select>
        {['one', 'few', 'many'].map((key, i) =>
          <label key={key}>{copy[i + 3]}
            <input maxLength={100} required value={draft[key]} disabled={!canManage}
              onChange={e => setDraft(v => ({ ...v, [key]: e.target.value }))} />
          </label>)}
        <div className="account-meta">
          {sample.map(n => <span key={n}>{currencyQuantity(n, {
            code: selected.code, name: selected.name,
            language, forms: [{ language, ...draft }],
          })} · </span>)}
        </div>
        {canManage && <button type="submit" disabled={busy}>{copy[2]}</button>}
      </form>}
    </div>
  </section>
}
