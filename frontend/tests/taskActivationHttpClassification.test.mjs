import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

// Offline source-level HTTP classification only: never sends real activation,
// reads browser session storage or overwrites owner's original pending UUID.
const original = readFileSync(new URL('../src/api/templateApi.js', import.meta.url), 'utf8')
const source = original.replace(
  "import { apiRequest } from './client'",
  "const apiRequest = (...args) => globalThis.__task401TestRequester(...args)"
)
const { activateTaskTemplateIdempotent, getTemplateActivationReceipt } = await import(
  'data:text/javascript;base64,' + Buffer.from(source).toString('base64')
)
const uuid = 'e489ea25-4e10-48ee-b8ea-7a9f4b2e3456'
const payload = Object.freeze({ title: 'Synthetic task', startDate: '2026-11-22', endDate: '2026-11-25' })
const fakeToken = 'synthetic-jwt'

test('401 authentication entrypoint is distinctly surfaced without another task POST', async () => {
  const calls = []
  globalThis.__task401TestRequester = async (path, options) => {
    calls.push({ path, options })
    return {
      ok: false, status: 401, json: async () =>
        ({ status: 401, error: 'Unauthorized', message: 'Authentication is required' })
    }
  }
  await assert.rejects(
    activateTaskTemplateIdempotent(fakeToken, 13, 43, payload, uuid, () => fakeToken),
    error => error.status === 401 && error.message === 'Authentication is required'
  )
  assert.equal(calls.length, 1)
  assert.equal(calls[0].options.headers['Idempotency-Key'], uuid)
  assert.equal(calls[0].options.getCurrentToken(), fakeToken)
  assert.match(calls[0].path, /\/43\/activate-task\/idempotent$/)
})

test('500 LocalDate fingerprint crash is NOT mislabeled 401 or proof of failed commit', async () => {
  globalThis.__task401TestRequester = async () => ({
    ok: false, status: 500, json: async () => ({
      message: 'Cannot fingerprint activation request'
    })
  })
  await assert.rejects(
    activateTaskTemplateIdempotent(fakeToken, 13, 43, payload, uuid, () => fakeToken),
    error => error.status === 500 && /fingerprint/.test(error.message)
  )
})

test('receipt lookup 404 and write 409 keep distinct statuses and same original UUID', async () => {
  const calls = []
  globalThis.__task401TestRequester = async (path, options) => {
    calls.push({ path, options })
    return { ok: false, status: path.includes('/receipts/') ? 404 : 409,
      json: async () => ({ message: 'No matching receipt or conflicting intent' }) }
  }
  await assert.rejects(
    getTemplateActivationReceipt(fakeToken, 13, uuid),
    error => error.status === 404
  )
  await assert.rejects(
    activateTaskTemplateIdempotent(fakeToken, 13, 43, payload, uuid, () => fakeToken),
    error => error.status === 409
  )
  assert.equal(calls.length, 2)
  for (const call of calls) assert.equal(call.options.headers['Idempotency-Key'], uuid)
  assert.equal(calls.filter(call => call.options.method === 'POST').length, 1)
})

test('malformed UUID aborts before any network call', async () => {
  let count = 0
  globalThis.__task401TestRequester = async () => { count++; throw Error('SHOULD_NOT_FETCH') }
  await assert.rejects(
    activateTaskTemplateIdempotent(fakeToken, 13, 43, payload, '../../foreign', () => fakeToken),
    /INVALID_TEMPLATE_OPERATION_ID/
  )
  await assert.rejects(getTemplateActivationReceipt(fakeToken, 13, 'bad'), /INVALID_TEMPLATE_OPERATION_ID/)
  assert.equal(count, 0)
})
