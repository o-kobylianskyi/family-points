import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { API_URL } from '../api/baseUrl'
import { useTranslation } from 'react-i18next'
import LanguageSwitcher from '../components/LanguageSwitcher'

export default function AcceptMemberInvitePage() {
  const { t } = useTranslation()
  const [query] = useSearchParams()
  const token = query.get('token') || ''
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState(false)
  const [error, setError] = useState('')

  const submit = async event => {
    event.preventDefault()
    setError('')
    if (password !== confirmation) return setError(t('inviteAcceptUi.passwordMismatch'))
    if (!token) return setError(t('inviteAcceptUi.noToken'))
    setBusy(true)
    try {
      const response = await fetch(`${API_URL}/auth/member-invitations/accept`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, username: username.trim(), password }),
      })
      if (!response.ok) {
        let message = response.status === 503 ? t('inviteAcceptUi.disabled') :
          response.status === 409 ? t('inviteAcceptUi.conflict') :
          t('inviteAcceptUi.invalid')
        if (response.status >= 500 && response.status !== 503) message = t('inviteAcceptUi.server')
        throw new Error(message)
      }
      setDone(true)
      setPassword('')
      setConfirmation('')
    } catch (e) {
      setError(e.message || t('inviteAcceptUi.failed'))
    } finally {
      setBusy(false)
    }
  }

  return <div className="login-page"><div className="login-card">
    <LanguageSwitcher />
    <h1>{t("inviteAcceptUi.title")}</h1>
    {done ? <>
      <p>{t('inviteAcceptUi.success')}</p>
      <Link to="/login">{t("inviteAcceptUi.login")}</Link>
    </> : <form onSubmit={submit}>
      <p>{t('inviteAcceptUi.description')}</p>
      <label>{t('inviteAcceptUi.username')}
        <input required minLength={3} maxLength={64} autoComplete="username"
          pattern="[a-zA-Z0-9._-]{3,64}" value={username} onChange={e => setUsername(e.target.value)} />
      </label>
      <label>{t('inviteAcceptUi.password')}
        <input required type="password" autoComplete="new-password" minLength={8} maxLength={128}
          value={password} onChange={e => setPassword(e.target.value)} />
      </label>
      <label>{t('inviteAcceptUi.confirm')}
        <input required type="password" autoComplete="new-password" minLength={8} maxLength={128}
          value={confirmation} onChange={e => setConfirmation(e.target.value)} />
      </label>
      {!token && <p className="error-message">{t('inviteAcceptUi.noToken')}</p>}
      {error && <p className="error-message" role="alert">{error}</p>}
      <button disabled={busy || !token}>{busy ? t('inviteAcceptUi.creating') : t('inviteAcceptUi.accept')}</button>
    </form>}
    <p><Link to="/login">{t("inviteAcceptUi.return")}</Link></p>
  </div></div>
}
