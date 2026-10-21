import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { API_URL } from '../api/baseUrl'

export default function UsernameAvailability({ username, currentUsername = '', onResult }) {
  const { t } = useTranslation()
  const [result, setResult] = useState(null)
  const [busy, setBusy] = useState(false)
  const normalized = username.trim()
  const unchanged = currentUsername && normalized.toLowerCase() === currentUsername.trim().toLowerCase()
  const valid = /^[A-Za-z0-9._-]{3,64}$/.test(normalized)

  useEffect(() => {
    setResult(null)
    onResult?.(null)
  }, [username, currentUsername])

  const check = async () => {
    if (!valid) { setResult('invalid'); onResult?.(false); return }
    if (unchanged) { setResult('available'); onResult?.(true); return }
    setBusy(true)
    try {
      const response = await fetch(`${API_URL}/auth/username-availability?username=${encodeURIComponent(normalized)}`)
      if (!response.ok) throw new Error('http')
      const data = await response.json()
      const available = Boolean(data.valid && data.available)
      setResult(available ? 'available' : 'taken')
      onResult?.(available)
    } catch {
      setResult('error')
      onResult?.(null)
    } finally { setBusy(false) }
  }

  return <div className="username-availability">
    <button type="button" className="secondary-button" onClick={check} disabled={busy || !normalized}>
      {busy ? t('usernameCheck.checking') : t('usernameCheck.button')}
    </button>
    {result && <span role="status" className={result === 'available' ? 'username-availability-success' : 'username-availability-error'}>
      {t('usernameCheck.' + result)}
    </span>}
  </div>
}
