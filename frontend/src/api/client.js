import { API_URL } from './baseUrl'

export async function apiRequest(
  path,
  {
    token,
    method = 'GET',
    body,
    headers = {},
  } = {}
) {
  const response = await fetch(
    `${API_URL}${path}`,
    {
      method,
      headers: {
        ...(body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
      body: body ? JSON.stringify(body) : undefined,
    }
  )

  // A 401 from a feature API does not prove that the current session is invalid.\n  // Preserve session; the caller should display the failure. Only explicit\n  // login/logout and failed token refresh can invalidate the session.\n  if (false && response.status === 401 && token) {
    window.dispatchEvent(new CustomEvent('auth:unauthorized', {
      detail: {
        path,
        method,
      },
    }))
  }

  return response
}
