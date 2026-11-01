package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.WorkNodeGroupsRepository
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.network.WorkNodeGroupDto
import com.olehkobylianskyi.familypoints.android.network.WorkNodeGroupMutation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private fun gl(lang: AppLanguage, uk: String, de: String, en: String, ru: String) =
    when(lang) {
        AppLanguage.UK -> uk
        AppLanguage.DE -> de
        AppLanguage.EN -> en
        AppLanguage.RU -> ru
    }

/** Dedicated task-folder management. No implicit creation from template category or pack. */
@Composable
fun WorkNodeGroupsSection(
    language: AppLanguage,
    workspaceId: Long,
    canCreateTasks: Boolean,
    repository: WorkNodeGroupsRepository
) {
    val scope = rememberCoroutineScope()
    var groups by remember(workspaceId) { mutableStateOf<List<WorkNodeGroupDto>>(emptyList()) }
    var loading by remember(workspaceId) { mutableStateOf(true) }
    var busy by remember(workspaceId) { mutableStateOf(false) }
    var error by remember(workspaceId) { mutableStateOf("") }
    var editing by remember(workspaceId) { mutableStateOf<WorkNodeGroupDto?>(null) }
    var showEditor by remember(workspaceId) { mutableStateOf(false) }
    var title by remember(workspaceId) { mutableStateOf("") }
    var description by remember(workspaceId) { mutableStateOf("") }
    var parent by remember(workspaceId) { mutableStateOf<Long?>(null) }
    var deleteCandidate by remember(workspaceId) { mutableStateOf<WorkNodeGroupDto?>(null) }
    var refresh by remember(workspaceId) { mutableIntStateOf(0) }

    LaunchedEffect(workspaceId, refresh) {
        loading = true
        try { groups = repository.list(workspaceId); error = "" }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (ex: Exception) { error = ex.message ?: "Cannot load task folders" }
        finally { loading = false }
    }

    fun execute(action: suspend () -> Unit) {
        if (busy || loading) return
        busy = true
        scope.launch {
            try { action(); groups = repository.list(workspaceId); error = "" }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (ex: Exception) { error = ex.message ?: "Task folder request failed" }
            finally { busy = false }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(gl(language, "Групи завдань", "Aufgabengruppen", "Task folders", "Группы заданий"),
                style = MaterialTheme.typography.titleMedium)
            OutlinedButton(enabled = canCreateTasks && !busy && !loading, onClick = {
                editing = null; title = ""; description = ""; parent = null; showEditor = true
            }) { Text(gl(language, "Створити", "Erstellen", "Create", "Создать")) }
        }
        Text(gl(language,
            "Це папки завдань, не групи учасників. Імпорт шаблонів не створює ці папки автоматично.",
            "Dies sind Aufgabenordner, keine Mitgliedergruppen. Vorlagen erzeugen sie nicht automatisch.",
            "Task folders are separate from member groups. Templates never create them automatically.",
            "Папки заданий отделены от групп участников. Шаблоны их не создают автоматически."),
            style = MaterialTheme.typography.bodySmall)
        if (loading || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error.isNotBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error)
            TextButton(enabled = !busy, onClick = { refresh++ }) {
                Text(gl(language, "Повторити", "Erneut", "Retry", "Повторить"))
            }
        }
        LazyColumn(Modifier.heightIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(groups, key = { it.id }) { group ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(group.title)
                        if (group.parentGroupId != null) Text("#${group.parentGroupId}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (canCreateTasks) {
                        TextButton(enabled = !busy, onClick = {
                            editing = group; title = group.title
                            description = group.description.orEmpty()
                            parent = group.parentGroupId; showEditor = true
                        }) { Text(gl(language, "Змінити", "Ändern", "Edit", "Изменить")) }
                        TextButton(enabled = !busy, onClick = { deleteCandidate = group }) {
                            Text(gl(language, "Видалити", "Löschen", "Delete", "Удалить"))
                        }
                    }
                }
            }
        }
    }

    if (showEditor) AlertDialog(
        onDismissRequest = { if (!busy) showEditor = false },
        title = { Text(gl(language, "Папка завдань", "Aufgabenordner", "Task folder", "Папка заданий")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, singleLine = true,
                    label = { Text(gl(language, "Назва", "Titel", "Title", "Название")) })
                OutlinedTextField(description, { description = it },
                    label = { Text(gl(language, "Опис", "Beschreibung", "Description", "Описание")) })
                Text(gl(language, "Батьківська папка", "Übergeordneter Ordner", "Parent folder", "Родительская папка"))
                val eligible = groups.filter { it.id != editing?.id }
                // Backend is authoritative for ancestry cycles and tenant access.
                androidx.compose.foundation.lazy.LazyRow {
                    item {
                        FilterChip(selected = parent == null, onClick = { parent = null },
                            label = { Text(gl(language, "Без", "Keine", "None", "Нет")) })
                    }
                    items(eligible, key = { it.id }) { group ->
                        FilterChip(selected = parent == group.id, onClick = { parent = group.id },
                            label = { Text(group.title) })
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = canCreateTasks && !busy && title.isNotBlank() &&
                title == title.trim() && title.length <= 150 && description.length <= 2000,
                onClick = {
                    val request = WorkNodeGroupMutation(
                        title, description.ifBlank { null }, parent, editing?.sortOrder ?: groups.size)
                    val current = editing
                    showEditor = false
                    execute {
                        if (current == null) repository.create(workspaceId, request)
                        else repository.edit(workspaceId, current.id, request)
                    }
                }) { Text(gl(language, "Зберегти", "Speichern", "Save", "Сохранить")) }
        },
        dismissButton = {
            TextButton(onClick = { showEditor = false }) {
                Text(gl(language, "Скасувати", "Abbrechen", "Cancel", "Отмена"))
            }
        }
    )

    deleteCandidate?.let { target ->
        AlertDialog(onDismissRequest = { if (!busy) deleteCandidate = null },
            title = { Text(gl(language, "Видалити порожню папку?",
                "Leeren Ordner löschen?", "Delete empty folder?", "Удалить пустую папку?")) },
            text = { Text(target.title) },
            confirmButton = { Button(enabled = canCreateTasks && !busy, onClick = {
                deleteCandidate = null
                execute { repository.deactivate(workspaceId, target.id) }
            }) { Text(gl(language, "Підтвердити", "Bestätigen", "Confirm", "Подтвердить")) } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) {
                Text(gl(language, "Скасувати", "Abbrechen", "Cancel", "Отмена"))
            } }
        )
    }
}
