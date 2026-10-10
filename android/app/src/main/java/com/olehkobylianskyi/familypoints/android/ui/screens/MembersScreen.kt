package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.MemberSaveRequest
import com.olehkobylianskyi.familypoints.android.data.MembersGroupResponse
import com.olehkobylianskyi.familypoints.android.data.MembersPageData
import com.olehkobylianskyi.familypoints.android.data.MembersRepository
import com.olehkobylianskyi.familypoints.android.data.WorkspaceMemberResponse
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.ui.components.AppHeader
import kotlinx.coroutines.launch
import retrofit2.HttpException

private fun memberText(language: AppLanguage, uk: String, de: String, en: String, ru: String): String =
    when (language) {
        AppLanguage.UK -> uk
        AppLanguage.DE -> de
        AppLanguage.EN -> en
        AppLanguage.RU -> ru
    }

@Composable
fun MembersScreen(
    language: AppLanguage,
    currentUser: CurrentUserResponse,
    repository: MembersRepository,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (DashboardDestination) -> Unit,
    onUnauthorized: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val allowed = "MANAGE_MEMBERS" in currentUser.permissions
    var data by remember { mutableStateOf<MembersPageData?>(null) }
    var busy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("members") }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WorkspaceMemberResponse?>(null) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("CHILD") }
    var roleId by remember { mutableStateOf<Long?>(null) }
    var menu by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }

    suspend fun reload() {
        data = repository.load(currentUser.workspaceId)
    }
    fun failure(exception: Exception) {
        if (exception is HttpException && exception.code() == 401) onUnauthorized()
        else error = exception.message ?: "Request failed"
    }
    fun edit(member: WorkspaceMemberResponse?) {
        editing = member
        name = member?.name ?: ""
        type = member?.memberType ?: "CHILD"
        roleId = member?.workspaceRoleId ?: data?.roles?.firstOrNull()?.id
        editorOpen = true
    }
    fun mutate(operation: suspend () -> Unit) {
        scope.launch {
            busy = true
            error = ""
            try {
                operation()
                reload()
            } catch (e: Exception) {
                failure(e)
            } finally {
                busy = false
            }
        }
    }
    LaunchedEffect(currentUser.workspaceId) {
        loading = true
        try { reload() } catch (e: Exception) { failure(e) }
        finally { loading = false }
    }

    val membersTitle = memberText(language, "Учасники", "Mitglieder", "Members", "Участники")
    val groupsTitle = memberText(language, "Групи", "Gruppen", "Groups", "Группы")
    val saveTitle = memberText(language, "Зберегти", "Speichern", "Save", "Сохранить")
    val cancelTitle = memberText(language, "Скасувати", "Abbrechen", "Cancel", "Отмена")

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        AppHeader(language, currentUser, onLanguageChange, onLogout, onNavigate)
        Text(membersTitle, style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { tab = "members" }) { Text(membersTitle) }
            OutlinedButton(onClick = { tab = "groups" }) { Text(groupsTitle) }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (loading) Text(memberText(language, "Завантаження…", "Laden…", "Loading…", "Загрузка…"))
        else {
            if (tab == "members" && allowed) {
                Button(onClick = { edit(null) }) {
                    Text(memberText(language, "Додати учасника", "Mitglied hinzufügen", "Add member", "Добавить участника"))
                }
            }
            if (tab == "groups" && allowed) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text(memberText(language, "Назва групи", "Gruppenname", "Group name", "Название группы")) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        enabled = groupName.isNotBlank() && !busy,
                        onClick = {
                            val trimmed = groupName.trim()
                            mutate {
                                repository.createGroup(currentUser.workspaceId, trimmed)
                                groupName = ""
                            }
                        }
                    ) { Text("+") }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (tab == "members") {
                    items(data?.members.orEmpty(), key = { "m" + it.id }) { member ->
                        Card(modifier = Modifier.fillMaxWidth().then(
                            if (allowed) Modifier.clickable { edit(member) } else Modifier
                        )) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(member.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    when (member.memberType) {
                                        "PARENT" -> memberText(language, "Батьки", "Elternteil", "Parent", "Родитель")
                                        "CHILD" -> memberText(language, "Дитина", "Kind", "Child", "Ребёнок")
                                        else -> memberText(language, "Інше", "Sonstige", "Other", "Другое")
                                    }
                                )
                                Text(member.workspaceRoleName)
                            }
                        }
                    }
                } else {
                    val groups = data?.groups.orEmpty()
                    val childIds = groups.flatMap { it.childGroups.orEmpty() }.map { it.groupId }.toSet()
                    val roots = groups.filter { it.id !in childIds }
                    items(roots, key = { "g" + it.id }) { group ->
                        GroupTreeCard(group, groups, emptySet(), language)
                    }
                }
            }
        }
    }

    if (editorOpen) {
        AlertDialog(
            onDismissRequest = { if (!busy) editorOpen = false },
            title = { Text(if (editing == null)
                memberText(language, "Новий учасник", "Neues Mitglied", "New member", "Новый участник")
                else memberText(language, "Редагувати учасника", "Mitglied bearbeiten", "Edit member", "Редактировать участника")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = {
                        Text(memberText(language, "Ім’я", "Name", "Name", "Имя"))
                    }, singleLine = true)
                    OutlinedButton(onClick = { menu = "type" }, modifier = Modifier.fillMaxWidth()) {
                        Text(type + " ▾")
                    }
                    DropdownMenu(expanded = menu == "type", onDismissRequest = { menu = "" }) {
                        listOf("PARENT", "CHILD", "OTHER").forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { type = option; menu = "" })
                        }
                    }
                    OutlinedButton(onClick = { menu = "role" }, modifier = Modifier.fillMaxWidth()) {
                        Text((data?.roles?.firstOrNull { it.id == roleId }?.name ?: "—") + " ▾")
                    }
                    DropdownMenu(expanded = menu == "role", onDismissRequest = { menu = "" }) {
                        data?.roles.orEmpty().forEach { role ->
                            DropdownMenuItem(text = { Text(role.name) }, onClick = { roleId = role.id; menu = "" })
                        }
                    }
                    if (editing != null) TextButton(enabled = !busy, onClick = { confirmDelete = true }) {
                        Text(memberText(language, "Видалити", "Löschen", "Delete", "Удалить"),
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(enabled = !busy && name.isNotBlank() && roleId != null, onClick = {
                    val request = MemberSaveRequest(name.trim(), type, roleId!!)
                    val id = editing?.id
                    mutate {
                        repository.save(currentUser.workspaceId, id, request)
                        editorOpen = false
                    }
                }) { Text(saveTitle) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { editorOpen = false }) { Text(cancelTitle) } }
        )
    }
    if (confirmDelete && editing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(memberText(language, "Видалити учасника?", "Mitglied löschen?", "Delete member?", "Удалить участника?")) },
            text = { Text(memberText(language,
                "Історія завдань, балів і покупок залишиться, але учасник відображатиметься як «Видалено».",
                "Aufgaben-, Punkte- und Kaufhistorie bleibt erhalten; der Name wird als gelöscht angezeigt.",
                "Task, points and purchase history stays, but the member will appear as deleted.",
                "История заданий, баллов и покупок останется; участник будет отображаться как удалённый.")) },
            confirmButton = {
                Button(enabled = !busy, onClick = {
                    val id = editing?.id ?: return@Button
                    confirmDelete = false
                    mutate {
                        repository.delete(currentUser.workspaceId, id)
                        editorOpen = false
                    }
                }) { Text(memberText(language, "Видалити", "Löschen", "Delete", "Удалить")) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(cancelTitle) } }
        )
    }
}

@Composable
private fun GroupTreeCard(
    group: MembersGroupResponse,
    groups: List<MembersGroupResponse>,
    visited: Set<Long>,
    language: AppLanguage
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(group.name, style = MaterialTheme.typography.titleMedium)
            if (group.id in visited) {
                Text(memberText(language, "Циклічне посилання", "Zyklischer Verweis", "Circular reference", "Циклическая ссылка"))
                return@Column
            }
            group.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
            Text("${group.members.orEmpty().size} · ${group.childGroups.orEmpty().size}")
            group.members.orEmpty().forEach { member -> Text("• " + member.memberName) }
            group.balances.orEmpty().forEach { balance ->
                Text("${balance.amount} ${balance.name ?: balance.code ?: ""}")
            }
            group.childGroups.orEmpty().forEach { child ->
                val nested = groups.firstOrNull { it.id == child.groupId }
                if (nested == null) Text("↳ " + child.groupName)
                else GroupTreeCard(nested, groups, visited + group.id, language)
            }
        }
    }
}
