import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { linkGoogleAccount } from '../api/authApi'
import GoogleAuthButton from './GoogleAuthButton'
import { useTranslation } from 'react-i18next'

/**
 * Link an explicit Google identity to the currently signed-in account.
 * Never infer account ownership from a shared contact email address.
 */
export default function GoogleAccountLink() {
  const { t } = useTranslation()
  const { getAccessToken } = useAuth()
  const [currentPassword, setCurrentPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  if (!import.meta.env.VITE_GOOGLE_CLIENT_ID) return null

  const handleGoogleCredential = async (idToken) => {
    if (!currentPassword) {
      setError(t('googleLinkUi.passwordRequired'))
      return
    }
    setError('')
    setNotice('')
    setBusy(true)
    try {
      const token = getAccessToken()
      if (!token) throw new Error(t('googleLinkUi.loginRequired'))
      await linkGoogleAccount(token, idToken, currentPassword)
      setCurrentPassword('')
      setNotice(t('googleLinkUi.linked'))
    } catch (e) {
      setError(e.message || t('googleLinkUi.linkError'))
    } finally {
      setBusy(false)
    }
  }

  return <section className="settings-section">
    <h2>{t('googleLinkUi.title')}</h2>
    <p>{t('googleLinkUi.description')}</p>
    <div style={{ maxWidth: 350, display: 'grid', gap: 12 }}>
      <label>{t('googleLinkUi.currentPassword')}
        <input type="password" autoComplete="current-password"
          value={currentPassword} onChange={e => setCurrentPassword(e.target.value)}
          disabled={busy} />
      </label>
      <GoogleAuthButton disabled={busy || !currentPassword} onCredential={handleGoogleCredential} />
      {error && <p className="error-message" role="alert">{error}</p>}
      {notice && <p className="settings-success" role="status">{notice}</p>}
    </div>
  </section>
}
