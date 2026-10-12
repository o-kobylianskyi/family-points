import { apiRequest } from './client'

export async function getMyReputation(token) {
  const response = await apiRequest('/api/me/reputation', { token })
  if (!response.ok) throw new Error('Не вдалося завантажити репутацію')
  return response.json()
}
