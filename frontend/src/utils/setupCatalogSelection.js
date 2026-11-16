// Catalog V49 selection is read-only until an explicit reviewed finalize.
// Validate instead of silently dropping corrupt/foreign catalog entries.
export function validateSetupCatalog(data) {
  if (!data || !Array.isArray(data.templates) || !Array.isArray(data.packs)) throw new Error('INVALID_SETUP_CATALOG')
  const codes = new Set()
  for (const item of data.templates) {
    if (!item || typeof item.code !== 'string' || !item.code.trim() ||
        !['TASK', 'REWARD'].includes(item.kind) || codes.has(item.code)) throw new Error('INVALID_SETUP_TEMPLATE')
    codes.add(item.code)
  }
  const packCodes = new Set()
  for (const pack of data.packs) {
    if (!pack || typeof pack.code !== 'string' || !pack.code.trim() ||
        packCodes.has(pack.code) || !Array.isArray(pack.templateCodes) ||
        pack.templateCodes.some(code => !codes.has(code)) ||
        new Set(pack.templateCodes).size !== pack.templateCodes.length) throw new Error('INVALID_SETUP_PACK')
    packCodes.add(pack.code)
  }
  return data
}

export function selectSetupMatching(previous, matchingCodes, checked, catalogCodes) {
  const selectable = new Set(catalogCodes)
  const matches = new Set(matchingCodes)
  if ([...matches].some(code => !selectable.has(code))) throw new Error('INVALID_SETUP_SELECTION')
  const retained = previous.filter(code => selectable.has(code) && !matches.has(code))
  if (!checked) return retained
  return [...new Set([...retained, ...previous.filter(code => selectable.has(code) && matches.has(code)), ...matches])]
}
