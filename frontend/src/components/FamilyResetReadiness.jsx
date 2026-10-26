import { useTranslation } from 'react-i18next'

const copy = {
  uk: { title: 'Вибіркове очищення даних сім’ї', status: 'Очікується безпечне підключення до сервера', description: 'Очищення стане доступним тільки після перевірки прав адміністратора, попереднього перегляду складу даних і повторного підтвердження особи.', retained: 'Обліковий запис, учасники сім’ї, інші сім’ї та глобальні шаблони залишаться захищеними.', caution: 'Зараз очищення недоступне. Жодні дані не змінюються.' },
  ru: { title: 'Выборочная очистка данных семьи', status: 'Ожидается безопасное подключение к серверу', description: 'Очистка станет доступной только после проверки прав администратора, предварительного просмотра данных и повторного подтверждения личности.', retained: 'Учётная запись, участники семьи, другие семьи и глобальные шаблоны останутся защищены.', caution: 'Сейчас очистка недоступна. Данные не изменяются.' },
  de: { title: 'Selektives Zurücksetzen der Familiendaten', status: 'Sichere Serveranbindung steht noch aus', description: 'Das Zurücksetzen wird erst nach Berechtigungsprüfung, Vorschau der betroffenen Daten und erneuter Authentifizierung verfügbar.', retained: 'Benutzerkonto, Familienmitglieder, andere Familien und globale Vorlagen bleiben geschützt.', caution: 'Zurzeit ist kein Zurücksetzen möglich. Es werden keine Daten geändert.' },
  en: { title: 'Selective family data reset', status: 'Secure server integration pending', description: 'Reset will become available only after administrator authorization, a preview of affected data, and fresh identity verification.', retained: 'Your account, family members, other families and global templates remain protected.', caution: 'Reset is currently unavailable. No data is changed.' }
}
export default function FamilyResetReadiness() {
  const { i18n } = useTranslation()
  const strings = copy[i18n.language.split('-')[0]] || copy.en
  return <section className="settings-section" aria-labelledby="family-reset-heading">
    <h2 id="family-reset-heading">{strings.title}</h2>
    <p><strong>{strings.status}</strong></p>
    <p>{strings.description}</p>
    <p>{strings.retained}</p>
    <p role="status">{strings.caution}</p>
  </section>
}
