package com.olehkobylianskyi.familypoints.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.dashboardStrings
import com.olehkobylianskyi.familypoints.android.ui.screens.DashboardDestination

@Composable
fun AppHeader(
    language: AppLanguage,
    currentUser: CurrentUserResponse,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (DashboardDestination) -> Unit
) {
    val text = dashboardStrings(language)
    var navigationMenuOpen by remember { mutableStateOf(false) }
    var languageMenuOpen by remember { mutableStateOf(false) }

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

            OutlinedButton(onClick = onLogout) {
                Text(text.logout)
            }
        }
    }
}
