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
