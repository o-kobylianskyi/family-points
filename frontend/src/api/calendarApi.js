import { apiRequest } from './client'

async function json(response) {
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || `Calendar API: HTTP ${response.status}`)
  }
  return response.json()
}

export const getCalendar = async (token, workspaceId, { from, to, memberId, type = 'ALL' }) => {
  const params = new URLSearchParams({ from, to, type })
  if (memberId) params.set('memberId', String(memberId))
  return json(await apiRequest(`/workspaces/${workspaceId}/calendar?${params}`, { token }))
}

export const getCalendarTimeZone = async (token, workspaceId) =>
  json(await apiRequest(`/workspaces/${workspaceId}/calendar/time-zone`, { token }))

export const setCalendarTimeZone = async (token, workspaceId, timeZone) =>
  json(await apiRequest(`/workspaces/${workspaceId}/calendar/time-zone`, { token, method: 'PUT', body: { timeZone } }))
