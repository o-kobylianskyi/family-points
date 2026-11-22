export const rewardRecoveryMessages = {
  uk: {
    heading: 'Перевірка придбання нагороди',
    check: 'Перевірити квитанцію',
    checking: 'Перевіряємо квитанцію…',
    unknown: 'Результат придбання невідомий. Початкову операцію збережено. Не повторюйте списання.',
    unavailable: 'Квитанція недоступна або операція не підтверджена. Повторне придбання заблоковано.',
    auth: 'Потрібна авторизація цього самого облікового запису. Операція збережена.',
    conflict: 'Конфлікт операції. Потрібна перевірка, повторне списання заборонене.',
    confirmed: 'Покупку підтверджено. Нагороду ще не використано.',
    redeemed: 'Покупку підтверджено. Нагороду вже використано.',
    cancelled: 'Історичну покупку знайдено, але нагороду скасовано. Перевірте історію балів і повернення окремо.',
    blocked: 'Придбання нагород тимчасово вимкнено до завершення перевірки безпеки.',
    operation: 'Операція',
    status: 'Стан квитанції',
    legacy: 'Збережено стару операцію без початкової ціни й типу балів. UUID не змінюється; автоматичне підтвердження та повторне придбання заблоковані.',
    originalPrice: 'Початкова вартість',

  },
  ru: {
    heading: 'Проверка покупки награды',
    check: 'Проверить квитанцию',
    checking: 'Проверяем квитанцию…',
    unknown: 'Результат покупки неизвестен. Исходная операция сохранена. Не повторяйте списание.',
    unavailable: 'Квитанция недоступна или операция не подтверждена. Повторная покупка заблокирована.',
    auth: 'Требуется авторизация той же учётной записи. Операция сохранена.',
    conflict: 'Конфликт операции. Требуется проверка, повторное списание запрещено.',
    confirmed: 'Покупка подтверждена. Награда ещё не использована.',
    redeemed: 'Покупка подтверждена. Награда уже использована.',
    cancelled: 'Историческая покупка найдена, но награда отменена. Проверьте историю баллов и возврат отдельно.',
    blocked: 'Покупка наград временно отключена до завершения проверки безопасности.',
    operation: 'Операция',
    status: 'Статус квитанции',
    legacy: 'Сохранена старая операция без исходной цены и типа баллов. UUID не меняется; автоматическое подтверждение и повторная покупка заблокированы.',
    originalPrice: 'Исходная стоимость',

  },
  de: {
    heading: 'Belohnungskauf prüfen',
    check: 'Beleg prüfen',
    checking: 'Beleg wird geprüft…',
    unknown: 'Das Kaufergebnis ist unbekannt. Der ursprüngliche Vorgang bleibt gespeichert. Nicht erneut abbuchen.',
    unavailable: 'Beleg nicht verfügbar oder Kauf nicht bestätigt. Erneuter Kauf ist gesperrt.',
    auth: 'Mit demselben Konto erneut anmelden. Der Vorgang bleibt gespeichert.',
    conflict: 'Vorgangskonflikt. Prüfung erforderlich; keine erneute Abbuchung.',
    confirmed: 'Kauf bestätigt. Belohnung noch nicht eingelöst.',
    redeemed: 'Kauf bestätigt. Belohnung bereits eingelöst.',
    cancelled: 'Historischer Kauf gefunden, aber Belohnung storniert. Punktebuchungen und Erstattung separat prüfen.',
    blocked: 'Belohnungskäufe bleiben bis zum Abschluss der Sicherheitsprüfung gesperrt.',
    operation: 'Vorgang',
    status: 'Belegstatus',
    legacy: 'Alter Vorgang ohne ursprünglichen Preis und Punkttyp erhalten. UUID bleibt unverändert; automatische Bestätigung und erneuter Kauf sind gesperrt.',
    originalPrice: 'Ursprünglicher Preis',

  },
  en: {
    heading: 'Verify reward purchase',
    check: 'Check receipt',
    checking: 'Checking receipt…',
    unknown: 'Purchase outcome is unknown. The original operation is preserved. Do not charge again.',
    unavailable: 'Receipt unavailable or purchase unconfirmed. Another purchase is blocked.',
    auth: 'Sign in using the same account. The operation is preserved.',
    conflict: 'Operation conflict. Review is required; another debit is prohibited.',
    confirmed: 'Purchase confirmed. Reward has not been redeemed.',
    redeemed: 'Purchase confirmed. Reward has already been redeemed.',
    cancelled: 'Historical purchase found, but reward was cancelled. Review point history and any refund separately.',
    blocked: 'Reward purchases are disabled until the security review is complete.',
    operation: 'Operation',
    status: 'Receipt status',
    legacy: 'Previous operation lacks the original price and point type. UUID is preserved; automatic confirmation and another purchase remain blocked.',    originalPrice: 'Original price',

  },
}

export function rewardRecoveryCopy(language) {
  return rewardRecoveryMessages[String(language || 'en').split('-')[0]] || rewardRecoveryMessages.en
}

export function rewardRecoveryStatusMessage(status, receipt, language) {
  const copy = rewardRecoveryCopy(language)
  if (status === 'CONFIRMED_PURCHASE') {
    return receipt?.purchase?.status === 'REDEEMED' ? copy.redeemed : copy.confirmed
  }
  if (status === 'HISTORICAL_PURCHASE_CANCELLED_HOLD') return copy.cancelled
  if (status === 'LEGACY_SNAPSHOT_MISSING_HOLD') return copy.legacy
  if (status === 'AUTH_REQUIRED_NO_REPLAY') return copy.auth
  if (status === 'CONFLICT_NO_REPLAY') return copy.conflict
  if (status === 'NOT_FOUND_NO_REPLAY' || status === 'READ_UNAVAILABLE_NO_REPLAY') return copy.unavailable
  return copy.unknown
}
