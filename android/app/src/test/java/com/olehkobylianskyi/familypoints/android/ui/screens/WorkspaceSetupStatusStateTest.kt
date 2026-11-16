package com.olehkobylianskyi.familypoints.android.ui.screens

import com.olehkobylianskyi.familypoints.android.network.WorkspaceSetupStateDto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceSetupStatusStateTest {
    private val codes = listOf(
        "MEMBERS", "MEMBER_GROUPS", "WORK_NODE_GROUPS", "CATALOG_SELECTION",
        "TASK_REVIEW", "REWARD_REVIEW", "FINAL_REVIEW"
    )

    private fun state(
        workspaceId: Long = 17,
        dataEpoch: Long = 0,
        revision: Long = 0,
        status: String = "NOT_STARTED",
        steps: Map<String, String>? = codes.associateWith { "PENDING" }
    ) = WorkspaceSetupStateDto(
        workspaceId = workspaceId,
        dataEpoch = dataEpoch,
        revision = revision,
        status = status,
        steps = steps,
        completedAt = null
    )

    @Test fun acceptsCompleteSevenStepMetadataWithoutWrite() {
        assertTrue(validWorkspaceSetupState(state(), 17))
        assertTrue(validWorkspaceSetupState(state(revision = 3, status = "IN_PROGRESS"), 17))
        assertTrue(validWorkspaceSetupState(
            state(status = "COMPLETED", steps = codes.associateWith { "COMPLETE" }), 17
        ))
    }

    @Test fun rejectsCrossFamilyOrInvalidEpochRevision() {
        assertFalse(validWorkspaceSetupState(state(workspaceId = 18), 17))
        assertFalse(validWorkspaceSetupState(state(dataEpoch = -1), 17))
        assertFalse(validWorkspaceSetupState(state(revision = -1), 17))
    }

    @Test fun acceptsNewAndPartiallyConfiguredFamilies() {
        assertTrue(validWorkspaceSetupState(state(steps = emptyMap()), 17))
        assertTrue(validWorkspaceSetupState(state(steps = mapOf("MEMBERS" to "COMPLETE")), 17))
        assertTrue(validWorkspaceSetupState(state(
            status = "IN_PROGRESS", steps = mapOf("MEMBERS" to "COMPLETE", "MEMBER_GROUPS" to "SKIPPED")
        ), 17))
    }

    @Test fun rejectsMissingExtraAndUnknownSteps() {
        assertFalse(validWorkspaceSetupState(state(steps = null), 17))
        assertFalse(validWorkspaceSetupState(
            state(steps = codes.associateWith { "PENDING" } + ("UNEXPECTED" to "COMPLETE")), 17
        ))
        assertFalse(validWorkspaceSetupState(
            state(steps = codes.associateWith { if (it == "TASK_REVIEW") "UNKNOWN" else "PENDING" }), 17
        ))
    }

    @Test fun completedFamiliesRequireAllSevenTerminalDecisions() {
        assertFalse(validWorkspaceSetupState(state(status = "COMPLETED", steps = emptyMap()), 17))
        assertFalse(validWorkspaceSetupState(state(
            status = "COMPLETED", steps = mapOf("MEMBERS" to "COMPLETE")
        ), 17))
        assertFalse(validWorkspaceSetupState(state(
            status = "COMPLETED", steps = codes.associateWith { "PENDING" }
        ), 17))
        assertTrue(validWorkspaceSetupState(state(
            status = "COMPLETED", steps = codes.associateWith {
                if (it == "MEMBER_GROUPS") "SKIPPED" else "COMPLETE"
            }
        ), 17))
    }

    @Test fun rejectsUnknownOverallStatus() {
        assertFalse(validWorkspaceSetupState(state(status = "DISABLED"), 17))
        assertFalse(validWorkspaceSetupState(state(status = "BROKEN"), 17))
    }
}
