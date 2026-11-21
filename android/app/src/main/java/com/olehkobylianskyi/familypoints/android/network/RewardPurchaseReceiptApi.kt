package com.olehkobylianskyi.familypoints.android.network

import com.olehkobylianskyi.familypoints.android.data.RewardPurchaseSummary
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/**
 * Draft PR57 read-only receipt shape. No POST methods and not connected to UI.
 * Backend approval and real PostgreSQL ledger QA are required before use.
 */
data class RewardPurchaseReceiptResponse(
    val operationId: String,
    val purchase: RewardPurchaseSummary,
    val replayed: Boolean
)

interface RewardPurchaseReceiptApi {
    @GET("workspaces/{workspaceId}/rewards/purchases/receipts/{operationId}")
    suspend fun getReceipt(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("operationId") operationId: String
    ): RewardPurchaseReceiptResponse
}
