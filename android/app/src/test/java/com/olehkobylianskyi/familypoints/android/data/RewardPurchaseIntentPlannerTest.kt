package com.olehkobylianskyi.familypoints.android.data

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RewardPurchaseIntentPlannerTest {
    private val uuid = UUID.fromString("60a9f6ef-5a39-4302-a539-7eabebc593ad")
    private val reward = RewardDefinitionSummary(
        id = 50, title = "30 minutes", description = null,
        pointTypeId = 7, pointTypeCode = "POINTS", pointTypeName = "Points",
        priceAmount = 10, minimumReputation = null, requiresApproval = false,
        rewardKind = "TIME", defaultDurationMinutes = 30,
        acquisitionMode = "DIRECT", categories = emptyList(),
        requirements = emptyList(), active = true
    )
    private val scope = RewardPurchaseIntentPlanner.ActorScope(
        accountId = 11, workspaceId = 12, ownMemberId = 13,
        dataEpoch = 14, canManageRewards = false, online = true
    )

    private fun plan(
        s: RewardPurchaseIntentPlanner.ActorScope = scope,
        r: RewardDefinitionSummary = reward,
        target: Long = 13,
        typeId: Long = 7,
        code: String = "POINTS"
    ) = RewardPurchaseIntentPlanner.prepare(s, r, target, typeId, code, uuid)

    @Test fun ownMemberIntentPreservesOriginalUuidAndEpoch() {
        val result = plan()!!
        assertEquals(uuid, result.operationId)
        assertEquals("SELF", result.route)
        assertEquals(14L, result.epoch)
        assertEquals(50L, result.rewardId)
    }

    @Test fun managerRouteRequiresPermissionAndSpecificTarget() {
        assertNull(plan(target = 99))
        val result = plan(s = scope.copy(canManageRewards = true), target = 99)!!
        assertEquals("MANAGER", result.route)
        assertEquals(99L, result.memberId)
    }

    @Test fun offlineUnapprovedOrWrongCurrencyFailsClosed() {
        assertNull(plan(s = scope.copy(online = false)))
        assertNull(plan(r = reward.copy(requiresApproval = true)))
        assertNull(plan(r = reward.copy(acquisitionMode = "REQUEST")))
        assertNull(plan(r = reward.copy(active = false)))
        assertNull(plan(r = reward.copy(priceAmount = -1)))
        assertNull(plan(typeId = 8))
        assertNull(plan(code = "OTHER"))
        assertNull(plan(s = scope.copy(dataEpoch = -1)))
        assertFalse(RewardPurchaseIntentPlanner.FINANCIAL_POST_GATE_OPEN)
    }

    @Test fun zeroPriceIsStillAServerPurchaseNotGenericSpend() {
        assertNotNull(plan(r = reward.copy(priceAmount = 0)))
        assertFalse(RewardPurchaseIntentPlanner.FINANCIAL_POST_GATE_OPEN)
    }
}
