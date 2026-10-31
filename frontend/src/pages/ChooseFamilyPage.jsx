import { useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTranslation } from 'react-i18next'

function familyErrorMessage(error, t, fallbackKey) {
  const status = error?.status
  if (status === 401) return t('familyChoice.sessionExpired')
  if (status === 403) return t('familyChoice.forbidden')
  if (status === 404) return t('familyChoice.notFound')
  if (status === 409) return t('familyChoice.conflict')
  if (status >= 500) return t('familyChoice.serverError')
  if (error instanceof TypeError || /failed to fetch|networkerror|network request failed|load failed/i.test(error?.message || '')) return t('familyChoice.networkError')
  return t(fallbackKey)
}

export default function ChooseFamilyPage() {
  const { t } = useTranslation()
  const { currentUser, loading, needsWorkspace, getAvailableWorkspaces,
    switchWorkspace, createFirstFamily, logout, sessionError } = useAuth()
  const [choices, setChoices] = useState([])
  const [familyName, setFamilyName] = useState('')
  const [ownerName, setOwnerName] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [retryKey, setRetryKey] = useState(0)
  const [choicesLoaded, setChoicesLoaded] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    if (!needsWorkspace) return
    let alive = true
    getAvailableWorkspaces().then(value => { if (alive) { setChoices(value); setChoicesLoaded(true); setError('') } })
      .catch(e => { if (alive) { setChoicesLoaded(false); setError(familyErrorMessage(e, t, 'familyChoice.loadingError')) } })
    return () => { alive = false }
  }, [needsWorkspace, retryKey])

  if (loading) return <div className="app-loading">{t('common.loading')}</div>
  if (currentUser && !needsWorkspace) return <Navigate to="/" replace />
  if (!needsWorkspace) return <Navigate to="/login" replace />

  const activate = async (workspaceId) => {
    setError('')
    setBusy(true)
    try {
      await switchWorkspace(workspaceId)
    } catch (e) {
      setError(familyErrorMessage(e, t, 'familyChoice.selectionError'))
      setBusy(false)
    }
  }

  const create = async (event) => {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      await createFirstFamily(familyName, ownerName)
    } catch (e) {
      setError(familyErrorMessage(e, t, 'familyChoice.creationError'))
      setBusy(false)
    }
  }

  return <div className="login-page">
    <div className="login-card family-choice-card">
      <h1>{t('familyChoice.title')}</h1>
      {(error || sessionError) && <p className="error-message" role="alert">{error || familyErrorMessage(sessionError, t, 'familyChoice.sessionExpired')}</p>}
      {!choicesLoaded && <button type="button" disabled={busy}
        onClick={() => setRetryKey(value => value + 1)}>{t('familyChoice.retry')}</button>}
      {choicesLoaded && choices.length > 0 ? <>
        <p>{t('familyChoice.choose')}</p>
        <div className="family-choice-layout">
          <div className="family-choice-list">
            {choices.map(choice =>
              <button type="button" key={choice.workspaceId} disabled={busy}
                onClick={() => activate(choice.workspaceId)}>
                {choice.workspaceName} — {choice.memberName}
              </button>)}
          </div>
        </div>
      </> : choicesLoaded ? <>
        <p>{t('familyChoice.empty')}</p>
        <form onSubmit={create}>
          <label>{t('familyChoice.name')}
            <input value={familyName} maxLength={100} required disabled={busy}
              onChange={e => setFamilyName(e.target.value)} />
          </label>
          <label>{t('familyChoice.ownerName')}
            <input value={ownerName} maxLength={100} required disabled={busy}
              onChange={e => setOwnerName(e.target.value)} />
          </label>
          <button type="submit" disabled={busy}>{t('familyChoice.create')}</button>
        </form>
      </> : null}
      <div className="family-choice-footer">
        <button className="family-choice-logout" type="button" disabled={busy}
          onClick={() => { logout(); navigate('/login') }}>{t('familyChoice.logout')}</button>
      </div>
    </div>
  </div>
}
