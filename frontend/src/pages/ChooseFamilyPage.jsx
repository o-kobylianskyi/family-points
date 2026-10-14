import { useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function ChooseFamilyPage() {
  const { currentUser, loading, needsWorkspace, getAvailableWorkspaces,
    switchWorkspace, createFirstFamily, logout } = useAuth()
  const [choices, setChoices] = useState([])
  const [familyName, setFamilyName] = useState('')
  const [ownerName, setOwnerName] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    if (!needsWorkspace) return
    let alive = true
    getAvailableWorkspaces().then(value => { if (alive) setChoices(value) })
      .catch(() => { if (alive) setError('Не вдалося завантажити сім’ї') })
    return () => { alive = false }
  }, [needsWorkspace])

  if (loading) return <div className="app-loading">Завантаження…</div>
  if (currentUser && !needsWorkspace) return <Navigate to="/" replace />
  if (!needsWorkspace) return <Navigate to="/login" replace />

  const activate = async (workspaceId) => {
    setError('')
    setBusy(true)
    try {
      await switchWorkspace(workspaceId)
    } catch (e) {
      setError(e.message || 'Не вдалося вибрати сім’ю')
      setBusy(false)
    }
  }

  const create = async (event) => {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      await createFirstFamily(familyName, ownerName)
    } catch (e) {
      setError(e.message || 'Не вдалося створити сім’ю')
      setBusy(false)
    }
  }

  return <div className="login-page">
    <div className="login-card">
      <h1>Вибір сім’ї</h1>
      {choices.length > 0 ? <>
        <p>Оберіть сім’ю, з якою хочете працювати.</p>
        {choices.map(choice =>
          <button type="button" key={choice.workspaceId} disabled={busy}
            onClick={() => activate(choice.workspaceId)}>
            {choice.workspaceName} — {choice.memberName}
          </button>)}
      </> : <>
        <p>Акаунт створено. Тепер можна створити першу сім’ю.</p>
        <form onSubmit={create}>
          <label>Назва сім’ї
            <input value={familyName} maxLength={100} required disabled={busy}
              onChange={e => setFamilyName(e.target.value)} />
          </label>
          <label>Ваше ім’я
            <input value={ownerName} maxLength={100} required disabled={busy}
              onChange={e => setOwnerName(e.target.value)} />
          </label>
          <button type="submit" disabled={busy}>Створити сім’ю</button>
        </form>
      </>}
      {error && <p className="error-message" role="alert">{error}</p>}
      <button type="button" disabled={busy} onClick={() => { logout(); navigate('/login') }}>
        Вийти
      </button>
    </div>
  </div>
}
