import { API_URL } from './baseUrl'

export async function login(username, password) {
  const response = await fetch(`${API_URL}/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      username,
      password,
    }),
  })

  if (!response.ok) {
    throw new Error('Неправильний логін або пароль')
  }

  return response.json()
}

export async function getCurrentUser(token) {
  const response = await fetch(`${API_URL}/api/me`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  if (!response.ok) {
    throw new Error('Не вдалося отримати дані користувача')
  }

  return response.json()
}

export async function refreshAccessToken(token) {
  const response = await fetch(`${API_URL}/auth/refresh`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  if (!response.ok) {
    throw new Error('Не вдалося продовжити сесію')
  }

  return response.json()
}

export async function listAccountWorkspaces(token) {
  const response = await fetch(`${API_URL}/api/me/workspaces`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok) throw new Error('Не вдалося отримати список сімей')
  return response.json()
}

export async function activateAccountWorkspace(token, workspaceId) {
  const response = await fetch(
    `${API_URL}/api/me/workspaces/${encodeURIComponent(workspaceId)}/activate`,
    { method: 'POST', headers: { Authorization: `Bearer ${token}` } }
  )
  if (!response.ok) throw new Error('Немає доступу до вибраної сім’ї')
  return response.json()
}
