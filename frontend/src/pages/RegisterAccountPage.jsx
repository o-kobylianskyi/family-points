import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { registerGlobalAccount } from '../api/authApi'
import { useAuth } from '../context/AuthContext'
import GoogleAuthButton from '../components/GoogleAuthButton'

export default function RegisterAccountPage() {
  const { currentUser, needsWorkspace, loginWithGoogle } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
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
      setError(e.message || 'Не вдалося зареєструватися')
    } finally {
      setBusy(false)
    }
  }
  if (sent) return <div className="login-page"><div className="login-card"><h1>Перевір пошту</h1><p>Ми надіслали лист із посиланням для підтвердження email. Після підтвердження можеш увійти.</p><Link to="/login">До входу</Link></div></div>

  return <div className="login-page"><div className="login-card">
    <h1>Новий акаунт FamilyPoints</h1>
    <p>Спочатку створіть акаунт. Після входу зможете створити або вибрати сім’ю.</p>
    <form onSubmit={submit}>
      <label>Логін
        <input value={username} minLength={3} maxLength={50} pattern="[A-Za-z0-9._-]{3,50}"
          autoComplete="username" required disabled={busy}
          onChange={e => setUsername(e.target.value)} />
      </label>
      <label>Email (обов’язково)
        <input type="email" value={email} maxLength={254} autoComplete="email" required disabled={busy}
          onChange={e => setEmail(e.target.value)} />
      </label>
      <label>Пароль (12–128 символів)
        <input type="password" value={password} minLength={12} maxLength={128}
          autoComplete="new-password" required disabled={busy}
          onChange={e => setPassword(e.target.value)} />
      </label>
      {error && <p className="error-message" role="alert">{error}</p>}
      <button type="submit" disabled={busy}>Створити акаунт</button>
    </form>
    <GoogleAuthButton disabled={busy} onCredential={async idToken => {
      setBusy(true)
      setError('')
      try {
        const user = await loginWithGoogle(idToken, true)
        navigate(user ? '/' : '/choose-family', { replace: true })
      } catch (e) {
        setError(e.message || 'Не вдалося створити Google-акаунт')
      } finally {
        setBusy(false)
      }
    }} />
    <p><Link to="/login">Повернутися до входу</Link></p>
  </div></div>
}
