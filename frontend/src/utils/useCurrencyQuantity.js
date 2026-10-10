import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getPointTypes, getPointNameForms } from '../api/economyApi'
import { currencyQuantity } from './formatCurrencyAmount'

/** Hook shared by pages with currency amounts; preserves built-in fallbacks when API is older. */
export function useCurrencyQuantity(token, workspaceId) {
  const { i18n } = useTranslation()
  const [types, setTypes] = useState([])
  const [forms, setForms] = useState({})

  useEffect(() => {
    if (!workspaceId || !token) return
    let active = true
    getPointTypes(token, workspaceId).then(async result => {
      if (!active) return
      setTypes(result)
      const entries = await Promise.all(result.map(async type => {
        try { return [type.id, await getPointNameForms(token, workspaceId, type.id)] }
        catch { return [type.id, []] }
      }))
      if (active) setForms(Object.fromEntries(entries))
    }).catch(() => {})
    return () => { active = false }
  }, [token, workspaceId])

  return (amount, code, id = null, fallback = null) => {
    const type = types.find(item => (id != null && item.id === id) || item.code === code)
    return currencyQuantity(amount, {
      code: type?.code || code,
      name: fallback || type?.name || code,
      forms: forms[type?.id || id] || [],
      language: i18n.language,
    })
  }
}
