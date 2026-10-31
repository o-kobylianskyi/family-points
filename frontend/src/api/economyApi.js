import { apiRequest } from './client'

async function handleResponse(
  response,
  fallbackMessage
) {
  if (!response.ok) {
    let message = fallbackMessage

    try {
      const error = await response.json()

      if (error.message) {
        message = error.message
      }
    } catch {
      // Backend не повернув JSON
    }

    throw new Error(message)
  }

  return response.json()
}

export async function getPointTypes(
  token,
  workspaceId
) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/economy/point-types`,
    { token }
  )

  return handleResponse(
    response,
    'Failed to load point types'
  )
}
export async function getPointNameForms(token, workspaceId, pointTypeId) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/economy/point-types/${pointTypeId}/name-forms`, { token }
  )
  return handleResponse(response, 'Failed to load currency forms')
}

export async function savePointNameForms(token, workspaceId, pointTypeId, language, forms) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/economy/point-types/${pointTypeId}/name-forms/${language}`,
    { token, method: 'PUT', body: forms }
  )
  return handleResponse(response, 'Failed to save currency forms')
}

export async function createPointType(token, workspaceId, { code, name, sortOrder = 100 }) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/economy/point-types`,
    { token, method: 'POST', body: { code, name, sortOrder } }
  )
  return handleResponse(response, 'Failed to create currency')
}

export async function getAllPointTypes(token, workspaceId) {
  const response = await apiRequest(`/workspaces/${workspaceId}/economy/point-types/all`, { token })
  return handleResponse(response, 'Failed to load all currencies')
}

export async function updatePointType(token, workspaceId, id, { name, sortOrder, active }) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/economy/point-types/${id}`,
    { token, method: 'PUT', body: { name, sortOrder, active } }
  )
  return handleResponse(response, 'Failed to update currency')
}
