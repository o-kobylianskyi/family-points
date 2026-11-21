import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import NumberInput from './NumberInput'
import { POINT_AMOUNT_PRESETS } from '../utils/pointOperationDefaults'

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
}) {
  const { t } = useTranslation()

  const [amount, setAmount] = useState(String(initialAmount))
  const [description, setDescription] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const submitLock = useRef(false)
  const [error, setError] = useState('')
  const insufficient = operation === 'spend' && Number(amount) > Math.max(0, Number(availableBalance) || 0)

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
          <div className="fp-number-field">
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
          </div>

          <label>
            {t('pointOperation.description')}

            <textarea
              rows="3"
              value={description}
              onChange={(event) =>
                setDescription(event.target.value)
              }
              disabled={submitting}
            />
          </label>

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
              disabled={submitting || insufficient}
            >
              {submitting
                ? t('common.saving')
                : t(`pointOperation.${operation}.submit`)}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default PointOperationModal