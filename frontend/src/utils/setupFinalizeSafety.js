// V49 immutable receipt validation. The selected-code order, family, epoch
// and revision form part of the reviewed/confirmed operation; a syntactically
// successful HTTP response must never finish a different local operation.
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
export function validSetupFinalizeOperation(op, workspaceId, accountId) {
  return !!op && UUID.test(String(op.operationId || '')) &&
    String(op.workspaceId) === String(workspaceId) &&
    String(op.accountId) === String(accountId) &&
    Number.isSafeInteger(op.epoch) && op.epoch >= 0 &&
    Number.isSafeInteger(op.revision) && op.revision >= 0 &&
    Array.isArray(op.codes) && op.codes.length > 0 && op.codes.length <= 142 &&
    op.codes.every(code => typeof code === 'string' && !!code.trim()) &&
    new Set(op.codes).size === op.codes.length
}
export function assertSetupFinalizeReceipt(receipt, operation) {
  if (!validSetupFinalizeOperation(operation, operation?.workspaceId, operation?.accountId) ||
      receipt?.operationId !== operation.operationId ||
      String(receipt?.workspaceId) !== String(operation.workspaceId) ||
      receipt?.dataEpoch !== operation.epoch ||
      receipt?.revision !== operation.revision + 1 ||
      receipt?.setupStatus !== 'COMPLETED' ||
      !Array.isArray(receipt?.importResult?.selectedCodes) ||
      JSON.stringify(receipt.importResult.selectedCodes) !== JSON.stringify(operation.codes) ||
      !Array.isArray(receipt.importResult.toImport) ||
      !Array.isArray(receipt.importResult.alreadyImported) ||
      receipt.importResult.toImport.length + receipt.importResult.alreadyImported.length !== operation.codes.length ||
      new Set([...receipt.importResult.toImport, ...receipt.importResult.alreadyImported]).size !== operation.codes.length ||
      [...receipt.importResult.toImport, ...receipt.importResult.alreadyImported].some(code => !operation.codes.includes(code))) {
    throw new Error('SETUP_FINALIZE_RECEIPT_MISMATCH')
  }
  return receipt
}

/**
 * One reconciliation policy for page startup and cross-tab storage events.
 * A missing shared marker does not invalidate this tab's original operation.
 * A malformed or competing operation fails closed until manually resolved.
 * Storage is treated as untrusted and cannot authorize a new POST.
 */
export function reconcileSetupPending(session, shared, workspaceId, accountId) {
  if (session && !validSetupFinalizeOperation(session, workspaceId, accountId)) return {blocked: true}
  if (shared && !validSetupFinalizeOperation(shared, workspaceId, accountId)) return {blocked: true}
  if (session && shared && (
    session.operationId !== shared.operationId ||
    session.revision !== shared.revision ||
    session.epoch !== shared.epoch ||
    JSON.stringify(session.codes) !== JSON.stringify(shared.codes)
  )) return {blocked: true}
  return shared || session || null
}
