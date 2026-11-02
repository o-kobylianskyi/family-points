import { apiRequest } from './client'

async function read(response) {
  response = await response
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    const error = new Error(body.message || body.detail || `Template API: HTTP ${response.status}`)
    error.status = response.status
    throw error
  }
  return response.status === 204 || response.status === 201 && response.headers.get('content-length') === '0'
    ? null : response.text().then(text => text ? JSON.parse(text) : null)
}
export const getTemplateCatalog = token => read(apiRequest('/template-catalog', { token }))
export const getPrivateTemplates = (token, workspaceId) => read(apiRequest(`/workspaces/${workspaceId}/templates`, { token }))
export const previewTemplateImport = (token, workspaceId, selectedCodes) =>
  read(apiRequest(`/workspaces/${workspaceId}/templates/imports/preview`, { token, method: 'POST', body: { selectedCodes } }))
export const importTemplates = (token, workspaceId, selectedCodes) =>
  read(apiRequest(`/workspaces/${workspaceId}/templates/imports`, { token, method: 'POST', body: { selectedCodes } }))
export const updatePrivateTemplate = (token, workspaceId, id, body) =>
  read(apiRequest(`/workspaces/${workspaceId}/templates/${id}`, { token, method: 'PUT', body }))
export const deletePrivateTemplate = (token, workspaceId, id) =>
  read(apiRequest(`/workspaces/${workspaceId}/templates/${id}`, { token, method: 'DELETE' }))

export const getTemplateCreationDraft = (token, workspaceId, templateId) =>
  read(apiRequest(`/workspaces/${workspaceId}/templates/${templateId}/creation-draft`, { token }))

// Explicit parent action; changes only server-returned creation draft, not persisted templates.
export const applyTemplateEconomySuggestion = (token, workspaceId, templateId) =>
  read(apiRequest(`/workspaces/${workspaceId}/templates/${templateId}/creation-draft/apply-economy-suggestion`, { token, method: 'POST' }))


// V47/V48: durable receipts are separate from creation DTOs. These calls are
// opt-in: existing editors are NOT silently redirected to activation endpoints.
// apiRequest owns fresh workspace data-epoch handshake and prevents overriding it.
const operationHeaders = operationId => {
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(String(operationId))) {
    throw new Error('INVALID_TEMPLATE_OPERATION_ID')
  }
  return { 'Idempotency-Key': operationId }
}
export const importTemplatesIdempotent = (token, workspaceId, selectedCodes, operationId) =>
  read(apiRequest(`/workspaces/${workspaceId}/templates/imports/idempotent`, {
    token, method: 'POST', body: { selectedCodes }, headers: operationHeaders(operationId)
  }))
export const getTemplateImportReceipt = (token, workspaceId, operationId) =>
  read(apiRequest(`/workspaces/${workspaceId}/templates/imports/receipts/${operationId}`, {
    token, cache: 'no-store', headers: operationHeaders(operationId)
  }))
export const activateTaskTemplateIdempotent = (token, workspaceId, templateId, request, operationId) =>
  read(apiRequest(`/workspaces/${workspaceId}/templates/${templateId}/activate-task/idempotent`, {
    token, method: 'POST', body: request, headers: operationHeaders(operationId)
  }))
export const activateRewardTemplateIdempotent = (token, workspaceId, templateId, request, operationId) =>
  read(apiRequest(`/workspaces/${workspaceId}/templates/${templateId}/activate-reward/idempotent`, {
    token, method: 'POST', body: request, headers: operationHeaders(operationId)
  }))
export const getTemplateActivationReceipt = (token, workspaceId, operationId) =>
  read(apiRequest(`/workspaces/${workspaceId}/templates/activations/receipts/${operationId}`, {
    token, cache: 'no-store', headers: operationHeaders(operationId)
  }))
