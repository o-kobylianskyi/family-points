import { apiRequest } from './client'

async function read(response) {
  response = await response
  if (!response.ok) {
    const error = new Error('RESET_API_ERROR')
    error.status = response.status
    throw error
  }
  return response.json()
}

export const getResetPreview = (token, workspaceId) =>
  read(apiRequest(`/workspaces/${workspaceId}/data-reset/preview`, { token, cache: 'no-store' }))

export const planReset = (token, workspaceId, categories) =>
  read(apiRequest(`/workspaces/${workspaceId}/data-reset/plan`, {
    token, method: 'POST', body: { categories }
  }))

// Read-only PostgreSQL FK audit; this never authorizes deletion.
export async function getResetDependencyAudit(token, workspaceId) {
  if (!/^\d+$/.test(String(workspaceId))) throw new Error('INVALID_WORKSPACE_ID')
  const response = await apiRequest(`/workspaces/${workspaceId}/data-reset/dependency-audit`, {
    token, cache: 'no-store',
  })
  if (!response.ok) {
    const error = new Error('DEPENDENCY_AUDIT_UNAVAILABLE')
    error.status = response.status
    throw error
  }
  const result = await response.json()
  if (String(result?.workspaceId) !== String(workspaceId) ||
      !Array.isArray(result.blockers) || !Array.isArray(result.foreignKeys) ||
      result.executionEnabled !== false)
    throw new Error('INVALID_DEPENDENCY_AUDIT')
  return result
}
