package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.PointTypeResponse
import com.olehkobylianskyi.familypoints.android.data.RewardDefinitionSummary
import com.olehkobylianskyi.familypoints.android.data.RewardNegotiationData
import com.olehkobylianskyi.familypoints.android.data.RewardNegotiationRepository
import com.olehkobylianskyi.familypoints.android.data.TaskRewardRequestCreateRequest
import com.olehkobylianskyi.familypoints.android.data.TaskRewardRequestResponse
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.rewardNegotiationStrings
import com.olehkobylianskyi.familypoints.android.ui.components.AppHeader
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun RewardNegotiationScreen(
    language: AppLanguage,
    currentUser: CurrentUserResponse,
    definitionId: Long,
    selectedDate: String,
    instanceId: Long,
    repository: RewardNegotiationRepository,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (DashboardDestination) -> Unit,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onUnauthorized: () -> Unit
) {
    val text = rewardNegotiationStrings(language)
    val scope = rememberCoroutineScope()

    var data by remember { mutableStateOf<RewardNegotiationData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    var pointTypeId by remember { mutableStateOf<Long?>(null) }
    var pointAmount by remember { mutableStateOf("") }
    var reputationAmount by remember { mutableStateOf("") }
    var rewardDefinitionId by remember { mutableStateOf<Long?>(null) }
    var customRewardTitle by remember { mutableStateOf("") }
    var durationPreset by remember { mutableStateOf("ANY") }
    var customDurationMinutes by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    var pointTypeMenuOpen by remember { mutableStateOf(false) }
    var rewardMenuOpen by remember { mutableStateOf(false) }
    var durationMenuOpen by remember { mutableStateOf(false) }

    suspend fun load() {
        loading = true
        error = ""

        try {
            val loaded = repository.load(
                currentUser.workspaceId,
                definitionId,
                selectedDate,
                instanceId
            )
            data = loaded

            val task = loaded.taskInstance
            pointTypeId =
                task.rewardPointTypeId
                    ?: loaded.pointTypes.firstOrNull()?.id
            pointAmount = (task.rewardAmount ?: 0).toString()
            reputationAmount =
                (task.rewardReputationAmount ?: 0).toString()
            rewardDefinitionId = null
            customRewardTitle = ""
            durationPreset = "ANY"
            customDurationMinutes = ""
            comment = ""
        } catch (exception: HttpException) {
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

    LaunchedEffect(
        currentUser.workspaceId,
        definitionId,
        selectedDate,
        instanceId
    ) {
        load()
    }

    val currentData = data
    val activeRequest = currentData?.requests
        ?.firstOrNull {
            it.status == "REQUESTED" ||
                it.status == "APPROVED"
        }

    val selectedReward = currentData?.rewards
        ?.firstOrNull { it.id == rewardDefinitionId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
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
            OutlinedButton(onClick = onBack) {
                Text("← " + text.cancel)
            }
        }

        item {
            Text(
                text.title,
                style = MaterialTheme.typography.headlineMedium
            )

            currentData?.taskInstance?.title?.let {
                Text(it)
            }
        }

        if (loading) {
            item {
                Text(text.loading)
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

        if (!loading && activeRequest != null) {
            item {
                ActiveRewardRequestCard(
                    language = language,
                    request = activeRequest,
                    saving = saving,
                    onCancel = {
                        scope.launch {
                            saving = true
                            error = ""
                            try {
                                repository.cancel(
                                    currentUser.workspaceId,
                                    activeRequest.id
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
                                saving = false
                            }
                        }
                    }
                )
            }
        }

        if (!loading && currentData != null && activeRequest == null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text.points,
                            style = MaterialTheme.typography.titleLarge
                        )

                        DropdownSelector(
                            label = text.pointType,
                            value = currentData.pointTypes
                                .firstOrNull { it.id == pointTypeId }
                                ?.name
                                ?: "—",
                            expanded = pointTypeMenuOpen,
                            onOpen = { pointTypeMenuOpen = true },
                            onDismiss = {
                                pointTypeMenuOpen = false
                            },
                            options = currentData.pointTypes,
                            optionText = { it.name },
                            onSelect = {
                                pointTypeId = it.id
                                pointTypeMenuOpen = false
                            }
                        )

                        OutlinedTextField(
                            value = pointAmount,
                            onValueChange = {
                                pointAmount =
                                    it.filter(Char::isDigit)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text.amount) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            )
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text.reputation,
                            style = MaterialTheme.typography.titleLarge
                        )

                        OutlinedTextField(
                            value = reputationAmount,
                            onValueChange = {
                                reputationAmount =
                                    it.filter(Char::isDigit)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text.amount) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            )
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text.reward,
                            style = MaterialTheme.typography.titleLarge
                        )

                        DropdownSelectorNullable(
                            label = text.fromCatalog,
                            value = selectedReward?.title
                                ?: text.noCatalogReward,
                            expanded = rewardMenuOpen,
                            onOpen = { rewardMenuOpen = true },
                            onDismiss = {
                                rewardMenuOpen = false
                            },
                            emptyLabel = text.noCatalogReward,
                            options = currentData.rewards,
                            optionText = { it.title },
                            onClear = {
                                rewardDefinitionId = null
                                rewardMenuOpen = false
                            },
                            onSelect = {
                                rewardDefinitionId = it.id
                                customRewardTitle = ""
                                rewardMenuOpen = false
                                durationPreset = "ANY"
                                customDurationMinutes = ""
                            }
                        )

                        if (selectedReward != null) {
                            val details = buildString {
                                append(selectedReward.title)
                                append(" · ")
                                append(text.normalPrice)
                                append(" ")
                                append(selectedReward.priceAmount)

                                if (
                                    selectedReward.rewardKind ==
                                    "TIME_BASED"
                                ) {
                                    append(" · ")
                                    append(text.timeReward)

                                    selectedReward
                                        .defaultDurationMinutes
                                        ?.let {
                                            append(" · ")
                                            append(text.typical)
                                            append(" ")
                                            append(it)
                                            append(" ")
                                            append(text.minutes)
                                        }
                                }
                            }

                            Text(
                                details,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (
                            selectedReward?.rewardKind ==
                            "TIME_BASED"
                        ) {
                            val durationOptions = listOf(
                                "ANY" to text.any,
                                "15" to "15 " + text.minutes,
                                "30" to "30 " + text.minutes,
                                "45" to "45 " + text.minutes,
                                "60" to text.oneHour,
                                "90" to text.ninetyMinutes,
                                "120" to text.twoHours,
                                "CUSTOM" to text.customTime
                            )

                            DropdownSelector(
                                label = text.time,
                                value = durationOptions
                                    .firstOrNull {
                                        it.first == durationPreset
                                    }
                                    ?.second
                                    ?: text.any,
                                expanded = durationMenuOpen,
                                onOpen = {
                                    durationMenuOpen = true
                                },
                                onDismiss = {
                                    durationMenuOpen = false
                                },
                                options = durationOptions,
                                optionText = { it.second },
                                onSelect = {
                                    durationPreset = it.first
                                    durationMenuOpen = false
                                }
                            )

                            if (durationPreset == "CUSTOM") {
                                OutlinedTextField(
                                    value = customDurationMinutes,
                                    onValueChange = {
                                        customDurationMinutes =
                                            it.filter(Char::isDigit)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = {
                                        Text(text.minutes)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = customRewardTitle,
                            onValueChange = {
                                customRewardTitle = it.take(150)
                                if (it.isNotBlank()) {
                                    rewardDefinitionId = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(text.customReward)
                            },
                            placeholder = {
                                Text(text.customRewardHint)
                            },
                            singleLine = true
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = comment,
                    onValueChange = {
                        comment = it.take(500)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text.comment) },
                    placeholder = {
                        Text(text.commentHint)
                    },
                    minLines = 3
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        enabled = !saving
                    ) {
                        Text(text.cancel)
                    }

                    Button(
                        onClick = {
                            val points = pointAmount
                                .toIntOrNull()
                                ?.takeIf { it > 0 }

                            val reputation = reputationAmount
                                .toIntOrNull()
                                ?.takeIf { it > 0 }

                            val duration = when {
                                selectedReward?.rewardKind !=
                                    "TIME_BASED" -> null

                                durationPreset == "ANY" -> null

                                durationPreset == "CUSTOM" ->
                                    customDurationMinutes
                                        .toIntOrNull()
                                        ?.takeIf { it > 0 }

                                else ->
                                    durationPreset
                                        .toIntOrNull()
                            }

                            if (
                                points == null &&
                                reputation == null &&
                                rewardDefinitionId == null &&
                                customRewardTitle.trim().isBlank()
                            ) {
                                error = text.required
                                return@Button
                            }

                            val request =
                                TaskRewardRequestCreateRequest(
                                    pointTypeId =
                                        if (points != null) {
                                            pointTypeId
                                        } else {
                                            null
                                        },
                                    pointAmount = points,
                                    reputationAmount = reputation,
                                    rewardDefinitionId =
                                        rewardDefinitionId,
                                    durationMinutes = duration,
                                    customRewardTitle =
                                        customRewardTitle
                                            .trim()
                                            .ifBlank { null },
                                    comment =
                                        comment
                                            .trim()
                                            .ifBlank { null }
                                )

                            scope.launch {
                                saving = true
                                error = ""

                                try {
                                    repository.create(
                                        currentUser.workspaceId,
                                        instanceId,
                                        request
                                    )
                                    onSaved()
                                } catch (exception: HttpException) {
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
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !saving
                    ) {
                        Text(
                            if (saving) text.saving
                            else text.submit
                        )
                    }
                }
            }
        }

        item {
            Text(
                "",
                modifier = Modifier.padding(bottom = 36.dp)
            )
        }
    }
}

@Composable
private fun ActiveRewardRequestCard(
    language: AppLanguage,
    request: TaskRewardRequestResponse,
    saving: Boolean,
    onCancel: () -> Unit
) {
    val text = rewardNegotiationStrings(language)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text.activeRequest,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text.status + ": " + request.status
            )

            request.requestedPointAmount?.let {
                Text(
                    text.requested + ": " +
                        it + " " + text.points
                )
            }

            request.requestedReputationAmount?.let {
                Text(
                    text.reputation + ": +" + it
                )
            }

            request.requestedRewardTitle?.let {
                Text(text.reward + ": " + it)
            }

            request.requestedCustomRewardTitle?.let {
                Text(text.reward + ": " + it)
            }

            request.requestedDurationMinutes?.let {
                Text(
                    text.time + ": " +
                        it + " " + text.minutes
                )
            }

            if (!request.requestedComment.isNullOrBlank()) {
                Text(request.requestedComment)
            }

            if (request.status == "REQUESTED") {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text.cancelRequest)
                }
            }
        }
    }
}

@Composable
private fun <T> DropdownSelector(
    label: String,
    value: String,
    expanded: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    options: List<T>,
    optionText: (T) -> String,
    onSelect: (T) -> Unit
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
                    text = { Text(optionText(option)) },
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}

@Composable
private fun DropdownSelectorNullable(
    label: String,
    value: String,
    expanded: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    emptyLabel: String,
    options: List<RewardDefinitionSummary>,
    optionText: (RewardDefinitionSummary) -> String,
    onClear: () -> Unit,
    onSelect: (RewardDefinitionSummary) -> Unit
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
            DropdownMenuItem(
                text = { Text(emptyLabel) },
                onClick = onClear
            )

            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionText(option)) },
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}
