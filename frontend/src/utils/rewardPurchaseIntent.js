// Local-only immutable reward intent record for the forthcoming authoritative
// purchase receipt contract. Never infer a successful purchase from this record.
// This module does not send a POST, clear any pending intent or guess the API DTO.
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

const positiveId = value => typeof value === 'number' && Number.isSafeInteger(value) && value > 0
export const rewardPurchaseIntentKey = (accountId, workspaceId, memberId) => {
  if (typeof accountId !== 'string' && !positiveId(accountId)) throw new Error('INVALID_PURCHASE_SCOPE')
  if (String(accountId).trim() === '' || !positiveId(Number(workspaceId)) || !positiveId(Number(memberId))) {
    throw new Error('INVALID_PURCHASE_SCOPE')
  }
  return `fp:reward-purchase:intent-v1:${accountId}:${workspaceId}:${memberId}`
}

export function makeRewardPurchaseIntent({ accountId, workspaceId, memberId, rewardId, operationId }) {
  if (!UUID.test(String(operationId)) || !positiveId(Number(rewardId))) throw new Error('INVALID_PURCHASE_INTENT')
  const scope = rewardPurchaseIntentKey(accountId, workspaceId, memberId)
  return Object.freeze({
    version: 1, scope, accountId: String(accountId), workspaceId: Number(workspaceId),
    memberId: Number(memberId), rewardId: Number(rewardId), operationId: String(operationId),
    state: 'UNKNOWN'
  })
}

export function readRewardPurchaseIntent(storage, scope) {
  try {
    const value = JSON.parse(storage.getItem(scope) || 'null')
    if (!value || value.version !== 1 || value.scope !== scope ||
        value.state !== 'UNKNOWN' || !UUID.test(String(value.operationId)) ||
        !positiveId(value.workspaceId) || !positiveId(value.memberId) || !positiveId(value.rewardId) ||
        value.scope !== rewardPurchaseIntentKey(value.accountId, value.workspaceId, value.memberId)) return null
    return Object.freeze(value)
  } catch {
    return null
  }
}

export function saveRewardPurchaseIntent(storage, intent) {
  if (!intent || intent.state !== 'UNKNOWN' ||
      intent.scope !== rewardPurchaseIntentKey(intent.accountId, intent.workspaceId, intent.memberId) ||
      !UUID.test(String(intent.operationId)) || !positiveId(intent.rewardId)) {
    throw new Error('INVALID_PURCHASE_INTENT')
  }
  // A pre-existing operation for the same member must be reconciled by the
  // future authenticated server receipt; do not silently overwrite it.
  if (storage.getItem(intent.scope) !== null) throw new Error('PURCHASE_INTENT_ALREADY_PENDING')
  storage.setItem(intent.scope, JSON.stringify(intent))
}
