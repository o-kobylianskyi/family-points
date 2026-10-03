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