package com.olehkobylianskyi.familypoints.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.olehkobylianskyi.familypoints.android.network.AccountWorkspaceChoice
import kotlinx.coroutines.launch

/** Explicit online-only workspace switch. Never display this in offline mode. */
@Composable
fun WorkspaceSwitcher(
    activeWorkspaceId: Long,
    loadWorkspaces: suspend () -> List<AccountWorkspaceChoice>,
    activate: suspend (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var choices by remember { mutableStateOf<List<AccountWorkspaceChoice>>(emptyList()) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier) {
        Button(enabled = !busy, onClick = {
            scope.launch {
                busy = true
                error = false
                try {
                    choices = loadWorkspaces()
                    expanded = choices.size > 1
                } catch (_: Exception) { error = true }
                finally { busy = false }
            }
        }) { Text(if (busy) "..." else "Сім'я ▾") }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text((if (choice.workspaceId == activeWorkspaceId) "✓ " else "") + choice.workspaceName) },
                    enabled = !busy && choice.workspaceId != activeWorkspaceId,
                    onClick = {
                        expanded = false
                        scope.launch {
                            busy = true
                            error = false
                            try { activate(choice.workspaceId) }
                            catch (_: Exception) { error = true }
                            finally { busy = false }
                        }
                    }
                )
            }
        }
        if (error) {
            Snackbar { Text("Не вдалося змінити сім'ю") }
        }
    }
}
