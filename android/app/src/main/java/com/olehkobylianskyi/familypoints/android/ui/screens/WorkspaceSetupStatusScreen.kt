package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.network.WorkspaceSetupStateDto
import kotlinx.coroutines.CancellationException

private val setupStepCodes = listOf(
    "MEMBERS", "MEMBER_GROUPS", "WORK_NODE_GROUPS", "CATALOG_SELECTION",
    "TASK_REVIEW", "REWARD_REVIEW", "FINAL_REVIEW"
)

/** Reject incomplete, foreign-workspace or malformed server setup metadata before rendering. */
internal fun validWorkspaceSetupState(state: WorkspaceSetupStateDto, workspaceId: Long): Boolean {
    val steps = state.steps ?: return false
    return state.workspaceId == workspaceId &&
        state.dataEpoch >= 0 && state.revision >= 0 &&
        state.status in setOf("NOT_STARTED", "IN_PROGRESS", "COMPLETED") &&
        steps.keys == setupStepCodes.toSet() &&
        steps.values.all { it in setOf("PENDING", "IN_PROGRESS", "COMPLETE", "SKIPPED") }
}

private fun setupText(language: AppLanguage, uk: String, ru: String, de: String, en: String) =
    when (language) {
        AppLanguage.UK -> uk
        AppLanguage.RU -> ru
        AppLanguage.DE -> de
        AppLanguage.EN -> en
    }

private fun setupStepTitle(language: AppLanguage, code: String): String = when (code) {
    "MEMBERS" -> setupText(language, "Учасники", "Участники", "Mitglieder", "Members")
    "MEMBER_GROUPS" -> setupText(language, "Групи учасників", "Группы участников", "Mitgliedergruppen", "Member groups")
    "WORK_NODE_GROUPS" -> setupText(language, "Папки завдань", "Папки задач", "Aufgabenordner", "Task folders")
    "CATALOG_SELECTION" -> setupText(language, "Шаблони", "Шаблоны", "Vorlagen", "Templates")
    "TASK_REVIEW" -> setupText(language, "Перевірка завдань", "Проверка задач", "Aufgaben prüfen", "Task review")
    "REWARD_REVIEW" -> setupText(language, "Перевірка нагород", "Проверка наград", "Belohnungen prüfen", "Reward review")
    else -> setupText(language, "Підсумкова перевірка", "Итоговая проверка", "Abschlussprüfung", "Final review")
}

private fun setupStepStatus(language: AppLanguage, status: String) = when (status) {
    "COMPLETE" -> setupText(language, "Виконано", "Выполнено", "Erledigt", "Complete")
    "SKIPPED" -> setupText(language, "Пропущено", "Пропущено", "Übersprungen", "Skipped")
    "IN_PROGRESS" -> setupText(language, "У процесі", "В процессе", "In Bearbeitung", "In progress")
    else -> setupText(language, "Очікує", "Ожидает", "Ausstehend", "Pending")
}

/** V49 read-only entry after creating a new family. No writes, imports or step completion. */
@Composable
fun WorkspaceSetupStatusScreen(
    language: AppLanguage,
    workspaceId: Long,
    loadState: suspend (Long) -> WorkspaceSetupStateDto,
    onContinue: () -> Unit
) {
    var attempt by remember(workspaceId) { mutableIntStateOf(0) }
    var loading by remember(workspaceId) { mutableStateOf(true) }
    var state by remember(workspaceId) { mutableStateOf<WorkspaceSetupStateDto?>(null) }
    var unavailable by remember(workspaceId) { mutableStateOf(false) }

    LaunchedEffect(workspaceId, attempt) {
        loading = true
        unavailable = false
        state = null
        try {
            val response = loadState(workspaceId)
            if (!validWorkspaceSetupState(response, workspaceId)) {
                unavailable = true
            } else {
                state = response
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            unavailable = true
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            setupText(language, "Налаштування сім’ї", "Настройка семьи", "Familie einrichten", "Family setup"),
            style = MaterialTheme.typography.headlineMedium
        )
        if (loading) {
            Text(setupText(language, "Завантаження…", "Загрузка…", "Laden…", "Loading…"))
        }
        if (unavailable) {
            Text(
                setupText(language,
                    "Серверне налаштування недоступне. Жодних змін не виконано.",
                    "Серверная настройка недоступна. Изменений не внесено.",
                    "Servereinrichtung nicht verfügbar. Es wurden keine Änderungen vorgenommen.",
                    "Server setup unavailable. No changes were made."),
                color = MaterialTheme.colorScheme.error
            )
            OutlinedButton(onClick = { attempt++ }) {
                Text(setupText(language, "Повторити", "Повторить", "Erneut versuchen", "Retry"))
            }
        }
        state?.let { setup ->
            Text(setupText(language,
                "Це перегляд стану. Шаблони не імпортуються, а завдання й нагороди не активуються.",
                "Это просмотр состояния. Шаблоны не импортируются, задачи и награды не активируются.",
                "Dies ist nur eine Statusansicht. Vorlagen werden nicht importiert und Aufgaben/Belohnungen nicht aktiviert.",
                "This is a status view only. Templates are not imported and tasks/rewards are not activated."))
            setupStepCodes.forEachIndexed { index, code ->
                Text("${index + 1}. ${setupStepTitle(language, code)} — ${setupStepStatus(language, setup.steps?.get(code) ?: "PENDING")}")
            }
        }
        Button(onClick = onContinue) {
            Text(setupText(language, "Перейти до сім’ї", "Перейти к семье", "Zur Familie", "Continue to family"))
        }
    }
}
