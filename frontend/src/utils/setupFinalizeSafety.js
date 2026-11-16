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
  if (session && shared && !sameSetupFinalizeOperation(session, shared)) return {blocked: true}
  return shared || session || null
}

export const SETUP_STEP_CODES = Object.freeze(['MEMBERS','MEMBER_GROUPS','WORK_NODE_GROUPS','CATALOG_SELECTION','TASK_REVIEW','REWARD_REVIEW','FINAL_REVIEW'])

/** Avoid redundant step PATCH requests when the requested decision already exists.
 * This is a UI guard only; server revision and permission checks remain authoritative. */
export function canChangeSetupStep(state, code, desiredStatus) {
  return !!state && state.status !== 'COMPLETED' &&
    SETUP_STEP_CODES.includes(code) &&
    ['COMPLETE', 'SKIPPED'].includes(desiredStatus) &&
    state.steps?.[code] !== desiredStatus
}

// New V46 families legitimately have empty steps until parents select them.
export function assertSetupState(state, workspaceId) {
  const steps = state?.steps
  if (!state || String(state.workspaceId) !== String(workspaceId) ||
      !Number.isSafeInteger(state.dataEpoch) || state.dataEpoch < 0 ||
      !Number.isSafeInteger(state.revision) || state.revision < 0 ||
      !['NOT_STARTED','IN_PROGRESS','COMPLETED'].includes(state.status) ||
      !steps || Array.isArray(steps) || typeof steps !== 'object' ||
      Object.entries(steps).some(([code,status]) =>
        !SETUP_STEP_CODES.includes(code) || !['PENDING','COMPLETE','SKIPPED'].includes(status)) ||
      (state.status === 'COMPLETED' && (Object.keys(steps).length !== SETUP_STEP_CODES.length ||
        Object.values(steps).some(value => !['COMPLETE','SKIPPED'].includes(value)))))
    throw new Error('SETUP_STATE_MISMATCH')
  return state
}
export function assertSetupReview(review, state, codes) {
  if (!Array.isArray(codes) || !codes.length ||
      codes.some(code => typeof code !== 'string' || !code.trim()) ||
      new Set(codes).size !== codes.length || !review ||
      String(review.workspaceId) !== String(state?.workspaceId) ||
      review.dataEpoch !== state?.dataEpoch || review.revision !== state?.revision ||
      review.setupStatus !== state?.status ||
      review.requiresExplicitConfirmation !== true || review.createsActiveDefinitions !== false ||
      !Array.isArray(review.selectedCodes) ||
      JSON.stringify(review.selectedCodes) !== JSON.stringify(codes) ||
      !Array.isArray(review.toImport) || !Array.isArray(review.alreadyImported) ||
      !Number.isSafeInteger(review.selectedCount) || review.selectedCount !== codes.length ||
      !Number.isSafeInteger(review.toImportCount) || review.toImportCount !== review.toImport.length ||
      !Number.isSafeInteger(review.alreadyImportedCount) ||
      review.alreadyImportedCount !== review.alreadyImported.length ||
      review.toImport.length + review.alreadyImported.length !== codes.length ||
      new Set([...review.toImport,...review.alreadyImported]).size !== codes.length ||
      [...review.toImport,...review.alreadyImported].some(code => !codes.includes(code)))
    throw new Error('SETUP_REVIEW_MISMATCH')
  return review
}

/** Ensure a successful step PATCH did not cross a workspace reset/epoch boundary.
 * The API layer already checks revision and target step; this guards epoch too. */
export function assertSetupStepTransition(before, after, step, status) {
  assertSetupState(before, before?.workspaceId)
  assertSetupState(after, before.workspaceId)
  // A step PATCH cannot finalize setup or rewrite decisions for other steps.
  // The server may add explicit PENDING entries, but pre-existing decisions
  // must remain unchanged unless this is the step the parent selected.
  if (!SETUP_STEP_CODES.includes(step) || !['COMPLETE', 'SKIPPED'].includes(status) ||
      before.status === 'COMPLETED' || after.status !== 'IN_PROGRESS' ||
      after.dataEpoch !== before.dataEpoch || after.revision !== before.revision + 1 ||
      after.steps?.[step] !== status ||
      Object.entries(before.steps).some(([code, value]) =>
        code !== step && after.steps?.[code] !== value)) {
    throw new Error('SETUP_STEP_TRANSITION_MISMATCH')
  }
  return after
}

/**
 * A lost PATCH response is NOT permission to automatically replay the write.
 * The subsequent GET is authoritative only for visible progress: it cannot
 * prove which browser tab made an otherwise identical change.
 */
export function reconcileSetupStepAfterFailure(before, observed, step, desiredStatus) {
  assertSetupState(before, before?.workspaceId)
  assertSetupState(observed, before.workspaceId)
  if (!SETUP_STEP_CODES.includes(step) || !['COMPLETE','SKIPPED'].includes(desiredStatus))
    throw new Error('SETUP_STEP_RECONCILIATION_INVALID')
  if (observed.dataEpoch !== before.dataEpoch || observed.revision < before.revision)
    throw new Error('SETUP_STEP_SCOPE_CHANGED')
  return observed.revision === before.revision + 1 &&
    observed.steps?.[step] === desiredStatus ? 'MATCHED' : 'UNCONFIRMED'
}

/** UUID alone is not an operation identity: all scoped immutable fields must match. */
export function sameSetupFinalizeOperation(a, b) {
  return validSetupFinalizeOperation(a, a?.workspaceId, a?.accountId) &&
    validSetupFinalizeOperation(b, a.workspaceId, a.accountId) &&
    a.operationId === b.operationId &&
    a.epoch === b.epoch && a.revision === b.revision &&
    JSON.stringify(a.codes) === JSON.stringify(b.codes)
}

/** A durable receipt alone does not permit the UI to unblock stale server state. */
export function assertSetupCompletionAfterReceipt(state, operation) {
  assertSetupState(state, operation?.workspaceId)
  if (!validSetupFinalizeOperation(operation, operation.workspaceId, operation.accountId) ||
      state.dataEpoch !== operation.epoch ||
      state.revision !== operation.revision + 1 ||
      state.status !== 'COMPLETED' ||
      !SETUP_STEP_CODES.every(code => ['COMPLETE','SKIPPED'].includes(state.steps?.[code])))
    throw new Error('SETUP_COMPLETION_STATE_MISMATCH')
  return state
}
