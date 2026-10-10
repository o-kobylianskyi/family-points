package com.olehkobylianskyi.familypoints.android

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.dashboardStrings
import com.olehkobylianskyi.familypoints.android.i18n.strings
import com.olehkobylianskyi.familypoints.android.storage.LanguageStore
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import com.olehkobylianskyi.familypoints.android.ui.screens.DashboardDestination
import com.olehkobylianskyi.familypoints.android.ui.screens.DashboardScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.LoginScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.PlaceholderScreen
import com.olehkobylianskyi.familypoints.android.ui.theme.FamilyPointsTheme
import kotlinx.coroutines.delay

private enum class AppDestination {
    DASHBOARD,
    TASKS,
    POINTS,
    REWARDS
}

class MainActivity : ComponentActivity() {

    private lateinit var authSessionManager: AuthSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenStore = TokenStore(this)
        val languageStore = LanguageStore(this)
        val dashboardRepository = DashboardRepository(tokenStore)
        authSessionManager = AuthSessionManager(tokenStore)

        setContent {
            FamilyPointsTheme {
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
                            onLanguageChange = { selected ->
                                languageStore.saveLanguage(selected)
                                language = selected
                            },
                            onLogout = {
                                authSessionManager.clearSession()
                                currentUser = null
                                destination = AppDestination.DASHBOARD
                            },
                            onNavigate = { target ->
                                destination = when (target) {
                                    DashboardDestination.TASKS ->
                                        AppDestination.TASKS
                                    DashboardDestination.POINTS ->
                                        AppDestination.POINTS
                                    DashboardDestination.REWARDS ->
                                        AppDestination.REWARDS
                                }
                            },
                            onUnauthorized = {
                                authSessionManager.clearSession()
                                currentUser = null
                                destination = AppDestination.DASHBOARD
                            }
                        )
                    }

                    else -> {
                        val dashboardText = dashboardStrings(language)
                        val title = when (destination) {
                            AppDestination.TASKS -> dashboardText.tasks
                            AppDestination.POINTS -> dashboardText.points
                            AppDestination.REWARDS -> dashboardText.rewards
                            AppDestination.DASHBOARD -> ""
                        }

                        PlaceholderScreen(
                            language = language,
                            title = title,
                            onBack = {
                                destination = AppDestination.DASHBOARD
                            }
                        )
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
