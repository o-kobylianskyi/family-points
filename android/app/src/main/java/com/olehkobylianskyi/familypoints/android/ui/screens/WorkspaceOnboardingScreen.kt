package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.network.AccountWorkspaceChoice
import kotlinx.coroutines.launch
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage

private data class OnboardingLabels(
    val chooseFamily: String,
    val noFamilies: String,
    val familyName: String,
    val ownerName: String,
    val createFamily: String,
    val logout: String,
    val loadFailed: String,
    val chooseFailed: String,
    val createFailed: String,
    val retry: String,
    val loading: String
)

private fun onboardingLabels(language: AppLanguage): OnboardingLabels = when (language) {
    AppLanguage.UK -> OnboardingLabels(
        "Вибір сім’ї", "Поки немає сімей. Створіть першу.", "Назва сім’ї", "Ваше ім’я",
        "Створити сім’ю", "Вийти", "Не вдалося завантажити сім’ї",
        "Не вдалося вибрати сім’ю", "Не вдалося створити сім’ю",
        "Повторити", "Завантаження…"
    )
    AppLanguage.RU -> OnboardingLabels(
        "Выбор семьи", "Пока нет семей. Создайте первую.", "Название семьи", "Ваше имя",
        "Создать семью", "Выйти", "Не удалось загрузить семьи",
        "Не удалось выбрать семью", "Не удалось создать семью",
        "Повторить", "Загрузка…"
    )
    AppLanguage.DE -> OnboardingLabels(
        "Familie auswählen", "Noch keine Familie vorhanden. Erstellen Sie die erste.",
        "Familienname", "Ihr Name", "Familie erstellen", "Abmelden",
        "Familien konnten nicht geladen werden", "Familie konnte nicht ausgewählt werden",
        "Familie konnte nicht erstellt werden", "Erneut versuchen", "Laden…"
    )
    AppLanguage.EN -> OnboardingLabels(
        "Choose family", "No families yet. Create your first one.", "Family name",
        "Your name", "Create family", "Log out", "Could not load families",
        "Could not select family", "Could not create family", "Retry", "Loading…"
    )
}

@Composable
fun WorkspaceOnboardingScreen(
    language: AppLanguage,
    loadChoices: suspend () -> List<AccountWorkspaceChoice>,
    choose: suspend (Long) -> CurrentUserResponse,
    create: suspend (String, String) -> CurrentUserResponse,
    onReady: (CurrentUserResponse) -> Unit,
    onCreated: (CurrentUserResponse) -> Unit,
    onLogout: () -> Unit
) {
    var choices by remember { mutableStateOf<List<AccountWorkspaceChoice>?>(null) }
    var familyName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var choicesLoading by remember { mutableStateOf(true) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val labels = onboardingLabels(language)

    // Read-only GET retry. Never repeats create/choose or any setup write.
    LaunchedEffect(loadAttempt) {
        choicesLoading = true
        error = ""
        try { choices = loadChoices() }
        catch (e: Exception) { choices = null; error = labels.loadFailed }
        finally { choicesLoading = false }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(labels.chooseFamily, style = MaterialTheme.typography.headlineMedium)
        if (choicesLoading) Text(labels.loading)
        choices?.forEach { choice ->
            Button(onClick = {
                scope.launch {
                    busy = true
                    error = ""
                    try { onReady(choose(choice.workspaceId)) }
                    catch (e: Exception) { error = labels.chooseFailed }
                    finally { busy = false }
                }
            }, enabled = !busy && !choicesLoading, modifier = Modifier.fillMaxWidth()) {
                Text(choice.workspaceName)
            }
        }
        if (choices?.isEmpty() == true) {
            Text(labels.noFamilies)
            OutlinedTextField(familyName, { familyName = it }, label = { Text(labels.familyName) },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(ownerName, { ownerName = it }, label = { Text(labels.ownerName) },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = {
                scope.launch {
                    busy = true
                    error = ""
                    try { onCreated(create(familyName, ownerName)) }
                    catch (e: Exception) { error = labels.createFailed }
                    finally { busy = false }
                }
            }, enabled = !busy && familyName.isNotBlank() && ownerName.isNotBlank()) {
                Text(labels.createFamily)
            }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (!choicesLoading && choices == null) {
            OutlinedButton(onClick = { loadAttempt += 1 }, enabled = !busy) { Text(labels.retry) }
        }
        OutlinedButton(onClick = onLogout, enabled = !busy) { Text(labels.logout) }
    }
}
