// Durable local UNKNOWN intent, not a financial ledger. This module makes no POST.
// v2 adds the immutable original point-unit and price snapshot matching Android.
// Existing v1 journals are retained for read-only manual review; never upgrade
// or fabricate their missing financial snapshot from a mutable catalog.
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
const positiveId = value => typeof value === 'number' && Number.isSafeInteger(value) && value > 0
const nonNegative = value => typeof value === 'number' && Number.isSafeInteger(value) && value >= 0
const snapshotValid = value => positiveId(value?.expectedPointTypeId) &&
  typeof value?.expectedPointTypeCode === 'string' &&
  value.expectedPointTypeCode.trim() !== '' &&
  nonNegative(value?.expectedPriceAmount)

export const rewardPurchaseIntentKey = (accountId, workspaceId, memberId) => {
  if (typeof accountId !== 'string' && !positiveId(accountId)) throw new Error('INVALID_PURCHASE_SCOPE')
  if (String(accountId).trim() === '' || !positiveId(Number(workspaceId)) || !positiveId(Number(memberId))) {
    throw new Error('INVALID_PURCHASE_SCOPE')
  }
  // Preserve the v1 storage key exactly: in-flight UUIDs must survive schema upgrade.
  return `fp:reward-purchase:intent-v1:${accountId}:${workspaceId}:${memberId}`
}

function validBase(value, scope) {
  return !!value && (value.version === 1 || value.version === 2) &&
    value.scope === scope && value.state === 'UNKNOWN' &&
    UUID.test(String(value.operationId)) && positiveId(value.workspaceId) &&
    positiveId(value.memberId) && positiveId(value.rewardId) &&
    value.scope === rewardPurchaseIntentKey(value.accountId, value.workspaceId, value.memberId)
}

export function makeRewardPurchaseIntent({
  accountId, workspaceId, memberId, rewardId, operationId,
  expectedPointTypeId, expectedPointTypeCode, expectedPriceAmount,
}) {
  if (!UUID.test(String(operationId)) || !positiveId(Number(rewardId)) ||
      !snapshotValid({ expectedPointTypeId, expectedPointTypeCode, expectedPriceAmount })) {
    throw new Error('INVALID_PURCHASE_INTENT')
  }
  const scope = rewardPurchaseIntentKey(accountId, workspaceId, memberId)
  return Object.freeze({
    version: 2, scope, accountId: String(accountId), workspaceId: Number(workspaceId),
    memberId: Number(memberId), rewardId: Number(rewardId), operationId: String(operationId),
    expectedPointTypeId, expectedPointTypeCode, expectedPriceAmount, state: 'UNKNOWN',
  })
}

export function readRewardPurchaseIntent(storage, scope) {
  try {
    const value = JSON.parse(storage.getItem(scope) || 'null')
    if (!validBase(value, scope)) return null
    // v1 is inspectable but has no price proof; never silently upgrade it.
    if (value.version === 2 && !snapshotValid(value)) return null
    return Object.freeze(value)
  } catch {
    return null
  }
}

export function saveRewardPurchaseIntent(storage, intent) {
  if (!intent || intent.version !== 2 ||
      intent.scope !== rewardPurchaseIntentKey(intent.accountId, intent.workspaceId, intent.memberId) ||
      !validBase(intent, intent.scope) || !snapshotValid(intent)) {
    throw new Error('INVALID_PURCHASE_INTENT')
  }
  // An unresolved UUID from either v1 or v2 must never be replaced or cleared.
  if (storage.getItem(intent.scope) !== null) throw new Error('PURCHASE_INTENT_ALREADY_PENDING')
  storage.setItem(intent.scope, JSON.stringify(intent))
}
