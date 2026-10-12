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
import androidx.compose.runtime.collectAsState
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
import com.olehkobylianskyi.familypoints.android.storage.OfflineLoginException
import com.olehkobylianskyi.familypoints.android.storage.OfflineLoginError
import com.olehkobylianskyi.familypoints.android.network.ServerReachability
import com.olehkobylianskyi.familypoints.android.network.ServerStatus
import retrofit2.HttpException
import java.io.IOException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    showOnlineReauthNotice: Boolean = false,
    initialUsername: String = "",
    onRememberUsername: (String) -> Unit = {},
    onLogin: suspend (String, String) -> CurrentUserResponse,
    onLoginSuccess: (CurrentUserResponse) -> Unit
) {
    var username by remember(initialUsername) { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var languageMenuOpen by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val serverStatus by ServerReachability.status.collectAsState()
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

        if (showOnlineReauthNotice) {
            Text(
                when (language) {
                    AppLanguage.UK -> "Сервер знову доступний. Для синхронізації потрібно повторно увійти онлайн."
                    AppLanguage.DE -> "Server wieder erreichbar. Zum Synchronisieren bitte erneut online anmelden."
                    AppLanguage.EN -> "Server is back. Sign in online again to synchronize."
                    AppLanguage.RU -> "Сервер снова доступен. Для синхронизации войдите онлайн повторно."
                },
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        if (serverStatus == ServerStatus.OFFLINE) {
            Text(
                when (language) {
                    AppLanguage.UK -> "Офлайн-вхід: введіть пароль акаунта, в який ви раніше входили на цьому телефоні онлайн."
                    AppLanguage.DE -> "Offline-Anmeldung: Passwort eines Kontos eingeben, mit dem Sie sich hier bereits online angemeldet haben."
                    AppLanguage.EN -> "Offline sign-in: enter the password of an account previously used online on this device."
                    AppLanguage.RU -> "Офлайн-вход: введите пароль аккаунта, в который вы ранее входили онлайн на этом устройстве."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

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
                        onRememberUsername(username)
                        password = ""
                        onLoginSuccess(user)
                    } catch (exception: OfflineLoginException) {
                        error = when (exception.reason) {
                            OfflineLoginError.NOT_ENROLLED -> when (language) {
                                AppLanguage.UK -> "Цей акаунт ще не підготовлений для офлайн-входу. Один раз увійдіть із сервером."
                                AppLanguage.DE -> "Dieses Konto ist nicht für Offline-Anmeldung eingerichtet. Melden Sie sich einmal online an."
                                AppLanguage.EN -> "This account is not enrolled for offline sign-in. Sign in online once first."
                                AppLanguage.RU -> "Аккаунт ещё не подготовлен к офлайн-входу. Один раз войдите при доступном сервере."
                            }
                            OfflineLoginError.EXPIRED -> when (language) {
                                AppLanguage.UK -> "Термін офлайн-доступу минув. Потрібен вхід із сервером."
                                AppLanguage.DE -> "Offline-Zugriff abgelaufen. Bitte online anmelden."
                                AppLanguage.EN -> "Offline access expired. Sign in online to renew."
                                AppLanguage.RU -> "Срок офлайн-доступа истёк. Войдите через сервер."
                            }
                            OfflineLoginError.INCORRECT_PASSWORD -> text.invalidCredentials
                            OfflineLoginError.LOCKED -> when (language) {
                                AppLanguage.UK -> "Забагато спроб. Зачекайте 5 хвилин і спробуйте знову."
                                AppLanguage.DE -> "Zu viele Versuche. Bitte 5 Minuten warten."
                                AppLanguage.EN -> "Too many attempts. Wait 5 minutes and try again."
                                AppLanguage.RU -> "Слишком много попыток. Подождите 5 минут."
                            }
                        }
                    } catch (exception: HttpException) {
                        error = if (exception.code() == 401 || exception.code() == 403) {
                            text.invalidCredentials
                        } else {
                            when (language) {
                                AppLanguage.UK -> "Помилка сервера (${exception.code()}). Спробуйте ще раз."
                                AppLanguage.DE -> "Serverfehler (${exception.code()}). Bitte erneut versuchen."
                                AppLanguage.EN -> "Server error (${exception.code()}). Try again."
                                AppLanguage.RU -> "Ошибка сервера (${exception.code()}). Попробуйте снова."
                            }
                        }
                    } catch (_: IOException) {
                        error = when (language) {
                            AppLanguage.UK -> "Немає зв’язку із сервером. Якщо це перший вхід, підключіться до мережі."
                            AppLanguage.DE -> "Keine Serververbindung. Für die erste Anmeldung bitte online verbinden."
                            AppLanguage.EN -> "Cannot reach server. Connect for the first sign-in."
                            AppLanguage.RU -> "Нет связи с сервером. Для первого входа подключитесь к сети."
                        }
                    } catch (_: Exception) {
                        error = when (language) {
                            AppLanguage.UK -> "Не вдалося виконати вхід. Спробуйте ще раз."
                            AppLanguage.DE -> "Anmeldung fehlgeschlagen. Bitte erneut versuchen."
                            AppLanguage.EN -> "Sign-in failed. Please try again."
                            AppLanguage.RU -> "Не удалось войти. Попробуйте ещё раз."
                        }
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
                if (loading) text.loggingIn else if (serverStatus == ServerStatus.OFFLINE) {
                    when (language) {
                        AppLanguage.UK -> "Увійти офлайн"
                        AppLanguage.DE -> "Offline anmelden"
                        AppLanguage.EN -> "Sign in offline"
                        AppLanguage.RU -> "Войти офлайн"
                    }
                } else text.login
            )
        }
    }
}
