package com.olehkobylianskyi.familypoints.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.olehkobylianskyi.familypoints.android.storage.LanguageStore
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import com.olehkobylianskyi.familypoints.android.ui.screens.HomeScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.LoginScreen
import com.olehkobylianskyi.familypoints.android.ui.theme.FamilyPointsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenStore = TokenStore(this)
        val languageStore = LanguageStore(this)

        setContent {
            FamilyPointsTheme {
                var accessToken by remember {
                    mutableStateOf(tokenStore.getAccessToken())
                }

                var language by remember {
                    mutableStateOf(languageStore.getLanguage())
                }

                if (accessToken.isNullOrBlank()) {
                    LoginScreen(
                        language = language,
                        onLanguageChange = { selected ->
                            languageStore.saveLanguage(selected)
                            language = selected
                        },
                        onLoginSuccess = { token ->
                            tokenStore.saveAccessToken(token)
                            accessToken = token
                        }
                    )
                } else {
                    HomeScreen(
                        language = language,
                        onLogout = {
                            tokenStore.clear()
                            accessToken = null
                        }
                    )
                }
            }
        }
    }
}
