// Wire contract matches Backend B Draft PR57 RewardPurchaseOperationService.Receipt.
// Confirm purchase only in known server purchase lifecycle states (READY/REDEEMED).
// READY does not mean fulfilled; REDEEMED means already redeemed.
const CONFIRMED_PURCHASE_STATES = new Set(['READY', 'REDEEMED'])
// This is deliberately pure validation: no network, no storage clear, no POST retry.
const positive = value => typeof value === 'number' && Number.isSafeInteger(value) && value > 0
const nonNegative = value => typeof value === 'number' && Number.isSafeInteger(value) && value >= 0

export function verifyRewardPurchaseIdentity(receipt, intent) {
  if (!intent || intent.version !== 2 || intent.state !== 'UNKNOWN' ||
      !receipt || typeof receipt !== 'object' ||
      receipt.operationId !== intent.operationId ||
      typeof receipt.replayed !== 'boolean') {
    throw new Error('REWARD_PURCHASE_RECEIPT_INVALID')
  }
  const purchase = receipt.purchase
  if (!purchase || !positive(purchase.id) ||
      !positive(purchase.rewardDefinitionId) ||
      !positive(purchase.memberId) ||
      !positive(purchase.pointTypeId) ||
      !nonNegative(purchase.priceAmount) ||
      String(purchase.rewardDefinitionId) !== String(intent.rewardId) ||
      String(purchase.memberId) !== String(intent.memberId) ||
      typeof purchase.pointTypeCode !== 'string' ||
      purchase.pointTypeCode.trim() === '' ||
      purchase.pointTypeId !== intent.expectedPointTypeId ||
      purchase.pointTypeCode !== intent.expectedPointTypeCode ||
      purchase.priceAmount !== intent.expectedPriceAmount) {
    throw new Error('REWARD_PURCHASE_RECEIPT_INVALID')
  }
  // These identity and price checks run BEFORE interpreting purchase lifecycle.
  return receipt
}

export function verifyRewardPurchaseReceipt(receipt, intent) {
  verifyRewardPurchaseIdentity(receipt, intent)
  if (!CONFIRMED_PURCHASE_STATES.has(receipt.purchase.status)) {
    throw new Error('REWARD_PURCHASE_RECEIPT_INVALID')
  }
  // READY means purchase exists, not delivery. REDEEMED means already used.
  return receipt
}

export function classifyRewardPurchaseReceiptFailure(status) {
  if (status === 404) return 'NOT_FOUND_NO_REPLAY'
  if (status === 401 || status === 403) return 'AUTH_REQUIRED_NO_REPLAY'
  if (status === 409) return 'CONFLICT_NO_REPLAY'
  return 'UNKNOWN_NO_REPLAY'
}
