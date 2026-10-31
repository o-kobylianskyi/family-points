package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import com.olehkobylianskyi.familypoints.android.ui.components.FpCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.olehkobylianskyi.familypoints.android.ui.components.WorkspaceSwitcher
import com.olehkobylianskyi.familypoints.android.network.AccountWorkspaceChoice
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.DashboardData
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.formatPointAmount
import com.olehkobylianskyi.familypoints.android.i18n.dashboardStrings
import com.olehkobylianskyi.familypoints.android.i18n.taskStatusLabel
import com.olehkobylianskyi.familypoints.android.ui.components.AppHeader
import com.olehkobylianskyi.familypoints.android.network.ServerReachability
import com.olehkobylianskyi.familypoints.android.network.ServerStatus
import retrofit2.HttpException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DashboardDestination {
    DASHBOARD,
    MEMBERS,
    TASKS,
    POINTS,
    REWARDS,
    SETTINGS
}

@Composable
fun DashboardScreen(
    refreshNonce: Int = 0,
    onRefreshComplete: (Int) -> Unit = {},
    language: AppLanguage,
    currentUser: CurrentUserResponse,
    loadDashboard: suspend () -> DashboardData,
    cachedDashboard: () -> DashboardData?,
    loadReputation: suspend () -> Long,
    loadWorkspaces: suspend () -> List<AccountWorkspaceChoice>,
    onSwitchWorkspace: suspend (Long) -> Unit,
    canSwitchWorkspaces: Boolean,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (DashboardDestination) -> Unit,
    onUnauthorized: () -> Unit
) {
    val text = dashboardStrings(language)
    val scope = rememberCoroutineScope()

    val serverStatus by ServerReachability.status.collectAsState()
    var data by remember(currentUser.workspaceId, currentUser.memberId) { mutableStateOf<DashboardData?>(null) }
    var reputation by remember(currentUser.workspaceId, currentUser.memberId) { mutableStateOf<Long?>(null) }
    var showingCached by remember(currentUser.workspaceId, currentUser.memberId) { mutableStateOf(false) }
    var onlineEpochVerified by remember(currentUser.workspaceId, currentUser.memberId) { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(currentUser.memberId, currentUser.workspaceId, serverStatus, refreshNonce) {
        error = ""
        cachedDashboard()?.let { snapshot ->
            if (data == null && serverStatus == ServerStatus.OFFLINE) {
                data = snapshot
                showingCached = true
            }
        }
        if (serverStatus != ServerStatus.OFFLINE) {
            // Hide prior snapshots until authenticated epoch validation succeeds.
            data = null
            showingCached = false
            onlineEpochVerified = false
        }
        loading = data == null
        if (serverStatus == ServerStatus.OFFLINE) {
            reputation = null
            if (data == null) {
                error = when (language) {
                    AppLanguage.UK -> "Головна ще не збережена. Відкрийте її один раз із доступним сервером."
                    AppLanguage.DE -> "Startseite noch nicht gespeichert. Bitte einmal online öffnen."
                    AppLanguage.EN -> "Home page not cached yet. Open it once while the server is available."
                    AppLanguage.RU -> "Главная ещё не сохранена. Откройте её при доступном сервере."
                }
            }
            loading = false
            return@LaunchedEffect
        }
        try {
            data = loadDashboard()
            onlineEpochVerified = true
            reputation = try { loadReputation() } catch (_: Exception) { null }
            showingCached = false
        } catch (exception: HttpException) {
            if (exception.code() == 401) onUnauthorized()
            else if (data == null) error = exception.message()
        } catch (exception: Exception) {
            if (data == null) error = exception.message ?: text.loadFailed
        } finally {
            loading = false
            if (refreshNonce > 0) onRefreshComplete(refreshNonce)
        }
    }

    LaunchedEffect(currentUser.memberId, currentUser.workspaceId, serverStatus, refreshNonce) {
        if (serverStatus == ServerStatus.ONLINE && canSwitchWorkspaces) {
            while (true) {
                delay(30_000L)
                try {
                    data = loadDashboard()
                    onlineEpochVerified = true
                    reputation = try { loadReputation() } catch (_: Exception) { null }
                    showingCached = false
                    error = ""
                } catch (exception: HttpException) {
                    if (exception.code() == 401) onUnauthorized()
                } catch (_: Exception) {
                    // Keep the cached home screen visible while API is unavailable.
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = 12.dp,
            bottom = 40.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AppHeader(
                language = language,
                currentUser = currentUser,
                onLanguageChange = onLanguageChange,
                onLogout = onLogout,
                onNavigate = onNavigate
            )
        }
        if (showingCached) {
            item {
                Text(
                    when (language) {
                        AppLanguage.UK -> "⟳ Збережена копія · очікує синхронізації"
                        AppLanguage.DE -> "⟳ Gespeicherte Daten · Synchronisierung ausstehend"
                        AppLanguage.EN -> "⟳ Cached data · awaiting synchronization"
                        AppLanguage.RU -> "⟳ Сохранённая копия · ожидание синхронизации"
                    },
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        when {
            loading && data == null -> {
                item {
                    Text(text.loading)
                }
            }

            error.isNotBlank() && data == null -> {
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            data != null && (serverStatus == ServerStatus.OFFLINE || onlineEpochVerified) -> {
                val dashboard = data!!
                val active = dashboard.activeTasks

                item {
                    Column {
                        Text(
                            text = "${text.welcomePrefix}, ${currentUser.memberName}",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = text.todayWorkspace,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

        if (serverStatus == ServerStatus.ONLINE) {
            item {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End) {
                    WorkspaceSwitcher(
                        activeWorkspaceId = currentUser.workspaceId,
                        loadWorkspaces = loadWorkspaces,
                        activate = onSwitchWorkspace
                    )
                }
            }
        }
                item {
                    DashboardStatCard(
                        label = text.myBalance,
                        value = formatPointAmount(dashboard.balance.toLong(), language, "POINTS", text.points),
                        description = ""
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BoxStat(
                            modifier = Modifier.weight(1f),
                            label = when (language) {
                                AppLanguage.UK -> "Моя репутація"
                                AppLanguage.DE -> "Mein Ruf"
                                AppLanguage.EN -> "My reputation"
                                AppLanguage.RU -> "Моя репутация"
                            },
                            value = reputation?.toString() ?: "—",
                            footnote = ""
                        )
                        BoxStat(
                            modifier = Modifier.weight(1f),
                            label = text.myTasks,
                            value = active.size.toString(),
                            footnote = text.activeToday
                        )
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = text.today,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(text.currentTasks)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onNavigate(DashboardDestination.TASKS)
                                    }
                                ) {
                                    Text(text.allTasks)
                                }
                            }

                            if (active.isEmpty()) {
                                Text(
                                    text = text.noActiveTasks,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                        }
                    }
                }

                if (active.isNotEmpty()) {
                    items(active.take(5), key = { it.id }) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = task.title,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = taskStatusLabel(
                                        language,
                                        task.status
                                    ),
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }




            }
        }
    }
}

@Composable
private fun DashboardStatCard(
    label: String,
    value: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (description.isNotBlank()) Text(
                text = description,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun BoxStat(
    modifier: Modifier,
    label: String,
    value: String,
    footnote: String
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium)
            if (footnote.isNotBlank()) {
                Text(footnote, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
