import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { makeRewardPurchaseIntent, saveRewardPurchaseIntent } from '../src/utils/rewardPurchaseIntent.js'
import { inspectPendingRewardPurchase } from '../src/utils/rewardPurchaseReconciliation.js'

// Isolate the production rewardApi source with an injected fake API requester.
// All tests stay offline and never call real localhost or workspace data.
const source = readFileSync(new URL('../src/api/rewardApi.js', import.meta.url), 'utf8')
  .replace("import { apiRequest } from './client'", "const apiRequest = (...args) => globalThis.__rewardReceiptTestApiRequest(...args)")
const { getRewardPurchaseOperationReceipt } = await import(
  'data:text/javascript;base64,' + Buffer.from(source).toString('base64')
)

const operationId = 'e489ea25-4e10-48ee-b8ea-7a9f4b2e3456'
const intent = makeRewardPurchaseIntent({
  accountId: 'parent', workspaceId: 13, memberId: 7, rewardId: 45, operationId
})
const records = new Map()
const storage = {
  getItem: key => records.has(key) ? records.get(key) : null,
  setItem: (key, value) => records.set(key, value)
}
const purchase = {
  operationId, replayed: true,
  purchase: {
    id: 777, rewardDefinitionId: 45, memberId: 7,
    pointTypeId: 2, pointTypeCode: 'POINTS',
    priceAmount: 10, status: 'READY'
  }
}
function prepare() {
  records.clear()
  saveRewardPurchaseIntent(storage, intent)
  return records.get(intent.scope)
}
function readWithToken(token = 'fake-jwt-never-networked') {
  return inspectPendingRewardPurchase({
    storage, accountId: 'parent', workspaceId: 13, memberId: 7,
    receiptFetcher: (workspaceId, uuid) =>
      getRewardPurchaseOperationReceipt(token, workspaceId, uuid)
  })
}

test('PR57 read-only API uses exact original UUID and no-store GET; matching receipt confirms without journal deletion', async () => {
  const before = prepare()
  const calls = []
  globalThis.__rewardReceiptTestApiRequest = async (path, options) => {
    calls.push({ path, options })
    return { status: 200, json: async () => purchase }
  }
  const state = await readWithToken()
  assert.equal(state.status, 'CONFIRMED_PURCHASE')
  assert.equal(state.receipt.purchase.priceAmount, 10)
  assert.equal(records.get(intent.scope), before)
  assert.equal(calls.length, 1)
  assert.equal(calls[0].path, `/workspaces/13/rewards/purchases/receipts/${operationId}`)
  assert.deepEqual(calls[0].options, {
    token: 'fake-jwt-never-networked', method: 'GET', cache: 'no-store'
  })
})

test('real GET adapter keeps UNKNOWN and original UUID on 401/403/404/409/500', async () => {
  for (const status of [401, 403, 404, 409, 500]) {
    const before = prepare()
    let readBody = false
    globalThis.__rewardReceiptTestApiRequest = async () => ({
      status,
      json: async () => { readBody = true; throw Error('SHOULD_NOT_PARSE_ERROR_BODY') }
    })
    const result = await readWithToken()
    assert.notEqual(result.status, 'CONFIRMED_PURCHASE')
    assert.equal(readBody, false)
    assert.equal(records.get(intent.scope), before)
  }
})

test('receipt GET parsing error and connection loss never trigger a second POST or clear stored intent', async () => {
  for (const behavior of ['broken-json', 'network']) {
    const before = prepare()
    let calls = 0
    globalThis.__rewardReceiptTestApiRequest = async (path, options) => {
      calls++
      assert.equal(options.method, 'GET')
      if (behavior === 'network') throw Error('CONNECTION_RESET')
      return { status: 200, json: async () => { throw Error('BAD_JSON') } }
    }
    const result = await readWithToken()
    assert.equal(result.status, 'UNKNOWN_NO_REPLAY')
    assert.equal(calls, 1)
    assert.equal(records.get(intent.scope), before)
  }
})

test('Web receipt API is not yet wired into active reward mutation and the old path is quarantined from receipt recovery', () => {
  const page = readFileSync(new URL('../src/pages/PointsPage.jsx', import.meta.url), 'utf8')
  const modal = readFileSync(new URL('../src/components/PointOperationModal.jsx', import.meta.url), 'utf8')
  assert.doesNotMatch(page + modal, /getRewardPurchaseOperationReceipt/)
  assert.doesNotMatch(page + modal, /inspectPendingRewardPurchase/)
})
