import { useEffect, useMemo, useState } from 'react'

import { useAuth } from '../context/AuthContext'
import { useTranslation } from 'react-i18next'
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
  const { t } = useTranslation()
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

  // Consistent keyboard dismissal for task/reward dialogs; never interrupt a save.
  useEffect(() => {
    if (saving) return
    const onEscape = (event) => {
      if (event.key !== 'Escape' || event.defaultPrevented) return
      event.preventDefault()
      onClose()
    }
    window.addEventListener('keydown', onEscape)
    return () => window.removeEventListener('keydown', onEscape)
  }, [onClose, saving])


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
    if (saving) return

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
      setError(t('rewardNegUi.needReward'))
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
                ? t('rewardNegUi.review')
                : t('rewardNegUi.request')}
            </h2>
            <p>{title}</p>
          </div>
          <button
            type="button"
            className="modal-close-button"
            onClick={onClose}
            disabled={saving}
            aria-label={t("rewardNegUi.close")}
          >
            ×
          </button>
        </div>

        {loading ? (
          <div className="page-loading">{t("common.loading")}</div>
        ) : (
          <form className="task-reward-negotiation-form" onSubmit={submit}>
            {error && <div className="page-error">{error}</div>}

            <section>
              <h3>{t('rewardNegUi.points')}</h3>
              <div className="form-row">
                <label className="form-field">
                  <span>{t('rewardNegUi.type')}</span>
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
                  <span>{t('rewardNegUi.amount')}</span>
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
              <h3>{t('rewardNegUi.reputation')}</h3>
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
              <h3>{t('rewardNegUi.reward')}</h3>
              <label className="form-field">
                <span>{t('rewardNegUi.catalog')}</span>
                <select
                  value={rewardDefinitionId}
                  onChange={(event) => {
                    setRewardDefinitionId(event.target.value)
                    if (event.target.value) setCustomRewardTitle('')
                  }}
                >
                  <option value="">{t('rewardNegUi.none')}</option>
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
                    ? ` · ${t('rewardNegUi.price', { amount: selectedReward.priceAmount })}`
                    : ''}
                  {selectedReward.rewardKind === 'TIME_BASED'
                    ? ` · ${t('rewardNegUi.timeReward')}${selectedReward.defaultDurationMinutes ? ` · ${t('rewardNegUi.defaultDuration', { minutes: selectedReward.defaultDurationMinutes })}` : ''}`
                    : ''}
                </small>
              )}

              {selectedReward?.rewardKind === 'TIME_BASED' && (
                <div className="form-row task-reward-duration-row">
                  <label className="form-field">
                    <span>{t('rewardNegUi.duration')}</span>
                    <select
                      value={durationPreset}
                      onChange={(event) => setDurationPreset(event.target.value)}
                    >
                      <option value="ANY">{t("rewardNegUi.any")}</option>
                      <option value="15">{t("rewardNegUi.minute", { count: 15 })}</option>
                      <option value="30">{t("rewardNegUi.minute", { count: 30 })}</option>
                      <option value="45">{t("rewardNegUi.minute", { count: 45 })}</option>
                      <option value="60">{t("rewardNegUi.oneHour")}</option>
                      <option value="90">{t("rewardNegUi.hourHalf")}</option>
                      <option value="120">{t("rewardNegUi.twoHours")}</option>
                      <option value="CUSTOM">{t("rewardNegUi.customTime")}</option>
                    </select>
                  </label>

                  {durationPreset === 'CUSTOM' && (
                    <label className="form-field">
                      <span>{t('rewardNegUi.minutes')}</span>
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
                <span>{t('rewardNegUi.custom')}</span>
                <input
                  type="text"
                  maxLength={150}
                  value={customRewardTitle}
                  placeholder={t("rewardNegUi.customPlaceholder")}
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
                  ? t('rewardNegUi.adultComment')
                  : t('rewardNegUi.comment')}
              </span>
              <textarea
                rows={3}
                maxLength={500}
                value={comment}
                placeholder={
                  reviewing
                    ? t('rewardNegUi.adultPlaceholder')
                    : t('rewardNegUi.commentPlaceholder')
                }
                onChange={(event) => setComment(event.target.value)}
              />
            </label>

            {reviewing && request && (
              <div className="task-reward-original-request">
                <strong>{t('rewardNegUi.requested')}</strong>
                <span>
                  {request.requestedPointAmount
                    ? `${request.requestedPointAmount} ${t('rewardNegUi.points')}`
                    : t('rewardNegUi.noPoints')}
                  {request.requestedReputationAmount
                    ? ` · +${request.requestedReputationAmount} ${t('rewardNegUi.reputation')}`
                    : ''}
                  {request.requestedRewardTitle
                    ? ` · ${request.requestedRewardTitle}`
                    : ''}
                  {request.requestedDurationMinutes
                    ? ` · ${t('rewardNegUi.minute', { count: request.requestedDurationMinutes })}`
                    : request.requestedRewardDefinitionId
                      ? ` · ${t('rewardNegUi.anyTime')}`
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
                {t('common.cancel')}
              </button>
              <button
                type="submit"
                className="primary-button"
                disabled={saving}
              >
                {saving
                  ? t('rewardNegUi.saving')
                  : reviewing
                    ? t('rewardNegUi.approve')
                    : t('rewardNegUi.send')}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}

export default TaskRewardNegotiationModal
