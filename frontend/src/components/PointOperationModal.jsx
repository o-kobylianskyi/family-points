import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'

function PointOperationModal({
  operation,
  member,
  onClose,
  onSubmit,
}) {
  const { t } = useTranslation()

  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setAmount('')
    setDescription('')
    setError('')
  }, [operation, member?.id])

  if (!operation || !member) {
    return null
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    const numericAmount = Number(amount)

    if (
      !Number.isInteger(numericAmount) ||
      numericAmount <= 0
    ) {
      setError(t('pointOperation.invalidAmount'))
      return
    }

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
        error.message ||
          t('pointOperation.operationFailed')
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div
      className="modal-backdrop"
      onMouseDown={onClose}
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
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <label>
            {t('pointOperation.amount')}

            <input
              type="number"
              min="1"
              step="1"
              value={amount}
              onChange={(event) =>
                setAmount(event.target.value)
              }
              autoFocus
              disabled={submitting}
            />
          </label>

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
              disabled={submitting}
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