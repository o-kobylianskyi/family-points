import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { API_URL } from '../api/baseUrl'
import { useTranslation } from 'react-i18next'
import LanguageSwitcher from '../components/LanguageSwitcher'
import GoogleAuthButton from '../components/GoogleAuthButton'

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

  const responseError = async response => {
    let code = ''
    try {
      const body = await response.json()
      code = typeof body.detail === 'string' ? body.detail : ''
    } catch { /* no structured error body */ }
    if (response.status === 410) return t('inviteAcceptUi.gone')
    if (code.includes('INVITED_EMAIL_MISMATCH')) return t('inviteAcceptUi.googleMismatch')
    if (code.includes('GOOGLE_ACCOUNT_ALREADY_REGISTERED')) return t('inviteAcceptUi.googleExisting')
    if (response.status === 503) return t('inviteAcceptUi.disabled')
    if (response.status === 409) return t('inviteAcceptUi.conflict')
    if (response.status >= 500) return t('inviteAcceptUi.server')
    return `${t('inviteAcceptUi.invalid')} (HTTP ${response.status})`
  }

  const acceptGoogle = async idToken => {
    if (!token) return setError(t('inviteAcceptUi.noToken'))
    setBusy(true)
    setError('')
    try {
      const response = await fetch(`${API_URL}/auth/member-invitations/accept-google`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, idToken }),
      })
      if (!response.ok) throw new Error(await responseError(response))
      setDone(true)
      setPassword('')
      setConfirmation('')
    } catch (e) {
      setError(e.message || t('inviteAcceptUi.googleFailure', { status: 'NETWORK' }))
    } finally {
      setBusy(false)
    }
  }

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
      if (!response.ok) throw new Error(await responseError(response))
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
      <h3>{t('inviteAcceptUi.methodPassword')}</h3>
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
    {!done && token && <>
      <p>{t('inviteAcceptUi.googleOption')}</p>
      <GoogleAuthButton onCredential={acceptGoogle} disabled={busy} />
    </>}
    <p><Link to="/login">{t("inviteAcceptUi.return")}</Link></p>
  </div></div>
}
