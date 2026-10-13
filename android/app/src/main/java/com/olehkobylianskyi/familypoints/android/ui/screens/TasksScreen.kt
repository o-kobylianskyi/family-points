package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.TaskDefinitionResponse
import com.olehkobylianskyi.familypoints.android.data.TaskScheduleOccurrenceResponse
import com.olehkobylianskyi.familypoints.android.data.TaskInstanceResponse
import com.olehkobylianskyi.familypoints.android.data.TaskParticipantResponse
import com.olehkobylianskyi.familypoints.android.data.TasksRepository
import com.olehkobylianskyi.familypoints.android.data.WorkspaceMemberResponse
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.taskStatusLabel
import com.olehkobylianskyi.familypoints.android.i18n.tasksStrings
import com.olehkobylianskyi.familypoints.android.ui.components.AppHeader
import com.olehkobylianskyi.familypoints.android.network.ServerReachability
import com.olehkobylianskyi.familypoints.android.network.ServerStatus
import com.olehkobylianskyi.familypoints.android.ui.components.LocalizedCalendarDialog
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private enum class TasksPageTab {
    MY,
    MANAGEMENT
}

private enum class ManagementView(val apiValue: String) {
    CREATED("created"),
    ADMIN("admin"),
    OBSERVER("observer"),
    EXECUTOR("executor")
}

@Composable
fun TasksScreen(
    language: AppLanguage,
    currentUser: CurrentUserResponse,
    repository: TasksRepository,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (DashboardDestination) -> Unit,
    onBack: () -> Unit,
    onOpenTask: (Long, String) -> Unit,
    onCreateTask: () -> Unit,
    onUnauthorized: () -> Unit
) {
    val text = tasksStrings(language)
    val scope = rememberCoroutineScope()
    val serverStatus by ServerReachability.status.collectAsState()
    var showingCachedMy by remember { mutableStateOf(false) }
    var members by remember { mutableStateOf<List<WorkspaceMemberResponse>>(emptyList()) }
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var calendarOpen by remember { mutableStateOf(false) }
    var calendarMonth by remember {
        mutableStateOf(YearMonth.from(LocalDate.now()))
    }

    var tasks by remember { mutableStateOf<List<TaskInstanceResponse>>(emptyList()) }
    var scheduledOccurrences by remember { mutableStateOf<List<TaskScheduleOccurrenceResponse>>(emptyList()) }
    var openTasks by remember { mutableStateOf<List<TaskDefinitionResponse>>(emptyList()) }
    var definitions by remember { mutableStateOf<List<TaskDefinitionResponse>>(emptyList()) }

    var tab by remember { mutableStateOf(TasksPageTab.MY) }
    var managementView by remember { mutableStateOf(ManagementView.CREATED) }
    var managementMenuOpen by remember { mutableStateOf(false) }
    var managedTasks by remember { mutableStateOf<List<TaskDefinitionResponse>>(emptyList()) }
    var participantMap by remember {
        mutableStateOf<Map<Long, List<TaskParticipantResponse>>>(emptyMap())
    }

    var loadingMembers by remember { mutableStateOf(true) }
    var loadingTasks by remember { mutableStateOf(false) }
    var loadingManagement by remember { mutableStateOf(false) }
    var processingId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf("") }
    val mayBrowseMembers = currentUser.permissions.contains("VIEW_ALL_TASKS") || currentUser.permissions.contains("ADMIN_OVERRIDE")
    val mayCreateTasks = currentUser.permissions.contains("CREATE_TASKS") || currentUser.permissions.contains("ADMIN_OVERRIDE")

    suspend fun loadMyTasks() {
        val memberId = selectedMemberId ?: return
        error = ""
        scheduledOccurrences = emptyList()
        val cached = repository.cachedMyTab(currentUser.workspaceId, currentUser.memberId, memberId, selectedDate)
        showingCachedMy = cached != null
        if (cached != null) {
            tasks = cached.tasks
            openTasks = cached.openTasks
            definitions = cached.definitions
        } else {
            tasks = emptyList()
            openTasks = emptyList()
            definitions = emptyList()
        }
        loadingTasks = cached == null
        if (serverStatus == ServerStatus.OFFLINE) {
            loadingTasks = false
            if (cached == null) {
                error = when (language) {
                    AppLanguage.UK -> "Для цієї дати завдання ще не збережені на телефоні."
                    AppLanguage.DE -> "Für dieses Datum sind noch keine Aufgaben gespeichert."
                    AppLanguage.EN -> "Tasks for this date have not been saved on this phone."
                    AppLanguage.RU -> "Задания на эту дату ещё не сохранены на телефоне."
                }
            }
            return
        }

        try {
            val (loadedTasks, loadedOpen, loadedDefinitions) = repository.loadMyTab(
                currentUser.workspaceId, currentUser.memberId, memberId, selectedDate
            )
            tasks = loadedTasks
            openTasks = loadedOpen
            definitions = loadedDefinitions
            showingCachedMy = false
            // Projection is read-only; an absent/offline schedule must not hide persisted tasks.
            scheduledOccurrences = try {
                repository.loadScheduledOccurrences(currentUser.workspaceId, memberId, selectedDate)
                    .filter { it.instanceId == null }
            } catch (exception: retrofit2.HttpException) {
                if (exception.code() == 401) onUnauthorized()
                emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        } catch (exception: retrofit2.HttpException) {
            if (exception.code() == 401) onUnauthorized()
            else if (cached == null) error = exception.message()
        } catch (exception: Exception) {
            if (cached == null) error = exception.message ?: ""
        } finally {
            loadingTasks = false
        }
    }

    suspend fun loadManagement() {
        if (serverStatus == ServerStatus.OFFLINE) return
        loadingManagement = true
        error = ""

        try {
            val (loadedTasks, participants) =
                repository.loadManagement(
                    currentUser.workspaceId,
                    managementView.apiValue
                )

            managedTasks = loadedTasks
            participantMap = participants
        } catch (exception: retrofit2.HttpException) {
            if (exception.code() == 401) {
                onUnauthorized()
            } else {
                error = exception.message()
            }
        } catch (exception: Exception) {
            error = exception.message ?: ""
        } finally {
            loadingManagement = false
        }
    }

    LaunchedEffect(currentUser.workspaceId, serverStatus) {
        members = listOf(WorkspaceMemberResponse(
            id = currentUser.memberId,
            name = currentUser.memberName,
            memberType = currentUser.memberType,
            workspaceRoleId = currentUser.workspaceRoleId,
            workspaceRoleCode = currentUser.workspaceRoleCode,
            workspaceRoleName = currentUser.workspaceRoleName,
            workspaceId = currentUser.workspaceId
        ))
        if (selectedMemberId == null || !mayBrowseMembers) selectedMemberId = currentUser.memberId
        loadingMembers = false
        if (serverStatus != ServerStatus.OFFLINE) {
            try {
                members = if (mayBrowseMembers) repository.loadMembers(currentUser.workspaceId)
                    else members.filter { it.id == currentUser.memberId }
                if (!mayBrowseMembers) selectedMemberId = currentUser.memberId
            } catch (exception: retrofit2.HttpException) {
                if (exception.code() == 401) onUnauthorized()
            } catch (_: Exception) {
                // The current member and cached tasks remain available.
            }
        }
    }

    LaunchedEffect(selectedMemberId, selectedDate, serverStatus) {
        if (selectedMemberId != null) {
            loadMyTasks()
        }
    }

    LaunchedEffect(tab, managementView, serverStatus) {
        if (tab == TasksPageTab.MANAGEMENT && serverStatus == ServerStatus.ONLINE) {
            loadManagement()
        }
    }

    // When the Web editor adds/updates tasks, refresh the visible Android list.
    LaunchedEffect(tab, selectedMemberId, selectedDate, serverStatus) {
        if (tab == TasksPageTab.MY && serverStatus == ServerStatus.ONLINE) {
            while (true) {
                delay(30_000L)
                if (!loadingTasks && processingId == null) loadMyTasks()
            }
        }
    }

    if (calendarOpen) {
        LocalizedCalendarDialog(
            language = language,
            selectedDate = try {
                LocalDate.parse(selectedDate)
            } catch (_: Exception) {
                LocalDate.now()
            },
            visibleMonth = calendarMonth,
            onPreviousMonth = {
                calendarMonth = calendarMonth.minusMonths(1)
            },
            onNextMonth = {
                calendarMonth = calendarMonth.plusMonths(1)
            },
            onDateSelected = { date ->
                selectedDate = date.toString()
                calendarOpen = false
            },
            onDismiss = {
                calendarOpen = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AppHeader(
                language = language,
                currentUser = currentUser,
                onLanguageChange = onLanguageChange,
                onLogout = onLogout,
                onNavigate = onNavigate
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = onBack) {
                    Text("← " + text.back)
                }

                if (mayCreateTasks) {
                    Button(onClick = onCreateTask, enabled = serverStatus == ServerStatus.ONLINE) {
                        Text("+ " + text.newTask)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text.title, style = MaterialTheme.typography.headlineMedium)
                    Text(text.description)
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            if (tab == TasksPageTab.MY) loadMyTasks() else loadManagement()
                        }
                    },
                    enabled = serverStatus == ServerStatus.ONLINE &&
                        !loadingTasks && !loadingManagement && processingId == null
                ) {
                    Text(when (language) {
                        AppLanguage.UK -> "⟳ Оновити"
                        AppLanguage.DE -> "⟳ Neu laden"
                        AppLanguage.EN -> "⟳ Refresh"
                        AppLanguage.RU -> "⟳ Обновить"
                    })
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (tab == TasksPageTab.MY) {
                    Button(onClick = {}) {
                        Text(text.myTasksTab)
                    }
                } else {
                    OutlinedButton(onClick = { tab = TasksPageTab.MY }) {
                        Text(text.myTasksTab)
                    }
                }

                if (tab == TasksPageTab.MANAGEMENT) {
                    Button(onClick = {}) {
                        Text(text.managementTab)
                    }
                } else {
                    OutlinedButton(
                        onClick = { tab = TasksPageTab.MANAGEMENT }
                    ) {
                        Text(text.managementTab)
                    }
                }
            }
        }

        if (error.isNotBlank()) {
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (tab == TasksPageTab.MY) {
            if (showingCachedMy) {
                item {
                    Text(when (language) {
                        AppLanguage.UK -> "⟳ Збережені завдання · лише перегляд"
                        AppLanguage.DE -> "⟳ Gespeicherte Aufgaben · nur lesen"
                        AppLanguage.EN -> "⟳ Cached tasks · read only"
                        AppLanguage.RU -> "⟳ Сохранённые задания · только просмотр"
                    }, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val currentDate = try {
                                LocalDate.parse(selectedDate)
                            } catch (_: Exception) {
                                LocalDate.now()
                            }
                            selectedDate = currentDate.minusDays(1).toString()
                        }
                    ) {
                        Text("‹")
                    }

                    OutlinedButton(
                        onClick = {
                            val currentDate = try {
                                LocalDate.parse(selectedDate)
                            } catch (_: Exception) {
                                LocalDate.now()
                            }
                            calendarMonth = YearMonth.from(currentDate)
                            calendarOpen = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📅  " + selectedDate)
                    }

                    OutlinedButton(
                        onClick = {
                            val currentDate = try {
                                LocalDate.parse(selectedDate)
                            } catch (_: Exception) {
                                LocalDate.now()
                            }
                            selectedDate = currentDate.plusDays(1).toString()
                        }
                    ) {
                        Text("›")
                    }
                }
            }

            if (mayBrowseMembers) item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    members.forEach { member ->
                        if (selectedMemberId == member.id) {
                            Button(onClick = {}) {
                                Text(member.name)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    selectedMemberId = member.id
                                }
                            ) {
                                Text(member.name)
                            }
                        }
                    }
                }
            }

            if (scheduledOccurrences.isNotEmpty()) {
                item {
                    Text(
                        when (language) {
                            AppLanguage.UK -> "Заплановані виконання"
                            AppLanguage.DE -> "Geplante Ausführungen"
                            AppLanguage.EN -> "Scheduled occurrences"
                            AppLanguage.RU -> "Запланированные выполнения"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                items(scheduledOccurrences, key = { "scheduled-" + it.occurrenceKey }) { occurrence ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(occurrence.title, style = MaterialTheme.typography.titleMedium)
                            Text(occurrence.date + " · " + (occurrence.time?.take(5) ?: "—"))
                            Text(when (language) {
                                AppLanguage.UK -> "Заплановано"
                                AppLanguage.DE -> "Geplant"
                                AppLanguage.EN -> "Planned"
                                AppLanguage.RU -> "Запланировано"
                            })
                            if (selectedMemberId == currentUser.memberId &&
                                selectedDate <= LocalDate.now().toString() &&
                                serverStatus == ServerStatus.ONLINE && !showingCachedMy) {
                                Button(enabled = processingId == null, onClick = {
                                    scope.launch {
                                        processingId = -occurrence.definitionId
                                        error = ""
                                        try {
                                            repository.startScheduledOccurrence(
                                                currentUser.workspaceId, occurrence.definitionId,
                                                selectedDate, occurrence.slotKey
                                            )
                                            loadMyTasks()
                                        } catch (exception: retrofit2.HttpException) {
                                            if (exception.code() == 401) onUnauthorized()
                                            else error = exception.message()
                                        } catch (exception: Exception) {
                                            error = exception.message ?: ""
                                        } finally { processingId = null }
                                    }
                                }) {
                                    Text(when (language) {
                                        AppLanguage.UK -> "Почати"
                                        AppLanguage.DE -> "Starten"
                                        AppLanguage.EN -> "Start"
                                        AppLanguage.RU -> "Начать"
                                    })
                                }
                            }
                        }
                    }
                }
            }

            if ((loadingMembers || loadingTasks) && tasks.isEmpty() && openTasks.isEmpty()) {
                item {
                    Text(text.loading)
                }
            } else if (tasks.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = text.noTasks,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = text.noTasksDescription,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(tasks, key = { it.id }) { task ->
                    TaskInstanceCard(
                        language = language,
                        task = task,
                        members = members,
                        processing = processingId == task.id || showingCachedMy || serverStatus != ServerStatus.ONLINE,
                        onAction = { action ->
                            processingId = task.id
                            error = ""
                            try {
                                val updated = repository.performAction(
                                    currentUser.workspaceId,
                                    task.id,
                                    action
                                )
                                tasks = tasks.map {
                                    if (it.id == updated.id) updated else it
                                }
                            } catch (exception: retrofit2.HttpException) {
                                if (exception.code() == 401) {
                                    onUnauthorized()
                                } else {
                                    error = exception.message()
                                }
                            } catch (exception: Exception) {
                                error = exception.message ?: ""
                            } finally {
                                processingId = null
                            }
                        },
                        onOpen = {
                            if (showingCachedMy || serverStatus != ServerStatus.ONLINE) return@TaskInstanceCard
                            onOpenTask(
                                task.taskDefinitionId,
                                selectedDate
                            )
                        }
                    )
                }
            }

            item {
                Text(
                    text = text.openTitle,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (!loadingTasks && openTasks.isEmpty()) {
                item {
                    Text(text.openEmpty)
                }
            } else {
                items(
                    openTasks.sortedByDescending { it.id },
                    key = { "open-${it.id}" }
                ) { definition ->
                    OpenTaskCard(
                        language = language,
                        definition = definition,
                        members = members,
                        processing = processingId == -definition.id || showingCachedMy || serverStatus != ServerStatus.ONLINE,
                        onClaim = {
                            val memberId = selectedMemberId ?: return@OpenTaskCard
                            processingId = -definition.id
                            error = ""
                            try {
                                repository.claim(
                                    currentUser.workspaceId,
                                    definition.id,
                                    selectedDate,
                                    memberId
                                )
                                loadMyTasks()
                            } catch (exception: retrofit2.HttpException) {
                                if (exception.code() == 401) {
                                    onUnauthorized()
                                } else {
                                    error = exception.message()
                                }
                            } catch (exception: Exception) {
                                error = exception.message ?: ""
                            } finally {
                                processingId = null
                            }
                        }
                    )
                }
            }
        } else {
            item {
                val currentViewLabel = when (managementView) {
                    ManagementView.CREATED -> text.created
                    ManagementView.ADMIN -> text.admin
                    ManagementView.OBSERVER -> text.observer
                    ManagementView.EXECUTOR -> text.executorView
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            managementMenuOpen = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(currentViewLabel + " ▾")
                    }

                    DropdownMenu(
                        expanded = managementMenuOpen,
                        onDismissRequest = {
                            managementMenuOpen = false
                        }
                    ) {
                        ManagementView.entries.forEach { view ->
                            val label = when (view) {
                                ManagementView.CREATED -> text.created
                                ManagementView.ADMIN -> text.admin
                                ManagementView.OBSERVER -> text.observer
                                ManagementView.EXECUTOR -> text.executorView
                            }

                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    managementView = view
                                    managementMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            if (loadingManagement) {
                item {
                    Text(text.loading)
                }
            } else if (managedTasks.isEmpty()) {
                item {
                    Text(text.managementEmpty)
                }
            } else {
                items(managedTasks, key = { "managed-${it.id}" }) { task ->
                    ManagedTaskCard(
                        language = language,
                        task = task,
                        participants = participantMap[task.id].orEmpty(),
                        onOpen = {
                            onOpenTask(task.id, selectedDate)
                        }
                    )
                }
            }
        }

        item {
            Text(
                text = "",
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun TaskInstanceCard(
    language: AppLanguage,
    task: TaskInstanceResponse,
    members: List<WorkspaceMemberResponse>,
    processing: Boolean,
    onAction: suspend (String) -> Unit,
    onOpen: () -> Unit
) {
    val text = tasksStrings(language)
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "№${task.taskDefinitionId} · ${task.title}",
                style = MaterialTheme.typography.titleMedium
            )

            if (task.mandatory) {
                Text(text.mandatory)
            }

            if (!task.description.isNullOrBlank()) {
                Text(task.description)
            }

            task.dueTime?.let {
                Text("${text.dueTime}: ${it.take(5)}")
            }

            if ((task.rewardAmount ?: 0) > 0) {
                Text(
                    "${text.reward}: +${task.rewardAmount} " +
                        (task.rewardPointTypeCode ?: "")
                )
            }

            if ((task.rewardReputationAmount ?: 0) > 0) {
                Text(
                    "${text.reputation}: +${task.rewardReputationAmount}"
                )
            }

            if ((task.penaltyAmount ?: 0) > 0) {
                Text(
                    "${text.penalty}: -${task.penaltyAmount} " +
                        (task.penaltyPointTypeCode ?: "")
                )
            }

            if ((task.penaltyReputationAmount ?: 0) > 0) {
                Text(
                    "${text.reputation}: -${task.penaltyReputationAmount}"
                )
            }

            val executorName =
                members.firstOrNull { it.id == task.memberId }?.name
                    ?: "#${task.memberId}"

            Text("${text.executor}: $executorName")
            Text(taskStatusLabel(language, task.status))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (task.status) {
                    "PENDING" -> Button(
                        onClick = { scope.launch { onAction("start") } },
                        enabled = !processing
                    ) {
                        Text(text.start)
                    }

                    "IN_PROGRESS" -> Button(
                        onClick = { scope.launch { onAction("complete") } },
                        enabled = !processing
                    ) {
                        Text(text.complete)
                    }

                    "PAUSED" -> Button(
                        onClick = { scope.launch { onAction("resume") } },
                        enabled = !processing
                    ) {
                        Text(text.resume)
                    }
                }

                OutlinedButton(onClick = onOpen) {
                    Text(text.open)
                }
            }
        }
    }
}

@Composable
private fun OpenTaskCard(
    language: AppLanguage,
    definition: TaskDefinitionResponse,
    members: List<WorkspaceMemberResponse>,
    processing: Boolean,
    onClaim: suspend () -> Unit
) {
    val text = tasksStrings(language)
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = definition.title,
                style = MaterialTheme.typography.titleMedium
            )

            if (definition.mandatory) {
                Text(text.mandatory)
            }

            if (!definition.description.isNullOrBlank()) {
                Text(definition.description)
            }

            definition.dueTime?.let {
                Text("${text.dueTime}: ${it.take(5)}")
            }

            if ((definition.rewardAmount ?: 0) > 0) {
                Text(
                    "${text.reward}: +${definition.rewardAmount} " +
                        (definition.rewardPointTypeCode ?: "")
                )
            }

            definition.preferredMemberId?.let { preferredId ->
                val name =
                    members.firstOrNull { it.id == preferredId }?.name
                        ?: "#$preferredId"
                Text("★ $name")
            }

            Button(
                onClick = { scope.launch { onClaim() } },
                enabled = !processing
            ) {
                Text(text.claim)
            }
        }
    }
}

@Composable
private fun ManagedTaskCard(
    language: AppLanguage,
    task: TaskDefinitionResponse,
    participants: List<TaskParticipantResponse>,
    onOpen: () -> Unit
) {
    val text = tasksStrings(language)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "№${task.id} · ${task.title}",
                style = MaterialTheme.typography.titleMedium
            )

            if (!task.description.isNullOrBlank()) {
                Text(task.description)
            }

            fun names(role: String): String =
                participants
                    .filter { it.role == role }
                    .joinToString(", ") { it.actorName }

            names("ADMIN").takeIf { it.isNotBlank() }?.let {
                Text("${text.administrators}: $it")
            }

            names("OBSERVER").takeIf { it.isNotBlank() }?.let {
                Text("${text.observers}: $it")
            }

            names("EXECUTOR").takeIf { it.isNotBlank() }?.let {
                Text("${text.executors}: $it")
            }

            Text(
                if (task.active) text.active else text.inactive
            )

            OutlinedButton(onClick = onOpen) {
                Text(text.open)
            }
        }
    }
}



