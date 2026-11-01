package com.olehkobylianskyi.familypoints.android.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/** V50 registry metadata only. No archive bytes, export, restore or reset actions. */
data class ProtectiveBackupMetadataDto(
    val snapshotId: String,
    val workspaceId: Long,
    val operationId: String,
    val dataEpoch: Long,
    val resetMode: String,
    val selectedCategories: List<String>,
    val sourceDigest: String,
    val state: String,
    val createdAt: String,
    val expiresAt: String,
    val restoreVerified: Boolean,
    val executable: Boolean
)

interface ProtectiveBackupApi {
    @GET("workspaces/{workspaceId}/protective-backups")
    suspend fun list(
        @Header("Authorization") auth: String,
        @Path("workspaceId") workspaceId: Long,
        @Query("limit") limit: Int = 25
    ): List<ProtectiveBackupMetadataDto>
}
