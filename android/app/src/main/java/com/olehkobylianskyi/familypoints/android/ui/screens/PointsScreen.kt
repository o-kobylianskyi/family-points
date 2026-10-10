package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.PointTransactionResponse
import com.olehkobylianskyi.familypoints.android.data.PointsRepository
import com.olehkobylianskyi.familypoints.android.data.WorkspaceMemberResponse
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.createTaskStrings
import com.olehkobylianskyi.familypoints.android.i18n.pointsStrings
import com.olehkobylianskyi.familypoints.android.ui.components.AppHeader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.launch
import retrofit2.HttpException

private enum class PointOperation(
    val apiValue: String
) {
    EARN("earn"),
    SPEND("spend"),
    PENALTY("penalty")
}

@Composable
fun PointsScreen(
    language: AppLanguage,
    currentUser: CurrentUserResponse,
    repository: PointsRepository,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (DashboardDestination) -> Unit,
    onUnauthorized: () -> Unit
) {
    val text = pointsStrings(language)
    val createText = createTaskStrings(language)
    val scope = rememberCoroutineScope()

    var members by remember {
        mutableStateOf<List<WorkspaceMemberResponse>>(emptyList())
    }
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }
    var balance by remember { mutableStateOf(0L) }
    var history by remember {
        mutableStateOf<List<PointTransactionResponse>>(emptyList())
    }

    var loadingMembers by remember { mutableStateOf(true) }
    var loadingPoints by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    var pointOperation by remember {
        mutableStateOf<PointOperation?>(null)
    }

    suspend fun loadPoints(memberId: Long) {
        loadingPoints = true
        error = ""

        try {
            val result = repository.loadMemberPoints(
                currentUser.workspaceId,
                memberId
            )
            balance = result.first
            history = result.second
        } catch (exception: HttpException) {
            if (exception.code() == 401) {
                onUnauthorized()
            } else {
                error = exception.message()
            }
        } catch (exception: Exception) {
            error = exception.message ?: ""
        } finally {
            loadingPoints = false
        }
    }

    LaunchedEffect(currentUser.workspaceId) {
        loadingMembers = true
        error = ""

        try {
            members = repository.loadMembers(
                currentUser.workspaceId
            )

            selectedMemberId =
                members.firstOrNull {
                    it.id == currentUser.memberId
                }?.id
                    ?: members.firstOrNull()?.id
        } catch (exception: HttpException) {
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

    LaunchedEffect(selectedMemberId) {
        selectedMemberId?.let {
            loadPoints(it)
        }
    }

    val selectedMember = members.firstOrNull {
        it.id == selectedMemberId
    }

    if (
        pointOperation != null &&
        selectedMember != null
    ) {
        PointOperationDialog(
            language = language,
            operation = pointOperation!!,
            member = selectedMember,
            processing = processing,
            onDismiss = {
                if (!processing) {
                    pointOperation = null
                }
            },
            onSubmit = { amount, description ->
                scope.launch {
                    processing = true
                    error = ""

                    try {
                        repository.operate(
                            currentUser.workspaceId,
                            selectedMember.id,
                            pointOperation!!.apiValue,
                            amount,
                            description.trim().ifBlank { null }
                        )
                        pointOperation = null
                        loadPoints(selectedMember.id)
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

            if (error.isNotBlank()) {
                item {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (loadingMembers) {
                item {
                    Text(text.loading)
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        members.forEach { member ->
                            if (member.id == selectedMemberId) {
                                Button(
                                    onClick = {}
                                ) {
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
            }

            if (selectedMember != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                selectedMember.name,
                                style =
                                    MaterialTheme.typography.titleLarge
                            )

                            Text(
                                memberTypeLabel(
                                    language,
                                    selectedMember.memberType
                                )
                            )

                            Text(
                                text.balance,
                                style =
                                    MaterialTheme.typography.labelLarge
                            )

                            Text(
                                if (loadingPoints) {
                                    "—"
                                } else {
                                    balance.toString()
                                },
                                style =
                                    MaterialTheme.typography.headlineLarge
                            )

                            Text(text.points)

                            if (
                                "MANAGE_POINTS" in
                                    currentUser.permissions
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            pointOperation =
                                                PointOperation.EARN
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+ " + text.earn)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            pointOperation =
                                                PointOperation.SPEND
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("− " + text.spend)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        pointOperation =
                                            PointOperation.PENALTY
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text.penalty)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text.history,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(text.historyDescription)
            }

            if (loadingPoints) {
                item {
                    Text(text.loading)
                }
            } else if (history.isEmpty()) {
                item {
                    Text(text.noHistory)
                }
            } else {
                items(
                    history,
                    key = { it.id }
                ) { transaction ->
                    PointTransactionCard(
                        language = language,
                        transaction = transaction,
                        pointTypeLabel = pointTypeLabel(
                            createText,
                            transaction
                        )
                    )
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
}

@Composable
private fun PointOperationDialog(
    language: AppLanguage,
    operation: PointOperation,
    member: WorkspaceMemberResponse,
    processing: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    val text = pointsStrings(language)

    var amount by remember(
        operation,
        member.id
    ) {
        mutableStateOf("")
    }

    var description by remember(
        operation,
        member.id
    ) {
        mutableStateOf("")
    }

    var localError by remember(
        operation,
        member.id
    ) {
        mutableStateOf("")
    }

    val title = when (operation) {
        PointOperation.EARN -> text.earn
        PointOperation.SPEND -> text.spend
        PointOperation.PENALTY -> text.penalty
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(title)
                Text(
                    member.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter(Char::isDigit)
                        localError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text.amount) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    enabled = !processing
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it.take(255)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(text.operationDescription)
                    },
                    minLines = 3,
                    enabled = !processing
                )

                if (localError.isNotBlank()) {
                    Text(
                        localError,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val numericAmount = amount.toIntOrNull()

                    if (
                        numericAmount == null ||
                        numericAmount <= 0
                    ) {
                        localError = text.invalidAmount
                        return@Button
                    }

                    onSubmit(
                        numericAmount,
                        description
                    )
                },
                enabled = !processing
            ) {
                Text(text.save)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !processing
            ) {
                Text(text.cancel)
            }
        }
    )
}

@Composable
private fun PointTransactionCard(
    language: AppLanguage,
    transaction: PointTransactionResponse,
    pointTypeLabel: String
) {
    val text = pointsStrings(language)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    transactionTypeLabel(
                        language,
                        transaction.type
                    ),
                    style =
                        MaterialTheme.typography.titleMedium
                )

                Text(
                    formatAmount(transaction.amount) +
                        " " +
                        pointTypeLabel,
                    style =
                        MaterialTheme.typography.titleMedium
                )
            }

            Text(
                transactionDescription(
                    language,
                    transaction
                )
            )

            transaction.createdAt?.let {
                Text(
                    formatTransactionDate(
                        language,
                        it
                    ),
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun pointTypeLabel(
    text:
        com.olehkobylianskyi.familypoints.android.i18n.CreateTaskStrings,
    transaction: PointTransactionResponse
): String =
    when (
        transaction.pointTypeCode
            ?.uppercase()
    ) {
        "POINTS" -> text.pointsName
        "COPPER" -> text.copperName
        "SILVER" -> text.silverName
        "GOLD" -> text.goldName
        else ->
            transaction.pointTypeName
                ?: transaction.pointTypeCode
                ?: text.pointsName
    }

private fun memberTypeLabel(
    language: AppLanguage,
    memberType: String
): String {
    val text = pointsStrings(language)

    return when (memberType.uppercase()) {
        "ADULT" -> text.memberTypeAdult
        "CHILD" -> text.memberTypeChild
        else -> text.memberTypeOther
    }
}

private fun transactionTypeLabel(
    language: AppLanguage,
    type: String
): String =
    when (language) {
        AppLanguage.UK -> when (type) {
            "EARN" -> "Нарахування"
            "SPEND" -> "Витрата"
            "PENALTY" -> "Штраф"
            else -> type
        }

        AppLanguage.DE -> when (type) {
            "EARN" -> "Gutschrift"
            "SPEND" -> "Ausgabe"
            "PENALTY" -> "Strafe"
            else -> type
        }

        AppLanguage.EN -> when (type) {
            "EARN" -> "Earn"
            "SPEND" -> "Spend"
            "PENALTY" -> "Penalty"
            else -> type
        }

        AppLanguage.RU -> when (type) {
            "EARN" -> "Начисление"
            "SPEND" -> "Расход"
            "PENALTY" -> "Штраф"
            else -> type
        }
    }

private fun transactionDescription(
    language: AppLanguage,
    transaction: PointTransactionResponse
): String {
    val text = pointsStrings(language)
    val description = transaction.description
        ?.trim()
        .orEmpty()

    return when (transaction.sourceType) {
        "POINT_EXCHANGE" ->
            text.pointExchange

        "TASK" -> {
            val prefix = "Task completed:"
            val title =
                if (description.startsWith(prefix)) {
                    description
                        .removePrefix(prefix)
                        .trim()
                } else {
                    description
                }

            text.taskCompleted.format(
                title.ifBlank { text.unknownTask }
            )
        }

        "TASK_PENALTY" -> {
            val prefix = "Task missed:"
            val title =
                if (description.startsWith(prefix)) {
                    description
                        .removePrefix(prefix)
                        .trim()
                } else {
                    description
                }

            text.taskMissed.format(
                title.ifBlank { text.unknownTask }
            )
        }

        "SYSTEM" ->
            if (
                description ==
                "Initial test points"
            ) {
                text.initialTestPoints
            } else {
                description.ifBlank {
                    text.noDescription
                }
            }

        else ->
            description.ifBlank {
                text.noDescription
            }
    }
}

private fun formatAmount(amount: Int): String =
    if (amount > 0) {
        "+" + amount
    } else {
        amount.toString()
    }

private fun formatTransactionDate(
    language: AppLanguage,
    value: String
): String {
    return try {
        val dateTime = LocalDateTime.parse(value)
        val locale = Locale.forLanguageTag(
            language.code
        )

        dateTime.format(
            DateTimeFormatter
                .ofLocalizedDateTime(
                    FormatStyle.MEDIUM,
                    FormatStyle.SHORT
                )
                .withLocale(locale)
        )
    } catch (_: Exception) {
        value
    }
}
