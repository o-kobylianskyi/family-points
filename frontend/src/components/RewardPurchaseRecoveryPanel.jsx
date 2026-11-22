import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getRewardPurchaseOperationReceipt } from '../api/rewardApi'
import { inspectPendingRewardPurchase } from '../utils/rewardPurchaseReconciliation'
import { readRewardPurchaseIntent, rewardPurchaseIntentKey } from '../utils/rewardPurchaseIntent'
import { rewardRecoveryCopy, rewardRecoveryStatusMessage } from '../utils/rewardPurchaseRecoveryCopy'

// Read-only review of an already saved operation. Never creates a new purchase,
// clears an intent, retries POST, or infers a refund from CANCELLED.
export default function RewardPurchaseRecoveryPanel({
  accountId, workspaceId, memberId, getAccessToken,
}) {
  const { i18n } = useTranslation()
  const language = i18n.resolvedLanguage || i18n.language || 'en'
  const copy = rewardRecoveryCopy(language)
  const [pending, setPending] = useState(null)
  const [result, setResult] = useState(null)
  const [checking, setChecking] = useState(false)
  const requestLock = useRef(false)
  const generation = useRef(0)
  const isScoped = accountId != null &&
    Number.isSafeInteger(Number(workspaceId)) && Number(workspaceId) > 0 &&
    Number.isSafeInteger(Number(memberId)) && Number(memberId) > 0

  useEffect(() => {
    const current = ++generation.current
    let active = true
    // Session storage may be unavailable (privacy mode); review stays hidden.
    let record = null
    if (isScoped) {
      try {
        const scope = rewardPurchaseIntentKey(accountId, workspaceId, memberId)
        record = readRewardPurchaseIntent(sessionStorage, scope)
      } catch { /* No usable read-only journal. */ }
    }
    // Defer state updates to avoid synchronous effect setState lint issues.
    Promise.resolve().then(() => {
      if (!active || generation.current !== current) return
      setPending(record)
      setResult(null)
      setChecking(false)
      requestLock.current = false
    })
    return () => { active = false }
  }, [accountId, workspaceId, memberId, isScoped])

  const inspect = async () => {
    if (requestLock.current || !pending || !isScoped) return
    requestLock.current = true
    const current = generation.current
    setChecking(true)
    try {
      const reviewed = await inspectPendingRewardPurchase({
        storage: sessionStorage,
        accountId, workspaceId, memberId,
        receiptFetcher: (family, operationId) =>
          getRewardPurchaseOperationReceipt(getAccessToken(), family, operationId),
      })
      if (current === generation.current) setResult(reviewed)
    } catch {
      if (current === generation.current) {
        setResult({ status: 'UNKNOWN_NO_REPLAY', receipt: null })
      }
    } finally {
      if (current === generation.current) {
        requestLock.current = false
        setChecking(false)
      }
    }
  }

  if (!isScoped || !pending) return null
  const state = result?.status || 'UNKNOWN_NO_REPLAY'
  return (
    <section className="points-history-card" aria-label={copy.heading}>
      <div className="panel-header">
        <div>
          <h2>{copy.heading}</h2>
          <p>{copy.unknown}</p>
        </div>
      </div>
      <p>{copy.operation}: <code>{pending.operationId}</code></p>
      {pending.version === 2 && <p>{copy.originalPrice}: {pending.expectedPriceAmount} {pending.expectedPointTypeCode}</p>}
      {pending.version === 1 && <p role="alert">{copy.legacy}</p>}
      <p role="status">
        {copy.status}: {rewardRecoveryStatusMessage(state, result?.receipt, language)}
      </p>
      <button type="button" className="secondary-button"
        onClick={inspect} disabled={checking}>
        {checking ? copy.checking : copy.check}
      </button>
    </section>
  )
}
