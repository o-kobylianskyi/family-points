package com.olehkobylianskyi.familypoints.android

import android.graphics.Color
import android.os.Bundle
import android.os.Build
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.ui.components.WorkspaceSwitcher
import com.olehkobylianskyi.familypoints.android.auth.WorkspaceSelectionRequiredException
import com.olehkobylianskyi.familypoints.android.ui.screens.WorkspaceOnboardingScreen
import com.olehkobylianskyi.familypoints.android.auth.AuthSessionManager
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.CreateTaskRepository
import com.olehkobylianskyi.familypoints.android.data.DashboardRepository
import com.olehkobylianskyi.familypoints.android.data.RoleCatalogRepository
import com.olehkobylianskyi.familypoints.android.data.SettingsRepository
import com.olehkobylianskyi.familypoints.android.data.MembersRepository
import com.olehkobylianskyi.familypoints.android.data.PointsRepository
import com.olehkobylianskyi.familypoints.android.data.RewardNegotiationRepository
import com.olehkobylianskyi.familypoints.android.data.RewardsRepository
import com.olehkobylianskyi.familypoints.android.data.TaskDetailsRepository
import com.olehkobylianskyi.familypoints.android.data.TasksRepository
import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.network.ServerReachability
import com.olehkobylianskyi.familypoints.android.network.ServerStatus
import com.olehkobylianskyi.familypoints.android.ui.components.ServerStatusBanner
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.dashboardStrings
import com.olehkobylianskyi.familypoints.android.i18n.strings
import com.olehkobylianskyi.familypoints.android.storage.LastUsernameStore
import com.olehkobylianskyi.familypoints.android.storage.LanguageStore
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import com.olehkobylianskyi.familypoints.android.ui.screens.CreateTaskScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.DashboardDestination
import com.olehkobylianskyi.familypoints.android.ui.screens.CalendarScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.DashboardScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.SettingsScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.MembersScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.LoginScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.PlaceholderScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.PointsScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.RewardNegotiationScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.RewardsScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.TaskDetailsScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.TasksScreen
import com.olehkobylianskyi.familypoints.android.ui.theme.FamilyPointsTheme
import com.olehkobylianskyi.familypoints.android.ui.theme.FamilyThemeMode
import com.olehkobylianskyi.familypoints.android.ui.theme.FamilyThemePreference
import com.olehkobylianskyi.familypoints.android.update.AndroidApkInstaller
import com.olehkobylianskyi.familypoints.android.update.AndroidUpdateChecker
import com.olehkobylianskyi.familypoints.android.update.AvailableAndroidUpdate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AppDestination {
    DASHBOARD,
    CALENDAR,
    TASKS,
    TASK_DETAILS,
    CREATE_TASK,
    REWARD_NEGOTIATION,
    MEMBERS,
    POINTS,
    REWARDS,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var authSessionManager: AuthSessionManager

    @Suppress("DEPRECATION") // Required only for Android versions before edge-to-edge enforcement.
    private fun configureLegacySystemBars() {
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 15+ controls system bar surfaces using edge-to-edge drawing.
        // Keep explicit white system bars only for older Android versions.
        if (Build.VERSION.SDK_INT < 35) {
            configureLegacySystemBars()
        }

        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        val tokenStore = TokenStore(this)
        val languageStore = LanguageStore(this)
        val lastUsernameStore = LastUsernameStore(this)
        val dashboardRepository = DashboardRepository(tokenStore, com.olehkobylianskyi.familypoints.android.storage.OfflineDashboardStore(this))
        val tasksRepository = TasksRepository(tokenStore, com.olehkobylianskyi.familypoints.android.storage.OfflineTasksStore(this))
        val createTaskRepository = CreateTaskRepository(tokenStore)
        val taskDetailsRepository = TaskDetailsRepository(tokenStore)
        val rewardNegotiationRepository =
            RewardNegotiationRepository(tokenStore)
        val rewardsRepository = RewardsRepository(tokenStore, com.olehkobylianskyi.familypoints.android.storage.OfflineRewardsStore(this))
        val offlineMembersStore = com.olehkobylianskyi.familypoints.android.storage.OfflineMembersStore(this)
        val pointsRepository = PointsRepository(
            tokenStore,
            com.olehkobylianskyi.familypoints.android.storage.OfflineBalanceStore(this),
            offlineMembersStore,
            com.olehkobylianskyi.familypoints.android.storage.OfflinePointsQueueStore(this),
            com.olehkobylianskyi.familypoints.android.storage.OfflinePointTypesStore(this)
        )
        val membersRepository = MembersRepository(tokenStore, offlineMembersStore)
        val settingsRepository = SettingsRepository(tokenStore)
        val roleCatalogRepository = RoleCatalogRepository(tokenStore)
        authSessionManager = AuthSessionManager(tokenStore, com.olehkobylianskyi.familypoints.android.storage.OfflineAccountStore(this))

        val themePreference = FamilyThemePreference(this)
        setContent {
            var themeMode by remember { mutableStateOf(themePreference.get()) }
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val dark = when (themeMode) {
                FamilyThemeMode.DARK -> true
                FamilyThemeMode.LIGHT -> false
                FamilyThemeMode.SYSTEM -> systemDark
            }
            SideEffect {
                if (Build.VERSION.SDK_INT < 35) {
                    window.statusBarColor = if (dark) Color.rgb(16, 23, 30) else Color.rgb(245, 250, 255)
                    window.navigationBarColor = if (dark) Color.rgb(16, 23, 30) else Color.rgb(245, 250, 255)
                }
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            FamilyPointsTheme(mode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    var language by remember {
                        mutableStateOf(languageStore.getLanguage())
                    }
                    val status by ServerReachability.status.collectAsState()
                    val connectionScope = rememberCoroutineScope()
                    var availableUpdate by remember { mutableStateOf<AvailableAndroidUpdate?>(null) }
                    var skippedUpdateVersion by remember { mutableStateOf<Int?>(null) }
                    var updateDownloading by remember { mutableStateOf(false) }
                    var updateProgress by remember { mutableStateOf(0) }
                    var updateError by remember { mutableStateOf<String?>(null) }

                    // Foreground check on app launch and every six hours while open.
                    // The public manifest requires no session and offline failures are ignored.
                    LaunchedEffect(Unit) {
                        while (true) {
                            val next = AndroidUpdateChecker.check()
                            if (next != null && next.versionCode != skippedUpdateVersion) {
                                availableUpdate = next
                            }
                            delay(6 * 60 * 60 * 1000L)
                        }
                    }
                    availableUpdate?.let { update ->
                        AlertDialog(
                            onDismissRequest = {
                                if (!updateDownloading) {
                                    skippedUpdateVersion = update.versionCode
                                    availableUpdate = null
                                }
                            },
                            title = { Text(when (language) {
                                AppLanguage.UK -> "Доступне оновлення"
                                AppLanguage.DE -> "Update verfügbar"
                                AppLanguage.EN -> "Update available"
                                AppLanguage.RU -> "Доступно обновление"
                            }) },
                            text = { Text(if (updateDownloading) {
                                when (language) {
                                    AppLanguage.UK -> "Завантаження та перевірка APK: $updateProgress%"
                                    AppLanguage.DE -> "APK wird geladen und geprüft: $updateProgress%"
                                    AppLanguage.EN -> "Downloading and verifying APK: $updateProgress%"
                                    AppLanguage.RU -> "Загрузка и проверка APK: $updateProgress%"
                                }
                            } else if (updateError != null) {
                                updateError ?: ""
                            } else when (language) {
                                AppLanguage.UK -> "Нова версія FamilyPoints ${update.versionName}. Завантажити та встановити?"
                                AppLanguage.DE -> "FamilyPoints ${update.versionName} ist verfügbar. Herunterladen und installieren?"
                                AppLanguage.EN -> "FamilyPoints ${update.versionName} is available. Download and install?"
                                AppLanguage.RU -> "Доступна новая версия FamilyPoints ${update.versionName}. Скачать и установить?"
                            }) },
                            confirmButton = {
                                TextButton(enabled = !updateDownloading, onClick = {
                                    updateError = null
                                    updateProgress = 0
                                    updateDownloading = true
                                    connectionScope.launch {
                                        try {
                                            val apk = AndroidApkInstaller.download(this@MainActivity, update) { progress ->
                                                // File I/O runs off-main; state updates stay on the UI dispatcher.
                                                connectionScope.launch { updateProgress = progress }
                                            }
                                            updateDownloading = false
                                            skippedUpdateVersion = update.versionCode
                                            availableUpdate = null
                                            AndroidApkInstaller.install(this@MainActivity, apk)
                                        } catch (e: Exception) {
                                            updateDownloading = false
                                            updateError = e.message ?: "APK update failed"
                                        }
                                    }
                                }) { Text(when (language) {
                                    AppLanguage.UK -> "Завантажити"
                                    AppLanguage.DE -> "Herunterladen"
                                    AppLanguage.EN -> "Download"
                                    AppLanguage.RU -> "Скачать"
                                }) }
                            },
                            dismissButton = {
                                TextButton(enabled = !updateDownloading, onClick = {
                                    skippedUpdateVersion = update.versionCode
                                    availableUpdate = null
                                }) { Text(when (language) {
                                    AppLanguage.UK -> "Пізніше"
                                    AppLanguage.DE -> "Später"
                                    AppLanguage.EN -> "Later"
                                    AppLanguage.RU -> "Позже"
                                }) }
                            }
                        )
                    }

                    // Check actual backend availability, including Wi-Fi with no API.
                    LaunchedEffect(Unit) {
                        while (true) {
                            ApiClient.probeServer()
                            delay(if (ServerReachability.status.value == ServerStatus.OFFLINE) {
                                20_000L
                            } else {
                                60_000L
                            })
                        }
                    }

                    if (status == ServerStatus.OFFLINE) {
                        ServerStatusBanner(
                            language = language,
                            onRetry = {
                                // Keep the offline banner visible until the health
                                // probe actually confirms this backend instance.
                                connectionScope.launch { ApiClient.probeServer() }
                            }
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(modifier = Modifier.fillMaxSize()) {
                var currentUser by remember {
                    mutableStateOf<CurrentUserResponse?>(null)
                }

                var needsWorkspaceSelection by remember { mutableStateOf(false) }

                var authLoading by remember {
                    mutableStateOf(true)
                }

                var showOnlineReauthNotice by remember {
                    mutableStateOf(false)
                }

                var destination by remember {
                    mutableStateOf(AppDestination.DASHBOARD)
                }

                var selectedTaskDefinitionId by remember {
                    mutableStateOf<Long?>(null)
                }

                var selectedTaskDate by remember {
                    mutableStateOf("")
                }

                var selectedTaskSlotKey by remember { mutableStateOf("default") }

                var selectedTaskInstanceId by remember {
                    mutableStateOf<Long?>(null)
                }

                var parentTaskDefinitionId by remember {
                    mutableStateOf<Long?>(null)
                }

                LaunchedEffect(Unit) {
                    currentUser = authSessionManager.restoreSession()
                    needsWorkspaceSelection = authSessionManager.hasUnselectedWorkspace()
                    authLoading = false
                }

                // An offline password proves access to local snapshots only.
                // Once the API comes back, require fresh online authentication
                // rather than silently granting server-side write permissions.
                LaunchedEffect(status, currentUser?.memberId) {
                    if (currentUser != null &&
                        status == ServerStatus.ONLINE &&
                        authSessionManager.isOfflineAuthenticated()
                    ) {
                        authSessionManager.clearSession()
                        currentUser = null
                        destination = AppDestination.DASHBOARD
                        showOnlineReauthNotice = true
                    }
                }

                // Sync locally queued parent credits/penalties after an online
                // authentication, even when the Points page isn't open.
                LaunchedEffect(status, currentUser?.memberId) {
                    val user = currentUser
                    if (status == ServerStatus.ONLINE &&
                        user != null &&
                        !authSessionManager.isOfflineAuthenticated() &&
                        (user.permissions.contains("MANAGE_POINTS") ||
                            user.permissions.contains("ADMIN_OVERRIDE"))
                    ) {
                        try {
                            pointsRepository.synchronizePending(user.workspaceId, user.memberId)
                        } catch (e: retrofit2.HttpException) {
                            if (e.code() == 401) {
                                authSessionManager.clearSession(invalidateOfflineAccount = true)
                                currentUser = null
                                destination = AppDestination.DASHBOARD
                            }
                        } catch (_: Exception) {
                            // Keep durable pending operations for next reconnect.
                        }
                    }
                }

                LaunchedEffect(currentUser) {
                    while (currentUser != null) {
                        delay(60_000L)

                        if (!authSessionManager.refreshIfNeeded()) {
                            currentUser = null
                            destination = AppDestination.DASHBOARD
                        }
                    }
                }

                val unauthorized = {
                    authSessionManager.clearSession(invalidateOfflineAccount = true)
                    currentUser = null
                    destination = AppDestination.DASHBOARD
                }

                val changeLanguage: (AppLanguage) -> Unit = { selected ->
                    languageStore.saveLanguage(selected)
                    language = selected
                }

                val logout = {
                    authSessionManager.clearSession()
                    currentUser = null
                    destination = AppDestination.DASHBOARD
                }

                val navigate: (DashboardDestination) -> Unit = { target ->
                    destination = when (target) {
                        DashboardDestination.DASHBOARD ->
                            AppDestination.DASHBOARD
                        DashboardDestination.MEMBERS ->
                            AppDestination.MEMBERS
                        DashboardDestination.TASKS ->
                            AppDestination.TASKS
                        DashboardDestination.POINTS ->
                            AppDestination.POINTS
                        DashboardDestination.REWARDS ->
                            AppDestination.REWARDS
                        DashboardDestination.SETTINGS ->
                            AppDestination.SETTINGS
                    }
                }

                var refreshNonce by remember { mutableStateOf(0) }
                var pullRefreshing by remember { mutableStateOf(false) }
                val pullScope = rememberCoroutineScope()
                val refreshable = currentUser != null && status == ServerStatus.ONLINE &&
                    destination in listOf(
                        AppDestination.DASHBOARD, AppDestination.TASKS, AppDestination.MEMBERS,
                        AppDestination.POINTS, AppDestination.REWARDS
                    )
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    PullToRefreshBox(
                        isRefreshing = pullRefreshing,
                        onRefresh = {
                            if (refreshable && !pullRefreshing) {
                                pullRefreshing = true
                                refreshNonce++
                                pullScope.launch {
                                    delay(1200L)
                                    pullRefreshing = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                when {
                    authLoading -> {
                        LoadingScreen(language)
                    }

                    needsWorkspaceSelection -> {
                        WorkspaceOnboardingScreen(
                            loadChoices = { authSessionManager.listWorkspaces() },
                            choose = { id -> authSessionManager.switchWorkspace(id) },
                            create = { family, owner -> authSessionManager.createFirstFamily(family, owner) },
                            onReady = { user ->
                                needsWorkspaceSelection = false
                                currentUser = user
                                destination = AppDestination.DASHBOARD
                            },
                            onLogout = {
                                authSessionManager.clearSession()
                                needsWorkspaceSelection = false
                                currentUser = null
                            }
                        )
                    }

                    currentUser == null -> {
                        LoginScreen(
                            language = language,
                            showOnlineReauthNotice = showOnlineReauthNotice,
                            initialUsername = lastUsernameStore.get(),
                            onRememberUsername = { lastUsernameStore.save(it) },
                            onLanguageChange = { selected ->
                                languageStore.saveLanguage(selected)
                                language = selected
                            },
                            onLogin = { username, password ->
                                try {
                                    authSessionManager.login(username, password)
                                } catch (error: WorkspaceSelectionRequiredException) {
                                    needsWorkspaceSelection = true
                                    throw error
                                }
                            },
                            onRegister = { name, email, pass -> authSessionManager.registerAccount(name, email, pass) },
                            onGoogleAuth = { idToken, createAccount ->
                                try {
                                    authSessionManager.loginWithGoogle(idToken, createAccount)
                                } catch (error: WorkspaceSelectionRequiredException) {
                                    needsWorkspaceSelection = true
                                    throw error
                                }
                            },
                            onLoginSuccess = { user ->
                                showOnlineReauthNotice = false
                                currentUser = user
                                destination = AppDestination.DASHBOARD
                            }
                        )
                    }

                    destination == AppDestination.DASHBOARD -> {
                        DashboardScreen(
                            refreshNonce = refreshNonce,
                            language = language,
                            currentUser = currentUser!!,
                            loadDashboard = {
                                dashboardRepository.load(currentUser!!)
                            },
                            cachedDashboard = {
                                dashboardRepository.cached(currentUser!!)
                            },
                            loadReputation = {
                                val member = currentUser!!
                                val token = tokenStore.getAccessToken() ?: error("Not signed in")
                                val response = ApiClient.authApi.getMyReputation("Bearer $token")
                                check(response.memberId == member.memberId && response.workspaceId == member.workspaceId) {
                                    "Reputation identity mismatch"
                                }
                                response.balance
                            },
                            loadWorkspaces = {
                                if (authSessionManager.isOfflineAuthenticated()) emptyList()
                                else authSessionManager.listWorkspaces()
                            },
                            canSwitchWorkspaces = !authSessionManager.isOfflineAuthenticated(),
                            onSwitchWorkspace = { workspaceId ->
                                val user = authSessionManager.switchWorkspace(workspaceId)
                                selectedTaskDefinitionId = null
                                selectedTaskInstanceId = null
                                selectedTaskDate = ""
                                parentTaskDefinitionId = null
                                currentUser = user
                                destination = AppDestination.DASHBOARD
                            },
                            onLanguageChange = changeLanguage,
                            onLogout = logout,
                            onNavigate = navigate,
                            onUnauthorized = unauthorized
                        )
                    }

                    destination == AppDestination.CALENDAR -> {
                        CalendarScreen(language = language)
                    }

                    destination == AppDestination.TASKS -> {
                        TasksScreen(
                            refreshNonce = refreshNonce,
                            language = language,
                            currentUser = currentUser!!,
                            repository = tasksRepository,
                            onLanguageChange = changeLanguage,
                            onLogout = logout,
                            onNavigate = navigate,
                            onBack = {
                                destination = AppDestination.DASHBOARD
                            },
                            onOpenTask = { definitionId, date, slotKey ->
                                selectedTaskDefinitionId = definitionId
                                selectedTaskDate = date
                                selectedTaskSlotKey = slotKey
                                destination = AppDestination.TASK_DETAILS
                            },
                            onCreateTask = {
                                destination = AppDestination.CREATE_TASK
                            },
                            onUnauthorized = unauthorized
                        )
                    }

                    destination == AppDestination.TASK_DETAILS -> {
                        val definitionId = selectedTaskDefinitionId

                        if (definitionId == null) {
                            destination = AppDestination.TASKS
                        } else {
                            TaskDetailsScreen(
                                language = language,
                                currentUser = currentUser!!,
                                definitionId = definitionId,
                                selectedDate = selectedTaskDate,
                                selectedSlotKey = selectedTaskSlotKey,
                                repository = taskDetailsRepository,
                                settingsRepository = settingsRepository,
                                onLanguageChange = changeLanguage,
                                onLogout = logout,
                                onNavigate = navigate,
                                onBack = {
                                    destination = AppDestination.TASKS
                                },
                                onOpenTask = { childId, date ->
                                    selectedTaskDefinitionId = childId
                                    selectedTaskDate = date
                                    selectedTaskSlotKey = "default"
                                },
                                onCreateSubtask = { parentId ->
                                    parentTaskDefinitionId = parentId
                                    destination = AppDestination.CREATE_TASK
                                },
                                onRequestReward = { instanceId ->
                                    selectedTaskInstanceId = instanceId
                                    destination =
                                        AppDestination.REWARD_NEGOTIATION
                                },
                                onUnauthorized = unauthorized
                            )
                        }
                    }

                    destination == AppDestination.CREATE_TASK -> {
                        CreateTaskScreen(
                            language = language,
                            currentUser = currentUser!!,
                            parentTaskDefinitionId = parentTaskDefinitionId,
                            repository = createTaskRepository,
                            onLanguageChange = changeLanguage,
                            onLogout = logout,
                            onNavigate = navigate,
                            onCancel = {
                                destination =
                                    if (parentTaskDefinitionId != null) {
                                        AppDestination.TASK_DETAILS
                                    } else {
                                        AppDestination.TASKS
                                    }
                                parentTaskDefinitionId = null
                            },
                            onCreated = {
                                destination =
                                    if (parentTaskDefinitionId != null) {
                                        AppDestination.TASK_DETAILS
                                    } else {
                                        AppDestination.TASKS
                                    }
                                parentTaskDefinitionId = null
                            },
                            onUnauthorized = unauthorized
                        )
                    }

                    destination == AppDestination.REWARD_NEGOTIATION -> {
                        val definitionId = selectedTaskDefinitionId
                        val instanceId = selectedTaskInstanceId

                        if (
                            definitionId == null ||
                            instanceId == null ||
                            selectedTaskDate.isBlank()
                        ) {
                            destination = AppDestination.TASK_DETAILS
                        } else {
                            RewardNegotiationScreen(
                                language = language,
                                currentUser = currentUser!!,
                                definitionId = definitionId,
                                selectedDate = selectedTaskDate,
                                instanceId = instanceId,
                                repository =
                                    rewardNegotiationRepository,
                                onLanguageChange = changeLanguage,
                                onLogout = logout,
                                onNavigate = navigate,
                                onBack = {
                                    destination =
                                        AppDestination.TASK_DETAILS
                                },
                                onSaved = {
                                    destination =
                                        AppDestination.TASK_DETAILS
                                },
                                onUnauthorized = unauthorized
                            )
                        }
                    }

                    destination == AppDestination.MEMBERS -> {
                        MembersScreen(
                            refreshNonce = refreshNonce,
                            language = language,
                            currentUser = currentUser!!,
                            repository = membersRepository,
                            pointsRepository = pointsRepository,
                            onLanguageChange = changeLanguage,
                            onLogout = logout,
                            onNavigate = navigate,
                            onUnauthorized = unauthorized
                        )
                    }

                    destination == AppDestination.POINTS -> {
                        PointsScreen(
                            refreshNonce = refreshNonce,
                            language = language,
                            currentUser = currentUser!!,
                            repository = pointsRepository,
                            rewardsRepository = rewardsRepository,
                            settingsRepository = settingsRepository,
                            onLanguageChange = changeLanguage,
                            onLogout = logout,
                            onNavigate = navigate,
                            onUnauthorized = unauthorized
                        )
                    }

                    destination == AppDestination.REWARDS -> {
                        RewardsScreen(
                            refreshNonce = refreshNonce,
                            language = language,
                            currentUser = currentUser!!,
                            repository = rewardsRepository,
                            onLanguageChange = changeLanguage,
                            onLogout = logout,
                            onNavigate = navigate,
                            onUnauthorized = unauthorized
                        )
                    }

                    destination == AppDestination.SETTINGS -> {
                        SettingsScreen(
                            language = language,
                            themeMode = themeMode,
                            onThemeModeChange = { selected -> themeMode = selected; themePreference.set(selected) },
                            currentUser = currentUser!!,
                            repository = settingsRepository,
                            membersRepository = membersRepository,
                            roleCatalogRepository = roleCatalogRepository,
                            onLanguageChange = changeLanguage,
                            onLogout = logout,
                            onNavigate = navigate,
                            onUnauthorized = unauthorized,
                            onUpdateFound = { update ->
                                skippedUpdateVersion = null
                                availableUpdate = update
                            }
                        )
                    }

                    else -> {
                        val dashboardText = dashboardStrings(language)
                        val title = when (destination) {
                            AppDestination.MEMBERS -> dashboardText.members
                            AppDestination.POINTS -> dashboardText.points
                            AppDestination.SETTINGS -> dashboardText.settings
                            else -> ""
                        }

                        PlaceholderScreen(
                            language = language,
                            currentUser = currentUser!!,
                            title = title,
                            onLanguageChange = changeLanguage,
                            onLogout = logout,
                            onNavigate = navigate,
                            onBack = {
                                destination = AppDestination.DASHBOARD
                            }
                        )
                    }
                }
                    }
                    }
                    // Fixed native navigation remains visible on all main screens.
                    if (currentUser != null && !authLoading &&
                        destination in listOf(AppDestination.DASHBOARD, AppDestination.CALENDAR, AppDestination.TASKS,
                            AppDestination.MEMBERS, AppDestination.POINTS, AppDestination.REWARDS,
                            AppDestination.SETTINGS)) {
                        NavigationBar(
                            modifier = Modifier.fillMaxWidth().height(76.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            val destinations = listOf(
                                Triple(AppDestination.DASHBOARD, Icons.Outlined.Home,
                                    when(language) { AppLanguage.UK -> "Головна"; AppLanguage.DE -> "Start"; AppLanguage.EN -> "Home"; AppLanguage.RU -> "Главная" }),
                                Triple(AppDestination.CALENDAR, Icons.Outlined.DateRange,
                                    when(language) { AppLanguage.UK -> "Календар"; AppLanguage.DE -> "Kalender"; AppLanguage.EN -> "Calendar"; AppLanguage.RU -> "Календарь" }),
                                Triple(AppDestination.TASKS, Icons.Outlined.Checklist,
                                    when(language) { AppLanguage.UK -> "Завдання"; AppLanguage.DE -> "Aufgaben"; AppLanguage.EN -> "Tasks"; AppLanguage.RU -> "Задачи" }),
                                Triple(AppDestination.MEMBERS, Icons.Outlined.Group,
                                    when(language) { AppLanguage.UK -> "Учасники"; AppLanguage.DE -> "Familie"; AppLanguage.EN -> "Members"; AppLanguage.RU -> "Участники" }),
                                Triple(AppDestination.REWARDS, Icons.Outlined.CardGiftcard,
                                    when(language) { AppLanguage.UK -> "Нагороди"; AppLanguage.DE -> "Prämien"; AppLanguage.EN -> "Rewards"; AppLanguage.RU -> "Награды" })
                            )
                            destinations.forEach { (target, glyph, caption) ->
                                NavigationBarItem(
                                    selected = destination == target,
                                    onClick = { destination = target },
                                    icon = { Icon(glyph, contentDescription = caption) },
                                    label = { Text(caption, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                    alwaysShowLabel = false
                                )
                            }
                        }
                    }
                    }
                    }
                }
                }
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (::authSessionManager.isInitialized) {
            authSessionManager.markActivity()
        }
        return super.dispatchTouchEvent(event)
    }

    // Key activity is already tracked by Compose interaction handlers.
    // Do not override ComponentActivity.dispatchKeyEvent: AndroidX restricts
    // its dispatch implementation to its own library group.
}

@Composable
private fun LoadingScreen(language: AppLanguage) {
    val text = strings(language)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text.loading)
    }
}
