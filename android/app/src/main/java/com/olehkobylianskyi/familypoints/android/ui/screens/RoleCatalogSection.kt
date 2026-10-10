package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.*
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import kotlinx.coroutines.launch

private fun rt(language: AppLanguage, uk: String, de: String, en: String, ru: String): String =
    when (language) {
        AppLanguage.UK -> uk
        AppLanguage.DE -> de
        AppLanguage.EN -> en
        AppLanguage.RU -> ru
    }

@Composable
fun RoleCatalogSection(
    language: AppLanguage,
    workspaceId: Long,
    canManage: Boolean,
    repository: RoleCatalogRepository,
    onUnauthorized: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var sets by remember { mutableStateOf<List<RoleSetItem>>(emptyList()) }
    var roles by remember { mutableStateOf<List<RoleDefinitionItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var setEditing by remember { mutableStateOf<RoleSetItem?>(null) }
    var roleEditing by remember { mutableStateOf<RoleDefinitionItem?>(null) }
    var dialog by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSetId by remember { mutableStateOf<Long?>(null) }
    var setMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf(false) }
    suspend fun reload() {
        val result = repository.load(workspaceId)
        sets = result.first
        roles = result.second
    }
    fun fail(e: Exception) {
        if (e is retrofit2.HttpException && e.code() == 401) onUnauthorized()
        else error = e.message ?: "Request failed"
    }
    fun operate(action: suspend () -> Unit) {
        scope.launch {
            busy = true
            error = ""
            try { action(); reload(); dialog = "" }
            catch (e: Exception) { fail(e) }
            finally { busy = false }
        }
    }
    LaunchedEffect(workspaceId) {
        loading = true
        try { reload() } catch (e: Exception) { fail(e) } finally { loading = false }
    }
    fun openSet(item: RoleSetItem?) {
        setEditing = item; name = item?.name ?: ""; description = item?.description ?: ""
        pendingDelete = false; dialog = "set"
    }
    fun openRole(item: RoleDefinitionItem?, defaultSetId: Long?) {
        roleEditing = item; name = item?.name ?: ""; description = item?.description ?: ""
        selectedSetId = item?.roleSetId ?: defaultSetId
        pendingDelete = false; dialog = "role"
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(rt(language, "Каталог ролей", "Rollenkatalog", "Role catalog", "Каталог ролей"),
                style = MaterialTheme.typography.titleLarge)
            if (canManage) TextButton(onClick = { openSet(null) }) {
                Text(rt(language, "+ Набір", "+ Set", "+ Set", "+ Набор"))
            }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (loading) Text(rt(language, "Завантаження…", "Laden…", "Loading…", "Загрузка…"))
        sets.forEach { set ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(set.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (set.systemDefault) Text(rt(language, "Системний", "System", "System", "Системный"))
                        else if (canManage) TextButton(onClick = { openSet(set) }) {
                            Text(rt(language, "Змінити", "Ändern", "Edit", "Изменить"))
                        }
                    }
                    set.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    roles.filter { it.roleSetId == set.id }.forEach { role ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(role.name, modifier = Modifier.weight(1f))
                            if (canManage && !role.systemDefault) TextButton(onClick = { openRole(role, set.id) }) {
                                Text(rt(language, "Змінити", "Ändern", "Edit", "Изменить"))
                            }
                        }
                    }
                    if (canManage) TextButton(onClick = { openRole(null, set.id) }) {
                        Text(rt(language, "+ Роль", "+ Rolle", "+ Role", "+ Роль"))
                    }
                }
            }
        }
        val without = roles.filter { it.roleSetId == null }
        if (without.isNotEmpty() || canManage) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(rt(language, "Без набору", "Ohne Set", "Without set", "Без набора"),
                        style = MaterialTheme.typography.titleMedium)
                    without.forEach { role ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(role.name, modifier = Modifier.weight(1f))
                            if (canManage && !role.systemDefault) TextButton(onClick = { openRole(role, null) }) {
                                Text(rt(language, "Змінити", "Ändern", "Edit", "Изменить"))
                            }
                        }
                    }
                    if (canManage) TextButton(onClick = { openRole(null, null) }) {
                        Text(rt(language, "+ Роль", "+ Rolle", "+ Role", "+ Роль"))
                    }
                }
            }
        }
    }
    if (dialog.isNotBlank()) {
        val isSet = dialog == "set"
        val editable = if (isSet) setEditing?.systemDefault != true else roleEditing?.systemDefault != true
        AlertDialog(
            onDismissRequest = { if (!busy) dialog = "" },
            title = { Text(if (isSet) rt(language, "Набір ролей", "Rollenset", "Role set", "Набор ролей")
                else rt(language, "Роль", "Rolle", "Role", "Роль")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it },
                        label = { Text(rt(language, "Назва", "Name", "Name", "Название")) },
                        modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(description, { description = it },
                        label = { Text(rt(language, "Опис", "Beschreibung", "Description", "Описание")) },
                        modifier = Modifier.fillMaxWidth())
                    if (!isSet) {
                        OutlinedButton(onClick = { setMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text((sets.firstOrNull { it.id == selectedSetId }?.name
                                ?: rt(language, "Без набору", "Ohne Set", "Without set", "Без набора")) + " ▾")
                        }
                        DropdownMenu(expanded = setMenu, onDismissRequest = { setMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(rt(language, "Без набору", "Ohne Set", "Without set", "Без набора")) },
                                onClick = { selectedSetId = null; setMenu = false })
                            sets.forEach { set ->
                                DropdownMenuItem(text = { Text(set.name) }, onClick = {
                                    selectedSetId = set.id; setMenu = false
                                })
                            }
                        }
                    }
                    if (pendingDelete) Text(rt(language, "Підтвердьте видалення", "Löschen bestätigen",
                        "Confirm deletion", "Подтвердите удаление"),
                        color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Column {
                    if (pendingDelete) {
                        Button(enabled = !busy, onClick = {
                            if (isSet) setEditing?.id?.let { id -> operate { repository.deleteSet(workspaceId, id) } }
                            else roleEditing?.id?.let { id -> operate { repository.deleteRole(workspaceId, id) } }
                        }) { Text(rt(language, "Видалити", "Löschen", "Delete", "Удалить")) }
                    } else {
                        Button(enabled = !busy && editable && name.isNotBlank(), onClick = {
                            val bodyName = name.trim()
                            val bodyDescription = description.trim().ifBlank { null }
                            if (isSet) operate {
                                repository.saveSet(workspaceId, setEditing?.id, RoleSetSave(bodyName, bodyDescription))
                            } else operate {
                                repository.saveRole(workspaceId, roleEditing?.id,
                                    RoleDefinitionSave(bodyName, bodyDescription, selectedSetId))
                            }
                        }) { Text(rt(language, "Зберегти", "Speichern", "Save", "Сохранить")) }
                        val itemId = if (isSet) setEditing?.id else roleEditing?.id
                        if (itemId != null && editable) TextButton(onClick = { pendingDelete = true }) {
                            Text(rt(language, "Видалити", "Löschen", "Delete", "Удалить"))
                        }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { dialog = "" }) {
                Text(rt(language, "Скасувати", "Abbrechen", "Cancel", "Отмена"))
            } }
        )
    }
}
