import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { getTaskScheduleSlots, createTaskScheduleSlot, updateTaskScheduleSlot, setTaskScheduleSlotEnabled } from '../api/taskApi'

const normalize = (time) => time?.slice(0, 5) ?? ''

export default function TaskScheduleEditor({ definitionId, onChanged }) {
  const { currentUser, getAccessToken } = useAuth()
  const [slots, setSlots] = useState([])
  const [newKey, setNewKey] = useState('')
  const [newTime, setNewTime] = useState('')
  const [drafts, setDrafts] = useState({})
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const workspaceId = currentUser.workspaceId
  const refresh = async () => {
    const items = await getTaskScheduleSlots(getAccessToken(), workspaceId, definitionId)
    setSlots(items)
    setDrafts(Object.fromEntries(items.map(s => [s.id, normalize(s.scheduledTime)])))
    onChanged?.()
  }
  useEffect(() => {
    let active = true
    getTaskScheduleSlots(getAccessToken(), workspaceId, definitionId)
      .then(items => { if (active) { setSlots(items); setDrafts(Object.fromEntries(items.map(s => [s.id, normalize(s.scheduledTime)]))) } })
      .catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [definitionId, workspaceId, getAccessToken])

  const run = async (action) => {
    try {
      setPending(true)
      setError('')
      await action()
      await refresh()
    } catch (e) { setError(e.message) } finally { setPending(false) }
  }
  const add = (event) => {
    event.preventDefault()
    const key = newKey.trim()
    if (!/^[a-zA-Z0-9_-]{1,40}$/.test(key) || key === 'default') {
      setError('Ключ має містити 1–40 латинських літер, цифр, _ або -. Назва default зарезервована.')
      return
    }
    if (slots.some(s => s.slotKey === key)) {
      setError('Слот із таким ключем уже існує')
      return
    }
    run(async () => {
      await createTaskScheduleSlot(getAccessToken(), workspaceId, definitionId, key, newTime)
      setNewKey('')
      setNewTime('')
    })
  }

  return (
    <section className="task-details-card">
      <h2>Розклад виконань</h2>
      <p className="task-details-muted">Кожен слот — окреме виконання. Зміна часу не змінює історію вже створених виконань. Вимкнення припиняє планування нових.</p>
      {error && <p className="page-error" role="alert">{error}</p>}
      {slots.length === 0 && <p>Окремих слотів немає. Використовується звичайний час завдання.</p>}
      <div style={{ display: 'grid', gap: '0.75rem' }}>
        {slots.map(slot => (
          <div key={slot.id} style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '0.5rem' }}>
            <strong style={{ minWidth: '8rem' }}>{slot.slotKey}</strong>
            <input type="time" aria-label={`Час: ${slot.slotKey}`} value={drafts[slot.id] ?? ''}
              disabled={pending} onChange={e => setDrafts(p => ({ ...p, [slot.id]: e.target.value }))} />
            <button type="button" className="task-action-button" disabled={pending || normalize(slot.scheduledTime) === drafts[slot.id]}
              onClick={() => run(() => updateTaskScheduleSlot(getAccessToken(), workspaceId, definitionId, slot.id, drafts[slot.id], slot.enabled))}>Зберегти час</button>
            <button type="button" className="task-action-button secondary" disabled={pending}
              onClick={() => run(() => setTaskScheduleSlotEnabled(getAccessToken(), workspaceId, definitionId, slot.id, !slot.enabled))}>
              {slot.enabled ? 'Вимкнути' : 'Увімкнути'}
            </button>
            <span>{slot.enabled ? 'Активний' : 'Вимкнений'}</span>
          </div>
        ))}
      </div>
      <form onSubmit={add} style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'end', gap: '0.5rem', marginTop: '1rem' }}>
        <label>Ключ слота
          <input value={newKey} placeholder="morning / evening / third" disabled={pending}
            onChange={e => setNewKey(e.target.value)} required maxLength={40} />
        </label>
        <label>Час (необов'язково)
          <input type="time" value={newTime} disabled={pending} onChange={e => setNewTime(e.target.value)} />
        </label>
        <button className="task-action-button primary" disabled={pending} type="submit">Додати слот</button>
      </form>
    </section>
  )
}
