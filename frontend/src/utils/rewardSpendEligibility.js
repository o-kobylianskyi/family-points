// A catalog entry only describes the reward; the backend remains authoritative
// for balances, member eligibility, limits, cooldown and purchase price.
export function eligibleDirectRewards(rewards) {
  if (!Array.isArray(rewards)) return []
  return rewards.filter(reward =>
    reward &&
    Number.isSafeInteger(reward.id) && reward.id > 0 &&
    typeof reward.title === 'string' && reward.title.trim() &&
    reward.active === true &&
    reward.acquisitionMode === 'DIRECT' &&
    reward.requiresApproval === false &&
    Number.isSafeInteger(reward.priceAmount) && reward.priceAmount >= 0 &&
    Number.isSafeInteger(reward.pointTypeId) && reward.pointTypeId > 0 &&
    typeof reward.pointTypeCode === 'string' && reward.pointTypeCode.trim()
  )
}

// GET /rewards/{id}/availability is a schedule check ONLY. It cannot prove
// member-specific affordability or other purchase requirements.
export function rewardScheduleIsOpen(availability, rewardId) {
  return !!availability &&
    String(availability.rewardId) === String(rewardId) &&
    availability.scheduleOpenNow === true
}
