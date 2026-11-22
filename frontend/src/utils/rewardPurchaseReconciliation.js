import { readRewardPurchaseIntent, rewardPurchaseIntentKey } from './rewardPurchaseIntent.js'
import {
  classifyRewardPurchaseReceiptFailure,
  verifyRewardPurchaseIdentity,
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
  // Legacy v1 lacks immutable original price/unit. Do not infer it from the
  // current catalog or claim a matching receipt proves the original amount.
  if (pending.version !== 2) {
    return Object.freeze({ status: 'LEGACY_SNAPSHOT_MISSING_HOLD', receipt: null, intent: pending })
  }
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
    // A cancelled purchase is still a historical committed purchase. Do not
    // confuse it with missing receipt or authorization to charge again.
    // Validate the exact original identity and authoritative price first.
    const receipt = verifyRewardPurchaseIdentity(response.body, pending)
    if (receipt.purchase.status === 'CANCELLED') {
      return Object.freeze({ status: 'HISTORICAL_PURCHASE_CANCELLED_HOLD', receipt, intent: pending })
    }
    verifyRewardPurchaseReceipt(receipt, pending)
    return Object.freeze({ status: 'CONFIRMED_PURCHASE', receipt, intent: pending })
  } catch {
    return Object.freeze({ status: 'UNKNOWN_NO_REPLAY', receipt: null, intent: pending })
  }
}
