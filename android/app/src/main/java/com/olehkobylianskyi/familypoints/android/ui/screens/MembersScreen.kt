package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import com.olehkobylianskyi.familypoints.android.network.MediaBindRequest
import com.olehkobylianskyi.familypoints.android.network.MediaCatalogItemDto
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import com.olehkobylianskyi.familypoints.android.ui.components.FpCard as Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
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
import com.olehkobylianskyi.familypoints.android.data.PointsRepository
import com.olehkobylianskyi.familypoints.android.network.ServerReachability
import com.olehkobylianskyi.familypoints.android.network.ServerStatus
import com.olehkobylianskyi.familypoints.android.i18n.formatPointAmount
import com.olehkobylianskyi.familypoints.android.data.WorkspaceMemberResponse
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.ui.components.AppHeader
import com.olehkobylianskyi.familypoints.android.ui.components.MemberMediaAvatar
import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.network.MediaTargetDto
import com.olehkobylianskyi.familypoints.android.network.MediaRefDto
import com.olehkobylianskyi.familypoints.android.network.MediaReferenceBatchDto
import com.olehkobylianskyi.familypoints.android.network.MediaRefDto
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
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
    refreshNonce: Int = 0,
    onRefreshComplete: (Int) -> Unit = {},
    language: AppLanguage,
    currentUser: CurrentUserResponse,
    repository: MembersRepository,
    pointsRepository: PointsRepository,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (DashboardDestination) -> Unit,
    onUnauthorized: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val serverStatus by ServerReachability.status.collectAsState()
    val online = serverStatus == ServerStatus.ONLINE
    var balances by remember { mutableStateOf<Map<Long, Long>>(emptyMap()) }
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var mediaRefresh by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) mediaRefresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var avatarRefs by remember(currentUser.accountId, currentUser.workspaceId, currentUser.username) {
        mutableStateOf<Map<Long, MediaRefDto>>(emptyMap())
    }
    val bearer = if (online) tokenStore.getAccessToken()?.let { "Bearer $it" } else null

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
    var accessMode by remember { mutableStateOf("profile") }
    var childLogin by remember { mutableStateOf("") }
    var childPassword by remember { mutableStateOf("") }
    var confirmChildPassword by remember { mutableStateOf("") }
    var invitationEmail by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var editingGroupId by remember { mutableStateOf<Long?>(null) }
    var grants by remember { mutableStateOf<List<GroupPermissionGrantResponse>>(emptyList()) }
    var avatarDraftKey by remember { mutableStateOf<String?>(null) }
    var avatarDraftClear by remember { mutableStateOf(false) }
    var avatarDraftPhoto by remember { mutableStateOf<Uri?>(null) }
    var avatarPhotoPreview by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(avatarDraftPhoto) {
        val uri = avatarDraftPhoto
        avatarPhotoPreview = if (uri == null) null else withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } catch (_: Exception) { null }
        }
    }
    var avatarVersion by remember { mutableStateOf<Int?>(null) }
    var avatarCatalog by remember(currentUser.accountId, currentUser.workspaceId) {
        mutableStateOf<List<MediaCatalogItemDto>>(emptyList())
    }
    var avatarEditorError by remember { mutableStateOf("") }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            avatarDraftPhoto = uri
            avatarDraftKey = null
            avatarDraftClear = false
        }
    }
    LaunchedEffect(currentUser.accountId, currentUser.workspaceId, bearer) {
        avatarCatalog = emptyList()
        val auth = bearer ?: return@LaunchedEffect
        try {
            avatarCatalog = ApiClient.mediaApi.catalog(auth, currentUser.workspaceId)
                .filter { it.category == "AVATAR" }
        } catch (_: Exception) { /* Optional picker; leave currently bound images intact. */ }
    }
    LaunchedEffect(editing?.id, editorOpen, bearer) {
        val id = editing?.id ?: return@LaunchedEffect
        if (!editorOpen) return@LaunchedEffect
        avatarVersion = null
        val auth = bearer ?: return@LaunchedEffect
        try {
            avatarVersion = ApiClient.mediaApi.memberAvatar(auth, currentUser.workspaceId, id).version
        } catch (_: Exception) {
            avatarEditorError = "Не вдалося отримати версію аватара"
        }
    }

    suspend fun reload() {
        data = repository.load(currentUser.workspaceId, currentUser.memberId)
    }
    fun failure(exception: Exception) {
        // A feature endpoint 401 does not prove the login session expired.\n        // Keep the user on this screen and display the actual failure.\n        error = if (exception is HttpException)\n            "Помилка запиту HTTP ${exception.code()}: ${exception.message()}"\n        else exception.message ?: "Request failed"
    }
    fun edit(member: WorkspaceMemberResponse?) {
        editing = member
        avatarDraftKey = null
        avatarDraftClear = false
        avatarDraftPhoto = null
        avatarVersion = null
        avatarEditorError = ""
        accessMode = "profile"
        childLogin = ""
        childPassword = ""
        confirmChildPassword = ""
        invitationEmail = member?.invitationEmail ?: ""
        name = member?.name ?: ""
        type = member?.memberType ?: "CHILD"
        roleId = member?.workspaceRoleId ?: data?.roles?.firstOrNull { it.code == "CHILD" }?.id
            ?: data?.roles?.firstOrNull()?.id
        editorOpen = true
    }
    fun mutate(operation: suspend () -> Unit) {
        if (!online) return
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
    LaunchedEffect(currentUser.workspaceId, currentUser.memberId, serverStatus, refreshNonce) {
        loading = false
        error = ""
        val saved = repository.cached(currentUser.workspaceId, currentUser.memberId)
        val cachedPeople = repository.cachedMembers(currentUser.workspaceId, currentUser.memberId)
        data = saved ?: MembersPageData(cachedPeople, emptyList(), emptyList())
        balances = cachedPeople.mapNotNull { member ->
            if (canManagePoints || member.id == currentUser.memberId)
                pointsRepository.cachedConfirmedBalance(currentUser.workspaceId, member.id)
                    ?.let { member.id to it }
            else null
        }.toMap()
        if (!online) return@LaunchedEffect
        loading = saved == null && cachedPeople.isEmpty()
        try {
            reload()
            data?.members?.let { people ->
                pointsRepository.prefetchBalances(currentUser.workspaceId,
                    if (canManagePoints) people else people.filter { it.id == currentUser.memberId })
                balances = people.mapNotNull { member ->
                    if (canManagePoints || member.id == currentUser.memberId)
                        pointsRepository.cachedConfirmedBalance(currentUser.workspaceId, member.id)
                            ?.let { member.id to it }
                    else null
                }.toMap()
            }
        } catch (e: Exception) {
            if (data?.members.isNullOrEmpty()) failure(e)
        } finally {
            loading = false
            if (refreshNonce > 0) onRefreshComplete(refreshNonce)
        }
    }

    LaunchedEffect(currentUser.accountId, currentUser.workspaceId, currentUser.username, data?.members, serverStatus, refreshNonce, mediaRefresh) {
        // Keep existing authorized thumbnails while refreshing on app resume.
        val people = data?.members.orEmpty()
        val auth = bearer ?: return@LaunchedEffect
        if (people.isEmpty()) return@LaunchedEffect
        try {
            // Endpoint accepts <=100 targets; keep batches bounded and workspace-scoped.
            val refs = mutableMapOf<Long, MediaRefDto>()
            for (batch in people.chunked(100)) {
                val response = ApiClient.mediaApi.references(
                    auth, currentUser.workspaceId,
                    MediaReferenceBatchDto(batch.map { MediaTargetDto("MEMBER", it.id, "AVATAR") })
                )
                response.forEach { item ->
                    item.image?.let { refs[item.target.targetId] = it }
                }
            }
            avatarRefs = refs
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            // Media is supplemental; failure must not log users out or hide member profiles.
        }
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
        if (!online) {
            Text(
                memberText(language,
                    "Офлайн · збережені учасники й баланси. Редагування недоступне.",
                    "Offline · gespeicherte Mitglieder und Guthaben. Bearbeitung nicht verfügbar.",
                    "Offline · saved members and balances. Editing is unavailable.",
                    "Офлайн · сохранённые участники и балансы. Редактирование недоступно."),
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (loading) Text(memberText(language, "Завантаження…", "Laden…", "Loading…", "Загрузка…"))
        else {
            if (tab == "members" && allowed && online) {
                Button(onClick = { edit(null) }) {
                    Text(memberText(language, "Додати учасника", "Mitglied hinzufügen", "Add member", "Добавить участника"))
                }
            }
            if (tab == "groups" && allowed && online) {
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
                contentPadding = PaddingValues(top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (tab == "members") {
                    items(data?.members.orEmpty(), key = { "m" + it.id }) { member ->
                        Card(modifier = Modifier.fillMaxWidth().then(
                            if (allowed && online) Modifier.clickable { edit(member) } else Modifier
                        )) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    MemberMediaAvatar(currentUser.workspaceId, bearer, avatarRefs[member.id], member.name)
                                    Text(member.name, style = MaterialTheme.typography.titleMedium)
                                }
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
                                if (allowed && online) {
                                    (member.accountEmail ?: member.invitationEmail)?.let { contact ->
                                        Text("Email: $contact", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    val status = when (member.accessStatus) {
                                        "ACTIVE" -> memberText(language, "Акаунт активний", "Konto aktiv",
                                            "Account active", "Аккаунт активен")
                                        "DISABLED" -> memberText(language, "Акаунт вимкнено", "Konto deaktiviert",
                                            "Account disabled", "Аккаунт отключён")
                                        "PENDING_INVITATION" -> memberText(language, "Запрошення очікує підтвердження",
                                            "Einladung ausstehend", "Invitation pending", "Приглашение ожидает подтверждения")
                                        "NO_ACCOUNT" -> memberText(language, "Без акаунта", "Kein Konto",
                                            "No account", "Без аккаунта")
                                        else -> null
                                    }
                                    status?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                balances[member.id]?.let { cached ->
                                    Text(
                                        formatPointAmount(cached, language, "POINTS", "балів"),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
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

    LaunchedEffect(editingGroupId, serverStatus, refreshNonce) {
        if (!online) return@LaunchedEffect
        val id = editingGroupId ?: return@LaunchedEffect
        try { grants = repository.getGroupPermissions(currentUser.workspaceId, id) }
        catch (e: Exception) { failure(e) }
    }

    val groupToEdit = if (online) data?.groups?.firstOrNull { it.id == editingGroupId } else null
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

    if (editorOpen && online) {
        AlertDialog(
            onDismissRequest = { if (!busy) editorOpen = false },
            title = { Text(if (editing == null)
                memberText(language, "Новий учасник", "Neues Mitglied", "New member", "Новый участник")
                else memberText(language, "Редагувати учасника", "Mitglied bearbeiten", "Edit member", "Редактировать участника")) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = {
                        Text(memberText(language, "Ім’я", "Name", "Name", "Имя"))
                    }, singleLine = true)
                    if (editing != null) {
                        Text(memberText(language, "Аватар", "Avatar", "Avatar", "Аватар"),
                            style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (avatarPhotoPreview != null && avatarDraftPhoto != null) {
                                Image(bitmap = avatarPhotoPreview!!.asImageBitmap(),
                                    contentDescription = name,
                                    modifier = Modifier.size(42.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop)
                            } else {
                                MemberMediaAvatar(currentUser.workspaceId, bearer,
                                    if (avatarDraftClear) null
                                    else avatarDraftKey?.let { MediaRefDto("CATALOG", null, it, 0, null) }
                                        ?: avatarRefs[editing!!.id],
                                    editing!!.name)
                            }
                            TextButton(enabled = !busy, onClick = { photoPicker.launch("image/*") }) {
                                Text(memberText(language, "Завантажити фото", "Foto hochladen", "Upload photo", "Загрузить фото"))
                            }
                        }
                        if (avatarCatalog.isNotEmpty()) {
                            androidx.compose.foundation.layout.Box {
                                OutlinedButton(onClick = { menu = "avatar" }, enabled = !busy,
                                    modifier = Modifier.fillMaxWidth()) {
                                    Text((avatarDraftKey ?: memberText(language, "Обрати аватар",
                                        "Avatar auswählen", "Choose avatar", "Выбрать аватар")) + " ▾")
                                }
                                DropdownMenu(expanded = menu == "avatar",
                                    onDismissRequest = { menu = "" }) {
                                    avatarCatalog.forEach { option ->
                                        DropdownMenuItem(text = {
                                            Text(option.key.removePrefix("avatar-").replace('-', ' '))
                                        }, onClick = {
                                            avatarDraftKey = option.key
                                            avatarDraftPhoto = null
                                            avatarDraftClear = false
                                            menu = ""
                                        })
                                    }
                                }
                            }
                        }
                        TextButton(enabled = !busy, onClick = {
                            avatarDraftClear = true
                            avatarDraftKey = null
                            avatarDraftPhoto = null
                        }) {
                            Text(memberText(language, "Скинути аватар", "Avatar zurücksetzen", "Reset avatar", "Сбросить аватар"))
                        }
                        if (avatarDraftKey != null || avatarDraftPhoto != null || avatarDraftClear) {
                            Text(memberText(language, "Аватар зміниться після збереження", "Avatar ändert sich nach Speichern",
                                "Avatar changes after Save", "Аватар изменится после сохранения"),
                                style = MaterialTheme.typography.labelSmall)
                        }
                        if (avatarEditorError.isNotBlank()) {
                            Text(avatarEditorError, color = MaterialTheme.colorScheme.error)
                        }
                    }
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
                    Text(memberText(language, "Доступ учасника", "Mitgliedszugang", "Member access", "Доступ участника"),
                        style = MaterialTheme.typography.titleSmall)
                    val hasAccount = editing?.accessStatus in listOf("ACTIVE", "DISABLED")
                    val invitePending = editing?.accessStatus == "PENDING_INVITATION"
                    if (hasAccount) {
                        Text(memberText(language,
                            "Акаунт уже прив'язано. Створення другого логіна або повторне запрошення недоступні.",
                            "Konto bereits verknüpft. Neues Login oder Einladung nicht verfügbar.",
                            "Account already linked. New login or invitation unavailable.",
                            "Аккаунт уже привязан. Новый логин или приглашение недоступны."),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (invitePending) {
                        Text(memberText(language,
                            "Запрошення очікує підтвердження. Повторити можна через 2 хвилини; старе посилання стане недійсним.",
                            "Einladung ausstehend. Nach 2 Minuten erneut senden; alter Link wird ungültig.",
                            "Invitation pending. Resend after 2 minutes; previous link will be revoked.",
                            "Приглашение ожидает подтверждения. Повторить через 2 минуты; старая ссылка станет недействительной."),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    listOf(
                        "profile" to memberText(language, "Лише профіль / без змін доступу", "Nur Profil / kein Zugang", "Profile only / no access change", "Только профиль / без изменений"),
                        "account" to memberText(language, "Створити окремий логін", "Eigenes Login erstellen", "Create separate login", "Создать отдельный логин"),
                        "invite" to if (invitePending) memberText(language,
                            "Надіслати запрошення повторно", "Einladung erneut senden", "Resend invitation", "Отправить приглашение повторно")
                            else memberText(language, "Запросити через email", "Per E-Mail einladen", "Invite by email", "Пригласить по email")
                    ).filter { !hasAccount || it.first == "profile" }.forEach { (option, title) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { accessMode = option },
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(selected = accessMode == option, onClick = { accessMode = option })
                            Text(title, modifier = Modifier.weight(1f))
                        }
                    }
                    if (accessMode == "account") {
                        OutlinedTextField(value = childLogin, onValueChange = { childLogin = it },
                            label = { Text(memberText(language, "Логін", "Benutzername", "Username", "Логин")) },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = childPassword, onValueChange = { childPassword = it },
                            label = { Text(memberText(language, "Пароль", "Passwort", "Password", "Пароль")) },
                            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = confirmChildPassword, onValueChange = { confirmChildPassword = it },
                            label = { Text(memberText(language, "Повторіть пароль", "Passwort wiederholen", "Confirm password", "Повторите пароль")) },
                            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    }
                    if (accessMode == "invite") {
                        OutlinedTextField(value = invitationEmail, onValueChange = { invitationEmail = it },
                            label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Text(memberText(language,
                            "Запрошення працює тільки якщо цю функцію ввімкнено на сервері.",
                            "Einladung nur bei aktivierter Serverfunktion.",
                            "Invitations require the server feature to be enabled.",
                            "Приглашения требуют включения функции на сервере."),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (editing != null) TextButton(enabled = !busy, onClick = { confirmDelete = true }) {
                        Text(memberText(language, "Видалити", "Löschen", "Delete", "Удалить"),
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(enabled = !busy && name.isNotBlank() && roleId != null &&
                    (editing == null || (avatarDraftKey == null && avatarDraftPhoto == null && !avatarDraftClear) || avatarVersion != null) &&
                    (accessMode != "account" || (childLogin.matches(Regex("[a-zA-Z0-9._-]{3,64}")) && childPassword.length in 8..128 && childPassword == confirmChildPassword)) &&
                    (accessMode != "invite" || android.util.Patterns.EMAIL_ADDRESS.matcher(invitationEmail).matches()), onClick = {
                    val request = MemberSaveRequest(name.trim(), type, roleId!!)
                    val id = editing?.id
                    mutate {
                        repository.save(currentUser.workspaceId, id, request, accessMode, childLogin.trim(), childPassword, invitationEmail.trim())
                        if (id != null && (avatarDraftClear || avatarDraftKey != null || avatarDraftPhoto != null)) {
                            val auth = bearer ?: throw IllegalStateException("Sign in online to save avatar")
                            val version = avatarVersion ?: throw IllegalStateException("Avatar version unavailable")
                            if (avatarDraftClear) {
                                ApiClient.mediaApi.clearMemberAvatar(auth, currentUser.workspaceId, id, version)
                            } else {
                                val photo = avatarDraftPhoto
                                val asset = if (photo != null) {
                                    val resolver = context.contentResolver
                                    val mime = resolver.getType(photo)
                                    require(mime == "image/jpeg" || mime == "image/png") { "JPEG/PNG only" }
                                    val bytes = resolver.openInputStream(photo)?.use { input ->
                                        val stream = java.io.ByteArrayOutputStream()
                                        val chunk = ByteArray(8192)
                                        while (true) {
                                            val read = input.read(chunk)
                                            if (read < 0) break
                                            require(stream.size() + read <= 5 * 1024 * 1024) { "Max 5 MiB" }
                                            stream.write(chunk, 0, read)
                                        }
                                        stream.toByteArray()
                                    } ?: throw IllegalStateException("Cannot read image")
                                    ApiClient.mediaApi.uploadMemberImage(auth, currentUser.workspaceId,
                                        MultipartBody.Part.createFormData("file", "avatar.jpg",
                                            bytes.toRequestBody(mime.toMediaType()))).assetId
                                } else null
                                ApiClient.mediaApi.bindMemberAvatar(auth, currentUser.workspaceId, id,
                                    MediaBindRequest(asset, avatarDraftKey, version))
                            }
                            val latest = ApiClient.mediaApi.memberAvatar(auth, currentUser.workspaceId, id)
                            avatarRefs = avatarRefs + (id to latest)
                        }
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
                    "${it.amount} ${memberGroupCurrencyLabel(language, it.code, it.name)}"
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
                                Text(memberGroupCurrencyLabel(language, balance.code, balance.name),
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
                            Text(memberGroupRoleLabel(language, role), modifier = Modifier.weight(1f))
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
            title = { Text(memberGroupRoleLabel(language, role)) },
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
                                memberGroupCurrencyLabel(language, balance.code, balance.name) +
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

private fun memberGroupCurrencyLabel(language: AppLanguage, code: String?, fallback: String?): String =
    when (code?.uppercase()) {
        "POINTS" -> memberText(language, "Бали", "Punkte", "Points", "Баллы")
        "COPPER" -> memberText(language, "Мідь", "Kupfer", "Copper", "Медь")
        "SILVER" -> memberText(language, "Срібло", "Silber", "Silver", "Серебро")
        "GOLD" -> memberText(language, "Золото", "Gold", "Gold", "Золото")
        else -> fallback?.takeIf { it.isNotBlank() } ?: code ?: "—"
    }

private fun memberGroupRoleLabel(language: AppLanguage, role: GroupRoleResponse): String =
    when (role.systemCode?.uppercase()) {
        "LEADER" -> memberText(language, "Керівник", "Leiter", "Leader", "Руководитель")
        "DEPUTY" -> memberText(language, "Заступник", "Stellvertreter", "Deputy", "Заместитель")
        "SENIOR" -> memberText(language, "Старший", "Senior", "Senior", "Старший")
        "EXECUTOR" -> memberText(language, "Виконавець", "Ausführender", "Executor", "Исполнитель")
        "ASSISTANT" -> memberText(language, "Помічник", "Assistent", "Assistant", "Помощник")
        "REVIEWER" -> memberText(language, "Перевіряльник", "Prüfer", "Reviewer", "Проверяющий")
        "OBSERVER" -> memberText(language, "Спостерігач", "Beobachter", "Observer", "Наблюдатель")
        else -> role.name
    }
