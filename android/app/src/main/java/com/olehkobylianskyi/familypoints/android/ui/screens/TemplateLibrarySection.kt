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
    canManage: Boolean,
    repository: TemplateRepository,
    onUnauthorized: () -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
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
    var creationDraft by remember(workspaceId) { mutableStateOf<com.google.gson.JsonObject?>(null) }
    var editing by remember(workspaceId) { mutableStateOf<PrivateTemplateDto?>(null) }
    var editTitle by remember(workspaceId) { mutableStateOf("") }
    var editJson by remember(workspaceId) { mutableStateOf("") }
    var confirmDelete by remember(workspaceId) { mutableStateOf(false) }
    var refresh by remember(workspaceId) { mutableIntStateOf(0) }
    var catalogLoaded by remember(workspaceId) { mutableStateOf(false) }
    var libraryLoaded by remember(workspaceId) { mutableStateOf(false) }
    var loadedDraftWorkspace by remember(workspaceId) { mutableStateOf<Long?>(null) }
    var previewCodes by remember(workspaceId) { mutableStateOf<List<String>>(emptyList()) }

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
            items(visibleTemplates, key = { it.code }) { item ->
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
                        Text(title)
                        Text(item.kind, style = MaterialTheme.typography.bodySmall)
                        if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodySmall)
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
                    ": " + result.alreadyImported.orEmpty().joinToString().ifBlank { "—" })
                Text(tl(language, "Будуть додані", "Wird hinzugefügt", "Will be added", "Будут добавлены") +
                    ": " + result.toImport.orEmpty().joinToString().ifBlank { "—" })
                Button(enabled = canManage && !busy && !loading && catalogLoaded && libraryLoaded && previewCodes == selected.sorted() && result.toImport.orEmpty().isNotEmpty(),
                    onClick = {
                        val codes = previewCodes.toList()
                        perform {
                            if (selected.sorted() != codes) return@perform
                            repository.importSelected(workspaceId, codes)
                            library = repository.privateTemplates(workspaceId)
                            selected = emptySet()
                            preview = null
                            previewCodes = emptyList()
                            notice = tl(language, "Імпортовано", "Importiert", "Imported", "Импортировано")
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
            items(library, key = { it.id }) { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title.orEmpty().ifBlank { "#${item.id}" })
                        Text(item.kind.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    }
                    if (canManage && item.kind in listOf("TASK", "REWARD")) {
                        TextButton(enabled = !busy && !loading, onClick = {
                            perform {
                                val draft = repository.creationDraft(workspaceId, item.id)
                                if (draft.get("templateId")?.asLong == item.id) {
                                    creationDraft = draft
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
                        editTitle = item.title.orEmpty()
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
                    val request = creationDraft?.getAsJsonObject("request")
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
            title = { Text(tl(language, "Редагувати шаблон", "Vorlage bearbeiten", "Edit template", "Редактировать шаблон")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(editTitle, { editTitle = it.take(200) },
                        enabled = !busy, modifier = Modifier.fillMaxWidth(),
                        label = { Text(tl(language, "Назва", "Titel", "Title", "Название")) })
                    OutlinedTextField(editJson, { editJson = it.take(30001) },
                        enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                        label = { Text(tl(language, "Визначення JSON", "Definition (JSON)", "Definition JSON", "Определение JSON")) })
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
                        repository.update(workspaceId, id, title, json)
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
