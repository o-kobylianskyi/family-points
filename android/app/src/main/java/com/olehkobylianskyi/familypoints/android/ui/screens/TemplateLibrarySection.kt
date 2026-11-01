package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.TemplateRepository
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.network.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

private fun tl(language: AppLanguage, uk: String, de: String, en: String, ru: String) =
    when (language) {
        AppLanguage.UK -> uk
        AppLanguage.DE -> de
        AppLanguage.EN -> en
        AppLanguage.RU -> ru
    }

@Composable
fun TemplateLibrarySection(
    language: AppLanguage,
    workspaceId: Long,
    accountId: Long,
    canManage: Boolean,
    canCreateTasks: Boolean,
    activationPermissions: Set<String>,
    repository: TemplateRepository,
    onUnauthorized: () -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val setupContext = androidx.compose.ui.platform.LocalContext.current
    val pendingStore = remember(setupContext) {
        com.olehkobylianskyi.familypoints.android.storage.TemplatePendingOperationStore(setupContext)
    }
    val tokens = remember(setupContext) {
        com.olehkobylianskyi.familypoints.android.storage.TokenStore(setupContext)
    }
    val receiptRepository = remember(tokens) {
        com.olehkobylianskyi.familypoints.android.data.TemplateReceiptRepository(tokens)
    }
    val finalizeCoordinator = remember(tokens, pendingStore) {
        com.olehkobylianskyi.familypoints.android.data.ConfirmedSetupFinalizeCoordinator(tokens, pendingStore)
    }
    val importCoordinator = remember(tokens, pendingStore, receiptRepository) {
        com.olehkobylianskyi.familypoints.android.data.ConfirmedTemplateImportCoordinator(tokens, pendingStore, receiptRepository)
    }
    val recovery = remember(tokens, pendingStore, receiptRepository) {
        com.olehkobylianskyi.familypoints.android.data.TemplateOperationRecovery(tokens, pendingStore, receiptRepository)
    }
    var pendingImports by remember(accountId, workspaceId) {
        mutableStateOf(pendingStore.pending(accountId, workspaceId).filter { it.kind in setOf("IMPORT", "FINALIZE") })
    }

    var economy by remember(workspaceId) { mutableStateOf<TemplateEconomySuggestionsDto?>(null) }
    var economyUnavailable by remember(workspaceId) { mutableStateOf(false) }
    var catalog by remember(workspaceId) { mutableStateOf<List<CatalogTemplateDto>>(emptyList()) }
    var packs by remember(workspaceId) { mutableStateOf<List<com.google.gson.JsonObject>>(emptyList()) }
    var packFilter by remember(workspaceId) { mutableStateOf("ALL") }
    var library by remember(workspaceId) { mutableStateOf<List<PrivateTemplateDto>>(emptyList()) }
    var selected by remember(workspaceId) { mutableStateOf<Set<String>>(emptySet()) }
    var preview by remember(workspaceId) { mutableStateOf<TemplateImportPreviewDto?>(null) }
    var loading by remember(workspaceId) { mutableStateOf(true) }
    var busy by remember(workspaceId) { mutableStateOf(false) }
    var error by remember(workspaceId) { mutableStateOf("") }
    var notice by remember(workspaceId) { mutableStateOf("") }
    var creationDraft by remember(accountId, workspaceId) { mutableStateOf<com.google.gson.JsonObject?>(null) }
    var activationText by remember(accountId, workspaceId) { mutableStateOf("") }
    var activationConfirm by remember(accountId, workspaceId) { mutableStateOf(false) }
    var activationAdvanced by remember(accountId, workspaceId) { mutableStateOf(false) }
    var pendingActivation by remember(accountId, workspaceId) {
        mutableStateOf(pendingStore.pending(accountId, workspaceId)
            .filter { it.kind in setOf("TASK", "REWARD") })
    }
    val activationCoordinator = remember(tokens, pendingStore, receiptRepository) {
        com.olehkobylianskyi.familypoints.android.data.ConfirmedTemplateActivationCoordinator(
            tokens, pendingStore, receiptRepository,
            com.olehkobylianskyi.familypoints.android.storage.TemplateOperationCipher()
        )
    }
    var editing by remember(workspaceId) { mutableStateOf<PrivateTemplateDto?>(null) }
    var editTitle by remember(workspaceId) { mutableStateOf("") }
    var editJson by remember(workspaceId) { mutableStateOf("") }
    var confirmDelete by remember(workspaceId) { mutableStateOf(false) }
    var refresh by remember(workspaceId) { mutableIntStateOf(0) }
    var catalogLoaded by remember(workspaceId) { mutableStateOf(false) }
    var libraryLoaded by remember(workspaceId) { mutableStateOf(false) }
    var loadedDraftWorkspace by remember(workspaceId) { mutableStateOf<Long?>(null) }
    var taskGroupExpanded by remember(workspaceId, packFilter) { mutableStateOf(false) }
    var rewardGroupExpanded by remember(workspaceId, packFilter) { mutableStateOf(false) }
    var libraryExpanded by remember(workspaceId) { mutableStateOf(false) }
    var previewCodes by remember(workspaceId) { mutableStateOf<List<String>>(emptyList()) }
    var setupReview by remember(workspaceId) { mutableStateOf<WorkspaceSetupReviewDto?>(null) }
    var setupReviewCodes by remember(workspaceId) { mutableStateOf<List<String>>(emptyList()) }
    var setupCanFinalize by remember(accountId, workspaceId) { mutableStateOf(false) }
    var showFinalizeConfirm by remember(accountId, workspaceId) { mutableStateOf(false) }


    fun failure(e: Exception) {
        if (e is HttpException && e.code() == 401) onUnauthorized()
        else error = e.message ?: "Template API failed"
    }

    LaunchedEffect(workspaceId, refresh) {
        loading = true
        error = ""
        selected = emptySet()
        preview = null
        creationDraft = null
        loadedDraftWorkspace = null
        editing = null
        confirmDelete = false
        previewCodes = emptyList()
        catalog = emptyList()
        economy = null
        economyUnavailable = false
        library = emptyList()
        packs = emptyList()
        packFilter = "ALL"
        catalogLoaded = false
        libraryLoaded = false
        try {
            val result = repository.catalog()
            catalog = result.templates.orEmpty()
            packs = result.packs.orEmpty()
            catalogLoaded = true
            // Suggestions are read-only and MUST NOT block template management.
            try {
                val suggestions = repository.economySuggestions()
                if (suggestions.schema == "familypoints.economy.suggestions.v1" &&
                    suggestions.version == 1 &&
                    suggestions.sourceCatalogVersion == result.version &&
                    suggestions.currencyCode == "POINTS" &&
                    !suggestions.autoApply && !suggestions.workspaceMutation &&
                    suggestions.approvalRequiredBeforeActivation) {
                    economy = suggestions
                } else {
                    economyUnavailable = true
                }
            } catch (_: Exception) {
                economyUnavailable = true
            }
            library = repository.privateTemplates(workspaceId)
            libraryLoaded = true
        } catch (e: Exception) { failure(e) }
        finally { loading = false }
    }

    fun perform(action: suspend () -> Unit) {
        if (busy || loading) return
        busy = true
        error = ""
        notice = ""
        scope.launch {
            try { action() } catch (e: Exception) { failure(e) }
            finally { busy = false }
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tl(language, "Бібліотека шаблонів", "Vorlagenbibliothek", "Template library", "Библиотека шаблонов"),
                style = MaterialTheme.typography.titleLarge)
            TextButton(enabled = !busy && !loading, onClick = onClose) { Text(tl(language, "Закрити", "Schließen", "Close", "Закрыть")) }
        }
        Text(tl(language, "Імпорт зберігає вибрані шаблони в бібліотеці сім'ї, але не створює активні завдання або нагороди.",
            "Import speichert Vorlagen nur in der Familienbibliothek, nicht als aktive Aufgaben oder Belohnungen.",
            "Import saves selections into your family library, not as active tasks or rewards.",
            "Импорт сохраняет шаблоны в семейной библиотеке без создания активных задач или наград."))
        if (canManage && !loading && catalogLoaded) {
            OutlinedButton(enabled = !busy, onClick = {
                setupReview = null
                setupCanFinalize = false
                val ordered = catalog.map { it.code }.filter { it in selected }
                setupReviewCodes = ordered
                perform {
                    val setup = com.olehkobylianskyi.familypoints.android.data.WorkspaceSetupRepository(
                        com.olehkobylianskyi.familypoints.android.storage.TokenStore(
                            setupContext
                        )
                    )
                    val state = setup.state(workspaceId)
                    val review = setup.review(workspaceId, state.revision, ordered)
                    check(review.selectedCodes == ordered && review.dataEpoch == state.dataEpoch &&
                        ordered == catalog.map { it.code }.filter { it in selected }) {
                        "Setup review changed; refresh before confirming"
                    }
                    setupCanFinalize = state.status != "COMPLETED" &&
                        listOf("MEMBERS", "MEMBER_GROUPS", "WORK_NODE_GROUPS", "CATALOG_SELECTION",
                            "TASK_REVIEW", "REWARD_REVIEW", "FINAL_REVIEW").all {
                            state.steps?.get(it) in listOf("COMPLETE", "SKIPPED")
                        }
                    setupReview = review
                }
            }) {
                Text(tl(language, "Перевірити вибір для налаштування",
                    "Auswahl für Einrichtung prüfen", "Review setup selection",
                    "Проверить выбор для настройки"))
            }
        }
        if (setupReview != null && setupReviewCodes ==
            catalog.map { it.code }.filter { it in selected }) {
            val result = setupReview!!
            AlertDialog(onDismissRequest = { setupReview = null },
                title = { Text(tl(language, "Перегляд налаштування",
                    "Einrichtung prüfen", "Setup review", "Проверка настройки")) },
                text = { Column {
                    Text("${result.selectedCount} / ${result.alreadyImportedCount} / ${result.toImportCount}")
                    Text(tl(language,
                        "Вибрано / вже імпортовано / до імпорту. Це лише перегляд: нічого не створено.",
                        "Ausgewählt / bereits importiert / zu importieren. Nur Vorschau, nichts erstellt.",
                        "Selected / imported / to import. Read-only review; nothing was created.",
                        "Выбрано / импортировано / к импорту. Только просмотр, ничего не создано."))
                } },
                confirmButton = { TextButton(onClick = { setupReview = null }) {
                    Text(tl(language, "Закрити", "Schließen", "Close", "Закрыть"))
                } })
        }
        if (canManage && setupCanFinalize && setupReview != null &&
            setupReviewCodes == catalog.map { it.code }.filter { it in selected } &&
            pendingImports.isEmpty()) {
            OutlinedButton(enabled = !busy, onClick = { showFinalizeConfirm = true }) {
                Text(tl(language, "Завершити налаштування та імпортувати",
                    "Einrichtung abschließen und importieren",
                    "Finalize setup and import", "Завершить настройку и импортировать"))
            }
        }
        if (showFinalizeConfirm && setupReview != null) {
            val reviewed = setupReview!!
            AlertDialog(
                onDismissRequest = { if (!busy) showFinalizeConfirm = false },
                title = { Text(tl(language, "Завершити налаштування?",
                    "Einrichtung abschließen?", "Finalize setup?", "Завершить настройку?")) },
                text = { Text(tl(language,
                    "Шаблони будуть імпортовані без активації завдань чи нагород.",
                    "Vorlagen werden ohne Aktivierung importiert.",
                    "Templates are imported without task or reward activation.",
                    "Шаблоны импортируются без активации заданий и наград.")) },
                confirmButton = {
                    Button(enabled = !busy && pendingImports.isEmpty(), onClick = {
                        val codes = setupReviewCodes.toList()
                        showFinalizeConfirm = false
                        perform {
                            val state = com.olehkobylianskyi.familypoints.android.data.WorkspaceSetupRepository(tokens)
                                .state(workspaceId)
                            check(state.revision == reviewed.revision &&
                                state.dataEpoch == reviewed.dataEpoch &&
                                codes == catalog.map { it.code }.filter { it in selected }) {
                                "Setup changed; review again"
                            }
                            when (val outcome = finalizeCoordinator.confirm(accountId, workspaceId, state.revision, codes)) {
                                is com.olehkobylianskyi.familypoints.android.data.ConfirmedSetupFinalizeCoordinator.Result.Confirmed -> {
                                    library = repository.privateTemplates(workspaceId)
                                    setupReview = null
                                    setupCanFinalize = false
                                    notice = tl(language, "Налаштування завершено", "Einrichtung abgeschlossen",
                                        "Setup completed", "Настройка завершена")
                                }
                                is com.olehkobylianskyi.familypoints.android.data.ConfirmedSetupFinalizeCoordinator.Result.Uncertain ->
                                    notice = tl(language, "Перевірте квитанцію", "Beleg prüfen",
                                        "Check receipt", "Проверьте квитанцию")
                                is com.olehkobylianskyi.familypoints.android.data.ConfirmedSetupFinalizeCoordinator.Result.Blocked ->
                                    error = outcome.reason
                            }
                            pendingImports = pendingStore.pending(accountId, workspaceId)
                                .filter { it.kind in setOf("IMPORT", "FINALIZE") }
                        }
                    }) { Text(tl(language, "Підтвердити", "Bestätigen", "Confirm", "Подтвердить")) }
                },
                dismissButton = { TextButton(onClick = { showFinalizeConfirm = false }) {
                    Text(tl(language, "Скасувати", "Abbrechen", "Cancel", "Отмена"))
                } }
            )
        }
        if (canManage && pendingImports.isNotEmpty()) {
            Text(tl(language, "Є незавершений імпорт. Новий імпорт заблоковано до перевірки.",
                "Ungeklärter Import. Neuer Import bis zur Prüfung gesperrt.",
                "Unresolved import. New imports are blocked pending verification.",
                "Незавершённый импорт. Новые импорты заблокированы до проверки."))
            OutlinedButton(enabled = !busy, onClick = {
                perform {
                    for (operation in pendingImports) recovery.check(accountId, workspaceId, operation)
                    pendingImports = pendingStore.pending(accountId, workspaceId).filter { it.kind in setOf("IMPORT", "FINALIZE") }
                    if (pendingImports.isEmpty()) library = repository.privateTemplates(workspaceId)
                    notice = if (pendingImports.isEmpty())
                        tl(language, "Квитанцію підтверджено", "Beleg bestätigt", "Receipt confirmed", "Квитанция подтверждена")
                    else tl(language, "Імпорт потребує перевірки. Повтору немає.", "Import muss geprüft werden. Kein Wiederholen.",
                        "Import needs review. No automatic retry.", "Импорт требует проверки. Без повтора.")
                }
            }) { Text(tl(language, "Перевірити квитанцію", "Beleg prüfen", "Check receipt", "Проверить квитанцию")) }
        }
        var showSetupStatus by remember(workspaceId) { mutableStateOf(false) }
        OutlinedButton(enabled = !busy && !loading, onClick = {
            showSetupStatus = !showSetupStatus
        }) {
            Text(tl(language, "Кроки налаштування сім'ї", "Schritte der Familieneinrichtung",
                "Family setup steps", "Шаги настройки семьи"))
        }
        if (showSetupStatus) {
            WorkspaceSetupOverviewSection(
                language = language, workspaceId = workspaceId,
                canManage = canManage,
                repository = remember(tokens) {
                    com.olehkobylianskyi.familypoints.android.data.WorkspaceSetupOverviewRepository(tokens)
                }
            )
        }
        var showBackupRegistry by remember(accountId, workspaceId) { mutableStateOf(false) }
        if (canManage) {
            OutlinedButton(enabled = !busy && !loading, onClick = {
                showBackupRegistry = !showBackupRegistry
            }) {
                Text(tl(language, "Резервні копії — журнал", "Sicherungsregister",
                    "Backup registry", "Реестр резервных копий"))
            }
            if (showBackupRegistry) {
                ProtectiveBackupMetadataSection(
                    language = language,
                    workspaceId = workspaceId,
                    repository = remember(tokens) {
                        com.olehkobylianskyi.familypoints.android.data.ProtectiveBackupMetadataRepository(tokens)
                    }
                )
            }
        }
        // Task folders are a distinct Backend entity; never map catalog labels into them.
        var showTaskFolders by remember(workspaceId) { mutableStateOf(false) }
        OutlinedButton(enabled = !loading && !busy, onClick = {
            showTaskFolders = !showTaskFolders
        }) {
            Text(tl(language, "Папки завдань", "Aufgabenordner",
                "Task folders", "Папки заданий"))
        }
        if (showTaskFolders) {
            WorkNodeGroupsSection(
                language = language, workspaceId = workspaceId,
                canCreateTasks = canCreateTasks,
                repository = remember(tokens) {
                    com.olehkobylianskyi.familypoints.android.data.WorkNodeGroupsRepository(tokens)
                }
            )
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error.isNotBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error)
            OutlinedButton(enabled = !busy, onClick = { refresh++ }) {
                Text(tl(language, "Повторити", "Wiederholen", "Retry", "Повторить"))
            }
        }
        if (notice.isNotBlank()) Text(notice, color = MaterialTheme.colorScheme.primary)
        val visibleTemplates = if (packFilter == "ALL") catalog else {
            val pack = packs.firstOrNull { it.get("code")?.asString == packFilter }
            val codes = pack?.getAsJsonArray("templateCodes")?.mapNotNull {
                if (it.isJsonPrimitive) it.asString else null
            }?.toSet().orEmpty()
            catalog.filter { it.code in codes }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(tl(language, "Рекомендовані шаблони", "Empfohlene Vorlagen", "Suggested templates", "Рекомендуемые шаблоны"),
                    style = MaterialTheme.typography.titleMedium)
                if (!loading && catalogLoaded && catalog.isEmpty()) Text(tl(language, "Каталог поки порожній.", "Der Katalog ist noch leer.",
                    "Catalog is empty for now.", "Каталог пока пуст."))
            }
            if (packs.isNotEmpty()) item {
                Text(tl(language, "Набори", "Pakete", "Packs", "Наборы"))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = packFilter == "ALL", onClick = { packFilter = "ALL" },
                            label = { Text(tl(language, "Усі", "Alle", "All", "Все")) })
                    }
                    items(packs, key = { it.get("code")?.asString ?: it.toString() }) { pack ->
                        val code = pack.get("code")?.asString.orEmpty()
                        val lang = when (language) {
                            AppLanguage.UK -> "uk"
                            AppLanguage.DE -> "de"
                            AppLanguage.EN -> "en"
                            AppLanguage.RU -> "ru"
                        }
                        val title = pack.getAsJsonObject("title")
                        val label = title?.get(lang)?.asString ?: title?.get("en")?.asString ?: code
                        FilterChip(selected = packFilter == code, onClick = { packFilter = code },
                            label = { Text(label) })
                    }
                }
            }
            if (packs.isNotEmpty() && packFilter != "ALL") item {
                val chosen = packs.firstOrNull { it.get("code")?.asString == packFilter }
                val langCode = when (language) {
                    AppLanguage.UK -> "uk"
                    AppLanguage.DE -> "de"
                    AppLanguage.EN -> "en"
                    AppLanguage.RU -> "ru"
                }
                val name = chosen?.getAsJsonObject("title")?.get(langCode)?.asString
                    ?: chosen?.getAsJsonObject("title")?.get("en")?.asString ?: packFilter
                Text(name, style = MaterialTheme.typography.titleSmall)
            }
            if (visibleTemplates.isNotEmpty()) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = canManage && !busy && catalogLoaded && libraryLoaded,
                        onClick = {
                            selected = selected + visibleTemplates.map { it.code }
                            preview = null
                            previewCodes = emptyList()
                        }) { Text(tl(language, "Вибрати видимі", "Sichtbare wählen", "Select visible", "Выбрать видимые")) }
                    OutlinedButton(enabled = canManage && !busy && catalogLoaded && libraryLoaded,
                        onClick = {
                            selected = selected - visibleTemplates.map { it.code }.toSet()
                            preview = null
                            previewCodes = emptyList()
                        }) { Text(tl(language, "Зняти вибір", "Auswahl aufheben", "Deselect visible", "Снять выбор")) }
                }
            }
            for (kind in listOf("TASK", "REWARD")) {
                val group = visibleTemplates.filter { it.kind == kind }
                if (group.isNotEmpty()) {
                    val expanded = if (kind == "TASK") taskGroupExpanded else rewardGroupExpanded
                    item(key = "section-" + kind) {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text((if (kind == "TASK")
                                tl(language, "Завдання", "Aufgaben", "Tasks", "Задания")
                                else tl(language, "Нагороди", "Belohnungen", "Rewards", "Награды")) +
                                " (" + group.size + ")", style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f))
                            if (group.size > 3) TextButton(onClick = {
                                if (kind == "TASK") taskGroupExpanded = !taskGroupExpanded
                                else rewardGroupExpanded = !rewardGroupExpanded
                            }) { Text(if (expanded)
                                tl(language, "Згорнути", "Weniger", "Show less", "Свернуть")
                                else tl(language, "Усі", "Alle", "Show all", "Все")) }
                        }
                    }
                    val displayedTemplates = if (expanded) group else group.take(3)
            items(displayedTemplates, key = { it.code }) { item ->
                val alreadyImported = library.any { it.catalogCode == item.code }
                val title = item.title?.get(when(language) {
                    AppLanguage.UK -> "uk"
                    AppLanguage.DE -> "de"
                    AppLanguage.EN -> "en"
                    AppLanguage.RU -> "ru"
                }) ?: item.title?.get("en") ?: item.code
                val lang = when(language) {
                    AppLanguage.UK -> "uk"
                    AppLanguage.DE -> "de"
                    AppLanguage.EN -> "en"
                    AppLanguage.RU -> "ru"
                }
                val description = item.description?.get(lang) ?: item.description?.get("en").orEmpty()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = item.code in selected, enabled = canManage && !busy && !loading && catalogLoaded && libraryLoaded,
                        onCheckedChange = { yes ->
                            selected = if (yes) selected + item.code else selected - item.code
                            preview = null
                            previewCodes = emptyList()
                        })
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TemplateCategoryIcon(
                                workspaceId = workspaceId,
                                key = templateCategoryMediaKey(item.kind, item.definition),
                                repository = repository
                            )
                            Text(title)
                        }
                        Text(if (item.kind == "TASK")
                            tl(language, "Завдання", "Aufgabe", "Task", "Задание")
                            else tl(language, "Нагорода", "Belohnung", "Reward", "Награда"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val recommendation = if (item.kind == "TASK") {
                            economy?.tasks?.firstOrNull { it.code == item.code }
                        } else {
                            economy?.rewards?.firstOrNull { it.code == item.code }
                        }
                        if (recommendation != null) {
                            val points = if (item.kind == "TASK")
                                recommendation.suggestedRewardPoints else recommendation.suggestedPricePoints
                            val suggestions = mutableListOf<String>()
                            if (item.kind == "TASK" && !recommendation.difficultySuggestion.isNullOrBlank()) {
                                suggestions += tl(language, "Складність", "Schwierigkeit", "Difficulty", "Сложность") +
                                    ": " + recommendation.difficultySuggestion
                            }
                            suggestions += if (points == null) {
                                tl(language, "Бали: не встановлено", "Punkte: nicht festgelegt",
                                    "Points: not set", "Баллы: не указаны")
                            } else {
                                tl(language, "Рекомендація", "Vorschlag", "Suggestion", "Рекомендация") +
                                    ": $points " + tl(language, "балів", "Punkte", "points", "баллов")
                            }
                            Text(suggestions.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(tl(language,
                                "Лише рекомендація. Перевіряє та підтверджує дорослий.",
                                "Nur ein Vorschlag. Erwachsene müssen prüfen und bestätigen.",
                                "Suggestion only. An adult must review and approve.",
                                "Только рекомендация. Требуется проверка и подтверждение взрослого."),
                                style = MaterialTheme.typography.labelSmall)
                        }
                        if (alreadyImported) Text(tl(language, "Уже в бібліотеці", "Bereits gespeichert", "Already in library", "Уже в библиотеке"), color = MaterialTheme.colorScheme.primary)
                        val duration = if (item.kind == "REWARD") {
                            item.definition?.get("defaultDurationMinutes")
                                ?.takeUnless { it.isJsonNull }?.asInt
                                ?: item.definition?.get("durationMinutes")
                                    ?.takeUnless { it.isJsonNull }?.asInt
                        } else null
                        if (duration != null && duration > 0) {
                            Text(
                                tl(language, "Тривалість: ", "Dauer: ", "Duration: ", "Длительность: ") +
                                    duration.toString() + " " + tl(language, "хв", "Min.", "min", "мин"),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (description.isNotBlank()) {
                            var descriptionExpanded by remember(item.code) { mutableStateOf(false) }
                            Text(description,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = if (descriptionExpanded) Int.MAX_VALUE else 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            if (description.length > 85) OutlinedButton(
                                onClick = { descriptionExpanded = !descriptionExpanded }
                            ) { Text(if (descriptionExpanded)
                                tl(language, "Згорнути опис", "Beschreibung einklappen", "Hide description", "Свернуть описание")
                                else tl(language, "Читати повністю", "Vollständig lesen", "Read more", "Читать полностью")) }
                        }
                    }
                }
            }
                }
            }
            item {
                Text("${selected.size} / ${catalog.size}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !busy && selected.isNotEmpty(), onClick = {
                        selected = emptySet()
                        preview = null
                        previewCodes = emptyList()
                    }) { Text(tl(language, "Очистити", "Leeren", "Clear", "Очистить")) }
                    Button(enabled = canManage && !busy && !loading && catalogLoaded && libraryLoaded && selected.isNotEmpty(), onClick = {
                        val codes = selected.sorted()
                        perform {
                            val response = repository.preview(workspaceId, codes)
                            if (selected.sorted() == codes) {
                                previewCodes = codes
                                preview = response
                            }
                        }
                    }) { Text(tl(language, "Перегляд імпорту", "Import prüfen", "Preview import", "Просмотр импорта")) }
                }
            }
            if (preview != null) item {
                val result = preview!!
                Text(tl(language, "Уже збережені", "Bereits vorhanden", "Already saved", "Уже сохранены") +
                    ": " + result.alreadyImported.orEmpty().size)
                Text(tl(language, "Будуть додані", "Wird hinzugefügt", "Will be added", "Будут добавлены") +
                    ": " + result.toImport.orEmpty().size)
                if (result.toImport.orEmpty().isEmpty()) {
                    Text(tl(language, "Усі вибрані шаблони вже збережено. Оберіть інший для імпорту.",
                        "Alle ausgewählten Vorlagen sind bereits gespeichert. Wählen Sie eine andere.",
                        "All selected templates are already saved. Choose another to import.",
                        "Все выбранные шаблоны уже сохранены. Выберите другой для импорта."),
                        style = MaterialTheme.typography.bodySmall)
                }
                Button(enabled = canManage && !busy && !loading && catalogLoaded && libraryLoaded && previewCodes == selected.sorted() && pendingImports.isEmpty() && result.toImport.orEmpty().isNotEmpty(),
                    onClick = {
                        val codes = previewCodes.toList()
                        perform {
                            if (selected.sorted() != codes) return@perform
                            when (val result = importCoordinator.confirm(accountId, workspaceId, codes)) {
                                is com.olehkobylianskyi.familypoints.android.data.ConfirmedTemplateImportCoordinator.Result.Confirmed -> {
                                    library = repository.privateTemplates(workspaceId)
                                    selected = emptySet()
                                    preview = null
                                    previewCodes = emptyList()
                                    notice = tl(language, "Імпортовано", "Importiert", "Imported", "Импортировано")
                                }
                                is com.olehkobylianskyi.familypoints.android.data.ConfirmedTemplateImportCoordinator.Result.Uncertain ->
                                    notice = tl(language, "Результат невідомий. Перевірте квитанцію.",
                                        "Ergebnis unklar. Beleg prüfen.", "Result uncertain. Check receipt.",
                                        "Результат неизвестен. Проверьте квитанцию.")
                                is com.olehkobylianskyi.familypoints.android.data.ConfirmedTemplateImportCoordinator.Result.Blocked ->
                                    error = result.reason
                            }
                            pendingImports = pendingStore.pending(accountId, workspaceId).filter { it.kind in setOf("IMPORT", "FINALIZE") }
                        }
                    }) { Text(tl(language, "Імпортувати вибране", "Auswahl importieren",
                        "Import selected", "Импортировать выбранное")) }
            }
            item {
                HorizontalDivider()
                Text(tl(language, "Мої шаблони", "Meine Vorlagen", "My templates", "Мои шаблоны"),
                    style = MaterialTheme.typography.titleMedium)
                if (!loading && libraryLoaded && library.isEmpty()) Text(tl(language, "Бібліотека порожня.", "Bibliothek ist leer.",
                    "Library is empty.", "Библиотека пуста."))
            }
            if (library.size > 3) item {
                TextButton(onClick = { libraryExpanded = !libraryExpanded }) {
                    Text(if (libraryExpanded)
                        tl(language, "Згорнути", "Weniger", "Show less", "Свернуть")
                        else tl(language, "Показати всі", "Alle anzeigen", "Show all", "Показать все") +
                            " (" + library.size + ")")
                }
            }
            items(if (libraryExpanded) library else library.take(3), key = { it.id }) { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val definition = remember(item.id, item.definitionJson) {
                            try {
                                com.google.gson.JsonParser.parseString(item.definitionJson.orEmpty())
                                    .takeIf { it.isJsonObject }?.asJsonObject
                            } catch (_: Exception) {
                                null
                            }
                        }
                        TemplateCategoryIcon(
                            workspaceId = workspaceId,
                            key = templateCategoryMediaKey(item.kind.orEmpty(), definition),
                            repository = repository
                        )
                        Column {
                            Text(item.title.orEmpty().ifBlank { "#${item.id}" })
                            Text(if (item.kind == "TASK") tl(language, "Завдання", "Aufgabe", "Task", "Задание")
                                else tl(language, "Нагорода", "Belohnung", "Reward", "Награда"),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (canManage && item.kind in listOf("TASK", "REWARD")) {
                        TextButton(enabled = !busy && !loading, onClick = {
                            perform {
                                val draft = repository.creationDraft(workspaceId, item.id)
                                if (draft.get("templateId")?.asLong == item.id) {
                                    creationDraft = draft
                                    activationText = draft.getAsJsonObject("request")?.toString().orEmpty()
                                    loadedDraftWorkspace = workspaceId
                                } else {
                                    error = tl(language, "Відповідь сервера не відповідає вибраному шаблону.",
                                        "Serverantwort passt nicht zur gewählten Vorlage.",
                                        "Server response does not match the selected template.",
                                        "Ответ сервера не соответствует выбранному шаблону.")
                                }
                            }
                        }) { Text(tl(language, "Чернетка", "Entwurf", "Draft", "Черновик")) }
                    }
                    if (canManage) TextButton(enabled = !busy, onClick = {
                        editing = item
                        editTitle = item.title.orEmpty().take(150)
                        editJson = item.definitionJson.orEmpty()
                        confirmDelete = false
                    }) { Text(tl(language, "Змінити", "Bearbeiten", "Edit", "Изменить")) }
                }
            }
        }
    }
    if (creationDraft != null && loadedDraftWorkspace == workspaceId) {
        AlertDialog(
            onDismissRequest = { creationDraft = null },
            title = { Text(tl(language, "Чернетка створення", "Erstellungsentwurf",
                "Creation draft", "Черновик создания")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tl(language,
                        "Лише перегляд. Учасника, валюту та обов'язкові поля треба вказати перед створенням. Нічого ще не активовано.",
                        "Nur Vorschau. Mitglied, Währung und Pflichtfelder sind vor dem Erstellen nötig. Nichts aktiviert.",
                        "Preview only. Member, currency and required fields must be supplied before creation. Nothing activated.",
                        "Только просмотр. Для создания нужны участник, валюта и обязательные поля. Ничего не активировано."))
                    val draft = creationDraft
                    val suggestion = draft?.getAsJsonObject("economySuggestion")
                    // A suggestion is typed by its draft kind. Never take an amount
                    // from the other entity kind or turn missing/invalid values into zero.
                    val draftKind = draft?.get("kind")?.takeUnless { it.isJsonNull }?.asString
                    val amountField = when (draftKind) {
                        "TASK" -> "suggestedRewardPoints"
                        "REWARD" -> "suggestedPricePoints"
                        else -> null
                    }
                    val suggestedAmount = applicableEconomyDraftAmount(draft, canManage)
                    val draftId = draft?.get("templateId")?.takeUnless { it.isJsonNull }?.asLong
                    val mayApply = suggestedAmount != null && draftId != null
                    if (mayApply) {
                        Text(tl(language,
                            "Рекомендація: $suggestedAmount балів. Застосування змінює лише цю чернетку.",
                            "Vorschlag: $suggestedAmount Punkte. Nur dieser Entwurf wird geändert.",
                            "Suggestion: $suggestedAmount points. Only this draft changes.",
                            "Рекомендация: $suggestedAmount баллов. Меняется только этот черновик."))
                        OutlinedButton(enabled = !busy, onClick = {
                            val id = draftId ?: return@OutlinedButton
                            perform {
                                val applied = repository.applyEconomySuggestion(workspaceId, id)
                                if (loadedDraftWorkspace != workspaceId ||
                                    creationDraft?.get("templateId")?.asLong != id ||
                                    applied.get("templateId")?.asLong != id ||
                                    applied.get("kind")?.asString != draftKind ||
                                    !applied.has("request") || !applied.get("request").isJsonObject ||
                                    applied.get("suggestionAppliedToDraft")?.asBoolean != true ||
                                    applied.get("requiresConfirmation")?.asBoolean != true) {
                                    throw SecurityException("Unexpected economy draft response")
                                }
                                creationDraft = applied
                                activationText = applied.getAsJsonObject("request")?.toString().orEmpty()
                            }
                        }) {
                            Text(tl(language, "Застосувати до чернетки", "Auf Entwurf anwenden",
                                "Apply to draft", "Применить к черновику"))
                        }
                    }
                    if (draft?.get("suggestionAppliedToDraft")?.asBoolean == true) {
                        Text(tl(language, "Рекомендацію застосовано лише до чернетки. Нічого не створено.",
                            "Nur im Entwurf übernommen. Nichts erstellt.",
                            "Applied to draft only. Nothing was created.",
                            "Применено только к черновику. Ничего не создано."),
                            color = MaterialTheme.colorScheme.primary)
                    }
                    val request = creationDraft?.getAsJsonObject("request")
                    val allowedToActivate = when(draftKind) {
                        "TASK" -> "CREATE_TASKS" in activationPermissions
                        "REWARD" -> "MANAGE_REWARDS" in activationPermissions
                        else -> false
                    }
                    ActivationRequestFieldsEditor(
                        language = language,
                        kind = draftKind.orEmpty(),
                        jsonText = activationText,
                        enabled = !busy && allowedToActivate,
                        onChange = { activationText = it }
                    )
                    TextButton(onClick = { activationAdvanced = !activationAdvanced }) {
                        Text(if (activationAdvanced)
                            tl(language, "Сховати JSON", "JSON verbergen", "Hide JSON", "Скрыть JSON")
                        else tl(language, "Розширений JSON", "Erweiterter JSON", "Advanced JSON", "Расширенный JSON"))
                    }
                    if (activationAdvanced) OutlinedTextField(
                        value = activationText,
                        onValueChange = { activationText = it.take(30000) },
                        enabled = !busy && allowedToActivate,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 8,
                        label = { Text(tl(language, "Повний JSON запиту створення",
                            "Vollständiger JSON-Erstellungsantrag",
                            "Full creation request JSON", "Полный JSON запроса создания")) }
                    )
                    val reviewedActivation = remember(activationText) {
                        try {
                            com.google.gson.JsonParser.parseString(activationText)
                                .takeIf { it.isJsonObject }?.asJsonObject
                        } catch (_: Exception) { null }
                    }
                    val activationReady = allowedToActivate && draftId != null &&
                        reviewedActivation != null && reviewedActivation.entrySet().isNotEmpty() &&
                        reviewedActivation.entrySet().all { it.value != null } &&
                        pendingActivation.none { it.templateId == draftId && it.kind == draftKind }
                    if (pendingActivation.isNotEmpty()) {
                        Text(tl(language,
                            "Невизначена активація: спочатку перевірте квитанцію. Нове підтвердження заблоковано.",
                            "Ungeklärte Aktivierung: zuerst Beleg prüfen. Neue Bestätigung gesperrt.",
                            "Activation pending: check receipt before a new confirmation.",
                            "Активация не подтверждена: сначала проверьте квитанцию."),
                            color = MaterialTheme.colorScheme.error)
                        OutlinedButton(enabled = !busy, onClick = {
                            perform {
                                for (operation in pendingActivation) recovery.check(accountId, workspaceId, operation)
                                pendingActivation = pendingStore.pending(accountId, workspaceId)
                                    .filter { it.kind in setOf("TASK", "REWARD") }
                                if (pendingActivation.isEmpty()) {
                                    notice = tl(language, "Квитанцію підтверджено", "Beleg bestätigt",
                                        "Receipt confirmed", "Квитанция подтверждена")
                                }
                            }
                        }) { Text(tl(language, "Перевірити квитанцію", "Beleg prüfen",
                            "Check receipt", "Проверить квитанцию")) }
                    }
                    Button(enabled = !busy && activationReady && pendingActivation.isEmpty(),
                        onClick = { activationConfirm = true }) {
                        Text(tl(language, "Перевірити та створити", "Prüfen und erstellen",
                            "Review and create", "Проверить и создать"))
                    }
                    if (activationConfirm && reviewedActivation != null && draftId != null && draftKind != null) {
                        val exactRequest = reviewedActivation.deepCopy()
                        AlertDialog(
                            onDismissRequest = { activationConfirm = false },
                            title = { Text(tl(language, "Підтвердити створення?",
                                "Erstellung bestätigen?", "Confirm creation?", "Подтвердить создание?")) },
                            text = { Text(exactRequest.toString().take(1200)) },
                            confirmButton = { Button(onClick = {
                                activationConfirm = false
                                perform {
                                    when (val outcome = activationCoordinator.confirm(
                                        accountId, workspaceId, draftId, draftKind,
                                        activationPermissions, exactRequest
                                    )) {
                                        is com.olehkobylianskyi.familypoints.android.data.ConfirmedTemplateActivationCoordinator.Result.Confirmed -> {
                                            creationDraft = null
                                            notice = tl(language, "Створено", "Erstellt", "Created", "Создано")
                                        }
                                        is com.olehkobylianskyi.familypoints.android.data.ConfirmedTemplateActivationCoordinator.Result.Uncertain ->
                                            notice = tl(language, "Результат невідомий. Перевірте квитанцію.",
                                                "Ergebnis unklar. Beleg prüfen.", "Result unknown. Check receipt.",
                                                "Результат неизвестен. Проверьте квитанцию.")
                                        is com.olehkobylianskyi.familypoints.android.data.ConfirmedTemplateActivationCoordinator.Result.Blocked ->
                                            error = outcome.reason
                                    }
                                    pendingActivation = pendingStore.pending(accountId, workspaceId)
                                        .filter { it.kind in setOf("TASK", "REWARD") }
                                }
                            }) { Text(tl(language, "Підтвердити", "Bestätigen", "Confirm", "Подтвердить")) } },
                            dismissButton = { TextButton(onClick = { activationConfirm = false }) {
                                Text(tl(language, "Скасувати", "Abbrechen", "Cancel", "Отмена"))
                            } }
                        )
                    }
                    if (request == null) {
                        Text(tl(language, "Сервер не повернув поля чернетки.",
                            "Der Server hat keine Entwurfsfelder geliefert.",
                            "The server did not return draft fields.",
                            "Сервер не вернул поля черновика."),
                            color = MaterialTheme.colorScheme.error)
                    } else {
                        val mandatory = request.entrySet().filter { (_, value) ->
                            value == null || value.isJsonNull
                        }.map { it.key }
                        if (mandatory.isNotEmpty()) {
                            Text(tl(language, "Потрібно уточнити: ", "Noch auszufüllen: ",
                                "Still required: ", "Нужно заполнить: ") +
                                mandatory.joinToString(", "),
                                color = MaterialTheme.colorScheme.error)
                        }
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(request.entrySet().toList()) { entry ->
                                Column {
                                    Text(entry.key, style = MaterialTheme.typography.labelMedium)
                                    Text(if (entry.value == null || entry.value.isJsonNull) "—"
                                        else if (entry.value.isJsonPrimitive) entry.value.asString
                                        else entry.value.toString().take(300),
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { creationDraft = null }) {
                    Text(tl(language, "Закрити", "Schließen", "Close", "Закрыть"))
                }
            }
        )
    }
    val current = editing
    if (current != null) {
        val validJson = remember(editJson) {
            editJson.length <= 30000 && try {
                val parsed = com.google.gson.JsonParser.parseString(editJson)
                parsed.isJsonObject
            } catch (_: Exception) { false }
        }
        AlertDialog(
            onDismissRequest = { if (!busy) editing = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(tl(language, "Параметри шаблону", "Vorlageneinstellungen", "Template settings", "Параметры шаблона")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (current.kind == "TASK") tl(language, "Завдання", "Aufgabe", "Task", "Задание")
                        else tl(language, "Нагорода", "Belohnung", "Reward", "Награда"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(editTitle, { editTitle = it.take(150) },
                        enabled = !busy, modifier = Modifier.fillMaxWidth(),
                        label = { Text(tl(language, "Назва", "Titel", "Title", "Название")) })
                    TemplateFieldsEditor(
                        language = language,
                        kind = current.kind.orEmpty(),
                        jsonText = editJson,
                        enabled = !busy,
                        onChange = { editJson = it }
                    )
                    if (!validJson) Text(tl(language, "Потрібен коректний JSON-об’єкт до 30 000 символів.",
                        "Gültiges JSON-Objekt bis 30.000 Zeichen erforderlich.",
                        "Valid JSON object up to 30,000 characters required.",
                        "Требуется корректный JSON-объект до 30 000 символов."),
                        color = MaterialTheme.colorScheme.error)
                    TextButton(enabled = !busy, onClick = { confirmDelete = true }) {
                        Text(tl(language, "Видалити шаблон", "Vorlage löschen", "Delete template", "Удалить шаблон"))
                    }
                }
            },
            confirmButton = {
                Button(enabled = canManage && !busy && editTitle.isNotBlank() && validJson, onClick = {
                    val id = current.id
                    val title = editTitle.trim()
                    val json = editJson
                    perform {
                        repository.update(workspaceId, id, title, json, current.kind.orEmpty())
                        library = repository.privateTemplates(workspaceId)
                        editing = null
                        notice = tl(language, "Збережено", "Gespeichert", "Saved", "Сохранено")
                    }
                }) { Text(tl(language, "Зберегти", "Speichern", "Save", "Сохранить")) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { editing = null }) {
                Text(tl(language, "Скасувати", "Abbrechen", "Cancel", "Отмена"))
            } }
        )
        if (confirmDelete) AlertDialog(
            onDismissRequest = { if (!busy) confirmDelete = false },
            title = { Text(tl(language, "Видалити шаблон?", "Vorlage löschen?", "Delete template?", "Удалить шаблон?")) },
            text = { Text(current.title.orEmpty()) },
            confirmButton = { Button(enabled = canManage && !busy, onClick = {
                perform {
                    repository.delete(workspaceId, current.id)
                    library = repository.privateTemplates(workspaceId)
                    confirmDelete = false
                    editing = null
                }
            }) { Text(tl(language, "Видалити", "Löschen", "Delete", "Удалить")) } },
            dismissButton = { TextButton(enabled = !busy, onClick = { confirmDelete = false }) {
                Text(tl(language, "Скасувати", "Abbrechen", "Cancel", "Отмена"))
            } }
        )
    }
}
