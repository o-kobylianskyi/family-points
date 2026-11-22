import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { makeRewardPurchaseIntent } from '../src/utils/rewardPurchaseIntent.js'
import {
  verifyRewardPurchaseReceipt, classifyRewardPurchaseReceiptFailure
} from '../src/utils/rewardPurchaseReceipt.js'

const operationId = 'e489ea25-4e10-48ee-b8ea-7a9f4b2e3456'
const intent = makeRewardPurchaseIntent({ accountId: 'parent', workspaceId: 13, memberId: 7, rewardId: 45, operationId, expectedPointTypeId: 4, expectedPointTypeCode: 'POINTS', expectedPriceAmount: 0 })
const receipt = () => ({
  operationId, replayed: false,
  purchase: { id: 123, rewardDefinitionId: 45, memberId: 7, pointTypeId: 4,
    pointTypeCode: 'POINTS', priceAmount: 0, status: 'READY' }
})

test('PR57 reward receipt matches original intent; zero price and replay are allowed, READY is purchase only', () => {
  assert.deepEqual(verifyRewardPurchaseReceipt(receipt(), intent), receipt())
  assert.equal(verifyRewardPurchaseReceipt({ ...receipt(), replayed: true }, intent).replayed, true)
  assert.equal(verifyRewardPurchaseReceipt(receipt(), intent).purchase.status, 'READY')
  assert.equal(verifyRewardPurchaseReceipt({ ...receipt(), purchase: { ...receipt().purchase, status: 'REDEEMED' } }, intent).purchase.status, 'REDEEMED')
})

test('foreign identity, unknown price/unit and malformed purchase receipts fail closed', () => {
  for (const patch of [
    { operationId: 'different' }, { replayed: null },
    { purchase: { ...receipt().purchase, id: 0 } },
    { purchase: { ...receipt().purchase, rewardDefinitionId: 46 } },
    { purchase: { ...receipt().purchase, memberId: 8 } },
    { purchase: { ...receipt().purchase, pointTypeId: 0 } },
    { purchase: { ...receipt().purchase, pointTypeId: 5 } },
    { purchase: { ...receipt().purchase, pointTypeCode: '' } },
    { purchase: { ...receipt().purchase, pointTypeCode: 'OTHER' } },
    { purchase: { ...receipt().purchase, priceAmount: -1 } },
    { purchase: { ...receipt().purchase, priceAmount: 100 } },
    { purchase: { ...receipt().purchase, priceAmount: '10' } },
    { purchase: { ...receipt().purchase, status: '' } },
    { purchase: { ...receipt().purchase, status: 'CANCELLED' } },
    { purchase: { ...receipt().purchase, status: 'PENDING_APPROVAL' } },
    { purchase: { ...receipt().purchase, status: 'FAILED' } },
    { purchase: { ...receipt().purchase, status: 'READY ' } },
    { purchase: null }
  ]) assert.throws(
    () => verifyRewardPurchaseReceipt({ ...receipt(), ...patch }, intent),
    /REWARD_PURCHASE_RECEIPT_INVALID/
  )
})

test('401/403/404/409 never authorize replay; pending UUID remains unchanged', () => {
  assert.equal(classifyRewardPurchaseReceiptFailure(401), 'AUTH_REQUIRED_NO_REPLAY')
  assert.equal(classifyRewardPurchaseReceiptFailure(403), 'AUTH_REQUIRED_NO_REPLAY')
  assert.equal(classifyRewardPurchaseReceiptFailure(404), 'NOT_FOUND_NO_REPLAY')
  assert.equal(classifyRewardPurchaseReceiptFailure(409), 'CONFLICT_NO_REPLAY')
  assert.equal(classifyRewardPurchaseReceiptFailure(500), 'UNKNOWN_NO_REPLAY')
  assert.equal(intent.operationId, operationId)
})

test('PR57 receipt parser is not wired into live purchase until finance-atomicity gate passes', () => {
  const page = readFileSync(new URL('../src/pages/PointsPage.jsx', import.meta.url), 'utf8')
  const modal = readFileSync(new URL('../src/components/PointOperationModal.jsx', import.meta.url), 'utf8')
  assert.doesNotMatch(page + modal, /verifyRewardPurchaseReceipt|saveRewardPurchaseIntent/)
})
