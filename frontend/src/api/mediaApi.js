import { apiRequest } from './client'
import { optimizeMediaUpload } from '../utils/optimizeMediaUpload'

async function json(response) {
  if (!response.ok) {
    const payload = await response.json().catch(() => ({}))
    const error = new Error(payload.message || `Media API: HTTP ${response.status}`)
    error.status = response.status
    throw error
  }
  return response.status === 204 ? null : response.json()
}

export const getMediaCatalog = async (token, workspaceId) =>
  json(await apiRequest(`/workspaces/${workspaceId}/media/catalog`, { token }))
export const getMediaManifest = async (token, workspaceId) =>
  json(await apiRequest(`/workspaces/${workspaceId}/media/manifest`, { token }))
export const getMediaBinding = async (token, workspaceId, type, targetId, slot) =>
  json(await apiRequest(`/workspaces/${workspaceId}/media/bindings/${type}/${targetId}/${slot}`, { token }))
export const putMediaBinding = async (token, workspaceId, type, targetId, slot, selection, expectedVersion) =>
  json(await apiRequest(`/workspaces/${workspaceId}/media/bindings/${type}/${targetId}/${slot}`,
    { token, method: 'PUT', body: { ...selection, expectedVersion } }))
export const deleteMediaBinding = async (token, workspaceId, type, targetId, slot, expectedVersion) =>
  json(await apiRequest(`/workspaces/${workspaceId}/media/bindings/${type}/${targetId}/${slot}?expectedVersion=${expectedVersion}`,
    { token, method: 'DELETE' }))

// Never put a Bearer-protected URL into img.src directly; caller revokes the Blob URL.
export async function fetchAuthorizedMediaObjectUrl(token, path, workspaceId) {
  // Never attach a session token to a URL outside the active family.
  const prefix = `/workspaces/${workspaceId}/media/`
  if (!Number.isSafeInteger(Number(workspaceId)) || !path?.startsWith(prefix) ||
      /[?#]/.test(path) || path.includes('..') || path.includes('//') ||
      !/^\/workspaces\/\d+\/media\/[A-Za-z0-9/_-]+$/.test(path)) {
    throw new Error('Invalid workspace media path')
  }
  const response = await apiRequest(path, { token, cache: 'no-store' })
  if (!response.ok) throw new Error(`Media image unavailable (HTTP ${response.status})`)
  const blob = await response.blob()
  if (!blob.type.startsWith('image/')) throw new Error('Unexpected media content type')
  return URL.createObjectURL(blob)
}

export async function uploadMedia(token, workspaceId, file) {
  const form = new FormData()
  const prepared = await optimizeMediaUpload(file)
  form.append('file', prepared)
  // multipart must set its own boundary; do not use the JSON apiRequest helper.
  const { API_URL } = await import('./baseUrl')
  let response
  try {
    response = await fetch(`${API_URL}/workspaces/${workspaceId}/media/uploads`, {
      method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: form,
    })
  } catch {
    throw new Error('Не вдалося передати зображення. Перевірте з’єднання та спробуйте ще раз.')
  }
  if (response.status === 413) {
    const error = new Error('Зображення завелике для сервера. Спробуйте файл меншого розміру.')
    error.status = 413
    throw error
  }
  return json(response)
}
