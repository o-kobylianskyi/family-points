import { apiRequest } from './client'

async function jsonOrError(response, message) {
  if (!response.ok) {
    let detail = ''
    try { detail = (await response.json())?.message ?? '' } catch {}
    throw new Error(detail || message)
  }
  if (response.status === 204) return null
  return response.json()
}

export async function getWorkspaceMembers(token, workspaceId) {
  return jsonOrError(await apiRequest(`/workspaces/${workspaceId}/members`, { token }), 'Не вдалося завантажити учасників')
}

export async function createWorkspaceMember(token, workspaceId, body) {
  return jsonOrError(await apiRequest(`/workspaces/${workspaceId}/members`, { token, method: 'POST', body }), 'Не вдалося створити учасника')
}

export async function updateWorkspaceMember(token, workspaceId, memberId, body) {
  return jsonOrError(await apiRequest(`/workspaces/${workspaceId}/members/${memberId}`, { token, method: 'PUT', body }), 'Не вдалося оновити учасника')
}

export async function deleteWorkspaceMember(token, workspaceId, memberId) {
  return jsonOrError(await apiRequest(`/workspaces/${workspaceId}/members/${memberId}`, { token, method: 'DELETE' }), 'Не вдалося видалити учасника')
}

export async function getWorkspaceRoles(token, workspaceId) {
  return jsonOrError(await apiRequest(`/workspaces/${workspaceId}/roles`, { token }), 'Не вдалося завантажити ролі')
}
