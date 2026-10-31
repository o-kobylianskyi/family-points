import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

import uk from './locales/uk.json'
import de from './locales/de.json'
import en from './locales/en.json'
import ru from './locales/ru.json'

const savedLanguage = localStorage.getItem('language')

const browserLanguage =
  navigator.language?.split('-')[0]

const supportedLanguages = [
  'uk',
  'de',
  'en',
  'ru',
]

const initialLanguage =
  savedLanguage ||
  (
    supportedLanguages.includes(browserLanguage)
      ? browserLanguage
      : 'uk'
  )

i18n
  .use(initReactI18next)
  .init({
    resources: {
      uk: { translation: uk },
      de: { translation: de },
      en: { translation: en },
      ru: { translation: ru },
    },

    lng: initialLanguage,
    fallbackLng: 'uk',

    interpolation: {
      escapeValue: false,
    },
  })

export function changeLanguage(language) {
  if (!supportedLanguages.includes(language)) {
    return
  }

  localStorage.setItem('language', language)
  i18n.changeLanguage(language)
}

export default i18n