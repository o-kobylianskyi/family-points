// V47 import operations: only a user-confirmed action creates an operation ID.
// Session storage is deliberately scoped to account, workspace and epoch; no
// credentials, access tokens, or request bodies with personal data are stored.
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
export const operationStorageKey = (accountId, workspaceId) =>
  `fp:onboarding-import:v47:${accountId}:${workspaceId}`

export function validImportOperation(record, workspaceId, epoch) {
  return !!record && record.version === 1 && record.workspaceId === String(workspaceId) &&
    Number.isSafeInteger(record.epoch) && record.epoch >= 0 &&
    (epoch === undefined || record.epoch === epoch) &&
    UUID.test(record.operationId) &&
    Array.isArray(record.codes) && record.codes.length > 0 && record.codes.length <= 142 &&
    record.codes.every(code => typeof code === 'string' && code.length > 0 && code.length < 100) &&
    new Set(record.codes).size === record.codes.length
}
export function readImportOperation(storage, key, workspaceId) {
  try {
    const record = JSON.parse(storage.getItem(key) || 'null')
    return validImportOperation(record, workspaceId) ? record : null
  } catch { return null }
}
export function storeImportOperation(storage, key, record) {
  if (!validImportOperation(record, record?.workspaceId)) throw new Error('INVALID_IMPORT_OPERATION')
  storage.setItem(key, JSON.stringify(record))
}
export function makeImportOperation(workspaceId, epoch, codes, uuid) {
  const record = { version: 1, workspaceId: String(workspaceId), epoch, codes: [...codes], operationId: uuid }
  if (!validImportOperation(record, workspaceId, epoch)) throw new Error('INVALID_IMPORT_OPERATION')
  return record
}
export function assertImportReceipt(receipt, operation) {
  if (!validImportOperation(operation, operation.workspaceId)) throw new Error('INVALID_IMPORT_OPERATION')
  const result = receipt?.importResult
  const selected = result?.selectedCodes
  if (receipt?.operationId !== operation.operationId || !Array.isArray(selected) ||
      selected.length !== operation.codes.length ||
      selected.some((code, index) => code !== operation.codes[index]) ||
      !Array.isArray(result?.alreadyImported) || !Array.isArray(result?.toImport) ||
      [...result.toImport, ...result.alreadyImported].length !== selected.length ||
      new Set([...result.toImport, ...result.alreadyImported]).size !== selected.length ||
      [...result.toImport, ...result.alreadyImported].some(code => !selected.includes(code))) {
    throw new Error('IMPORT_RECEIPT_MISMATCH')
  }
  return receipt
}
