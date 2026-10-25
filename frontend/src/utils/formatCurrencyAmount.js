/** Display quantities using saved forms first, then built-in defaults for standard currencies. */
export const standardCurrencyForms = {
  POINTS: {
    uk: { one: 'бал', few: 'бали', many: 'балів' },
    ru: { one: 'балл', few: 'балла', many: 'баллов' },
    en: { one: 'point', few: 'points', many: 'points' },
    de: { one: 'Punkt', few: 'Punkte', many: 'Punkte' },
  },
  COPPER: {
    uk: { one: 'мідна монета', few: 'мідні монети', many: 'мідних монет' },
    ru: { one: 'медная монета', few: 'медные монеты', many: 'медных монет' },
    en: { one: 'copper coin', few: 'copper coins', many: 'copper coins' },
    de: { one: 'Kupfermünze', few: 'Kupfermünzen', many: 'Kupfermünzen' },
  },
  SILVER: {
    uk: { one: 'срібна монета', few: 'срібні монети', many: 'срібних монет' },
    ru: { one: 'серебряная монета', few: 'серебряные монеты', many: 'серебряных монет' },
    en: { one: 'silver coin', few: 'silver coins', many: 'silver coins' },
    de: { one: 'Silbermünze', few: 'Silbermünzen', many: 'Silbermünzen' },
  },
  GOLD: {
    uk: { one: 'золота монета', few: 'золоті монети', many: 'золотих монет' },
    ru: { one: 'золотая монета', few: 'золотые монеты', many: 'золотых монет' },
    en: { one: 'gold coin', few: 'gold coins', many: 'gold coins' },
    de: { one: 'Goldmünze', few: 'Goldmünzen', many: 'Goldmünzen' },
  },
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
  const fallback = standardCurrencyForms[String(code).toUpperCase()]?.[lang]
  const label = configured?.[kind] || fallback?.[kind] || name || code || ''
  return `${amount} ${label}`.trim()
}
