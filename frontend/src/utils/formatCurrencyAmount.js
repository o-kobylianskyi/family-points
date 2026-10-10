/** Display a quantity using configured forms, with built-in POINTS fallback. */
const builtIn = {
  uk: { one: 'бал', few: 'бали', many: 'балів' },
  ru: { one: 'балл', few: 'балла', many: 'баллов' },
  en: { one: 'point', few: 'points', many: 'points' },
  de: { one: 'Punkt', few: 'Punkte', many: 'Punkte' },
}

export function currencyQuantity(amount, { code, name, forms = [], language = 'uk' } = {}) {
  const n = Math.abs(Number(amount))
  if (!Number.isFinite(n)) return String(amount)
  const lang = String(language).split('-')[0].toLowerCase()
  const integer = Math.trunc(n)
  const lastTwo = integer % 100
  const last = integer % 10
  const kind = (lang === 'uk' || lang === 'ru')
    ? (lastTwo >= 11 && lastTwo <= 14 ? 'many' : last === 1 ? 'one' : last >= 2 && last <= 4 ? 'few' : 'many')
    : (n === 1 ? 'one' : 'many')
  const configured = Array.isArray(forms) ? forms.find(f => f.language === lang) : null
  const fallback = String(code).toUpperCase() === 'POINTS' ? builtIn[lang] : null
  const label = configured?.[kind] || fallback?.[kind] || name || code || ''
  return `${amount} ${label}`.trim()
}
