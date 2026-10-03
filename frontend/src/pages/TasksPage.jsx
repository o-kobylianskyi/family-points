import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'

import { useAuth } from '../context/AuthContext'
import { getWorkspaceMembers } from '../api/workspaceApi'
import {
  claimTask,
  completeTask,
  pauseTask,
  resumeTask,
  cancelTask,
  releaseTask,
  delegateTask,
  getMemberTasks,
  getOpenTasks,
  getTaskDefinitionsByView,
  getTaskParticipants,
  getTaskInstanceHistory,
  getTaskDefinitionHistory,
  setTaskDefinitionActive,
  startTask,
} from '../api/taskApi'

import CreateTaskModal from '../components/CreateTaskModal'

function getToday() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function newestFirst(items) {
  return [...items].sort((a, b) => Number(b.id) - Number(a.id))
}

function TasksPage() {
  const { t, i18n } = useTranslation()
  const { currentUser, getAccessToken } = useAuth()

  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [editingTask, setEditingTask] = useState(null)
  const [members, setMembers] = useState([])
  const [selectedMemberId, setSelectedMemberId] = useState(null)
  const [selectedDate, setSelectedDate] = useState(getToday())

  const [tasks, setTasks] = useState([])
  const [openTasks, setOpenTasks] = useState([])
  const [managementView, setManagementView] = useState('created')
  const [pageTab, setPageTab] = useState('my')
  const [managedTasks, setManagedTasks] = useState([])
  const [participantMap, setParticipantMap] = useState({})

  const [loadingMembers, setLoadingMembers] = useState(true)
  const [loadingTasks, setLoadingTasks] = useState(false)
  const [processingTaskId, setProcessingTaskId] = useState(null)
  const [delegatingTaskId, setDelegatingTaskId] = useState(null)
  const [delegateToMemberId, setDelegateToMemberId] = useState('')
  const [error, setError] = useState('')
  const [historyTaskId, setHistoryTaskId] = useState(null)
  const [historyByTaskId, setHistoryByTaskId] = useState({})
  const [loadingHistoryTaskId, setLoadingHistoryTaskId] = useState(null)
  const [definitionHistoryId, setDefinitionHistoryId] = useState(null)
  const [historyByDefinitionId, setHistoryByDefinitionId] = useState({})
  const [loadingDefinitionHistoryId, setLoadingDefinitionHistoryId] = useState(null)

  useEffect(() => {
    const loadMembers = async () => {
      try {
        setError('')

        const data = await getWorkspaceMembers(
          getAccessToken(),
          currentUser.workspaceId
        )

        setMembers(data)

        if (data.length > 0) {
          const currentMemberExists = data.some(
            (member) => member.id === currentUser.memberId
          )

          setSelectedMemberId(
            currentMemberExists
              ? currentUser.memberId
              : data[0].id
          )
        }
      } catch (error) {
        setError(error.message)
      } finally {
        setLoadingMembers(false)
      }
    }

    loadMembers()
  }, [
    currentUser.workspaceId,
    currentUser.memberId,
    getAccessToken,
  ])

  useEffect(() => {
    if (!selectedMemberId || !selectedDate) {
      return
    }

    loadAllTasks()
  }, [
    selectedMemberId,
    selectedDate,
    currentUser.workspaceId,
  ])

  const loadAllTasks = async () => {
    try {
      setLoadingTasks(true)
      setError('')

      const token = getAccessToken()

      const [memberTasks, availableOpenTasks] =
        await Promise.all([
          getMemberTasks(
            token,
            currentUser.workspaceId,
            selectedMemberId,
            selectedDate
          ),
          getOpenTasks(
            token,
            currentUser.workspaceId,
            selectedDate,
            selectedMemberId
          ),
        ])

      setTasks(memberTasks)
      setOpenTasks(availableOpenTasks)
    } catch (error) {
      setError(error.message)
    } finally {
      setLoadingTasks(false)
    }
  }

  const loadManagedTasks = async () => {
    try {
      const token = getAccessToken()
      const definitions = await getTaskDefinitionsByView(token, currentUser.workspaceId, managementView)
      setManagedTasks(definitions)
      const entries = await Promise.all(definitions.map(async (definition) => [definition.id, await getTaskParticipants(token, currentUser.workspaceId, definition.id)]))
      setParticipantMap(Object.fromEntries(entries))
    } catch (error) { setError(error.message) }
  }

  useEffect(() => {
    loadManagedTasks()
  }, [managementView, currentUser.workspaceId, getAccessToken])

  const handleDefinitionActive = async (task) => {
    try {
      setProcessingTaskId(`definition-${task.id}`)
      setError('')
      await setTaskDefinitionActive(getAccessToken(), currentUser.workspaceId, task.id, !task.active)
      await loadManagedTasks()
      await loadAllTasks()
    } catch (error) {
      setError(error.message)
    } finally {
      setProcessingTaskId(null)
    }
  }

  const handleTaskAction = async (task, action) => {
    try {
      setProcessingTaskId(task.id)
      setError('')

      const token = getAccessToken()
      let updatedTask

      if (action === 'start') {
        updatedTask = await startTask(
          token,
          currentUser.workspaceId,
          task.id
        )
      }

      if (action === 'complete') {
        updatedTask = await completeTask(token, currentUser.workspaceId, task.id)
      }

      if (action === 'pause') {
        updatedTask = await pauseTask(token, currentUser.workspaceId, task.id)
      }

      if (action === 'resume') {
        updatedTask = await resumeTask(token, currentUser.workspaceId, task.id)
      }

      if (action === 'cancel') {
        updatedTask = await cancelTask(token, currentUser.workspaceId, task.id)
      }

      if (action === 'release') {
        await releaseTask(token, currentUser.workspaceId, task.id)
        await loadAllTasks()
        return
      }

      if (updatedTask) {
        setTasks((current) =>
          current.map((item) =>
            item.id === updatedTask.id
              ? updatedTask
              : item
          )
        )
      }
    } catch (error) {
      setError(error.message)
    } finally {
      setProcessingTaskId(null)
    }
  }

  const handleClaim = async (definition) => {
    try {
      setProcessingTaskId(`open-${definition.id}`)
      setError('')

      await claimTask(
        getAccessToken(),
        currentUser.workspaceId,
        definition.id,
        selectedDate,
        selectedMemberId
      )

      await loadAllTasks()
    } catch (error) {
      setError(error.message)
    } finally {
      setProcessingTaskId(null)
    }
  }

  const handleDelegate = async (task) => {
    if (!delegateToMemberId) {
      return
    }

    try {
      setProcessingTaskId(task.id)
      setError('')

      await delegateTask(
        getAccessToken(),
        currentUser.workspaceId,
        task.id,
        Number(delegateToMemberId)
      )

      setDelegatingTaskId(null)
      setDelegateToMemberId('')
      await loadAllTasks()
    } catch (error) {
      setError(error.message)
    } finally {
      setProcessingTaskId(null)
    }
  }

  const beginDelegation = (task) => {
    setDelegatingTaskId(task.id)

    const firstOtherMember = members.find(
      (member) => member.id !== task.memberId
    )

    setDelegateToMemberId(
      firstOtherMember
        ? String(firstOtherMember.id)
        : ''
    )
  }

  const toggleTaskHistory = async (task) => {
    if (historyTaskId === task.id) {
      setHistoryTaskId(null)
      return
    }

    setHistoryTaskId(task.id)

    if (historyByTaskId[task.id]) {
      return
    }

    try {
      setLoadingHistoryTaskId(task.id)
      const history = await getTaskInstanceHistory(
        getAccessToken(),
        currentUser.workspaceId,
        task.id
      )
      setHistoryByTaskId((current) => ({
        ...current,
        [task.id]: history,
      }))
    } catch (error) {
      setError(error.message)
    } finally {
      setLoadingHistoryTaskId(null)
    }
  }

  const toggleDefinitionHistory = async (definition) => {
    if (definitionHistoryId === definition.id) {
      setDefinitionHistoryId(null)
      return
    }

    setDefinitionHistoryId(definition.id)

    if (historyByDefinitionId[definition.id]) {
      return
    }

    try {
      setLoadingDefinitionHistoryId(definition.id)
      const history = await getTaskDefinitionHistory(
        getAccessToken(),
        currentUser.workspaceId,
        definition.id
      )
      setHistoryByDefinitionId((current) => ({
        ...current,
        [definition.id]: history,
      }))
    } catch (error) {
      setError(error.message)
    } finally {
      setLoadingDefinitionHistoryId(null)
    }
  }

  const auditEventText = (event) => {
    const actor = event.performedByName || t('tasks.history.system', { defaultValue: 'Система' })
    const target = event.toActorName || ''

    switch (event.eventType) {
      case 'INSTANCE_CREATED': return `${actor}: ${t('tasks.history.created', { defaultValue: 'створено виконання завдання' })}`
      case 'INSTANCE_GENERATED': return `${actor}: ${t('tasks.history.generated', { defaultValue: 'автоматично створено виконання завдання' })}`
      case 'INSTANCE_CLAIMED': return `${actor}: ${t('tasks.history.claimed', { defaultValue: 'прийняв(ла) завдання' })}`
      case 'INSTANCE_STARTED': return `${actor}: ${t('tasks.history.started', { defaultValue: 'почав(ла) виконання' })}`
      case 'INSTANCE_PAUSED': return `${actor}: призупинив(ла) виконання`
      case 'INSTANCE_RESUMED': return `${actor}: продовжив(ла) виконання`
      case 'INSTANCE_CANCELLED': return `${actor}: відмінив(ла) виконання`
      case 'INSTANCE_COMPLETED': return `${actor}: ${t('tasks.history.completed', { defaultValue: 'завершив(ла) завдання' })}`
      case 'INSTANCE_MISSED': return `${actor}: ${t('tasks.history.missed', { defaultValue: 'позначив(ла) завдання пропущеним' })}`
      case 'INSTANCE_EXCUSED': return `${actor}: ${t('tasks.history.excused', { defaultValue: 'звільнив(ла) від виконання' })}`
      case 'INSTANCE_DELEGATED': return `${actor}: ${t('tasks.history.delegated', { defaultValue: 'передав(ла) завдання' })} ${event.fromActorName || ''} → ${target}`
      case 'DEFINITION_CREATED': return `${actor}: створив(ла) завдання`
      case 'DEFINITION_UPDATED': return `${actor}: змінив(ла) завдання`
      case 'DEFINITION_ACTIVATED': return `${actor}: активував(ла) завдання`
      case 'DEFINITION_DEACTIVATED': return `${actor}: деактивував(ла) завдання`
      default: return event.eventType
    }
  }

  const memberName = (memberId) =>
    members.find((member) => member.id === memberId)?.name
      ?? `#${memberId}`

  const getInitials = (name) => {
    if (!name) {
      return '?'
    }

    return name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part[0])
      .join('')
      .toUpperCase()
  }

  const formatDateTime = (value) => {
    if (!value) {
      return null
    }

    return new Intl.DateTimeFormat(
      i18n.resolvedLanguage || i18n.language,
      {
        dateStyle: 'medium',
        timeStyle: 'short',
      }
    ).format(new Date(value))
  }

  const formatTime = (value) =>
    value ? value.substring(0, 5) : null

  const selectedMember = members.find(
    (member) => member.id === selectedMemberId
  )

  const renderTaskConditions = (task) => (
    <div className="task-conditions">
      {task.dueTime && (
        <span>
          {t('tasks.dueTime')}:{' '}
          <strong>{formatTime(task.dueTime)}</strong>
        </span>
      )}

      {task.rewardAmount > 0 && (
        <span className="task-reward">
          {t('tasks.reward')}:{' '}
          <strong>
            +{task.rewardAmount}{' '}
            {task.rewardPointTypeCode}
          </strong>
        </span>
      )}

      {task.penaltyAmount > 0 && (
        <span className="task-penalty">
          {t('tasks.penalty')}:{' '}
          <strong>
            -{task.penaltyAmount}{' '}
            {task.penaltyPointTypeCode}
          </strong>
        </span>
      )}
    </div>
  )

  if (loadingMembers) {
    return (
      <div className="page-loading">
        {t('common.loading')}
      </div>
    )
  }

  return (
    <>
      <div className="page-title tasks-page-title">
        <div>
          <h1>{t('tasks.title')}</h1>
          <p>{t('tasks.description')}</p>
        </div>

        <div className="tasks-page-actions">
          <input
            className="task-date-input"
            type="date"
            value={selectedDate}
            onChange={(event) =>
              setSelectedDate(event.target.value)
            }
          />

          <button
            type="button"
            className="primary-button"
            onClick={() => setCreateModalOpen(true)}
          >
            + {t('tasks.createButton')}
          </button>
        </div>
      </div>

      <div className="page-tabs">
        <button type="button" className={pageTab === 'my' ? 'page-tab active' : 'page-tab'} onClick={() => setPageTab('my')}>Мої завдання</button>
        <button type="button" className={pageTab === 'management' ? 'page-tab active' : 'page-tab'} onClick={() => setPageTab('management')}>Керування</button>
      </div>

      {error && (
        <div className="page-error">
          {error}
        </div>
      )}

      <div className={pageTab === 'my' ? 'member-selector' : 'member-selector ui-hidden'}>
        {members.map((member) => (
          <button
            key={member.id}
            type="button"
            className={
              member.id === selectedMemberId
                ? 'member-selector-button active'
                : 'member-selector-button'
            }
            onClick={() =>
              setSelectedMemberId(member.id)
            }
          >
            <span className="selector-avatar">
              {getInitials(member.name)}
            </span>

            <span>
              <strong>{member.name}</strong>
              <small>
                {t(`memberType.${member.memberType}`)}
              </small>
            </span>
          </button>
        ))}
      </div>

      <section className={pageTab === 'my' ? 'tasks-panel' : 'tasks-panel ui-hidden'}>
        <div className="tasks-panel-header">
          <div>
            <h2>{selectedMember?.name}</h2>
            <p>
              {t('tasks.taskCount', {
                count: tasks.length,
              })}
            </p>
          </div>
        </div>

        {loadingTasks ? (
          <div className="empty-state">
            {t('common.loading')}
          </div>
        ) : tasks.length === 0 ? (
          <div className="empty-state">
            <strong>{t('tasks.noTasks')}</strong>
            <p>{t('tasks.noTasksDescription')}</p>
          </div>
        ) : (
          <div className="task-list">
            {newestFirst(tasks).map((task) => (
              <article
                className="task-card"
                key={task.id}
              >
                <div className="task-card-main">
                  <div className="task-title-row">
                    <h3>№{task.taskDefinitionId} · {task.title}</h3>

                    {task.mandatory && (
                      <span className="task-mandatory">
                        {t('tasks.mandatory')}
                      </span>
                    )}
                  </div>

                  {task.description && (
                    <p className="task-description">
                      {task.description}
                    </p>
                  )}

                  {renderTaskConditions(task)}

                  <div className="task-assignment-meta">
                    {task.originalAssignedMemberId &&
                      task.originalAssignedMemberId !== task.memberId && (
                        <span>
                          {t('tasks.assignment.assignedInitially')}:{' '}
                          <strong>
                            {memberName(task.originalAssignedMemberId)}
                          </strong>
                        </span>
                      )}

                    <span>
                      {t('tasks.assignment.executor')}:{' '}
                      <strong>
                        {memberName(task.memberId)}
                      </strong>
                    </span>

                    {task.claimedAt && (
                      <span>
                        {t('tasks.assignment.claimedAt')}:{' '}
                        {formatDateTime(task.claimedAt)}
                      </span>
                    )}
                  </div>

                  <div className="task-meta">
                    <span
                      className={`task-status task-status-${task.status.toLowerCase()}`}
                    >
                      {t(
                        `taskStatus.${task.status}`,
                        { defaultValue: task.status }
                      )}
                    </span>

                    {task.startedAt && (
                      <span>
                        {t('tasks.startedAt')}:{' '}
                        {formatDateTime(task.startedAt)}
                      </span>
                    )}

                    {task.completedAt && (
                      <span>
                        {t('tasks.completedAt')}:{' '}
                        {formatDateTime(task.completedAt)}
                      </span>
                    )}
                  </div>
                </div>

                <div className="task-card-actions">
                  {task.status === 'PENDING' && (
                    <button
                      type="button"
                      className="task-action-button"
                      disabled={processingTaskId === task.id}
                      onClick={() =>
                        handleTaskAction(task, 'start')
                      }
                    >
                      {t('tasks.start')}
                    </button>
                  )}

                  {task.status === 'IN_PROGRESS' && (
                    <>
                      <button
                        type="button"
                        className="task-action-button primary"
                        disabled={processingTaskId === task.id}
                        onClick={() => handleTaskAction(task, 'complete')}
                      >
                        {t('tasks.complete')}
                      </button>
                      <button
                        type="button"
                        className="task-action-button secondary"
                        disabled={processingTaskId === task.id}
                        onClick={() => handleTaskAction(task, 'pause')}
                      >
                        Призупинити
                      </button>
                    </>
                  )}

                  {task.status === 'PAUSED' && (
                    <button
                      type="button"
                      className="task-action-button primary"
                      disabled={processingTaskId === task.id}
                      onClick={() => handleTaskAction(task, 'resume')}
                    >
                      Продовжити
                    </button>
                  )}

                  {['PENDING', 'IN_PROGRESS', 'PAUSED'].includes(task.status) && (
                    <button
                      type="button"
                      className="task-action-button secondary"
                      disabled={processingTaskId === task.id}
                      onClick={() => {
                        if (window.confirm('Відмінити це виконання завдання?')) {
                          handleTaskAction(task, 'cancel')
                        }
                      }}
                    >
                      Відмінити
                    </button>
                  )}

                  {['PENDING', 'IN_PROGRESS', 'PAUSED'].includes(task.status)
                    && !['SINGLE_MEMBER', 'GROUP_SHARED'].includes(task.assignmentPolicy) && (
                    <button
                      type="button"
                      className="task-action-button secondary"
                      disabled={processingTaskId === task.id}
                      onClick={() => {
                        if (window.confirm('Відмовитися від завдання? Воно знову стане доступним для взяття.')) {
                          handleTaskAction(task, 'release')
                        }
                      }}
                    >
                      Відмовитися
                    </button>
                  )}

                  <button
                    type="button"
                    className="task-action-button secondary"
                    disabled={loadingHistoryTaskId === task.id}
                    onClick={() => toggleTaskHistory(task)}
                  >
                    {historyTaskId === task.id
                      ? t('tasks.history.hide', { defaultValue: 'Сховати історію' })
                      : t('tasks.history.show', { defaultValue: 'Історія' })}
                  </button>

                  {task.delegationAllowed &&
                    ['PENDING', 'IN_PROGRESS', 'PAUSED'].includes(task.status) && (
                      <button
                        type="button"
                        className="task-action-button secondary"
                        disabled={processingTaskId === task.id}
                        onClick={() => beginDelegation(task)}
                      >
                        {t('tasks.assignment.delegate')}
                      </button>
                    )}
                <Link className="task-action-button secondary task-open-link" to={`/tasks/${task.taskDefinitionId}`}>Відкрити →</Link>
                </div>

                {historyTaskId === task.id && (
                  <div className="task-history-panel">
                    <strong>{t('tasks.history.title', { defaultValue: 'Історія' })}</strong>
                    {loadingHistoryTaskId === task.id ? (
                      <p>{t('common.loading')}</p>
                    ) : (historyByTaskId[task.id] || []).length === 0 ? (
                      <p>{t('tasks.history.empty', { defaultValue: 'Історія поки порожня.' })}</p>
                    ) : (
                      <div className="task-history-list">
                        {(historyByTaskId[task.id] || []).map((event) => (
                          <div className="task-history-event" key={event.id}>
                            <time>{formatDateTime(event.occurredAt)}</time>
                            <span>{auditEventText(event)}</span>
                            {event.details && <small>{event.details}</small>}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}

                {delegatingTaskId === task.id && (
                  <div className="task-delegation-panel">
                    <select
                      value={delegateToMemberId}
                      onChange={(event) =>
                        setDelegateToMemberId(event.target.value)
                      }
                    >
                      {members
                        .filter(
                          (member) =>
                            member.id !== task.memberId
                        )
                        .map((member) => (
                          <option
                            key={member.id}
                            value={member.id}
                          >
                            {member.name}
                          </option>
                        ))}
                    </select>

                    <button
                      type="button"
                      className="task-action-button primary"
                      disabled={
                        !delegateToMemberId ||
                        processingTaskId === task.id
                      }
                      onClick={() => handleDelegate(task)}
                    >
                      {t('tasks.assignment.delegateConfirm')}
                    </button>

                    <button
                      type="button"
                      className="task-action-button secondary"
                      onClick={() => {
                        setDelegatingTaskId(null)
                        setDelegateToMemberId('')
                      }}
                    >
                      {t('common.cancel', {
                        defaultValue: 'Cancel',
                      })}
                    </button>
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </section>

      <section className={pageTab === 'my' ? 'tasks-panel open-tasks-panel' : 'tasks-panel open-tasks-panel ui-hidden'}>
        <div className="tasks-panel-header">
          <div>
            <h2>{t('tasks.open.title')}</h2>
            <p>
              {t('tasks.open.description', {
                name: selectedMember?.name,
              })}
            </p>
          </div>

          <span className="open-task-count">
            {openTasks.length}
          </span>
        </div>

        {loadingTasks ? (
          <div className="empty-state">
            {t('common.loading')}
          </div>
        ) : openTasks.length === 0 ? (
          <div className="empty-state compact">
            <strong>{t('tasks.open.empty')}</strong>
          </div>
        ) : (
          <div className="task-list">
            {newestFirst(openTasks).map((task) => (
              <article
                className="task-card open-task-card"
                key={`open-${task.id}`}
              >
                <div className="task-card-main">
                  <div className="task-title-row">
                    <h3>{task.title}</h3>

                    <span className="task-open-badge">
                      {t('tasks.open.badge')}
                    </span>

                    {task.mandatory && (
                      <span className="task-mandatory">
                        {t('tasks.mandatory')}
                      </span>
                    )}
                  </div>

                  {task.description && (
                    <p className="task-description">
                      {task.description}
                    </p>
                  )}

                  {renderTaskConditions(task)}

                  <div className="task-assignment-meta">
                    <span>
                      {t('tasks.assignment.scope')}:{' '}
                      <strong>
                        {t(
                          `assignmentPolicy.${task.assignmentPolicy}`,
                          {
                            defaultValue:
                              task.assignmentPolicy,
                          }
                        )}
                      </strong>
                    </span>

                    {task.preferredMemberId && (
                      <span className="preferred-executor">
                        {t('tasks.assignment.preferred')}:{' '}
                        <strong>
                          {memberName(task.preferredMemberId)}
                        </strong>
                      </span>
                    )}

                    {task.delegationAllowed && (
                      <span>
                        {t('tasks.assignment.delegationAllowed')}
                      </span>
                    )}
                  </div>
                </div>

                <div className="task-card-actions">
                  <button
                    type="button"
                    className="task-action-button primary"
                    disabled={
                      processingTaskId === `open-${task.id}`
                    }
                    onClick={() => handleClaim(task)}
                  >
                    {t('tasks.open.claim')}
                  </button>
                </div>

                {definitionHistoryId === task.id && (
                  <div className="task-history-panel">
                    <strong>Історія Definition №{task.id}</strong>
                    {loadingDefinitionHistoryId === task.id ? (
                      <p>{t('common.loading')}</p>
                    ) : (historyByDefinitionId[task.id] || []).length === 0 ? (
                      <p>Історія поки порожня.</p>
                    ) : (
                      <div className="task-history-list">
                        {(historyByDefinitionId[task.id] || []).map((event) => (
                          <div className="task-history-event" key={event.id}>
                            <time>{formatDateTime(event.occurredAt)}</time>
                            <span>{auditEventText(event)}</span>
                            {event.details && <small>{event.details}</small>}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </section>

      <section className={pageTab === 'management' ? 'tasks-panel task-management-panel' : 'tasks-panel task-management-panel ui-hidden'}>
        <div className="tasks-panel-header"><div><h2>{t('tasks.management.title')}</h2><p>{t('tasks.management.description')}</p></div></div>
        <div className="task-management-tabs">
          {['created','admin','observer','executor'].map((view) => (
            <button type="button" key={view} className={managementView === view ? 'secondary-button active' : 'secondary-button'} onClick={() => setManagementView(view)}>
              {t(`tasks.management.${view}`)}
            </button>
          ))}
        </div>
        {managedTasks.length === 0 ? <div className="empty-state compact">{t('tasks.management.empty')}</div> : (
          <div className="task-list">{newestFirst(managedTasks).map((task) => (
            <article className="task-card" key={`managed-${task.id}`}>
              <div className="task-card-main"><div className="task-title-row"><h3>№{task.id} · {task.title}</h3></div>
                {task.description && <p className="task-description">{task.description}</p>}
                <div className="task-participant-summary">
                  {['ADMIN','OBSERVER','EXECUTOR'].map((role) => {
                    const names=(participantMap[task.id] || []).filter((p)=>p.role===role).map((p)=>p.actorName)
                    return names.length ? <span key={role}><strong>{t(`tasks.management.role.${role}`)}:</strong> {names.join(', ')}</span> : null
                  })}
                  <span><strong>{t('tasks.management.status')}:</strong> {task.active ? t('tasks.management.active') : t('tasks.management.inactive')}</span>
                </div>
              </div>
              <div className="task-card-actions management-actions">
                <button
                  type="button"
                  className="secondary-button"
                  disabled={loadingDefinitionHistoryId === task.id}
                  onClick={() => toggleDefinitionHistory(task)}
                >
                  {definitionHistoryId === task.id ? 'Сховати історію' : 'Історія'}
                </button>
                {(managementView === 'created' || managementView === 'admin') && (
                  <>
                    <button type="button" className="secondary-button" onClick={() => setEditingTask(task)}>
                      {t('common.edit')}
                    </button>
                    <button type="button" className="secondary-button" disabled={processingTaskId === `definition-${task.id}`} onClick={() => handleDefinitionActive(task)}>
                      {task.active ? t('tasks.management.deactivate') : t('tasks.management.activate')}
                    </button>
                  </>
                )}
              </div>

              {definitionHistoryId === task.id && (
                <div className="task-history-panel">
                  <strong>Історія Definition №{task.id}</strong>
                  {loadingDefinitionHistoryId === task.id ? (
                    <p>{t('common.loading')}</p>
                  ) : (historyByDefinitionId[task.id] || []).length === 0 ? (
                    <p>Історія поки порожня.</p>
                  ) : (
                    <div className="task-history-list">
                      {(historyByDefinitionId[task.id] || []).map((event) => (
                        <div className="task-history-event" key={event.id}>
                          <time>{formatDateTime(event.occurredAt)}</time>
                          <span>{auditEventText(event)}</span>
                          {event.details && <small>{event.details}</small>}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </article>
          ))}</div>
        )}
      </section>

      {createModalOpen && (
        <CreateTaskModal
          members={members}
          defaultMemberId={selectedMemberId}
          onClose={() => setCreateModalOpen(false)}
          onCreated={() => loadAllTasks()}
        />
      )}

      {editingTask && (
        <CreateTaskModal
          members={members}
          defaultMemberId={selectedMemberId}
          initialTask={editingTask}
          initialParticipants={participantMap[editingTask.id] || []}
          onClose={() => setEditingTask(null)}
          onCreated={async () => {
            setEditingTask(null)
            await loadManagedTasks()
            await loadAllTasks()
          }}
        />
      )}
    </>
  )
}

export default TasksPage
