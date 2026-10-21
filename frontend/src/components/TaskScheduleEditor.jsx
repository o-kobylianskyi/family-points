import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useTranslation } from 'react-i18next'
import { getTaskScheduleSlots, createTaskScheduleSlot, updateTaskScheduleSlot, setTaskScheduleSlotEnabled } from '../api/taskApi'

const normalize = (time) => time?.slice(0, 5) ?? ''


function parseTime(value) {
  const raw = String(value ?? '').trim().replace('.', ':')
  if (!raw) return null
  let hour, minute
  if (/^\d{1,2}$/.test(raw)) {
    hour = Number(raw); minute = 0
  } else if (/^\d{3,4}$/.test(raw)) {
    const padded = raw.padStart(4, '0')
    hour = Number(padded.slice(0, 2)); minute = Number(padded.slice(2))
  } else {
    const match = raw.match(/^(\d{1,2}):(\d{1,2})$/)
    if (!match) return null
    hour = Number(match[1]); minute = Number(match[2])
  }
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
}

function suggestedTime(slots) {
  const times = slots.map(s => normalize(s.scheduledTime)).filter(t => /^([01]\d|2[0-3]):[0-5]\d$/.test(t))
  if (!times.length) return '09:00'
  const last = times.sort().at(-1)
  const minutes = Number(last.slice(0, 2)) * 60 + Number(last.slice(3)) + 60
  return `${String(Math.floor((minutes % 1440) / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`
}
function suggestedKey(slots) {
  let number = 1
  while (slots.some(s => s.slotKey === `slot_${number}`)) number++
  return `slot_${number}`
}

export default function TaskScheduleEditor({ definitionId, onChanged }) {
  const { t } = useTranslation()
  const { currentUser, getAccessToken } = useAuth()
  const [slots, setSlots] = useState([])
  const [newKey, setNewKey] = useState('')
  const [newTime, setNewTime] = useState('09:00')
  const [drafts, setDrafts] = useState({})
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const workspaceId = currentUser.workspaceId
  const refresh = async () => {
    const items = await getTaskScheduleSlots(getAccessToken(), workspaceId, definitionId)
    setSlots(items)
    setNewTime(suggestedTime(items))
    setDrafts(Object.fromEntries(items.map(s => [s.id, normalize(s.scheduledTime)])))
    onChanged?.()
  }
  useEffect(() => {
    let active = true
    getTaskScheduleSlots(getAccessToken(), workspaceId, definitionId)
      .then(items => { if (active) { setSlots(items); setNewTime(suggestedTime(items)); setDrafts(Object.fromEntries(items.map(s => [s.id, normalize(s.scheduledTime)]))) } })
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
    const key = newKey.trim() || suggestedKey(slots)
    if (!/^[a-zA-Z0-9_-]{1,40}$/.test(key) || key === 'default') {
      setError(t('scheduleUi.keyInvalid'))
      return
    }
    const time = parseTime(newTime)
    if (!time) {
      setError(t('scheduleUi.timeInvalid'))
      return
    }
    if (slots.some(s => s.slotKey === key)) {
      setError(t('scheduleUi.keyDuplicate'))
      return
    }
    run(async () => {
      await createTaskScheduleSlot(getAccessToken(), workspaceId, definitionId, key, time)
      setNewKey('')
      setNewTime(suggestedTime([...slots, { scheduledTime: time }]))
    })
  }

  return (
    <section className="task-details-card">
      <h2>{t('scheduleUi.title')}</h2>
      <p className="task-details-muted">{t('scheduleUi.description')}</p>
      {error && <p className="page-error" role="alert">{error}</p>}
      {slots.length === 0 && <p>{t('scheduleUi.empty')}</p>}
      <div style={{ display: 'grid', gap: '0.75rem' }}>
        {slots.map(slot => (
          <div key={slot.id} style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '0.5rem' }}>
            <strong style={{ minWidth: '8rem' }}>{slot.slotKey}</strong>
            <input type="text" inputMode="numeric" placeholder={t("scheduleUi.timePlaceholder")} aria-label={`${t('scheduleUi.time')}: ${slot.slotKey}`} value={drafts[slot.id] ?? ''}
              disabled={pending} onChange={e => setDrafts(p => ({ ...p, [slot.id]: e.target.value }))} onBlur={() => { const time = parseTime(drafts[slot.id]); if (time) setDrafts(p => ({ ...p, [slot.id]: time })) }} />
            <button type="button" className="task-action-button" disabled={pending || normalize(slot.scheduledTime) === parseTime(drafts[slot.id])}
              onClick={() => { const time = parseTime(drafts[slot.id]); if (!time) { setError(t('scheduleUi.timeExample')); return }; run(() => updateTaskScheduleSlot(getAccessToken(), workspaceId, definitionId, slot.id, time, slot.enabled)) }}>{t('scheduleUi.saveTime')}</button>
            <button type="button" className="task-action-button secondary" disabled={pending}
              onClick={() => run(() => setTaskScheduleSlotEnabled(getAccessToken(), workspaceId, definitionId, slot.id, !slot.enabled))}>
              {slot.enabled ? t('scheduleUi.disable') : t('scheduleUi.enable')}
            </button>
            <span>{slot.enabled ? t('scheduleUi.active') : t('scheduleUi.inactive')}</span>
          </div>
        ))}
      </div>
      <form noValidate onSubmit={add} style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'end', gap: '0.5rem', marginTop: '1rem' }}>
        <label>{t('scheduleUi.slotKey')}
          <input value={newKey} placeholder={suggestedKey(slots)} disabled={pending}
            onChange={e => setNewKey(e.target.value)} maxLength={40} />
        </label>
        <label>{t('scheduleUi.time')}
          <input type="text" inputMode="numeric" placeholder={t("scheduleUi.timePlaceholder")} lang="uk" aria-label={t("scheduleUi.newTime")} value={newTime} disabled={pending} onChange={e => setNewTime(e.target.value)} onBlur={() => { const time = parseTime(newTime); if (time) setNewTime(time) }} />
        </label>
        <button className="task-action-button primary" disabled={pending} type="submit">{t('scheduleUi.add')}</button>
      </form>
    </section>
  )
}
