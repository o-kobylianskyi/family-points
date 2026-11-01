package com.olehkobylianskyi.familypoints.android.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/** V46/V48 setup metadata and safe read-only selection review; no imports or activation. */
data class WorkspaceSetupStateDto(
    val workspaceId: Long,
    val dataEpoch: Long,
    val revision: Long,
    val status: String,
    val steps: Map<String, String>?,
    val completedAt: String?
)

data class WorkspaceSetupReviewRequest(
    val expectedRevision: Long,
    val selectedCatalogCodes: List<String>
)

data class WorkspaceSetupReviewDto(
    val workspaceId: Long,
    val dataEpoch: Long,
    val revision: Long,
    val setupStatus: String,
    val selectedCount: Int,
    val alreadyImportedCount: Int,
    val toImportCount: Int,
    val selectedCodes: List<String>,
    val alreadyImported: List<String>,
    val toImport: List<String>,
    val requiresExplicitConfirmation: Boolean,
    val createsActiveDefinitions: Boolean
)

interface WorkspaceSetupApi {
    @GET("workspaces/{workspaceId}/setup")
    suspend fun state(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): WorkspaceSetupStateDto

    /** HTTP POST is a dry run; backend enforces workspace manager rights and revision fencing. */
    @POST("workspaces/{workspaceId}/setup/review")
    suspend fun review(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Body request: WorkspaceSetupReviewRequest
    ): WorkspaceSetupReviewDto
}
