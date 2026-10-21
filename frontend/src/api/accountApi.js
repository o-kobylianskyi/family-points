import { apiRequest } from './client'

async function readError(response, fallback) {
  try { const body = await response.json(); return body.message || fallback } catch { return fallback }
}

export async function getWorkspaceAccounts(token, workspaceId) {
  const r = await apiRequest(`/workspaces/${workspaceId}/accounts`, { token })
  if (!r.ok) throw new Error(await readError(r, 'Failed to load accounts'))
  return r.json()
}
export async function createMemberAccount(token, workspaceId, memberId, data) {
  const r = await apiRequest(`/workspaces/${workspaceId}/members/${memberId}/account`, { token, method: 'POST', body: data })
  if (!r.ok) throw new Error(await readError(r, 'Failed to create account'))
  return r.json()
}
export async function updateMemberAccount(token, workspaceId, memberId, data) {
  const r = await apiRequest(`/workspaces/${workspaceId}/members/${memberId}/account`, { token, method: 'PUT', body: data })
  if (!r.ok) throw new Error(await readError(r, 'Failed to update account'))
  return r.json()
}
export async function resetMemberPassword(token, workspaceId, memberId, newPassword) {
  const r = await apiRequest(`/workspaces/${workspaceId}/members/${memberId}/account/password`, { token, method: 'PUT', body: { newPassword } })
  if (!r.ok) throw new Error(await readError(r, 'Failed to reset password'))
  return r.json()
}
export async function changeOwnPassword(token, currentPassword, newPassword) {
  const r = await apiRequest('/api/me/password', { token, method: 'PUT', body: { currentPassword, newPassword } })
  if (!r.ok) throw new Error(await readError(r, 'Failed to change password'))
}

export async function issueMemberEmailInvitation(token, workspaceId, memberId, email) {
  const r = await apiRequest(`/workspaces/${workspaceId}/members/${memberId}/email-invitation`,
    { token, method: 'POST', body: { email } })
  if (!r.ok) throw new Error(await readError(r,
    r.status === 503 ? 'Email-запрошення ще вимкнені на сервері' :
    r.status === 429 ? 'Повторіть спробу через дві хвилини' :
    'Не вдалося надіслати запрошення'))
}

export async function setPasswordWithGoogle(token, idToken, newPassword) {
  const r = await apiRequest('/api/account/google/set-password', {
    token, method: 'POST', body: { idToken, newPassword },
  })
  if (!r.ok) {
    const error = new Error('GOOGLE_PASSWORD_SETUP_FAILED')
    error.status = r.status
    throw error
  }
}
