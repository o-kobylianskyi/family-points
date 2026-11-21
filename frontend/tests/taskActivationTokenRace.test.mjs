import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

// Exercise the actual apiRequest source in isolation without a browser,
// credentials, live API, or external network.
const source = readFileSync(new URL('../src/api/client.js', import.meta.url), 'utf8')
  .replace("import { API_URL } from './baseUrl'", "const API_URL = 'http://localhost:8080'")
const { apiRequest } = await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'))

const path = '/workspaces/13/templates/43/activate-task/idempotent'
const token = 'synthetic-old-token'
const receiptKey = 'e489ea25-4e10-48ee-b8ea-7a9f4b2e3456'
const headers = { 'Idempotency-Key': receiptKey }

test('scoped activation aborts before POST when JWT rotates during epoch handshake', async () => {
  const previousFetch = globalThis.fetch
  const calls = []
  let liveToken = token
  globalThis.fetch = async (url, init) => {
    calls.push({ url, method: init.method || 'GET', headers: init.headers })
    assert.match(url, /\/data-epoch$/)
    liveToken = 'synthetic-refreshed-token'
    return { ok: true, status: 200, json: async () => ({ workspaceId: 13, dataEpoch: 0 }) }
  }
  try {
    await assert.rejects(
      apiRequest(path, { token, method: 'POST', headers, body: { title: 'Synthetic task' }, getCurrentToken: () => liveToken }),
      error => error.message === 'AUTH_SESSION_CHANGED_DURING_WRITE_PREFLIGHT' && error.status === 401
    )
    assert.equal(calls.length, 1)
    assert.equal(calls[0].method, 'GET')
    assert.equal(calls[0].headers.Authorization, 'Bearer ' + token)
    assert.equal(calls.some(call => call.method === 'POST'), false)
  } finally { globalThis.fetch = previousFetch }
})

test('scoped activation sends exactly one POST with original UUID when session unchanged', async () => {
  const previousFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (url, init) => {
    calls.push({ url, method: init.method || 'GET', headers: init.headers })
    if (url.endsWith('/data-epoch')) {
      return { ok: true, status: 200, json: async () => ({ workspaceId: 13, dataEpoch: 0 }) }
    }
    return { ok: true, status: 200 }
  }
  try {
    const result = await apiRequest(path, { token, method: 'POST', headers, body: { title: 'Synthetic task' }, getCurrentToken: () => token })
    assert.equal(result.status, 200)
    assert.deepEqual(calls.map(call => call.method), ['GET', 'POST'])
    assert.equal(calls[1].headers.Authorization, 'Bearer ' + token)
    assert.equal(calls[1].headers['Idempotency-Key'], receiptKey)
    assert.equal(calls[1].headers['X-Workspace-Data-Epoch'], '0')
  } finally { globalThis.fetch = previousFetch }
})

test('task modal wires current-token preflight on both initial and explicit recovery activation', () => {
  const modal = readFileSync(new URL('../src/components/CreateTaskModal.jsx', import.meta.url), 'utf8')
  const activationCalls = [...modal.matchAll(/activateTaskTemplateIdempotent\(([^\n]+)\)/g)]
  assert.equal(activationCalls.length, 2)
  for (const match of activationCalls) assert.match(match[1], /operation\.operationId, getAccessToken$/)
  assert.match(modal, /saveActivation\(sessionStorage, activationKey, operation\)/)
  assert.match(modal, /if \(retry\) setReceiptConfirmedMissing\(false\)/)
})
