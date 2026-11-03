// V48 activation receipts: each confirmed creation uses an immutable payload,
// operation UUID and family epoch. An uncertain response is never blind-retried.
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
const kinds = new Set(['TASK', 'REWARD'])
export const activationStorageKey = (accountId, workspaceId, kind) =>
  `fp:template-activation:v48:${accountId}:${workspaceId}:${kind}`

export function validActivation(record, workspaceId, kind, epoch) {
  return !!record && record.version === 1 &&
    String(record.workspaceId) === String(workspaceId) &&
    kinds.has(record.kind) && record.kind === kind &&
    Number.isSafeInteger(record.epoch) && record.epoch >= 0 &&
    (epoch === undefined || record.epoch === epoch) &&
    Number.isSafeInteger(record.templateId) && record.templateId > 0 &&
    UUID.test(String(record.operationId)) &&
    !!record.request && typeof record.request === 'object' &&
    !Array.isArray(record.request)
}
export function makeActivation(workspaceId, kind, epoch, templateId, request, operationId) {
  // Copy JSON payload once so later form edits cannot change the retry fingerprint.
  const record = {
    version: 1, workspaceId: String(workspaceId), kind, epoch,
    templateId: Number(templateId), operationId,
    request: JSON.parse(JSON.stringify(request)),
  }
  if (!validActivation(record, workspaceId, kind, epoch)) throw new Error('INVALID_ACTIVATION')
  return record
}
export function readActivation(storage, key, workspaceId, kind) {
  try {
    const record = JSON.parse(storage.getItem(key) || 'null')
    return validActivation(record, workspaceId, kind) ? record : null
  } catch { return null }
}
export function saveActivation(storage, key, record) {
  if (!validActivation(record, record?.workspaceId, record?.kind)) throw new Error('INVALID_ACTIVATION')
  storage.setItem(key, JSON.stringify(record))
}
export function verifyActivationReceipt(receipt, record) {
  if (!validActivation(record, record?.workspaceId, record?.kind) ||
      receipt?.operationId !== record.operationId ||
      String(receipt?.templateId) !== String(record.templateId) ||
      receipt?.kind !== record.kind ||
      !Number.isSafeInteger(receipt?.createdDefinitionId) ||
      receipt.createdDefinitionId <= 0) throw new Error('INVALID_ACTIVATION_RECEIPT')
  return receipt
}
