package com.olehkobylianskyi.familypoints.android.network

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/** V47: importing private templates never creates active tasks or rewards. */
data class TemplateImportReceiptResultDto(
    val selectedCodes: List<String>,
    val alreadyImported: List<String>,
    val toImport: List<String>
)
data class TemplateImportReceiptDto(
    val operationId: String,
    val replayed: Boolean,
    val importResult: TemplateImportReceiptResultDto
)

/** V48: createdDefinitionId is an identifier, not an active definition DTO. */
data class TemplateActivationReceiptDto(
    val operationId: String,
    val templateId: Long,
    val kind: String,
    val createdDefinitionId: Long,
    val replayed: Boolean
)

interface TemplateReceiptApi {
    @POST("workspaces/{workspaceId}/templates/imports/idempotent")
    suspend fun importWithReceipt(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Header("Idempotency-Key") operationId: String,
        @Body request: TemplateSelectionRequest
    ): TemplateImportReceiptDto

    @GET("workspaces/{workspaceId}/templates/imports/receipts/{operationId}")
    suspend fun importReceipt(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("operationId") operationId: String
    ): TemplateImportReceiptDto

    @POST("workspaces/{workspaceId}/templates/{templateId}/activate-task/idempotent")
    suspend fun activateTask(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("templateId") templateId: Long,
        @Header("Idempotency-Key") operationId: String,
        @Body request: JsonObject
    ): TemplateActivationReceiptDto

    @POST("workspaces/{workspaceId}/templates/{templateId}/activate-reward/idempotent")
    suspend fun activateReward(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("templateId") templateId: Long,
        @Header("Idempotency-Key") operationId: String,
        @Body request: JsonObject
    ): TemplateActivationReceiptDto

    @GET("workspaces/{workspaceId}/templates/activations/receipts/{operationId}")
    suspend fun activationReceipt(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("operationId") operationId: String
    ): TemplateActivationReceiptDto
}
