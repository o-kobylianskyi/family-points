package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.WorkspaceSetupOverviewRepository
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import kotlinx.coroutines.CancellationException

private fun ol(lang: AppLanguage, uk: String, de: String, en: String, ru: String): String =
    when (lang) {
        AppLanguage.UK -> uk
        AppLanguage.DE -> de
        AppLanguage.EN -> en
        AppLanguage.RU -> ru
    }

/** Read-only progress, not a wizard that silently marks server steps complete. */
@Composable
fun WorkspaceSetupOverviewSection(
    language: AppLanguage,
    workspaceId: Long,
    repository: WorkspaceSetupOverviewRepository
) {
    var overview by remember(workspaceId) {
        mutableStateOf<WorkspaceSetupOverviewRepository.Overview?>(null)
    }
    var loadError by remember(workspaceId) { mutableStateOf("") }
    var reload by remember(workspaceId) { mutableIntStateOf(0) }
    var loading by remember(workspaceId) { mutableStateOf(false) }
    LaunchedEffect(workspaceId, reload) {
        loading = true
        try {
            overview = repository.load(workspaceId)
            loadError = ""
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            overview = null
            loadError = error.message ?: "Setup status unavailable"
        } finally { loading = false }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(ol(language, "Стан налаштування сім'ї", "Familieneinrichtung",
            "Family setup status", "Настройка семьи"),
            style = MaterialTheme.typography.titleMedium)
        Text(ol(language,
            "Групи учасників і папки завдань — різні сутності. Цей огляд нічого не створює.",
            "Mitgliedergruppen und Aufgabenordner sind verschieden. Diese Ansicht erstellt nichts.",
            "Member groups and task folders are distinct. This view makes no changes.",
            "Группы участников и папки заданий различаются. Обзор ничего не создаёт."),
            style = MaterialTheme.typography.bodySmall)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (loadError.isNotBlank()) {
            Text(loadError, color = MaterialTheme.colorScheme.error)
        }
        val data = overview
        if (data != null) {
            Text(ol(language, "Групи учасників", "Mitgliedergruppen", "Member groups",
                "Группы участников") + ": " + data.memberGroupCount)
            Text(ol(language, "Папки завдань", "Aufgabenordner", "Task folders",
                "Папки заданий") + ": " + data.workNodeGroupCount)
            val labels = listOf(
                ol(language, "Учасники", "Mitglieder", "Members", "Участники"),
                ol(language, "Групи учасників", "Mitgliedergruppen", "Member groups", "Группы участников"),
                ol(language, "Папки завдань", "Aufgabenordner", "Task folders", "Папки заданий"),
                ol(language, "Вибір шаблонів", "Vorlagenauswahl", "Catalog selection", "Выбор шаблонов"),
                ol(language, "Перегляд завдань", "Aufgabenprüfung", "Task review", "Проверка заданий"),
                ol(language, "Перегляд нагород", "Belohnungsprüfung", "Reward review", "Проверка наград"),
                ol(language, "Фінальна перевірка", "Abschlussprüfung", "Final review", "Финальная проверка")
            )
            WorkspaceSetupOverviewRepository.STEPS.forEachIndexed { index, step ->
                val status = data.state.steps?.get(step)
                Text("${index + 1}. ${labels[index]}: " + (status ?: "PENDING"),
                    style = MaterialTheme.typography.bodySmall)
            }
            Text(if (WorkspaceSetupOverviewRepository.readyToFinalize(data.state))
                ol(language, "Серверні кроки заповнено. Перед завершенням перевірте імпорт.",
                    "Serverschritte erledigt. Import vor Abschluss prüfen.",
                    "Server steps recorded. Review import before finalizing.",
                    "Шаги на сервере заполнены. Проверьте импорт перед завершением.")
            else ol(language, "Налаштування ще не завершено.",
                "Einrichtung noch nicht abgeschlossen.", "Setup is not yet complete.",
                "Настройка ещё не завершена."))
        }
        OutlinedButton(enabled = !loading, onClick = { reload++ }) {
            Text(ol(language, "Оновити статус", "Status aktualisieren",
                "Refresh status", "Обновить статус"))
        }
    }
}
