package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.network.ProtectiveBackupMetadataDto
import com.olehkobylianskyi.familypoints.android.storage.TokenStore

/** A read-only manager-facing metadata projection. Never claims artifact restore readiness. */
class ProtectiveBackupMetadataRepository(private val tokens: TokenStore) {
    suspend fun list(workspaceId: Long, limit: Int = 25): List<ProtectiveBackupMetadataDto> {
        require(workspaceId > 0 && limit in 1..100)
        val token = tokens.getAccessToken() ?: error("Sign in required")
        return ApiClient.protectiveBackupApi.list("Bearer $token", workspaceId, limit).also { items ->
            check(items.all { it.workspaceId == workspaceId && it.dataEpoch >= 0 && it.snapshotId.isNotBlank() }) {
                "Backup metadata is invalid or belongs to a different family"
            }
        }
    }

    companion object {
        fun isMetadataOnly(item: ProtectiveBackupMetadataDto) = !item.restoreVerified || !item.executable
    }
}
