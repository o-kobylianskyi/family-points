package com.olehkobylianskyi.familypoints.android.network

import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * PREPARATION ONLY for Backend Draft PR57. Deliberately NOT registered in ApiClient.
 * The product must not invoke this interface before independent financial QA,
 * migration integration and explicit Architecture release gate.
 * Stable original Idempotency-Key must be persisted before an approved POST.
 */
internal interface RewardPurchaseIdempotentDraftApi {
    @POST("workspaces/{workspaceId}/rewards/{rewardId}/purchase/idempotent")
    suspend fun purchaseSelf(
        @Header("Authorization") auth: String,
        @Header("Idempotency-Key") operationId: String,
        @Header("X-Workspace-Data-Epoch") epoch: Long,
        @Path("workspaceId") workspaceId: Long,
        @Path("rewardId") rewardId: Long
    ): RewardPurchaseReceiptResponse

    @POST("workspaces/{workspaceId}/rewards/{rewardId}/purchase-for-member/{memberId}/idempotent")
    suspend fun purchaseForMember(
        @Header("Authorization") auth: String,
        @Header("Idempotency-Key") operationId: String,
        @Header("X-Workspace-Data-Epoch") epoch: Long,
        @Path("workspaceId") workspaceId: Long,
        @Path("rewardId") rewardId: Long,
        @Path("memberId") memberId: Long
    ): RewardPurchaseReceiptResponse
}
