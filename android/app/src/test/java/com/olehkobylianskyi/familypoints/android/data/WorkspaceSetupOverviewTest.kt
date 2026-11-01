package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.WorkspaceSetupStateDto
import org.junit.Assert.*
import org.junit.Test

class WorkspaceSetupOverviewTest {
    private fun state(steps: Map<String, String>?, status: String = "IN_PROGRESS") =
        WorkspaceSetupStateDto(
            workspaceId = 8L,
            dataEpoch = 2L,
            revision = 5L,
            status = status,
            steps = steps,
            completedAt = null
        )

    @Test fun sevenServerStepsAreRequiredAndSkippedCountsAsExplicitDecision() {
        assertEquals(7, WorkspaceSetupOverviewRepository.STEPS.size)
        val complete = WorkspaceSetupOverviewRepository.STEPS.associateWith { "COMPLETE" }
        assertTrue(WorkspaceSetupOverviewRepository.readyToFinalize(state(complete)))
        assertTrue(WorkspaceSetupOverviewRepository.readyToFinalize(
            state(complete + ("TASK_REVIEW" to "SKIPPED"))
        ))
        assertFalse(WorkspaceSetupOverviewRepository.readyToFinalize(
            state(complete - "WORK_NODE_GROUPS")
        ))
    }

    @Test fun partialOrCompletedStatusCannotBeFinalizedAgain() {
        val complete = WorkspaceSetupOverviewRepository.STEPS.associateWith { "COMPLETE" }
        assertFalse(WorkspaceSetupOverviewRepository.readyToFinalize(state(null)))
        assertFalse(WorkspaceSetupOverviewRepository.readyToFinalize(
            state(complete + ("REWARD_REVIEW" to "PENDING"))
        ))
        assertFalse(WorkspaceSetupOverviewRepository.readyToFinalize(
            state(complete, "COMPLETED")
        ))
    }

    @Test fun groupIdentityIsNotMergedWithMemberGroupsOrCategories() {
        assertTrue("MEMBER_GROUPS" in WorkspaceSetupOverviewRepository.STEPS)
        assertTrue("WORK_NODE_GROUPS" in WorkspaceSetupOverviewRepository.STEPS)
        assertFalse("CATALOG_CATEGORY_GROUP" in WorkspaceSetupOverviewRepository.STEPS)
    }
}
