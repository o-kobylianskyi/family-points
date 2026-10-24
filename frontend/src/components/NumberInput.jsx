import { useTranslation } from 'react-i18next'

/** Shared numeric field with an integrated stepper and one outer border. */
export default function NumberInput({ value, onChange, min, max, step = 1, disabled = false, ...props }) {
  const { t } = useTranslation()
  const amount = Number(value)
  const lower = min === undefined ? -Infinity : Number(min)
  const upper = max === undefined ? Infinity : Number(max)
  const delta = Number(step) > 0 ? Number(step) : 1
  const isFilled = value !== '' && value !== null && value !== undefined && Number.isFinite(amount)

  const adjust = (direction) => {
    const next = isFilled ? amount + direction * delta : (Number.isFinite(lower) ? lower : 0)
    const clamped = Math.max(lower, Math.min(upper, next))
    // Keep the existing controlled form's event-based onChange contract.
    onChange?.({ target: { value: String(clamped) } })
  }

  return (
    <div className="fp-number-control fp-number-control-shared">
      <input {...props} type="number" min={min} max={max} step={step}
        value={value} onChange={onChange} disabled={disabled} />
      <div className="fp-number-stepper" role="group" aria-label={props['aria-label'] || 'Number controls'}>
        <button type="button" aria-label={t('numberInput.increase', { field: props['aria-label'] || '' })} onClick={() => adjust(1)}
          disabled={disabled || (isFilled && amount >= upper)}><span className="fp-number-arrow" aria-hidden="true">▲</span></button>
        <button type="button" aria-label={t('numberInput.decrease', { field: props['aria-label'] || '' })} onClick={() => adjust(-1)}
          disabled={disabled || !isFilled || amount <= lower}><span className="fp-number-arrow" aria-hidden="true">▼</span></button>
      </div>
    </div>
  )
}
