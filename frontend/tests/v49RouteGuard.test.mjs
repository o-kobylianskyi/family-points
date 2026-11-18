import test from 'node:test'
import assert from 'node:assert/strict'
import { isUnexpectedSyntheticWrite, registerV49UnexpectedWriteGuard } from '../../qa/e2e/helpers/v49RouteGuard.mjs'

test('V49 browser guard classifies only mutating requests', () => {
  for (const method of ['POST', 'PUT', 'PATCH', 'DELETE']) {
    assert.equal(isUnexpectedSyntheticWrite(method), true)
    assert.equal(isUnexpectedSyntheticWrite(method.toLowerCase()), true)
  }
  for (const method of ['GET', 'HEAD', 'OPTIONS']) {
    assert.equal(isUnexpectedSyntheticWrite(method), false)
  }
})

test('V49 browser guard blocks writes outside workspace and records attempted paths', async () => {
  const attempts = []
  let matcher, callback
  await registerV49UnexpectedWriteGuard({
    async route(pattern, handler) { matcher = pattern; callback = handler },
  }, attempts)
  assert.equal(matcher, '**/*')
  assert.equal(typeof callback, 'function')

  async function dispatch(method, path) {
    let action = null
    await callback({
      request: () => ({
        method: () => method,
        url: () => 'https://qa.example.test' + path,
      }),
      abort: async reason => { action = 'abort:' + reason },
      continue: async () => { action = 'continue' },
    })
    return action
  }

  assert.equal(await dispatch('GET', '/api/workspaces/12/setup'), 'continue')
  assert.equal(await dispatch('HEAD', '/api/health'), 'continue')
  assert.equal(await dispatch('POST', '/api/auth/refresh'), 'abort:blockedbyclient')
  assert.equal(await dispatch('PUT', '/api/users/me'), 'abort:blockedbyclient')
  assert.equal(await dispatch('PATCH', '/api/workspaces/12/rewards'), 'abort:blockedbyclient')
  assert.equal(await dispatch('DELETE', '/api/other?secret=not-for-log'), 'abort:blockedbyclient')
  assert.deepEqual(attempts, [
    'POST /api/auth/refresh',
    'PUT /api/users/me',
    'PATCH /api/workspaces/12/rewards',
    'DELETE /api/other',
  ])
})

test('V49 browser guard rejects missing audit log instead of registering unsafe route', async () => {
  let registered = false
  await assert.rejects(
    registerV49UnexpectedWriteGuard({ route: async () => { registered = true } }, null),
    /V49_GUARD_REQUIRES_AUDIT_ARRAY/
  )
  assert.equal(registered, false)
})
