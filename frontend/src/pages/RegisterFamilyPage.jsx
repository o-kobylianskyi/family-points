import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { registerFirstFamily } from '../api/registrationApi'
import LanguageSwitcher from '../components/LanguageSwitcher'

const LIVE_REGISTRATION = import.meta.env.VITE_ENABLE_FAMILY_REGISTRATION === 'true'
const DEMO = { username: 'demo_parent', password: 'DemoPassword123!', ownerName: 'Тестовий учасник', familyName: 'Демонстраційна сім’я' }

export default function RegisterFamilyPage() {
  const { currentUser, login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', password: '', ownerName: '', familyName: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [preview, setPreview] = useState(false)

  if (currentUser && LIVE_REGISTRATION) return <Navigate to="/" replace />

  const change = (key) => (event) => setForm((previous) => ({ ...previous, [key]: event.target.value }))
  const submit = async (event) => {
    event.preventDefault()
    setError('')
    if (!/^[A-Za-z0-9._-]{3,50}$/.test(form.username)) {
      setError('Логін: 3–50 символів, латинські літери, цифри, крапка, дефіс або підкреслення.')
      return
    }
    if (form.password.length < 12 || form.password.length > 128) {
      setError('Пароль повинен містити від 12 до 128 символів.')
      return
    }
    if (!form.ownerName.trim() || !form.familyName.trim()) {
      setError('Введіть ім’я та назву сім’ї.')
      return
    }
    if (!LIVE_REGISTRATION) {
      setPreview(true)
      return
    }
    setBusy(true)
    try {
      await registerFirstFamily(form)
      await login(form.username, form.password)
      setForm((previous) => ({ ...previous, password: '' }))
      navigate('/', { replace: true })
    } catch (exception) {
      setError(exception.message || 'Помилка реєстрації.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-language"><LanguageSwitcher /></div>
        <div className="brand"><div className="brand-icon">FP</div>
          <div><h1>FamilyPoints</h1><p>Створення першої сім’ї</p></div>
        </div>
        {!LIVE_REGISTRATION && (
          <div role="status" style={{ marginBottom: 16, padding: 12, border: "1px solid #9aa9b8", borderRadius: 8 }}>
            <strong>ДЕМО — без запису в базу даних.</strong>
            <p>Можна переглянути форму й наступний крок. Реальний акаунт не створюється.</p>
            <button type="button" onClick={() => { setForm(DEMO); setPreview(false); setError("") }}>
              Заповнити тестовими даними
            </button>
          </div>
        )}
        <form onSubmit={submit}>
          <label>Назва сім’ї
            <input required maxLength={100} autoComplete="organization"
              value={form.familyName} onChange={change('familyName')} disabled={busy} />
          </label>
          <label>Ваше ім’я
            <input required maxLength={100} autoComplete="name"
              value={form.ownerName} onChange={change('ownerName')} disabled={busy} />
          </label>
          <label>Логін
            <input required minLength={3} maxLength={50} autoComplete="username"
              value={form.username} onChange={change('username')} disabled={busy} />
          </label>
          <label>Пароль (мінімум 12 символів)
            <input required type="password" minLength={12} maxLength={128}
              autoComplete="new-password" value={form.password}
              onChange={change('password')} disabled={busy} />
          </label>
          {error && <div className="error-message" role="alert">{error}</div>}
          <button type="submit" disabled={busy}>
            {busy ? 'Створюємо сім’ю…' : LIVE_REGISTRATION ? 'Зареєструватися' : 'Переглянути результат (демо)'}
          </button>
        </form>
        {preview && !LIVE_REGISTRATION && (
          <div role="status" style={{ marginTop: 16, padding: 16, border: "1px solid #9aa9b8", borderRadius: 8 }}>
            <h2>Попередній перегляд сім’ї</h2>
            <p><strong>Сім’я:</strong> {form.familyName}</p>
            <p><strong>Перший учасник:</strong> {form.ownerName}</p>
            <p><strong>Роль:</strong> Адміністратор сім’ї</p>
            <p><strong>Економіка:</strong> POINTS</p>
            <p><strong>Статус:</strong> Лише перегляд. Жодних даних не збережено.</p>
            <button type="button" onClick={() => setPreview(false)}>Повернутися до форми</button>
          </div>
        )}
        <p><Link to="/login">Уже маєте акаунт? Увійти</Link></p>
      </div>
    </div>
  )
}
