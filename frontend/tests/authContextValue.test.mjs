import test from 'node:test'
import assert from 'node:assert/strict'
import { AuthContext, useAuth } from '../src/context/authContextValue.js'

test('provider and consumer share one React context singleton', async () => {
  const secondImport = await import('../src/context/authContextValue.js')
  assert.strictEqual(secondImport.AuthContext, AuthContext)
  assert.equal(typeof AuthContext.Provider, 'object')
  assert.equal(typeof useAuth, 'function')
})
