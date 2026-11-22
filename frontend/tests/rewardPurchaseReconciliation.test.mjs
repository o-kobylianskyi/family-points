import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import {
  makeRewardPurchaseIntent, saveRewardPurchaseIntent,
  rewardPurchaseIntentKey
} from '../src/utils/rewardPurchaseIntent.js'
import { inspectPendingRewardPurchase } from '../src/utils/rewardPurchaseReconciliation.js'

const uuid = 'e489ea25-4e10-48ee-b8ea-7a9f4b2e3456'
const keys = new Map()
const storage = {
  getItem: key => keys.has(key) ? keys.get(key) : null,
  setItem: (key,value) => keys.set(key,value),
  removeItem: () => { throw new Error('UNSAFE_REMOVE') }
}
const intent = makeRewardPurchaseIntent({
  accountId: 'parent', workspaceId: 13, memberId: 7,
  rewardId: 45, operationId: uuid
})
const validBody = {
  operationId: uuid, replayed: true,
  purchase: { id: 95, rewardDefinitionId: 45, memberId: 7,
    pointTypeId: 3, pointTypeCode: 'POINTS', priceAmount: 10, status: 'READY' }
}
function setup() {
  keys.clear()
  saveRewardPurchaseIntent(storage, intent)
  return keys.get(intent.scope)
}
const input = fetcher => ({
  storage, accountId: 'parent', workspaceId: 13, memberId: 7,
  receiptFetcher: fetcher
})

test('receipt inspection is read-only, uses original UUID and confirms only authenticated matching body', async () => {
  const before = setup()
  let calls = 0
  const result = await inspectPendingRewardPurchase(input(async (workspaceId, operationId) => {
    calls++
    assert.equal(workspaceId, 13)
    assert.equal(operationId, uuid)
    return { status: 200, body: validBody }
  }))
  assert.equal(calls, 1)
  assert.equal(result.status, 'CONFIRMED_PURCHASE')
  assert.equal(result.receipt.purchase.priceAmount, 10)
  assert.equal(result.receipt.purchase.status, 'READY')
  assert.equal(keys.get(intent.scope), before)
})

test('absent receipt, unauthorized and network failure preserve original unknown operation', async () => {
  for (const [status, expected] of [
    [404, 'NOT_FOUND_NO_REPLAY'],
    [401, 'AUTH_REQUIRED_NO_REPLAY'],
    [403, 'AUTH_REQUIRED_NO_REPLAY'],
    [409, 'CONFLICT_NO_REPLAY'],
    [500, 'UNKNOWN_NO_REPLAY']
  ]) {
    const before = setup()
    const result = await inspectPendingRewardPurchase(input(async () => ({ status, body: null })))
    assert.equal(result.status, expected)
    assert.equal(result.receipt, null)
    assert.equal(keys.get(intent.scope), before)
  }
  const before = setup()
  assert.equal((await inspectPendingRewardPurchase(input(async () => {throw Error('offline')}))).status, 'UNKNOWN_NO_REPLAY')
  assert.equal(keys.get(intent.scope), before)
})

test('mismatched receipt, corrupt intent and wrong account all fail closed', async () => {
  const before = setup()
  const bad = await inspectPendingRewardPurchase(input(async () => ({
    status: 200, body: { ...validBody, purchase: { ...validBody.purchase, memberId: 8 } }
  })))
  assert.equal(bad.status, 'UNKNOWN_NO_REPLAY')
  assert.equal(keys.get(intent.scope), before)
  const foreign = await inspectPendingRewardPurchase({
    ...input(async () => {throw Error('SHOULD_NOT_CALL')}),
    accountId: 'other'
  })
  assert.equal(foreign.status, 'NO_VALID_INTENT')
  keys.set(rewardPurchaseIntentKey('parent', 13, 7), '{invalid')
  assert.equal((await inspectPendingRewardPurchase(input(async () => {throw Error('SHOULD_NOT_CALL')}))).status, 'NO_VALID_INTENT')
})


test('reload and changed account/member never transfer the old original UUID to another actor', async () => {
  const before = setup()
  const resumed = await inspectPendingRewardPurchase(input(async (workspaceId, operationId) => {
    assert.equal(workspaceId, 13)
    assert.equal(operationId, uuid)
    return { status: 200, body: validBody }
  }))
  assert.equal(resumed.status, 'CONFIRMED_PURCHASE')
  assert.equal(keys.get(intent.scope), before)
  for (const change of [{ accountId: 'another-account' }, { workspaceId: 14 }, { memberId: 8 }]) {
    let invoked = 0
    const other = await inspectPendingRewardPurchase({
      ...input(async () => { invoked++; return { status: 200, body: validBody } }),
      ...change
    })
    assert.equal(invoked, 0)
    assert.equal(other.status, 'NO_VALID_INTENT')
    assert.equal(keys.get(intent.scope), before)
  }
})

test('pending, cancelled, unknown and mismatched completed receipts do not confirm charge state', async () => {
  for (const state of ['PENDING_APPROVAL', 'FAILED', 'UNKNOWN', null]) {
    const before = setup()
    const result = await inspectPendingRewardPurchase(input(async () => ({
      status: 200,
      body: { ...validBody, purchase: { ...validBody.purchase, status: state } }
    })))
    assert.equal(result.status, 'UNKNOWN_NO_REPLAY')
    assert.equal(keys.get(intent.scope), before)
  }
  const before = setup()
  const operationMismatch = await inspectPendingRewardPurchase(input(async () => ({
    status: 200, body: { ...validBody, operationId: 'other' }
  })))
  assert.equal(operationMismatch.status, 'UNKNOWN_NO_REPLAY')
  assert.equal(keys.get(intent.scope), before)
})


test('authenticated matching CANCELLED receipt reports historical purchase and HOLD without releasing UUID', async () => {
  const before = setup()
  const confirmed = await inspectPendingRewardPurchase(input(async (workspaceId, operationId) => {
    assert.equal(workspaceId, 13)
    assert.equal(operationId, uuid)
    return { status: 200, body: {
      ...validBody, purchase: { ...validBody.purchase, status: 'CANCELLED' }
    } }
  }))
  assert.equal(confirmed.status, 'HISTORICAL_PURCHASE_CANCELLED_HOLD')
  assert.equal(confirmed.receipt.purchase.status, 'CANCELLED')
  assert.equal(confirmed.receipt.purchase.id, validBody.purchase.id)
  assert.equal(keys.get(intent.scope), before)

  for (const tamper of [
    { operationId: 'not-the-original' },
    { purchase: { ...validBody.purchase, status: 'CANCELLED', memberId: 99 } },
    { purchase: { ...validBody.purchase, status: 'CANCELLED', rewardDefinitionId: 99 } },
    { purchase: { ...validBody.purchase, status: 'CANCELLED', priceAmount: -1 } },
    { purchase: { ...validBody.purchase, status: 'CANCELLED', pointTypeCode: '' } }
  ]) {
    const result = await inspectPendingRewardPurchase(input(async () => ({
      status: 200,
      body: { ...validBody, purchase: { ...validBody.purchase, status: 'CANCELLED' }, ...tamper }
    })))
    assert.equal(result.status, 'UNKNOWN_NO_REPLAY')
    assert.equal(result.receipt, null)
    assert.equal(keys.get(intent.scope), before)
  }
})

test('reconciliation module never sends POST or clears pending, and is not yet wired to live spend', () => {
  const module = readFileSync(new URL('../src/utils/rewardPurchaseReconciliation.js', import.meta.url), 'utf8')
  const page = readFileSync(new URL('../src/pages/PointsPage.jsx', import.meta.url), 'utf8')
  assert.doesNotMatch(module, /apiRequest\(|fetch\(|removeItem\(|setItem\(/)
  assert.doesNotMatch(page, /inspectPendingRewardPurchase/)
})
