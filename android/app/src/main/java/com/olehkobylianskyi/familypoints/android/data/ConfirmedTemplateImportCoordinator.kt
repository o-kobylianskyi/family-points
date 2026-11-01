package com.olehkobylianskyi.familypoints.android.data

import com.google.gson.Gson
import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.network.TemplateImportReceiptDto
import com.olehkobylianskyi.familypoints.android.storage.TemplatePendingOperationStore
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import kotlinx.coroutines.CancellationException
import java.util.UUID

/**
 * Online-only adult-confirmed import boundary. Not invoked on screen display.
 * Never retries an uncertain POST; a pending operation must be resolved separately.
 */
class ConfirmedTemplateImportCoordinator(
    private val tokens: TokenStore,
    private val store: TemplatePendingOperationStore,
    private val receipts: TemplateReceiptRepository
) {
    private val gson = Gson()
    sealed class Result {
        data class Confirmed(val receipt: TemplateImportReceiptDto): Result()
        data class Uncertain(val operationId: UUID): Result()
        data class Blocked(val reason: String): Result()
    }

    suspend fun confirm(accountId: Long, workspaceId: Long, orderedCodes: List<String>): Result {
        require(accountId > 0 && workspaceId > 0)
        require(orderedCodes.isNotEmpty() && orderedCodes.all { it.isNotBlank() })
        require(orderedCodes.distinct().size == orderedCodes.size)
        // No parallel or unknown previous import may be silently bypassed with a new UUID.
        if (store.pending(accountId, workspaceId).any { it.kind == "IMPORT" })
            return Result.Blocked("Resolve existing import receipt before starting another")
        val token = tokens.getAccessToken() ?: return Result.Blocked("Sign in to confirm import")
        val epoch = try {
            ApiClient.workspaceEpochApi.getEpoch("Bearer $token", workspaceId)
        } catch (cancelled: CancellationException) { throw cancelled }
          catch (_: Exception) { return Result.Blocked("Server epoch unavailable") }
        if (epoch.workspaceId != workspaceId || epoch.dataEpoch < 0)
            return Result.Blocked("Invalid workspace epoch")
        val id = UUID.randomUUID()
        val entry = TemplatePendingOperationStore.Pending(
            id, accountId, workspaceId, epoch.dataEpoch, "IMPORT", null,
            gson.toJson(orderedCodes), "PREPARED"
        )
        // Durable write MUST happen before network POST.
        store.prepare(entry)
        return try {
            val receipt = receipts.importConfirmed(workspaceId, id, orderedCodes)
            check(receipt.importResult.selectedCodes == orderedCodes) {
                "Import receipt selection mismatch"
            }
            store.transition(entry, "CONFIRMED", epoch.dataEpoch)
            Result.Confirmed(receipt)
        } catch (cancelled: CancellationException) {
            store.transition(entry, "UNCERTAIN", epoch.dataEpoch)
            throw cancelled
        } catch (_: Exception) {
            // Timeout, disconnect and 409 can all have uncertain results.
            // Do not POST again or generate a new operation ID.
            store.transition(entry, "UNCERTAIN", epoch.dataEpoch)
            Result.Uncertain(id)
        }
    }
}
