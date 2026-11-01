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
  if (data.some(item => !item || String(item.workspaceId) !== String(workspaceId) ||
    typeof item.snapshotId !== 'string' || typeof item.state !== 'string'))
    throw new Error('BACKUP_METADATA_TENANT_MISMATCH')
  return data
}

export async function getProtectiveBackup(token, workspaceId, snapshotId) {
  if (!/^\d+$/.test(String(workspaceId)) ||
      typeof snapshotId !== 'string' ||
      !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(snapshotId))
    throw new Error('INVALID_BACKUP_DETAIL_REQUEST')
  const response = await apiRequest(`/workspaces/${workspaceId}/protective-backups/${snapshotId}`, {
    token, cache: 'no-store',
  })
  if (!response.ok) {
    const error = new Error(`BACKUP_DETAIL_HTTP_${response.status}`)
    error.status = response.status
    throw error
  }
  const data = await response.json()
  if (!data || String(data.workspaceId) !== String(workspaceId) ||
      String(data.snapshotId).toLowerCase() !== snapshotId.toLowerCase() ||
      typeof data.state !== 'string')
    throw new Error('INVALID_BACKUP_DETAIL_RESPONSE')
  return data
}
