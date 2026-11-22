package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.storage.RewardPurchaseIntentStore
import java.util.UUID

/**
 * Pure candidate-side validation. The backend remains the only price/ledger
 * authority. This does not persist intent, submit POST, or authorize a debit.
 */
internal object RewardPurchaseIntentPlanner {
    data class ActorScope(
        val accountId: Long,
        val workspaceId: Long,
        val ownMemberId: Long,
        val dataEpoch: Long,
        val canManageRewards: Boolean,
        val online: Boolean
    )

    fun prepare(
        scope: ActorScope,
        reward: RewardDefinitionSummary,
        targetMemberId: Long,
        validTypeId: Long,
        validTypeCode: String,
        operationId: UUID
    ): RewardPurchaseIntentStore.Intent? {
        if (!scope.online || scope.accountId <= 0 || scope.workspaceId <= 0 ||
            scope.ownMemberId <= 0 || scope.dataEpoch < 0 ||
            targetMemberId <= 0 || reward.id <= 0 ||
            !reward.active || reward.requiresApproval ||
            reward.acquisitionMode == "REQUEST" ||
            reward.priceAmount < 0 || reward.pointTypeId <= 0 ||
            reward.pointTypeId != validTypeId ||
            reward.pointTypeCode.isBlank() || reward.pointTypeCode != validTypeCode) return null
        val route = if (targetMemberId == scope.ownMemberId) "SELF" else {
            if (!scope.canManageRewards) return null
            "MANAGER"
        }
        return RewardPurchaseIntentStore.Intent(
            operationId, scope.accountId, scope.workspaceId, targetMemberId,
            reward.id, scope.dataEpoch, route, "PREPARED",
            reward.pointTypeId, reward.pointTypeCode, reward.priceAmount
        )
    }

    const val FINANCIAL_POST_GATE_OPEN = false
}
