import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import NumberInput from './NumberInput'
import { POINT_AMOUNT_PRESETS } from '../utils/pointOperationDefaults'
import { eligibleDirectRewards, rewardScheduleIsOpen } from '../utils/rewardSpendEligibility'
import { rewardRecoveryCopy } from '../utils/rewardPurchaseRecoveryCopy'

function PointOperationModal(props) {
  const scope = [props.operation, props.member?.id].join(':')
  return <PointOperationContent key={scope} {...props} />
}
function PointOperationContent({
  operation,
  member,
  onClose,
  onSubmit,
  initialAmount = 5,
  availableBalance = 0,
  canPurchaseRewards = false,
  rewardPurchaseEnabled = false,
  canManagePoints = false,
  loadRewards,
  loadRewardAvailability,
}) {
  const { t, i18n } = useTranslation()

  const [amount, setAmount] = useState(String(initialAmount))
  const [description, setDescription] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const submitLock = useRef(false)
  const [error, setError] = useState('')
  const [spendMode, setSpendMode] = useState(canManagePoints ? 'manual' : 'reward')
  const [rewardId, setRewardId] = useState('')
  const [rewards, setRewards] = useState([])
  const [rewardsLoading, setRewardsLoading] = useState(false)
  const [rewardsError, setRewardsError] = useState(false)
  const [rewardAttempted, setRewardAttempted] = useState(false)
  const [rewardSchedule, setRewardSchedule] = useState(null)
  const [scheduleLoading, setScheduleLoading] = useState(false)
  const [scheduleError, setScheduleError] = useState(false)
  const lang = (i18n.resolvedLanguage || i18n.language || 'en').split('-')[0]
  const messages = ({
    uk: ['Тип списання', 'Інше списання без нагороди', 'Списання за нагороду', 'Виберіть нагороду', 'Завантаження нагород…', 'Помилка завантаження нагород', 'Немає доступних нагород', 'Ціна', 'Запит уже було надіслано. Перевірте історію нагород перед повторною спробою.', 'Потрібне право керування нагородами', 'Отримати нагороду'],
    ru: ['Тип списания', 'Другое списание без награды', 'Списание за награду', 'Выберите награду', 'Загрузка наград…', 'Ошибка загрузки наград', 'Нет доступных наград', 'Цена', 'Запрос уже отправлен. Проверьте историю наград перед повторной попыткой.', 'Нужно право управления наградами', 'Получить награду'],
    de: ['Art der Abbuchung', 'Andere Abbuchung ohne Belohnung', 'Für Belohnung einlösen', 'Belohnung wählen', 'Belohnungen laden…', 'Belohnungen konnten nicht geladen werden', 'Keine Belohnungen verfügbar', 'Preis', 'Anfrage bereits gesendet. Vor erneutem Versuch den Belohnungsverlauf prüfen.', 'Berechtigung zur Belohnungsverwaltung erforderlich', 'Belohnung einlösen'],
    en: ['Spend type', 'Other spend without reward', 'Redeem reward', 'Choose reward', 'Loading rewards…', 'Failed to load rewards', 'No rewards available', 'Cost', 'Request already sent. Check reward history before retrying.', 'Reward management permission required', 'Redeem reward'],
  })[lang] || ['Spend type', 'Other spend without reward', 'Redeem reward', 'Choose reward', 'Loading rewards…', 'Failed to load rewards', 'No rewards available', 'Cost', 'Request already sent. Check reward history before retrying.', 'Reward management permission required', 'Redeem reward']
  const rewardMode = operation === 'spend' && spendMode === 'reward'
  const eligibleRewards = eligibleDirectRewards(rewards)
  const chosenReward = eligibleRewards.find(reward => String(reward.id) === rewardId)
  const chosenRewardId = chosenReward?.id ?? null
  const scheduleConfirmed = chosenReward && rewardScheduleIsOpen(rewardSchedule, chosenReward.id)
  const canSubmitReward = rewardPurchaseEnabled && !!chosenReward && canPurchaseRewards && !rewardAttempted &&
    !rewardsLoading && !rewardsError && !scheduleLoading && !scheduleError && scheduleConfirmed

  const insufficient = operation === 'spend' && Number(amount) > Math.max(0, Number(availableBalance) || 0)

  useEffect(() => {
    if (operation !== 'spend' || !member || !canPurchaseRewards || !loadRewards) return
    let active = true
    Promise.resolve().then(async () => {
      if (!active) return
      setRewardsLoading(true)
      try {
        const result = await loadRewards()
        if (active) setRewards(Array.isArray(result) ? result : [])
      } catch {
        if (active) setRewardsError(true)
      } finally {
        if (active) setRewardsLoading(false)
      }
    })
    return () => { active = false }
  }, [operation, member, canPurchaseRewards, loadRewards])

  useEffect(() => {
    if (!rewardMode || !chosenRewardId || !loadRewardAvailability) {
      return
    }
    let active = true
    Promise.resolve().then(async () => {
      if (!active) return
      setScheduleLoading(true)
      setScheduleError(false)
      setRewardSchedule(null)
      try {
        const availability = await loadRewardAvailability(chosenRewardId)
        if (active) setRewardSchedule(availability)
      } catch {
        if (active) setScheduleError(true)
      } finally {
        if (active) setScheduleLoading(false)
      }
    })
    return () => { active = false }
  }, [rewardMode, chosenRewardId, loadRewardAvailability])

  useEffect(() => {
    if (!operation || !member || submitting) return

    const onEscape = (event) => {
      if (event.key === 'Escape' && !event.defaultPrevented) {
        event.preventDefault()
        onClose()
      }
    }

    window.addEventListener('keydown', onEscape)
    return () => window.removeEventListener('keydown', onEscape)
  }, [operation, member, submitting, onClose])


  if (!operation || !member) {
    return null
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    if (submitLock.current || submitting) return
    if (operation === 'spend' && !rewardMode && !canManagePoints) return

    if (rewardMode) {
      if (!canSubmitReward) return
      submitLock.current = true
      setRewardAttempted(true)
      try {
        setSubmitting(true)
        setError('')
        await onSubmit({ operation, rewardId: chosenReward.id })
        onClose()
      } catch {
        // No server receipt/idempotency: unknown POST result MUST NOT be retried.
        setError(messages[8])
      } finally {
        setSubmitting(false)
        submitLock.current = false
      }
      return
    }

    const numericAmount = Number(amount)

    if (
      !Number.isInteger(numericAmount) ||
      numericAmount <= 0
    ) {
      setError(t('pointOperation.invalidAmount'))
      return
    }

    if (insufficient) {
      setError(t('pointOperation.insufficientBalance', {balance: Math.max(0, Number(availableBalance) || 0)}))
      return
    }

    submitLock.current = true
    try {
      setSubmitting(true)
      setError('')

      await onSubmit({
        operation,
        amount: numericAmount,
        description: description.trim(),
      })

      onClose()
    } catch (error) {
      setError(
        /not enough points|insufficient points/i.test(error?.message || '')
          ? t('pointOperation.insufficientBalance', {balance: Math.max(0, Number(availableBalance) || 0)})
          : (error.message || t('pointOperation.operationFailed'))
      )
    } finally {
      submitLock.current = false
      setSubmitting(false)
    }
  }

  return (
    <div
      className="modal-backdrop"
      onMouseDown={() => { if (!submitting) onClose() }}
    >
      <div
        className="modal-card"
        onMouseDown={(event) =>
          event.stopPropagation()
        }
      >
        <div className="modal-header">
          <div>
            <h2>
              {t(`pointOperation.${operation}.title`)}
            </h2>

            <p>{member.name}</p>
          </div>

          <button
            type="button"
            className="modal-close"
            onClick={onClose}
            disabled={submitting}
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          {operation === 'spend' && <div className="fp-number-field">
            <label htmlFor="point-spend-mode">{messages[0]}</label>
            <select id="point-spend-mode" value={spendMode}
              onChange={event => { setSpendMode(event.target.value); setRewardId(''); setRewardSchedule(null); setError('') }}
              disabled={submitting || rewardAttempted}>
              <option value="manual" disabled={!canManagePoints}>{messages[1]}</option>
              <option value="reward">{messages[2]}</option>
            </select>
            {rewardMode && <>
              {!canPurchaseRewards && <p role="alert">{messages[9]}</p>}
              {!rewardPurchaseEnabled && <p role="status">{rewardRecoveryCopy(lang).blocked}</p>}
              {rewardsLoading && <p role="status">{messages[4]}</p>}
              {rewardsError && <p role="alert">{messages[5]}</p>}
              {!rewardsLoading && !rewardsError && !eligibleRewards.length && <p role="status">{messages[6]}</p>}
              <label htmlFor="point-spend-reward">{messages[3]}</label>
              <select id="point-spend-reward" value={rewardId}
                onChange={event => { setRewardId(event.target.value); setRewardSchedule(null); setScheduleError(false); setError('') }}
                disabled={submitting || rewardAttempted || !canPurchaseRewards || rewardsLoading || rewardsError}>
                <option value="">{messages[3]}</option>
                {eligibleRewards.map(reward =>
                  <option key={reward.id} value={reward.id}>
                    {reward.title} — {reward.priceAmount} {reward.pointTypeCode || ''}
                  </option>)}
              </select>
              {chosenReward && <p role="status">{messages[7]}: {chosenReward.priceAmount} {chosenReward.pointTypeCode || ''}</p>}
              {chosenReward && <p className="point-operation-reward-note">{({
                uk:'Розклад перевірено окремо. Остаточну доступність, залишок балів, ліміти та ціну перевіряє сервер під час придбання.',
                ru:'Расписание проверяется отдельно. Окончательную доступность, баланс баллов, лимиты и цену сервер проверяет при покупке.',
                de:'Der Zeitplan wird separat geprüft. Verfügbarkeit, Punktestand, Limits und Preis prüft der Server beim Kauf endgültig.',
                en:'Schedule is checked separately. The server verifies final availability, points balance, limits, and price when purchasing.'
              })[lang] || 'The server verifies final reward eligibility and price when purchasing.'}</p>}
              {chosenReward && scheduleLoading && <p role="status">{({uk:'Перевірка часу доступності…',ru:'Проверка времени доступности…',de:'Verfügbarkeit wird geprüft…',en:'Checking schedule availability…'})[lang] || 'Checking schedule availability…'}</p>}
              {chosenReward && scheduleError && <p role="alert">{({uk:'Не вдалося перевірити доступність нагороди. Списання заблоковано.',ru:'Не удалось проверить доступность награды. Списание заблокировано.',de:'Verfügbarkeit konnte nicht geprüft werden. Einlösung gesperrt.',en:'Could not verify reward availability. Redemption blocked.'})[lang] || 'Could not verify reward availability.'}</p>}
              {chosenReward && !scheduleLoading && !scheduleError && rewardSchedule && !scheduleConfirmed && <p role="alert">{({uk:'Нагорода зараз недоступна за розкладом.',ru:'Награда сейчас недоступна по расписанию.',de:'Belohnung ist derzeit zeitlich nicht verfügbar.',en:'Reward is not available at this time.'})[lang] || 'Reward unavailable now.'}</p>}

            </>}
          </div>}
          {!rewardMode && <div className="fp-number-field">
            <label htmlFor="point-operation-amount">{t('pointOperation.amount')}</label>
            <NumberInput
              id="point-operation-amount"
              min="1"
              step="1"
              stepperStep={5}
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
              autoFocus
              disabled={submitting}
              aria-label={t('pointOperation.amount')}
            />
            <label htmlFor="point-operation-quick-amount" style={{display:'block',marginTop:8}}>
              {t('pointOperation.quickAmount')}
            </label>
            <select id="point-operation-quick-amount"
              value={POINT_AMOUNT_PRESETS.includes(Number(amount)) ? String(amount) : ''}
              onChange={event => {setAmount(event.target.value);setError('')}}
              disabled={submitting}>
              <option value="" disabled>{t('pointOperation.customAmount')}</option>
              {POINT_AMOUNT_PRESETS.map(value => <option key={value} value={value}>{value}</option>)}
            </select>
            {operation === 'spend' && insufficient && <p role="status" className="point-operation-balance-warning">
              {t('pointOperation.insufficientBalance', {balance: Math.max(0, Number(availableBalance) || 0)})}
            </p>}
          </div>}

          {!rewardMode && <label>
            {t('pointOperation.description')}

            <textarea
              rows="3"
              value={description}
              onChange={(event) =>
                setDescription(event.target.value)
              }
              disabled={submitting}
            />
          </label>}

          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          <div className="modal-actions">
            <button
              type="button"
              className="secondary-button"
              onClick={onClose}
              disabled={submitting}
            >
              {t('common.cancel')}
            </button>

            <button
              type="submit"
              className={`primary-button ${operation}`}
              disabled={submitting || (rewardMode ? !canSubmitReward : (operation === 'spend' && !canManagePoints) || insufficient)}
            >
              {submitting
                ? t('common.saving')
                : rewardMode ? messages[10] : t(`pointOperation.${operation}.submit`)}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default PointOperationModal