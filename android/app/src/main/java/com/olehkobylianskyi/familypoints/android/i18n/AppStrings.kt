package com.olehkobylianskyi.familypoints.android.i18n

data class AppStrings(
    val loading: String,
    val loginSubtitle: String,
    val username: String,
    val password: String,
    val login: String,
    val loggingIn: String,
    val invalidCredentials: String,
    val showPassword: String,
    val hidePassword: String,
    val loginSuccessful: String,
    val logout: String,
    val language: String
)

fun strings(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.UK -> AppStrings(
        loading = "Завантаження...",
        loginSubtitle = "Сімейні завдання, бали та нагороди",
        username = "Логін",
        password = "Пароль",
        login = "Увійти",
        loggingIn = "Вхід...",
        invalidCredentials = "Неправильний логін або пароль",
        showPassword = "Показати пароль",
        hidePassword = "Приховати пароль",
        loginSuccessful = "Вхід успішний",
        logout = "Вийти",
        language = "Мова"
    )

    AppLanguage.DE -> AppStrings(
        loading = "Wird geladen...",
        loginSubtitle = "Familienaufgaben, Punkte und Belohnungen",
        username = "Benutzername",
        password = "Passwort",
        login = "Anmelden",
        loggingIn = "Anmeldung...",
        invalidCredentials = "Benutzername oder Passwort ist falsch",
        showPassword = "Passwort anzeigen",
        hidePassword = "Passwort ausblenden",
        loginSuccessful = "Anmeldung erfolgreich",
        logout = "Abmelden",
        language = "Sprache"
    )

    AppLanguage.EN -> AppStrings(
        loading = "Loading...",
        loginSubtitle = "Workspace tasks, points and rewards",
        username = "Username",
        password = "Password",
        login = "Log in",
        loggingIn = "Logging in...",
        invalidCredentials = "Incorrect username or password",
        showPassword = "Show password",
        hidePassword = "Hide password",
        loginSuccessful = "Logged in successfully",
        logout = "Log out",
        language = "Language"
    )

    AppLanguage.RU -> AppStrings(
        loading = "Загрузка...",
        loginSubtitle = "Семейные задания, баллы и награды",
        username = "Логин",
        password = "Пароль",
        login = "Войти",
        loggingIn = "Вход...",
        invalidCredentials = "Неверный логин или пароль",
        showPassword = "Показать пароль",
        hidePassword = "Скрыть пароль",
        loginSuccessful = "Вход выполнен",
        logout = "Выйти",
        language = "Язык"
    )
}
