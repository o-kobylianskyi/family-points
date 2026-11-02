package com.olehkobylianskyi.familypoints.android.data

/** Backend /rewards/history currently exposes purchase events only. */
data class RewardHistoryPurchase(
    val id: Long,
    val rewardDefinitionId: Long?,
    val memberId: Long,
    val memberName: String?,
    val rewardTitle: String,
    val pointTypeId: Long?,
    val pointTypeCode: String?,
    val priceAmount: Int,
    val durationMinutes: Int?,
    val status: String,
    val purchasedAt: String?,
    val resolvedAt: String?
)

data class RewardPurchaseHistoryPage(
    val items: List<RewardHistoryPurchase>,
    val nextCursor: String?,
    val hasMore: Boolean
)
