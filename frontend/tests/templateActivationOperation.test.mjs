import test from 'node:test'
import assert from 'node:assert/strict'
import {
  activationStorageKey, validActivation, makeActivation, readActivation,
  saveActivation, verifyActivationReceipt
} from '../src/utils/templateActivationOperation.js'

const uuid = 'e489ea25-4e10-48ee-b8ea-7a9f4b2e3456'
const task = () => makeActivation(7, 'TASK', 4, 21, { title: 'Wash dishes', assignedMemberId: 5 }, uuid)
const receipt = () => ({operationId: uuid, templateId: 21, kind: 'TASK', createdDefinitionId: 500, replayed: true})

test('V48 TASK record has immutable deep-copied request', () => {
  const request = { title: 'Task', executors: [{ actorId: 5 }] }
  const record = makeActivation(7, 'TASK', 4, 21, request, uuid)
  request.executors[0].actorId = 6
  assert.equal(record.request.executors[0].actorId, 5)
  assert.equal(validActivation(record, 7, 'TASK', 4), true)
  assert.equal(validActivation(record, 8, 'TASK', 4), false)
  assert.equal(validActivation(record, 7, 'REWARD', 4), false)
  assert.equal(validActivation(record, 7, 'TASK', 5), false)
  assert.equal(validActivation({...record, templateId: 0}, 7, 'TASK'), false)
})
test('V48 UUID cannot be substituted and receipt ID is not an object DTO', () => {
  assert.deepEqual(verifyActivationReceipt(receipt(), task()), receipt())
  for (const patch of [
    {operationId:'another'}, {templateId:22}, {kind:'REWARD'},
    {createdDefinitionId:undefined}, {createdDefinitionId:0}
  ]) assert.throws(() => verifyActivationReceipt({...receipt(), ...patch}, task()))
})
test('V48 pending operations remain scoped to account, workspace and kind', () => {
  const k1 = activationStorageKey('parent', 7, 'TASK')
  assert.notEqual(k1, activationStorageKey('child', 7, 'TASK'))
  assert.notEqual(k1, activationStorageKey('parent', 8, 'TASK'))
  assert.notEqual(k1, activationStorageKey('parent', 7, 'REWARD'))
  const values = new Map()
  const storage = {getItem:key=>values.get(key)??null, setItem:(key,value)=>values.set(key,value)}
  saveActivation(storage, k1, task())
  assert.deepEqual(readActivation(storage, k1, 7, 'TASK'), task())
  assert.equal(readActivation(storage, k1, 8, 'TASK'), null)
  assert.equal(readActivation(storage, k1, 7, 'REWARD'), null)
  values.set(k1, '{broken')
  assert.equal(readActivation(storage, k1, 7, 'TASK'), null)
})
test('V48 REWARD permits deliberate zero price and TIME minutes', () => {
  const record = makeActivation(7, 'REWARD', 2, 33, {
    rewardKind: 'TIME_BASED', priceAmount: 0, defaultDurationMinutes: 30
  }, uuid)
  assert.equal(validActivation(record, 7, 'REWARD', 2), true)
  assert.equal(verifyActivationReceipt({...receipt(), kind:'REWARD', templateId:33}, record).createdDefinitionId, 500)
})
