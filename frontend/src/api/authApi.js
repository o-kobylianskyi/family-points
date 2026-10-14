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

export async function registerGlobalAccount(username, email, password) {
  const response = await fetch(`${API_URL}/auth/register-account`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, email, password }),
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

export async function verifyAccountEmail(token) {
  const response = await fetch(`${API_URL}/auth/email/verify`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token }),
  })
  if (!response.ok) throw new Error('Посилання недійсне або термін його дії минув')
}

export async function requestPasswordReset(email) {
  const response = await fetch(`${API_URL}/auth/email/request-reset`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  })
  if (!response.ok) throw new Error('Наразі не вдалося надіслати запит')
}

export async function resetAccountPassword(token, password) {
  const response = await fetch(`${API_URL}/auth/email/reset-password`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token, password }),
  })
  if (!response.ok) throw new Error('Посилання недійсне або термін його дії минув')
}

export async function googleAuth(idToken, createAccount = false) {
  const response = await fetch(`${API_URL}/auth/google${createAccount ? '/register' : ''}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken }),
  })
  if (!response.ok) {
    if (response.status === 503) throw new Error('Google-авторизація ще не налаштована на сервері')
    if (response.status === 409) throw new Error('Цей email уже використовується. Увійдіть за паролем і прив’яжіть Google у налаштуваннях.')
    if (response.status === 403) throw new Error('Google-акаунт ще не зареєстровано. Перейдіть на сторінку реєстрації.')
    throw new Error('Не вдалося підтвердити Google-акаунт')
  }
  return response.json()
}

export async function requestEmailVerification(email) {
  const response = await fetch(`${API_URL}/auth/email/request-verification`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  })
  if (!response.ok) throw new Error(response.status === 429
    ? 'Повторне надсилання доступне через дві хвилини'
    : 'Не вдалося відправити лист. Спробуйте пізніше')
}
