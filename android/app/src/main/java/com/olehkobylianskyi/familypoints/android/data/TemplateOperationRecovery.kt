package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TemplatePendingOperationStore
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import retrofit2.HttpException
import kotlinx.coroutines.CancellationException

/**
 * Explicit receipt-only recovery for a selected account/workspace.
 * Never POSTs, never generates UUIDs, never clears a pending operation.
 */
class TemplateOperationRecovery(
    private val tokens: TokenStore,
    private val store: TemplatePendingOperationStore,
    private val receipts: TemplateReceiptRepository
) {
    enum class Outcome { CONFIRMED, NOT_FOUND, HELD, UNKNOWN }

    suspend fun check(
        accountId: Long,
        workspaceId: Long,
        operation: TemplatePendingOperationStore.Pending
    ): Outcome {
        require(accountId > 0 && workspaceId > 0)
        require(operation.accountId == accountId && operation.workspaceId == workspaceId)
        if (operation.status == "HELD") return Outcome.HELD
        val token = tokens.getAccessToken() ?: return Outcome.UNKNOWN
        val epoch = try {
            ApiClient.workspaceEpochApi.getEpoch("Bearer $token", workspaceId)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { return Outcome.UNKNOWN }
        if (epoch.workspaceId != workspaceId || epoch.dataEpoch < 0) return Outcome.UNKNOWN
        if (epoch.dataEpoch != operation.epoch) {
            store.transition(operation, "HELD", epoch.dataEpoch)
            return Outcome.HELD
        }
        return try {
            if (operation.kind == "IMPORT") {
                receipts.importReceipt(workspaceId, operation.operationId)
            } else {
                val result = receipts.activationReceipt(workspaceId, operation.operationId)
                check(result.templateId == operation.templateId && result.kind == operation.kind) {
                    "Activation receipt does not match pending operation"
                }
            }
            store.transition(operation, "CONFIRMED", epoch.dataEpoch)
            Outcome.CONFIRMED
        } catch (e: HttpException) {
            if (e.code() == 404) Outcome.NOT_FOUND else Outcome.UNKNOWN
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { Outcome.UNKNOWN }
    }
}
