package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.network.CalendarEventDto
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import com.olehkobylianskyi.familypoints.android.network.ServerReachability
import com.olehkobylianskyi.familypoints.android.network.ServerStatus
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.CircularProgressIndicator
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import retrofit2.HttpException
import com.olehkobylianskyi.familypoints.android.ui.components.FpCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.util.Locale

/** Authenticated calendar events from Backend V36. Never synthesize missing events. */
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
fun CalendarScreen(
    language: AppLanguage,
    modifier: Modifier = Modifier,
    events: List<AgendaEventUi> = emptyList(),
    projectionLoaded: Boolean = false,
    currentUser: CurrentUserResponse? = null,
    onUnauthorized: () -> Unit = {}
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var mode by remember { mutableStateOf(CalendarMode.DAY) }
    var kind by remember { mutableStateOf(CalendarKind.ALL) }
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val serverStatus by ServerReachability.status.collectAsState()
    var remoteEvents by remember(currentUser?.workspaceId, currentUser?.memberId) { mutableStateOf<List<AgendaEventUi>>(emptyList()) }
    var loaded by remember(currentUser?.workspaceId, currentUser?.memberId) { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    val rangeStart = when (mode) {
        CalendarMode.DAY -> selectedDate
        CalendarMode.WEEK -> selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        CalendarMode.MONTH -> selectedDate.withDayOfMonth(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val rangeEnd = when (mode) {
        CalendarMode.DAY -> selectedDate
        CalendarMode.WEEK -> rangeStart.plusDays(6)
        CalendarMode.MONTH -> selectedDate.withDayOfMonth(selectedDate.lengthOfMonth())
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    }
    LaunchedEffect(currentUser?.workspaceId, currentUser?.memberId, rangeStart, rangeEnd, serverStatus) {
        loaded = false
        remoteEvents = emptyList() // Never show another date range as a confirmed projection.
        if (currentUser == null || serverStatus != ServerStatus.ONLINE) return@LaunchedEffect
        val token = tokenStore.getAccessToken() ?: return@LaunchedEffect
        loading = true
        loadError = false
        try {
            remoteEvents = ApiClient.calendarApi.events(
                "Bearer $token", currentUser.workspaceId, rangeStart.toString(), rangeEnd.toString()
            ).mapNotNull { it.toAgendaEvent() }
            loaded = true
        } catch (e: HttpException) {
            if (e.code() == 401) onUnauthorized() else loadError = true
        } catch (_: Exception) {
            loadError = true
        } finally {
            loading = false
        }
    }
    val visibleEvents = if (currentUser != null) remoteEvents else events
    val effectiveLoaded = if (currentUser != null) loaded else projectionLoaded
    val selectedEvents = visibleEvents.filter { event ->
        event.day == selectedDate && when (kind) {
            CalendarKind.ALL -> true
            CalendarKind.TASK -> event.type == AgendaEventType.TASK
            CalendarKind.REWARD -> event.type == AgendaEventType.REWARD_WINDOW
        }
    }
    val counts = selectedEvents.groupingBy { it.type }.eachCount()
    // Absence of a loaded backend projection is not a confirmed zero-count day.
    val hasProjection = effectiveLoaded
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
            val monthEnd = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth())
            val end = monthEnd.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            (0L..java.time.temporal.ChronoUnit.DAYS.between(start, end)).map(start::plusDays)
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
        if (loading) item { CircularProgressIndicator() }
        if (loadError) item {
            Text(calText(language, "Не вдалося оновити події календаря", "Kalender konnte nicht geladen werden",
                "Could not load calendar events", "Не удалось загрузить календарь"),
                color = MaterialTheme.colorScheme.error)
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
                OutlinedButton(onClick = { selectedDate = LocalDate.now().plusDays(1); mode = CalendarMode.DAY }) {
                    Text(calText(language, "Завтра", "Morgen", "Tomorrow", "Завтра"))
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
                    // The grid reflects the selected week/month, and markers reflect only supplied authorized data.
                    if (mode != CalendarMode.DAY) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            (0..6).forEach { index ->
                                val weekday = firstDay.plusDays(index.toLong()).dayOfWeek
                                Text(weekday.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1)
                            }
                        }
                    }
                    visibleDays.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            week.forEach { day ->
                                val hasTasks = visibleEvents.any { it.day == day && it.type == AgendaEventType.TASK }
                                val hasRewards = visibleEvents.any { it.day == day && it.type == AgendaEventType.REWARD_WINDOW }
                                Column(
                                    modifier = Modifier.weight(1f).clickable { selectedDate = day }.padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(34.dp).background(
                                            if (day == selectedDate) MaterialTheme.colorScheme.primary
                                            else Color.Transparent, CircleShape)) {
                                        Text(day.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = when {
                                                day == selectedDate -> MaterialTheme.colorScheme.onPrimary
                                                day.month != selectedDate.month -> MaterialTheme.colorScheme.onSurfaceVariant
                                                else -> MaterialTheme.colorScheme.onSurface
                                            })
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        if (hasTasks) Box(Modifier.size(5.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                        if (hasRewards) Box(Modifier.size(5.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                                        if (!hasTasks && !hasRewards) Box(Modifier.size(5.dp))
                                    }
                                }
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
                    else if (effectiveLoaded) Text(calText(language,
                        "Немає подій з визначеним часом", "Keine Termine mit Uhrzeit",
                        "No timed events", "Нет событий с указанным временем"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            else Text(if (effectiveLoaded) calText(language, "Немає завдань без часу", "Keine Aufgaben ohne Uhrzeit", "No untimed tasks", "Нет задач без времени")
                else calText(language, "Дані поки недоступні", "Daten noch nicht verfügbar",
                "Data not available yet", "Данные пока недоступны"),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Deadline time is not an event start; reward windows are acquisition windows. */
private fun CalendarEventDto.toAgendaEvent(): AgendaEventUi? {
    val day = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    val type = when (kind) {
        "TASK" -> AgendaEventType.TASK
        "REWARD_WINDOW" -> AgendaEventType.REWARD_WINDOW
        else -> return null
    }
    val start = if (type == AgendaEventType.TASK) {
        scheduledTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    } else startsAt?.let {
        runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalTime() }.getOrNull()
    }
    val end = if (type == AgendaEventType.REWARD_WINDOW) endsAt?.let {
        runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalTime() }.getOrNull()
    } else null
    val state = runCatching { AgendaEventStatus.valueOf(status.orEmpty()) }
        .getOrDefault(if (scheduleOpenNow) AgendaEventStatus.OPEN else AgendaEventStatus.PLANNED)
    return AgendaEventUi(key, day, type, title, start, end, state)
}
