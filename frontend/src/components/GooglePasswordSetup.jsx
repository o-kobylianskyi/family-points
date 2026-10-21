import UsernameAvailability from './UsernameAvailability'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import GoogleAuthButton from './GoogleAuthButton'
import { setPasswordWithGoogle } from '../api/accountApi'

export default function GooglePasswordSetup() {
  const { t } = useTranslation()
  const { getAccessToken, currentUser } = useAuth()
  const [username, setUsername] = useState(currentUser?.username || '')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const verifyGoogle = async idToken => {
    setError('')
    setSuccess('')
    if (!/^[A-Za-z0-9._-]{3,64}$/.test(username.trim())) {
      setError(t('googlePassword.usernameInvalid'))
      return
    }
    if (password.length < 8 || password.length > 128) {
      setError(t('googlePassword.invalid'))
      return
    }
    if (password !== confirm) {
      setError(t('googlePassword.mismatch'))
      return
    }
    setBusy(true)
    try {
      await setPasswordWithGoogle(getAccessToken(), idToken, password, username.trim())
      setPassword('')
      setConfirm('')
      setSuccess(t('googlePassword.success'))
    } catch (e) {
      setError(e.status === 409 ? t('googlePassword.usernameTaken') : t('googlePassword.error', { status: e.status || 'NETWORK' }))
    } finally {
      setBusy(false)
    }
  }

  return <section className="settings-section">
    <h2>{t('googlePassword.title')}</h2>
    <p>{t('googlePassword.description')}</p>

    <div className="google-password-form">
      <label className="google-password-field"><span>{t('googlePassword.username')}</span>
        <input type="text" autoComplete="username" value={username} maxLength={64} disabled={busy}
          onChange={e => setUsername(e.target.value)} /><UsernameAvailability username={username} currentUsername={currentUser?.username} />
      </label>
      <label className="google-password-field"><span>{t('googlePassword.new')}</span>
        <input type="password" autoComplete="new-password" minLength={8} maxLength={128}
          value={password} disabled={busy} onChange={e => setPassword(e.target.value)} />
      </label>
      <label className="google-password-field"><span>{t('googlePassword.confirm')}</span>
        <input type="password" autoComplete="new-password" minLength={8} maxLength={128}
          value={confirm} disabled={busy} onChange={e => setConfirm(e.target.value)} />
      </label>
      {error && <div className="error-message" role="alert">{error}</div>}
      {success && <div className="settings-success" role="status">{success}</div>}
      <p className="google-password-instructions">{t('googlePassword.button')}</p>
      <GoogleAuthButton disabled={busy} onCredential={verifyGoogle} />
    </div>
  </section>
}
