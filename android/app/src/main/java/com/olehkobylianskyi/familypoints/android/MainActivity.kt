package com.olehkobylianskyi.familypoints.android

import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.olehkobylianskyi.familypoints.android.auth.AuthSessionManager
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.DashboardRepository
import com.olehkobylianskyi.familypoints.android.data.TaskDetailsRepository
import com.olehkobylianskyi.familypoints.android.data.TasksRepository
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.dashboardStrings
import com.olehkobylianskyi.familypoints.android.i18n.strings
import com.olehkobylianskyi.familypoints.android.storage.LanguageStore
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import com.olehkobylianskyi.familypoints.android.ui.screens.DashboardDestination
import com.olehkobylianskyi.familypoints.android.ui.screens.DashboardScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.LoginScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.PlaceholderScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.TaskDetailsScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.TasksScreen
import com.olehkobylianskyi.familypoints.android.ui.theme.FamilyPointsTheme
import kotlinx.coroutines.delay

private enum class AppDestination {
    DASHBOARD,
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE

        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        val tokenStore = TokenStore(this)
        val languageStore = LanguageStore(this)
        val dashboardRepository = DashboardRepository(tokenStore)
        val tasksRepository = TasksRepository(tokenStore)
        val taskDetailsRepository = TaskDetailsRepository(tokenStore)
        authSessionManager = AuthSessionManager(tokenStore)

        setContent {
            FamilyPointsTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                var currentUser by remember {
                    mutableStateOf<CurrentUserResponse?>(null)
                }

                var authLoading by remember {
                    mutableStateOf(true)
                }

                var language by remember {
                    mutableStateOf(languageStore.getLanguage())
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

                var selectedTaskInstanceId by remember {
                    mutableStateOf<Long?>(null)
                }

                var parentTaskDefinitionId by remember {
                    mutableStateOf<Long?>(null)
                }

                LaunchedEffect(Unit) {
                    currentUser = authSessionManager.restoreSession()
                    authLoading = false
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
                    authSessionManager.clearSession()
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

                when {
                    authLoading -> {
                        LoadingScreen(language)
                    }

                    currentUser == null -> {
                        LoginScreen(
                            language = language,
                            onLanguageChange = { selected ->
                                languageStore.saveLanguage(selected)
                                language = selected
                            },
                            onLogin = { username, password ->
                                authSessionManager.login(
                                    username,
                                    password
                                )
                            },
                            onLoginSuccess = { user ->
                                currentUser = user
                                destination = AppDestination.DASHBOARD
                            }
                        )
                    }

                    destination == AppDestination.DASHBOARD -> {
                        DashboardScreen(
                            language = language,
                            currentUser = currentUser!!,
                            loadDashboard = {
                                dashboardRepository.load(currentUser!!)
                            },
                            onLanguageChange = changeLanguage,
                            onLogout = logout,
                            onNavigate = navigate,
                            onUnauthorized = unauthorized
                        )
                    }

                    destination == AppDestination.TASKS -> {
                        TasksScreen(
                            language = language,
                            currentUser = currentUser!!,
                            repository = tasksRepository,
                            onLanguageChange = changeLanguage,
                            onLogout = logout,
                            onNavigate = navigate,
                            onBack = {
                                destination = AppDestination.DASHBOARD
                            },
                            onOpenTask = { definitionId, date ->
                                selectedTaskDefinitionId = definitionId
                                selectedTaskDate = date
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
                                repository = taskDetailsRepository,
                                onLanguageChange = changeLanguage,
                                onLogout = logout,
                                onNavigate = navigate,
                                onBack = {
                                    destination = AppDestination.TASKS
                                },
                                onOpenTask = { childId, date ->
                                    selectedTaskDefinitionId = childId
                                    selectedTaskDate = date
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
                        PlaceholderScreen(
                            language = language,
                            currentUser = currentUser!!,
                            title = "+ " + dashboardStrings(language).tasks +
                                (
                                    parentTaskDefinitionId?.let {
                                        " · parent №" + it
                                    } ?: ""
                                ),
                            onLanguageChange = changeLanguage,
                            onLogout = logout,
                            onNavigate = navigate,
                            onBack = {
                                destination =
                                    if (parentTaskDefinitionId != null) {
                                        AppDestination.TASK_DETAILS
                                    } else {
                                        AppDestination.TASKS
                                    }
                                parentTaskDefinitionId = null
                            }
                        )
                    }

                    destination == AppDestination.REWARD_NEGOTIATION -> {
                        PlaceholderScreen(
                            language = language,
                            currentUser = currentUser!!,
                            title = "Reward request · instance №" +
                                (selectedTaskInstanceId ?: ""),
                            onLanguageChange = changeLanguage,
                            onLogout = logout,
                            onNavigate = navigate,
                            onBack = {
                                destination = AppDestination.TASK_DETAILS
                            }
                        )
                    }

                    else -> {
                        val dashboardText = dashboardStrings(language)
                        val title = when (destination) {
                            AppDestination.MEMBERS -> dashboardText.members
                            AppDestination.POINTS -> dashboardText.points
                            AppDestination.REWARDS -> dashboardText.rewards
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
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (::authSessionManager.isInitialized) {
            authSessionManager.markActivity()
        }
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::authSessionManager.isInitialized) {
            authSessionManager.markActivity()
        }
        return super.dispatchKeyEvent(event)
    }
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
