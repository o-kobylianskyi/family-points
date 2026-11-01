import { apiRequest } from './client'

async function read(response) {
  const result = await response
  const body = await result.json().catch(() => null)
  if (!result.ok) {
    const error = new Error(body?.message || body?.detail || `Setup API: HTTP ${result.status}`)
    error.status = result.status
    throw error
  }
  return body
}

const workspace = id => {
  if (!/^\\d+$/.test(String(id))) throw new Error('INVALID_WORKSPACE_ID')
  return String(id)
}

// V46 metadata. Read-only: never start or complete setup from loading a page.
export const getSetupState = (token, workspaceId) =>
  read(apiRequest(`/workspaces/${workspace(workspaceId)}/setup`, { token, cache: 'no-store' }))

// V48 revision-fenced review is side-effect free, despite using POST. The
// shared apiRequest obtains the original workspace epoch before sending it.
export async function reviewSetupCatalog(token, workspaceId, expectedRevision, selectedCodes) {
  if (!Number.isSafeInteger(expectedRevision) || expectedRevision < 0) throw new Error('INVALID_SETUP_REVISION')
  if (!Array.isArray(selectedCodes) || selectedCodes.some(code => typeof code !== 'string' || !code.trim()) || new Set(selectedCodes).size !== selectedCodes.length)
    throw new Error('INVALID_SETUP_SELECTION')
  const result = await read(apiRequest(`/workspaces/${workspace(workspaceId)}/setup/review`, {
    token, method: 'POST', body: { expectedRevision, selectedCatalogCodes: selectedCodes }, cache: 'no-store',
  }))
  if (String(result?.workspaceId) !== String(workspaceId) || result?.revision !== expectedRevision ||
      result?.requiresExplicitConfirmation !== true || result?.createsActiveDefinitions !== false ||
      JSON.stringify(result?.selectedCodes) !== JSON.stringify(selectedCodes))
    throw new Error('SETUP_REVIEW_RESPONSE_INVALID')
  return result
}

// V47/V48 receipt lookup only. A timeout is never permission to issue a NEW operation UUID.
export const getSetupImportReceipt = (token, workspaceId, operationId) =>
  read(apiRequest(`/workspaces/${workspace(workspaceId)}/templates/imports/receipts/${validUuid(operationId)}`, { token, cache: 'no-store' }))
export const getSetupActivationReceipt = (token, workspaceId, operationId) =>
  read(apiRequest(`/workspaces/${workspace(workspaceId)}/templates/activations/receipts/${validUuid(operationId)}`, { token, cache: 'no-store' }))

function validUuid(value) {
  if (typeof value !== 'string' || !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value))
    throw new Error('INVALID_SETUP_OPERATION_ID')
  return value
}
