package com.olehkobylianskyi.familypoints.android.data

/**
 * Read-only defense in depth. The Backend enforces workspace membership and ACL.
 * Before rendering a page, also fail closed if the response contradicts the
 * explicitly selected member or the caller's own-member scope.
 *
 * A manager's unfiltered view intentionally allows different member IDs.
 * This validator never converts purchase statuses into receipt/delivery events.
 */
object RewardHistoryPageGuard {
    fun validate(
        page: RewardPurchaseHistoryPage,
        viewerMemberId: Long,
        canManage: Boolean,
        requestedMemberId: Long?,
        requestedStatus: String?
    ): RewardPurchaseHistoryPage {
        require(viewerMemberId > 0L) { "Missing authenticated history viewer" }
        require(requestedMemberId == null || requestedMemberId > 0L)
        require(RewardHistoryPagination.validStatus(requestedStatus))
        val restrictedMember = if (canManage) requestedMemberId else viewerMemberId
        require(page.items.all { row ->
            row.id > 0L &&
                row.memberId > 0L &&
                (restrictedMember == null || row.memberId == restrictedMember) &&
                RewardHistoryPagination.validStatus(row.status) &&
                row.status != null &&
                (requestedStatus == null || row.status == requestedStatus)
        }) { "Reward history returned an invalid or foreign member purchase" }
        require(!page.hasMore || !page.nextCursor.isNullOrBlank()) {
            "Reward history continuation cursor is missing"
        }
        return page
    }
}
