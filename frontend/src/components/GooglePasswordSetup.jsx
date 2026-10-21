import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import GoogleAuthButton from './GoogleAuthButton'
import { setPasswordWithGoogle } from '../api/accountApi'

export default function GooglePasswordSetup() {
  const { t } = useTranslation()
  const { getAccessToken } = useAuth()
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const verifyGoogle = async idToken => {
    setError('')
    setSuccess('')
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
      await setPasswordWithGoogle(getAccessToken(), idToken, password)
      setPassword('')
      setConfirm('')
      setSuccess(t('googlePassword.success'))
    } catch (e) {
      setError(t('googlePassword.error', { status: e.status || 'NETWORK' }))
    } finally {
      setBusy(false)
    }
  }

  return <section className="settings-section">
    <h2>{t('googlePassword.title')}</h2>
    <p>{t('googlePassword.description')}</p>
    <div className="settings-form">
      <label>{t('googlePassword.new')}
        <input type="password" autoComplete="new-password" minLength={8} maxLength={128}
          value={password} disabled={busy} onChange={e => setPassword(e.target.value)} />
      </label>
      <label>{t('googlePassword.confirm')}
        <input type="password" autoComplete="new-password" minLength={8} maxLength={128}
          value={confirm} disabled={busy} onChange={e => setConfirm(e.target.value)} />
      </label>
      {error && <div className="error-message" role="alert">{error}</div>}
      {success && <div className="settings-success" role="status">{success}</div>}
      <p>{t('googlePassword.button')}</p>
      <GoogleAuthButton disabled={busy} onCredential={verifyGoogle} />
    </div>
  </section>
}
