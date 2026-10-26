package com.olehkobylianskyi.familypoints.android.ui.screens

import com.olehkobylianskyi.familypoints.android.BuildConfig
import com.olehkobylianskyi.familypoints.android.update.AndroidUpdateChecker
import com.olehkobylianskyi.familypoints.android.update.AvailableAndroidUpdate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import com.olehkobylianskyi.familypoints.android.ui.components.FpCard as Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.fillMaxHeight
import com.olehkobylianskyi.familypoints.android.network.ServerReachability
import com.olehkobylianskyi.familypoints.android.network.ServerStatus
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.SettingsAccountResponse
import com.olehkobylianskyi.familypoints.android.data.RoleCatalogRepository
import com.olehkobylianskyi.familypoints.android.data.SettingsRepository
import com.olehkobylianskyi.familypoints.android.data.WorkspaceMemberResponse
import com.olehkobylianskyi.familypoints.android.data.MembersRepository
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.ui.components.AppHeader
import com.olehkobylianskyi.familypoints.android.ui.theme.FamilyThemeMode
import kotlinx.coroutines.launch
import retrofit2.HttpException

private fun st(language: AppLanguage, uk: String, de: String, en: String, ru: String) = when (language) {
    AppLanguage.UK -> uk
    AppLanguage.DE -> de
    AppLanguage.EN -> en
    AppLanguage.RU -> ru
}

@Composable
fun SettingsScreen(
    language: AppLanguage,
    themeMode: FamilyThemeMode,
    onThemeModeChange: (FamilyThemeMode) -> Unit,
    currentUser: CurrentUserResponse,
    repository: SettingsRepository,
    membersRepository: MembersRepository,
    roleCatalogRepository: RoleCatalogRepository,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (DashboardDestination) -> Unit,
    onUnauthorized: () -> Unit,
    onUpdateFound: (AvailableAndroidUpdate) -> Unit
) {
    val scope = rememberCoroutineScope()
    val serverStatus by ServerReachability.status.collectAsState()
    val online = serverStatus == ServerStatus.ONLINE
    val canManage = "MANAGE_MEMBERS" in currentUser.permissions || "ADMIN_OVERRIDE" in currentUser.permissions
    var members by remember { mutableStateOf<List<WorkspaceMemberResponse>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<SettingsAccountResponse>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var selectedMember by remember { mutableStateOf<WorkspaceMemberResponse?>(null) }
    var username by remember { mutableStateOf("") }
    var accountPassword by remember { mutableStateOf("") }
    var accountConfirm by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(true) }
    var accountError by remember { mutableStateOf("") }
    var showTemplateLibrary by remember(currentUser.workspaceId) { mutableStateOf(false) }
    var checkingAppUpdate by remember { mutableStateOf(false) }
    var appUpdateStatus by remember { mutableStateOf("") }

    suspend fun reload() {
        if (canManage) {
            members = membersRepository.load(currentUser.workspaceId, currentUser.memberId).members
            accounts = repository.loadAccounts(currentUser.workspaceId)
        }
    }
    fun failure(e: Exception) {
        if (e is HttpException && e.code() == 401) onUnauthorized()
        else if (e is java.io.IOException) error = ""
        else error = e.message ?: "Request failed"
    }
    fun run(action: suspend () -> Unit) {
        if (!online) return
        scope.launch {
            busy = true
            error = ""
            notice = ""
            try {
                action()
                reload()
                notice = st(language, "Збережено", "Gespeichert", "Saved", "Сохранено")
            } catch (e: Exception) {
                failure(e)
            } finally { busy = false }
        }
    }
    LaunchedEffect(currentUser.workspaceId, online) {
        loading = true
        if (online) {
            try {
                reload()
            } catch (e: Exception) {
                failure(e)
            } finally {
                loading = false
            }
        } else {
            error = ""
            selectedMember = null
            loading = false
        }
    }
    val ownPasswordTitle = st(language, "Зміна пароля", "Passwort ändern", "Change password", "Смена пароля")
    val save = st(language, "Зберегти", "Speichern", "Save", "Сохранить")
    val cancel = st(language, "Скасувати", "Abbrechen", "Cancel", "Отмена")

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        AppHeader(language, currentUser, onLanguageChange, onLogout, onNavigate)
        Text(st(language, "Налаштування", "Einstellungen", "Settings", "Настройки"),
            style = MaterialTheme.typography.headlineMedium)
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (notice.isNotBlank()) Text(notice, color = MaterialTheme.colorScheme.primary)
        if (serverStatus == ServerStatus.CHECKING) {
            Text(st(language,
                "Перевірка з’єднання із сервером…",
                "Serververbindung wird geprüft…",
                "Checking server connection…",
                "Проверка соединения с сервером…"),
                color = MaterialTheme.colorScheme.tertiary)
        }
        if (serverStatus == ServerStatus.OFFLINE) {
            Text(st(language,
                "Офлайн: серверні налаштування недоступні. Мову можна змінити у шапці.",
                "Offline: Servereinstellungen nicht verfügbar. Sprache oben ändern.",
                "Offline: server settings unavailable. Change language in the header.",
                "Офлайн: настройки сервера недоступны. Язык можно изменить вверху."),
                color = MaterialTheme.colorScheme.tertiary)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            item {
                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(st(language, "Про застосунок", "Über die App", "About the app", "О приложении"),
                            style = MaterialTheme.typography.titleMedium)
                        Text("FamilyPoints ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})")
                        Button(enabled = !checkingAppUpdate, onClick = {
                            checkingAppUpdate = true
                            appUpdateStatus = ""
                            scope.launch {
                                try {
                                    val update = AndroidUpdateChecker.check()
                                    if (update != null) {
                                        onUpdateFound(update)
                                    } else {
                                        appUpdateStatus = st(language,
                                            "Немає доступного оновлення (або сервер недоступний).",
                                            "Kein Update verfügbar (oder Server nicht erreichbar).",
                                            "No update available (or update server unavailable).",
                                            "Обновлений нет (или сервер недоступен).")
                                    }
                                } finally {
                                    checkingAppUpdate = false
                                }
                            }
                        }) {
                            Text(if (checkingAppUpdate) st(language,
                                "Перевіряємо…", "Prüfe…", "Checking…", "Проверяем…")
                            else st(language, "Перевірити оновлення", "Auf Updates prüfen",
                                "Check for updates", "Проверить обновления"))
                        }
                        if (appUpdateStatus.isNotBlank()) Text(appUpdateStatus,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when (language) {
                        AppLanguage.UK -> "Оформлення"
                        AppLanguage.DE -> "Darstellung"
                        AppLanguage.EN -> "Appearance"
                        AppLanguage.RU -> "Оформление"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FamilyThemeMode.entries.forEach { option ->
                        FilterChip(
                            selected = themeMode == option,
                            onClick = { onThemeModeChange(option) },
                            label = { Text(when (option) {
                                FamilyThemeMode.SYSTEM -> when (language) {
                                    AppLanguage.UK -> "Система"
                                    AppLanguage.DE -> "System"
                                    AppLanguage.EN -> "System"
                                    AppLanguage.RU -> "Система"
                                }
                                FamilyThemeMode.LIGHT -> when (language) {
                                    AppLanguage.UK -> "Світла"
                                    AppLanguage.DE -> "Hell"
                                    AppLanguage.EN -> "Light"
                                    AppLanguage.RU -> "Светлая"
                                }
                                FamilyThemeMode.DARK -> when (language) {
                                    AppLanguage.UK -> "Темна"
                                    AppLanguage.DE -> "Dunkel"
                                    AppLanguage.EN -> "Dark"
                                    AppLanguage.RU -> "Тёмная"
                                }
                            }) }
                        )
                    }
                }
            }
        }
            item {
                if (online) Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(ownPasswordTitle, style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(oldPassword, { oldPassword = it },
                            label = { Text(st(language, "Поточний пароль", "Aktuelles Passwort", "Current password", "Текущий пароль")) },
                            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(newPassword, { newPassword = it },
                            label = { Text(st(language, "Новий пароль", "Neues Passwort", "New password", "Новый пароль")) },
                            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(confirmation, { confirmation = it },
                            label = { Text(st(language, "Повторіть пароль", "Passwort bestätigen", "Confirm password", "Подтвердите пароль")) },
                            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                        val valid = oldPassword.isNotBlank() && newPassword.length >= 8 && newPassword == confirmation
                        Button(enabled = !busy && valid, onClick = {
                            val current = oldPassword
                            val next = newPassword
                            run {
                                repository.changePassword(current, next)
                                oldPassword = ""
                                newPassword = ""
                                confirmation = ""
                            }
                        }) { Text(save) }
                        if (newPassword.isNotBlank() && newPassword.length < 8) {
                            Text(st(language, "Мінімум 8 символів", "Mindestens 8 Zeichen",
                                "At least 8 characters", "Минимум 8 символов"),
                                color = MaterialTheme.colorScheme.error)
                        }
                        if (confirmation.isNotBlank() && confirmation != newPassword) {
                            Text(st(language, "Паролі не збігаються", "Passwörter stimmen nicht überein",
                                "Passwords do not match", "Пароли не совпадают"),
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            if (online) item {
                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(st(language, "Шаблони завдань і нагород", "Aufgaben- und Belohnungsvorlagen",
                            "Task and reward templates", "Шаблоны заданий и наград"),
                            style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { showTemplateLibrary = true }) {
                            Text(st(language, "Відкрити бібліотеку", "Bibliothek öffnen",
                                "Open library", "Открыть библиотеку"))
                        }
                    }
                }
            }
            if (online) item {
                CurrencyFormsSection(
                    language = language,
                    workspaceId = currentUser.workspaceId,
                    canManage = "MANAGE_ECONOMY" in currentUser.permissions || "ADMIN_OVERRIDE" in currentUser.permissions,
                    repository = repository,
                    onUnauthorized = onUnauthorized
                )
            }
            if (online) item {
                RoleCatalogSection(
                    language = language,
                    workspaceId = currentUser.workspaceId,
                    canManage = "MANAGE_ROLES" in currentUser.permissions || "ADMIN_OVERRIDE" in currentUser.permissions,
                    repository = roleCatalogRepository,
                    onUnauthorized = onUnauthorized
                )
            }
            if (canManage && online) {
                item {
                    Text(st(language, "Облікові записи учасників", "Mitgliedskonten",
                        "Member accounts", "Учётные записи участников"),
                        style = MaterialTheme.typography.titleLarge)
                    if (loading) Text(st(language, "Завантаження…", "Laden…", "Loading…", "Загрузка…"))
                }
                items(members, key = { it.id }) { member ->
                    val account = accounts.firstOrNull { it.workspaceMemberId == member.id }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(member.name, style = MaterialTheme.typography.titleMedium)
                                Text(settingsWorkspaceRoleLabel(language, member.workspaceRoleCode, member.workspaceRoleName),
                                    style = MaterialTheme.typography.bodySmall)
                                Text(if (account == null) st(language, "Без акаунта", "Kein Konto", "No account", "Без аккаунта")
                                    else "@${account.username} · " + if (account.enabled)
                                        st(language, "Активний", "Aktiv", "Enabled", "Активный")
                                    else st(language, "Вимкнений", "Deaktiviert", "Disabled", "Отключён"))
                            }
                            TextButton(enabled = !busy, onClick = {
                                selectedMember = member
                                username = account?.username ?: ""
                                accountPassword = ""
                                accountConfirm = ""
                                enabled = account?.enabled ?: true
                                accountError = ""
                            }) {
                                Text(if (account == null) st(language, "Створити", "Erstellen", "Create", "Создать")
                                    else st(language, "Керувати", "Verwalten", "Manage", "Управлять"))
                            }
                        }
                    }
                }
            }
        }
    }
    if (online && showTemplateLibrary) {
        Dialog(
            onDismissRequest = { showTemplateLibrary = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                key(currentUser.workspaceId) {
                    TemplateLibrarySection(
                        language = language,
                        workspaceId = currentUser.workspaceId,
                        canManage = "ADMIN_OVERRIDE" in currentUser.permissions ||
                            "MANAGE_TEMPLATES" in currentUser.permissions ||
                            "MANAGE_TASKS" in currentUser.permissions ||
                            "MANAGE_REWARDS" in currentUser.permissions,
                        repository = remember(currentUser.workspaceId) { repository.templates() },
                        onUnauthorized = onUnauthorized,
                        onClose = { showTemplateLibrary = false }
                    )
                }
            }
        }
    }
    if (online) selectedMember?.let { member ->
        val existing = accounts.firstOrNull { it.workspaceMemberId == member.id }
        AlertDialog(
            onDismissRequest = { if (!busy) selectedMember = null },
            title = { Text(member.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (accountError.isNotBlank()) Text(accountError, color = MaterialTheme.colorScheme.error)
                    OutlinedTextField(username, { username = it },
                        label = { Text(st(language, "Логін", "Benutzername", "Username", "Логин")) })
                    OutlinedTextField(accountPassword, { accountPassword = it },
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text(if (existing == null) st(language, "Початковий пароль", "Initialpasswort", "Initial password", "Начальный пароль")
                            else st(language, "Новий пароль (необов’язково)", "Neues Passwort (optional)",
                                "New password (optional)", "Новый пароль (необязательно)")) })
                    OutlinedTextField(accountConfirm, { accountConfirm = it },
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text(st(language, "Підтвердження", "Bestätigung", "Confirmation", "Подтверждение")) })
                    if (existing != null) {
                        Row {
                            Checkbox(checked = enabled, enabled = member.id != currentUser.memberId,
                                onCheckedChange = { enabled = it })
                            Text(st(language, "Дозволити вхід", "Anmeldung erlauben",
                                "Allow login", "Разрешить вход"))
                        }
                    }
                }
            },
            confirmButton = {
                Button(enabled = !busy && username.isNotBlank(), onClick = {
                    if ((existing == null || accountPassword.isNotEmpty()) &&
                        (accountPassword.length < 8 || accountPassword != accountConfirm)) {
                        accountError = st(language, "Перевір пароль (мінімум 8 символів) і підтвердження",
                            "Passwort (mindestens 8 Zeichen) und Bestätigung prüfen",
                            "Check password (min 8 characters) and confirmation",
                            "Проверьте пароль (минимум 8 символов) и подтверждение")
                    } else {
                        val name = username.trim()
                        val password = accountPassword
                        val allowLogin = enabled
                        selectedMember = null
                        run {
                            if (existing == null) repository.createAccount(currentUser.workspaceId, member.id, name, password)
                            else {
                                repository.updateAccount(currentUser.workspaceId, member.id, name, allowLogin)
                                if (password.isNotBlank()) repository.resetAccountPassword(currentUser.workspaceId, member.id, password)
                            }
                        }
                    }
                }) { Text(save) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { selectedMember = null }) { Text(cancel) } }
        )
    }
}

private fun settingsWorkspaceRoleLabel(language: AppLanguage, code: String?, fallback: String): String =
    when (code?.uppercase()) {
        "FAMILY_ADMIN" -> st(language, "Адміністратор сім’ї", "Familienadministrator",
            "Family administrator", "Администратор семьи")
        "PARENT" -> st(language, "Батько / мати", "Elternteil", "Parent", "Родитель")
        "CHILD" -> st(language, "Дитина", "Kind", "Child", "Ребёнок")
        else -> fallback
    }
