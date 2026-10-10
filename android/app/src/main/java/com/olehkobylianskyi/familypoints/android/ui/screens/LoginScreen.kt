package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.strings
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogin: suspend (String, String) -> CurrentUserResponse,
    onLoginSuccess: (CurrentUserResponse) -> Unit
) {
    var username by remember { mutableStateOf("oleh") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var languageMenuOpen by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val text = strings(language)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            OutlinedButton(
                onClick = { languageMenuOpen = true },
                enabled = !loading
            ) {
                Text(language.label)
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

        Text(
            text = "FP",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 20.dp)
        )

        Text(
            text = "FamilyPoints",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = text.loginSubtitle,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text.username) },
            singleLine = true,
            enabled = !loading
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text(text.password) },
            singleLine = true,
            enabled = !loading,
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            trailingIcon = {
                IconButton(
                    onClick = { showPassword = !showPassword },
                    enabled = !loading
                ) {
                    Text(
                        text = if (showPassword) "◉" else "◎",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        )

        if (error.isNotBlank()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Button(
            onClick = {
                scope.launch {
                    error = ""
                    loading = true

                    try {
                        val user = onLogin(username, password)
                        password = ""
                        onLoginSuccess(user)
                    } catch (_: Exception) {
                        error = text.invalidCredentials
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            enabled = !loading
        ) {
            Text(
                if (loading) text.loggingIn else text.login
            )
        }
    }
}
