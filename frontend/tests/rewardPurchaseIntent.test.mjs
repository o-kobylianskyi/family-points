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