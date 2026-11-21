import { readRewardPurchaseIntent, rewardPurchaseIntentKey } from './rewardPurchaseIntent.js'
import {
  classifyRewardPurchaseReceiptFailure,
  verifyRewardPurchaseReceipt
} from './rewardPurchaseReceipt.js'

/**
 * Non-mutating reconciliation primitive for a future authenticated receipt GET.
 * Caller supplies a read-only receiptFetcher; no POST, retry, storage write,
 * or pending-intent deletion is performed here.
 *
 * In particular, GET 404 after response loss does NOT prove no charge; only
 * a validated, actor-authorized matching receipt can confirm purchase.
 */
export async function inspectPendingRewardPurchase({ storage, accountId, workspaceId, memberId, receiptFetcher }) {
  const scope = rewardPurchaseIntentKey(accountId, workspaceId, memberId)
  const pending = readRewardPurchaseIntent(storage, scope)
  if (!pending) return Object.freeze({ status: 'NO_VALID_INTENT', receipt: null })
  if (typeof receiptFetcher !== 'function') {
    return Object.freeze({ status: 'READ_UNAVAILABLE_NO_REPLAY', receipt: null, intent: pending })
  }

  try {
    const response = await receiptFetcher(pending.workspaceId, pending.operationId)
    // Fetcher must return the authoritative response body + HTTP status.
    // Never interpret a bare JSON body or a 404 as confirmed success/absence.
    if (!response || response.status !== 200) {
      return Object.freeze({
        status: classifyRewardPurchaseReceiptFailure(response?.status),
        receipt: null, intent: pending
      })
    }
    const receipt = verifyRewardPurchaseReceipt(response.body, pending)
    return Object.freeze({ status: 'CONFIRMED_PURCHASE', receipt, intent: pending })
  } catch {
    return Object.freeze({ status: 'UNKNOWN_NO_REPLAY', receipt: null, intent: pending })
  }
}
