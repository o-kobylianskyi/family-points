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
