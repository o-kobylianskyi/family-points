import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { verifyAccountEmail, requestPasswordReset, resetAccountPassword } from '../api/authApi'

export default function EmailActionPage({ action }) {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [pending, setPending] = useState(false)
  const [done, setDone] = useState(false)
  const [error, setError] = useState('')
  const labels = {
    verify: 'Підтвердження email',
    request: 'Відновити пароль',
    reset: 'Новий пароль',
  }
  const submit = async event => {
    event.preventDefault()
    setPending(true)
    setError('')
    try {
      if (action === 'verify') await verifyAccountEmail(token)
      else if (action === 'request') await requestPasswordReset(username, email)
      else await resetAccountPassword(token, password)
      setDone(true)
    } catch (e) {
      setError(e.message || 'Не вдалося виконати операцію')
    } finally {
      setPending(false)
    }
  }
  return <div className="login-page"><div className="login-card">
    <h1>{labels[action]}</h1>
    {done ? <>
      <p>{action === 'request' ? 'Якщо акаунт із підтвердженою адресою існує, надійде лист.' :
        'Операцію завершено. Тепер можна увійти.'}</p>
      <Link to="/login">До входу</Link>
    </> : <form onSubmit={submit}>
      {action === 'request' && <label>Логін
        <input autoComplete="username" required value={username}
          onChange={e => setUsername(e.target.value)} />
      </label>}
      {action === 'request' && <label>Email
        <input type="email" autoComplete="email" required value={email}
          onChange={e => setEmail(e.target.value)} />
      </label>}
      {action === 'reset' && <label>Новий пароль (від 12 символів)
        <input type="password" autoComplete="new-password" minLength={12} maxLength={128}
          required value={password} onChange={e => setPassword(e.target.value)} />
      </label>}
      {action !== 'request' && !token && <p className="error-message">У посиланні відсутній токен.</p>}
      {error && <p role="alert" className="error-message">{error}</p>}
      <button type="submit" disabled={pending || (action !== 'request' && !token)}>
        {pending ? 'Зачекайте…' : action === 'verify' ? 'Підтвердити email' :
          action === 'request' ? 'Надіслати посилання' : 'Змінити пароль'}
      </button>
    </form>}
    <p><Link to="/login">Повернутися до входу</Link></p>
  </div></div>
}
