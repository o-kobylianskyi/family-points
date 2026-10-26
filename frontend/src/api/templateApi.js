import { apiRequest } from './client'

async function read(response) {
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
