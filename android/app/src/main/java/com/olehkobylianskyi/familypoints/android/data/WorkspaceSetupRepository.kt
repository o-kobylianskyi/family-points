package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.network.WorkspaceSetupReviewRequest
import com.olehkobylianskyi.familypoints.android.storage.TokenStore

/** Read-only onboarding discovery and dry-run, independent of destructive reset. */
class WorkspaceSetupRepository(private val tokens: TokenStore) {
    private fun auth() = "Bearer " + (tokens.getAccessToken() ?: error("Missing access token"))

    suspend fun state(workspaceId: Long) = ApiClient.workspaceSetupApi.state(auth(), workspaceId).also {
        check(it.workspaceId == workspaceId && it.dataEpoch >= 0 && it.revision >= 0) {
            "Untrusted setup identity"
        }
    }

    suspend fun review(workspaceId: Long, expectedRevision: Long, selectedCodes: List<String>) =
        ApiClient.workspaceSetupApi.review(
            auth(), workspaceId, WorkspaceSetupReviewRequest(expectedRevision, selectedCodes)
        ).also {
            check(it.workspaceId == workspaceId && it.revision == expectedRevision &&
                it.dataEpoch >= 0 && !it.createsActiveDefinitions && it.requiresExplicitConfirmation &&
                it.selectedCount == it.selectedCodes.size &&
                it.alreadyImportedCount == it.alreadyImported.size &&
                it.toImportCount == it.toImport.size) {
                "Untrusted setup review response"
            }
        }
}
