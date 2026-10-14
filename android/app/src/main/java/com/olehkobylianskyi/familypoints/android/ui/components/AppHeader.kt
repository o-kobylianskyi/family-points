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
import androidx.compose.ui.text.style.TextOverflow
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

    // Narrow phones: identity occupies its own line; controls never squeeze text into letters.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column {
                OutlinedButton(onClick = { navigationMenuOpen = true }) {
                    Text("☰")
                }
                DropdownMenu(
                    expanded = navigationMenuOpen,
                    onDismissRequest = { navigationMenuOpen = false }
                ) {
                    listOf(
                        DashboardDestination.DASHBOARD to text.dashboard,
                        DashboardDestination.MEMBERS to text.members,
                        DashboardDestination.TASKS to text.tasks,
                        DashboardDestination.POINTS to when (language) {
                            AppLanguage.UK -> "Бали"
                            AppLanguage.RU -> "Баллы"
                            AppLanguage.DE -> "Punkte"
                            AppLanguage.EN -> "Points"
                        },
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentUser.memberName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = localizedHeaderRole(language, currentUser.workspaceRoleCode, currentUser.workspaceRoleName),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column {
                OutlinedButton(onClick = { languageMenuOpen = true }) {
                    Text(language.label, maxLines = 1, softWrap = false)
                }
                DropdownMenu(
                    expanded = languageMenuOpen,
                    onDismissRequest = { languageMenuOpen = false }
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
                Text(text.logout, maxLines = 1, softWrap = false)
            }
        }
    }
}

private fun localizedHeaderRole(language: AppLanguage, code: String?, fallback: String): String =
    when (code?.uppercase()) {
        "FAMILY_ADMIN" -> when (language) {
            AppLanguage.UK -> "Адміністратор сім’ї"
            AppLanguage.RU -> "Администратор семьи"
            AppLanguage.DE -> "Familienadministrator"
            AppLanguage.EN -> "Family administrator"
        }
        "PARENT" -> when (language) {
            AppLanguage.UK -> "Батько"
            AppLanguage.RU -> "Родитель"
            AppLanguage.DE -> "Elternteil"
            AppLanguage.EN -> "Parent"
        }
        "CHILD" -> when (language) {
            AppLanguage.UK -> "Дитина"
            AppLanguage.RU -> "Ребёнок"
            AppLanguage.DE -> "Kind"
            AppLanguage.EN -> "Child"
        }
        else -> fallback
    }
