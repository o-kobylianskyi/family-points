package com.olehkobylianskyi.familypoints.android.data

import org.junit.Assert.*
import org.junit.Test

class RewardHistoryPageGuardTest {
    private fun purchase(id: Long, member: Long, status: String = "READY") =
        RewardHistoryPurchase(
            id = id, rewardDefinitionId = null, memberId = member,
            memberName = "Member", rewardTitle = "Purchased snapshot",
            pointTypeId = 1L, pointTypeCode = "POINTS", priceAmount = 15,
            durationMinutes = null, status = status,
            purchasedAt = "2026-11-02T12:00:00", resolvedAt = null
        )

    private fun page(vararg rows: RewardHistoryPurchase) =
        RewardPurchaseHistoryPage(rows.toList(), nextCursor = null, hasMore = false)

    private fun rejected(block: () -> Unit) {
        try { block(); fail("History row must be rejected") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun childSeesOnlyOwnRowsEvenIfBackendLeaksSibling() {
        RewardHistoryPageGuard.validate(page(purchase(11, 7)), 7, false, null, null)
        rejected { RewardHistoryPageGuard.validate(page(purchase(12, 8)), 7, false, null, null) }
        rejected { RewardHistoryPageGuard.validate(page(purchase(11, 7), purchase(12, 8)), 7, false, 7, null) }
    }

    @Test fun managerFilteredChildMustNotMixSiblingOnAnyPage() {
        RewardHistoryPageGuard.validate(page(purchase(12, 8)), 7, true, 8, null)
        rejected { RewardHistoryPageGuard.validate(page(purchase(12, 8), purchase(13, 9)), 7, true, 8, null) }
        val mixed = page(purchase(12, 8), purchase(13, 9))
        assertEquals(2, RewardHistoryPageGuard.validate(mixed, 7, true, null, null).items.size)
    }

    @Test fun purchaseStatusFilterIsVerifiedRatherThanTrusted() {
        RewardHistoryPageGuard.validate(page(purchase(11, 7, "REDEEMED")), 7, false, null, "REDEEMED")
        rejected { RewardHistoryPageGuard.validate(page(purchase(11, 7, "READY")), 7, false, null, "REDEEMED") }
        rejected { RewardHistoryPageGuard.validate(page(purchase(11, 7, "DELIVERED")), 7, false, null, null) }
        rejected { RewardHistoryPageGuard.validate(page(purchase(11, 7, "READY")), 7, false, null, "RECEIVED") }
    }

    @Test fun missingCursorOrMalformedIdentityFailsClosed() {
        rejected {
            RewardHistoryPageGuard.validate(
                RewardPurchaseHistoryPage(listOf(purchase(11, 7)), null, true), 7, false, null, null
            )
        }
        rejected { RewardHistoryPageGuard.validate(page(purchase(0, 7)), 7, false, null, null) }
        rejected { RewardHistoryPageGuard.validate(page(purchase(11, 0)), 7, false, null, null) }
        rejected { RewardHistoryPageGuard.validate(page(purchase(11, 7)), 0, false, null, null) }
    }

    @Test fun readOnlyPurchaseHistoryRemainsUnmodifiedByValidation() {
        val original = page(purchase(15, 7))
        val validated = RewardHistoryPageGuard.validate(original, 7, false, null, null)
        assertSame(original, validated)
        assertEquals("Purchased snapshot", validated.items[0].rewardTitle)
    }
}
