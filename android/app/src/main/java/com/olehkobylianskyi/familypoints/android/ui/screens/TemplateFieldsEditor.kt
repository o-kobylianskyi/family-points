package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage

/** Edits only whitelisted portable definition properties; all other JSON fields are preserved. */
@Composable
internal fun TemplateFieldsEditor(
    language: AppLanguage,
    kind: String,
    jsonText: String,
    enabled: Boolean,
    onChange: (String) -> Unit
) {
    val root = remember(jsonText) {
        runCatching { JsonParser.parseString(jsonText).asJsonObject }.getOrNull()
    }
    val definition = root?.getAsJsonObject("definition") ?: root
    fun label(uk: String, de: String, en: String, ru: String) = when (language) {
        AppLanguage.UK -> uk
        AppLanguage.DE -> de
        AppLanguage.EN -> en
        AppLanguage.RU -> ru
    }
    fun value(key: String): String =
        definition?.get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()
    fun change(key: String, raw: String, number: Boolean = false, boolean: Boolean = false) {
        if (root == null) return
        val newRoot = root.deepCopy()
        val target = newRoot.getAsJsonObject("definition") ?: newRoot
        when {
            boolean -> target.addProperty(key, raw == "true")
            raw.isBlank() -> target.remove(key)
            number -> raw.toIntOrNull()?.let { target.addProperty(key, it) }
            else -> target.addProperty(key, raw)
        }
        onChange(newRoot.toString())
    }
    @Composable
    fun field(key: String, title: String, numeric: Boolean = false, multiline: Boolean = false) {
        val value = value(key)
        OutlinedTextField(
            value = value,
            onValueChange = { entered ->
                if ((!numeric || entered.isBlank() || entered.all(Char::isDigit)) &&
                    entered.length <= if (multiline) 4000 else 200
                ) change(key, entered, number = numeric)
            },
            label = { Text(title) },
            enabled = enabled,
            minLines = if (multiline) 2 else 1,
            maxLines = if (multiline) 4 else 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
    @Composable
    fun choice(key: String, title: String, choices: List<String>) {
        var opened by remember { mutableStateOf(false) }
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium)
            OutlinedButton(enabled = enabled, onClick = { opened = true },
                modifier = Modifier.fillMaxWidth()) { Text(value(key).ifEmpty { "—" } + " ▾") }
            DropdownMenu(expanded = opened, onDismissRequest = { opened = false }) {
                choices.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        change(key, option)
                        opened = false
                    })
                }
            }
        }
    }
    if (root == null || definition == null) {
        Text(label("Некоректний формат шаблону", "Ungültiges Vorlagenformat",
            "Invalid template format", "Некорректный формат шаблона"),
            color = MaterialTheme.colorScheme.error)
        return
    }
    Column(modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        field("description", label("Опис", "Beschreibung", "Description", "Описание"), multiline = true)
        if (kind == "TASK") {
            field("category", label("Категорія", "Kategorie", "Category", "Категория"))
            choice("recurrenceType", label("Повторення", "Wiederholung", "Recurrence", "Повторение"),
                listOf("ONCE", "DAILY", "WEEKLY", "MONTHLY"))
            field("dueTime", label("Час (HH:mm)", "Uhrzeit (HH:mm)", "Time (HH:mm)", "Время (HH:mm)"))
            field("rewardAmount", label("Бали за виконання", "Aufgabenpunkte", "Task points", "Баллы за выполнение"), numeric = true)
            field("penaltyAmount", label("Штрафні бали", "Strafpunkte", "Penalty points", "Штрафные баллы"), numeric = true)
            androidx.compose.foundation.layout.Row {
                Checkbox(checked = value("mandatory") == "true",
                    enabled = enabled, onCheckedChange = { change("mandatory", it.toString(), boolean = true) })
                Text(label("Обов’язкове", "Verpflichtend", "Mandatory", "Обязательно"))
            }
        } else if (kind == "REWARD") {
            field("category", label("Категорія", "Kategorie", "Category", "Категория"))
            choice("rewardKind", label("Тип нагороди", "Belohnungsart", "Reward type", "Тип награды"),
                listOf("STANDARD", "TIME"))
            field("durationMinutes", label("Тривалість (хв)", "Dauer (Min.)", "Duration (min)", "Длительность (мин)"), numeric = true)
            field("priceAmount", label("Вартість у балах", "Preis in Punkten", "Point price", "Стоимость в баллах"), numeric = true)
            field("cooldownSeconds", label("Пауза (сек)", "Pause (Sek.)", "Cooldown (sec)", "Пауза (сек)"), numeric = true)
            androidx.compose.foundation.layout.Row {
                Checkbox(checked = value("requiresApproval") == "true", enabled = enabled,
                    onCheckedChange = { change("requiresApproval", it.toString(), boolean = true) })
                Text(label("Підтвердження дорослого", "Genehmigung nötig",
                    "Adult approval", "Подтверждение взрослого"))
            }
        }
        Text(label("Зміни зберігаються лише в шаблоні, без створення завдання чи нагороди.",
            "Änderungen betreffen nur die Vorlage, nicht aktive Objekte.",
            "Changes affect this template only, not active tasks or rewards.",
            "Изменения сохраняются только в шаблоне."),
            style = MaterialTheme.typography.bodySmall)
    }
}
