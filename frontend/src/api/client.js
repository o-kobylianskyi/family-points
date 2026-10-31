import { API_URL } from './baseUrl'

// Epoch is deliberately NOT persisted: on every workspace write, obtain an
// authenticated current value. No automatic retries of non-idempotent writes.
const lastEpoch = new Map()
const quarantined = new Set()
const quarantinedWorkspaces = new Set()
const scopedMutation = /^(POST|PUT|PATCH|DELETE)$/
const workspacePath = /^\/workspaces\/([0-9]+)(?:\/|$)/
const EPOCH_NOTICE_KEY = 'fp:workspace-epoch-notice'

if (typeof window !== 'undefined') {
  window.addEventListener('storage', event => {
    if (event.key !== EPOCH_NOTICE_KEY || !event.newValue) return
    try {
      const notice = JSON.parse(event.newValue)
      if (!/^\d+$/.test(String(notice.workspaceId))) return
      quarantinedWorkspaces.add(String(notice.workspaceId))
      for (const key of lastEpoch.keys()) {
        if (key.endsWith(`:${notice.workspaceId}`)) quarantined.add(key)
      }
      window.dispatchEvent(new CustomEvent('workspace:data-epoch-changed', { detail: notice }))
    } catch { /* Ignore malformed cross-tab notifications. */ }
  })
}

function notifyEpochChange(workspaceId, previous, current) {
  if (typeof window !== 'undefined') {
    const detail = { workspaceId, previousEpoch: previous, dataEpoch: current }
    window.dispatchEvent(new CustomEvent('workspace:data-epoch-changed', { detail }))
    try { localStorage.setItem(EPOCH_NOTICE_KEY, JSON.stringify(detail)) } catch { /* Storage may be disabled. */ }
  }
}

async function readWorkspaceEpoch(token, workspaceId) {
  const response = await fetch(`${API_URL}/workspaces/${workspaceId}/data-epoch`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: 'no-store'
  })
  if (!response.ok) {
    const error = new Error('WORKSPACE_DATA_EPOCH_UNAVAILABLE')
    error.status = response.status
    throw error
  }
  const body = await response.json()
  const epoch = body?.dataEpoch
  if (String(body?.workspaceId) !== workspaceId ||
      !Number.isSafeInteger(epoch) || epoch < 0) {
    throw new Error('WORKSPACE_DATA_EPOCH_INVALID')
  }
  // Partition by token, never share an epoch across families or identities.
  const key = `${token}:${workspaceId}`
  if (quarantinedWorkspaces.has(workspaceId)) {
    const error = new Error('WORKSPACE_DATA_EPOCH_CHANGED')
    error.status = 409
    throw error
  }
  if (quarantined.has(key)) {
    const error = new Error('WORKSPACE_DATA_EPOCH_CHANGED')
    error.status = 409
    throw error
  }
  const previous = lastEpoch.get(key)
  if (previous !== undefined && previous !== epoch) {
    lastEpoch.delete(key)
    quarantined.add(key)
    quarantinedWorkspaces.add(workspaceId)
    notifyEpochChange(workspaceId, previous, epoch)
    const error = new Error('WORKSPACE_DATA_EPOCH_CHANGED')
    error.status = 409
    throw error
  }
  lastEpoch.set(key, epoch)
  return epoch
}

export async function apiRequest(
  path,
  {
    token,
    method = 'GET',
    body,
    headers = {},
    cache,
  } = {}
) {
  const requestMethod = method.toUpperCase()
  const workspaceId = workspacePath.exec(path)?.[1]
  let scopedHeaders = headers
  if (token && workspaceId && scopedMutation.test(requestMethod)) {
    const epoch = await readWorkspaceEpoch(token, workspaceId)
    // The epoch header must not be overridden by consumers.
    scopedHeaders = { ...headers, 'X-Workspace-Data-Epoch': String(epoch) }
  }
  const response = await fetch(
    `${API_URL}${path}`,
    {
      method,
      ...(cache ? { cache } : {}),
      headers: {
        ...(body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...scopedHeaders,
      },
      body: body ? JSON.stringify(body) : undefined,
    }
  )
  if (workspaceId && response.status === 409 && scopedMutation.test(requestMethod)) {
    // A 409 may be an ordinary business conflict. Notify consumers only if
    // a fresh handshake proves the workspace epoch has actually changed.
    if (token) {
      try {
        await readWorkspaceEpoch(token, workspaceId)
      } catch (error) {
        if (error.message !== 'WORKSPACE_DATA_EPOCH_CHANGED') {
          // Preserve the original response and its existing 409 handling.
        }
      }
    }
  }
  return response
}
