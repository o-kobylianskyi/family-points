package com.olehkobylianskyi.familypoints.android.ui.screens

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
import com.olehkobylianskyi.familypoints.android.data.TaskDetailsData
import com.olehkobylianskyi.familypoints.android.data.TaskDetailsRepository
import com.olehkobylianskyi.familypoints.android.data.WorkspaceMemberResponse
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.formatPointAmount
import com.olehkobylianskyi.familypoints.android.i18n.taskDetailsStrings
import com.olehkobylianskyi.familypoints.android.i18n.taskStatusLabel
import com.olehkobylianskyi.familypoints.android.ui.components.AppHeader
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun TaskDetailsScreen(
    language: AppLanguage,
    currentUser: CurrentUserResponse,
    definitionId: Long,
    selectedDate: String,
    repository: TaskDetailsRepository,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (DashboardDestination) -> Unit,
    onBack: () -> Unit,
    onOpenTask: (Long, String) -> Unit,
    onCreateSubtask: (Long) -> Unit,
    onRequestReward: (Long) -> Unit,
    onUnauthorized: () -> Unit
) {
    val text = taskDetailsStrings(language)
    val scope = rememberCoroutineScope()

    var data by remember { mutableStateOf<TaskDetailsData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var processing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var delegating by remember { mutableStateOf(false) }
    var delegateToMemberId by remember { mutableStateOf<Long?>(null) }
    var delegateMenuOpen by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true
        error = ""
        try {
            data = repository.load(
                currentUser.workspaceId,
                definitionId,
                selectedDate
            )
        } catch (exception: HttpException) {
            if (exception.code() == 401) onUnauthorized()
            else error = exception.message()
        } catch (exception: Exception) {
            error = exception.message ?: ""
        } finally {
            loading = false
        }
    }

    suspend fun runAction(action: String) {
        val instance = data?.instance ?: return
        processing = true
        error = ""
        try {
            repository.runInstanceAction(
                currentUser.workspaceId,
                instance.id,
                action
            )
            load()
        } catch (exception: HttpException) {
            if (exception.code() == 401) onUnauthorized()
            else error = exception.message()
        } catch (exception: Exception) {
            error = exception.message ?: ""
        } finally {
            processing = false
        }
    }

    LaunchedEffect(definitionId, selectedDate) {
        load()
    }

    if (confirmAction != null) {
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = {
                Text(
                    if (confirmAction == "cancel") text.cancel
                    else text.release
                )
            },
            text = {
                Text(
                    (if (confirmAction == "cancel") text.cancel else text.release) + "?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val action = confirmAction
                        confirmAction = null
                        if (action != null) scope.launch { runAction(action) }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) {
                    Text("×")
                }
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
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("← " + text.back)
            }
        }

        if (loading && data == null) {
            item { Text(text.loading) }
        }

        if (error.isNotBlank()) {
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        val details = data
        if (details != null) {
            val task = details.task
            val instance = details.instance

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text.task + " №" + task.id,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            task.title,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        if (!task.description.isNullOrBlank()) {
                            Text(task.description)
                        }
                        Text(if (task.active) text.active else text.inactive)
                    }
                }
            }

            if (instance != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text.execution + " · " + selectedDate,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text.executor + ": " +
                                    memberName(details.members, instance.memberId)
                            )
                            Text(
                                text.status + ": " +
                                    taskStatusLabel(language, instance.status)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                when (instance.status) {
                                    "PENDING" -> Button(
                                        onClick = {
                                            scope.launch { runAction("start") }
                                        },
                                        enabled = !processing
                                    ) {
                                        Text(text.start)
                                    }

                                    "IN_PROGRESS" -> {
                                        Button(
                                            onClick = {
                                                scope.launch { runAction("complete") }
                                            },
                                            enabled = !processing
                                        ) {
                                            Text(text.complete)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                scope.launch { runAction("pause") }
                                            },
                                            enabled = !processing
                                        ) {
                                            Text(text.pause)
                                        }
                                    }

                                    "PAUSED" -> Button(
                                        onClick = {
                                            scope.launch { runAction("resume") }
                                        },
                                        enabled = !processing
                                    ) {
                                        Text(text.resume)
                                    }
                                }
                            }

                            if (
                                instance.memberId == currentUser.memberId &&
                                instance.status in listOf(
                                    "PENDING",
                                    "IN_PROGRESS",
                                    "PAUSED"
                                )
                            ) {
                                OutlinedButton(
                                    onClick = { onRequestReward(instance.id) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text.requestReward)
                                }
                            }

                            if (
                                instance.delegationAllowed &&
                                instance.status in listOf(
                                    "PENDING",
                                    "IN_PROGRESS",
                                    "PAUSED"
                                )
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        delegating = !delegating
                                        if (delegateToMemberId == null) {
                                            delegateToMemberId =
                                                details.members.firstOrNull {
                                                    it.id != instance.memberId
                                                }?.id
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text.delegate)
                                }
                            }

                            if (delegating) {
                                val target = details.members
                                    .firstOrNull { it.id == delegateToMemberId }

                                OutlinedButton(
                                    onClick = { delegateMenuOpen = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(target?.name ?: "—")
                                }

                                DropdownMenu(
                                    expanded = delegateMenuOpen,
                                    onDismissRequest = {
                                        delegateMenuOpen = false
                                    }
                                ) {
                                    details.members
                                        .filter { it.id != instance.memberId }
                                        .forEach { member ->
                                            DropdownMenuItem(
                                                text = { Text(member.name) },
                                                onClick = {
                                                    delegateToMemberId = member.id
                                                    delegateMenuOpen = false
                                                }
                                            )
                                        }
                                }

                                Button(
                                    onClick = {
                                        val memberId = delegateToMemberId
                                        if (memberId != null) {
                                            scope.launch {
                                                processing = true
                                                try {
                                                    repository.delegate(
                                                        currentUser.workspaceId,
                                                        instance.id,
                                                        memberId
                                                    )
                                                    delegating = false
                                                    delegateToMemberId = null
                                                    load()
                                                } catch (exception: HttpException) {
                                                    if (exception.code() == 401) {
                                                        onUnauthorized()
                                                    } else {
                                                        error = exception.message()
                                                    }
                                                } catch (exception: Exception) {
                                                    error = exception.message ?: ""
                                                } finally {
                                                    processing = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !processing &&
                                        delegateToMemberId != null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text.transfer)
                                }
                            }

                            if (
                                instance.status in listOf(
                                    "PENDING",
                                    "IN_PROGRESS",
                                    "PAUSED"
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { confirmAction = "cancel" },
                                        enabled = !processing
                                    ) {
                                        Text(text.cancel)
                                    }
                                    OutlinedButton(
                                        onClick = { confirmAction = "release" },
                                        enabled = !processing
                                    ) {
                                        Text(text.release)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                DetailsCard(
                    title = text.basic,
                    rows = listOf(
                        text.author to (
                            task.createdByMemberName
                                ?: memberName(details.members, task.createdByMemberId)
                            ),
                        text.created to (task.createdAt ?: "—"),
                        text.updated to (task.updatedAt ?: "—"),
                        text.mandatory to (
                            if (task.mandatory) text.yes else text.no
                            ),
                        text.recurrence to (task.recurrenceType ?: "—"),
                        text.dueTime to (task.dueTime ?: "—"),
                        text.assignment to (task.assignmentPolicy ?: "—"),
                        text.delegation to (
                            if (task.delegationAllowed) text.allowed
                            else text.forbidden
                            )
                    )
                )
            }

            item {
                DetailsCard(
                    title = text.responsibilityAndPoints,
                    rows = listOf(
                        text.assignedTo to
                            memberName(details.members, task.assignedMemberId),
                        text.responsible to
                            memberName(details.members, task.responsibleMemberId),
                        text.preferredExecutor to
                            memberName(details.members, task.preferredMemberId),
                        text.reward to
                            rewardText(
                                language, task.rewardAmount,
                                task.rewardPointTypeCode,
                                true
                            ),
                        text.rewardReputation to
                            signedText(task.rewardReputationAmount, true),
                        text.penalty to
                            rewardText(
                                language, task.penaltyAmount,
                                task.penaltyPointTypeCode,
                                false
                            ),
                        text.penaltyReputation to
                            signedText(task.penaltyReputationAmount, false)
                    )
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text.subtasks,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Button(
                                onClick = { onCreateSubtask(task.id) }
                            ) {
                                Text("+ " + text.create)
                            }
                        }

                        if (details.subtasks.isEmpty()) {
                            Text(text.noSubtasks)
                        } else {
                            details.subtasks.forEach { subtask ->
                                OutlinedButton(
                                    onClick = {
                                        onOpenTask(subtask.id, selectedDate)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val rewardLabel =
                                        if ((subtask.rewardAmount ?: 0) > 0) {
                                            "+" + formatPointAmount(subtask.rewardAmount!!.toLong(), language,
                                                subtask.rewardPointTypeCode, subtask.rewardPointTypeCode ?: "")
                                        } else {
                                            text.noReward
                                        }

                                    Text(
                                        "№" + subtask.id + " · " +
                                            subtask.title + " · " + rewardLabel
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text.participants,
                            style = MaterialTheme.typography.titleLarge
                        )

                        if (details.participants.isEmpty()) {
                            Text(text.noParticipants)
                        } else {
                            details.participants.forEach { participant ->
                                Text(
                                    participant.actorName + " · " +
                                        participant.role
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text.management,
                            style = MaterialTheme.typography.titleLarge
                        )
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    processing = true
                                    try {
                                        repository.setDefinitionActive(
                                            currentUser.workspaceId,
                                            task.id,
                                            !task.active
                                        )
                                        load()
                                    } catch (exception: HttpException) {
                                        if (exception.code() == 401) {
                                            onUnauthorized()
                                        } else {
                                            error = exception.message()
                                        }
                                    } catch (exception: Exception) {
                                        error = exception.message ?: ""
                                    } finally {
                                        processing = false
                                    }
                                }
                            },
                            enabled = !processing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text(
                                if (task.active) text.deactivate
                                else text.activate
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text.history,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (details.history.isEmpty()) {
                item { Text(text.noHistory) }
            } else {
                items(details.history, key = { it.id }) { event ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(event.occurredAt)
                            Text(
                                event.eventType,
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (!event.details.isNullOrBlank()) {
                                Text(event.details)
                            }
                        }
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
}

@Composable
private fun DetailsCard(
    title: String,
    rows: List<Pair<String, String>>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            rows.forEach { pair ->
                Column {
                    Text(
                        pair.first,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(pair.second)
                }
            }
        }
    }
}

private fun memberName(
    members: List<WorkspaceMemberResponse>,
    id: Long?
): String {
    if (id == null) return "—"
    return members.firstOrNull { it.id == id }?.name ?: "#" + id
}

private fun rewardText(
    language: AppLanguage,
    amount: Int?,
    code: String?,
    positive: Boolean
): String {
    if ((amount ?: 0) <= 0) return "—"
    val sign = if (positive) "+" else "-"
    return sign + formatPointAmount(amount!!.toLong(), language, code, code ?: "")
}

private fun signedText(
    amount: Int?,
    positive: Boolean
): String {
    if ((amount ?: 0) <= 0) return "—"
    return (if (positive) "+" else "-") + amount
}
