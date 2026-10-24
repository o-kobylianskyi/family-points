package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.ui.components.FpCard
import java.time.LocalDate
import java.time.LocalTime

/** UI projection only, populated from an authorized server DTO in a future integration. */
enum class AgendaEventType { TASK, REWARD_WINDOW }
enum class AgendaEventStatus { PLANNED, COMPLETED, MISSED, UPCOMING, OPEN, UNAVAILABLE, CLOSED }

data class AgendaEventUi(
    val identity: String,
    val day: LocalDate,
    val type: AgendaEventType,
    val title: String,
    val startsAt: LocalTime?,
    val endsAt: LocalTime? = null,
    val status: AgendaEventStatus = AgendaEventStatus.PLANNED,
    val subtitle: String? = null,
    val participantLabels: List<String> = emptyList()
)

private fun translated(language: AppLanguage, uk: String, de: String, en: String, ru: String) =
    when (language) {
        AppLanguage.UK -> uk
        AppLanguage.DE -> de
        AppLanguage.EN -> en
        AppLanguage.RU -> ru
    }

@Composable
fun AgendaTimeline(language: AppLanguage, events: List<AgendaEventUi>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        events.sortedWith(compareBy<AgendaEventUi> { it.startsAt }.thenBy { it.identity }).forEach { event ->
            FpCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = when (event.type) {
                            AgendaEventType.TASK -> event.startsAt?.toString()?.let { translated(language, "До $it", "Bis $it", "Due $it", "До $it") } ?: "—"
                            AgendaEventType.REWARD_WINDOW -> listOfNotNull(event.startsAt, event.endsAt).joinToString("–")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (event.type == AgendaEventType.TASK) Icons.Outlined.Checklist else Icons.Outlined.CardGiftcard,
                        contentDescription = if (event.type == AgendaEventType.TASK)
                            translated(language, "Завдання", "Aufgabe", "Task", "Задача")
                        else translated(language, "Винагорода", "Belohnung", "Reward", "Награда"),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(event.title, style = MaterialTheme.typography.titleSmall)
                        Text(statusText(event.status, language),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        event.subtitle?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        if (event.participantLabels.isNotEmpty()) {
                            Text(event.participantLabels.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private fun statusText(s: AgendaEventStatus, l: AppLanguage): String = when (s) {
    AgendaEventStatus.PLANNED -> translated(l, "Заплановано", "Geplant", "Planned", "Запланировано")
    AgendaEventStatus.COMPLETED -> translated(l, "Виконано", "Erledigt", "Completed", "Выполнено")
    AgendaEventStatus.MISSED -> translated(l, "Пропущено", "Verpasst", "Missed", "Пропущено")
    AgendaEventStatus.UPCOMING -> translated(l, "Незабаром", "Demnächst", "Upcoming", "Скоро")
    AgendaEventStatus.OPEN -> translated(l, "Відкрито", "Verfügbar", "Open", "Открыто")
    AgendaEventStatus.UNAVAILABLE -> translated(l, "Умови не виконано", "Bedingungen fehlen", "Requirements unmet", "Условия не выполнены")
    AgendaEventStatus.CLOSED -> translated(l, "Закрито", "Geschlossen", "Closed", "Закрыто")
}
