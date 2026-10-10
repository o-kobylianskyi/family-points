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
import com.olehkobylianskyi.familypoints.android.data.GroupPointOperationRequest
import com.olehkobylianskyi.familypoints.android.data.GroupUpdateRequest
import com.olehkobylianskyi.familypoints.android.data.GroupPermissionGrantResponse
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
    val canManagePoints = currentUser.permissions.any { it == "MANAGE_POINTS" || it == "ADMIN_OVERRIDE" }
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
    var grants by remember { mutableStateOf<List<GroupPermissionGrantResponse>>(emptyList()) }

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
        roleId = member?.workspaceRoleId ?: data?.roles?.firstOrNull { it.code == "CHILD" }?.id
            ?: data?.roles?.firstOrNull()?.id
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
                        modifier = Modifier.padding(top = 8.dp),
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
                                Text(memberWorkspaceRoleLabel(language,
                                    data?.roles?.firstOrNull { it.id == member.workspaceRoleId }?.code,
                                    member.workspaceRoleName))
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

    LaunchedEffect(editingGroupId) {
        val id = editingGroupId ?: return@LaunchedEffect
        try { grants = repository.getGroupPermissions(currentUser.workspaceId, id) }
        catch (e: Exception) { failure(e) }
    }

    val groupToEdit = data?.groups?.firstOrNull { it.id == editingGroupId }
    if (groupToEdit != null && allowed) {
        GroupEditorDialog(
            group = groupToEdit,
            groups = data?.groups.orEmpty(),
            members = data?.members.orEmpty(),
            language = language,
            busy = busy,
            permissionGrants = grants,
            canManagePoints = canManagePoints,
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
            onSaveRole = { roleId, request, permissions -> mutate {
                repository.saveGroupRole(currentUser.workspaceId, groupToEdit.id, roleId, request, permissions)
                grants = repository.getGroupPermissions(currentUser.workspaceId, groupToEdit.id)
            } },
            onDeleteRole = { roleId -> mutate {
                repository.deleteGroupRole(currentUser.workspaceId, groupToEdit.id, roleId)
            } },
            onPostPoints = { request -> mutate {
                repository.postGroupPoints(currentUser.workspaceId, groupToEdit.id, request)
            } },
            onSavePermissions = { roleId, permissions -> mutate {
                repository.updateRolePermissions(currentUser.workspaceId, groupToEdit.id, roleId, permissions)
                grants = repository.getGroupPermissions(currentUser.workspaceId, groupToEdit.id)
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
                    Text(memberText(language, "Тип учасника", "Mitgliedstyp", "Member type", "Тип участника"),
                        style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(onClick = { menu = "type" }, modifier = Modifier.fillMaxWidth()) {
                        Text(memberTypeLabel(language, type) + " ▾")
                    }
                    DropdownMenu(expanded = menu == "type", onDismissRequest = { menu = "" }) {
                        listOf("PARENT", "CHILD", "OTHER").forEach { option ->
                            DropdownMenuItem(text = { Text(memberTypeLabel(language, option)) }, onClick = { type = option; menu = "" })
                        }
                    }
                    Text(memberText(language, "Роль у робочому просторі", "Rolle im Arbeitsbereich",
                        "Workspace role", "Роль в рабочем пространстве"),
                        style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(onClick = { menu = "role" }, modifier = Modifier.fillMaxWidth()) {
                        Text((data?.roles?.firstOrNull { it.id == roleId }?.let { memberWorkspaceRoleLabel(language, it.code, it.name) }
                            ?: memberText(language, "Оберіть роль", "Rolle wählen", "Select role", "Выберите роль")) + " ▾")
                    }
                    DropdownMenu(expanded = menu == "role", onDismissRequest = { menu = "" }) {
                        data?.roles.orEmpty().forEach { role ->
                            DropdownMenuItem(text = { Text(memberWorkspaceRoleLabel(language, role.code, role.name)) },
                                onClick = { roleId = role.id; menu = "" })
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
    if (visited.isEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                GroupTreeRow(group, groups, visited, language, onEdit)
            }
        }
    } else {
        GroupTreeRow(group, groups, visited, language, onEdit)
    }
}

@Composable
private fun GroupTreeRow(
    group: MembersGroupResponse,
    groups: List<MembersGroupResponse>,
    visited: Set<Long>,
    language: AppLanguage,
    onEdit: ((Long) -> Unit)?
) {
    val nested = visited.isNotEmpty()
    var expanded by remember(group.id) { mutableStateOf(!nested) }
    val members = group.members.orEmpty()
    val children = group.childGroups.orEmpty()
    val cycle = group.id in visited
    val balances = group.balances.orEmpty().filter { it.amount != 0L }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    (if (nested) "  ↳ " else "") +
                        (if (children.isNotEmpty()) (if (expanded) "▾ " else "▸ ") else "• ") +
                        group.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (onEdit != null) {
                TextButton(onClick = { onEdit(group.id) }) {
                    Text(memberText(language, "Змінити", "Ändern", "Edit", "Изменить"))
                }
            }
        }
        Text(
            "${members.size} " + memberText(language, "учасників", "Mitglieder", "members", "участников") +
                " · ${children.size} " + memberText(language, "підгруп", "Untergruppen", "subgroups", "подгрупп"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (cycle) {
            Text(memberText(language, "Циклічне посилання", "Zyklischer Verweis",
                "Circular reference", "Циклическая ссылка"), color = MaterialTheme.colorScheme.error)
        } else if (expanded) {
            group.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            if (members.isNotEmpty()) {
                Text(members.joinToString(" · ") { it.memberName }, style = MaterialTheme.typography.bodySmall)
            }
            if (balances.isNotEmpty()) {
                Text(balances.joinToString(" · ") {
                    "${it.amount} ${it.name ?: it.code ?: ""}"
                }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            children.forEach { child ->
                val childGroup = groups.firstOrNull { it.id == child.groupId }
                if (childGroup == null) {
                    Text("↳ " + child.groupName, style = MaterialTheme.typography.bodySmall)
                } else {
                    GroupTreeCard(childGroup, groups, visited + group.id, language, onEdit)
                }
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
    permissionGrants: List<GroupPermissionGrantResponse>,
    canManagePoints: Boolean,
    onClose: () -> Unit,
    onSave: (GroupUpdateRequest) -> Unit,
    onAddMember: (Long) -> Unit,
    onRemoveMember: (Long) -> Unit,
    onAddChild: (Long) -> Unit,
    onRemoveChild: (Long) -> Unit,
    onMemberRoles: (Long, List<Long>) -> Unit,
    onSaveRole: (Long?, GroupRoleSaveRequest, Map<String, String>?) -> Unit,
    onDeleteRole: (Long) -> Unit,
    onSavePermissions: (Long, Map<String, String>) -> Unit,
    onPostPoints: (GroupPointOperationRequest) -> Unit
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
    var roleNameTouched by remember(group.id) { mutableStateOf(false) }
    var roleProfile by remember(group.id) { mutableStateOf("NONE") }
    var roleProfileMenu by remember { mutableStateOf(false) }
    var roleToDelete by remember(group.id) { mutableStateOf<GroupRoleResponse?>(null) }
    var permissionRole by remember(group.id) { mutableStateOf<GroupRoleResponse?>(null) }
    var permissionValues by remember(group.id) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var profileMenu by remember { mutableStateOf(false) }
    var pointsDialog by remember { mutableStateOf(false) }
    var pointTypeId by remember { mutableStateOf<Long?>(null) }
    var pointAmount by remember { mutableStateOf("") }
    var pointReason by remember { mutableStateOf("") }
    var pointOperation by remember { mutableStateOf("EARN") }

    fun openRole(role: GroupRoleResponse?) {
        editingRole = role
        roleNameTouched = role != null
        roleProfile = if (role == null) "NONE" else permissionPresets.entries
            .firstOrNull { (_, permissions) ->
                permissions == permissionGrants.filter { it.roleId == role.id }
                    .associate { it.permission to it.scope }
            }?.key ?: "CUSTOM"
        roleName = role?.name ?: suggestedGroupRoleName(
            memberText(language, "Нова роль", "Neue Rolle", "New role", "Новая роль"),
            group.roles.orEmpty(), null
        )
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
                    Text(memberText(language, "Баланс групи", "Gruppenguthaben", "Group balances", "Баланс группы"),
                        style = MaterialTheme.typography.titleMedium)
                    if (canManagePoints && group.balances.orEmpty().isNotEmpty()) {
                        OutlinedButton(enabled = !busy, onClick = {
                            pointTypeId = group.balances.orEmpty().firstOrNull()?.pointTypeId
                            pointAmount = ""
                            pointReason = ""
                            pointOperation = "EARN"
                            pointsDialog = true
                        }) { Text(memberText(language, "Змінити бали", "Punkte ändern", "Adjust points", "Изменить баллы")) }
                    }
                    if (group.balances.orEmpty().isEmpty()) {
                        Text(memberText(language,
                            "Для цієї групи балансів поки немає",
                            "Keine Gruppenguthaben vorhanden",
                            "No group balances yet",
                            "Для этой группы пока нет балансов"))
                    } else {
                        group.balances.orEmpty().forEach { balance ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(balance.name?.takeIf { it.isNotBlank() } ?: balance.code ?: "#${balance.pointTypeId}",
                                    modifier = Modifier.weight(1f))
                                Text(balance.amount.toString())
                            }
                        }
                    }
                }
                item {
                    Text(memberText(language, "Доступні ролі", "Verfügbare Rollen", "Available roles", "Доступные роли"),
                        style = MaterialTheme.typography.titleMedium)
                    group.roles.orEmpty().forEach { role ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(role.name, modifier = Modifier.weight(1f))
                            if (role.visibility == "PRIVATE" && role.systemDefault != true) {
                                TextButton(enabled = !busy, onClick = {
                                    permissionRole = role
                                    permissionValues = permissionGrants.filter { it.roleId == role.id }
                                        .associate { it.permission to it.scope }
                                }) { Text(memberText(language, "Права", "Rechte", "Permissions", "Права")) }
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
    permissionRole?.let { role ->
        val profileNames = listOf("NONE", "EXECUTOR", "SENIOR", "LEADER", "CONTROL", "CUSTOM")
        AlertDialog(
            onDismissRequest = { if (!busy) permissionRole = null },
            title = { Text(role.name) },
            text = {
                LazyColumn {
                    item {
                        Text(memberText(language, "Профіль дозволів", "Berechtigungsprofil",
                            "Permission profile", "Профиль прав"))
                        OutlinedButton(onClick = { profileMenu = true }) {
                            Text(groupProfileLabel(language, profileNames.firstOrNull {
                                it != "CUSTOM" && permissionPresets[it] == permissionValues
                            } ?: "CUSTOM") + " ▾")
                        }
                        DropdownMenu(expanded = profileMenu, onDismissRequest = { profileMenu = false }) {
                            profileNames.forEach { profile ->
                                DropdownMenuItem(text = { Text(groupProfileLabel(language, profile)) }, onClick = {
                                    if (profile != "CUSTOM") permissionValues = permissionPresets[profile].orEmpty()
                                    profileMenu = false
                                })
                            }
                        }
                    }
                    permissionCategories.forEach { (category, permissions) ->
                        item { Text(groupPermissionCategory(language, category), style = MaterialTheme.typography.titleSmall) }
                        items(permissions) { permission ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Checkbox(checked = permission in permissionValues, onCheckedChange = { checked ->
                                    permissionValues = if (checked)
                                        permissionValues + (permission to "GROUP")
                                    else permissionValues - permission
                                })
                                Text(groupPermissionLabel(language, permission), modifier = Modifier.weight(1f))
                                if (permission in permissionValues) {
                                    TextButton(onClick = {
                                        val next = if (permissionValues[permission] == "GROUP") "GROUP_SUBTREE" else "GROUP"
                                        permissionValues = permissionValues + (permission to next)
                                    }) { Text(groupScopeLabel(language, permissionValues[permission] ?: "GROUP")) }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(enabled = !busy, onClick = {
                    onSavePermissions(role.id, permissionValues)
                    permissionRole = null
                }) { Text(memberText(language, "Зберегти", "Speichern", "Save", "Сохранить")) }
            },
            dismissButton = {
                TextButton(onClick = { permissionRole = null }) {
                    Text(memberText(language, "Скасувати", "Abbrechen", "Cancel", "Отмена"))
                }
            }
        )
    }
    if (pointsDialog && canManagePoints) {
        val amount = pointAmount.toIntOrNull()
        val existingBalance = group.balances.orEmpty().firstOrNull { it.pointTypeId == pointTypeId }
        val insufficient = pointOperation == "SPEND" && amount != null &&
            (existingBalance == null || existingBalance.amount < amount.toLong())
        AlertDialog(
            onDismissRequest = { if (!busy) pointsDialog = false },
            title = { Text(memberText(language, "Бали групи", "Gruppenpunkte", "Group points", "Баллы группы")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(memberText(language, "Тип балів", "Punkteart", "Point type", "Тип баллов"))
                    group.balances.orEmpty().forEach { balance ->
                        OutlinedButton(
                            onClick = { pointTypeId = balance.pointTypeId },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text((if (balance.pointTypeId == pointTypeId) "✓ " else "") +
                                (balance.name ?: balance.code ?: "#${balance.pointTypeId}") +
                                " (${balance.amount})")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pointOperation = "EARN" }) {
                            Text((if (pointOperation == "EARN") "✓ " else "") +
                                memberText(language, "Нарахувати", "Gutschreiben", "Credit", "Начислить"))
                        }
                        OutlinedButton(onClick = { pointOperation = "SPEND" }) {
                            Text((if (pointOperation == "SPEND") "✓ " else "") +
                                memberText(language, "Списати", "Abziehen", "Debit", "Списать"))
                        }
                    }
                    OutlinedTextField(
                        value = pointAmount,
                        onValueChange = { value -> pointAmount = value.filter { it.isDigit() }.take(9) },
                        label = { Text(memberText(language, "Кількість", "Anzahl", "Amount", "Количество")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pointReason,
                        onValueChange = { pointReason = it.take(255) },
                        label = { Text(memberText(language, "Причина", "Begründung", "Reason", "Причина")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (insufficient) {
                        Text(memberText(language, "Недостатньо балів", "Nicht genügend Punkte",
                            "Insufficient balance", "Недостаточно баллов"), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(enabled = !busy && pointTypeId != null && amount != null && amount > 0 && !insufficient,
                    onClick = {
                        val id = pointTypeId ?: return@Button
                        val value = amount ?: return@Button
                        onPostPoints(GroupPointOperationRequest(id, value, pointOperation, pointReason.trim().ifBlank { null }))
                        pointsDialog = false
                    }
                ) { Text(memberText(language, "Підтвердити", "Bestätigen", "Confirm", "Подтвердить")) }
            },
            dismissButton = {
                TextButton(onClick = { pointsDialog = false }) {
                    Text(memberText(language, "Скасувати", "Abbrechen", "Cancel", "Отмена"))
                }
            }
        )
    }
    if (roleDialog) {
        val comparablePermissions = permissionPresets[roleProfile]
        val equalPermissionRole = comparablePermissions?.let { selected ->
            group.roles.orEmpty().firstOrNull { role ->
                role.id != editingRole?.id &&
                    permissionGrants.filter { it.roleId == role.id }
                        .associate { it.permission to it.scope } == selected
            }
        }
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
                        onValueChange = { roleName = it; roleNameTouched = true },
                        label = { Text(memberText(language, "Назва", "Name", "Name", "Название")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roleDescription,
                        onValueChange = { roleDescription = it },
                        label = { Text(memberText(language, "Опис", "Beschreibung", "Description", "Описание")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(memberText(language, "Профіль дозволів", "Berechtigungsprofil",
                        "Permission profile", "Профиль прав"))
                    OutlinedButton(onClick = { roleProfileMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(groupProfileLabel(language, roleProfile) + " ▾")
                    }
                    DropdownMenu(expanded = roleProfileMenu, onDismissRequest = { roleProfileMenu = false }) {
                        listOf("NONE", "EXECUTOR", "SENIOR", "LEADER", "CONTROL", "CUSTOM").forEach { profile ->
                            DropdownMenuItem(text = { Text(profile) }, onClick = {
                                roleProfile = profile
                                if (!roleNameTouched && profile != "CUSTOM") {
                                    roleName = suggestedGroupRoleName(
                                        groupRoleProfileName(language, profile),
                                        group.roles.orEmpty(), editingRole?.id
                                    )
                                }
                                roleProfileMenu = false
                            })
                        }
                    }
                    val proposedPermissions = permissionPresets[roleProfile]
                    if (editingRole != null && proposedPermissions != null) {
                        val existingPermissions = permissionGrants.filter { it.roleId == editingRole?.id }
                            .associate { it.permission to it.scope }
                        if (proposedPermissions != existingPermissions) {
                            Text(memberText(language,
                                "Профіль змінить права ролі після збереження.",
                                "Das Profil ändert die Rechte nach dem Speichern.",
                                "Saving will update this role's permissions.",
                                "После сохранения профиль изменит права роли."))
                        }
                    }
                    if (equalPermissionRole != null) Text(memberText(language,
                        "Такі самі права вже має: " + equalPermissionRole.name,
                        "Gleiche Berechtigungen: " + equalPermissionRole.name,
                        "Same permissions as: " + equalPermissionRole.name,
                        "Такие же права у: " + equalPermissionRole.name))
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
                        GroupRoleSaveRequest(roleName.trim(), roleDescription.trim().ifBlank { null }, editingRole?.roleSetId),
                        permissionPresets[roleProfile]
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

private val permissionCategories = listOf(
    "Tasks" to listOf("TASK_VIEW", "TASK_CREATE", "TASK_ASSIGN", "TASK_MANAGE", "TASK_APPROVE"),
    "Members" to listOf("MEMBER_VIEW", "MEMBER_MANAGE"),
    "Groups" to listOf("GROUP_VIEW", "GROUP_MANAGE", "SUBGROUP_MANAGE"),
    "Points" to listOf("POINT_VIEW", "POINT_AWARD", "POINT_SPEND")
)

private val permissionPresets: Map<String, Map<String, String>> = mapOf(
    "NONE" to emptyMap(),
    "EXECUTOR" to mapOf("TASK_VIEW" to "GROUP"),
    "SENIOR" to listOf("TASK_VIEW", "TASK_ASSIGN", "TASK_APPROVE", "MEMBER_VIEW", "GROUP_VIEW")
        .associateWith { "GROUP" },
    "LEADER" to listOf(
        "TASK_VIEW", "TASK_CREATE", "TASK_ASSIGN", "TASK_MANAGE", "TASK_APPROVE",
        "MEMBER_VIEW", "MEMBER_MANAGE", "GROUP_VIEW", "GROUP_MANAGE",
        "SUBGROUP_MANAGE", "POINT_VIEW", "POINT_AWARD"
    ).associateWith { "GROUP" },
    "CONTROL" to listOf("TASK_VIEW", "TASK_APPROVE", "MEMBER_VIEW", "GROUP_VIEW", "POINT_VIEW")
        .associateWith { "GROUP" }
)

private fun suggestedGroupRoleName(base: String, existing: List<GroupRoleResponse>, editingId: Long?): String {
    val used = existing.filter { it.id != editingId }
        .map { it.name.trim().lowercase() }.toSet()
    if (base.trim().lowercase() !in used) return base
    var number = 2
    while (("$base $number").lowercase() in used) number++
    return "$base $number"
}

private fun groupRoleProfileName(language: AppLanguage, profile: String): String = when (profile) {
    "EXECUTOR" -> memberText(language, "Виконавець", "Ausführender", "Executor", "Исполнитель")
    "SENIOR" -> memberText(language, "Старший", "Senior", "Senior", "Старший")
    "LEADER" -> memberText(language, "Керівник", "Leitung", "Leader", "Руководитель")
    "CONTROL" -> memberText(language, "Контролер", "Kontrolle", "Controller", "Контролёр")
    else -> memberText(language, "Нова роль", "Neue Rolle", "New role", "Новая роль")
}

private fun groupProfileLabel(language: AppLanguage, profile: String): String = when (profile) {
    "NONE" -> memberText(language, "Без дозволів", "Keine Rechte", "No permissions", "Без прав")
    "EXECUTOR" -> groupRoleProfileName(language, "EXECUTOR")
    "SENIOR" -> groupRoleProfileName(language, "SENIOR")
    "LEADER" -> groupRoleProfileName(language, "LEADER")
    "CONTROL" -> groupRoleProfileName(language, "CONTROL")
    else -> memberText(language, "Власний", "Benutzerdefiniert", "Custom", "Свой")
}

private fun groupScopeLabel(language: AppLanguage, scope: String): String =
    if (scope == "GROUP_SUBTREE")
        memberText(language, "Група + підгрупи", "Gruppe + Untergruppen", "Group + subgroups", "Группа + подгруппы")
    else memberText(language, "Лише група", "Nur Gruppe", "Group only", "Только группа")

private fun groupPermissionCategory(language: AppLanguage, category: String): String = when (category) {
    "Tasks" -> memberText(language, "Завдання", "Aufgaben", "Tasks", "Задания")
    "Members" -> memberText(language, "Учасники", "Mitglieder", "Members", "Участники")
    "Groups" -> memberText(language, "Групи", "Gruppen", "Groups", "Группы")
    else -> memberText(language, "Бали", "Punkte", "Points", "Баллы")
}

private fun groupPermissionLabel(language: AppLanguage, permission: String): String = when (permission) {
    "TASK_VIEW" -> memberText(language, "Перегляд завдань", "Aufgaben ansehen", "View tasks", "Просмотр заданий")
    "TASK_CREATE" -> memberText(language, "Створення завдань", "Aufgaben erstellen", "Create tasks", "Создание заданий")
    "TASK_ASSIGN" -> memberText(language, "Призначення завдань", "Aufgaben zuweisen", "Assign tasks", "Назначение заданий")
    "TASK_MANAGE" -> memberText(language, "Керування завданнями", "Aufgaben verwalten", "Manage tasks", "Управление заданиями")
    "TASK_APPROVE" -> memberText(language, "Підтвердження завдань", "Aufgaben bestätigen", "Approve tasks", "Подтверждение заданий")
    "MEMBER_VIEW" -> memberText(language, "Перегляд учасників", "Mitglieder ansehen", "View members", "Просмотр участников")
    "MEMBER_MANAGE" -> memberText(language, "Керування учасниками", "Mitglieder verwalten", "Manage members", "Управление участниками")
    "GROUP_VIEW" -> memberText(language, "Перегляд груп", "Gruppen ansehen", "View groups", "Просмотр групп")
    "GROUP_MANAGE" -> memberText(language, "Керування групами", "Gruppen verwalten", "Manage groups", "Управление группами")
    "SUBGROUP_MANAGE" -> memberText(language, "Керування підгрупами", "Untergruppen verwalten", "Manage subgroups", "Управление подгруппами")
    "POINT_VIEW" -> memberText(language, "Перегляд балів", "Punkte ansehen", "View points", "Просмотр баллов")
    "POINT_AWARD" -> memberText(language, "Нарахування балів", "Punkte vergeben", "Award points", "Начисление баллов")
    "POINT_SPEND" -> memberText(language, "Списання балів", "Punkte ausgeben", "Spend points", "Списание баллов")
    else -> permission
}

private fun memberTypeLabel(language: AppLanguage, type: String): String = when (type) {
    "PARENT" -> memberText(language, "Батьки", "Elternteil", "Parent", "Родитель")
    "CHILD" -> memberText(language, "Дитина", "Kind", "Child", "Ребёнок")
    else -> memberText(language, "Інше", "Sonstige", "Other", "Другое")
}

private fun memberWorkspaceRoleLabel(
    language: AppLanguage,
    code: String?,
    fallback: String
): String = when (code) {
    "FAMILY_ADMIN" -> memberText(language, "Адміністратор сім’ї", "Familienadministrator",
        "Family administrator", "Администратор семьи")
    "PARENT" -> memberText(language, "Батьки", "Elternteil", "Parent", "Родитель")
    "CHILD" -> memberText(language, "Дитина", "Kind", "Child", "Ребёнок")
    else -> fallback
}
