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
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.strings
import com.olehkobylianskyi.familypoints.android.storage.LanguageStore
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import com.olehkobylianskyi.familypoints.android.ui.screens.HomeScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.LoginScreen
import com.olehkobylianskyi.familypoints.android.ui.theme.FamilyPointsTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var authSessionManager: AuthSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenStore = TokenStore(this)
        val languageStore = LanguageStore(this)
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

                LaunchedEffect(Unit) {
                    currentUser = authSessionManager.restoreSession()
                    authLoading = false
                }

                LaunchedEffect(currentUser) {
                    while (currentUser != null) {
                        delay(60_000L)

                        if (!authSessionManager.refreshIfNeeded()) {
                            currentUser = null
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
                            }
                        )
                    }

                    else -> {
                        HomeScreen(
                            language = language,
                            currentUser = currentUser!!,
                            onLogout = {
                                authSessionManager.clearSession()
                                currentUser = null
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
