package com.olehkobylianskyi.familypoints.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun LocalizedCalendarDialog(
    language: AppLanguage,
    selectedDate: LocalDate,
    visibleMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val locale = Locale.forLanguageTag(language.code)
    val monthTitle = visibleMonth
        .atDay(1)
        .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        .replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }

    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val orderedWeekDays = List(7) { offset ->
        DayOfWeek.of(
            ((firstDayOfWeek.value - 1 + offset) % 7) + 1
        )
    }

    val firstDay = visibleMonth.atDay(1)
    val firstOffset =
        (firstDay.dayOfWeek.value - firstDayOfWeek.value + 7) % 7

    val cells = List(firstOffset) { null } +
        (1..visibleMonth.lengthOfMonth()).map {
            visibleMonth.atDay(it)
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onPreviousMonth) {
                    Text("‹")
                }
                Text(
                    monthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
                TextButton(onClick = onNextMonth) {
                    Text("›")
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    orderedWeekDays.forEach { day ->
                        Text(
                            text = day.getDisplayName(
                                TextStyle.SHORT,
                                locale
                            ),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            if (date == null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            } else {
                                val isSelected = date == selectedDate
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .then(
                                            if (isSelected) {
                                                Modifier.background(
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                    CircleShape
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            onDateSelected(date)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        style = if (isSelected) {
                                            MaterialTheme.typography.titleSmall
                                        } else {
                                            MaterialTheme.typography.bodyMedium
                                        }
                                    )
                                }
                            }
                        }

                        repeat(7 - week.size) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    when (language) {
                        AppLanguage.UK -> "Скасувати"
                        AppLanguage.DE -> "Abbrechen"
                        AppLanguage.EN -> "Cancel"
                        AppLanguage.RU -> "Отмена"
                    }
                )
            }
        }
    )
}
