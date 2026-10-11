package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import com.olehkobylianskyi.familypoints.android.storage.OfflineBalanceStore
import com.olehkobylianskyi.familypoints.android.storage.OfflineMembersStore
import com.olehkobylianskyi.familypoints.android.storage.OfflinePointsQueueStore
import com.olehkobylianskyi.familypoints.android.storage.OfflinePointTypesStore
import com.olehkobylianskyi.familypoints.android.network.ServerReachability
import com.olehkobylianskyi.familypoints.android.network.ServerStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import retrofit2.HttpException
import java.io.IOException

class PointsRepository(
    private val tokenStore: TokenStore,
    private val offlineStore: OfflineBalanceStore,
    private val membersStore: OfflineMembersStore,
    private val queue: OfflinePointsQueueStore,
    private val typesStore: OfflinePointTypesStore
) {
    private val replayMutex = Mutex()

    private fun authorization(): String {
        val token = tokenStore.getAccessToken()
            ?: throw IllegalStateException("Missing access token")
        return "Bearer $token"
    }

    fun cachedPointTypes(workspaceId: Long, viewerId: Long): List<PointTypeResponse> =
        typesStore.load(workspaceId, viewerId).filter { it.active }.sortedBy { it.sortOrder }

    suspend fun refreshPointTypes(workspaceId: Long, viewerId: Long): List<PointTypeResponse> {
        val types = ApiClient.createTaskApi.getPointTypes(authorization(), workspaceId)
        typesStore.save(workspaceId, viewerId, types)
        return types.filter { it.active }.sortedBy { it.sortOrder }
    }

    fun cachedMembers(workspaceId: Long, viewerId: Long): List<WorkspaceMemberResponse> =
        membersStore.cachedMembers(workspaceId, viewerId)

    fun cachedConfirmedBalance(workspaceId: Long, memberId: Long): Long? =
        offlineStore.load(workspaceId, memberId)?.balance

    fun pending(workspaceId: Long, viewerId: Long): List<OfflinePointsQueueStore.Pending> =
        queue.pending(workspaceId, viewerId)

    fun queueOffline(
        workspaceId: Long, viewerId: Long, memberId: Long,
        pointType: PointTypeResponse,
        operation: String, amount: Int, description: String?
    ): OfflinePointsQueueStore.Pending =
        queue.enqueue(workspaceId, viewerId, memberId, pointType.id,
            pointType.code, pointType.name, operation, amount, description)

    /**
     * Requires online authentication; the server rechecks MANAGE_POINTS
     * and de-duplicates every clientOperationId within a DB transaction.
     */
    suspend fun synchronizePending(workspaceId: Long, viewerId: Long): Int {
        replayMutex.lock()
        try {
            if (ServerReachability.status.value != ServerStatus.ONLINE) return 0
            val auth = authorization()
            var confirmed = 0
            val affectedMembers = mutableSetOf<Long>()
            for (entry in queue.pending(workspaceId, viewerId)) {
            try {
                ApiClient.pointsApi.replayOffline(
                    auth, workspaceId, entry.memberId, entry.type,
                    OfflinePointOperationRequest(entry.operationId, entry.pointTypeId, entry.amount, entry.description)
                )
                queue.confirm(entry.operationId, workspaceId, viewerId)
                confirmed++
                affectedMembers.add(entry.memberId)
            } catch (error: HttpException) {
                // Keep failed operations for parental review; never silently
                // discard denied/invalid items. 401 ends synchronization.
                if (error.code() == 401 || error.code() == 403 || error.code() in 400..499) throw error
                break
            } catch (error: IOException) {
                break
            }
            }
            for (memberId in affectedMembers) {
                try {
                    val fresh = ApiClient.pointsApi.getBalance(auth, workspaceId, memberId)
                    offlineStore.save(workspaceId, memberId, fresh.balance)
                } catch (_: Exception) {
                    // Idempotent point operation was confirmed. Keep last snapshot
                    // until the next balance fetch; never queue a duplicate.
                }
            }
            return confirmed
        } finally {
            replayMutex.unlock()
        }
    }

    suspend fun prefetchBalances(
        workspaceId: Long, members: List<WorkspaceMemberResponse>
    ) = supervisorScope {
        val auth = authorization()
        members.map { member ->
            async {
                try {
                    val result = withTimeout(8_000L) {
                        ApiClient.pointsApi.getBalance(auth, workspaceId, member.id)
                    }
                    offlineStore.save(workspaceId, member.id, result.balance)
                } catch (error: TimeoutCancellationException) {
                    // Timeout affects one member only, never the whole list.
                } catch (_: IOException) {
                } catch (error: HttpException) {
                    if (error.code() == 401) throw error
                }
            }
        }.forEach { it.await() }
    }

    data class CachedPoints(val balance: Long, val history: List<PointTransactionResponse>, val offline: Boolean)

    suspend fun cachedBalance(workspaceId: Long, memberId: Long): Long? =
        offlineStore.load(workspaceId, memberId)?.balance

    suspend fun loadPointsWithFallback(workspaceId: Long, memberId: Long): CachedPoints {
        val auth = authorization()
        val balance = try {
            ApiClient.pointsApi.getBalance(auth, workspaceId, memberId).balance.also { value ->
                offlineStore.save(workspaceId, memberId, value)
            }
        } catch (error: IOException) {
            val cached = offlineStore.load(workspaceId, memberId) ?: throw error
            return CachedPoints(cached.balance, emptyList(), true)
        }
        return CachedPoints(balance, emptyList(), false)
    }

    suspend fun loadHistory(workspaceId: Long, memberId: Long): List<PointTransactionResponse> =
        ApiClient.pointsApi.getHistory(authorization(), workspaceId, memberId)

    suspend fun loadMembers(
        workspaceId: Long,
        viewerId: Long
    ): List<WorkspaceMemberResponse> {
        val members = ApiClient.taskApi.getWorkspaceMembers(authorization(), workspaceId)
        membersStore.saveMembers(workspaceId, viewerId, members)
        return members
    }

    // A failed history request must not make the current balance disappear.
    suspend fun loadMemberPoints(
        workspaceId: Long,
        memberId: Long
    ): Pair<Long, List<PointTransactionResponse>> {
        val auth = authorization()
        val balance = ApiClient.pointsApi.getBalance(auth, workspaceId, memberId).balance
        val history = try {
            ApiClient.pointsApi.getHistory(auth, workspaceId, memberId)
        } catch (error: Exception) {
            // A bad authorization must never be hidden as an empty history.
            if (error is retrofit2.HttpException && error.code() == 401) throw error
            emptyList()
        }
        return balance to history
    }

    suspend fun operate(
        workspaceId: Long,
        memberId: Long,
        pointTypeId: Long,
        operation: String,
        amount: Int,
        description: String?
    ) {
        ApiClient.pointsApi.operateTyped(
            authorization(), workspaceId, memberId, operation,
            TypedPointOperationRequest(pointTypeId, amount, description)
        )
    }
}
