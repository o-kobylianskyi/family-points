package com.olehkobylianskyi.familypoints.android.ui.screens

import com.olehkobylianskyi.familypoints.android.data.RewardDefinitionSummary
import com.olehkobylianskyi.familypoints.android.data.PointTypeResponse

/**
 * Reward purchases must never degrade into generic manual SPEND.
 * Financial purchase is disabled until Backend/QA/Architecture explicitly accept
 * the idempotent contract. Manual debit remains independent.
 */
internal object RewardSpendSelectionPolicy {
    const val PURCHASE_WRITES_ENABLED = false

    enum class Action { MANUAL_SPEND, REWARD_PURCHASE_DISABLED, INVALID }

    fun action(rewardSelected: Boolean, manualAmount: Int?, selectedTypeId: Long?): Action =
        when {
            rewardSelected -> Action.REWARD_PURCHASE_DISABLED
            manualAmount == null || manualAmount !in 1..100_000 ||
                selectedTypeId == null || selectedTypeId <= 0 -> Action.INVALID
            else -> Action.MANUAL_SPEND
        }


    fun catalog(
        rewards: List<RewardDefinitionSummary>,
        types: List<PointTypeResponse>,
        canManageRewards: Boolean,
        online: Boolean
    ): List<RewardDefinitionSummary> {
        if (!canManageRewards || !online) return emptyList()
        return rewards.filter { reward ->
            reward.active && reward.id > 0 &&
                reward.priceAmount >= 0 && reward.pointTypeId > 0 &&
                reward.pointTypeCode.isNotBlank() &&
                reward.acquisitionMode != "REQUEST" && !reward.requiresApproval &&
                types.any { it.id == reward.pointTypeId && it.code == reward.pointTypeCode }
        }
    }
}
