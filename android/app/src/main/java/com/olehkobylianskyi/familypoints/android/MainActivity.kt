package com.olehkobylianskyi.familypoints.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import com.olehkobylianskyi.familypoints.android.ui.screens.HomeScreen
import com.olehkobylianskyi.familypoints.android.ui.screens.LoginScreen
import com.olehkobylianskyi.familypoints.android.ui.theme.FamilyPointsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenStore = TokenStore(this)

        setContent {
            FamilyPointsTheme {
                var accessToken by remember {
                    mutableStateOf(tokenStore.getAccessToken())
                }

                if (accessToken.isNullOrBlank()) {
                    LoginScreen(
                        onLoginSuccess = { token ->
                            tokenStore.saveAccessToken(token)
                            accessToken = token
                        }
                    )
                } else {
                    HomeScreen(
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
