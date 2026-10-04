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
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
      body: body ? JSON.stringify(body) : undefined,
    }
  )

  /*
   * A 401 from an ordinary API endpoint does not necessarily mean that the
   * locally stored JWT is invalid. Endpoint/security mistakes must be shown
   * to the user as API errors instead of destroying an otherwise valid
   * session. AuthContext validates/restores the session through /auth/me.
   */
  return response
}
