import { apiRequest } from './client'

const allowedResetCategories = new Set(['PRIVATE_TEMPLATES','TASK_DEFINITIONS','TASK_EXECUTIONS','TASK_SCHEDULE_SLOTS','REWARD_DEFINITIONS','REWARD_REQUESTS','REWARD_PURCHASES','MEMBER_GROUPS','WORK_NODES','MEDIA_ASSETS','MEDIA_BINDINGS','POINT_LEDGER','REPUTATION_LEDGER'])
function validateRequest(workspaceId, categories) {
  if (!/^[1-9][0-9]*$/.test(String(workspaceId)) || !Number.isSafeInteger(Number(workspaceId))) throw new Error('INVALID_WORKSPACE_ID')
  if (categories !== undefined && (!Array.isArray(categories) || categories.length === 0 || new Set(categories).size !== categories.length || categories.some(code => !allowedResetCategories.has(code)))) throw new Error('INVALID_RESET_SELECTION')
}
function validateResponse(data, workspaceId, categories) {
  if (!data || String(data.workspaceId) !== String(workspaceId) || data.executable !== false ||
      !Array.isArray(data.blockers) || !Array.isArray(data.selectedCategories) ||
      data.selectedCategories.length !== categories.length ||
      new Set(data.selectedCategories).size !== categories.length ||
      !categories.every(code => data.selectedCategories.includes(code))) throw new Error('RESET_PLAN_INVALID')
  return data
}

async function read(response) {
  response = await response
  if (!response.ok) {
    const error = new Error('RESET_API_ERROR')
    error.status = response.status
    throw error
  }
  return response.json()
}

export async function getResetPreview(token, workspaceId) {
  validateRequest(workspaceId)
  const result = await read(apiRequest(`/workspaces/${workspaceId}/data-reset/preview`, { token, cache: 'no-store' }))
  if (!result || String(result.workspaceId) !== String(workspaceId) ||
      result.executionEnabled !== false || result.completeDependencyPlan !== false ||
      !result.categories || typeof result.categories !== 'object' || Array.isArray(result.categories) ||
      Object.entries(result.categories).some(([code, item]) => !allowedResetCategories.has(code) ||
        !item || !Number.isSafeInteger(item.recordCount) || item.recordCount < 0))
    throw new Error('RESET_PREVIEW_INVALID')
  return result
}

export async function planReset(token, workspaceId, categories) {
  validateRequest(workspaceId, categories)
  const result = await read(apiRequest(`/workspaces/${workspaceId}/data-reset/plan`, {
    token, method: 'POST', body: { categories }, cache: 'no-store'
  }))
  return validateResponse(result, workspaceId, categories)
}

// Read-only PostgreSQL FK audit; this never authorizes deletion.
export async function getResetDependencyAudit(token, workspaceId) {
  validateRequest(workspaceId)
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

export async function inspectResetSelection(token, workspaceId, categories) {
  validateRequest(workspaceId, categories)
  const response = await apiRequest(`/workspaces/${workspaceId}/data-reset/dependency-audit/selection`, {
    token, method: 'POST', body: { categories }, cache: 'no-store',
  })
  if (!response.ok) {
    const error = new Error('RESET_AUDIT_SELECTION_UNAVAILABLE')
    error.status = response.status
    throw error
  }
  const result = await response.json()
  if (!Array.isArray(result?.relevantForeignKeys)) throw new Error('INVALID_RESET_SELECTION_AUDIT')
  return validateResponse(result, workspaceId, categories)
}
