import { apiRequest } from './client'

// Read-only, authenticated global recommendations. Never persists prices.
export async function getEconomySuggestions(token) {
  const response = await apiRequest('/template-catalog/economy-suggestions', { token, cache: 'no-store' })
  if (!response.ok) {
    const error = new Error('ECONOMY_SUGGESTIONS_UNAVAILABLE')
    error.status = response.status
    throw error
  }
  const data = await response.json()
  if (data?.schema !== 'familypoints.economy.suggestions.v1' || data?.version !== 1 ||
      data?.currencyCode !== 'POINTS' || data?.autoApply !== false ||
      !Array.isArray(data.tasks) || !Array.isArray(data.rewards)) {
    throw new Error('ECONOMY_SUGGESTIONS_INVALID')
  }
  return data
}
