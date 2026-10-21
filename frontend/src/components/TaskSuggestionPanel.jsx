import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { API_URL } from '../api/baseUrl'

export default function TaskSuggestionPanel({ onClose }) {
  const { t } = useTranslation()
  const { currentUser, getAccessToken } = useAuth()
  const [items, setItems] = useState([])
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const isAdultManager = currentUser.memberType !== 'CHILD' &&
    (currentUser.permissions?.includes('MANAGE_TASKS') ||
     currentUser.permissions?.includes('APPROVE_TASKS') ||
     currentUser.permissions?.includes('ADMIN_OVERRIDE'))
  const api = `${API_URL}/workspaces/${currentUser.workspaceId}/task-suggestions`
  const headers = () => ({ 'Authorization': `Bearer ${getAccessToken()}`, 'Content-Type': 'application/json' })

  const load = async () => {
    const response = await fetch(api, { headers: headers() })
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    setItems(await response.json())
  }
  useEffect(() => { load().catch(e => setError(e.message)) }, [currentUser.workspaceId])

  const submit = async event => {
    event.preventDefault()
    if (!title.trim()) return
    setBusy(true); setError(''); setMessage('')
    try {
      const response = await fetch(api, { method: 'POST', headers: headers(), body: JSON.stringify({ title: title.trim(), description: description.trim() }) })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      setTitle(''); setDescription(''); setMessage(t('taskSuggestionUi.sent'))
      await load()
    } catch (e) { setError(`${t('taskSuggestionUi.error')} · ${e.message}`) }
    finally { setBusy(false) }
  }
  const decide = async (id, decision) => {
    setBusy(true); setError('')
    try {
      const response = await fetch(`${api}/${id}/decision?decision=${decision}`, { method: 'POST', headers: headers() })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      await load()
    } catch (e) { setError(`${t('taskSuggestionUi.error')} · ${e.message}`) }
    finally { setBusy(false) }
  }
  return <div className="modal-overlay" role="presentation">
    <div className="modal-content" role="dialog" aria-modal="true" aria-label={t('taskSuggestionUi.suggestTitle')} style={{ maxWidth: 580, padding: 24, maxHeight: '85vh', overflowY: 'auto' }}>
      <h2>{t('taskSuggestionUi.suggestTitle')}</h2>
      <p>{t('taskSuggestionUi.suggestDescription')}</p>
      <form onSubmit={submit}>
        <label>{t('taskSuggestionUi.title')}<input required maxLength={150} value={title} onChange={e => setTitle(e.target.value)} /></label>
        <label>{t('taskSuggestionUi.description')}<textarea maxLength={1000} value={description} onChange={e => setDescription(e.target.value)} /></label>
        <button type="submit" disabled={busy}>{t('taskSuggestionUi.submit')}</button>
      </form>
      {message && <p role="status">{message}</p>}
      {error && <p className="error-message" role="alert">{error}</p>}
      <h3>{t('taskSuggestionUi.requests')}</h3>
      {items.length === 0 && <p>{t('taskSuggestionUi.noRequests')}</p>}
      {items.map(item => <div className="card" key={item.id} style={{ marginBottom: 8, padding: 12 }}>
        <strong>{item.title}</strong><p>{item.description}</p>
        <small>{t('taskSuggestionUi.' + item.status.toLowerCase())}</small>
        {isAdultManager && item.status === 'PENDING' && <div style={{ display: 'flex', gap: 8 }}>
          <button type="button" disabled={busy} onClick={() => decide(item.id, 'APPROVED')}>{t('taskSuggestionUi.approve')}</button>
          <button type="button" disabled={busy} onClick={() => decide(item.id, 'REJECTED')}>{t('taskSuggestionUi.reject')}</button>
        </div>}
      </div>)}
      {isAdultManager && <p>{t('taskSuggestionUi.approvalReminder')}</p>}
      <button type="button" onClick={onClose}>{t('taskSuggestionUi.close')}</button>
    </div>
  </div>
}
