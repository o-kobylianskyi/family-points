import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getPointTypes, getPointNameForms } from '../api/economyApi'
import { currencyQuantity } from './formatCurrencyAmount'

/** Hook shared by pages with currency amounts; preserves built-in fallbacks when API is older. */
export function useCurrencyQuantity(token, workspaceId) {
  const { i18n } = useTranslation()
  const scope = `${token ?? 'anonymous'}:${workspaceId}`
  const [snapshot, setSnapshot] = useState({scope:null,types:[],forms:{}})

  useEffect(() => {
    let active = true
    if (!workspaceId || !token) return () => { active = false }
    getPointTypes(token, workspaceId).then(async result => {
      if (!active) return
      if (!Array.isArray(result)) return
      setSnapshot({scope,types:result,forms:{}})
      const entries = await Promise.all(result.map(async type => {
        try { return [type.id, await getPointNameForms(token, workspaceId, type.id)] }
        catch { return [type.id, []] }
      }))
      if (active) setSnapshot({scope,types:result,forms:Object.fromEntries(entries)})
    }).catch(() => {})
    return () => { active = false }
  }, [token, workspaceId, scope])
  const types = snapshot.scope === scope ? snapshot.types : []
  const forms = snapshot.scope === scope ? snapshot.forms : {}

  return (amount, code, id = null, fallback = null) => {
    const type = types.find(item => (id != null && String(item.id) === String(id)) ||
      (code != null && String(item.code).toUpperCase() === String(code).toUpperCase()))
    return currencyQuantity(amount, {
      code: type?.code || code,
      name: fallback || type?.name || code,
      forms: forms[type?.id || id] || [],
      language: i18n.language,
    })
  }
}
