package com.olehkobylianskyi.familypoints.android.data

import com.google.gson.Gson
import com.olehkobylianskyi.familypoints.android.network.*
import com.olehkobylianskyi.familypoints.android.storage.*
import kotlinx.coroutines.CancellationException
import java.util.UUID

/**
 * V49: a parent must explicitly confirm after reviewing selection and setup steps.
 * Durable journal is written before POST. No offline queue or automatic retry.
 */
class ConfirmedSetupFinalizeCoordinator(
    private val tokens: TokenStore,
    private val store: TemplatePendingOperationStore
) {
    sealed class Result {
        data class Confirmed(val receipt: SetupFinalizeImportReceiptDto) : Result()
        data class Uncertain(val operationId: UUID) : Result()
        data class Blocked(val reason: String) : Result()
    }
    private fun auth() = "Bearer " + (tokens.getAccessToken() ?: error("Sign in required"))
    suspend fun confirm(
        accountId: Long, workspaceId: Long, expectedRevision: Long, orderedCodes: List<String>
    ): Result {
        require(accountId > 0 && workspaceId > 0 && expectedRevision >= 0)
        require(orderedCodes.size == orderedCodes.distinct().size && orderedCodes.all { it.isNotBlank() })
        if (store.pending(accountId, workspaceId).any { it.kind == "FINALIZE" || it.kind == "IMPORT" })
            return Result.Blocked("Resolve existing import/finalize receipt first")
        val authentication = try { auth() } catch (_: IllegalStateException) {
            return Result.Blocked("Sign in required")
        }
        val epoch = try {
            ApiClient.workspaceEpochApi.getEpoch(authentication, workspaceId)
        } catch (cancelled: CancellationException) { throw cancelled }
          catch (_: Exception) { return Result.Blocked("Cannot verify family epoch") }
        if (epoch.workspaceId != workspaceId || epoch.dataEpoch < 0)
            return Result.Blocked("Invalid family epoch")
        val operation = UUID.randomUUID()
        val request = SetupFinalizeImportRequest(expectedRevision, orderedCodes)
        val payload = Gson().toJson(request)
        val pending = TemplatePendingOperationStore.Pending(
            operation, accountId, workspaceId, epoch.dataEpoch,
            "FINALIZE", null, payload, "PREPARED"
        )
        try {
            store.prepare(pending)
        } catch (cancelled: CancellationException) { throw cancelled }
          catch (_: IllegalStateException) {
              return Result.Blocked("Previous import/finalize still requires reconciliation")
          }
        return try {
            val receipt = ApiClient.workspaceSetupApi.finalizeImport(
                authentication, workspaceId, operation.toString(), request)
            check(receipt.operationId == operation.toString() &&
                receipt.workspaceId == workspaceId &&
                receipt.dataEpoch == epoch.dataEpoch &&
                receipt.setupStatus == "COMPLETED" &&
                receipt.revision > expectedRevision &&
                receipt.importResult.selectedCodes == orderedCodes) {
                "Finalization receipt mismatch"
            }
            store.transition(pending, "CONFIRMED", epoch.dataEpoch)
            Result.Confirmed(receipt)
        } catch (cancelled: CancellationException) {
            store.transition(pending, "UNCERTAIN", epoch.dataEpoch)
            throw cancelled
        } catch (_: Exception) {
            store.transition(pending, "UNCERTAIN", epoch.dataEpoch)
            Result.Uncertain(operation)
        }
    }
}
