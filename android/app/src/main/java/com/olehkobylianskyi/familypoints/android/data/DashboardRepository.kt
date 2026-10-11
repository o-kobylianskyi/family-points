package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import com.olehkobylianskyi.familypoints.android.storage.OfflineDashboardStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate

class DashboardRepository(
    private val tokenStore: TokenStore,
    private val offlineStore: OfflineDashboardStore
) {
    fun cached(currentUser: CurrentUserResponse): DashboardData? =
        offlineStore.load(currentUser.workspaceId, currentUser.memberId, LocalDate.now().toString())

    suspend fun load(currentUser: CurrentUserResponse): DashboardData =
        coroutineScope {
            val token = tokenStore.getAccessToken()
                ?: throw IllegalStateException("Missing access token")

            val authorization = "Bearer $token"
            val date = LocalDate.now().toString()

            val balance = async {
                ApiClient.dashboardApi.getMemberBalance(
                    authorization,
                    currentUser.workspaceId,
                    currentUser.memberId
                )
            }

            val tasks = async {
                ApiClient.dashboardApi.getMemberTasks(
                    authorization,
                    currentUser.workspaceId,
                    currentUser.memberId,
                    date
                )
            }

            val openTasks = async {
                ApiClient.dashboardApi.getOpenTasks(
                    authorization,
                    currentUser.workspaceId,
                    date,
                    currentUser.memberId
                )
            }

            DashboardData(
                balance = balance.await().balance,
                tasks = tasks.await(),
                openTasks = openTasks.await()
            ).also { result ->
                offlineStore.save(currentUser.workspaceId, currentUser.memberId, date, result)
            }
        }
}
