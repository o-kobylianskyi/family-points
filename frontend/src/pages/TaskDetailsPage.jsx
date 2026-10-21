import { useCurrencyQuantity } from '../utils/useCurrencyQuantity'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import CreateTaskModal from '../components/CreateTaskModal'
import TaskScheduleEditor from '../components/TaskScheduleEditor'
import TaskRewardNegotiationModal from '../components/TaskRewardNegotiationModal'
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
  completeScheduledTaskByManager,
  getTaskDefinitionPermissions,
} from '../api/taskApi'

function formatDateTime(value, locale) {
  if (!value) return '—'
  return new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function TaskDetailsPage() {
  const { t, i18n } = useTranslation()
  const { definitionId } = useParams()
  const [searchParams] = useSearchParams()
  const localToday = () => {
    const now = new Date()
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  }
  const selectedDate = searchParams.get('date') || localToday()
  const selectedSlotKey = searchParams.get('slotKey') || 'default'
  const { currentUser, getAccessToken } = useAuth()
  const currency = useCurrencyQuantity(getAccessToken(), currentUser.workspaceId)
  const [task, setTask] = useState(null)
  const [canManage, setCanManage] = useState(false)
  const [visibleDefinitions, setVisibleDefinitions] = useState([])
  const [instance, setInstance] = useState(null)
  const [delegating, setDelegating] = useState(false)
  const [delegateToMemberId, setDelegateToMemberId] = useState('')
  const [members, setMembers] = useState([])
  const [participants, setParticipants] = useState([])
  const [history, setHistory] = useState([])
  const [subtasks, setSubtasks] = useState([])
  const [createSubtaskOpen, setCreateSubtaskOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [error, setError] = useState('')
  const [processing, setProcessing] = useState(false)
  const [rewardNegotiationOpen, setRewardNegotiationOpen] = useState(false)

  const load = async () => {
    try {
      setError('')
      const token = getAccessToken()
      const [definition, definitions, memberList, participantList, audit, childTasks, execution, permissions] = await Promise.all([
        getTaskDefinition(token, currentUser.workspaceId, definitionId),
        getTaskDefinitions(token, currentUser.workspaceId),
        getWorkspaceMembers(token, currentUser.workspaceId),
        getTaskParticipants(token, currentUser.workspaceId, definitionId),
        getTaskDefinitionHistory(token, currentUser.workspaceId, definitionId),
        getTaskSubtasks(token, currentUser.workspaceId, definitionId),
        getTaskDefinitionInstance(token, currentUser.workspaceId, definitionId, selectedDate, selectedSlotKey),
        getTaskDefinitionPermissions(token, currentUser.workspaceId, definitionId),
      ])
      setCanManage(permissions.canManage === true)
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

  useEffect(() => { load() }, [definitionId, selectedDate, selectedSlotKey, currentUser.workspaceId])

  if (error && !task) return <div className="page-error">{error}</div>
  if (!task) return <div className="page-loading">{t("common.loading")}</div>

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

  const confirmScheduledCompletion = async () => {
    if (!window.confirm(t('taskDetailsUi.confirmCompletion', { title: task.title, date: selectedDate }))) return
    try {
      setProcessing(true)
      setError('')
      await completeScheduledTaskByManager(getAccessToken(), currentUser.workspaceId, task.id, selectedDate, selectedSlotKey)
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
      <Link className="task-details-back" to="/tasks">{t('taskDetailsUi.back')}</Link>

      {error && <div className="page-error">{error}</div>}

      {(ancestors.length > 0 || hiddenParent) && (
        <nav className="task-hierarchy-breadcrumb" aria-label={t("taskDetailsUi.hierarchy")}>
          {ancestors.map((parent) => (
            <span key={parent.id}>
              <Link to={`/tasks/${parent.id}?date=${selectedDate}`}>№{parent.id} · {parent.title}</Link>
              <span> → </span>
            </span>
          ))}
          {hiddenParent && <span>{t('taskDetailsUi.hiddenParent')}</span>}
          <strong>№{task.id} · {task.title}</strong>
        </nav>
      )}

      <header className="task-details-header">
        <div>
          <div className="task-details-number">{t('taskDetailsUi.taskNo', { id: task.id })}</div>
          <h1>{task.title}</h1>
          {task.description && <p>{task.description}</p>}
        </div>
        <span className={task.active ? 'task-status task-status-completed' : 'task-status'}>
          {task.active ? t('taskDetailsUi.active') : t('taskDetailsUi.inactive')}
        </span>
      </header>

      {instance && (
        <section className="task-details-card task-execution-card">
          <div className="task-details-section-header">
            <div>
              <h2>{t('taskDetailsUi.execution')} · {selectedDate} · {selectedSlotKey}</h2>
              <p className="task-details-muted">{t('taskDetailsUi.executor')} <strong>{memberName(instance.memberId)}</strong> · {t('taskDetailsUi.status')} <strong>{translateEnum('taskStatus', instance.status)}</strong></p>
            </div>
            <div className="task-card-actions">
              {instance.status === 'PENDING' && <button className="task-action-button primary" disabled={processing} onClick={() => runInstanceAction('start')}>{t('taskDetailsUi.start')}</button>}
              {instance.status === 'IN_PROGRESS' && <>
                <button className="task-action-button primary" disabled={processing} onClick={() => runInstanceAction('complete')}>{t('taskDetailsUi.complete')}</button>
                <button className="task-action-button" disabled={processing} onClick={() => runInstanceAction('pause')}>{t('taskDetailsUi.pause')}</button>
              </>}
              {instance.status === 'PAUSED' && <button className="task-action-button primary" disabled={processing} onClick={() => runInstanceAction('resume')}>{t('taskDetailsUi.resume')}</button>}
              {instance.memberId === currentUser.memberId
                && ['PENDING','IN_PROGRESS','PAUSED'].includes(instance.status) && (
                  <button
                    className="task-action-button"
                    type="button"
                    onClick={() => setRewardNegotiationOpen(true)}
                  >
                    {t('taskDetailsUi.changeReward')}
                  </button>
                )}
              {instance.delegationAllowed && ['PENDING','IN_PROGRESS','PAUSED'].includes(instance.status) &&
                <button className="task-action-button" onClick={() => { setDelegating(!delegating); setDelegateToMemberId(String(members.find(m => m.id !== instance.memberId)?.id || '')) }}>{t('taskDetailsUi.delegate')}</button>}
              {['PENDING','IN_PROGRESS','PAUSED'].includes(instance.status) &&
                <button className="task-action-button" disabled={processing} onClick={() => window.confirm(t('taskDetailsUi.cancelExecutionConfirm')) && runInstanceAction('cancel')}>{t('taskDetailsUi.cancelExecution')}</button>}
              {['PENDING','IN_PROGRESS','PAUSED'].includes(instance.status) &&
                <button className="task-action-button" disabled={processing} onClick={() => window.confirm(t('taskDetailsUi.releaseConfirm')) && runInstanceAction('release')}>{t('taskDetailsUi.release')}</button>}
            </div>
          </div>
          {delegating && <div className="task-delegation-panel">
            <select value={delegateToMemberId} onChange={(e) => setDelegateToMemberId(e.target.value)}>
              {members.filter(m => m.id !== instance.memberId).map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
            </select>
            <button className="primary-button" disabled={processing || !delegateToMemberId} onClick={handleDelegate}>{t('taskDetailsUi.transfer')}</button>
          </div>}
        </section>
      )}

      <div className="task-details-grid">
        <section className="task-details-card">
          <h2>{t('taskDetailsUi.basics')}</h2>
          <dl className="task-details-fields">
            <div><dt>{t('taskDetailsUi.author')}</dt><dd>{author}</dd></div>
            <div><dt>{t('taskDetailsUi.created')}</dt><dd>{formatTaskDateTime(task.createdAt)}</dd></div>
            <div><dt>{t('taskDetailsUi.updated')}</dt><dd>{formatTaskDateTime(task.updatedAt)}</dd></div>
            <div><dt>{t('taskDetailsUi.mandatory')}</dt><dd>{task.mandatory ? t('taskDetailsUi.yes') : t('taskDetailsUi.no')}</dd></div>
            <div><dt>{t('taskDetailsUi.recurrence')}</dt><dd>{translateEnum('taskRecurrence', task.recurrenceType)}</dd></div>
            <div><dt>{t('taskDetailsUi.due')}</dt><dd>{task.dueTime || '—'}</dd></div>
            <div><dt>{t('taskDetailsUi.assignment')}</dt><dd>{translateEnum('assignmentPolicy', task.assignmentPolicy)}</dd></div>
            <div><dt>{t('taskDetailsUi.delegation')}</dt><dd>{task.delegationAllowed ? t('taskDetailsUi.allowed') : t('taskDetailsUi.forbidden')}</dd></div>
          </dl>
        </section>

        <section className="task-details-card">
          <h2>{t('taskDetailsUi.responsibility')}</h2>
          <dl className="task-details-fields">
            <div><dt>{t('taskDetailsUi.assigned')}</dt><dd>{memberName(task.assignedMemberId)}</dd></div>
            <div><dt>{t('taskDetailsUi.responsible')}</dt><dd>{memberName(task.responsibleMemberId)}</dd></div>
            <div><dt>{t('taskDetailsUi.preferred')}</dt><dd>{memberName(task.preferredMemberId)}</dd></div>
            <div><dt>{t('taskDetailsUi.reward')}</dt><dd>{task.rewardAmount ? `+${currency(task.rewardAmount, task.rewardPointTypeCode, task.rewardPointTypeId, translateEnum('pointType', task.rewardPointTypeCode))}` : '—'}</dd></div>
            <div><dt>{t('taskDetailsUi.completionRep')}</dt><dd>{task.rewardReputationAmount > 0 ? `+${task.rewardReputationAmount}` : '—'}</dd></div>
            <div><dt>{t('taskDetailsUi.penalty')}</dt><dd>{task.penaltyAmount ? `-${currency(task.penaltyAmount, task.penaltyPointTypeCode, task.penaltyPointTypeId, translateEnum('pointType', task.penaltyPointTypeCode))}` : '—'}</dd></div>
            <div><dt>{t('taskDetailsUi.missedRep')}</dt><dd>{task.penaltyReputationAmount > 0 ? `-${task.penaltyReputationAmount}` : '—'}</dd></div>
          </dl>
        </section>
      </div>

      <section className="task-details-card">
        <div className="task-details-section-header">
          <h2>{t('taskDetailsUi.subtasks')}</h2>
          <button type="button" className="task-action-button primary" onClick={() => setCreateSubtaskOpen(true)}>{t('taskDetailsUi.create')}</button>
        </div>
        {subtasks.length === 0 ? (
          <p className="task-details-muted">{t('taskDetailsUi.noSubtasks')}</p>
        ) : (
          <div className="task-details-subtasks">
            {subtasks.map((subtask) => (
              <Link key={subtask.id} className="task-details-subtask" to={`/tasks/${subtask.id}`}>
                <span><strong>№{subtask.id}</strong> · {subtask.title}</span>
                <span>{subtask.rewardAmount ? `+${currency(subtask.rewardAmount, subtask.rewardPointTypeCode, subtask.rewardPointTypeId, translateEnum('pointType', subtask.rewardPointTypeCode))}` : t('taskDetailsUi.noReward')} →</span>
              </Link>
            ))}
          </div>
        )}
        <p className="task-details-muted">{t('taskDetailsUi.subtaskNote')}</p>
      </section>

      <section className="task-details-card">
        <div className="task-details-section-header">
          <h2>{t('taskDetailsUi.members')}</h2>
        </div>
        {participants.length === 0 ? <p className="task-details-muted">{t('taskDetailsUi.noMembers')}</p> :
          <div className="task-participant-list">{participants.map((p) =>
            <span key={p.id} className="task-participant-chip">{p.actorName || `#${p.actorId}`} · {translateEnum('taskParticipantRole', p.role)}</span>
          )}</div>}
      </section>

      {canManage && <section className="task-details-card">
        <div className="task-details-section-header">
          <h2>{t('taskDetailsUi.management')}</h2>
          <div className="task-card-actions">
            <button className="task-action-button primary" type="button" disabled={processing}
              onClick={() => setEditOpen(true)}>{t('taskDetailsUi.edit')}</button>
            {task.active && (!instance || ['PENDING', 'IN_PROGRESS'].includes(instance.status)) &&
              selectedDate <= localToday() && (
              <button className="task-action-button" type="button" disabled={processing}
                onClick={confirmScheduledCompletion}>{t('taskDetailsUi.markComplete')} · {selectedDate} · {selectedSlotKey}</button>
            )}
            <button className="task-action-button secondary" disabled={processing} onClick={toggleActive}>
              {task.active ? t('taskDetailsUi.deactivate') : t('taskDetailsUi.activate')}
            </button>
          </div>
        </div>
      </section>}

      {canManage && <TaskScheduleEditor definitionId={task.id} onChanged={load} />}

      <section className="task-details-card">
        <h2>{t('taskDetailsUi.history')}</h2>
        {history.length === 0 ? <p className="task-details-muted">{t('taskDetailsUi.noHistory')}</p> :
          <div className="task-details-history">{history.map((event) =>
            <div key={event.id} className="task-details-history-row">
              <time>{formatTaskDateTime(event.occurredAt)}</time>
              <strong>{translateEnum('taskAudit.eventType', event.eventType)}</strong>
              {event.details && <span>{translateAuditDetails(event)}</span>}
            </div>
          )}</div>}
      </section>
      {rewardNegotiationOpen && instance && (
        <TaskRewardNegotiationModal
          task={instance}
          onClose={() => setRewardNegotiationOpen(false)}
          onSaved={async () => {
            setRewardNegotiationOpen(false)
            await load()
          }}
        />
      )}

      {canManage && editOpen && (
        <CreateTaskModal
          members={members}
          existingTaskDefinitions={visibleDefinitions}
          defaultMemberId={task.assignedMemberId ?? currentUser.memberId}
          initialTask={task}
          initialParticipants={participants}
          onClose={() => setEditOpen(false)}
          onCreated={async () => { setEditOpen(false); await load() }}
        />
      )}

      {canManage && createSubtaskOpen && (
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
