package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Badge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.PointNameFormDto
import com.olehkobylianskyi.familypoints.android.data.PointTypeResponse
import com.olehkobylianskyi.familypoints.android.data.RewardDefinitionCreateRequest
import com.olehkobylianskyi.familypoints.android.data.RewardDefinitionSummary
import com.olehkobylianskyi.familypoints.android.data.RewardRequestReviewRequest
import com.olehkobylianskyi.familypoints.android.data.RewardRequirementRequest
import com.olehkobylianskyi.familypoints.android.data.RewardRequestSummary
import com.olehkobylianskyi.familypoints.android.data.RewardsPageData
import com.olehkobylianskyi.familypoints.android.data.RewardsRepository
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.PointNameForms
import com.olehkobylianskyi.familypoints.android.i18n.formatPointAmount
import com.olehkobylianskyi.familypoints.android.i18n.createTaskStrings
import com.olehkobylianskyi.familypoints.android.i18n.rewardsStrings
import com.olehkobylianskyi.familypoints.android.ui.components.AppHeader
import kotlinx.coroutines.launch
import retrofit2.HttpException

private enum class RewardsTab {
    CATALOG,
    REQUESTS,
    MANAGE
}

@Composable
fun RewardsScreen(
    language: AppLanguage,
    currentUser: CurrentUserResponse,
    repository: RewardsRepository,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (DashboardDestination) -> Unit,
    onUnauthorized: () -> Unit
) {
    val text = rewardsStrings(language)
    val createText = createTaskStrings(language)
    val scope = rememberCoroutineScope()

    val canManage =
        "MANAGE_REWARDS" in currentUser.permissions ||
            "ADMIN_OVERRIDE" in currentUser.permissions

    var tab by remember { mutableStateOf(RewardsTab.CATALOG) }
    var data by remember { mutableStateOf<RewardsPageData?>(null) }
    var nameForms by remember { mutableStateOf<Map<Long, List<PointNameFormDto>>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var processing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    var customTitle by remember { mutableStateOf("") }
    var customDescription by remember { mutableStateOf("") }

    var categoryName by remember { mutableStateOf("") }

    var rewardTitle by remember { mutableStateOf("") }
    var rewardDescription by remember { mutableStateOf("") }
    var rewardPointTypeId by remember { mutableStateOf<Long?>(null) }
    var rewardPrice by remember { mutableStateOf("20") }
    var minimumReputation by remember { mutableStateOf("") }
    var acquisitionMode by remember { mutableStateOf("DIRECT") }
    var rewardKind by remember { mutableStateOf("STANDARD") }
    var defaultDuration by remember { mutableStateOf("") }
    var requiresApproval by remember { mutableStateOf(false) }
    var selectedCategoryIds by remember {
        mutableStateOf<Set<Long>>(emptySet())
    }
    var beforeTaskIds by remember {
        mutableStateOf<Set<Long>>(emptySet())
    }
    var afterTaskIds by remember {
        mutableStateOf<Set<Long>>(emptySet())
    }
    var blockingMode by remember { mutableStateOf("NONE") }
    var blockedCategoryIds by remember {
        mutableStateOf<Set<Long>>(emptySet())
    }
    var blockedRewardIds by remember {
        mutableStateOf<Set<Long>>(emptySet())
    }

    var pointTypeMenuOpen by remember { mutableStateOf(false) }
    var acquisitionMenuOpen by remember { mutableStateOf(false) }
    var kindMenuOpen by remember { mutableStateOf(false) }
    var blockingMenuOpen by remember { mutableStateOf(false) }

    var reviewingRequestId by remember { mutableStateOf<Long?>(null) }
    var reviewPointTypeId by remember { mutableStateOf<Long?>(null) }
    var reviewPrice by remember { mutableStateOf("20") }
    var reviewMinReputation by remember { mutableStateOf("") }
    var reviewDuration by remember { mutableStateOf("") }

    fun pointTypeLabel(
        pointTypes: List<PointTypeResponse>,
        id: Long?,
        code: String?
    ): String {
        val type = pointTypes.firstOrNull { it.id == id }
        val value = type?.code ?: code ?: return ""
        return when (value.uppercase()) {
            "POINTS" -> createText.pointsName
            "COPPER" -> createText.copperName
            "SILVER" -> createText.silverName
            "GOLD" -> createText.goldName
            else -> type?.name ?: value
        }
    }

    fun requestStatusLabel(status: String): String =
        when (status) {
            "REQUESTED" -> text.waiting
            "APPROVED" -> text.approved
            "WAITING_REQUIREMENTS" -> text.waitingRequirements
            "READY_TO_PURCHASE" -> text.ready
            "PURCHASED" -> text.purchased
            "REJECTED" -> text.rejected
            "CANCELLED" -> text.cancelled
            else -> status
        }

    fun acquisitionLabel(mode: String): String =
        when (mode) {
            "REQUEST" -> text.requestOnly
            "DIRECT_OR_REQUEST" -> text.directOrRequest
            else -> text.direct
        }

    fun requirements(): List<RewardRequirementRequest> {
        val before = beforeTaskIds.map {
            RewardRequirementRequest(
                phase = "BEFORE_REWARD",
                requirementType = "TASK_COMPLETED",
                taskDefinitionId = it,
                timeScope = "TODAY",
                windowValue = null,
                description = null,
                required = true,
                blockingMode = "NONE",
                blockedCategoryIds = emptyList(),
                blockedRewardDefinitionIds = emptyList()
            )
        }

        val after = afterTaskIds.map {
            RewardRequirementRequest(
                phase = "AFTER_REWARD",
                requirementType = "TASK_COMPLETED",
                taskDefinitionId = it,
                timeScope = "SINCE_REWARD",
                windowValue = null,
                description = null,
                required = true,
                blockingMode = blockingMode,
                blockedCategoryIds =
                    if (blockingMode == "CATEGORIES") {
                        blockedCategoryIds.toList()
                    } else {
                        emptyList()
                    },
                blockedRewardDefinitionIds =
                    if (blockingMode == "SPECIFIC_REWARDS") {
                        blockedRewardIds.toList()
                    } else {
                        emptyList()
                    }
            )
        }

        return before + after
    }

    suspend fun load() {
        loading = true
        error = ""
        try {
            val loaded = repository.load(currentUser.workspaceId)
            data = loaded
            if (rewardPointTypeId == null) {
                rewardPointTypeId = loaded.pointTypes.firstOrNull()?.id
            }
            if (reviewPointTypeId == null) {
                reviewPointTypeId = loaded.pointTypes.firstOrNull()?.id
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
            loading = false
        }
    }

    suspend fun run(action: suspend () -> Unit) {
        processing = true
        error = ""
        try {
            action()
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

    LaunchedEffect(currentUser.workspaceId) {
        load()
    }

    LaunchedEffect(data?.pointTypes) {
        val types = data?.pointTypes.orEmpty()
        val received = mutableMapOf<Long, List<PointNameFormDto>>()
        types.forEach { type ->
            try {
                received[type.id] = repository.loadPointNameForms(currentUser.workspaceId, type.id)
            } catch (e: HttpException) {
                if (e.code() == 401) onUnauthorized()
            } catch (_: Exception) {
                // Older backend versions may not yet expose word-form settings.
            }
        }
        nameForms = received
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

        RewardsTabs(
            selected = tab,
            catalog = text.catalog,
            requests = text.requests,
            requestCount = data?.requests?.size ?: 0,
            manage = text.settings,
            canManage = canManage,
            onSelect = { tab = it }
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
                Text(text.subtitle)
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
                item { Text(text.loading) }
            }

            val current = data
            if (!loading && current != null) {
                if (current.obligations.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text.obligations,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                current.obligations.forEach { obligation ->
                                    Text(
                                        obligation.memberName + ": " +
                                            obligation.title + " · " +
                                            obligation.rewardTitle + " · " +
                                            obligation.blockingMode
                                    )
                                }
                            }
                        }
                    }
                }

                when (tab) {
                    RewardsTab.CATALOG -> {
                        if (current.rewards.isEmpty()) {
                            item { Text(text.emptyRewards) }
                        } else {
                            items(current.rewards, key = { it.id }) { reward ->
                                RewardCatalogCard(
                                    language = language,
                                    reward = reward,
                                    pointLabel = pointTypeLabel(
                                        current.pointTypes,
                                        reward.pointTypeId,
                                        reward.pointTypeCode
                                    ),
                                    pointCode = reward.pointTypeCode,
                                    configuredForms = reward.pointTypeId?.let { nameForms[it] }.orEmpty(),
                                    acquisitionLabel =
                                        acquisitionLabel(reward.acquisitionMode),
                                    processing = processing,
                                    onPurchase = {
                                        scope.launch {
                                            run {
                                                repository.purchaseReward(
                                                    currentUser.workspaceId,
                                                    reward.id
                                                )
                                            }
                                        }
                                    },
                                    onRequest = {
                                        scope.launch {
                                            run {
                                                repository.requestCatalogReward(
                                                    currentUser.workspaceId,
                                                    reward.id
                                                )
                                                tab = RewardsTab.REQUESTS
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text.customReward,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(text.customRewardHint)

                                    OutlinedTextField(
                                        value = customTitle,
                                        onValueChange = {
                                            customTitle = it.take(150)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(text.name) }
                                    )

                                    OutlinedTextField(
                                        value = customDescription,
                                        onValueChange = {
                                            customDescription = it.take(1000)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = {
                                            Text(text.customDescription)
                                        },
                                        minLines = 2
                                    )

                                    Button(
                                        onClick = {
                                            if (customTitle.trim().isBlank()) {
                                                return@Button
                                            }

                                            scope.launch {
                                                run {
                                                    repository.createCustomRequest(
                                                        currentUser.workspaceId,
                                                        customTitle.trim(),
                                                        customDescription
                                                            .trim()
                                                            .ifBlank { null }
                                                    )
                                                    customTitle = ""
                                                    customDescription = ""
                                                    tab = RewardsTab.REQUESTS
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !processing
                                    ) {
                                        Text(text.send)
                                    }
                                }
                            }
                        }
                    }

                    RewardsTab.REQUESTS -> {
                        if (current.requests.isEmpty()) {
                            item { Text(text.emptyRequests) }
                        } else {
                            items(current.requests, key = { it.id }) { request ->
                                RewardRequestCard(
                                    language = language,
                                    request = request,
                                    statusLabel =
                                        requestStatusLabel(request.status),
                                    pointLabel = pointTypeLabel(
                                        current.pointTypes,
                                        request.pointTypeId,
                                        request.pointTypeCode
                                    ),
                                    pointCode = request.pointTypeCode,
                                    configuredForms = request.pointTypeId?.let { nameForms[it] }.orEmpty(),
                                    canManage = canManage,
                                    processing = processing,
                                    reviewing =
                                        reviewingRequestId == request.id,
                                    pointTypes = current.pointTypes,
                                    rewards = current.rewards,
                                    reviewPointTypeId = reviewPointTypeId,
                                    reviewPrice = reviewPrice,
                                    reviewMinReputation =
                                        reviewMinReputation,
                                    reviewDuration = reviewDuration,
                                    onReviewPointTypeChange = {
                                        reviewPointTypeId = it
                                    },
                                    onReviewPriceChange = {
                                        reviewPrice = it
                                    },
                                    onReviewMinReputationChange = {
                                        reviewMinReputation = it
                                    },
                                    onReviewDurationChange = {
                                        reviewDuration = it
                                    },
                                    onBeginReview = {
                                        val reward = current.rewards
                                            .firstOrNull {
                                                it.id ==
                                                    request.rewardDefinitionId
                                            }
                                        reviewingRequestId = request.id
                                        reviewPointTypeId =
                                            request.pointTypeId
                                                ?: reward?.pointTypeId
                                                ?: current.pointTypes
                                                    .firstOrNull()
                                                    ?.id
                                        reviewPrice =
                                            (
                                                request.priceAmount
                                                    ?: reward?.priceAmount
                                                    ?: 20
                                                ).toString()
                                        reviewMinReputation =
                                            (
                                                request.minimumReputation
                                                    ?: reward
                                                        ?.minimumReputation
                                                )
                                                ?.toString()
                                                ?: ""
                                        reviewDuration =
                                            (
                                                request.durationMinutes
                                                    ?: reward
                                                        ?.defaultDurationMinutes
                                                )
                                                ?.toString()
                                                ?: ""
                                    },
                                    onCancelReview = {
                                        reviewingRequestId = null
                                    },
                                    onApprove = {
                                        val pointType =
                                            reviewPointTypeId
                                                ?: return@RewardRequestCard
                                        scope.launch {
                                            run {
                                                repository.approveRequest(
                                                    currentUser.workspaceId,
                                                    request.id,
                                                    RewardRequestReviewRequest(
                                                        pointTypeId =
                                                            pointType,
                                                        priceAmount =
                                                            reviewPrice
                                                                .toIntOrNull()
                                                                ?: 0,
                                                        minimumReputation =
                                                            reviewMinReputation
                                                                .toIntOrNull(),
                                                        durationMinutes =
                                                            reviewDuration
                                                                .toIntOrNull(),
                                                        requirements =
                                                            emptyList()
                                                    )
                                                )
                                                reviewingRequestId = null
                                            }
                                        }
                                    },
                                    onReject = {
                                        scope.launch {
                                            run {
                                                repository.rejectRequest(
                                                    currentUser.workspaceId,
                                                    request.id
                                                )
                                            }
                                        }
                                    },
                                    onRefresh = {
                                        scope.launch {
                                            run {
                                                repository.refreshRequest(
                                                    currentUser.workspaceId,
                                                    request.id
                                                )
                                            }
                                        }
                                    },
                                    onPurchase = {
                                        scope.launch {
                                            run {
                                                repository.purchaseRequest(
                                                    currentUser.workspaceId,
                                                    request.id
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    RewardsTab.MANAGE -> {
                        if (!canManage) {
                            item { Text(text.settings) }
                        } else {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement =
                                            Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text.categories,
                                            style =
                                                MaterialTheme.typography
                                                    .titleLarge
                                        )

                                        current.categories.forEach {
                                            Text("• " + it.name)
                                        }

                                        OutlinedTextField(
                                            value = categoryName,
                                            onValueChange = {
                                                categoryName = it.take(100)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = {
                                                Text(text.newCategory)
                                            }
                                        )

                                        OutlinedButton(
                                            onClick = {
                                                val value =
                                                    categoryName.trim()
                                                if (value.isBlank()) {
                                                    return@OutlinedButton
                                                }
                                                scope.launch {
                                                    run {
                                                        repository.createCategory(
                                                            currentUser
                                                                .workspaceId,
                                                            value,
                                                            current.categories
                                                                .size
                                                        )
                                                        categoryName = ""
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !processing
                                        ) {
                                            Text(text.add)
                                        }
                                    }
                                }
                            }

                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement =
                                            Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text.newReward,
                                            style =
                                                MaterialTheme.typography
                                                    .titleLarge
                                        )

                                        OutlinedTextField(
                                            value = rewardTitle,
                                            onValueChange = {
                                                rewardTitle = it.take(150)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = { Text(text.name) }
                                        )

                                        OutlinedTextField(
                                            value = rewardDescription,
                                            onValueChange = {
                                                rewardDescription =
                                                    it.take(1000)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = {
                                                Text(text.description)
                                            },
                                            minLines = 2
                                        )

                                        RewardPointTypeSelector(
                                            label = text.pointType,
                                            pointTypes = current.pointTypes,
                                            selectedId = rewardPointTypeId,
                                            expanded = pointTypeMenuOpen,
                                            onOpen = {
                                                pointTypeMenuOpen = true
                                            },
                                            onDismiss = {
                                                pointTypeMenuOpen = false
                                            },
                                            labelFor = {
                                                pointTypeLabel(
                                                    current.pointTypes,
                                                    it.id,
                                                    it.code
                                                )
                                            },
                                            onSelect = {
                                                rewardPointTypeId = it.id
                                                pointTypeMenuOpen = false
                                            }
                                        )

                                        OutlinedTextField(
                                            value = rewardPrice,
                                            onValueChange = {
                                                rewardPrice =
                                                    it.filter(Char::isDigit)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = { Text(text.price) },
                                            keyboardOptions =
                                                KeyboardOptions(
                                                    keyboardType =
                                                        KeyboardType.Number
                                                )
                                        )

                                        OutlinedTextField(
                                            value = minimumReputation,
                                            onValueChange = {
                                                minimumReputation =
                                                    it.filter(Char::isDigit)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = {
                                                Text(text.minReputation)
                                            },
                                            placeholder = {
                                                Text(text.noLimit)
                                            },
                                            keyboardOptions =
                                                KeyboardOptions(
                                                    keyboardType =
                                                        KeyboardType.Number
                                                )
                                        )

                                        SimpleSelector(
                                            label = text.acquisition,
                                            value = acquisitionLabel(
                                                acquisitionMode
                                            ),
                                            expanded = acquisitionMenuOpen,
                                            onOpen = {
                                                acquisitionMenuOpen = true
                                            },
                                            onDismiss = {
                                                acquisitionMenuOpen = false
                                            },
                                            options = listOf(
                                                "DIRECT" to text.direct,
                                                "REQUEST" to text.requestOnly,
                                                "DIRECT_OR_REQUEST" to
                                                    text.directOrRequest
                                            ),
                                            onSelect = {
                                                acquisitionMode = it
                                                acquisitionMenuOpen = false
                                            }
                                        )

                                        SimpleSelector(
                                            label = text.rewardKind,
                                            value =
                                                if (
                                                    rewardKind ==
                                                    "TIME_BASED"
                                                ) {
                                                    text.timeBased
                                                } else {
                                                    text.standard
                                                },
                                            expanded = kindMenuOpen,
                                            onOpen = {
                                                kindMenuOpen = true
                                            },
                                            onDismiss = {
                                                kindMenuOpen = false
                                            },
                                            options = listOf(
                                                "STANDARD" to text.standard,
                                                "TIME_BASED" to
                                                    text.timeBased
                                            ),
                                            onSelect = {
                                                rewardKind = it
                                                if (
                                                    it != "TIME_BASED"
                                                ) {
                                                    defaultDuration = ""
                                                }
                                                kindMenuOpen = false
                                            }
                                        )

                                        if (rewardKind == "TIME_BASED") {
                                            OutlinedTextField(
                                                value = defaultDuration,
                                                onValueChange = {
                                                    defaultDuration =
                                                        it.filter(
                                                            Char::isDigit
                                                        )
                                                },
                                                modifier =
                                                    Modifier.fillMaxWidth(),
                                                label = {
                                                    Text(
                                                        text.defaultDuration
                                                    )
                                                },
                                                keyboardOptions =
                                                    KeyboardOptions(
                                                        keyboardType =
                                                            KeyboardType.Number
                                                    )
                                            )
                                        }

                                        CheckOption(
                                            checked = requiresApproval,
                                            label = text.requiresApproval,
                                            onChange = {
                                                requiresApproval = it
                                            }
                                        )

                                        Text(
                                            text.categories,
                                            style =
                                                MaterialTheme.typography
                                                    .titleMedium
                                        )

                                        current.categories.forEach {
                                                category ->
                                            CheckOption(
                                                checked =
                                                    category.id in
                                                        selectedCategoryIds,
                                                label = category.name,
                                                onChange = {
                                                    selectedCategoryIds =
                                                        toggleLong(
                                                            selectedCategoryIds,
                                                            category.id
                                                        )
                                                }
                                            )
                                        }

                                        TaskChecklist(
                                            title = text.beforeConditions,
                                            tasks = current.tasks,
                                            selected = beforeTaskIds,
                                            onToggle = {
                                                beforeTaskIds =
                                                    toggleLong(
                                                        beforeTaskIds,
                                                        it
                                                    )
                                            }
                                        )

                                        TaskChecklist(
                                            title = text.afterConditions,
                                            tasks = current.tasks,
                                            selected = afterTaskIds,
                                            onToggle = {
                                                afterTaskIds =
                                                    toggleLong(
                                                        afterTaskIds,
                                                        it
                                                    )
                                            }
                                        )

                                        if (afterTaskIds.isNotEmpty()) {
                                            SimpleSelector(
                                                label = text.blocking,
                                                value = blockingLabel(
                                                    text,
                                                    blockingMode
                                                ),
                                                expanded =
                                                    blockingMenuOpen,
                                                onOpen = {
                                                    blockingMenuOpen = true
                                                },
                                                onDismiss = {
                                                    blockingMenuOpen = false
                                                },
                                                options =
                                                    blockingOptions(text),
                                                onSelect = {
                                                    blockingMode = it
                                                    blockingMenuOpen = false
                                                }
                                            )

                                            if (
                                                blockingMode ==
                                                "CATEGORIES"
                                            ) {
                                                current.categories.forEach {
                                                        category ->
                                                    CheckOption(
                                                        checked =
                                                            category.id in
                                                                blockedCategoryIds,
                                                        label = category.name,
                                                        onChange = {
                                                            blockedCategoryIds =
                                                                toggleLong(
                                                                    blockedCategoryIds,
                                                                    category.id
                                                                )
                                                        }
                                                    )
                                                }
                                            }

                                            if (
                                                blockingMode ==
                                                "SPECIFIC_REWARDS"
                                            ) {
                                                current.rewards.forEach {
                                                        reward ->
                                                    CheckOption(
                                                        checked =
                                                            reward.id in
                                                                blockedRewardIds,
                                                        label = reward.title,
                                                        onChange = {
                                                            blockedRewardIds =
                                                                toggleLong(
                                                                    blockedRewardIds,
                                                                    reward.id
                                                                )
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                val pointTypeId =
                                                    rewardPointTypeId
                                                        ?: return@Button
                                                if (
                                                    rewardTitle
                                                        .trim()
                                                        .isBlank()
                                                ) {
                                                    return@Button
                                                }

                                                val request =
                                                    RewardDefinitionCreateRequest(
                                                        title =
                                                            rewardTitle
                                                                .trim(),
                                                        description =
                                                            rewardDescription
                                                                .trim()
                                                                .ifBlank {
                                                                    null
                                                                },
                                                        pointTypeId =
                                                            pointTypeId,
                                                        priceAmount =
                                                            rewardPrice
                                                                .toIntOrNull()
                                                                ?: 0,
                                                        minimumReputation =
                                                            minimumReputation
                                                                .toIntOrNull(),
                                                        requiresApproval =
                                                            requiresApproval,
                                                        rewardKind =
                                                            rewardKind,
                                                        defaultDurationMinutes =
                                                            if (
                                                                rewardKind ==
                                                                "TIME_BASED"
                                                            ) {
                                                                defaultDuration
                                                                    .toIntOrNull()
                                                            } else {
                                                                null
                                                            },
                                                        acquisitionMode =
                                                            acquisitionMode,
                                                        categoryIds =
                                                            selectedCategoryIds
                                                                .toList(),
                                                        requirements =
                                                            requirements()
                                                    )

                                                scope.launch {
                                                    run {
                                                        repository.createReward(
                                                            currentUser
                                                                .workspaceId,
                                                            request
                                                        )
                                                        rewardTitle = ""
                                                        rewardDescription = ""
                                                        rewardPrice = "20"
                                                        minimumReputation = ""
                                                        selectedCategoryIds =
                                                            emptySet()
                                                        beforeTaskIds =
                                                            emptySet()
                                                        afterTaskIds =
                                                            emptySet()
                                                        blockingMode = "NONE"
                                                        blockedCategoryIds =
                                                            emptySet()
                                                        blockedRewardIds =
                                                            emptySet()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !processing
                                        ) {
                                            Text(text.createReward)
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    text.recentPurchases,
                                    style =
                                        MaterialTheme.typography.titleLarge
                                )
                            }

                            if (current.purchases.isEmpty()) {
                                item {
                                    Text(text.noPurchases)
                                }
                            } else {
                                items(
                                    current.purchases.take(10),
                                    key = { "purchase-" + it.id }
                                ) { purchase ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier =
                                                Modifier.padding(12.dp)
                                        ) {
                                            Text(
                                                purchase.rewardTitle,
                                                style =
                                                    MaterialTheme.typography
                                                        .titleMedium
                                            )
                                            Text(purchase.memberName)
                                            Text(
                                                purchase.priceAmount
                                                    .toString() +
                                                    " " +
                                                    pointTypeLabel(
                                                        current.pointTypes,
                                                        purchase.pointTypeId,
                                                        purchase.pointTypeCode
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RewardsTabs(
    selected: RewardsTab,
    catalog: String,
    requests: String,
    requestCount: Int,
    manage: String,
    canManage: Boolean,
    onSelect: (RewardsTab) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 2
    ) {
        RewardTabButton(
            selected = selected == RewardsTab.CATALOG,
            label = catalog,
            onClick = { onSelect(RewardsTab.CATALOG) },
            modifier = Modifier.fillMaxWidth(0.48f)
        )
        RewardTabButton(
            selected = selected == RewardsTab.REQUESTS,
            label = requests,
            count = requestCount,
            onClick = { onSelect(RewardsTab.REQUESTS) },
            modifier = Modifier.fillMaxWidth(0.48f)
        )
        if (canManage) RewardTabButton(
            selected = selected == RewardsTab.MANAGE,
            label = manage,
            onClick = { onSelect(RewardsTab.MANAGE) },
            modifier = Modifier.fillMaxWidth(0.48f)
        )
    }
}

@Composable
private fun RewardTabButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null
) {
    val content: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (count != null && count > 0) {
                Badge { Text(count.toString()) }
            }
        }
    }
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { content() }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { content() }
    }
}

@Composable
private fun RewardCatalogCard(
    language: AppLanguage,
    reward: RewardDefinitionSummary,
    pointLabel: String,
    pointCode: String?,
    configuredForms: List<PointNameFormDto>,
    acquisitionLabel: String,
    processing: Boolean,
    onPurchase: () -> Unit,
    onRequest: () -> Unit
) {
    val text = rewardsStrings(language)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                reward.title,
                style = MaterialTheme.typography.titleLarge
            )
            if (!reward.description.isNullOrBlank()) {
                Text(reward.description)
            }

            Text(
                formatPointAmount(reward.priceAmount.toLong(), language, pointCode, pointLabel,
                    configuredForms.map { PointNameForms(it.language, it.one, it.few, it.many) }),
                style = MaterialTheme.typography.titleMedium
            )

            reward.requirements.forEach { requirement ->
                Text(
                    (
                        if (requirement.phase == "AFTER_REWARD") {
                            text.afterReward
                        } else {
                            text.beforeReward
                        }
                        ) + ": " +
                        (
                            requirement.taskTitle
                                ?: requirement.description
                                ?: requirement.requirementType
                            )
                )
            }

            if (reward.categories.isNotEmpty()) {
                Text(
                    reward.categories.joinToString(" · ") {
                        it.name
                    }
                )
            }

            Text(acquisitionLabel)

            reward.minimumReputation?.let {
                Text(text.reputation + " ≥ " + it)
            }

            if (reward.rewardKind == "TIME_BASED") {
                Text(
                    text.timed + " · " +
                        (
                            reward.defaultDurationMinutes
                                ?.let { it.toString() + " min" }
                                ?: text.byAgreement
                            )
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (
                    reward.acquisitionMode != "REQUEST" &&
                    !reward.requiresApproval
                ) {
                    Button(
                        onClick = onPurchase,
                        enabled = !processing
                    ) {
                        Text(text.get)
                    }
                }

                if (
                    reward.acquisitionMode != "DIRECT" ||
                    reward.requiresApproval
                ) {
                    OutlinedButton(
                        onClick = onRequest,
                        enabled = !processing
                    ) {
                        Text(text.ask)
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardRequestCard(
    language: AppLanguage,
    request: RewardRequestSummary,
    statusLabel: String,
    pointLabel: String,
    pointCode: String?,
    configuredForms: List<PointNameFormDto>,
    canManage: Boolean,
    processing: Boolean,
    reviewing: Boolean,
    pointTypes: List<PointTypeResponse>,
    rewards: List<RewardDefinitionSummary>,
    reviewPointTypeId: Long?,
    reviewPrice: String,
    reviewMinReputation: String,
    reviewDuration: String,
    onReviewPointTypeChange: (Long) -> Unit,
    onReviewPriceChange: (String) -> Unit,
    onReviewMinReputationChange: (String) -> Unit,
    onReviewDurationChange: (String) -> Unit,
    onBeginReview: () -> Unit,
    onCancelReview: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRefresh: () -> Unit,
    onPurchase: () -> Unit
) {
    val text = rewardsStrings(language)
    var pointMenuOpen by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                request.title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(request.requestedByMemberName)
            Text(statusLabel)

            if (!request.description.isNullOrBlank()) {
                Text(request.description)
            }

            request.priceAmount?.let {
                Text(
                    text.price + ": " + formatPointAmount(it.toLong(), language, pointCode, pointLabel,
                        configuredForms.map { form -> PointNameForms(form.language, form.one, form.few, form.many) })
                )
            }

            request.durationMinutes?.let {
                Text(text.duration + ": " + it + " min")
            }

            request.requirements.forEach { requirement ->
                Text(
                    (
                        if (requirement.phase == "AFTER_REWARD") {
                            text.afterReward
                        } else {
                            text.beforeReward
                        }
                        ) + ": " +
                        (
                            requirement.taskTitle
                                ?: requirement.description
                                ?: requirement.requirementType
                            )
                )
            }

            if (
                canManage &&
                request.status == "REQUESTED" &&
                !reviewing
            ) {
                Button(
                    onClick = onBeginReview,
                    enabled = !processing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text.review)
                }
                OutlinedButton(
                    onClick = onReject,
                    enabled = !processing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text.reject)
                }
            }

            if (canManage && reviewing) {
                RewardPointTypeSelector(
                    label = text.pointType,
                    pointTypes = pointTypes,
                    selectedId = reviewPointTypeId,
                    expanded = pointMenuOpen,
                    onOpen = { pointMenuOpen = true },
                    onDismiss = { pointMenuOpen = false },
                    labelFor = { it.name },
                    onSelect = {
                        onReviewPointTypeChange(it.id)
                        pointMenuOpen = false
                    }
                )

                OutlinedTextField(
                    value = reviewPrice,
                    onValueChange = {
                        onReviewPriceChange(
                            it.filter(Char::isDigit)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text.price) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                OutlinedTextField(
                    value = reviewMinReputation,
                    onValueChange = {
                        onReviewMinReputationChange(
                            it.filter(Char::isDigit)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text.minReputation) },
                    placeholder = { Text(text.noLimit) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                if (
                    rewards.firstOrNull {
                        it.id == request.rewardDefinitionId
                    }?.rewardKind == "TIME_BASED"
                ) {
                    OutlinedTextField(
                        value = reviewDuration,
                        onValueChange = {
                            onReviewDurationChange(
                                it.filter(Char::isDigit)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text.duration) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        enabled = !processing
                    ) {
                        Text(text.approve)
                    }
                    OutlinedButton(onClick = onCancelReview) {
                        Text(text.cancelled)
                    }
                }
            }

            if (
                !canManage &&
                request.status == "WAITING_REQUIREMENTS"
            ) {
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !processing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text.refresh)
                }
            }

            if (
                !canManage &&
                request.status == "READY_TO_PURCHASE"
            ) {
                Button(
                    onClick = onPurchase,
                    enabled = !processing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text.purchaseReward)
                }
            }
        }
    }
}

@Composable
private fun RewardPointTypeSelector(
    label: String,
    pointTypes: List<PointTypeResponse>,
    selectedId: Long?,
    expanded: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    labelFor: (PointTypeResponse) -> String,
    onSelect: (PointTypeResponse) -> Unit
) {
    Column {
        Text(label)
        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                (
                    pointTypes.firstOrNull {
                        it.id == selectedId
                    }?.let(labelFor)
                        ?: "—"
                    ) + " ▾"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            pointTypes.forEach {
                DropdownMenuItem(
                    text = { Text(labelFor(it)) },
                    onClick = { onSelect(it) }
                )
            }
        }
    }
}

@Composable
private fun SimpleSelector(
    label: String,
    value: String,
    expanded: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    Column {
        Text(label)
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
            options.forEach {
                DropdownMenuItem(
                    text = { Text(it.second) },
                    onClick = { onSelect(it.first) }
                )
            }
        }
    }
}

@Composable
private fun CheckOption(
    checked: Boolean,
    label: String,
    onChange: (Boolean) -> Unit
) {
    Row {
        Checkbox(
            checked = checked,
            onCheckedChange = onChange
        )
        Text(
            label,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun TaskChecklist(
    title: String,
    tasks: List<com.olehkobylianskyi.familypoints.android.data.TaskDefinitionResponse>,
    selected: Set<Long>,
    onToggle: (Long) -> Unit
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium
        )
        tasks.forEach { task ->
            CheckOption(
                checked = task.id in selected,
                label = "№" + task.id + " · " + task.title,
                onChange = { onToggle(task.id) }
            )
        }
    }
}

private fun toggleLong(
    current: Set<Long>,
    id: Long
): Set<Long> =
    if (id in current) current - id else current + id

private fun blockingOptions(
    text: com.olehkobylianskyi.familypoints.android.i18n.RewardsStrings
): List<Pair<String, String>> =
    listOf(
        "NONE" to text.none,
        "WARN_ONLY" to text.warnOnly,
        "ALL_REWARDS" to text.allRewards,
        "CATEGORIES" to text.selectedCategories,
        "SPECIFIC_REWARDS" to text.selectedRewards
    )

private fun blockingLabel(
    text: com.olehkobylianskyi.familypoints.android.i18n.RewardsStrings,
    value: String
): String =
    blockingOptions(text)
        .firstOrNull { it.first == value }
        ?.second
        ?: value

