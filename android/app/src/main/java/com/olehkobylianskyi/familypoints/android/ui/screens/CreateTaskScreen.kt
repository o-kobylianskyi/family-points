package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.CreateTaskReferenceData
import com.olehkobylianskyi.familypoints.android.data.CreateTaskRepository
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.MemberGroupSummary
import com.olehkobylianskyi.familypoints.android.data.PointTypeResponse
import com.olehkobylianskyi.familypoints.android.data.TaskActorRef
import com.olehkobylianskyi.familypoints.android.data.TaskDefinitionCreateRequest
import com.olehkobylianskyi.familypoints.android.data.TaskDefinitionResponse
import com.olehkobylianskyi.familypoints.android.data.WorkspaceMemberResponse
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.createTaskStrings
import com.olehkobylianskyi.familypoints.android.ui.components.AppHeader
import com.olehkobylianskyi.familypoints.android.ui.components.LocalizedCalendarDialog
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

private enum class EconomyPreset {
    SIMPLE,
    NORMAL,
    HARD,
    CUSTOM
}

private enum class DateField {
    START,
    END
}

@Composable
fun CreateTaskScreen(
    language: AppLanguage,
    currentUser: CurrentUserResponse,
    parentTaskDefinitionId: Long?,
    repository: CreateTaskRepository,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (DashboardDestination) -> Unit,
    onCancel: () -> Unit,
    onCreated: (TaskDefinitionResponse) -> Unit,
    onUnauthorized: () -> Unit
) {
    val text = createTaskStrings(language)
    val scope = rememberCoroutineScope()

    var referenceData by remember {
        mutableStateOf<CreateTaskReferenceData?>(null)
    }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var mandatory by remember { mutableStateOf(false) }
    var delegationAllowed by remember { mutableStateOf(false) }

    var recurrenceType by remember { mutableStateOf("ONCE") }
    var startDate by remember {
        mutableStateOf(LocalDate.now().toString())
    }
    var endDate by remember { mutableStateOf("") }
    var recurrenceDayOfWeek by remember { mutableStateOf(1) }
    var recurrenceDayOfMonth by remember { mutableStateOf("1") }
    var dueTime by remember { mutableStateOf("") }

    var administrators by remember {
        mutableStateOf<Set<String>>(emptySet())
    }
    var observers by remember {
        mutableStateOf<Set<String>>(emptySet())
    }
    var executors by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    var economyPreset by remember {
        mutableStateOf(EconomyPreset.SIMPLE)
    }

    var rewardPointTypeId by remember { mutableStateOf<Long?>(null) }
    var penaltyPointTypeId by remember { mutableStateOf<Long?>(null) }
    var rewardAmount by remember { mutableStateOf("10") }
    var penaltyAmount by remember { mutableStateOf("5") }
    var rewardReputation by remember { mutableStateOf("1") }
    var penaltyReputation by remember { mutableStateOf("1") }

    var recurrenceMenuOpen by remember { mutableStateOf(false) }
    var weekDayMenuOpen by remember { mutableStateOf(false) }
    var rewardTypeMenuOpen by remember { mutableStateOf(false) }
    var penaltyTypeMenuOpen by remember { mutableStateOf(false) }
    var economyPresetMenuOpen by remember { mutableStateOf(false) }

    var dateField by remember { mutableStateOf<DateField?>(null) }
    var calendarMonth by remember {
        mutableStateOf(YearMonth.from(LocalDate.now()))
    }

    fun actorKey(type: String, id: Long): String =
        type + ":" + id

    fun actorRefs(keys: Set<String>): List<TaskActorRef> =
        keys.mapNotNull { key ->
            val parts = key.split(":")
            if (parts.size != 2) return@mapNotNull null
            val id = parts[1].toLongOrNull() ?: return@mapNotNull null
            TaskActorRef(parts[0], id)
        }

    fun applyPreset(preset: EconomyPreset) {
        economyPreset = preset
        when (preset) {
            EconomyPreset.SIMPLE -> {
                rewardAmount = "10"
                penaltyAmount = "5"
                rewardReputation = "1"
                penaltyReputation = "1"
            }
            EconomyPreset.NORMAL -> {
                rewardAmount = "20"
                penaltyAmount = "10"
                rewardReputation = "2"
                penaltyReputation = "1"
            }
            EconomyPreset.HARD -> {
                rewardAmount = "40"
                penaltyAmount = "20"
                rewardReputation = "4"
                penaltyReputation = "2"
            }
            EconomyPreset.CUSTOM -> Unit
        }
    }

    fun pointTypeLabel(type: PointTypeResponse): String =
        when (type.code.uppercase()) {
            "POINTS" -> text.pointsName
            "COPPER" -> text.copperName
            "SILVER" -> text.silverName
            "GOLD" -> text.goldName
            else -> type.name
        }

    fun firstFreeTitle(data: CreateTaskReferenceData): String {
        val used = data.definitions
            .map { it.title.trim().lowercase() }
            .toSet()

        var number = 1
        while (
            (text.defaultTitleBase + " " + number)
                .lowercase() in used
        ) {
            number += 1
        }
        return text.defaultTitleBase + " " + number
    }

    fun openCalendar(field: DateField) {
        val current = when (field) {
            DateField.START -> startDate
            DateField.END -> endDate.ifBlank { startDate }
        }

        val date = try {
            LocalDate.parse(current)
        } catch (_: Exception) {
            LocalDate.now()
        }

        calendarMonth = YearMonth.from(date)
        dateField = field
    }

    fun submitTask() {
        val data = referenceData ?: return
        val trimmedTitle = title.trim()

        if (trimmedTitle.isBlank()) {
            error = text.titleRequired
            return
        }

        if (
            data.definitions.any {
                it.title.trim().equals(
                    trimmedTitle,
                    ignoreCase = true
                )
            }
        ) {
            error = text.duplicateTitle
            return
        }

        if (executors.isEmpty()) {
            error = text.executorRequired
            return
        }

        if (startDate.isBlank()) {
            error = text.startDateRequired
            return
        }

        if (
            endDate.isNotBlank() &&
            endDate < startDate
        ) {
            error = text.endDateInvalid
            return
        }

        val normalizedTime = normalizeTaskTime(dueTime)

        if (normalizedTime == null) {
            error = text.dueTimeInvalid
            return
        }

        val monthlyDay = recurrenceDayOfMonth.toIntOrNull()

        if (
            recurrenceType == "MONTHLY" &&
            (
                monthlyDay == null ||
                monthlyDay !in 1..31
            )
        ) {
            error = text.monthDayInvalid
            return
        }

        val executorRefs = actorRefs(executors)
        val singleMemberExecutor =
            executorRefs.size == 1 &&
                executorRefs[0].actorType == "MEMBER"

        val reward = rewardAmount
            .toIntOrNull()
            ?.takeIf { it > 0 }

        val penalty = penaltyAmount
            .toIntOrNull()
            ?.takeIf { it > 0 }

        val request = TaskDefinitionCreateRequest(
            assignmentPolicy =
                if (singleMemberExecutor) {
                    "SINGLE_MEMBER"
                } else {
                    "PARTICIPANTS"
                },
            assignedMemberId =
                if (singleMemberExecutor) {
                    executorRefs[0].actorId
                } else {
                    null
                },
            targetGroupId = null,
            preferredMemberId = null,
            responsibleMemberId = null,
            parentTaskDefinitionId = parentTaskDefinitionId,
            delegationAllowed = delegationAllowed,
            roleMatchMode = "ANY",
            requiredGroupRoleIds = emptyList(),
            administrators = actorRefs(administrators),
            observers = actorRefs(observers),
            executors = executorRefs,
            title = trimmedTitle,
            description = description.trim().ifBlank { null },
            mandatory = mandatory,
            recurrenceType = recurrenceType,
            startDate = startDate,
            endDate = endDate.ifBlank { null },
            recurrenceDayOfWeek =
                if (recurrenceType == "WEEKLY") {
                    recurrenceDayOfWeek
                } else {
                    null
                },
            recurrenceDayOfMonth =
                if (recurrenceType == "MONTHLY") {
                    monthlyDay
                } else {
                    null
                },
            rewardPointTypeId =
                if (reward != null) {
                    rewardPointTypeId
                } else {
                    null
                },
            rewardAmount = reward,
            penaltyPointTypeId =
                if (penalty != null) {
                    penaltyPointTypeId
                } else {
                    null
                },
            penaltyAmount = penalty,
            rewardReputationAmount =
                rewardReputation
                    .toIntOrNull()
                    ?.takeIf { it > 0 },
            penaltyReputationAmount =
                penaltyReputation
                    .toIntOrNull()
                    ?.takeIf { it > 0 },
            dueTime =
                normalizedTime.takeIf {
                    it.isNotBlank()
                }
        )

        scope.launch {
            saving = true
            error = ""
            try {
                val created = repository.create(
                    currentUser.workspaceId,
                    request
                )
                onCreated(created)
            } catch (exception: retrofit2.HttpException) {
                if (exception.code() == 401) {
                    onUnauthorized()
                } else {
                    error = exception.message()
                }
            } catch (exception: Exception) {
                error = exception.message ?: ""
            } finally {
                saving = false
            }
        }
    }

    LaunchedEffect(currentUser.workspaceId) {
        loading = true
        error = ""

        try {
            val data = repository.loadReferenceData(
                currentUser.workspaceId
            )
            referenceData = data

            if (title.isBlank()) {
                title = firstFreeTitle(data)
            }

            if (executors.isEmpty()) {
                executors = setOf(
                    actorKey("MEMBER", currentUser.memberId)
                )
            }

            val firstPointType = data.pointTypes.firstOrNull()
            if (rewardPointTypeId == null) {
                rewardPointTypeId = firstPointType?.id
            }
            if (penaltyPointTypeId == null) {
                penaltyPointTypeId = firstPointType?.id
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
            loading = false
        }
    }

    if (dateField != null) {
        val selected = when (dateField) {
            DateField.START -> startDate
            DateField.END -> endDate.ifBlank { startDate }
            null -> startDate
        }

        LocalizedCalendarDialog(
            language = language,
            selectedDate = try {
                LocalDate.parse(selected)
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
                when (dateField) {
                    DateField.START -> startDate = date.toString()
                    DateField.END -> endDate = date.toString()
                    null -> Unit
                }
                dateField = null
            },
            onDismiss = {
                dateField = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        AppHeader(
            language = language,
            currentUser = currentUser,
            onLanguageChange = onLanguageChange,
            onLogout = onLogout,
            onNavigate = onNavigate
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !saving
            ) {
                Text("← " + text.cancel)
            }

            Button(
                onClick = { submitTask() },
                modifier = Modifier.weight(1f),
                enabled = !saving &&
                    !loading &&
                    executors.isNotEmpty()
            ) {
                Text(
                    if (saving) text.saving else text.create
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item {
            Text(
                text.title,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(text.description)
        }

        if (parentTaskDefinitionId != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text.parentTask + " №" +
                            parentTaskDefinitionId,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        if (error.isNotBlank()) {
            item {
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (loading) {
            item {
                Text(
                    when (language) {
                        AppLanguage.UK -> "Завантаження..."
                        AppLanguage.DE -> "Wird geladen..."
                        AppLanguage.EN -> "Loading..."
                        AppLanguage.RU -> "Загрузка..."
                    }
                )
            }
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it.take(150)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text.taskTitle) },
                singleLine = true,
                enabled = !saving
            )
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it.take(1000)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text.taskDescription) },
                minLines = 3,
                enabled = !saving
            )
        }

        item {
            SelectorButton(
                label = text.recurrence,
                value = recurrenceLabel(
                    text,
                    recurrenceType
                ),
                expanded = recurrenceMenuOpen,
                onOpen = { recurrenceMenuOpen = true },
                onDismiss = { recurrenceMenuOpen = false },
                options = listOf(
                    "ONCE" to text.once,
                    "DAILY" to text.daily,
                    "WEEKLY" to text.weekly,
                    "MONTHLY" to text.monthly
                ),
                onSelect = { value ->
                    recurrenceType = value
                    recurrenceMenuOpen = false
                }
            )
        }

        item {
            DateSelector(
                label = if (recurrenceType == "ONCE") {
                    text.executionDate
                } else {
                    text.startDate
                },
                date = startDate,
                onClick = {
                    openCalendar(DateField.START)
                }
            )
        }

        if (recurrenceType != "ONCE") {
            item {
                DateSelector(
                    label = text.endDate,
                    date = endDate.ifBlank { "—" },
                    onClick = {
                        openCalendar(DateField.END)
                    }
                )
            }
        }

        if (recurrenceType == "WEEKLY") {
            item {
                val weekdays = weekdayLabels(language)

                SelectorButton(
                    label = text.dayOfWeek,
                    value = weekdays[
                        recurrenceDayOfWeek - 1
                    ],
                    expanded = weekDayMenuOpen,
                    onOpen = { weekDayMenuOpen = true },
                    onDismiss = {
                        weekDayMenuOpen = false
                    },
                    options = weekdays.mapIndexed {
                            index,
                            label ->
                        (index + 1).toString() to label
                    },
                    onSelect = { value ->
                        recurrenceDayOfWeek =
                            value.toInt()
                        weekDayMenuOpen = false
                    }
                )
            }
        }

        if (recurrenceType == "MONTHLY") {
            item {
                OutlinedTextField(
                    value = recurrenceDayOfMonth,
                    onValueChange = {
                        recurrenceDayOfMonth =
                            it.filter(Char::isDigit)
                                .take(2)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text.dayOfMonth) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )
            }
        }

        item {
            OutlinedTextField(
                value = dueTime,
                onValueChange = { dueTime = it.take(5) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text.dueTime) },
                supportingText = {
                    Text(text.dueTimeHint)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )
        }

        item {
            CheckRow(
                checked = mandatory,
                label = text.mandatory,
                onCheckedChange = { mandatory = it }
            )
            CheckRow(
                checked = delegationAllowed,
                label = text.delegationAllowed,
                onCheckedChange = {
                    delegationAllowed = it
                }
            )
        }

        item {
            Text(
                text.participants,
                style = MaterialTheme.typography.titleLarge
            )
            Text(text.participantsHint)
        }

        referenceData?.let { data ->
            item {
                ParticipantSection(
                    title = text.administrators,
                    members = data.members,
                    groups = emptyList(),
                    selected = administrators,
                    includeGroups = false,
                    onToggle = { key ->
                        administrators =
                            toggleKey(administrators, key)
                    }
                )
                Text(
                    text.memberOnlyParticipants,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item {
                ParticipantSection(
                    title = text.observers,
                    members = data.members,
                    groups = emptyList(),
                    selected = observers,
                    includeGroups = false,
                    onToggle = { key ->
                        observers =
                            toggleKey(observers, key)
                    }
                )
                Text(
                    text.memberOnlyParticipants,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item {
                ParticipantSection(
                    title = text.executors,
                    members = data.members,
                    groups = data.groups,
                    selected = executors,
                    includeGroups = true,
                    onToggle = { key ->
                        executors =
                            toggleKey(executors, key)
                    }
                )
                Text(
                    text.authorHint,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item {
            Text(
                text.economyTitle,
                style = MaterialTheme.typography.titleLarge
            )
            Text(text.economyHint)

            val presetLabel = when (economyPreset) {
                EconomyPreset.SIMPLE -> text.simple
                EconomyPreset.NORMAL -> text.normal
                EconomyPreset.HARD -> text.hard
                EconomyPreset.CUSTOM -> text.custom
            }

            Column {
                OutlinedButton(
                    onClick = {
                        economyPresetMenuOpen = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(presetLabel + " ▾")
                }

                DropdownMenu(
                    expanded = economyPresetMenuOpen,
                    onDismissRequest = {
                        economyPresetMenuOpen = false
                    }
                ) {
                    EconomyPreset.entries.forEach { preset ->
                        val label = when (preset) {
                            EconomyPreset.SIMPLE -> text.simple
                            EconomyPreset.NORMAL -> text.normal
                            EconomyPreset.HARD -> text.hard
                            EconomyPreset.CUSTOM -> text.custom
                        }

                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                applyPreset(preset)
                                economyPresetMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        referenceData?.let { data ->
            item {
                EconomySection(
                    title = text.reward,
                    pointTypeLabel = text.pointType,
                    amountLabel = text.amount,
                    reputationLabel = text.reputation,
                    pointTypes = data.pointTypes,
                    pointTypeLabelFor = { pointTypeLabel(it) },
                    pointTypeId = rewardPointTypeId,
                    pointMenuOpen = rewardTypeMenuOpen,
                    onOpenPointMenu = {
                        rewardTypeMenuOpen = true
                    },
                    onDismissPointMenu = {
                        rewardTypeMenuOpen = false
                    },
                    onPointTypeSelected = {
                        rewardPointTypeId = it
                        rewardTypeMenuOpen = false
                    },
                    amount = rewardAmount,
                    onAmountChange = {
                        rewardAmount = it
                        economyPreset = EconomyPreset.CUSTOM
                    },
                    reputation = rewardReputation,
                    onReputationChange = {
                        rewardReputation = it
                        economyPreset = EconomyPreset.CUSTOM
                    }
                )
            }

            item {
                EconomySection(
                    title = text.penalty,
                    pointTypeLabel = text.pointType,
                    amountLabel = text.amount,
                    reputationLabel = text.reputation,
                    pointTypes = data.pointTypes,
                    pointTypeLabelFor = { pointTypeLabel(it) },
                    pointTypeId = penaltyPointTypeId,
                    pointMenuOpen = penaltyTypeMenuOpen,
                    onOpenPointMenu = {
                        penaltyTypeMenuOpen = true
                    },
                    onDismissPointMenu = {
                        penaltyTypeMenuOpen = false
                    },
                    onPointTypeSelected = {
                        penaltyPointTypeId = it
                        penaltyTypeMenuOpen = false
                    },
                    amount = penaltyAmount,
                    onAmountChange = {
                        penaltyAmount = it
                        economyPreset = EconomyPreset.CUSTOM
                    },
                    reputation = penaltyReputation,
                    onReputationChange = {
                        penaltyReputation = it
                        economyPreset = EconomyPreset.CUSTOM
                    }
                )
            }
        }

        item {
            Text(
                text = "",
                modifier = Modifier.padding(bottom = 36.dp)
            )
        }
      }
    }
}

@Composable
private fun SelectorButton(
    label: String,
    value: String,
    expanded: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium
        )
        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(value + " ▾")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.second) },
                    onClick = {
                        onSelect(option.first)
                    }
                )
            }
        }
    }
}

@Composable
private fun DateSelector(
    label: String,
    date: String,
    onClick: () -> Unit
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium
        )
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📅  " + date)
        }
    }
}

@Composable
private fun CheckRow(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            label,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun ParticipantSection(
    title: String,
    members: List<WorkspaceMemberResponse>,
    groups: List<MemberGroupSummary>,
    selected: Set<String>,
    includeGroups: Boolean,
    onToggle: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium
            )

            members.forEach { member ->
                val key = "MEMBER:" + member.id
                CheckRow(
                    checked = key in selected,
                    label = member.name,
                    onCheckedChange = {
                        onToggle(key)
                    }
                )
            }

            if (includeGroups) {
                groups.forEach { group ->
                    val key = "GROUP:" + group.id
                    CheckRow(
                        checked = key in selected,
                        label = "👥 " + group.name,
                        onCheckedChange = {
                            onToggle(key)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EconomySection(
    title: String,
    pointTypeLabel: String,
    amountLabel: String,
    reputationLabel: String,
    pointTypes: List<PointTypeResponse>,
    pointTypeLabelFor: (PointTypeResponse) -> String,
    pointTypeId: Long?,
    pointMenuOpen: Boolean,
    onOpenPointMenu: () -> Unit,
    onDismissPointMenu: () -> Unit,
    onPointTypeSelected: (Long) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    reputation: String,
    onReputationChange: (String) -> Unit
) {
    val selectedType =
        pointTypes.firstOrNull { it.id == pointTypeId }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                pointTypeLabel,
                style = MaterialTheme.typography.labelMedium
            )

            OutlinedButton(
                onClick = onOpenPointMenu,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    (selectedType?.let(pointTypeLabelFor)
                        ?: "—") + " ▾"
                )
            }

            DropdownMenu(
                expanded = pointMenuOpen,
                onDismissRequest = onDismissPointMenu
            ) {
                pointTypes.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(pointTypeLabelFor(type))
                        },
                        onClick = {
                            onPointTypeSelected(type.id)
                        }
                    )
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = {
                    onAmountChange(
                        it.filter(Char::isDigit)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(amountLabel) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = reputation,
                onValueChange = {
                    onReputationChange(
                        it.filter(Char::isDigit)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(reputationLabel) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true
            )
        }
    }
}

private fun recurrenceLabel(
    text: com.olehkobylianskyi.familypoints.android.i18n.CreateTaskStrings,
    value: String
): String = when (value) {
    "DAILY" -> text.daily
    "WEEKLY" -> text.weekly
    "MONTHLY" -> text.monthly
    else -> text.once
}

private fun weekdayLabels(
    language: AppLanguage
): List<String> = when (language) {
    AppLanguage.UK -> listOf(
        "Понеділок",
        "Вівторок",
        "Середа",
        "Четвер",
        "Пʼятниця",
        "Субота",
        "Неділя"
    )
    AppLanguage.DE -> listOf(
        "Montag",
        "Dienstag",
        "Mittwoch",
        "Donnerstag",
        "Freitag",
        "Samstag",
        "Sonntag"
    )
    AppLanguage.EN -> listOf(
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "Sunday"
    )
    AppLanguage.RU -> listOf(
        "Понедельник",
        "Вторник",
        "Среда",
        "Четверг",
        "Пятница",
        "Суббота",
        "Воскресенье"
    )
}

private fun toggleKey(
    current: Set<String>,
    key: String
): Set<String> =
    if (key in current) current - key else current + key

private fun normalizeTaskTime(value: String): String? {
    val raw = value.trim()
    if (raw.isBlank()) return ""

    val digits = raw.filter(Char::isDigit)

    val hours: Int
    val minutes: Int

    when {
        raw.matches(Regex("^\\d{1,2}$")) -> {
            hours = raw.toInt()
            minutes = 0
        }

        raw.matches(Regex("^\\d{3,4}$")) -> {
            val padded = raw.padStart(4, '0')
            hours = padded.substring(0, 2).toInt()
            minutes = padded.substring(2, 4).toInt()
        }

        raw.matches(Regex("^\\d{1,2}[:.]\\d{1,2}$")) -> {
            val parts = raw.split(':', '.')
            hours = parts[0].toInt()
            minutes = parts[1].toInt()
        }

        else -> return null
    }

    if (hours !in 0..23 || minutes !in 0..59) {
        return null
    }

    return hours.toString().padStart(2, '0') +
        ":" +
        minutes.toString().padStart(2, '0')
}
