import { apiRequest } from './client'

async function handle(request, fallback) {
  const response = await request
  if (!response.ok) {
    let message = fallback
    try {
      const body = await response.json()
      if (body.message) message = body.message
    } catch {}
    throw new Error(message)
  }
  if (response.status === 204) return null
  return response.json()
}

const setsBase = workspaceId => `/workspaces/${workspaceId}/role-sets`
const rolesBase = workspaceId => `/workspaces/${workspaceId}/role-definitions`

export const getRoleSets = (token, workspaceId) =>
  handle(apiRequest(setsBase(workspaceId), { token }), 'Failed to load role sets')

export const createRoleSet = (token, workspaceId, body) =>
  handle(apiRequest(setsBase(workspaceId), { token, method: 'POST', body }), 'Failed to create role set')

export const updateRoleSet = (token, workspaceId, roleSetId, body) =>
  handle(apiRequest(`${setsBase(workspaceId)}/${roleSetId}`, { token, method: 'PUT', body }), 'Failed to update role set')

export const deleteRoleSet = (token, workspaceId, roleSetId) =>
  handle(apiRequest(`${setsBase(workspaceId)}/${roleSetId}`, { token, method: 'DELETE' }), 'Failed to delete role set')

export const getRoleDefinitions = (token, workspaceId) =>
  handle(apiRequest(rolesBase(workspaceId), { token }), 'Failed to load roles')

export const createRoleDefinition = (token, workspaceId, body) =>
  handle(apiRequest(rolesBase(workspaceId), { token, method: 'POST', body }), 'Failed to create role')

export const updateRoleDefinition = (token, workspaceId, roleId, body) =>
  handle(apiRequest(`${rolesBase(workspaceId)}/${roleId}`, { token, method: 'PUT', body }), 'Failed to update role')

export const deleteRoleDefinition = (token, workspaceId, roleId) =>
  handle(apiRequest(`${rolesBase(workspaceId)}/${roleId}`, { token, method: 'DELETE' }), 'Failed to delete role')
