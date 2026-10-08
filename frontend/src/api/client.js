const API_URL =
  window.location.hostname === 'dev.family-point.com'
    ? 'https://api-dev.family-point.com:8443'
    : `http://${window.location.hostname}:8080`

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
