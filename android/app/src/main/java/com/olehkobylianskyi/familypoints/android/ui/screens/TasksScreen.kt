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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.TaskDefinitionResponse
import com.olehkobylianskyi.familypoints.android.data.TaskInstanceResponse
import com.olehkobylianskyi.familypoints.android.data.TaskParticipantResponse
import com.olehkobylianskyi.familypoints.android.data.TasksRepository
import com.olehkobylianskyi.familypoints.android.data.WorkspaceMemberResponse
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.taskStatusLabel
import com.olehkobylianskyi.familypoints.android.i18n.tasksStrings
import java.time.LocalDate
import kotlinx.coroutines.launch

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
    onBack: () -> Unit,
    onOpenTask: (Long, String) -> Unit,
    onCreateTask: () -> Unit,
    onUnauthorized: () -> Unit
) {
    val text = tasksStrings(language)
    val context = LocalContext.current

    var members by remember { mutableStateOf<List<WorkspaceMemberResponse>>(emptyList()) }
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }

    var tasks by remember { mutableStateOf<List<TaskInstanceResponse>>(emptyList()) }
    var openTasks by remember { mutableStateOf<List<TaskDefinitionResponse>>(emptyList()) }
    var definitions by remember { mutableStateOf<List<TaskDefinitionResponse>>(emptyList()) }

    var tab by remember { mutableStateOf(TasksPageTab.MY) }
    var managementView by remember { mutableStateOf(ManagementView.CREATED) }
    var managedTasks by remember { mutableStateOf<List<TaskDefinitionResponse>>(emptyList()) }
    var participantMap by remember {
        mutableStateOf<Map<Long, List<TaskParticipantResponse>>>(emptyMap())
    }

    var loadingMembers by remember { mutableStateOf(true) }
    var loadingTasks by remember { mutableStateOf(false) }
    var loadingManagement by remember { mutableStateOf(false) }
    var processingId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf("") }

    suspend fun loadMyTasks() {
        val memberId = selectedMemberId ?: return
        loadingTasks = true
        error = ""

        try {
            val (loadedTasks, loadedOpen, loadedDefinitions) =
                repository.loadMyTab(
                    currentUser.workspaceId,
                    memberId,
                    selectedDate
                )

            tasks = loadedTasks
            openTasks = loadedOpen
            definitions = loadedDefinitions
        } catch (exception: retrofit2.HttpException) {
            if (exception.code() == 401) {
                onUnauthorized()
            } else {
                error = exception.message()
            }
        } catch (exception: Exception) {
            error = exception.message ?: ""
        } finally {
            loadingTasks = false
        }
    }

    suspend fun loadManagement() {
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

    LaunchedEffect(currentUser.workspaceId) {
        loadingMembers = true
        try {
            members = repository.loadMembers(currentUser.workspaceId)

            selectedMemberId =
                members.firstOrNull { it.id == currentUser.memberId }?.id
                    ?: members.firstOrNull()?.id
        } catch (exception: retrofit2.HttpException) {
            if (exception.code() == 401) {
                onUnauthorized()
            } else {
                error = exception.message()
            }
        } catch (exception: Exception) {
            error = exception.message ?: ""
        } finally {
            loadingMembers = false
        }
    }

    LaunchedEffect(selectedMemberId, selectedDate) {
        if (selectedMemberId != null) {
            loadMyTasks()
        }
    }

    LaunchedEffect(tab, managementView) {
        if (tab == TasksPageTab.MANAGEMENT) {
            loadManagement()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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

                Button(onClick = onCreateTask) {
                    Text("+ " + text.newTask)
                }
            }
        }

        item {
            Text(
                text = text.title,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(text.description)
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
            item {
                OutlinedButton(
                    onClick = {
                        val currentDate = try {
                            LocalDate.parse(selectedDate)
                        } catch (_: Exception) {
                            LocalDate.now()
                        }

                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                selectedDate = LocalDate.of(
                                    year,
                                    month + 1,
                                    dayOfMonth
                                ).toString()
                            },
                            currentDate.year,
                            currentDate.monthValue - 1,
                            currentDate.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📅  " + selectedDate)
                }
            }

            item {
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

            if (loadingMembers || loadingTasks) {
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
                        processing = processingId == task.id,
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
                        processing = processingId == -definition.id,
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ManagementView.entries.forEach { view ->
                        val label = when (view) {
                            ManagementView.CREATED -> text.created
                            ManagementView.ADMIN -> text.admin
                            ManagementView.OBSERVER -> text.observer
                            ManagementView.EXECUTOR -> text.executorView
                        }

                        if (managementView == view) {
                            Button(onClick = {}) {
                                Text(label)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { managementView = view }
                            ) {
                                Text(label)
                            }
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

