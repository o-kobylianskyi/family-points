package com.olehkobylianskyi.familypoints.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.network.MediaRefDto
import com.olehkobylianskyi.familypoints.android.network.MediaReferenceBatchDto
import com.olehkobylianskyi.familypoints.android.network.MediaTargetDto
import com.olehkobylianskyi.familypoints.android.network.ServerReachability
import com.olehkobylianskyi.familypoints.android.network.ServerStatus
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.dashboardStrings
import com.olehkobylianskyi.familypoints.android.ui.screens.DashboardDestination

/**
 * Variant A — floating popovers.
 * Every DropdownMenu lives directly inside the Box of its own trigger, so
 * Compose PopupPositionProvider anchors it to that trigger instead of the Row.
 */
@Composable
fun AppHeader(
    language: AppLanguage,
    currentUser: CurrentUserResponse,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (DashboardDestination) -> Unit
) {
    val text = dashboardStrings(language)
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var mediaRefresh by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) mediaRefresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val serverStatus by ServerReachability.status.collectAsState()
    var ownAvatar by remember(currentUser.accountId, currentUser.workspaceId, currentUser.memberId) {
        mutableStateOf<MediaRefDto?>(null)
    }
    val bearer = if (serverStatus == ServerStatus.ONLINE)
        tokenStore.getAccessToken()?.let { "Bearer $it" } else null
    LaunchedEffect(currentUser.accountId, currentUser.workspaceId, currentUser.memberId, bearer, mediaRefresh) {
        ownAvatar = null // No cross-workspace or cross-account image may remain visible.
        val authorized = bearer ?: return@LaunchedEffect
        try {
            val items = ApiClient.mediaApi.references(
                authorized, currentUser.workspaceId,
                MediaReferenceBatchDto(listOf(MediaTargetDto("MEMBER", currentUser.memberId, "AVATAR")))
            )
            ownAvatar = items.firstOrNull()?.image
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            // Optional media failures must not end login.
        }
    }
    var navigationMenuOpen by remember { mutableStateOf(false) }
    var languageMenuOpen by remember { mutableStateOf(false) }
    val menuSurface = MaterialTheme.colorScheme.surface
    val selectedSurface = MaterialTheme.colorScheme.primaryContainer
    val selectedText = MaterialTheme.colorScheme.onPrimaryContainer

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = if (navigationMenuOpen) selectedSurface else MaterialTheme.colorScheme.surfaceVariant
            ) {
                IconButton(onClick = {
                    languageMenuOpen = false
                    navigationMenuOpen = !navigationMenuOpen
                }) {
                    Icon(Icons.Outlined.Menu, contentDescription = text.mainSections,
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            DropdownMenu(
                expanded = navigationMenuOpen,
                onDismissRequest = { navigationMenuOpen = false },
                modifier = Modifier
                    .width(240.dp)
                    .background(menuSurface, RoundedCornerShape(22.dp)),
                offset = DpOffset(x = 0.dp, y = 7.dp)
            ) {
                val entries = listOf(
                    Triple(DashboardDestination.DASHBOARD, text.dashboard, Icons.Outlined.Home),
                    Triple(DashboardDestination.TASKS, text.tasks, Icons.Outlined.TaskAlt),
                    Triple(DashboardDestination.POINTS, when (language) {
                        AppLanguage.UK -> "Бали"
                        AppLanguage.RU -> "Баллы"
                        AppLanguage.DE -> "Punkte"
                        AppLanguage.EN -> "Points"
                    }, Icons.Outlined.Payments),
                    Triple(DashboardDestination.REWARDS, text.rewards, Icons.Outlined.CardGiftcard),
                    Triple(DashboardDestination.MEMBERS, text.members, Icons.Outlined.Group),
                    Triple(DashboardDestination.SETTINGS, text.settings, Icons.Outlined.Settings)
                )
                entries.forEachIndexed { index, (destination, title, glyph) ->
                    DropdownMenuItem(
                        modifier = Modifier.background(
                            if (index == 0) selectedSurface else menuSurface,
                            RoundedCornerShape(12.dp)
                        ),
                        text = { Text(title, color = if (index == 0) selectedText else MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(glyph, contentDescription = null,
                            tint = if (index == 0) selectedText else MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = {
                            navigationMenuOpen = false
                            onNavigate(destination)
                        }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DropdownMenuItem(
                    text = { Text(text.logout) },
                    leadingIcon = { Icon(Icons.Outlined.Logout, contentDescription = null) },
                    onClick = {
                        navigationMenuOpen = false
                        onLogout()
                    }
                )
            }
        }

        MemberMediaAvatar(
            workspaceId = currentUser.workspaceId,
            bearer = bearer,
            image = ownAvatar,
            name = currentUser.memberName
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currentUser.memberName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = localizedHeaderRole(language, currentUser.workspaceRoleCode, currentUser.workspaceRoleName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = if (languageMenuOpen) selectedSurface else MaterialTheme.colorScheme.surfaceVariant
            ) {
                TextButton(onClick = {
                    navigationMenuOpen = false
                    languageMenuOpen = !languageMenuOpen
                }) {
                    Text(language.name, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp))
                }
            }
            DropdownMenu(
                expanded = languageMenuOpen,
                onDismissRequest = { languageMenuOpen = false },
                modifier = Modifier
                    .width(210.dp)
                    .background(menuSurface, RoundedCornerShape(22.dp)),
                offset = DpOffset(x = 0.dp, y = 7.dp)
            ) {
                AppLanguage.entries.forEach { item ->
                    val selected = item == language
                    val flag = when (item) {
                        AppLanguage.UK -> "🇺🇦"
                        AppLanguage.DE -> "🇩🇪"
                        AppLanguage.EN -> "🇬🇧"
                        AppLanguage.RU -> "🇷🇺"
                    }
                    DropdownMenuItem(
                        modifier = Modifier.background(
                            if (selected) selectedSurface else menuSurface,
                            RoundedCornerShape(12.dp)
                        ),
                        text = { Text(item.label, maxLines = 1,
                            color = if (selected) selectedText else MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Text(flag) },
                        trailingIcon = if (selected) {
                            { Icon(Icons.Outlined.Check, contentDescription = null, tint = selectedText) }
                        } else null,
                        onClick = {
                            languageMenuOpen = false
                            onLanguageChange(item)
                        }
                    )
                }
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
