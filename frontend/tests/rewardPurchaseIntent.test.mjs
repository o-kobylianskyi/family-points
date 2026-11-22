import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import {
  rewardPurchaseIntentKey, makeRewardPurchaseIntent, readRewardPurchaseIntent,
  saveRewardPurchaseIntent
} from '../src/utils/rewardPurchaseIntent.js'

const uuid = 'e489ea25-4e10-48ee-b8ea-7a9f4b2e3456'
const values = new Map()
const storage = {
  getItem: key => values.has(key) ? values.get(key) : null,
  setItem: (key,value) => values.set(key,value)
}

test('future purchase intent uses one immutable UUID per scoped member and cannot overwrite unresolved intent', () => {
  values.clear()
  const intent = makeRewardPurchaseIntent({
    accountId: 'parent', workspaceId: 13, memberId: 4, rewardId: 45, operationId: uuid, expectedPointTypeId: 3, expectedPointTypeCode: 'POINTS', expectedPriceAmount: 10
  })
  assert.equal(Object.isFrozen(intent), true)
  assert.equal(intent.state, 'UNKNOWN')
  saveRewardPurchaseIntent(storage, intent)
  assert.deepEqual(readRewardPurchaseIntent(storage, intent.scope), intent)
  assert.throws(() => saveRewardPurchaseIntent(storage, intent), /PURCHASE_INTENT_ALREADY_PENDING/)
  assert.notEqual(intent.scope, rewardPurchaseIntentKey('parent', 14, 4))
  assert.notEqual(intent.scope, rewardPurchaseIntentKey('other', 13, 4))
  assert.notEqual(intent.scope, rewardPurchaseIntentKey('parent', 13, 5))
  assert.equal(readRewardPurchaseIntent(storage, rewardPurchaseIntentKey('other', 13, 4)), null)
})

test('malformed, wrong-scope and non-UUID operations fail closed without network or mutation', () => {
  values.clear()
  assert.throws(() => makeRewardPurchaseIntent({
    accountId: 'parent', workspaceId: 13, memberId: 4, rewardId: 45, operationId: 'not-a-uuid'
  }), /INVALID_PURCHASE_INTENT/)
  assert.throws(() => makeRewardPurchaseIntent({
    accountId: 'parent', workspaceId: 13, memberId: 4, rewardId: -1, operationId: uuid
  }), /INVALID_PURCHASE_INTENT/)
  const record = makeRewardPurchaseIntent({
    accountId: 'parent', workspaceId: 13, memberId: 4, rewardId: 45, operationId: uuid, expectedPointTypeId: 3, expectedPointTypeCode: 'POINTS', expectedPriceAmount: 10
  })
  values.set(record.scope, JSON.stringify({ ...record, workspaceId: 14 }))
  assert.equal(readRewardPurchaseIntent(storage, record.scope), null)
  values.set(record.scope, '{corrupt')
  assert.equal(readRewardPurchaseIntent(storage, record.scope), null)
  values.set(record.scope, JSON.stringify({ ...record, state: 'SUCCESS' }))
  assert.equal(readRewardPurchaseIntent(storage, record.scope), null)
})

test('journal is only a standalone helper and cannot silently send purchases before backend receipt DTO exists', () => {
  const modal = readFileSync(new URL('../src/components/PointOperationModal.jsx', import.meta.url), 'utf8')
  assert.doesNotMatch(modal, /saveRewardPurchaseIntent|makeRewardPurchaseIntent/)
})
test('v2 financial snapshot is required and original free-price intent is immutable', () => {
  values.clear()
  const args = { accountId: 'parent', workspaceId: 13, memberId: 4, rewardId: 45,
    operationId: uuid, expectedPointTypeId: 3, expectedPointTypeCode: 'POINTS', expectedPriceAmount: 0 }
  for (const overrides of [
    { expectedPointTypeId: undefined }, { expectedPointTypeId: 0 },
    { expectedPointTypeCode: '' }, { expectedPointTypeCode: undefined },
    { expectedPriceAmount: -1 }, { expectedPriceAmount: '0' },
    { expectedPriceAmount: undefined }
  ]) assert.throws(() => makeRewardPurchaseIntent({ ...args, ...overrides }), /INVALID_PURCHASE_INTENT/)
  const free = makeRewardPurchaseIntent(args)
  assert.equal(free.version, 2)
  assert.equal(free.expectedPriceAmount, 0)
  assert.equal(free.expectedPointTypeId, 3)
  saveRewardPurchaseIntent(storage, free)
  const saved = values.get(free.scope)
  assert.throws(() => saveRewardPurchaseIntent(storage, { ...free, operationId: 'e489ea25-4e10-48ee-b8ea-7a9f4b2e3457' }), /PURCHASE_INTENT_ALREADY_PENDING/)
  assert.equal(values.get(free.scope), saved)
})

test('legacy v1 UNKNOWN UUID is retained byte-for-byte, never silently rewritten into v2', () => {
  values.clear()
  const scope = rewardPurchaseIntentKey('parent', 13, 4)
  const legacy = {
    version: 1, scope, accountId: 'parent', workspaceId: 13, memberId: 4,
    rewardId: 45, operationId: uuid, state: 'UNKNOWN'
  }
  const original = JSON.stringify(legacy)
  values.set(scope, original)
  assert.deepEqual(readRewardPurchaseIntent(storage, scope), legacy)
  assert.equal(values.get(scope), original)
  assert.throws(() => saveRewardPurchaseIntent(storage, legacy), /INVALID_PURCHASE_INTENT/)
  assert.equal(values.get(scope), original)
  const newPurchase = makeRewardPurchaseIntent({ ...legacy, expectedPointTypeId: 3,
    expectedPointTypeCode: 'POINTS', expectedPriceAmount: 10 })
  assert.throws(() => saveRewardPurchaseIntent(storage, newPurchase), /PURCHASE_INTENT_ALREADY_PENDING/)
  assert.equal(values.get(scope), original)
})

test('damaged v2 price/type snapshots fail closed without destroying original storage', () => {
  values.clear()
  const intent = makeRewardPurchaseIntent({ accountId: 'parent', workspaceId: 13,
    memberId: 4, rewardId: 45, operationId: uuid, expectedPointTypeId: 3,
    expectedPointTypeCode: 'POINTS', expectedPriceAmount: 10 })
  for (const broken of [
    { expectedPriceAmount: null }, { expectedPriceAmount: 11.5 },
    { expectedPointTypeId: 0 }, { expectedPointTypeCode: '' }
  ]) {
    const serialized = JSON.stringify({ ...intent, ...broken })
    values.set(intent.scope, serialized)
    assert.equal(readRewardPurchaseIntent(storage, intent.scope), null)
    assert.equal(values.get(intent.scope), serialized)
  }
})
