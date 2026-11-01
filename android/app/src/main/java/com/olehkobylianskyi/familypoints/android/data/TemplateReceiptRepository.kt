package com.olehkobylianskyi.familypoints.android.data

import com.google.gson.JsonObject
import com.olehkobylianskyi.familypoints.android.network.*
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import java.util.UUID

/**
 * Explicit online-only V47/V48 transport. No automatic retries and no offline enqueue.
 * The caller must durably retain the ORIGINAL UUID, exact ordered selection/payload,
 * account, workspace and epoch before sending a confirmed mutation.
 * If the response is uncertain, query the receipt before any same-key retry.
 */
class TemplateReceiptRepository(
    private val tokenProvider: () -> String?,
    private val api: TemplateReceiptApi
) {
    constructor(tokens: TokenStore): this({ tokens.getAccessToken() }, ApiClient.templateReceiptApi)
    private fun auth(): String = "Bearer " + (tokenProvider() ?: error("Missing access token"))
    private fun operationId(value: UUID): String = value.toString()
    private fun authForExpected(expectedToken: String): String {
        require(expectedToken.isNotBlank())
        check(tokenProvider() == expectedToken) { "Account changed before receipt request" }
        return "Bearer $expectedToken"
    }

    suspend fun importConfirmed(workspaceId: Long, operation: UUID, orderedCodes: List<String>): TemplateImportReceiptDto {
        require(workspaceId > 0)
        require(orderedCodes.size == orderedCodes.toSet().size && orderedCodes.all { it.isNotBlank() })
        val result = api.importWithReceipt(
            auth(), workspaceId, operationId(operation), TemplateSelectionRequest(orderedCodes))
        check(result.operationId == operationId(operation)) { "Import receipt identity mismatch" }
        return result
    }

    suspend fun importReceipt(workspaceId: Long, operation: UUID, expectedToken: String? = null): TemplateImportReceiptDto {
        require(workspaceId > 0)
        val result = api.importReceipt(if (expectedToken == null) auth() else authForExpected(expectedToken), workspaceId, operationId(operation))
        check(result.operationId == operationId(operation)) { "Import receipt identity mismatch" }
        return result
    }

    suspend fun activateConfirmed(
        workspaceId: Long,
        templateId: Long,
        kind: String,
        operation: UUID,
        reviewedRequest: JsonObject,
        expectedAccessToken: String? = null
    ): TemplateActivationReceiptDto {
        require(workspaceId > 0 && templateId > 0)
        require(kind == "TASK" || kind == "REWARD")
        // Capture one authenticated identity for this entire mutation. A switch
        // between coordinator preflight and HTTP dispatch must never send the
        // original operation under another user's new credentials.
        val currentToken = tokenProvider() ?: error("Missing access token")
        check(expectedAccessToken == null || expectedAccessToken == currentToken) {
            "Account session changed before activation transport"
        }
        val authorization = "Bearer $currentToken"
        val api = this.api
        val result = when (kind) {
            "TASK" -> api.activateTask(authorization, workspaceId, templateId, operationId(operation), reviewedRequest)
            else -> api.activateReward(authorization, workspaceId, templateId, operationId(operation), reviewedRequest)
        }
        check(result.operationId == operationId(operation) &&
            result.templateId == templateId && result.kind == kind && result.createdDefinitionId > 0
        ) { "Activation receipt identity mismatch" }
        return result
    }

    suspend fun activationReceipt(workspaceId: Long, operation: UUID, expectedToken: String? = null): TemplateActivationReceiptDto {
        require(workspaceId > 0)
        val result = api.activationReceipt(if (expectedToken == null) auth() else authForExpected(expectedToken), workspaceId, operationId(operation))
        check(result.operationId == operationId(operation) && result.createdDefinitionId > 0) {
            "Activation receipt identity mismatch"
        }
        return result
    }
}
