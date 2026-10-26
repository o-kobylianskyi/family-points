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
    var library by remember(workspaceId) { mutableStateOf<List<PrivateTemplateDto>>(emptyList()) }
    var selected by remember(workspaceId) { mutableStateOf<Set<String>>(emptySet()) }
    var preview by remember(workspaceId) { mutableStateOf<TemplateImportPreviewDto?>(null) }
    var loading by remember(workspaceId) { mutableStateOf(true) }
    var busy by remember(workspaceId) { mutableStateOf(false) }
    var error by remember(workspaceId) { mutableStateOf("") }
    var notice by remember(workspaceId) { mutableStateOf("") }
    var refresh by remember(workspaceId) { mutableIntStateOf(0) }

    fun failure(e: Exception) {
        if (e is HttpException && e.code() == 401) onUnauthorized()
        else error = e.message ?: "Template API failed"
    }

    LaunchedEffect(workspaceId, refresh) {
        loading = true
        error = ""
        selected = emptySet()
        preview = null
        catalog = emptyList()
        library = emptyList()
        try {
            catalog = repository.catalog().templates.orEmpty()
            library = repository.privateTemplates(workspaceId)
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
            TextButton(onClick = onClose) { Text(tl(language, "Закрити", "Schließen", "Close", "Закрыть")) }
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
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(tl(language, "Рекомендовані шаблони", "Empfohlene Vorlagen", "Suggested templates", "Рекомендуемые шаблоны"),
                    style = MaterialTheme.typography.titleMedium)
                if (!loading && catalog.isEmpty()) Text(tl(language, "Каталог поки порожній.", "Der Katalog ist noch leer.",
                    "Catalog is empty for now.", "Каталог пока пуст."))
            }
            items(catalog, key = { it.code }) { item ->
                val title = item.title?.get(when(language) {
                    AppLanguage.UK -> "uk"
                    AppLanguage.DE -> "de"
                    AppLanguage.EN -> "en"
                    AppLanguage.RU -> "ru"
                }) ?: item.title?.get("en") ?: item.code
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = item.code in selected, enabled = canManage && !busy && !loading,
                        onCheckedChange = { yes ->
                            selected = if (yes) selected + item.code else selected - item.code
                            preview = null
                        })
                    Column {
                        Text(title)
                        Text(item.kind, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Text("${selected.size} / ${catalog.size}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !busy && selected.isNotEmpty(), onClick = {
                        selected = emptySet()
                        preview = null
                    }) { Text(tl(language, "Очистити", "Leeren", "Clear", "Очистить")) }
                    Button(enabled = canManage && !busy && !loading && selected.isNotEmpty(), onClick = {
                        val codes = selected.sorted()
                        perform { preview = repository.preview(workspaceId, codes) }
                    }) { Text(tl(language, "Перегляд імпорту", "Import prüfen", "Preview import", "Просмотр импорта")) }
                }
            }
            if (preview != null) item {
                val result = preview!!
                Text(tl(language, "Уже збережені", "Bereits vorhanden", "Already saved", "Уже сохранены") +
                    ": " + result.alreadyImported.orEmpty().joinToString().ifBlank { "—" })
                Text(tl(language, "Будуть додані", "Wird hinzugefügt", "Will be added", "Будут добавлены") +
                    ": " + result.toImport.orEmpty().joinToString().ifBlank { "—" })
                Button(enabled = canManage && !busy && !loading && result.toImport.orEmpty().isNotEmpty(),
                    onClick = {
                        val codes = selected.sorted()
                        perform {
                            repository.import(workspaceId, codes)
                            library = repository.privateTemplates(workspaceId)
                            selected = emptySet()
                            preview = null
                            notice = tl(language, "Імпортовано", "Importiert", "Imported", "Импортировано")
                        }
                    }) { Text(tl(language, "Імпортувати вибране", "Auswahl importieren",
                        "Import selected", "Импортировать выбранное")) }
            }
            item {
                HorizontalDivider()
                Text(tl(language, "Мої шаблони", "Meine Vorlagen", "My templates", "Мои шаблоны"),
                    style = MaterialTheme.typography.titleMedium)
                if (!loading && library.isEmpty()) Text(tl(language, "Бібліотека порожня.", "Bibliothek ist leer.",
                    "Library is empty.", "Библиотека пуста."))
            }
            items(library, key = { it.id }) { item ->
                Column {
                    Text(item.title.orEmpty().ifBlank { "#${item.id}" })
                    Text(item.kind.orEmpty(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
