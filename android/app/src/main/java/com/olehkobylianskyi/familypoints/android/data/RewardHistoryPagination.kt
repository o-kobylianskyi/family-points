package com.olehkobylianskyi.familypoints.android.data

/**
 * Backend purchase events have durable purchase IDs. Keep first appearance when
 * a changing history snapshot overlaps two cursor pages. Never merge request or
 * obligation records into purchase history without a verified event contract.
 */
object RewardHistoryPagination {
    fun append(
        current: List<RewardHistoryPurchase>,
        next: RewardPurchaseHistoryPage
    ): List<RewardHistoryPurchase> {
        require(next.items.all { it.id > 0L && it.memberId > 0L })
        require(!next.hasMore || !next.nextCursor.isNullOrBlank()) {
            "A continuing history page must supply a cursor"
        }
        val seen = current.mapTo(mutableSetOf()) { it.id }
        return current + next.items.filter { seen.add(it.id) }
    }

    fun validStatus(status: String?): Boolean =
        status == null || status in setOf(
            "PENDING_APPROVAL", "READY", "REDEEMED", "CANCELLED"
        )
}
