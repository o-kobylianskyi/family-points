package com.olehkobylianskyi.familypoints.android.i18n

private fun translated(language: AppLanguage, uk: String, de: String, en: String, ru: String) =
    when (language) {
        AppLanguage.UK -> uk
        AppLanguage.DE -> de
        AppLanguage.EN -> en
        AppLanguage.RU -> ru
    }

fun localizedCurrencyName(language: AppLanguage, code: String?, fallback: String): String =
    when (code?.uppercase()) {
        "POINTS" -> translated(language, "Бали", "Punkte", "Points", "Баллы")
        "COPPER" -> translated(language, "Мідь", "Kupfer", "Copper", "Медь")
        "SILVER" -> translated(language, "Срібло", "Silber", "Silver", "Серебро")
        "GOLD" -> translated(language, "Золото", "Gold", "Gold", "Золото")
        else -> fallback
    }

fun localizedRoleSetName(language: AppLanguage, code: String?, fallback: String): String =
    when (code?.uppercase()) {
        "MANAGEMENT" -> translated(language, "Керівництво", "Leitung", "Management", "Управление")
        "EXECUTION" -> translated(language, "Виконання", "Ausführung", "Execution", "Исполнение")
        "CONTROL" -> translated(language, "Контроль", "Kontrolle", "Control", "Контроль")
        else -> fallback
    }

fun localizedRoleSetDescription(language: AppLanguage, code: String?, fallback: String?): String? =
    when (code?.uppercase()) {
        "MANAGEMENT" -> translated(language, "Стандартні ролі керівництва",
            "Standardrollen der Leitung", "Default management roles", "Стандартные роли руководства")
        "EXECUTION" -> translated(language, "Стандартні ролі виконавців",
            "Standardrollen für Ausführung", "Default execution roles", "Стандартные роли исполнителей")
        "CONTROL" -> translated(language, "Стандартні ролі контролю",
            "Standardrollen der Kontrolle", "Default control roles", "Стандартные роли контроля")
        else -> fallback
    }

fun localizedSystemRoleName(language: AppLanguage, code: String?, fallback: String): String =
    when (code?.uppercase()) {
        "LEADER" -> translated(language, "Керівник", "Leiter", "Leader", "Руководитель")
        "DEPUTY" -> translated(language, "Заступник", "Stellvertreter", "Deputy", "Заместитель")
        "SENIOR" -> translated(language, "Старший", "Senior", "Senior", "Старший")
        "EXECUTOR" -> translated(language, "Виконавець", "Ausführender", "Executor", "Исполнитель")
        "ASSISTANT" -> translated(language, "Помічник", "Assistent", "Assistant", "Помощник")
        "REVIEWER" -> translated(language, "Перевіряльник", "Prüfer", "Reviewer", "Проверяющий")
        "OBSERVER" -> translated(language, "Спостерігач", "Beobachter", "Observer", "Наблюдатель")
        else -> fallback
    }
