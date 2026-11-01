package com.olehkobylianskyi.familypoints.android.network

import com.google.gson.Gson
import com.olehkobylianskyi.familypoints.android.data.WorkspaceSetupOverviewRepository
import org.junit.Assert.*
import org.junit.Test

class WorkspaceSetupStepContractTest {
    private val gson = Gson()
    @Test fun stepMutationPreservesExactRevisionAndExplicitStatus() {
        val request = WorkspaceSetupStepRequest("SKIPPED", 17L)
        assertEquals("""{"status":"SKIPPED","expectedRevision":17}""", gson.toJson(request))
        assertTrue("FINAL_REVIEW" in WorkspaceSetupOverviewRepository.STEPS)
    }
    @Test fun revisionAndEpochAreRetainedInReturnedSetupState() {
        val state = gson.fromJson(
            """{"workspaceId":2,"dataEpoch":4,"revision":18,"status":"IN_PROGRESS",
                "steps":{"MEMBERS":"COMPLETE","MEMBER_GROUPS":"SKIPPED"},
                "completedAt":null}""",
            WorkspaceSetupStateDto::class.java
        )
        assertEquals(2L, state.workspaceId)
        assertEquals(4L, state.dataEpoch)
        assertEquals(18L, state.revision)
        assertFalse(WorkspaceSetupOverviewRepository.readyToFinalize(state))
    }
}
