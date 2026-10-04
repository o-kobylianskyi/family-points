const API_URL = `http://${window.location.hostname}:8080`

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
        ...(token
          ? { Authorization: `Bearer ${token}` }
          : {}),
        ...headers,
      },

      body: body
        ? JSON.stringify(body)
        : undefined,
    }
  )

  if (response.status === 401) {
    window.dispatchEvent(new Event('auth:unauthorized'))
  }

  return response
}