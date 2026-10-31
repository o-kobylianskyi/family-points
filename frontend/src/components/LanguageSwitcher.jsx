import { useTranslation } from 'react-i18next'
import { changeLanguage } from '../i18n'

const languages = [
  { code: 'uk', label: 'Українська' },
  { code: 'de', label: 'Deutsch' },
  { code: 'en', label: 'English' },
  { code: 'ru', label: 'Русский' },
]

function LanguageSwitcher() {
  const { i18n } = useTranslation()

  const currentLanguage =
    i18n.resolvedLanguage || i18n.language

  return (
    <select
      className="language-switcher"
      value={currentLanguage}
      onChange={(event) =>
        changeLanguage(event.target.value)
      }
      aria-label="Language"
    >
      {languages.map((language) => (
        <option
          key={language.code}
          value={language.code}
        >
          {language.label}
        </option>
      ))}
    </select>
  )
}

export default LanguageSwitcher