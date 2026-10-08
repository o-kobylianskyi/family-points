const API_URL =
    ['localhost', '127.0.0.1'].includes(window.location.hostname)
        ? 'http://localhost:8080'
        : 'https://api-dev.family-point.com:8443'

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

  if (response.status === 401 && token) {
    window.dispatchEvent(new CustomEvent('auth:unauthorized', {
      detail: {
        path,
        method,
      },
    }))
  }

  return response
}
