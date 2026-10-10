package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate

class DashboardRepository(
    private val tokenStore: TokenStore
) {
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
            )
        }
}
