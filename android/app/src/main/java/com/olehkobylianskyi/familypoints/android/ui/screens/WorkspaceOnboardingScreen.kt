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

@Composable
fun WorkspaceOnboardingScreen(
    loadChoices: suspend () -> List<AccountWorkspaceChoice>,
    choose: suspend (Long) -> CurrentUserResponse,
    create: suspend (String, String) -> CurrentUserResponse,
    onReady: (CurrentUserResponse) -> Unit,
    onLogout: () -> Unit
) {
    var choices by remember { mutableStateOf<List<AccountWorkspaceChoice>?>(null) }
    var familyName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try { choices = loadChoices() }
        catch (e: Exception) { error = e.message ?: "Не вдалося завантажити сім’ї" }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Вибір сім’ї", style = MaterialTheme.typography.headlineMedium)
        choices?.forEach { choice ->
            Button(onClick = {
                scope.launch {
                    busy = true
                    error = ""
                    try { onReady(choose(choice.workspaceId)) }
                    catch (e: Exception) { error = e.message ?: "Помилка входу" }
                    finally { busy = false }
                }
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(choice.workspaceName)
            }
        }
        if (choices?.isEmpty() == true) {
            Text("Поки немає сімей. Створіть першу.")
            OutlinedTextField(familyName, { familyName = it }, label = { Text("Назва сім’ї") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(ownerName, { ownerName = it }, label = { Text("Ваше ім’я") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = {
                scope.launch {
                    busy = true
                    error = ""
                    try { onReady(create(familyName, ownerName)) }
                    catch (e: Exception) { error = e.message ?: "Не вдалося створити сім’ю" }
                    finally { busy = false }
                }
            }, enabled = !busy && familyName.isNotBlank() && ownerName.isNotBlank()) {
                Text("Створити сім’ю")
            }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onLogout, enabled = !busy) { Text("Вийти") }
    }
}
