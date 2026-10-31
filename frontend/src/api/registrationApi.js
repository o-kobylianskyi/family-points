import { API_URL } from './baseUrl'

export async function registerFirstFamily(details) {
  const response = await fetch(`${API_URL}/auth/register-family`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(details),
  })
  if (response.status === 503) throw new Error('Реєстрацію тимчасово вимкнено.')
  if (response.status === 409) throw new Error('Цей логін уже використовується.')
  if (!response.ok) throw new Error('Не вдалося створити сім’ю. Перевірте дані.')
  return response.json()
}
