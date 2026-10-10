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
import androidx.compose.material3.Checkbox
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
import com.olehkobylianskyi.familypoints.android.data.GroupUpdateRequest
import com.olehkobylianskyi.familypoints.android.data.GroupRoleResponse
import com.olehkobylianskyi.familypoints.android.data.GroupRoleSaveRequest
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
    var editingGroupId by remember { mutableStateOf<Long?>(null) }

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
                        GroupTreeCard(group, groups, emptySet(), language, if (allowed) { id -> editingGroupId = id } else null)
                    }
                }
            }
        }
    }

    val groupToEdit = data?.groups?.firstOrNull { it.id == editingGroupId }
    if (groupToEdit != null && allowed) {
        GroupEditorDialog(
            group = groupToEdit,
            groups = data?.groups.orEmpty(),
            members = data?.members.orEmpty(),
            language = language,
            busy = busy,
            onClose = { editingGroupId = null },
            onSave = { request ->
                mutate {
                    repository.updateGroup(currentUser.workspaceId, groupToEdit.id, request)
                    editingGroupId = null
                }
            },
            onAddMember = { id -> mutate {
                repository.addGroupMember(currentUser.workspaceId, groupToEdit.id, id)
            } },
            onRemoveMember = { id -> mutate {
                repository.removeGroupMember(currentUser.workspaceId, groupToEdit.id, id)
            } },
            onAddChild = { id -> mutate {
                repository.addChildGroup(currentUser.workspaceId, groupToEdit.id, id)
            } },
            onRemoveChild = { id -> mutate {
                repository.removeChildGroup(currentUser.workspaceId, groupToEdit.id, id)
            } },
            onMemberRoles = { id, roles -> mutate {
                repository.setGroupMemberRoles(currentUser.workspaceId, groupToEdit.id, id, roles)
            } },
            onSaveRole = { roleId, request -> mutate {
                repository.saveGroupRole(currentUser.workspaceId, groupToEdit.id, roleId, request)
            } },
            onDeleteRole = { roleId -> mutate {
                repository.deleteGroupRole(currentUser.workspaceId, groupToEdit.id, roleId)
            } }
        )
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
    language: AppLanguage,
    onEdit: ((Long) -> Unit)?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(group.name, style = MaterialTheme.typography.titleMedium)
                if (onEdit != null) TextButton(onClick = { onEdit(group.id) }) {
                    Text(memberText(language, "Редагувати", "Bearbeiten", "Edit", "Редактировать"))
                }
            }
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
                else GroupTreeCard(nested, groups, visited + group.id, language, onEdit)
            }
        }
    }
}

@Composable
private fun GroupEditorDialog(
    group: MembersGroupResponse,
    groups: List<MembersGroupResponse>,
    members: List<WorkspaceMemberResponse>,
    language: AppLanguage,
    busy: Boolean,
    onClose: () -> Unit,
    onSave: (GroupUpdateRequest) -> Unit,
    onAddMember: (Long) -> Unit,
    onRemoveMember: (Long) -> Unit,
    onAddChild: (Long) -> Unit,
    onRemoveChild: (Long) -> Unit,
    onMemberRoles: (Long, List<Long>) -> Unit,
    onSaveRole: (Long?, GroupRoleSaveRequest) -> Unit,
    onDeleteRole: (Long) -> Unit
) {
    var name by remember(group.id) { mutableStateOf(group.name) }
    var description by remember(group.id) { mutableStateOf(group.description ?: "") }
    var showInNavigation by remember(group.id) { mutableStateOf(group.showInNavigation ?: false) }
    var selectedMember by remember(group.id) { mutableStateOf<Long?>(null) }
    var selectedChild by remember(group.id) { mutableStateOf<Long?>(null) }
    var selectedRoleMember by remember(group.id) { mutableStateOf<Long?>(null) }
    var chosenRoles by remember(group.id) { mutableStateOf<Set<Long>>(emptySet()) }
    var openMenu by remember { mutableStateOf("") }
    var editingRole by remember(group.id) { mutableStateOf<GroupRoleResponse?>(null) }
    var roleDialog by remember(group.id) { mutableStateOf(false) }
    var roleName by remember(group.id) { mutableStateOf("") }
    var roleDescription by remember(group.id) { mutableStateOf("") }
    var roleToDelete by remember(group.id) { mutableStateOf<GroupRoleResponse?>(null) }

    fun openRole(role: GroupRoleResponse?) {
        editingRole = role
        roleName = role?.name ?: ""
        roleDescription = role?.description ?: ""
        roleDialog = true
    }

    val memberCandidates = members.filter { item ->
        group.members.orEmpty().none { it.memberId == item.id }
    }
    val groupCandidates = groups.filter { item ->
        item.id != group.id &&
            group.childGroups.orEmpty().none { it.groupId == item.id }
    }
    val activeRoleMember = group.members.orEmpty().firstOrNull { it.memberId == selectedRoleMember }
    val dialogTitle = memberText(language, "Редагувати групу", "Gruppe bearbeiten", "Edit group", "Редактировать группу")
    val addText = memberText(language, "Додати", "Hinzufügen", "Add", "Добавить")

    AlertDialog(
        onDismissRequest = { if (!busy) onClose() },
        title = { Text(dialogTitle) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(memberText(language, "Назва", "Name", "Name", "Название")) }
                    )
                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(memberText(language, "Опис", "Beschreibung", "Description", "Описание")) }
                    )
                    Row {
                        Checkbox(checked = showInNavigation, onCheckedChange = { showInNavigation = it })
                        Text(memberText(language, "Показувати в меню", "Im Menü anzeigen", "Show in menu", "Показывать в меню"))
                    }
                }
                item {
                    Text(memberText(language, "Учасники групи", "Gruppenmitglieder", "Group members", "Участники группы"),
                        style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { openMenu = "member" }, modifier = Modifier.fillMaxWidth()) {
                        Text((memberCandidates.firstOrNull { it.id == selectedMember }?.name ?: addText) + " ▾")
                    }
                    DropdownMenu(expanded = openMenu == "member", onDismissRequest = { openMenu = "" }) {
                        memberCandidates.forEach { member ->
                            DropdownMenuItem(text = { Text(member.name) }, onClick = {
                                selectedMember = member.id
                                openMenu = ""
                            })
                        }
                    }
                    Button(enabled = !busy && selectedMember != null, onClick = {
                        selectedMember?.let(onAddMember)
                        selectedMember = null
                    }) { Text(addText) }
                }
                items(group.members.orEmpty(), key = { "member" + it.memberId }) { member ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(member.memberName, modifier = Modifier.weight(1f))
                        TextButton(enabled = !busy, onClick = {
                            selectedRoleMember = member.memberId
                            chosenRoles = member.roleIds.orEmpty().toSet()
                        }) { Text(memberText(language, "Ролі", "Rollen", "Roles", "Роли")) }
                        TextButton(enabled = !busy, onClick = { onRemoveMember(member.memberId) }) { Text("×") }
                    }
                }
                item {
                    Text(memberText(language, "Підгрупи", "Untergruppen", "Subgroups", "Подгруппы"),
                        style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { openMenu = "child" }, modifier = Modifier.fillMaxWidth()) {
                        Text((groupCandidates.firstOrNull { it.id == selectedChild }?.name ?: addText) + " ▾")
                    }
                    DropdownMenu(expanded = openMenu == "child", onDismissRequest = { openMenu = "" }) {
                        groupCandidates.forEach { child ->
                            DropdownMenuItem(text = { Text(child.name) }, onClick = {
                                selectedChild = child.id
                                openMenu = ""
                            })
                        }
                    }
                    Button(enabled = !busy && selectedChild != null, onClick = {
                        selectedChild?.let(onAddChild)
                        selectedChild = null
                    }) { Text(addText) }
                }
                items(group.childGroups.orEmpty(), key = { "child" + it.groupId }) { child ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(child.groupName, modifier = Modifier.weight(1f))
                        TextButton(enabled = !busy, onClick = { onRemoveChild(child.groupId) }) { Text("×") }
                    }
                }
                item {
                    Text(memberText(language, "Доступні ролі", "Verfügbare Rollen", "Available roles", "Доступные роли"),
                        style = MaterialTheme.typography.titleMedium)
                    group.roles.orEmpty().forEach { role ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(role.name, modifier = Modifier.weight(1f))
                            if (role.visibility == "PRIVATE" && role.systemDefault != true) {
                                TextButton(enabled = !busy, onClick = { openRole(role) }) {
                                    Text(memberText(language, "Змінити", "Ändern", "Edit", "Изменить"))
                                }
                                TextButton(enabled = !busy, onClick = { roleToDelete = role }) {
                                    Text("×")
                                }
                            }
                        }
                    }
                    OutlinedButton(enabled = !busy, onClick = { openRole(null) }) {
                        Text(memberText(language, "+ Локальна роль", "+ Lokale Rolle", "+ Local role", "+ Локальная роль"))
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = !busy && name.isNotBlank(), onClick = {
                onSave(GroupUpdateRequest(name.trim(), description.trim().ifBlank { null }, showInNavigation))
            }) { Text(memberText(language, "Зберегти", "Speichern", "Save", "Сохранить")) }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onClose) {
                Text(memberText(language, "Закрити", "Schließen", "Close", "Закрыть"))
            }
        }
    )
    if (roleDialog) {
        val duplicate = group.roles.orEmpty().any {
            it.id != editingRole?.id && it.name.trim().equals(roleName.trim(), ignoreCase = true)
        }
        AlertDialog(
            onDismissRequest = { if (!busy) roleDialog = false },
            title = { Text(memberText(language, "Локальна роль", "Lokale Rolle", "Local role", "Локальная роль")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = roleName,
                        onValueChange = { roleName = it },
                        label = { Text(memberText(language, "Назва", "Name", "Name", "Название")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roleDescription,
                        onValueChange = { roleDescription = it },
                        label = { Text(memberText(language, "Опис", "Beschreibung", "Description", "Описание")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (duplicate) Text(
                        memberText(language, "Роль із такою назвою вже існує", "Name bereits vorhanden",
                            "Role name already exists", "Роль с таким названием уже существует"),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(enabled = !busy && roleName.isNotBlank() && !duplicate, onClick = {
                    onSaveRole(
                        editingRole?.id,
                        GroupRoleSaveRequest(roleName.trim(), roleDescription.trim().ifBlank { null }, editingRole?.roleSetId)
                    )
                    roleDialog = false
                }) { Text(memberText(language, "Зберегти", "Speichern", "Save", "Сохранить")) }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { roleDialog = false }) {
                    Text(memberText(language, "Скасувати", "Abbrechen", "Cancel", "Отмена"))
                }
            }
        )
    }
    roleToDelete?.let { role ->
        AlertDialog(
            onDismissRequest = { roleToDelete = null },
            title = { Text(memberText(language, "Видалити локальну роль?", "Lokale Rolle löschen?",
                "Delete local role?", "Удалить локальную роль?")) },
            text = { Text(role.name) },
            confirmButton = {
                Button(enabled = !busy, onClick = {
                    onDeleteRole(role.id)
                    roleToDelete = null
                }) { Text(memberText(language, "Видалити", "Löschen", "Delete", "Удалить")) }
            },
            dismissButton = {
                TextButton(onClick = { roleToDelete = null }) {
                    Text(memberText(language, "Скасувати", "Abbrechen", "Cancel", "Отмена"))
                }
            }
        )
    }
    if (activeRoleMember != null) {
        AlertDialog(
            onDismissRequest = { selectedRoleMember = null },
            title = { Text(activeRoleMember.memberName) },
            text = {
                Column {
                    group.roles.orEmpty().forEach { role ->
                        Row {
                            Checkbox(
                                checked = role.id in chosenRoles,
                                onCheckedChange = { checked ->
                                    chosenRoles = if (checked) chosenRoles + role.id else chosenRoles - role.id
                                }
                            )
                            Text(role.name)
                        }
                    }
                }
            },
            confirmButton = {
                Button(enabled = !busy, onClick = {
                    onMemberRoles(activeRoleMember.memberId, chosenRoles.toList())
                    selectedRoleMember = null
                }) { Text(memberText(language, "Зберегти", "Speichern", "Save", "Сохранить")) }
            },
            dismissButton = {
                TextButton(onClick = { selectedRoleMember = null }) {
                    Text(memberText(language, "Скасувати", "Abbrechen", "Cancel", "Отмена"))
                }
            }
        )
    }
}
