import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { API_URL } from '../api/baseUrl'

export default function AcceptMemberInvitePage() {
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
    if (password !== confirmation) return setError('Паролі не збігаються')
    if (!token) return setError('Посилання не містить токена запрошення')
    setBusy(true)
    try {
      const response = await fetch(`${API_URL}/auth/member-invitations/accept`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, username: username.trim(), password }),
      })
      if (!response.ok) {
        let message = response.status === 503 ? 'Запрошення ще не увімкнені на сервері' :
          response.status === 409 ? 'Такий логін уже зайнятий або профіль має акаунт' :
          'Посилання недійсне, використане або минув термін дії'
        if (response.status >= 500 && response.status !== 503) message = 'Помилка сервера. Спробуйте пізніше'
        throw new Error(message)
      }
      setDone(true)
      setPassword('')
      setConfirmation('')
    } catch (e) {
      setError(e.message || 'Не вдалося прийняти запрошення')
    } finally {
      setBusy(false)
    }
  }

  return <div className="login-page"><div className="login-card">
    <h1>Запрошення у FamilyPoints</h1>
    {done ? <>
      <p>Акаунт створено й прив’язано до існуючого учасника сім’ї.</p>
      <Link to="/login">Перейти до входу</Link>
    </> : <form onSubmit={submit}>
      <p>Створіть власний логін і пароль. Посилання дійсне лише один раз протягом 24 годин.</p>
      <label>Логін
        <input required minLength={3} maxLength={64} autoComplete="username"
          pattern="[a-zA-Z0-9._-]{3,64}" value={username} onChange={e => setUsername(e.target.value)} />
      </label>
      <label>Пароль
        <input required type="password" autoComplete="new-password" minLength={8} maxLength={128}
          value={password} onChange={e => setPassword(e.target.value)} />
      </label>
      <label>Повторіть пароль
        <input required type="password" autoComplete="new-password" minLength={8} maxLength={128}
          value={confirmation} onChange={e => setConfirmation(e.target.value)} />
      </label>
      {!token && <p className="error-message">У посиланні відсутній токен</p>}
      {error && <p className="error-message" role="alert">{error}</p>}
      <button disabled={busy || !token}>{busy ? 'Створюємо…' : 'Прийняти запрошення'}</button>
    </form>}
    <p><Link to="/login">Повернутися до входу</Link></p>
  </div></div>
}
