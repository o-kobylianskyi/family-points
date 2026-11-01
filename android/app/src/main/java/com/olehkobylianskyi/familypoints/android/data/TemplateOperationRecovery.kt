package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TemplatePendingOperationStore
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import retrofit2.HttpException
import kotlinx.coroutines.CancellationException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
        if (operation.status !in setOf("PREPARED", "UNCERTAIN")) return Outcome.UNKNOWN
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
        if (tokens.getAccessToken() != token) return Outcome.UNKNOWN
        return try {
            if (operation.kind == "FINALIZE") {
                val response = ApiClient.workspaceSetupApi.finalizeReceipt(
                    "Bearer $token", workspaceId, operation.operationId.toString()
                )
                val request = Gson().fromJson(
                    operation.payload,
                    com.olehkobylianskyi.familypoints.android.network.SetupFinalizeImportRequest::class.java
                )
                check(request != null && response.operationId == operation.operationId.toString() &&
                    response.workspaceId == workspaceId && response.dataEpoch == operation.epoch &&
                    response.setupStatus == "COMPLETED" &&
                    response.revision > request.expectedRevision &&
                    response.importResult.selectedCodes == request.selectedCatalogCodes
                ) { "Finalize receipt conflicts with durable request" }
            } else if (operation.kind == "IMPORT") {
                val response = receipts.importReceipt(workspaceId, operation.operationId, token)
                val expected = Gson().fromJson<List<String>>(
                    operation.payload, object : TypeToken<List<String>>() {}.type
                )
                check(expected != null && response.importResult.selectedCodes == expected) {
                    "Import receipt selection differs from persisted operation"
                }
            } else if (operation.kind == "TASK" || operation.kind == "REWARD") {
                val result = receipts.activationReceipt(workspaceId, operation.operationId, token)
                check(result.templateId == operation.templateId && result.kind == operation.kind) {
                    "Activation receipt does not match pending operation"
                }
            } else { return Outcome.UNKNOWN }
            val verifiedAfterReceipt = ApiClient.workspaceEpochApi.getEpoch("Bearer $token", workspaceId)
            if (verifiedAfterReceipt.workspaceId != workspaceId || verifiedAfterReceipt.dataEpoch < 0) return Outcome.UNKNOWN
            if (verifiedAfterReceipt.dataEpoch != operation.epoch) {
                store.transition(operation, "HELD", verifiedAfterReceipt.dataEpoch)
                return Outcome.HELD
            }
            if (!ReceiptRecoveryGuard.sameIdentityAndEpoch(
                    operation.accountId, accountId, operation.workspaceId,
                    verifiedAfterReceipt.workspaceId, operation.epoch,
                    verifiedAfterReceipt.dataEpoch, token, tokens.getAccessToken()
                )) return Outcome.UNKNOWN
            store.transition(operation, "CONFIRMED", verifiedAfterReceipt.dataEpoch)
            Outcome.CONFIRMED
        } catch (e: HttpException) {
            if (e.code() == 404) Outcome.NOT_FOUND else Outcome.UNKNOWN
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { Outcome.UNKNOWN }
    }
}
