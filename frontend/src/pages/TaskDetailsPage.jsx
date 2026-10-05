import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import CreateTaskModal from '../components/CreateTaskModal'
import { getWorkspaceMembers } from '../api/workspaceApi'
import {
  getTaskDefinition,
  getTaskDefinitions,
  getTaskDefinitionInstance,
  startTask, completeTask, pauseTask, resumeTask, cancelTask, releaseTask, delegateTask,
  getTaskDefinitionHistory,
  getTaskParticipants,
  getTaskSubtasks,
  setTaskDefinitionActive,
} from '../api/taskApi'

function formatDateTime(value, locale) {
  if (!value) return '—'
  return new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function TaskDetailsPage() {
  const { t, i18n } = useTranslation()
  const { definitionId } = useParams()
  const [searchParams] = useSearchParams()
  const selectedDate = searchParams.get('date') || new Date().toISOString().slice(0, 10)
  const { currentUser, getAccessToken } = useAuth()
  const [task, setTask] = useState(null)
  const [visibleDefinitions, setVisibleDefinitions] = useState([])
  const [instance, setInstance] = useState(null)
  const [delegating, setDelegating] = useState(false)
  const [delegateToMemberId, setDelegateToMemberId] = useState('')
  const [members, setMembers] = useState([])
  const [participants, setParticipants] = useState([])
  const [history, setHistory] = useState([])
  const [subtasks, setSubtasks] = useState([])
  const [createSubtaskOpen, setCreateSubtaskOpen] = useState(false)
  const [error, setError] = useState('')
  const [processing, setProcessing] = useState(false)

  const load = async () => {
    try {
      setError('')
      const token = getAccessToken()
      const [definition, definitions, memberList, participantList, audit, childTasks, execution] = await Promise.all([
        getTaskDefinition(token, currentUser.workspaceId, definitionId),
        getTaskDefinitions(token, currentUser.workspaceId),
        getWorkspaceMembers(token, currentUser.workspaceId),
        getTaskParticipants(token, currentUser.workspaceId, definitionId),
        getTaskDefinitionHistory(token, currentUser.workspaceId, definitionId),
        getTaskSubtasks(token, currentUser.workspaceId, definitionId),
        getTaskDefinitionInstance(token, currentUser.workspaceId, definitionId, selectedDate),
      ])
      setTask(definition)
      setVisibleDefinitions(definitions)
      setMembers(memberList)
      setParticipants(participantList)
      setHistory(audit)
      setSubtasks(childTasks)
      setInstance(execution)
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => { load() }, [definitionId, selectedDate, currentUser.workspaceId])

  if (error && !task) return <div className="page-error">{error}</div>
  if (!task) return <div className="page-loading">Завантаження...</div>

  const memberName = (id) => members.find((m) => m.id === id)?.name || (id ? `#${id}` : '—')
  const formatTaskDateTime = (value) => formatDateTime(value, i18n.resolvedLanguage || i18n.language)
  const translateEnum = (prefix, value) =>
    value ? t(`${prefix}.${value}`, { defaultValue: value }) : '—'

  const translateAuditDetails = (event) => {
    const details = event.details?.trim()
    if (!details) return ''

    const generatedMatch = details.match(/^Generated for (\d{4}-\d{2}-\d{2})$/)
    if (generatedMatch) {
      return t('taskAudit.details.generatedFor', { date: generatedMatch[1] })
    }

    const createdMatch = details.match(/^Created for (\d{4}-\d{2}-\d{2})$/)
    if (createdMatch) {
      return t('taskAudit.details.createdFor', { date: createdMatch[1] })
    }

    return details
  }
  const author = task.createdByMemberName || memberName(task.createdByMemberId)

  const visibleById = new Map(visibleDefinitions.map((definition) => [definition.id, definition]))
  const ancestors = []
  let parentId = task.parentTaskDefinitionId
  const visited = new Set()
  while (parentId && !visited.has(parentId)) {
    visited.add(parentId)
    const parent = visibleById.get(parentId)
    if (!parent) break
    ancestors.unshift(parent)
    parentId = parent.parentTaskDefinitionId
  }
  const hiddenParent = Boolean(task.parentTaskDefinitionId && !visibleById.has(task.parentTaskDefinitionId))

  const runInstanceAction = async (action) => {
    if (!instance) return
    try {
      setProcessing(true); setError('')
      const token = getAccessToken()
      const actions = { start: startTask, complete: completeTask, pause: pauseTask, resume: resumeTask, cancel: cancelTask, release: releaseTask }
      await actions[action](token, currentUser.workspaceId, instance.id)
      await load()
    } catch (e) { setError(e.message) } finally { setProcessing(false) }
  }

  const handleDelegate = async () => {
    if (!instance || !delegateToMemberId) return
    try {
      setProcessing(true); setError('')
      await delegateTask(getAccessToken(), currentUser.workspaceId, instance.id, Number(delegateToMemberId))
      setDelegating(false); setDelegateToMemberId('')
      await load()
    } catch (e) { setError(e.message) } finally { setProcessing(false) }
  }

  const toggleActive = async () => {
    try {
      setProcessing(true)
      setError('')
      await setTaskDefinitionActive(getAccessToken(), currentUser.workspaceId, task.id, !task.active)
      await load()
    } catch (e) { setError(e.message) } finally { setProcessing(false) }
  }

  return (
    <div className="task-details-page">
      <Link className="task-details-back" to="/tasks">← До завдань</Link>

      {error && <div className="page-error">{error}</div>}

      {(ancestors.length > 0 || hiddenParent) && (
        <nav className="task-hierarchy-breadcrumb" aria-label="Ієрархія завдання">
          {ancestors.map((parent) => (
            <span key={parent.id}>
              <Link to={`/tasks/${parent.id}?date=${selectedDate}`}>№{parent.id} · {parent.title}</Link>
              <span> → </span>
            </span>
          ))}
          {hiddenParent && <span>Батьківське завдання недоступне → </span>}
          <strong>№{task.id} · {task.title}</strong>
        </nav>
      )}

      <header className="task-details-header">
        <div>
          <div className="task-details-number">Завдання №{task.id}</div>
          <h1>{task.title}</h1>
          {task.description && <p>{task.description}</p>}
        </div>
        <span className={task.active ? 'task-status task-status-completed' : 'task-status'}>
          {task.active ? 'Активне' : 'Неактивне'}
        </span>
      </header>

      {instance && (
        <section className="task-details-card task-execution-card">
          <div className="task-details-section-header">
            <div>
              <h2>Виконання · {selectedDate}</h2>
              <p className="task-details-muted">Виконавець: <strong>{memberName(instance.memberId)}</strong> · Статус: <strong>{translateEnum('taskStatus', instance.status)}</strong></p>
            </div>
            <div className="task-card-actions">
              {instance.status === 'PENDING' && <button className="task-action-button primary" disabled={processing} onClick={() => runInstanceAction('start')}>Почати</button>}
              {instance.status === 'IN_PROGRESS' && <>
                <button className="task-action-button primary" disabled={processing} onClick={() => runInstanceAction('complete')}>Виконано</button>
                <button className="task-action-button" disabled={processing} onClick={() => runInstanceAction('pause')}>Призупинити</button>
              </>}
              {instance.status === 'PAUSED' && <button className="task-action-button primary" disabled={processing} onClick={() => runInstanceAction('resume')}>Продовжити</button>}
              {instance.delegationAllowed && ['PENDING','IN_PROGRESS','PAUSED'].includes(instance.status) &&
                <button className="task-action-button" onClick={() => { setDelegating(!delegating); setDelegateToMemberId(String(members.find(m => m.id !== instance.memberId)?.id || '')) }}>Делегувати</button>}
              {['PENDING','IN_PROGRESS','PAUSED'].includes(instance.status) &&
                <button className="task-action-button" disabled={processing} onClick={() => window.confirm('Відмінити виконання?') && runInstanceAction('cancel')}>Відмінити</button>}
              {['PENDING','IN_PROGRESS','PAUSED'].includes(instance.status) &&
                <button className="task-action-button" disabled={processing} onClick={() => window.confirm('Відмовитися від завдання?') && runInstanceAction('release')}>Відмовитися</button>}
            </div>
          </div>
          {delegating && <div className="task-delegation-panel">
            <select value={delegateToMemberId} onChange={(e) => setDelegateToMemberId(e.target.value)}>
              {members.filter(m => m.id !== instance.memberId).map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
            </select>
            <button className="primary-button" disabled={processing || !delegateToMemberId} onClick={handleDelegate}>Передати</button>
          </div>}
        </section>
      )}

      <div className="task-details-grid">
        <section className="task-details-card">
          <h2>Основне</h2>
          <dl className="task-details-fields">
            <div><dt>Автор</dt><dd>{author}</dd></div>
            <div><dt>Створено</dt><dd>{formatTaskDateTime(task.createdAt)}</dd></div>
            <div><dt>Оновлено</dt><dd>{formatTaskDateTime(task.updatedAt)}</dd></div>
            <div><dt>Обов'язкове</dt><dd>{task.mandatory ? 'Так' : 'Ні'}</dd></div>
            <div><dt>Повторення</dt><dd>{translateEnum('taskRecurrence', task.recurrenceType)}</dd></div>
            <div><dt>Термін</dt><dd>{task.dueTime || '—'}</dd></div>
            <div><dt>Призначення</dt><dd>{translateEnum('assignmentPolicy', task.assignmentPolicy)}</dd></div>
            <div><dt>Делегування</dt><dd>{task.delegationAllowed ? 'Дозволено' : 'Заборонено'}</dd></div>
          </dl>
        </section>

        <section className="task-details-card">
          <h2>Відповідальність і бали</h2>
          <dl className="task-details-fields">
            <div><dt>Призначено</dt><dd>{memberName(task.assignedMemberId)}</dd></div>
            <div><dt>Відповідальний</dt><dd>{memberName(task.responsibleMemberId)}</dd></div>
            <div><dt>Бажаний виконавець</dt><dd>{memberName(task.preferredMemberId)}</dd></div>
            <div><dt>Нагорода</dt><dd>{task.rewardAmount ? `+${task.rewardAmount} ${translateEnum('pointType', task.rewardPointTypeCode)}` : '—'}</dd></div>
            <div><dt>Репутація за виконання</dt><dd>{task.rewardReputationAmount > 0 ? `+${task.rewardReputationAmount}` : '—'}</dd></div>
            <div><dt>Штраф</dt><dd>{task.penaltyAmount ? `-${task.penaltyAmount} ${translateEnum('pointType', task.penaltyPointTypeCode)}` : '—'}</dd></div>
            <div><dt>Репутація за невиконання</dt><dd>{task.penaltyReputationAmount > 0 ? `-${task.penaltyReputationAmount}` : '—'}</dd></div>
          </dl>
        </section>
      </div>

      <section className="task-details-card">
        <div className="task-details-section-header">
          <h2>Підзавдання</h2>
          <button type="button" className="task-action-button primary" onClick={() => setCreateSubtaskOpen(true)}>+ Створити</button>
        </div>
        {subtasks.length === 0 ? (
          <p className="task-details-muted">Підзавдань поки немає.</p>
        ) : (
          <div className="task-details-subtasks">
            {subtasks.map((subtask) => (
              <Link key={subtask.id} className="task-details-subtask" to={`/tasks/${subtask.id}`}>
                <span><strong>№{subtask.id}</strong> · {subtask.title}</span>
                <span>{subtask.rewardAmount ? `+${subtask.rewardAmount} ${translateEnum('pointType', subtask.rewardPointTypeCode)}` : 'Без винагороди'} →</span>
              </Link>
            ))}
          </div>
        )}
        <p className="task-details-muted">Створення підзавдання буде використовувати бюджет і контекст цього завдання.</p>
      </section>

      <section className="task-details-card">
        <div className="task-details-section-header">
          <h2>Учасники</h2>
        </div>
        {participants.length === 0 ? <p className="task-details-muted">Додаткових учасників немає.</p> :
          <div className="task-participant-list">{participants.map((p) =>
            <span key={p.id} className="task-participant-chip">{p.actorName || `#${p.actorId}`} · {translateEnum('taskParticipantRole', p.role)}</span>
          )}</div>}
      </section>

      <section className="task-details-card">
        <div className="task-details-section-header">
          <h2>Керування</h2>
          <button className="task-action-button secondary" disabled={processing} onClick={toggleActive}>
            {task.active ? 'Деактивувати' : 'Активувати'}
          </button>
        </div>
        <p className="task-details-muted">Редагування та видалення TaskDefinition будуть зосереджені на цій сторінці.</p>
      </section>

      <section className="task-details-card">
        <h2>Історія</h2>
        {history.length === 0 ? <p className="task-details-muted">Історія поки порожня.</p> :
          <div className="task-details-history">{history.map((event) =>
            <div key={event.id} className="task-details-history-row">
              <time>{formatTaskDateTime(event.occurredAt)}</time>
              <strong>{translateEnum('taskAudit.eventType', event.eventType)}</strong>
              {event.details && <span>{translateAuditDetails(event)}</span>}
            </div>
          )}</div>}
      </section>
      {createSubtaskOpen && (
        <CreateTaskModal
          members={members}
          existingTaskDefinitions={visibleDefinitions}
          defaultMemberId={instance?.memberId || currentUser.memberId}
          parentTaskDefinitionId={task.id}
          parentTaskTitle={task.title}
          onClose={() => setCreateSubtaskOpen(false)}
          onCreated={async () => {
            setCreateSubtaskOpen(false)
            await load()
          }}
        />
      )}
    </div>
  )
}

export default TaskDetailsPage
