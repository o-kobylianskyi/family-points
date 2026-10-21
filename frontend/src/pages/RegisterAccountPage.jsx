import UsernameAvailability from '../components/UsernameAvailability'
import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { registerGlobalAccount, requestEmailVerification } from '../api/authApi'
import { useAuth } from '../context/AuthContext'
import GoogleAuthButton from '../components/GoogleAuthButton'
import { useTranslation } from 'react-i18next'
import LanguageSwitcher from '../components/LanguageSwitcher'

export default function RegisterAccountPage() {
  const { t } = useTranslation()
  const { currentUser, needsWorkspace, loginWithGoogle } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [resendMessage, setResendMessage] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (currentUser) return <Navigate to="/" replace />
  if (needsWorkspace) return <Navigate to="/choose-family" replace />

  const submit = async e => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await registerGlobalAccount(username.trim(), email.trim(), password)
      setPassword('')
      setSent(true)
    } catch (e) {
      setError(e.message || t('accountRegisterUi.registrationError'))
    } finally {
      setBusy(false)
    }
  }
  if (sent) return <div className="login-page"><div className="login-card"><h1>{t('accountRegisterUi.verifyTitle')}</h1><p>{t('accountRegisterUi.verifyDescription')}</p><button type="button" onClick={async () => {
      setResendMessage('')
      try {
        await requestEmailVerification(username, email)
        setResendMessage(t('accountRegisterUi.resendDone'))
      } catch (error) { setResendMessage(error.message) }
    }}>{t('accountRegisterUi.resend')}</button>
    {resendMessage && <p role="status">{resendMessage}</p>}
    <Link to="/login">{t("accountRegisterUi.login")}</Link></div></div>

  return <div className="login-page"><div className="login-card">
    <LanguageSwitcher /><h1>{t("accountRegisterUi.title")}</h1>
    <p>{t('accountRegisterUi.description')}</p>
    <form onSubmit={submit}>
      <label>{t('accountRegisterUi.username')}
        <input value={username} minLength={3} maxLength={64} pattern="[A-Za-z0-9._-]{3,64}"
          autoComplete="username" required disabled={busy}
          onChange={e => setUsername(e.target.value)} /><UsernameAvailability username={username} />
      </label>
      <label>{t('accountRegisterUi.email')}
        <input type="email" value={email} maxLength={254} autoComplete="email" required disabled={busy}
          onChange={e => setEmail(e.target.value)} />
      </label>
      <label>{t('accountRegisterUi.password')}
        <input type="password" value={password} minLength={8} maxLength={128}
          autoComplete="new-password" required disabled={busy}
          onChange={e => setPassword(e.target.value)} />
      </label>
      {error && <p className="error-message" role="alert">{error}</p>}
      <button type="submit" disabled={busy}>{t("accountRegisterUi.create")}</button>
    </form>
    <GoogleAuthButton disabled={busy} onCredential={async idToken => {
      setBusy(true)
      setError('')
      try {
        const user = await loginWithGoogle(idToken, true)
        navigate(user ? '/' : '/choose-family', { replace: true })
      } catch (e) {
        setError(e.googleAuthStatus
              ? `${t('authUi.googleVerifyFailed')} — ${t('authUi.googleHttpDetails', {
                  status: e.googleAuthStatus,
                  reason: e.googleAuthReason,
                })}`
              : e instanceof TypeError ? t('authUi.googleNetworkError')
              : e.message || t('authUi.googleRegistrationError'))
      } finally {
        setBusy(false)
      }
    }} />
    <p><Link to="/login">{t("accountRegisterUi.return")}</Link></p>
  </div></div>
}
