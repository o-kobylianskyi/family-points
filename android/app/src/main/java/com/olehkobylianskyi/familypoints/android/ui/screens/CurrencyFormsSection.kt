package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.*
import com.olehkobylianskyi.familypoints.android.i18n.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

private fun cf(language: AppLanguage, uk: String, de: String, en: String, ru: String): String =
    when (language) {
        AppLanguage.UK -> uk
        AppLanguage.DE -> de
        AppLanguage.EN -> en
        AppLanguage.RU -> ru
    }

@Composable
fun CurrencyFormsSection(
    language: AppLanguage,
    workspaceId: Long,
    canManage: Boolean,
    repository: SettingsRepository,
    onUnauthorized: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var types by remember(workspaceId) { mutableStateOf<List<PointTypeResponse>>(emptyList()) }
    var selected by remember { mutableStateOf<PointTypeResponse?>(null) }
    var languageCode by remember { mutableStateOf("uk") }
    var languageMenu by remember { mutableStateOf(false) }
    var forms by remember { mutableStateOf<List<PointNameFormDto>>(emptyList()) }
    var one by remember { mutableStateOf("") }
    var few by remember { mutableStateOf("") }
    var many by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }

    fun failure(e: Exception) {
        if (e is HttpException && e.code() == 401) onUnauthorized()
        else error = e.message ?: "Request failed"
    }

    LaunchedEffect(workspaceId) {
        try { types = repository.pointTypes(workspaceId) } catch (e: Exception) { failure(e) }
    }
    LaunchedEffect(selected?.id) {
        val type = selected ?: return@LaunchedEffect
        try { forms = repository.pointNameForms(workspaceId, type.id) }
        catch (e: Exception) { failure(e) }
    }
    LaunchedEffect(forms, languageCode) {
        val current = forms.firstOrNull { it.language == languageCode }
        one = current?.one ?: ""
        few = current?.few ?: ""
        many = current?.many ?: ""
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(cf(language, "Відмінювання валют", "Währungsformen", "Currency word forms",
            "Склонение валют"), style = MaterialTheme.typography.titleLarge)
        Text(cf(language, "Задайте форми для кожної валюти та мови.",
            "Wortformen pro Währung und Sprache festlegen.",
            "Configure quantity forms for each currency and language.",
            "Настройте формы для каждой валюты и языка."))
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (notice.isNotBlank()) Text(notice, color = MaterialTheme.colorScheme.primary)
        types.forEach { type ->
            TextButton(onClick = { selected = type; notice = ""; error = "" },
                modifier = Modifier.fillMaxWidth()) {
                Text(type.name + " (" + type.code + ")")
            }
        }
    }
    val type = selected
    if (type != null) {
        AlertDialog(
            onDismissRequest = { if (!busy) selected = null },
            title = { Text(type.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { languageMenu = true }) {
                        Text(languageCode.uppercase() + " ▾")
                    }
                    DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                        listOf("uk" to "Українська", "ru" to "Русский",
                            "en" to "English", "de" to "Deutsch").forEach { (code, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                languageCode = code; languageMenu = false
                            })
                        }
                    }
                    OutlinedTextField(one, { one = it.take(100) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(cf(language, "Для 1", "Für 1", "For 1", "Для 1")) })
                    OutlinedTextField(few, { few = it.take(100) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(cf(language, "Для 2–4", "Für 2–4", "For 2–4", "Для 2–4")) })
                    OutlinedTextField(many, { many = it.take(100) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(cf(language, "Для 5+", "Für 5+", "For 5+", "Для 5+")) })
                    val chosen = listOf(PointNameForms(languageCode, one, few, many))
                    listOf(1L, 2L, 5L, 11L, 21L).forEach { number ->
                        val previewLanguage = when (languageCode) {
                            "uk" -> AppLanguage.UK
                            "ru" -> AppLanguage.RU
                            "en" -> AppLanguage.EN
                            else -> AppLanguage.DE
                        }
                        Text(formatPointAmount(number, previewLanguage, type.code, type.name, chosen))
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = canManage && !busy && one.isNotBlank() && few.isNotBlank() && many.isNotBlank(),
                    onClick = {
                        val id = type.id
                        val code = languageCode
                        val payload = PointNameFormSave(one.trim(), few.trim(), many.trim())
                        scope.launch {
                            busy = true
                            error = ""
                            try {
                                repository.savePointNameForms(workspaceId, id, code, payload)
                                forms = repository.pointNameForms(workspaceId, id)
                                notice = cf(language, "Збережено", "Gespeichert", "Saved", "Сохранено")
                                selected = null
                            } catch (e: Exception) { failure(e) }
                            finally { busy = false }
                        }
                    }
                ) { Text(cf(language, "Зберегти", "Speichern", "Save", "Сохранить")) }
            },
            dismissButton = {
                TextButton(onClick = { if (!busy) selected = null }) {
                    Text(cf(language, "Закрити", "Schließen", "Close", "Закрыть"))
                }
            }
        )
    }
}
