import test from 'node:test'
import assert from 'node:assert/strict'
import {
  operationStorageKey, validImportOperation, readImportOperation,
  storeImportOperation, makeImportOperation, assertImportReceipt
} from '../src/utils/onboardingImportOperation.js'

const uuid = 'e489ea25-4e10-48ee-b8ea-7a9f4b2e3456'
const make = () => makeImportOperation(14, 3, ['TASK_A', 'REWARD_B'], uuid)
const receipt = () => ({
  operationId: uuid,
  replayed: true,
  importResult: {
    selectedCodes: ['TASK_A', 'REWARD_B'],
    alreadyImported: ['REWARD_B'],
    toImport: ['TASK_A']
  }
})

test('V47 operation UUID, ordered selection and epoch validate strictly', () => {
  const operation = make()
  assert.equal(validImportOperation(operation, 14, 3), true)
  assert.equal(validImportOperation(operation, 15, 3), false)
  assert.equal(validImportOperation(operation, 14, 4), false)
  assert.equal(validImportOperation({...operation, codes: ['TASK_A', 'TASK_A']}, 14), false)
  assert.equal(validImportOperation({...operation, operationId: 'invalid'}, 14), false)
  assert.equal(validImportOperation({...operation, codes: []}, 14), false)
  assert.equal(validImportOperation({...operation, codes: Array(143).fill('X')}, 14), false)
})
test('account and family keys partition pending operations', () => {
  assert.notEqual(operationStorageKey('one', 14), operationStorageKey('two', 14))
  assert.notEqual(operationStorageKey('one', 14), operationStorageKey('one', 15))
})
test('session storage survives read and rejects foreign/corrupt record', () => {
  const map = new Map()
  const storage = {getItem: key => map.get(key) ?? null, setItem: (key, value) => map.set(key, value)}
  const key = operationStorageKey('parent', 14)
  storeImportOperation(storage, key, make())
  assert.deepEqual(readImportOperation(storage, key, 14), make())
  assert.equal(readImportOperation(storage, key, 15), null)
  storage.setItem(key, '{"broken"')
  assert.equal(readImportOperation(storage, key, 14), null)
})
test('receipt validates operation identity, exact order and full partition', () => {
  assert.deepEqual(assertImportReceipt(receipt(), make()), receipt())
  assert.throws(() => assertImportReceipt({...receipt(), operationId: 'other'}, make()))
  assert.throws(() => assertImportReceipt({...receipt(), importResult: {...receipt().importResult, selectedCodes: ['REWARD_B','TASK_A']}}, make()))
  assert.throws(() => assertImportReceipt({...receipt(), importResult: {...receipt().importResult, toImport: ['TASK_A','TASK_A']}}, make()))
  assert.throws(() => assertImportReceipt({...receipt(), importResult: {...receipt().importResult, alreadyImported: []}}, make()))
  assert.throws(() => assertImportReceipt({...receipt(), importResult: {...receipt().importResult, toImport: ['FOREIGN']}}, make()))
})
