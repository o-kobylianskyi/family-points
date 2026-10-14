import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { linkGoogleAccount } from '../api/authApi'
import GoogleAuthButton from './GoogleAuthButton'

/**
 * Link an explicit Google identity to the currently signed-in account.
 * Never infer account ownership from a shared contact email address.
 */
export default function GoogleAccountLink() {
  const { getAccessToken } = useAuth()
  const [currentPassword, setCurrentPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  if (!import.meta.env.VITE_GOOGLE_CLIENT_ID) return null

  const handleGoogleCredential = async (idToken) => {
    if (!currentPassword) {
      setError('Спочатку введіть поточний пароль цього акаунта')
      return
    }
    setError('')
    setNotice('')
    setBusy(true)
    try {
      const token = getAccessToken()
      if (!token) throw new Error('Потрібно увійти повторно')
      await linkGoogleAccount(token, idToken, currentPassword)
      setCurrentPassword('')
      setNotice('Google-акаунт успішно прив’язано. Тепер ним можна входити.')
    } catch (e) {
      setError(e.message || 'Помилка прив’язки Google')
    } finally {
      setBusy(false)
    }
  }

  return <section className="settings-section">
    <h2>Google-акаунт</h2>
    <p>Прив’язати Google для входу в цей логін. Навіть якщо інші члени сім’ї використовують ту саму пошту, їхні акаунти не зміняться.</p>
    <div style={{ maxWidth: 350, display: 'grid', gap: 12 }}>
      <label>Поточний пароль
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
