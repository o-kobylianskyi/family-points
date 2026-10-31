package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import com.olehkobylianskyi.familypoints.android.ui.components.FpCard as Card
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.PointTransactionResponse
import com.olehkobylianskyi.familypoints.android.data.PointsRepository
import com.olehkobylianskyi.familypoints.android.data.SettingsRepository
import com.olehkobylianskyi.familypoints.android.data.PointNameFormDto
import com.olehkobylianskyi.familypoints.android.data.RewardsRepository
import com.olehkobylianskyi.familypoints.android.data.RewardDefinitionSummary
import com.olehkobylianskyi.familypoints.android.data.PointTypeResponse
import com.olehkobylianskyi.familypoints.android.data.WorkspaceMemberResponse
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.formatPointAmount
import com.olehkobylianskyi.familypoints.android.i18n.localizedCurrencyName
import com.olehkobylianskyi.familypoints.android.i18n.createTaskStrings
import com.olehkobylianskyi.familypoints.android.i18n.pointsStrings
import com.olehkobylianskyi.familypoints.android.ui.components.AppHeader
import com.olehkobylianskyi.familypoints.android.storage.OfflinePointsQueueStore
import com.olehkobylianskyi.familypoints.android.storage.PointQuickAmountStore
import com.olehkobylianskyi.familypoints.android.network.ServerReachability
import com.olehkobylianskyi.familypoints.android.network.ServerStatus
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
    refreshNonce: Int = 0,
    onRefreshComplete: (Int) -> Unit = {},
    language: AppLanguage,
    currentUser: CurrentUserResponse,
    repository: PointsRepository,
    rewardsRepository: RewardsRepository,
    settingsRepository: SettingsRepository,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (DashboardDestination) -> Unit,
    onUnauthorized: () -> Unit
) {
    val text = pointsStrings(language)
    val createText = createTaskStrings(language)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val quickAmounts = remember(context) { PointQuickAmountStore(context) }
    val serverStatus by ServerReachability.status.collectAsState()
    val canManage = currentUser.permissions.any { it == "MANAGE_POINTS" || it == "ADMIN_OVERRIDE" }
    val canPurchaseRewards = currentUser.permissions.any {
        it == "MANAGE_REWARDS" || it == "ADMIN_OVERRIDE"
    }
    var pendingOperations by remember { mutableStateOf<List<OfflinePointsQueueStore.Pending>>(emptyList()) }
    var memberBalances by remember { mutableStateOf<Map<Long, Long>>(emptyMap()) }
    var syncMessage by remember { mutableStateOf("") }
    var pointTypes by remember { mutableStateOf<List<PointTypeResponse>>(emptyList()) }
    var currencyForms by remember(currentUser.workspaceId) { mutableStateOf<Map<Long, List<PointNameFormDto>>>(emptyMap()) }
    var rewardCatalog by remember { mutableStateOf<List<RewardDefinitionSummary>>(emptyList()) }

    var members by remember {
        mutableStateOf<List<WorkspaceMemberResponse>>(emptyList())
    }
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }
    var balance by remember { mutableStateOf(0L) }
    var offlineSnapshot by remember { mutableStateOf(false) }
    var hasBalance by remember { mutableStateOf(false) }
    var history by remember {
        mutableStateOf<List<PointTransactionResponse>>(emptyList())
    }

    var historyExpanded by remember(currentUser.workspaceId, selectedMemberId) { mutableStateOf(false) }
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
        history = emptyList()
        hasBalance = false
        offlineSnapshot = true

        // Cache first: show balance immediately, then fetch a newer snapshot.
        repository.cachedBalance(currentUser.workspaceId, memberId)?.let { cached ->
            // One shared balance snapshot drives both the member chip and card.
            memberBalances = memberBalances + (memberId to cached)
            if (selectedMemberId == memberId) {
                balance = cached
                hasBalance = true
            }
        }

        if (serverStatus != ServerStatus.ONLINE) {
            loadingPoints = false
            if (!hasBalance) {
                error = when (language) {
                    AppLanguage.UK -> "Баланс ще не збережений на телефоні. Спочатку відкрийте його онлайн."
                    AppLanguage.DE -> "Kontostand noch nicht gespeichert. Bitte zuerst online öffnen."
                    AppLanguage.EN -> "Balance not cached yet. Open it online first."
                    AppLanguage.RU -> "Баланс ещё не сохранён на телефоне. Откройте его онлайн."
                }
            }
            return
        }

        try {
            val result = repository.loadPointsWithFallback(
                currentUser.workspaceId,
                memberId
            )
            memberBalances = memberBalances + (memberId to result.balance)
            if (selectedMemberId == memberId) {
                balance = result.balance
                hasBalance = true
                offlineSnapshot = result.offline
            }
            if (!result.offline) {
                scope.launch {
                    try {
                        val recent = repository.loadHistory(currentUser.workspaceId, memberId)
                        if (selectedMemberId == memberId) history = recent
                    } catch (exception: HttpException) {
                        if (exception.code() == 401) onUnauthorized()
                    } catch (_: Exception) {
                        // History is optional and never blocks a current balance.
                    }
                }
            }
        } catch (exception: HttpException) {
            if (exception.code() == 401) {
                onUnauthorized()
            } else {
                error = exception.message()
            }
        } catch (exception: Exception) {
            if (!hasBalance) error = exception.message ?: ""
        } finally {
            loadingPoints = false
        }
    }

    LaunchedEffect(currentUser.accountId, currentUser.username, currentUser.workspaceId, currentUser.memberId, serverStatus, refreshNonce) {
        pointTypes = repository.cachedPointTypes(currentUser.workspaceId, currentUser.memberId)
        if (serverStatus == ServerStatus.ONLINE) {
            try {
                pointTypes = repository.refreshPointTypes(currentUser.workspaceId, currentUser.memberId)
            } catch (exception: HttpException) {
                if (exception.code() == 401) onUnauthorized()
            } catch (_: Exception) {
                // Keep cached currencies available for offline queueing.
            }
        }
    }

    LaunchedEffect(currentUser.workspaceId, pointTypes, serverStatus, refreshNonce) {
        if (serverStatus != ServerStatus.ONLINE) return@LaunchedEffect
        val updated = currencyForms.toMutableMap()
        for (type in pointTypes) {
            try {
                updated[type.id] = settingsRepository.pointNameForms(currentUser.workspaceId, type.id)
            } catch (e: HttpException) {
                if (e.code() == 401) onUnauthorized()
                // Preserve prior forms for non-auth errors; fall back to built-in rules.
            } catch (_: Exception) {
                // Older servers may not support custom forms.
            }
        }
        currencyForms = updated
    }

    LaunchedEffect(currentUser.accountId, currentUser.username, currentUser.workspaceId, currentUser.memberId, serverStatus, refreshNonce) {
        rewardCatalog = rewardsRepository.cached(currentUser.workspaceId, currentUser.memberId)
            ?.rewards.orEmpty()
        if (serverStatus == ServerStatus.ONLINE) {
            try {
                rewardCatalog = rewardsRepository.refreshCatalog(
                    currentUser.workspaceId, currentUser.memberId
                ).rewards
            } catch (exception: HttpException) {
                if (exception.code() == 401) onUnauthorized()
            } catch (_: Exception) {
                // Show existing catalog; purchases still require server confirmation.
            }
        }
    }

    LaunchedEffect(currentUser.accountId, currentUser.username, currentUser.workspaceId, currentUser.memberId, serverStatus, refreshNonce) {
        loadingMembers = false
        error = ""
        pendingOperations = repository.pending(currentUser.workspaceId, currentUser.memberId, currentUser.accountId)
        val quarantinedCount = repository.quarantinedLegacyCount()
        if (quarantinedCount > 0) {
            syncMessage = when (language) {
                AppLanguage.UK -> "Є $quarantinedCount старих офлайн-операцій без підтвердженого власника. Вони збережені, але заблоковані від синхронізації."
                AppLanguage.DE -> "$quarantinedCount ältere Offline-Vorgänge ohne bestätigten Kontoinhaber wurden sicher zurückgehalten."
                AppLanguage.EN -> "$quarantinedCount legacy offline operations have no verified owner; retained but blocked from replay."
                AppLanguage.RU -> "$quarantinedCount старых офлайн-операций без подтверждённого владельца сохранены, но заблокированы."
            }
        }
        val cached = repository.cachedMembers(currentUser.workspaceId, currentUser.memberId)
        members = (if (canManage) cached else cached.filter { it.id == currentUser.memberId }).ifEmpty { listOf(WorkspaceMemberResponse(
            id = currentUser.memberId,
            name = currentUser.memberName,
            memberType = currentUser.memberType,
            workspaceRoleId = currentUser.workspaceRoleId,
            workspaceRoleCode = currentUser.workspaceRoleCode,
            workspaceRoleName = currentUser.workspaceRoleName,
            workspaceId = currentUser.workspaceId
        )) }
        if (selectedMemberId !in members.map { it.id }) {
            selectedMemberId = members.firstOrNull { it.id == currentUser.memberId }?.id
                ?: members.firstOrNull()?.id
        }
        memberBalances = members.mapNotNull { member ->
            repository.cachedConfirmedBalance(currentUser.workspaceId, member.id)
                ?.let { member.id to it }
        }.toMap()

        if (serverStatus != ServerStatus.ONLINE) return@LaunchedEffect
        try {
            val receivedMembers = repository.loadMembers(currentUser.workspaceId, currentUser.memberId)
            members = if (canManage) receivedMembers else receivedMembers.filter { it.id == currentUser.memberId }
            if (selectedMemberId !in members.map { it.id }) {
                selectedMemberId = currentUser.memberId
            }
            repository.prefetchBalances(currentUser.workspaceId, members)
            memberBalances = members.mapNotNull { member ->
                repository.cachedConfirmedBalance(currentUser.workspaceId, member.id)
                    ?.let { member.id to it }
            }.toMap()
            // Replay only after a fresh online authentication confirms permissions.
            if (canManage) {
                val count = repository.synchronizePending(currentUser.workspaceId, currentUser.memberId, currentUser.accountId)
                pendingOperations = repository.pending(currentUser.workspaceId, currentUser.memberId, currentUser.accountId)
                if (count > 0) {
                    syncMessage = when (language) {
                        AppLanguage.UK -> "Синхронізовано операцій: $count"
                        AppLanguage.DE -> "Synchronisierte Vorgänge: $count"
                        AppLanguage.EN -> "Synchronized operations: $count"
                        AppLanguage.RU -> "Синхронизировано операций: $count"
                    }
                    // Replay updates the disk cache for affected members.
                    // Refresh the Compose state immediately, not only the cache.
                    memberBalances = members.mapNotNull { member ->
                        repository.cachedConfirmedBalance(currentUser.workspaceId, member.id)
                            ?.let { member.id to it }
                    }.toMap()
                    selectedMemberId?.let { loadPoints(it) }
                }
            }
        } catch (exception: HttpException) {
            if (exception.code() == 401) onUnauthorized()
            else {
                // A forbidden replay is not an expired session: retain pending
                // operations for parental review rather than logging the user out.
                pendingOperations = repository.pending(currentUser.workspaceId, currentUser.memberId, currentUser.accountId)
                syncMessage = if (exception.code() == 403) {
                    when (language) {
                        AppLanguage.UK -> "Сервер заборонив синхронізацію (403). Операції залишилися в черзі. Перевірте права керування балами."
                        AppLanguage.DE -> "Synchronisierung verweigert (403). Vorgänge bleiben gespeichert. Punkteberechtigung prüfen."
                        AppLanguage.EN -> "Synchronization denied (403). Operations remain queued. Check points permissions."
                        AppLanguage.RU -> "Синхронизация запрещена (403). Операции остались в очереди. Проверьте права управления баллами."
                    }
                } else when (language) {
                    AppLanguage.UK -> "Деякі операції очікують підтвердження сервера."
                    AppLanguage.DE -> "Einige Vorgänge warten auf Serverbestätigung."
                    AppLanguage.EN -> "Some operations await server confirmation."
                    AppLanguage.RU -> "Некоторые операции ожидают подтверждения сервера."
                }
            }
        } catch (_: Exception) {
            // Keep all cached members and pending operations visible.
        }
    }

    LaunchedEffect(currentUser.accountId, currentUser.username, currentUser.workspaceId, selectedMemberId, serverStatus, refreshNonce) {
        try {
            selectedMemberId?.let { loadPoints(it) }
        } finally {
            if (refreshNonce > 0) onRefreshComplete(refreshNonce)
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
            pointTypes = pointTypes,
            workspaceId = currentUser.workspaceId,
            viewerId = currentUser.memberId,
            quickAmounts = quickAmounts,
            rewardCatalog = rewardCatalog.filter { reward ->
                canPurchaseRewards &&
                    serverStatus == ServerStatus.ONLINE &&
                    reward.active &&
                    reward.acquisitionMode != "REQUEST" &&
                    !reward.requiresApproval &&
                    pointTypes.any { type -> type.id == reward.pointTypeId }
            },
            onDismiss = {
                if (!processing) {
                    pointOperation = null
                }
            },
            onSubmit = { amount, description, pointType, selectedReward ->
                scope.launch {
                    processing = true
                    error = ""

                    try {
                        val kind = pointOperation!!.apiValue
                        if (selectedReward != null) {
                            check(kind == "spend" && canPurchaseRewards &&
                                serverStatus == ServerStatus.ONLINE) {
                                "Reward purchases require an online session"
                            }
                            rewardsRepository.purchaseForMember(
                                currentUser.workspaceId, selectedMember.id, selectedReward.id
                            )
                            pointOperation = null
                            loadPoints(selectedMember.id)
                        } else if (serverStatus != ServerStatus.ONLINE) {
                            check(canManage && kind != "spend") { "Offline spending is disabled" }
                            repository.queueOffline(
                                currentUser.workspaceId,
                                currentUser.memberId,
                                currentUser.accountId,
                                selectedMember.id,
                                pointType,
                                kind,
                                amount,
                                description.trim().ifBlank { null }
                            )
                            pendingOperations = repository.pending(
                                currentUser.workspaceId, currentUser.memberId, currentUser.accountId
                            )
                            syncMessage = when (language) {
                                AppLanguage.UK -> "Операцію збережено на телефоні. Вона очікує підтвердження сервера."
                                AppLanguage.DE -> "Vorgang lokal gespeichert und wartet auf Serverbestätigung."
                                AppLanguage.EN -> "Operation saved locally, awaiting server confirmation."
                                AppLanguage.RU -> "Операция сохранена на телефоне, ожидает подтверждения сервера."
                            }
                            quickAmounts.rememberSuccessful(
                                currentUser.workspaceId, currentUser.memberId,
                                kind, pointType.id, amount
                            )
                            pointOperation = null
                        } else {
                            repository.operate(
                                currentUser.workspaceId,
                                selectedMember.id,
                                pointType.id,
                                kind,
                                amount,
                                description.trim().ifBlank { null }
                            )
                            quickAmounts.rememberSuccessful(
                                currentUser.workspaceId, currentUser.memberId,
                                kind, pointType.id, amount
                            )
                            pointOperation = null
                            loadPoints(selectedMember.id)
                        }
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

            if (pendingOperations.isNotEmpty() || syncMessage.isNotBlank()) {
                item {
                    if (pendingOperations.isNotEmpty()) {
                        Text(when (language) {
                            AppLanguage.UK -> "Очікують синхронізації: ${pendingOperations.size}. У підтверджений баланс не враховані."
                            AppLanguage.DE -> "Ausstehende Vorgänge: ${pendingOperations.size}. Noch nicht im Kontostand enthalten."
                            AppLanguage.EN -> "Pending operations: ${pendingOperations.size}. Not yet included in confirmed balance."
                            AppLanguage.RU -> "Ожидают синхронизации: ${pendingOperations.size}. Не включены в подтверждённый баланс."
                        }, color = MaterialTheme.colorScheme.tertiary)
                    }
                    if (syncMessage.isNotBlank()) Text(syncMessage, color = MaterialTheme.colorScheme.tertiary)
                    if (pendingOperations.isNotEmpty() && canManage && serverStatus == ServerStatus.ONLINE) {
                        OutlinedButton(
                            enabled = !processing,
                            onClick = {
                                scope.launch {
                                    processing = true
                                    try {
                                        val synced = repository.synchronizePending(
                                            currentUser.workspaceId, currentUser.memberId, currentUser.accountId
                                        )
                                        pendingOperations = repository.pending(
                                            currentUser.workspaceId, currentUser.memberId, currentUser.accountId
                                        )
                                        memberBalances = members.mapNotNull { member ->
                                            repository.cachedConfirmedBalance(currentUser.workspaceId, member.id)
                                                ?.let { member.id to it }
                                        }.toMap()
                                        selectedMemberId?.let { loadPoints(it) }
                                        syncMessage = when (language) {
                                            AppLanguage.UK -> "Підтверджено операцій: $synced"
                                            AppLanguage.DE -> "Bestätigte Vorgänge: $synced"
                                            AppLanguage.EN -> "Confirmed operations: $synced"
                                            AppLanguage.RU -> "Подтверждено операций: $synced"
                                        }
                                    } catch (e: HttpException) {
                                        if (e.code() == 401) onUnauthorized()
                                        else syncMessage = when (language) {
                                            AppLanguage.UK -> "Сервер відхилив синхронізацію (HTTP ${e.code()}). Операції залишилися в черзі."
                                            AppLanguage.DE -> "Synchronisierung abgelehnt (HTTP ${e.code()}). Vorgänge bleiben gespeichert."
                                            AppLanguage.EN -> "Synchronization rejected (HTTP ${e.code()}). Operations remain queued."
                                            AppLanguage.RU -> "Синхронизация отклонена (HTTP ${e.code()}). Операции остались в очереди."
                                        }
                                    } catch (_: Exception) {
                                        syncMessage = when (language) {
                                            AppLanguage.UK -> "Синхронізація не вдалася. Чергу збережено."
                                            AppLanguage.DE -> "Synchronisierung fehlgeschlagen. Warteschlange gespeichert."
                                            AppLanguage.EN -> "Sync failed. Queue is saved."
                                            AppLanguage.RU -> "Синхронизация не удалась. Очередь сохранена."
                                        }
                                    } finally {
                                        processing = false
                                    }
                                }
                            }
                        ) {
                            Text(when (language) {
                                AppLanguage.UK -> "⟳ Синхронізувати"
                                AppLanguage.DE -> "⟳ Synchronisieren"
                                AppLanguage.EN -> "⟳ Sync now"
                                AppLanguage.RU -> "⟳ Синхронизировать"
                            })
                        }
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
                                    Text(member.name + (memberBalances[member.id]?.let { " · $it" } ?: ""))
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

                            if (loadingPoints) {
                                Text("⟳ " + when (language) {
                                    AppLanguage.UK -> "Оновлення балансу…"
                                    AppLanguage.DE -> "Kontostand wird aktualisiert…"
                                    AppLanguage.EN -> "Updating balance…"
                                    AppLanguage.RU -> "Обновление баланса…"
                                }, color = MaterialTheme.colorScheme.primary)
                            } else if (hasBalance && !offlineSnapshot) {
                                Text("✓ " + when (language) {
                                    AppLanguage.UK -> "Баланс оновлено"
                                    AppLanguage.DE -> "Kontostand aktualisiert"
                                    AppLanguage.EN -> "Balance updated"
                                    AppLanguage.RU -> "Баланс обновлён"
                                }, color = MaterialTheme.colorScheme.primary)
                            }
                            if (offlineSnapshot && hasBalance && !loadingPoints) {
                                Text(
                                    when (language) {
                                        AppLanguage.UK -> "Офлайн: показано останній збережений баланс. Списання недоступне."
                                        AppLanguage.DE -> "Offline: letzter gespeicherter Kontostand. Keine Ausgaben möglich."
                                        AppLanguage.EN -> "Offline: last saved balance. Spending unavailable."
                                        AppLanguage.RU -> "Офлайн: последний сохранённый баланс. Списание недоступно."
                                    },
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }

                            Text(
                                text.balance,
                                style =
                                    MaterialTheme.typography.labelLarge
                            )

                            Text(
                                if (!hasBalance && memberBalances[selectedMember.id] == null) {
                                    "—"
                                } else {
                                    formatPointAmount(
                                        memberBalances[selectedMember.id] ?: balance,
                                        language, "POINTS", text.points,
                                        currencyForms[pointTypes.firstOrNull { it.code.equals("POINTS", ignoreCase = true) }?.id]
                                            .orEmpty().map { com.olehkobylianskyi.familypoints.android.i18n.PointNameForms(it.language, it.one, it.few, it.many) }
                                    )
                                },
                                style =
                                    MaterialTheme.typography.headlineLarge
                            )

                            pendingOperations.filter { it.memberId == selectedMember.id }
                                .takeIf { it.isNotEmpty() }?.let { pending ->
                                    Text(when (language) {
                                        AppLanguage.UK -> "Очікують синхронізації: ${pending.size}"
                                        AppLanguage.DE -> "Ausstehende Vorgänge: ${pending.size}"
                                        AppLanguage.EN -> "Pending operations: ${pending.size}"
                                        AppLanguage.RU -> "Ожидают синхронизации: ${pending.size}"
                                    }, color = MaterialTheme.colorScheme.tertiary)
                                    pending.groupBy { it.pointTypeId }.forEach { (_, entries) ->
                                        val first = entries.first()
                                        val delta = entries.sumOf {
                                            if (it.type == "earn") it.amount.toLong() else -it.amount.toLong()
                                        }
                                        val label = if (first.pointTypeId == null) {
                                            when (language) {
                                                AppLanguage.UK -> "Валюта за замовчуванням (старий запис)"
                                                AppLanguage.DE -> "Standardwährung (älterer Eintrag)"
                                                AppLanguage.EN -> "Default currency (legacy entry)"
                                                AppLanguage.RU -> "Валюта по умолчанию (старая запись)"
                                            }
                                        } else localizedCurrencyName(
                                            language, first.pointTypeCode, first.pointTypeName ?: first.pointTypeCode.orEmpty()
                                        )
                                        Text("${if (delta > 0) "+" else ""}$delta · $label",
                                            color = MaterialTheme.colorScheme.tertiary)
                                    }
                                }

                            if (canManage && pointTypes.isEmpty()) {
                                Text(when (language) {
                                    AppLanguage.UK -> "Список валют не збережений. Відкрийте «Бали» з доступним сервером."
                                    AppLanguage.DE -> "Keine Währungen gespeichert. Öffnen Sie Punkte einmal online."
                                    AppLanguage.EN -> "Currencies not cached. Open Points once online."
                                    AppLanguage.RU -> "Список валют не сохранён. Откройте «Баллы» онлайн."
                                }, color = MaterialTheme.colorScheme.tertiary)
                            }
                            if (canManage && !processing && pointTypes.isNotEmpty()) {
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
                                        enabled = serverStatus == ServerStatus.ONLINE && hasBalance && !offlineSnapshot,
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text.history, style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { historyExpanded = !historyExpanded }) {
                        Text(when (language) {
                            AppLanguage.UK -> if (historyExpanded) "Згорнути" else "Показати"
                            AppLanguage.DE -> if (historyExpanded) "Zuklappen" else "Anzeigen"
                            AppLanguage.EN -> if (historyExpanded) "Collapse" else "Show"
                            AppLanguage.RU -> if (historyExpanded) "Свернуть" else "Показать"
                        } + " (" + history.size + ")")
                    }
                }
                if (historyExpanded) Text(text.historyDescription)
            }

            if (historyExpanded && loadingPoints) {
                item {
                    Text(text.loading)
                }
            } else if (historyExpanded && history.isEmpty()) {
                item {
                    Text(text.noHistory)
                }
            } else if (historyExpanded) {
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
                        ),
                        forms = currencyForms[pointTypes.firstOrNull {
                            it.code.equals(transaction.pointTypeCode, ignoreCase = true)
                        }?.id].orEmpty().map {
                            com.olehkobylianskyi.familypoints.android.i18n.PointNameForms(it.language, it.one, it.few, it.many)
                        }
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
    pointTypes: List<PointTypeResponse>,
    workspaceId: Long,
    viewerId: Long,
    quickAmounts: PointQuickAmountStore,
    rewardCatalog: List<RewardDefinitionSummary>,
    onDismiss: () -> Unit,
    onSubmit: (Int, String, PointTypeResponse, RewardDefinitionSummary?) -> Unit
) {
    val text = pointsStrings(language)
    var selectedTypeId by remember(operation, member.id, pointTypes) {
        mutableStateOf(
            pointTypes.firstOrNull { it.code.equals("POINTS", ignoreCase = true) }?.id
                ?: pointTypes.firstOrNull()?.id
        )
    }
    val selectedType = pointTypes.firstOrNull { it.id == selectedTypeId }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var rewardMenuExpanded by remember { mutableStateOf(false) }
    var rewardId by remember(operation, member.id) { mutableStateOf<Long?>(null) }
    val selectedReward = if (operation == PointOperation.SPEND) {
        rewardCatalog.firstOrNull { it.id == rewardId }
    } else null
    // Never silently switch a selected reward to a manual debit when the
    // catalog changes while the dialog is open.
    val rewardSelectionExpired = rewardId != null && selectedReward == null
    val rewardCurrency = selectedReward?.let { reward ->
        pointTypes.firstOrNull { it.id == reward.pointTypeId }
    }

    // A new dialog starts at 20, or the last successfully used amount.
    // Switching currencies restores the amount last used for that currency.
    var amount by remember(operation, member.id, selectedTypeId) {
        mutableStateOf(
            selectedTypeId?.let { id ->
                quickAmounts.suggested(workspaceId, viewerId, operation.apiValue, id).toString()
            } ?: "20"
        )
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
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                if (operation == PointOperation.SPEND) {
                    Text(
                        when (language) {
                            AppLanguage.UK -> "На що витрачаємо?"
                            AppLanguage.DE -> "Wofür werden Punkte ausgegeben?"
                            AppLanguage.EN -> "What is this spending for?"
                            AppLanguage.RU -> "На что тратим?"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !processing,
                            onClick = { rewardMenuExpanded = true }
                        ) {
                            Text(selectedReward?.title ?: when (language) {
                                AppLanguage.UK -> "Без нагороди · ручне списання"
                                AppLanguage.DE -> "Ohne Belohnung · manuelle Ausgabe"
                                AppLanguage.EN -> "No reward · manual spending"
                                AppLanguage.RU -> "Без награды · ручное списание"
                            })
                        }
                        DropdownMenu(
                            expanded = rewardMenuExpanded,
                            onDismissRequest = { rewardMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(when (language) {
                                    AppLanguage.UK -> "Без нагороди · ручне списання"
                                    AppLanguage.DE -> "Ohne Belohnung · manuelle Ausgabe"
                                    AppLanguage.EN -> "No reward · manual spending"
                                    AppLanguage.RU -> "Без награды · ручное списание"
                                }) },
                                onClick = { rewardId = null; rewardMenuExpanded = false }
                            )
                            rewardCatalog.forEach { reward ->
                                DropdownMenuItem(
                                    text = {
                                        Text(reward.title + " · " + reward.priceAmount +
                                            " " + localizedCurrencyName(
                                                language, reward.pointTypeCode, reward.pointTypeName
                                            ))
                                    },
                                    onClick = {
                                        rewardId = reward.id
                                        rewardMenuExpanded = false
                                        localError = ""
                                    }
                                )
                            }
                        }
                    }
                }

                if (rewardSelectionExpired) {
                    Text(
                        when (language) {
                            AppLanguage.UK -> "Нагорода більше недоступна. Оберіть іншу або ручне списання."
                            AppLanguage.DE -> "Belohnung nicht mehr verfügbar. Bitte erneut auswählen."
                            AppLanguage.EN -> "Reward is no longer available. Choose another or manual spending."
                            AppLanguage.RU -> "Награда больше недоступна. Выберите другую или ручное списание."
                        },
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (selectedReward != null) {
                    Text(when (language) {
                        AppLanguage.UK -> "Ціна: ${selectedReward.priceAmount} ${localizedCurrencyName(language, selectedReward.pointTypeCode, selectedReward.pointTypeName)}"
                        AppLanguage.DE -> "Preis: ${selectedReward.priceAmount} ${localizedCurrencyName(language, selectedReward.pointTypeCode, selectedReward.pointTypeName)}"
                        AppLanguage.EN -> "Price: ${selectedReward.priceAmount} ${localizedCurrencyName(language, selectedReward.pointTypeCode, selectedReward.pointTypeName)}"
                        AppLanguage.RU -> "Цена: ${selectedReward.priceAmount} ${localizedCurrencyName(language, selectedReward.pointTypeCode, selectedReward.pointTypeName)}"
                    }, color = MaterialTheme.colorScheme.primary)
                    Text(when (language) {
                        AppLanguage.UK -> "Буде оформлено покупку нагороди, а не окреме ручне списання. Сервер перевірить баланс та умови."
                        AppLanguage.DE -> "Ein Belohnungskauf statt manueller Ausgabe. Der Server prüft Guthaben und Bedingungen."
                        AppLanguage.EN -> "This creates a reward purchase, not a separate manual debit. The server checks balance and requirements."
                        AppLanguage.RU -> "Будет оформлена покупка награды вместо ручного списания. Сервер проверит баланс и условия."
                    }, style = MaterialTheme.typography.bodySmall)
                } else {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter(Char::isDigit)
                        localError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text.amount + (selectedType?.let {
                        " · " + localizedCurrencyName(language, it.code, it.name)
                    } ?: "")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    enabled = !processing
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !processing && (amount.toIntOrNull() ?: 0) > 10,
                        onClick = {
                            val current = amount.toIntOrNull() ?: 20
                            amount = (current - 10).toString()
                            localError = ""
                        }
                    ) { Text("−10") }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !processing && (amount.toIntOrNull() ?: 0) <= 99_990,
                        onClick = {
                            val current = amount.toIntOrNull() ?: 0
                            amount = (current + 10).toString()
                            localError = ""
                        }
                    ) { Text("+10") }
                }

                Text(
                    when (language) {
                        AppLanguage.UK -> "Валюта"
                        AppLanguage.DE -> "Währung"
                        AppLanguage.EN -> "Currency"
                        AppLanguage.RU -> "Валюта"
                    },
                    style = MaterialTheme.typography.labelMedium
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !processing && pointTypes.isNotEmpty(),
                        onClick = { currencyMenuExpanded = true }
                    ) {
                        Text(selectedType?.let {
                            localizedCurrencyName(language, it.code, it.name) + " (" + it.code + ") ▾"
                        } ?: "—")
                    }
                    DropdownMenu(
                        expanded = currencyMenuExpanded,
                        onDismissRequest = { currencyMenuExpanded = false }
                    ) {
                        pointTypes.forEach { pointType ->
                            DropdownMenuItem(
                                text = {
                                    Text(localizedCurrencyName(language, pointType.code, pointType.name)
                                        + " (" + pointType.code + ")")
                                },
                                onClick = {
                                    selectedTypeId = pointType.id
                                    currencyMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                }

                if (selectedReward == null) {
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

                }

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
                    if (rewardSelectionExpired) return@Button
                    val reward = selectedReward
                    if (reward != null) {
                        val currency = rewardCurrency
                        if (currency == null) {
                            localError = when (language) {
                                AppLanguage.UK -> "Валюта нагороди недоступна"
                                AppLanguage.DE -> "Belohnungswährung nicht verfügbar"
                                AppLanguage.EN -> "Reward currency unavailable"
                                AppLanguage.RU -> "Валюта награды недоступна"
                            }
                            return@Button
                        }
                        onSubmit(reward.priceAmount, "", currency, reward)
                        return@Button
                    }
                    val numericAmount = amount.toIntOrNull()

                    if (
                        numericAmount == null ||
                        numericAmount !in 1..100_000
                    ) {
                        localError = text.invalidAmount
                        return@Button
                    }

                    val currency = selectedType
                    if (currency == null) {
                        localError = when (language) {
                            AppLanguage.UK -> "Оберіть валюту"
                            AppLanguage.DE -> "Währung auswählen"
                            AppLanguage.EN -> "Select a currency"
                            AppLanguage.RU -> "Выберите валюту"
                        }
                        return@Button
                    }
                    onSubmit(numericAmount, description, currency, null)
                },
                enabled = !processing && !rewardSelectionExpired &&
                    (selectedReward?.let { rewardCurrency != null } ?: (selectedType != null))
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
    pointTypeLabel: String,
    forms: List<com.olehkobylianskyi.familypoints.android.i18n.PointNameForms>
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
                    // Numeric amount must use the same plural forms as the balance.
                    // Never append the currency display name directly (e.g. "10 бали").
                    if (forms.isNotEmpty() || transaction.pointTypeCode.equals("POINTS", ignoreCase = true) ||
                        transaction.pointTypeCode.isNullOrBlank()
                    ) {
                        val sign = if (transaction.amount > 0) "+" else ""
                        sign + formatPointAmount(
                            transaction.amount.toLong(),
                            language,
                            transaction.pointTypeCode ?: "POINTS",
                            pointTypeLabel,
                            forms
                        )
                    } else {
                        formatAmount(transaction.amount) + " " + pointTypeLabel
                    },
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
