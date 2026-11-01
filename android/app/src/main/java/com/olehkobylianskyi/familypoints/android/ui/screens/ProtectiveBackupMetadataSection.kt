package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.data.ProtectiveBackupMetadataRepository
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import kotlinx.coroutines.CancellationException

/** Metadata ONLY: no reset, export or restore CTA until Backend security signoff. */
@Composable
fun ProtectiveBackupMetadataSection(
    language: AppLanguage,
    workspaceId: Long,
    repository: ProtectiveBackupMetadataRepository
) {
    fun t(uk:String,de:String,en:String,ru:String) = when(language) {
        AppLanguage.UK -> uk; AppLanguage.DE -> de; AppLanguage.EN -> en; AppLanguage.RU -> ru
    }
    var rows by remember(workspaceId) {
        mutableStateOf<List<com.olehkobylianskyi.familypoints.android.network.ProtectiveBackupMetadataDto>>(emptyList())
    }
    var error by remember(workspaceId) { mutableStateOf("") }
    var reload by remember(workspaceId) { mutableIntStateOf(0) }
    var loading by remember(workspaceId) { mutableStateOf(false) }
    LaunchedEffect(workspaceId, reload) {
        loading = true
        try { rows = repository.list(workspaceId); error = "" }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (e: Exception) { rows = emptyList(); error = e.message ?: "Backup metadata unavailable" }
        finally { loading = false }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t("Резервні копії — журнал", "Sicherungen — Register",
            "Backups — registry", "Резервные копии — реестр"),
            style = MaterialTheme.typography.titleMedium)
        Text(t("Це лише записи сервера. Експорт, відновлення й очищення недоступні.",
            "Nur Servereinträge. Export, Wiederherstellung und Löschung sind nicht verfügbar.",
            "Server metadata only. Export, restore and reset are unavailable.",
            "Только записи сервера. Экспорт, восстановление и очистка недоступны."),
            style = MaterialTheme.typography.bodySmall)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (!loading && error.isEmpty() && rows.isEmpty())
            Text(t("Записів немає", "Keine Einträge", "No records", "Нет записей"))
        rows.take(25).forEach { item ->
            HorizontalDivider()
            Text(item.snapshotId.take(12) + " · " + item.state)
            Text(t("Створено", "Erstellt", "Created", "Создано") + ": " + item.createdAt,
                style = MaterialTheme.typography.bodySmall)
            Text(t("Зберігання до", "Gültig bis", "Retention until", "Хранение до") +
                ": " + item.expiresAt, style = MaterialTheme.typography.bodySmall)
            Text(item.resetMode + " · " + item.selectedCategories.joinToString(", "),
                style = MaterialTheme.typography.bodySmall)
            Text(t("Факт відновлюваності не підтверджено інтерфейсом",
                "Wiederherstellbarkeit durch die App nicht bestätigt",
                "App has not verified restorability",
                "Приложение не подтвердило восстановимость"),
                style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(enabled = !loading, onClick = { reload++ }) {
            Text(t("Оновити", "Aktualisieren", "Refresh", "Обновить"))
        }
    }
}
