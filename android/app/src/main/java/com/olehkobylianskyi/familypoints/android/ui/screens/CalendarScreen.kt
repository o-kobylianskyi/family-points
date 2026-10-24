package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.ui.components.FpCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.util.Locale

/** Calendar UI only. Server projections are intentionally not fabricated before API approval. */
private enum class CalendarMode { DAY, WEEK, MONTH }
private enum class CalendarKind { ALL, TASK, REWARD }

private fun calText(l: AppLanguage, uk: String, de: String, en: String, ru: String) =
    when (l) {
        AppLanguage.UK -> uk
        AppLanguage.DE -> de
        AppLanguage.EN -> en
        AppLanguage.RU -> ru
    }

private fun localeFor(l: AppLanguage): Locale = when (l) {
    AppLanguage.UK -> Locale.forLanguageTag("uk-UA")
    AppLanguage.DE -> Locale.GERMAN
    AppLanguage.EN -> Locale.ENGLISH
    AppLanguage.RU -> Locale.forLanguageTag("ru-RU")
}

@Composable
fun CalendarScreen(language: AppLanguage, modifier: Modifier = Modifier, events: List<AgendaEventUi> = emptyList()) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var mode by remember { mutableStateOf(CalendarMode.DAY) }
    var kind by remember { mutableStateOf(CalendarKind.ALL) }
    val selectedEvents = events.filter { event ->
        event.day == selectedDate && when (kind) {
            CalendarKind.ALL -> true
            CalendarKind.TASK -> event.type == AgendaEventType.TASK
            CalendarKind.REWARD -> event.type == AgendaEventType.REWARD_WINDOW
        }
    }
    val counts = selectedEvents.groupingBy { it.type }.eachCount()
    // Absence of a loaded backend projection is not a confirmed zero-count day.
    val hasProjection = events.isNotEmpty()
    val timed = selectedEvents.filter { it.startsAt != null }
    val untimed = selectedEvents.filter { it.startsAt == null }
    val locale = localeFor(language)
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val firstDay = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val visibleDays = when (mode) {
        CalendarMode.DAY -> listOf(selectedDate)
        CalendarMode.WEEK -> (0L..6L).map(firstDay::plusDays)
        CalendarMode.MONTH -> {
            val monthStart = selectedDate.withDayOfMonth(1)
            val start = monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            (0L..41L).map(start::plusDays)
        }
    }
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            Text(calText(language, "Календар", "Kalender", "Calendar", "Календарь"),
                style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalendarMode.entries.forEach { option ->
                    FilterChip(
                        selected = option == mode,
                        onClick = { mode = option },
                        label = { Text(when (option) {
                            CalendarMode.DAY -> calText(language, "День", "Tag", "Day", "День")
                            CalendarMode.WEEK -> calText(language, "Тиждень", "Woche", "Week", "Неделя")
                            CalendarMode.MONTH -> calText(language, "Місяць", "Monat", "Month", "Месяц")
                        }) }
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    selectedDate = when (mode) {
                        CalendarMode.DAY -> selectedDate.minusDays(1)
                        CalendarMode.WEEK -> selectedDate.minusWeeks(1)
                        CalendarMode.MONTH -> selectedDate.minusMonths(1)
                    }
                }) { Text("‹") }
                OutlinedButton(onClick = { selectedDate = LocalDate.now() }) {
                    Text(calText(language, "Сьогодні", "Heute", "Today", "Сегодня"))
                }
                OutlinedButton(onClick = {
                    selectedDate = when (mode) {
                        CalendarMode.DAY -> selectedDate.plusDays(1)
                        CalendarMode.WEEK -> selectedDate.plusWeeks(1)
                        CalendarMode.MONTH -> selectedDate.plusMonths(1)
                    }
                }) { Text("›") }
            }
            Text(selectedDate.format(dateFormat),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp))
        }
        item {
            FpCard {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // This is navigation, not a claim that server-side events exist.
                    visibleDays.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            week.forEach { day ->
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    style = if (day == selectedDate) MaterialTheme.typography.titleMedium
                                        else MaterialTheme.typography.bodyMedium,
                                    color = if (day.month == selectedDate.month) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { selectedDate = day }.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val metrics = listOf(
                    Triple(calText(language, "Подій", "Termine", "Events", "Событий"),
                        selectedEvents.size, CalendarKind.ALL),
                    Triple(calText(language, "Завдань", "Aufgaben", "Tasks", "Задач"),
                        counts[AgendaEventType.TASK] ?: 0, CalendarKind.TASK),
                    Triple(calText(language, "Винагород", "Belohnungen", "Rewards", "Наград"),
                        counts[AgendaEventType.REWARD_WINDOW] ?: 0, CalendarKind.REWARD)
                )
                metrics.forEach { (label, count, destination) ->
                    FpCard(modifier = Modifier.weight(1f).clickable { kind = destination }) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (hasProjection) count.toString() else "—", style = MaterialTheme.typography.titleLarge)
                            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalendarKind.entries.forEach { option ->
                    FilterChip(
                        selected = option == kind,
                        onClick = { kind = option },
                        label = { Text(when (option) {
                            CalendarKind.ALL -> calText(language, "Усе", "Alle", "All", "Все")
                            CalendarKind.TASK -> calText(language, "Завдання", "Aufgaben", "Tasks", "Задачи")
                            CalendarKind.REWARD -> calText(language, "Винагороди", "Belohnungen", "Rewards", "Награды")
                        }) }
                    )
                }
            }
        }
        item {
            FpCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(calText(language, "Розклад дня", "Tagesübersicht", "Daily agenda", "Расписание дня"),
                        style = MaterialTheme.typography.titleMedium)
                    if (timed.isNotEmpty()) AgendaTimeline(language, timed)
                    else Text(calText(language,
                        "Події ще не завантажуються: очікується серверний календар завдань і часових вікон винагород. Порожній екран не означає, що завдань немає.",
                        "Termine werden noch nicht geladen: Die Server-API für Aufgaben und Belohnungszeiträume steht noch aus. Dies ist keine bestätigte leere Agenda.",
                        "Events are not loaded yet: the server API for tasks and reward windows is pending. This is not a confirmed empty agenda.",
                        "События пока не загружаются: ожидается серверный API задач и окон наград. Это не подтверждение отсутствия событий."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Text(calText(language, "Завдання без часу", "Aufgaben ohne Uhrzeit",
                "Tasks without a time", "Задачи без времени"),
                style = MaterialTheme.typography.titleMedium)
            if (untimed.isNotEmpty()) AgendaTimeline(language, untimed)
            else Text(calText(language, "Дані поки недоступні", "Daten noch nicht verfügbar",
                "Data not available yet", "Данные пока недоступны"),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
