package com.olehkobylianskyi.familypoints.android.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.network.TemplateActivationReceiptDto
import com.olehkobylianskyi.familypoints.android.storage.TemplateOperationCipher
import com.olehkobylianskyi.familypoints.android.storage.TemplatePendingOperationStore
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import kotlinx.coroutines.CancellationException
import java.util.UUID

/**
 * Explicit parent-confirmed online TASK/REWARD activation.
 * UI must supply a fully reviewed server-compatible request and actual permissions.
 * No offline enqueue, no automatic retry, no activation from catalog browsing.
 */
class ConfirmedTemplateActivationCoordinator(
    private val tokens: TokenStore,
    private val store: TemplatePendingOperationStore,
    private val receipts: TemplateReceiptRepository,
    private val cipher: TemplateOperationCipher
) {
    sealed class Result {
        data class Confirmed(val receipt: TemplateActivationReceiptDto): Result()
        data class Uncertain(val operationId: UUID): Result()
        data class Blocked(val reason: String): Result()
    }

    suspend fun confirm(
        accountId: Long, workspaceId: Long, templateId: Long, kind: String,
        permissions: Set<String>, reviewedRequest: JsonObject
    ): Result {
        require(accountId > 0 && workspaceId > 0 && templateId > 0)
        require(kind == "TASK" || kind == "REWARD")
        // Exact feature permissions are required in addition to server workspace ACL.
        val authorized = (kind == "TASK" && "CREATE_TASKS" in permissions) ||
            (kind == "REWARD" && "MANAGE_REWARDS" in permissions)
        if (!authorized) return Result.Blocked("Parent permission required")
        if (reviewedRequest.entrySet().isEmpty()) return Result.Blocked("Review the full activation request first")
        // An unknown/HELD activation in the same family must not be bypassed
        // by signing in with a different account. SQLite prepare enforces this
        // workspace-wide as the final atomic gate.
        if (store.pending(accountId, workspaceId).any {
                it.kind == kind && it.templateId == templateId
            }) return Result.Blocked("Resolve previous activation receipt first")
        val token = tokens.getAccessToken() ?: return Result.Blocked("Sign in before activation")
        val epoch = try {
            ApiClient.workspaceEpochApi.getEpoch("Bearer $token", workspaceId)
        } catch (cancelled: CancellationException) { throw cancelled }
          catch (_: Exception) { return Result.Blocked("Server epoch unavailable") }
        if (epoch.workspaceId != workspaceId || epoch.dataEpoch < 0)
            return Result.Blocked("Invalid workspace epoch")
        // The session may change while the first authoritative epoch GET is in flight.
        if (tokens.getAccessToken() != token)
            return Result.Blocked("Account changed before activation")
        val operation = UUID.randomUUID()
        // Encrypt BEFORE storing. Never persist plaintext task/reward request.
        val encrypted = cipher.encrypt(reviewedRequest.toString())
        val entry = TemplatePendingOperationStore.Pending(
            operation, accountId, workspaceId, epoch.dataEpoch,
            kind, templateId, encrypted, "PREPARED"
        )
        try {
            store.prepare(entry)
        } catch (cancelled: CancellationException) { throw cancelled }
          catch (_: IllegalStateException) { return Result.Blocked("Previous activation is pending") }
        // Re-check the session after SQLite prepare: do not send a mutation under
        // a token that changed between durable persistence and HTTP dispatch.
        if (tokens.getAccessToken() != token) {
            runCatching { store.transition(entry, "UNCERTAIN", epoch.dataEpoch) }
            return Result.Uncertain(operation)
        }
        return try {
            val receipt = receipts.activateConfirmed(workspaceId, templateId, kind, operation,
                JsonParser.parseString(cipher.decrypt(entry.payload)).asJsonObject)
            // The second epoch GET is required even after a successful POST.
            val verified = ApiClient.workspaceEpochApi.getEpoch("Bearer $token", workspaceId)
            when (ActivationCompletionGate.decide(
                accountId, accountId, workspaceId, verified.workspaceId,
                entry.epoch, verified.dataEpoch, token, tokens.getAccessToken()
            )) {
                ActivationCompletionGate.Decision.CONFIRM -> {
                    store.transition(entry, "CONFIRMED", verified.dataEpoch)
                    Result.Confirmed(receipt)
                }
                ActivationCompletionGate.Decision.HOLD -> {
                    store.transition(entry, "HELD", verified.dataEpoch)
                    Result.Uncertain(operation)
                }
                ActivationCompletionGate.Decision.UNCERTAIN -> {
                    store.transition(entry, "UNCERTAIN", epoch.dataEpoch)
                    Result.Uncertain(operation)
                }
            }
        } catch (cancelled: CancellationException) {
            // A concurrent generation change may already have put the row in HELD.
            runCatching { store.transition(entry, "UNCERTAIN", epoch.dataEpoch) }
            throw cancelled
        } catch (_: Exception) {
            // Preserve terminal journal rows; never reopen HELD or CONFIRMED.
            runCatching { store.transition(entry, "UNCERTAIN", epoch.dataEpoch) }
            Result.Uncertain(operation)
        }
    }
}
