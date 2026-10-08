package com.olehkobylianskyi.familypoints.android.i18n

data class AppStrings(
    val loginSubtitle: String,
    val username: String,
    val password: String,
    val login: String,
    val enterCredentials: String,
    val invalidCredentials: String,
    val serverError: String,
    val noConnection: String,
    val loginFailed: String,
    val loginSuccessful: String,
    val logout: String,
    val language: String
)

fun strings(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.UK -> AppStrings(
        loginSubtitle = "Увійдіть у свій акаунт",
        username = "Логін",
        password = "Пароль",
        login = "Увійти",
        enterCredentials = "Введіть логін і пароль",
        invalidCredentials = "Неправильний логін або пароль",
        serverError = "Помилка сервера",
        noConnection = "Немає з'єднання із сервером",
        loginFailed = "Не вдалося виконати вхід",
        loginSuccessful = "Вхід успішний",
        logout = "Вийти",
        language = "Мова"
    )

    AppLanguage.DE -> AppStrings(
        loginSubtitle = "Melden Sie sich bei Ihrem Konto an",
        username = "Benutzername",
        password = "Passwort",
        login = "Anmelden",
        enterCredentials = "Benutzername und Passwort eingeben",
        invalidCredentials = "Benutzername oder Passwort ist falsch",
        serverError = "Serverfehler",
        noConnection = "Keine Verbindung zum Server",
        loginFailed = "Anmeldung fehlgeschlagen",
        loginSuccessful = "Anmeldung erfolgreich",
        logout = "Abmelden",
        language = "Sprache"
    )

    AppLanguage.EN -> AppStrings(
        loginSubtitle = "Sign in to your account",
        username = "Username",
        password = "Password",
        login = "Sign in",
        enterCredentials = "Enter username and password",
        invalidCredentials = "Incorrect username or password",
        serverError = "Server error",
        noConnection = "No connection to the server",
        loginFailed = "Sign in failed",
        loginSuccessful = "Signed in successfully",
        logout = "Sign out",
        language = "Language"
    )

    AppLanguage.RU -> AppStrings(
        loginSubtitle = "Войдите в свой аккаунт",
        username = "Логин",
        password = "Пароль",
        login = "Войти",
        enterCredentials = "Введите логин и пароль",
        invalidCredentials = "Неправильный логин или пароль",
        serverError = "Ошибка сервера",
        noConnection = "Нет соединения с сервером",
        loginFailed = "Не удалось выполнить вход",
        loginSuccessful = "Вход выполнен",
        logout = "Выйти",
        language = "Язык"
    )
}
