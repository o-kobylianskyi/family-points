package com.olehkobylianskyi.familypoints.android.i18n

data class DashboardStrings(
    val welcomePrefix: String,
    val todayWorkspace: String,
    val myBalance: String,
    val points: String,
    val myTasks: String,
    val activeToday: String,
    val availableToClaim: String,
    val openTasks: String,
    val today: String,
    val currentTasks: String,
    val allTasks: String,
    val noActiveTasks: String,
    val quickActions: String,
    val mainSections: String,
    val tasks: String,
    val rewards: String,
    val members: String,
    val settings: String,
    val dashboard: String,
    val menu: String,
    val logout: String,
    val loading: String,
    val loadFailed: String,
    val notConvertedYet: String
)

fun dashboardStrings(language: AppLanguage): DashboardStrings = when (language) {
    AppLanguage.UK -> DashboardStrings(
        welcomePrefix = "Вітаю",
        todayWorkspace = "Ваш робочий простір на сьогодні",
        myBalance = "Мій баланс",
        points = "балів",
        myTasks = "Мої завдання",
        activeToday = "активних сьогодні",
        availableToClaim = "Доступно взяти",
        openTasks = "відкритих завдань",
        today = "Сьогодні",
        currentTasks = "Ваші актуальні завдання",
        allTasks = "Усі завдання",
        noActiveTasks = "На сьогодні активних завдань немає.",
        quickActions = "Швидкі дії",
        mainSections = "Основні розділи",
        tasks = "Завдання",
        rewards = "Нагороди",
        members = "Учасники",
        settings = "Налаштування",
        dashboard = "Головна",
        menu = "Меню",
        logout = "Вийти",
        loading = "Завантаження...",
        loadFailed = "Не вдалося завантажити головну сторінку",
        notConvertedYet = "Цей екран ще не перенесений в Android"
    )

    AppLanguage.DE -> DashboardStrings(
        welcomePrefix = "Willkommen",
        todayWorkspace = "Ihr Arbeitsbereich für heute",
        myBalance = "Mein Guthaben",
        points = "Punkte",
        myTasks = "Meine Aufgaben",
        activeToday = "heute aktiv",
        availableToClaim = "Verfügbar",
        openTasks = "offene Aufgaben",
        today = "Heute",
        currentTasks = "Ihre aktuellen Aufgaben",
        allTasks = "Alle Aufgaben",
        noActiveTasks = "Für heute gibt es keine aktiven Aufgaben.",
        quickActions = "Schnellzugriff",
        mainSections = "Hauptbereiche",
        tasks = "Aufgaben",
        rewards = "Belohnungen",
        members = "Mitglieder",
        settings = "Einstellungen",
        dashboard = "Übersicht",
        menu = "Menü",
        logout = "Abmelden",
        loading = "Wird geladen...",
        loadFailed = "Startseite konnte nicht geladen werden",
        notConvertedYet = "Dieser Bildschirm wurde noch nicht auf Android übertragen"
    )

    AppLanguage.EN -> DashboardStrings(
        welcomePrefix = "Welcome",
        todayWorkspace = "Your workspace for today",
        myBalance = "My balance",
        points = "points",
        myTasks = "My tasks",
        activeToday = "active today",
        availableToClaim = "Available to claim",
        openTasks = "open tasks",
        today = "Today",
        currentTasks = "Your current tasks",
        allTasks = "All tasks",
        noActiveTasks = "There are no active tasks for today.",
        quickActions = "Quick actions",
        mainSections = "Main sections",
        tasks = "Tasks",
        rewards = "Rewards",
        members = "Members",
        settings = "Settings",
        dashboard = "Dashboard",
        menu = "Menu",
        logout = "Log out",
        loading = "Loading...",
        loadFailed = "Could not load dashboard",
        notConvertedYet = "This screen has not been converted to Android yet"
    )

    AppLanguage.RU -> DashboardStrings(
        welcomePrefix = "Привет",
        todayWorkspace = "Ваше рабочее пространство на сегодня",
        myBalance = "Мой баланс",
        points = "баллов",
        myTasks = "Мои задания",
        activeToday = "активных сегодня",
        availableToClaim = "Доступно взять",
        openTasks = "открытых заданий",
        today = "Сегодня",
        currentTasks = "Ваши актуальные задания",
        allTasks = "Все задания",
        noActiveTasks = "На сегодня активных заданий нет.",
        quickActions = "Быстрые действия",
        mainSections = "Основные разделы",
        tasks = "Задания",
        rewards = "Награды",
        members = "Участники",
        settings = "Настройки",
        dashboard = "Главная",
        menu = "Меню",
        logout = "Выйти",
        loading = "Загрузка...",
        loadFailed = "Не удалось загрузить главную страницу",
        notConvertedYet = "Этот экран ещё не перенесён на Android"
    )
}

fun taskStatusLabel(language: AppLanguage, status: String): String {
    return when (language) {
        AppLanguage.UK -> when (status) {
            "PENDING" -> "Очікує"
            "IN_PROGRESS" -> "Виконується"
            "COMPLETED" -> "Виконано"
            "MISSED" -> "Пропущено"
            "EXCUSED" -> "Звільнено"
            "PAUSED" -> "Призупинено"
            "CANCELLED" -> "Скасовано"
            "RELEASED" -> "Відмовлено"
            else -> status
        }

        AppLanguage.DE -> when (status) {
            "PENDING" -> "Ausstehend"
            "IN_PROGRESS" -> "In Bearbeitung"
            "COMPLETED" -> "Erledigt"
            "MISSED" -> "Verpasst"
            "EXCUSED" -> "Entschuldigt"
            "PAUSED" -> "Pausiert"
            "CANCELLED" -> "Abgebrochen"
            "RELEASED" -> "Freigegeben"
            else -> status
        }

        AppLanguage.EN -> when (status) {
            "PENDING" -> "Pending"
            "IN_PROGRESS" -> "In progress"
            "COMPLETED" -> "Completed"
            "MISSED" -> "Missed"
            "EXCUSED" -> "Excused"
            "PAUSED" -> "Paused"
            "CANCELLED" -> "Cancelled"
            "RELEASED" -> "Released"
            else -> status
        }

        AppLanguage.RU -> when (status) {
            "PENDING" -> "Ожидает"
            "IN_PROGRESS" -> "Выполняется"
            "COMPLETED" -> "Выполнено"
            "MISSED" -> "Пропущено"
            "EXCUSED" -> "Освобождено"
            "PAUSED" -> "Приостановлено"
            "CANCELLED" -> "Отменено"
            "RELEASED" -> "Отказано"
            else -> status
        }
    }
}
