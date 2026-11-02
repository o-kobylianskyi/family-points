import { apiRequest } from './client'

// WorkNode GROUP is a task-folder entity, not a MemberGroup actor.
const base = workspaceId => `/workspaces/${workspaceId}/work-node-groups`
async function read(result) {
  const response = await result
  if (!response.ok) {
    const data = await response.json().catch(() => ({}))
    const error = new Error(data.message || `WorkNode API: HTTP ${response.status}`)
    error.status = response.status
    throw error
  }
  return response.status === 204 ? null : response.json()
}
export const getWorkNodeGroups = (token, workspaceId) =>
  read(apiRequest(base(workspaceId), { token, cache: 'no-store' }))
export const createWorkNodeGroup = (token, workspaceId, payload) =>
  read(apiRequest(base(workspaceId), { token, method: 'POST', body: payload }))
export const updateWorkNodeGroup = (token, workspaceId, groupId, payload) =>
  read(apiRequest(`${base(workspaceId)}/${groupId}`, { token, method: 'PUT', body: payload }))
export const deleteWorkNodeGroup = (token, workspaceId, groupId) =>
  read(apiRequest(`${base(workspaceId)}/${groupId}`, { token, method: 'DELETE' }))
