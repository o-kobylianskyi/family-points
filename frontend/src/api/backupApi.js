import { apiRequest } from './client'

// Backend V50 currently exposes metadata only. No export/download/restore contract.
export async function listProtectiveBackups(token, workspaceId, limit = 25) {
  if (!/^\d+$/.test(String(workspaceId)) || !Number.isSafeInteger(limit) || limit < 1 || limit > 100)
    throw new Error('INVALID_BACKUP_LIST_REQUEST')
  const response = await apiRequest(`/workspaces/${workspaceId}/protective-backups?limit=${limit}`, {
    token, cache: 'no-store',
  })
  if (!response.ok) {
    const error = new Error(`BACKUP_METADATA_HTTP_${response.status}`)
    error.status = response.status
    throw error
  }
  const data = await response.json()
  if (!Array.isArray(data)) throw new Error('INVALID_BACKUP_METADATA_RESPONSE')
  return data.filter(item => item && String(item.workspaceId) === String(workspaceId) &&
    typeof item.snapshotId === 'string' && typeof item.state === 'string')
}
