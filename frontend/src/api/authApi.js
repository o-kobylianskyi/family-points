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

export async function registerGlobalAccount(username, password) {
  const response = await fetch(`${API_URL}/auth/register-account`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  if (!response.ok) throw new Error(response.status === 503
    ? 'Реєстрація ще не увімкнена на сервері'
    : 'Не вдалося створити акаунт')
  return response.json()
}

export async function createFirstFamily(token, familyName, ownerName) {
  const response = await fetch(`${API_URL}/api/onboarding/first-family`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ familyName, ownerName }),
  })
  if (!response.ok) throw new Error(response.status === 503
    ? 'Створення першої сім’ї ще не увімкнене на сервері'
    : 'Не вдалося створити сім’ю')
  return response.json()
}

export async function createAdditionalFamily(token, familyName, ownerName) {
  const response = await fetch(`${API_URL}/api/account/families`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ familyName: familyName.trim(), ownerName: ownerName.trim() }),
  })
  if (!response.ok) {
    if (response.status === 503) throw new Error('Створення додаткових сімей вимкнено на сервері')
    if (response.status === 403) throw new Error('Недостатньо прав для створення сім’ї')
    throw new Error('Не вдалося створити сім’ю')
  }
  return response.json()
}
