package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.DashboardData
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.dashboardStrings
import com.olehkobylianskyi.familypoints.android.i18n.taskStatusLabel
import retrofit2.HttpException

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
    language: AppLanguage,
    currentUser: CurrentUserResponse,
    loadDashboard: suspend () -> DashboardData,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (DashboardDestination) -> Unit,
    onUnauthorized: () -> Unit
) {
    val text = dashboardStrings(language)

    var data by remember { mutableStateOf<DashboardData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var languageMenuOpen by remember { mutableStateOf(false) }
    var navigationMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser.memberId, currentUser.workspaceId) {
        loading = true
        error = ""

        try {
            data = loadDashboard()
        } catch (exception: HttpException) {
            if (exception.code() == 401) {
                onUnauthorized()
            } else {
                error = exception.message()
            }
        } catch (exception: Exception) {
            error = exception.message ?: text.loadFailed
        } finally {
            loading = false
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column {
                        OutlinedButton(
                            onClick = { navigationMenuOpen = true }
                        ) {
                            Text("☰")
                        }

                        DropdownMenu(
                            expanded = navigationMenuOpen,
                            onDismissRequest = {
                                navigationMenuOpen = false
                            }
                        ) {
                            listOf(
                                DashboardDestination.DASHBOARD to text.dashboard,
                                DashboardDestination.MEMBERS to text.members,
                                DashboardDestination.TASKS to text.tasks,
                                DashboardDestination.POINTS to text.points,
                                DashboardDestination.REWARDS to text.rewards,
                                DashboardDestination.SETTINGS to text.settings
                            ).forEach { entry ->
                                DropdownMenuItem(
                                    text = { Text(entry.second) },
                                    onClick = {
                                        navigationMenuOpen = false
                                        onNavigate(entry.first)
                                    }
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = currentUser.memberName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = currentUser.workspaceRoleName,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column {
                        OutlinedButton(
                            onClick = { languageMenuOpen = true }
                        ) {
                            Text(language.label)
                        }

                        DropdownMenu(
                            expanded = languageMenuOpen,
                            onDismissRequest = {
                                languageMenuOpen = false
                            }
                        ) {
                            AppLanguage.entries.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.label) },
                                    onClick = {
                                        onLanguageChange(item)
                                        languageMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onLogout
                    ) {
                        Text(text.logout)
                    }
                }
            }
        }

        when {
            loading -> {
                item {
                    Text(text.loading)
                }
            }

            error.isNotBlank() -> {
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            data != null -> {
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

                item {
                    DashboardStatCard(
                        label = text.myBalance,
                        value = dashboard.balance.toString(),
                        description = text.points
                    )
                }

                item {
                    DashboardStatCard(
                        label = text.myTasks,
                        value = active.size.toString(),
                        description = text.activeToday
                    )
                }

                item {
                    DashboardStatCard(
                        label = text.availableToClaim,
                        value = dashboard.openTasks.size.toString(),
                        description = text.openTasks
                    )
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

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = text.quickActions,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = text.mainSections,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Button(
                                onClick = {
                                    onNavigate(DashboardDestination.TASKS)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text.tasks)
                            }

                            OutlinedButton(
                                onClick = {
                                    onNavigate(DashboardDestination.POINTS)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text(text.points)
                            }

                            OutlinedButton(
                                onClick = {
                                    onNavigate(DashboardDestination.REWARDS)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text(text.rewards)
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
        modifier = Modifier.fillMaxWidth()
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
            Text(
                text = description,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
