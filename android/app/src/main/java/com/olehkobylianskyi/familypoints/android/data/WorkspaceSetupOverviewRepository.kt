package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.network.WorkspaceSetupStateDto
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Read-only existing-family group inventory. No catalog-to-group seeding. */
class WorkspaceSetupOverviewRepository(private val tokens: TokenStore) {
    data class Overview(
        val state: WorkspaceSetupStateDto,
        val memberGroupCount: Int,
        val workNodeGroupCount: Int
    )
    private fun auth() = "Bearer " + (tokens.getAccessToken() ?: error("Missing access token"))

    suspend fun load(workspaceId: Long): Overview = coroutineScope {
        require(workspaceId > 0)
        val authentication = auth()
        val state = async { ApiClient.workspaceSetupApi.state(authentication, workspaceId) }
        val memberGroups = async { ApiClient.membersApi.getGroups(authentication, workspaceId) }
        val taskGroups = async { ApiClient.workNodeGroupsApi.list(authentication, workspaceId) }
        Overview(state.await(), memberGroups.await().size, taskGroups.await().count { it.active })
            .also { check(it.state.workspaceId == workspaceId && it.state.dataEpoch >= 0L) }
    }

    companion object {
        val STEPS = listOf("MEMBERS", "MEMBER_GROUPS", "WORK_NODE_GROUPS",
            "CATALOG_SELECTION", "TASK_REVIEW", "REWARD_REVIEW", "FINAL_REVIEW")
        fun readyToFinalize(state: WorkspaceSetupStateDto): Boolean =
            state.status != "COMPLETED" && STEPS.all {
                state.steps?.get(it) in setOf("COMPLETE", "SKIPPED")
            }
    }
}
