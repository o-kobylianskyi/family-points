// Purchase-only cursor pagination; a server receipt/delivery status must not
// be invented from purchase state. Keep IDs stable across overlapping pages.
export const PURCHASE_STATUSES = Object.freeze(['PENDING_APPROVAL','READY','REDEEMED','CANCELLED'])
export function validateRewardHistoryPage(page) {
  if (!page || !Array.isArray(page.items) || typeof page.hasMore !== 'boolean' ||
      (page.nextCursor != null && typeof page.nextCursor !== 'string') ||
      (page.hasMore && !page.nextCursor) ||
      page.items.some(item => !Number.isSafeInteger(item?.id) || item.id <= 0 ||
        !PURCHASE_STATUSES.includes(item.status))) throw new Error('INVALID_PURCHASE_HISTORY_PAGE')
  return page
}
// Defense-in-depth: a history consumer must not display a sibling row even
// if a malformed server response slips through an otherwise authorized GET.
export function verifyRewardHistoryMemberScope(page, {canManage, currentMemberId, selectedMemberId = ''}) {
  const expected = canManage
    ? (selectedMemberId === '' ? null : String(selectedMemberId))
    : String(currentMemberId)
  if (expected != null && page.items.some(item => String(item.memberId) !== expected)) {
    throw new Error('REWARD_HISTORY_MEMBER_SCOPE_MISMATCH')
  }
  return page
}
export function appendRewardHistory(current, incoming) {
  const seen = new Set(current.map(item=>item.id))
  return [...current, ...incoming.filter(item=>!seen.has(item.id))]
}
