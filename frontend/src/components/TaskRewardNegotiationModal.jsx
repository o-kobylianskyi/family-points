import { useEffect, useMemo, useState } from 'react'

import { useAuth } from '../context/AuthContext'
import { getPointTypes } from '../api/economyApi'
import { getRewards } from '../api/rewardApi'
import {
  approveTaskRewardRequest,
  createTaskRewardRequest,
} from '../api/taskApi'

function TaskRewardNegotiationModal({
  task,
  request = null,
  mode = 'create',
  onClose,
  onSaved,
}) {
  const { currentUser, getAccessToken } = useAuth()
  const reviewing = mode === 'review'

  const [pointTypes, setPointTypes] = useState([])
  const [rewards, setRewards] = useState([])
  const [pointTypeId, setPointTypeId] = useState('')
  const [pointAmount, setPointAmount] = useState('')
  const [reputationAmount, setReputationAmount] = useState('')
  const [rewardDefinitionId, setRewardDefinitionId] = useState('')
  const [customRewardTitle, setCustomRewardTitle] = useState('')
  const [durationPreset, setDurationPreset] = useState('ANY')
  const [customDurationMinutes, setCustomDurationMinutes] = useState('')
  const [comment, setComment] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const title = task?.title || request?.taskTitle || ''

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true)
        const token = getAccessToken()
        const [pointData, rewardData] = await Promise.all([
          getPointTypes(token, currentUser.workspaceId),
          getRewards(token, currentUser.workspaceId),
        ])

        setPointTypes(pointData)
        setRewards(rewardData)

        if (reviewing && request) {
          setPointTypeId(
            request.requestedPointTypeId
              ? String(request.requestedPointTypeId)
              : pointData[0]?.id
                ? String(pointData[0].id)
                : ''
          )
          setPointAmount(
            request.requestedPointAmount == null
              ? ''
              : String(request.requestedPointAmount)
          )
          setReputationAmount(
            request.requestedReputationAmount == null
              ? ''
              : String(request.requestedReputationAmount)
          )
          setRewardDefinitionId(
            request.requestedRewardDefinitionId
              ? String(request.requestedRewardDefinitionId)
              : ''
          )
          setCustomRewardTitle(request.requestedCustomRewardTitle || '')
          const requestedDuration = request.requestedDurationMinutes
          if (requestedDuration == null) {
            setDurationPreset('ANY')
            setCustomDurationMinutes('')
          } else if ([15, 30, 45, 60, 90, 120].includes(requestedDuration)) {
            setDurationPreset(String(requestedDuration))
            setCustomDurationMinutes('')
          } else {
            setDurationPreset('CUSTOM')
            setCustomDurationMinutes(String(requestedDuration))
          }
          setComment(request.requestedComment || '')
        } else {
          setPointTypeId(
            task?.rewardPointTypeId
              ? String(task.rewardPointTypeId)
              : pointData[0]?.id
                ? String(pointData[0].id)
                : ''
          )
          setPointAmount(String(task?.rewardAmount ?? 0))
          setReputationAmount(String(task?.rewardReputationAmount ?? 0))
          setDurationPreset('ANY')
          setCustomDurationMinutes('')
        }
      } catch (loadError) {
        setError(loadError.message)
      } finally {
        setLoading(false)
      }
    }

    load()
  }, [currentUser.workspaceId, getAccessToken, reviewing, request, task])

  const selectedReward = useMemo(
    () => rewards.find((reward) => String(reward.id) === rewardDefinitionId),
    [rewards, rewardDefinitionId]
  )

  const submit = async (event) => {
    event.preventDefault()

    const durationMinutes =
      durationPreset === 'ANY'
        ? null
        : durationPreset === 'CUSTOM'
          ? (Number(customDurationMinutes) > 0 ? Number(customDurationMinutes) : null)
          : Number(durationPreset)

    const payload = {
      pointTypeId:
        Number(pointAmount) > 0 && pointTypeId
          ? Number(pointTypeId)
          : null,
      pointAmount:
        Number(pointAmount) > 0
          ? Number(pointAmount)
          : null,
      reputationAmount:
        Number(reputationAmount) > 0
          ? Number(reputationAmount)
          : null,
      rewardDefinitionId:
        rewardDefinitionId
          ? Number(rewardDefinitionId)
          : null,
      durationMinutes:
        selectedReward?.rewardKind === 'TIME_BASED'
          ? durationMinutes
          : null,
      customRewardTitle:
        customRewardTitle.trim() || null,
      comment: comment.trim() || null,
    }

    if (
      !payload.pointAmount
      && !payload.reputationAmount
      && !payload.rewardDefinitionId
      && !payload.customRewardTitle
    ) {
      setError('Вкажіть хоча б один варіант винагороди.')
      return
    }

    try {
      setSaving(true)
      setError('')
      const token = getAccessToken()

      if (reviewing) {
        await approveTaskRewardRequest(
          token,
          currentUser.workspaceId,
          request.id,
          payload
        )
      } else {
        await createTaskRewardRequest(
          token,
          currentUser.workspaceId,
          task.id,
          payload
        )
      }

      onSaved?.()
      onClose()
    } catch (saveError) {
      setError(saveError.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="modal-backdrop">
      <div className="modal-card task-reward-negotiation-modal">
        <div className="modal-header">
          <div>
            <h2>
              {reviewing
                ? 'Розглянути зміну винагороди'
                : 'Запросити іншу винагороду'}
            </h2>
            <p>{title}</p>
          </div>
          <button
            type="button"
            className="modal-close-button"
            onClick={onClose}
            aria-label="Закрити"
          >
            ×
          </button>
        </div>

        {loading ? (
          <div className="page-loading">Завантаження…</div>
        ) : (
          <form className="task-reward-negotiation-form" onSubmit={submit}>
            {error && <div className="page-error">{error}</div>}

            <section>
              <h3>Бали</h3>
              <div className="form-row">
                <label className="form-field">
                  <span>Тип балів</span>
                  <select
                    value={pointTypeId}
                    onChange={(event) => setPointTypeId(event.target.value)}
                  >
                    {pointTypes.map((pointType) => (
                      <option key={pointType.id} value={pointType.id}>
                        {pointType.name}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="form-field">
                  <span>Кількість</span>
                  <input
                    type="number"
                    min="0"
                    step="10"
                    value={pointAmount}
                    onChange={(event) => setPointAmount(event.target.value)}
                  />
                </label>
              </div>
            </section>

            <section>
              <h3>Репутація</h3>
              <label className="form-field">
                <span>Кількість</span>
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={reputationAmount}
                  onChange={(event) => setReputationAmount(event.target.value)}
                />
              </label>
            </section>

            <section>
              <h3>Нагорода</h3>
              <label className="form-field">
                <span>З каталогу</span>
                <select
                  value={rewardDefinitionId}
                  onChange={(event) => {
                    setRewardDefinitionId(event.target.value)
                    if (event.target.value) setCustomRewardTitle('')
                  }}
                >
                  <option value="">Без нагороди з каталогу</option>
                  {rewards.map((reward) => (
                    <option key={reward.id} value={reward.id}>
                      {reward.title}
                    </option>
                  ))}
                </select>
              </label>

              {selectedReward && (
                <small className="role-muted">
                  {selectedReward.title}
                  {selectedReward.priceAmount != null
                    ? ` · звичайна ціна ${selectedReward.priceAmount}`
                    : ''}
                  {selectedReward.rewardKind === 'TIME_BASED'
                    ? ` · часова нагорода${selectedReward.defaultDurationMinutes ? ` · типово ${selectedReward.defaultDurationMinutes} хв` : ''}`
                    : ''}
                </small>
              )}

              {selectedReward?.rewardKind === 'TIME_BASED' && (
                <div className="form-row task-reward-duration-row">
                  <label className="form-field">
                    <span>Час</span>
                    <select
                      value={durationPreset}
                      onChange={(event) => setDurationPreset(event.target.value)}
                    >
                      <option value="ANY">Будь-який</option>
                      <option value="15">15 хв</option>
                      <option value="30">30 хв</option>
                      <option value="45">45 хв</option>
                      <option value="60">1 година</option>
                      <option value="90">1 год 30 хв</option>
                      <option value="120">2 години</option>
                      <option value="CUSTOM">Інший час…</option>
                    </select>
                  </label>

                  {durationPreset === 'CUSTOM' && (
                    <label className="form-field">
                      <span>Хвилин</span>
                      <input
                        type="number"
                        min="1"
                        value={customDurationMinutes}
                        onChange={(event) => setCustomDurationMinutes(event.target.value)}
                      />
                    </label>
                  )}
                </div>
              )}

              <label className="form-field">
                <span>Або свій варіант</span>
                <input
                  type="text"
                  maxLength={150}
                  value={customRewardTitle}
                  placeholder="Наприклад: ще 30 хвилин гри"
                  onChange={(event) => {
                    setCustomRewardTitle(event.target.value)
                    if (event.target.value) setRewardDefinitionId('')
                  }}
                />
              </label>
            </section>

            <label className="form-field">
              <span>
                {reviewing
                  ? 'Коментар / умови від дорослого'
                  : 'Коментар'}
              </span>
              <textarea
                rows={3}
                maxLength={500}
                value={comment}
                placeholder={
                  reviewing
                    ? 'Наприклад: добре, але максимум 30 балів'
                    : 'Наприклад: зроблю, якщо дозволиш ще пограти'
                }
                onChange={(event) => setComment(event.target.value)}
              />
            </label>

            {reviewing && request && (
              <div className="task-reward-original-request">
                <strong>Просив виконавець:</strong>
                <span>
                  {request.requestedPointAmount
                    ? `${request.requestedPointAmount} балів`
                    : 'без балів'}
                  {request.requestedReputationAmount
                    ? ` · +${request.requestedReputationAmount} репутації`
                    : ''}
                  {request.requestedRewardTitle
                    ? ` · ${request.requestedRewardTitle}`
                    : ''}
                  {request.requestedDurationMinutes
                    ? ` · ${request.requestedDurationMinutes} хв`
                    : request.requestedRewardDefinitionId
                      ? ' · час: будь-який'
                      : ''}
                  {request.requestedCustomRewardTitle
                    ? ` · ${request.requestedCustomRewardTitle}`
                    : ''}
                </span>
              </div>
            )}

            <div className="modal-actions">
              <button
                type="button"
                className="secondary-button"
                onClick={onClose}
                disabled={saving}
              >
                Скасувати
              </button>
              <button
                type="submit"
                className="primary-button"
                disabled={saving}
              >
                {saving
                  ? 'Збереження…'
                  : reviewing
                    ? 'Схвалити умови'
                    : 'Надіслати запит'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}

export default TaskRewardNegotiationModal
