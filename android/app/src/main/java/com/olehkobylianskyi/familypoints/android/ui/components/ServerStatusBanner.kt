package com.olehkobylianskyi.familypoints.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage

/** Shared compact banner for a backend connection failure on any route. */
@Composable
fun ServerStatusBanner(
    language: AppLanguage,
    onRetry: () -> Unit
) {
    val message = when (language) {
        AppLanguage.UK -> "Сервер недоступний · локальні дані"
        AppLanguage.DE -> "Server nicht erreichbar · lokale Daten"
        AppLanguage.EN -> "Server unavailable · local data"
        AppLanguage.RU -> "Сервер недоступен · локальные данные"
    }
    val retry = when (language) {
        AppLanguage.UK -> "Повторити"
        AppLanguage.DE -> "Erneut"
        AppLanguage.EN -> "Retry"
        AppLanguage.RU -> "Повторить"
    }

    Surface(color = MaterialTheme.colorScheme.errorContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠ $message",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(onClick = onRetry) {
                Text(retry, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
