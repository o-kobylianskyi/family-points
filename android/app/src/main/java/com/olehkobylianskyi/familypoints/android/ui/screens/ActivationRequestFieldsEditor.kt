package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonNull
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage

/**
 * Edits ONLY properties actually supplied by the creation-draft endpoint.
 * Unrecognized fields and explicit JSON nulls are retained until the parent changes them.
 * This is a request editor, not a template-definition editor.
 */
@Composable
internal fun ActivationRequestFieldsEditor(
    language: AppLanguage,
    kind: String,
    jsonText: String,
    enabled: Boolean,
    onChange: (String) -> Unit
) {
    fun t(uk: String, de: String, en: String, ru: String) = when(language) {
        AppLanguage.UK -> uk
        AppLanguage.DE -> de
        AppLanguage.EN -> en
        AppLanguage.RU -> ru
    }
    val root = remember(jsonText) {
        runCatching { JsonParser.parseString(jsonText).asJsonObject }.getOrNull()
    }
    if (root == null) {
        Text(t("Неправильний JSON запиту", "Ungültiger Anfrage-JSON",
            "Invalid request JSON", "Неверный JSON запроса"),
            color = MaterialTheme.colorScheme.error)
        return
    }
    val fields = listOf(
        "name" to t("Назва", "Name", "Name", "Название"),
        "title" to t("Назва", "Titel", "Title", "Название"),
        "description" to t("Опис", "Beschreibung", "Description", "Описание"),
        "rewardAmount" to t("Бали за завдання", "Aufgabenpunkte", "Task points", "Баллы за задание"),
        "penaltyAmount" to t("Штрафні бали", "Strafpunkte", "Penalty points", "Штрафные баллы"),
        "price" to t("Ціна (порожнє = null)", "Preis (leer = null)", "Price (blank = null)", "Цена (пусто = null)"),
        "priceAmount" to t("Ціна", "Preis", "Price", "Цена"),
        "defaultDurationMinutes" to t("Час, хвилини", "Dauer, Minuten", "Duration, minutes", "Время, минуты"),
        "durationMinutes" to t("Час, хвилини", "Dauer, Minuten", "Duration, minutes", "Время, минуты"),
        "cooldownSeconds" to t("Пауза, секунди", "Pause, Sekunden", "Cooldown, seconds", "Пауза, секунды")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        fields.forEach { (key, label) ->
            val original = root.get(key)
            if (root.has(key) && (original.isJsonNull || original.isJsonPrimitive)) {
                val primitive = original.takeUnless { it.isJsonNull }?.asJsonPrimitive
                val number = primitive?.isNumber == true ||
                    key in setOf("price", "priceAmount", "rewardAmount", "penaltyAmount",
                        "defaultDurationMinutes", "durationMinutes", "cooldownSeconds")
                val editable = primitive == null || !primitive.isBoolean
                if (editable) {
                    OutlinedTextField(
                        value = original.takeUnless { it.isJsonNull }?.asString.orEmpty(),
                        onValueChange = { next ->
                            if (next.length <= 4000 &&
                                (!number || next.isBlank() || next.all(Char::isDigit))) {
                                val copy: JsonObject = root.deepCopy()
                                when {
                                    next.isBlank() && original.isJsonNull -> copy.add(key, JsonNull.INSTANCE)
                                    next.isBlank() && number -> copy.add(key, JsonNull.INSTANCE)
                                    number -> next.toLongOrNull()?.let { copy.addProperty(key, it) }
                                    else -> copy.addProperty(key, next)
                                }
                                onChange(copy.toString())
                            }
                        },
                        enabled = enabled,
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = if (key == "description") 4 else 1
                    )
                }
            }
        }
        Text(
            t("Поля, яких немає в серверній чернетці, не додаються. Null не перетворюється на 0.",
                "Fehlende Felder werden nicht ergänzt. Null wird nicht zu 0.",
                "Missing server fields are not added. Null is never converted to zero.",
                "Отсутствующие поля не добавляются. Null не становится нулём."),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
