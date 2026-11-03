package com.olehkobylianskyi.familypoints.android.data

import org.junit.Assert.*
import org.junit.Test

class RewardHistoryPaginationTest {
    private fun purchase(id: Long, member: Long = 7L) = RewardHistoryPurchase(
        id = id,
        rewardDefinitionId = null,
        memberId = member,
        memberName = "Child",
        rewardTitle = "Original snapshot",
        pointTypeId = null,
        pointTypeCode = "POINTS",
        priceAmount = 10,
        durationMinutes = null,
        status = "READY",
        purchasedAt = "2026-11-01T10:00:00",
        resolvedAt = null
    )

    @Test fun moreThanTenPurchasesRemainVisibleAndOverlapDoesNotDuplicate() {
        val first = RewardPurchaseHistoryPage((21L downTo 7L).map(::purchase), "next", true)
        val second = RewardPurchaseHistoryPage((7L downTo 1L).map(::purchase), null, false)
        val page1 = RewardHistoryPagination.append(emptyList(), first)
        val full = RewardHistoryPagination.append(page1, second)
        assertEquals(15, page1.size)
        assertEquals(21, full.size)
        assertEquals(21L, full.first().id)
        assertEquals(1L, full.last().id)
        assertEquals(full.size, full.map { it.id }.distinct().size)
        assertEquals("Original snapshot", full.first().rewardTitle)
    }

    @Test fun rejectsMissingCursorAndInvalidPurchaseIdentity() {
        try {
            RewardHistoryPagination.append(emptyList(),
                RewardPurchaseHistoryPage(listOf(purchase(1)), null, true))
            fail("Cursor required for continuation")
        } catch (_: IllegalArgumentException) { }
        try {
            RewardHistoryPagination.append(emptyList(),
                RewardPurchaseHistoryPage(listOf(purchase(-1)), null, false))
            fail("Purchase identity is required")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun statusNamesComeFromPersistedBackendEnumOnly() {
        for (status in listOf("PENDING_APPROVAL", "READY", "REDEEMED", "CANCELLED")) {
            assertTrue(RewardHistoryPagination.validStatus(status))
        }
        assertTrue(RewardHistoryPagination.validStatus(null))
        for (unsupported in listOf("DELIVERED", "RECEIVED", "USED", "EXPIRED", "UNKNOWN")) {
            assertFalse(RewardHistoryPagination.validStatus(unsupported))
        }
    }
}
