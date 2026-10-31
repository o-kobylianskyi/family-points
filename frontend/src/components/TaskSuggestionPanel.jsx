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
  return <div className="modal-backdrop task-suggestion-backdrop" role="presentation" onMouseDown={e => { if (e.target === e.currentTarget) onClose() }}>
    <section className="modal-card task-suggestion-dialog" role="dialog" aria-modal="true" aria-labelledby="task-suggestion-heading">
      <header className="task-suggestion-header">
        <div>
          <h2 id="task-suggestion-heading">{t('taskSuggestionUi.suggestTitle')}</h2>
          <p>{t('taskSuggestionUi.suggestDescription')}</p>
        </div>
        <button type="button" className="modal-close" onClick={onClose} aria-label={t('taskSuggestionUi.close')}>×</button>
      </header>
      <div className="task-suggestion-body">
        <form className="task-suggestion-form" onSubmit={submit}>
          <label className="task-suggestion-field">
            <span>{t('taskSuggestionUi.title')}</span>
            <input required maxLength={150} value={title} disabled={busy} onChange={e => setTitle(e.target.value)} />
          </label>
          <label className="task-suggestion-field">
            <span>{t('taskSuggestionUi.description')}</span>
            <textarea rows={4} maxLength={1000} value={description} disabled={busy} onChange={e => setDescription(e.target.value)} />
          </label>
          <div className="task-suggestion-actions">
            <button type="submit" className="primary-button" disabled={busy || !title.trim()}>{t('taskSuggestionUi.submit')}</button>
          </div>
        </form>
        {message && <p className="settings-success" role="status">{message}</p>}
        {error && <p className="error-message" role="alert">{error}</p>}
        <div className="task-suggestion-list">
          <h3>{t('taskSuggestionUi.requests')}</h3>
          {items.length === 0 && <p className="task-suggestion-empty">{t('taskSuggestionUi.noRequests')}</p>}
          {items.map(item => <article className="task-suggestion-item" key={item.id}>
            <div className="task-suggestion-item-header">
              <strong>{item.title}</strong>
              <span className="task-suggestion-status">{t('taskSuggestionUi.' + item.status.toLowerCase())}</span>
            </div>
            {item.description && <p>{item.description}</p>}
            {isAdultManager && item.status === 'PENDING' && <div className="task-suggestion-review-actions">
              <button type="button" className="primary-button" disabled={busy} onClick={() => decide(item.id, 'APPROVED')}>{t('taskSuggestionUi.approve')}</button>
              <button type="button" className="secondary-button" disabled={busy} onClick={() => decide(item.id, 'REJECTED')}>{t('taskSuggestionUi.reject')}</button>
            </div>}
          </article>)}
          {isAdultManager && <p className="task-suggestion-hint">{t('taskSuggestionUi.approvalReminder')}</p>}
        </div>
      </div>
      <footer className="task-suggestion-footer">
        <button type="button" className="secondary-button" onClick={onClose}>{t('taskSuggestionUi.close')}</button>
      </footer>
    </section>
  </div>
}
