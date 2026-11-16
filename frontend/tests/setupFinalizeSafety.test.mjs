import test from 'node:test'
import assert from 'node:assert/strict'
import {validSetupFinalizeOperation, assertSetupFinalizeReceipt, reconcileSetupPending, assertSetupState, assertSetupReview, SETUP_STEP_CODES} from '../src/utils/setupFinalizeSafety.js'

const operation = () => ({
  accountId: 'account-A',workspaceId: 14,epoch: 3,revision: 7,
  operationId:'e489ea25-4e10-48ee-b8ea-7a9f4b2e3456',codes:['TASK_A','REWARD_B']
})
const receipt = () => ({
  operationId:operation().operationId,workspaceId:14,dataEpoch:3,revision:8,setupStatus:'COMPLETED',
  importResult:{selectedCodes:['TASK_A','REWARD_B'],toImport:['TASK_A'],alreadyImported:['REWARD_B']}
})
test('V49 pending operation is isolated by account and family', () => {
  assert.equal(validSetupFinalizeOperation(operation(),14,'account-A'),true)
  assert.equal(validSetupFinalizeOperation(operation(),15,'account-A'),false)
  assert.equal(validSetupFinalizeOperation(operation(),14,'account-B'),false)
  assert.equal(validSetupFinalizeOperation({...operation(),codes:[]},14,'account-A'),false)
  assert.equal(validSetupFinalizeOperation({...operation(),codes:['TASK_A','TASK_A']},14,'account-A'),false)
  assert.equal(validSetupFinalizeOperation({...operation(),operationId:'bad'},14,'account-A'),false)
})
test('V49 validates complete, ordered and scoped receipt',()=>{
  assert.deepEqual(assertSetupFinalizeReceipt(receipt(),operation()),receipt())
  for(const patch of [
    {workspaceId:15},{dataEpoch:4},{revision:7},{setupStatus:'ACTIVE'},
    {operationId:'wrong'},{importResult:{...receipt().importResult,selectedCodes:['REWARD_B','TASK_A']}},
    {importResult:{...receipt().importResult,alreadyImported:['TASK_A']}},
    {importResult:{...receipt().importResult,toImport:['TASK_A','TASK_A']}},
  ]) assert.throws(()=>assertSetupFinalizeReceipt({...receipt(),...patch},operation()))
})
test('V49 does not accept a foreign selection even when counts agree',()=>{
  const forged={...receipt(),importResult:{selectedCodes:['TASK_A','REWARD_B'],toImport:['TASK_A'],alreadyImported:['FOREIGN']}}
  assert.throws(()=>assertSetupFinalizeReceipt(forged,operation()))
})

test('V49 cross-tab marker reconciliation rejects foreign or conflicting operations', () => {
  const first = operation()
  const second = {...first, operationId:'b489ea25-4e10-48ee-b8ea-7a9f4b2e3456'}
  assert.equal(reconcileSetupPending(null,null,14,'account-A'),null)
  assert.deepEqual(reconcileSetupPending(first,null,14,'account-A'),first)
  assert.deepEqual(reconcileSetupPending(null,first,14,'account-A'),first)
  assert.deepEqual(reconcileSetupPending(first,{...first},14,'account-A'),first)
  assert.deepEqual(reconcileSetupPending(first,second,14,'account-A'),{blocked:true})
  assert.deepEqual(reconcileSetupPending(first,{...first, codes:['REWARD_B','TASK_A']},14,'account-A'),{blocked:true})
  assert.deepEqual(reconcileSetupPending(first,{...first, revision:8},14,'account-A'),{blocked:true})
  assert.deepEqual(reconcileSetupPending(first,{...first, epoch:4},14,'account-A'),{blocked:true})
  assert.deepEqual(reconcileSetupPending(null,{...first,workspaceId:15},14,'account-A'),{blocked:true})
  assert.deepEqual(reconcileSetupPending(null,{blocked:true},14,'account-A'),{blocked:true})
})

test('V49 state honors empty V46 steps but rejects malformed and prematurely completed wizard',()=>{
  const initial={workspaceId:14,dataEpoch:0,revision:0,status:'NOT_STARTED',steps:{}}
  assert.deepEqual(assertSetupState(initial,14),initial)
  const full=Object.fromEntries(SETUP_STEP_CODES.map(code=>[code,'COMPLETE']))
  const complete={...initial,revision:7,status:'COMPLETED',steps:full}
  assert.deepEqual(assertSetupState(complete,14),complete)
  for(const bad of [
    {...initial,workspaceId:15}, {...initial,dataEpoch:-1},
    {...initial,revision:1.5},{...initial,steps:null},
    {...initial,steps:{OTHER:'COMPLETE'}}, {...initial,steps:{MEMBERS:'BAD'}},
    {...initial,status:'COMPLETED',steps:{}},
    {...complete,steps:{...full,FINAL_REVIEW:'PENDING'}},
    {...initial,status:'IN_PROGRESS',steps:[]},
  ])assert.throws(()=>assertSetupState(bad,14))
})
test('V49 review verifies immutable epoch/revision/code ordering and complete counts',()=>{
  const state={workspaceId:14,dataEpoch:3,revision:7,status:'IN_PROGRESS',steps:{}}
  const codes=['TASK_A','REWARD_B']
  const review={workspaceId:14,dataEpoch:3,revision:7,setupStatus:'IN_PROGRESS',
    selectedCount:2,toImportCount:1,alreadyImportedCount:1,
    selectedCodes:codes,toImport:['TASK_A'],alreadyImported:['REWARD_B'],
    requiresExplicitConfirmation:true,createsActiveDefinitions:false}
  assert.deepEqual(assertSetupReview(review,state,codes),review)
  for(const invalid of [
    {workspaceId:15},{dataEpoch:4},{revision:8},{setupStatus:'COMPLETED'},
    {selectedCount:3},{toImportCount:0},{alreadyImportedCount:0},
    {requiresExplicitConfirmation:false},{createsActiveDefinitions:true},
    {selectedCodes:[...codes].reverse()},{toImport:['REWARD_B']},
    {alreadyImported:['FOREIGN']},{toImport:['TASK_A','TASK_A'],toImportCount:2},
  ])assert.throws(()=>assertSetupReview({...review,...invalid},state,codes))
  assert.throws(()=>assertSetupReview(review,state,['TASK_A','TASK_A']))
})
