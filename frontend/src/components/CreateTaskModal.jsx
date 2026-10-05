import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'

import { useAuth } from '../context/AuthContext'
import { getPointTypes } from '../api/economyApi'
import { createTaskDefinition, updateTaskDefinition } from '../api/taskApi'
import { getMemberGroups } from '../api/memberGroupApi'

function getTodayLocalDate() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function normalizeTime(value) {
  const raw = value.trim()

  if (!raw) {
    return ''
  }

  let hours
  let minutes

  if (/^\d{1,2}$/.test(raw)) {
    hours = Number(raw)
    minutes = 0
  } else if (/^\d{3,4}$/.test(raw)) {
    const padded = raw.padStart(4, '0')
    hours = Number(padded.slice(0, 2))
    minutes = Number(padded.slice(2, 4))
  } else {
    const match = raw.match(/^(\d{1,2})[:.](\d{1,2})$/)

    if (!match) {
      return null
    }

    hours = Number(match[1])
    minutes = Number(match[2])
  }

  if (
    !Number.isInteger(hours) ||
    !Number.isInteger(minutes) ||
    hours < 0 ||
    hours > 23 ||
    minutes < 0 ||
    minutes > 59
  ) {
    return null
  }

  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`
}

function CreateTaskModal({
  members,
  existingTaskDefinitions = [],
  defaultMemberId,
  onClose,
  onCreated,
  initialTask = null,
  initialParticipants = [],
  parentTaskDefinitionId = null,
  parentTaskTitle = null,
}) {
  const { t } = useTranslation()
  const { currentUser, getAccessToken } = useAuth()

  const editing = Boolean(initialTask)

  const firstFreeTaskTitle = () => {
    const base = t('tasks.create.defaultTitleBase')
    const used = new Set(
      existingTaskDefinitions
        .filter(task => task.id !== initialTask?.id)
        .map(task => (task.title || '').trim().toLocaleLowerCase())
    )
    let number = 1
    while (used.has(`${base} ${number}`.toLocaleLowerCase())) number += 1
    return `${base} ${number}`
  }
  const [memberGroups, setMemberGroups] = useState([])
  const participantKeys = (role) => initialParticipants
    .filter((p) => p.role === role)
    .map((p) => `${p.actorType}:${p.actorId}`)
  const [administrators, setAdministrators] = useState(() => participantKeys('ADMIN').filter(key => key.startsWith('MEMBER:')))
  const [observers, setObservers] = useState(() => participantKeys('OBSERVER').filter(key => key.startsWith('MEMBER:')))
  const [executors, setExecutors] = useState(() => {
    const existing = participantKeys('EXECUTOR')
    if (existing.length || editing) return existing
    const initialMemberId = defaultMemberId ?? currentUser?.memberId ?? members[0]?.id
    return initialMemberId ? [`MEMBER:${initialMemberId}`] : []
  })

  const [title, setTitle] = useState(() => initialTask?.title ?? firstFreeTaskTitle())
  const [titleTouched, setTitleTouched] = useState(Boolean(initialTask))
  const [description, setDescription] = useState(initialTask?.description ?? '')
  const [mandatory, setMandatory] = useState(initialTask?.mandatory ?? false)
  const [delegationAllowed, setDelegationAllowed] = useState(initialTask?.delegationAllowed ?? false)

  const [recurrenceType, setRecurrenceType] =
    useState(initialTask?.recurrenceType ?? 'ONCE')

  const [startDate, setStartDate] =
    useState(initialTask?.startDate ?? getTodayLocalDate())

  const [endDate, setEndDate] = useState(initialTask?.endDate ?? '')
  const [recurrenceDayOfWeek, setRecurrenceDayOfWeek] =
    useState(String(initialTask?.recurrenceDayOfWeek ?? 1))
  const [recurrenceDayOfMonth, setRecurrenceDayOfMonth] =
    useState(String(initialTask?.recurrenceDayOfMonth ?? 1))
  const [dueTime, setDueTime] = useState(initialTask?.dueTime?.slice?.(0, 5) ?? '')

  const [pointTypes, setPointTypes] = useState([])

  const [rewardPointTypeId, setRewardPointTypeId] =
    useState(initialTask?.rewardPointTypeId ?? '')
  const [rewardAmount, setRewardAmount] = useState(initialTask?.rewardAmount ?? '')

  const [penaltyPointTypeId, setPenaltyPointTypeId] =
    useState(initialTask?.penaltyPointTypeId ?? '')
  const [penaltyAmount, setPenaltyAmount] = useState(initialTask?.penaltyAmount ?? '')

  const [loading, setLoading] = useState(false)
  const [loadingPointTypes, setLoadingPointTypes] =
    useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const loadPointTypes = async () => {
      try {
        setLoadingPointTypes(true)

        const token = getAccessToken()

        const data = await getPointTypes(
          token,
          currentUser.workspaceId
        )

        setPointTypes(data)

        if (!editing && data.length > 0) {
          setRewardPointTypeId(data[0].id)
          setPenaltyPointTypeId(data[0].id)
        }
      } catch (error) {
        setError(error.message)
      } finally {
        setLoadingPointTypes(false)
      }
    }

    loadPointTypes()
  }, [currentUser.workspaceId, getAccessToken])

  useEffect(() => {
    const loadGroups = async () => {
      try {
        const token = getAccessToken()
        const data = await getMemberGroups(token, currentUser.workspaceId)
        setMemberGroups(data)
      } catch (error) {
        setError(error.message)
      }
    }
    loadGroups()
  }, [currentUser.workspaceId, getAccessToken])

  const actorKey = (actorType, actorId) => `${actorType}:${actorId}`
  const toggleActor = (setter, actorType, actorId) => {
    const key = actorKey(actorType, actorId)
    setter((current) => current.includes(key) ? current.filter((item) => item !== key) : [...current, key])
  }
  const actorRefs = (keys) => keys.map((key) => {
    const [actorType, rawId] = key.split(':')
    return { actorType, actorId: Number(rawId) }
  })

  const handleDueTimeBlur = () => {
    const normalized = normalizeTime(dueTime)

    if (normalized !== null) {
      setDueTime(normalized)
    }
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    const trimmedTitle = title.trim()

    if (!trimmedTitle) {
      setError(t('tasks.create.titleRequired'))
      return
    }

    const duplicateTitle = existingTaskDefinitions.some(task =>
      task.id !== initialTask?.id &&
      (task.title || '').trim().toLocaleLowerCase() === trimmedTitle.toLocaleLowerCase()
    )
    if (duplicateTitle) {
      setError(t('tasks.create.duplicateTitle', { title: trimmedTitle }))
      return
    }

    if (executors.length === 0) {
      setError(t('tasks.create.executorRequired'))
      return
    }

    if (!startDate) {
      setError(t('tasks.create.startDateRequired'))
      return
    }

    if (endDate && endDate < startDate) {
      setError(t('tasks.create.endDateInvalid'))
      return
    }

    const normalizedDueTime = normalizeTime(dueTime)

    if (normalizedDueTime === null) {
      setError(t('tasks.create.dueTimeInvalid'))
      return
    }

    const monthlyDay = Number(recurrenceDayOfMonth)

    if (
      recurrenceType === 'MONTHLY' &&
      (
        !Number.isInteger(monthlyDay) ||
        monthlyDay < 1 ||
        monthlyDay > 31
      )
    ) {
      setError(t('tasks.create.monthDayInvalid'))
      return
    }

    try {
      setLoading(true)
      setError('')

      const reward =
        Number(rewardAmount) > 0
          ? Number(rewardAmount)
          : null

      const penalty =
        Number(penaltyAmount) > 0
          ? Number(penaltyAmount)
          : null

      const executorRefs = actorRefs(executors)
      const singleMemberExecutor =
        executorRefs.length === 1 && executorRefs[0].actorType === 'MEMBER'
          ? executorRefs[0]
          : null

      const request = {
        assignmentPolicy: singleMemberExecutor ? 'SINGLE_MEMBER' : 'PARTICIPANTS',
        assignedMemberId: singleMemberExecutor ? singleMemberExecutor.actorId : null,
        targetGroupId: null,
        preferredMemberId: null,
        responsibleMemberId: null,
        parentTaskDefinitionId: editing ? null : parentTaskDefinitionId,
        roleMatchMode: 'ANY',
        requiredGroupRoleIds: [],
        administrators: actorRefs(administrators),
        observers: actorRefs(observers),
        executors: executorRefs,
        title: trimmedTitle,
        description: description.trim() || null,
        mandatory,
        delegationAllowed,
        recurrenceType,

        startDate,
        endDate: endDate || null,

        recurrenceDayOfWeek:
          recurrenceType === 'WEEKLY'
            ? Number(recurrenceDayOfWeek)
            : null,

        recurrenceDayOfMonth:
          recurrenceType === 'MONTHLY'
            ? monthlyDay
            : null,

        dueTime: normalizedDueTime || null,

        rewardPointTypeId:
          reward !== null
            ? Number(rewardPointTypeId)
            : null,

        rewardAmount: reward,

        penaltyPointTypeId:
          penalty !== null
            ? Number(penaltyPointTypeId)
            : null,

        penaltyAmount: penalty,
      }

      const token = getAccessToken()

      const saved = editing
        ? await updateTaskDefinition(token, currentUser.workspaceId, initialTask.id, request)
        : await createTaskDefinition(token, currentUser.workspaceId, request)

      onCreated?.(saved)
      onClose()
    } catch (error) {
      setError(error.message)
    } finally {
      setLoading(false)
    }
  }

  const isOnce = recurrenceType === 'ONCE'
  const isWeekly = recurrenceType === 'WEEKLY'
  const isMonthly = recurrenceType === 'MONTHLY'

  return (
    <div className="modal-backdrop">
      <div className="modal-card create-task-modal">
        <div className="modal-header">
          <div>
            <h2>{editing ? t('tasks.edit.title') : t('tasks.create.title')}</h2>
            <p>{editing ? t('tasks.edit.description') : t('tasks.create.description')}</p>
          </div>

          <button
            type="button"
            className="modal-close-button"
            onClick={onClose}
            aria-label={t('common.close')}
          >
            ×
          </button>
        </div>

        <form
          className="create-task-form"
          onSubmit={handleSubmit}
        >
          {error && (
            <div className="page-error">
              {error}
            </div>
          )}

          {!editing && parentTaskDefinitionId && (
            <div className="task-parent-context">
              <strong>Підзавдання для №{parentTaskDefinitionId}</strong>
              {parentTaskTitle && <span>{parentTaskTitle}</span>}
            </div>
          )}

          <label className="form-field">
            <span>{t('tasks.create.taskTitle')}</span>

            <input
              type="text"
              value={title}
              maxLength={150}
              onChange={(event) => {
                setTitleTouched(true)
                setTitle(event.target.value)
              }}
              required
            />
          </label>

          <label className="form-field">
            <span>{t('tasks.create.taskDescription')}</span>

            <textarea
              value={description}
              maxLength={1000}
              rows={3}
              onChange={(event) =>
                setDescription(event.target.value)
              }
            />
          </label>

          <label className="form-field">
            <span>{t('tasks.create.recurrence')}</span>

            <select
              value={recurrenceType}
              onChange={(event) =>
                setRecurrenceType(event.target.value)
              }
            >
              <option value="ONCE">
                {t('taskRecurrence.ONCE')}
              </option>
              <option value="DAILY">
                {t('taskRecurrence.DAILY')}
              </option>
              <option value="WEEKLY">
                {t('taskRecurrence.WEEKLY')}
              </option>
              <option value="MONTHLY">
                {t('taskRecurrence.MONTHLY')}
              </option>
            </select>
          </label>

          <div className="form-row">
            <label className="form-field">
              <span>
                {isOnce
                  ? t('tasks.create.executionDate')
                  : t('tasks.create.startDate')}
              </span>

              <input
                type="date"
                value={startDate}
                onChange={(event) =>
                  setStartDate(event.target.value)
                }
                required
              />
            </label>

            {!isOnce && (
              <label className="form-field">
                <span>{t('tasks.create.endDate')}</span>

                <input
                  type="date"
                  value={endDate}
                  min={startDate || undefined}
                  onChange={(event) =>
                    setEndDate(event.target.value)
                  }
                />
              </label>
            )}
          </div>

          {isWeekly && (
            <label className="form-field">
              <span>{t('tasks.create.dayOfWeek')}</span>

              <select
                value={recurrenceDayOfWeek}
                onChange={(event) =>
                  setRecurrenceDayOfWeek(event.target.value)
                }
              >
                <option value="1">{t('weekdays.monday')}</option>
                <option value="2">{t('weekdays.tuesday')}</option>
                <option value="3">{t('weekdays.wednesday')}</option>
                <option value="4">{t('weekdays.thursday')}</option>
                <option value="5">{t('weekdays.friday')}</option>
                <option value="6">{t('weekdays.saturday')}</option>
                <option value="7">{t('weekdays.sunday')}</option>
              </select>
            </label>
          )}

          {isMonthly && (
            <label className="form-field">
              <span>{t('tasks.create.dayOfMonth')}</span>

              <input
                type="number"
                min="1"
                max="31"
                step="1"
                value={recurrenceDayOfMonth}
                onChange={(event) =>
                  setRecurrenceDayOfMonth(event.target.value)
                }
                required
              />
            </label>
          )}

          <label className="form-field">
            <span>{t('tasks.create.dueTime')}</span>

            <input
              type="text"
              inputMode="numeric"
              placeholder={t('tasks.create.dueTimePlaceholder')}
              value={dueTime}
              onChange={(event) =>
                setDueTime(event.target.value)
              }
              onBlur={handleDueTimeBlur}
            />

            <small>
              {t('tasks.create.dueTimeHint')}
            </small>
          </label>

          <label className="form-checkbox">
            <input
              type="checkbox"
              checked={mandatory}
              onChange={(event) =>
                setMandatory(event.target.checked)
              }
            />

            <span>
              {t('tasks.create.mandatory')}
            </span>
          </label>

          <label className="form-checkbox">
            <input
              type="checkbox"
              checked={delegationAllowed}
              onChange={(event) =>
                setDelegationAllowed(event.target.checked)
              }
            />

            <span>
              {t('tasks.create.delegationAllowed')}
            </span>
          </label>

          <div className="task-participants-editor">
            <h3>{t('tasks.create.participants')}</h3>
            <p className="role-muted">{t('tasks.create.participantsHint')}</p>
            {[
              ['administrators', administrators, setAdministrators],
              ['observers', observers, setObservers],
              ['executors', executors, setExecutors],
            ].map(([role, selected, setter]) => (
              <div className="participant-role-block" key={role}>
                <strong>{t(`tasks.create.${role}`)}</strong>
                <div className="participant-choice-grid">
                  {members.map((member) => {
                    const key = actorKey('MEMBER', member.id)
                    return (
                      <label className="form-checkbox" key={`${role}-${key}`}>
                        <input
                          type="checkbox"
                          checked={selected.includes(key)}
                          onChange={() => toggleActor(setter, 'MEMBER', member.id)}
                        />
                        <span>{member.name}</span>
                      </label>
                    )
                  })}
                  {role === 'executors' && memberGroups.map((group) => {
                    const key = actorKey('GROUP', group.id)
                    return (
                      <label className="form-checkbox" key={`${role}-${key}`}>
                        <input
                          type="checkbox"
                          checked={selected.includes(key)}
                          onChange={() => toggleActor(setter, 'GROUP', group.id)}
                        />
                        <span>👥 {group.name}</span>
                      </label>
                    )
                  })}
                </div>
                {(role === 'administrators' || role === 'observers') && (
                  <small>{t('tasks.create.memberOnlyParticipants')}</small>
                )}
              </div>
            ))}
            <small>{t('tasks.create.authorHint')}</small>
          </div>

          <div className="task-economy-grid">
            <section className="task-value-section">
              <h3>{t('tasks.create.reward')}</h3>

              <div className="form-row">
                <label className="form-field">
                  <span>{t('tasks.create.pointType')}</span>

                  <select
                    value={rewardPointTypeId}
                    disabled={
                      loadingPointTypes ||
                      pointTypes.length === 0
                    }
                    onChange={(event) =>
                      setRewardPointTypeId(event.target.value)
                    }
                  >
                    {pointTypes.map((pointType) => (
                      <option
                        key={pointType.id}
                        value={pointType.id}
                      >
                        {t(`pointType.${pointType.code}`, { defaultValue: pointType.name })}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="form-field">
                  <span>{t('tasks.create.amount')}</span>

                  <input
                    type="number"
                    min="0"
                    step="1"
                    value={rewardAmount}
                    onChange={(event) =>
                      setRewardAmount(event.target.value)
                    }
                  />
                </label>
              </div>
            </section>

            <section className="task-value-section">
              <h3>{t('tasks.create.penalty')}</h3>

              <div className="form-row">
                <label className="form-field">
                  <span>{t('tasks.create.pointType')}</span>

                  <select
                    value={penaltyPointTypeId}
                    disabled={
                      loadingPointTypes ||
                      pointTypes.length === 0
                    }
                    onChange={(event) =>
                      setPenaltyPointTypeId(event.target.value)
                    }
                  >
                    {pointTypes.map((pointType) => (
                      <option
                        key={pointType.id}
                        value={pointType.id}
                      >
                        {t(`pointType.${pointType.code}`, { defaultValue: pointType.name })}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="form-field">
                  <span>{t('tasks.create.amount')}</span>

                  <input
                    type="number"
                    min="0"
                    step="1"
                    value={penaltyAmount}
                    onChange={(event) =>
                      setPenaltyAmount(event.target.value)
                    }
                  />
                </label>
              </div>
            </section>
          </div>

          <div className="modal-actions">
            <button
              type="button"
              className="secondary-button"
              onClick={onClose}
              disabled={loading}
            >
              {t('common.cancel')}
            </button>

            <button
              type="submit"
              className="primary-button"
              disabled={
                loading ||
                loadingPointTypes ||
                executors.length === 0
              }
            >
              {loading
                ? t('common.saving')
                : editing ? t('tasks.edit.save') : t('tasks.create.submit')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default CreateTaskModal
