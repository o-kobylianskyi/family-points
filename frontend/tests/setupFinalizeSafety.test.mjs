import test from 'node:test'
import assert from 'node:assert/strict'
import {validSetupFinalizeOperation, assertSetupFinalizeReceipt, reconcileSetupPending, assertSetupState, assertSetupReview, SETUP_STEP_CODES, reconcileSetupStepAfterFailure, assertSetupStepTransition, canChangeSetupStep, sameSetupFinalizeOperation, assertSetupCompletionAfterReceipt} from '../src/utils/setupFinalizeSafety.js'

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

test('V49 accepts a partially progressed seven-step Backend state before finalize',()=>{
  const partial={workspaceId:14,dataEpoch:2,revision:2,status:'IN_PROGRESS',
    steps:{MEMBERS:'COMPLETE',MEMBER_GROUPS:'SKIPPED'}}
  assert.deepEqual(assertSetupState(partial,14),partial)
  assert.deepEqual(assertSetupState({...partial,steps:{...partial.steps,WORK_NODE_GROUPS:'PENDING'}},14).steps.WORK_NODE_GROUPS,'PENDING')
})

test('V49 step PATCH lost-response reconciliation never implies a write replay',()=>{
  const before={workspaceId:14,dataEpoch:3,revision:2,status:'IN_PROGRESS',
    steps:{MEMBERS:'COMPLETE',MEMBER_GROUPS:'SKIPPED'}}
  const committed={...before,revision:3,steps:{...before.steps,WORK_NODE_GROUPS:'COMPLETE'}}
  assert.equal(reconcileSetupStepAfterFailure(before,committed,'WORK_NODE_GROUPS','COMPLETE'),'MATCHED')
  assert.equal(reconcileSetupStepAfterFailure(before,before,'WORK_NODE_GROUPS','COMPLETE'),'UNCONFIRMED')
  assert.equal(reconcileSetupStepAfterFailure(before,{...committed,steps:{...committed.steps,WORK_NODE_GROUPS:'SKIPPED'}},'WORK_NODE_GROUPS','COMPLETE'),'UNCONFIRMED')
  assert.equal(reconcileSetupStepAfterFailure(before,{...committed,revision:4},'WORK_NODE_GROUPS','COMPLETE'),'UNCONFIRMED')
  assert.equal(reconcileSetupStepAfterFailure(before,{
    ...committed,status:'COMPLETED',steps:Object.fromEntries(SETUP_STEP_CODES.map(code=>[code,'COMPLETE']))
  },'WORK_NODE_GROUPS','COMPLETE'),'UNCONFIRMED')
  assert.equal(reconcileSetupStepAfterFailure(before,{
    ...committed,steps:{...committed.steps,MEMBER_GROUPS:'COMPLETE'}
  },'WORK_NODE_GROUPS','COMPLETE'),'UNCONFIRMED')
  assert.equal(reconcileSetupStepAfterFailure(before,{
    ...committed,steps:{WORK_NODE_GROUPS:'COMPLETE'}
  },'WORK_NODE_GROUPS','COMPLETE'),'UNCONFIRMED')
  for(const foreign of [{...committed,workspaceId:15},{...committed,dataEpoch:4},{...committed,revision:1}])
    assert.throws(()=>reconcileSetupStepAfterFailure(before,foreign,'WORK_NODE_GROUPS','COMPLETE'))
  assert.throws(()=>reconcileSetupStepAfterFailure(before,committed,'UNKNOWN','COMPLETE'))
})

test('V49 verified receipt marker cleanup requires exact immutable operation identity',()=>{
  const original=operation()
  assert.equal(sameSetupFinalizeOperation(original,{...original}),true)
  for(const forged of [
    {...original,operationId:'b489ea25-4e10-48ee-b8ea-7a9f4b2e3456'},
    {...original,accountId:'other-account'}, {...original,workspaceId:15},
    {...original,epoch:4}, {...original,revision:8},
    {...original,codes:[...original.codes].reverse()},
    {...original,codes:['TASK_A','FOREIGN']},
    {...original,codes:['TASK_A','TASK_A']}, {blocked:true}, null,
  ])assert.equal(sameSetupFinalizeOperation(original,forged),false)
  assert.deepEqual(reconcileSetupPending(original,{...original,codes:['TASK_A','FOREIGN']},14,'account-A'),{blocked:true})
})

test('V49 final receipt cannot clear pending while server state is stale or incomplete',()=>{
  const op=operation()
  const all=Object.fromEntries(SETUP_STEP_CODES.map(code=>[code,'COMPLETE']))
  const completed={workspaceId:14,dataEpoch:op.epoch,revision:op.revision+1,
    status:'COMPLETED',steps:all}
  assert.deepEqual(assertSetupCompletionAfterReceipt(completed,op),completed)
  for(const forged of [
    {...completed,workspaceId:15}, {...completed,dataEpoch:op.epoch+1},
    {...completed,revision:op.revision}, {...completed,revision:op.revision+2},
    {...completed,status:'IN_PROGRESS'},
    {...completed,steps:{...all,FINAL_REVIEW:'PENDING'}},
    {...completed,steps:{...all,FINAL_REVIEW:undefined}},
    {...completed,steps:{}},
  ])assert.throws(()=>assertSetupCompletionAfterReceipt(forged,op))
  assert.throws(()=>assertSetupCompletionAfterReceipt(completed,{...op,codes:[]}))
})

test('V49 step controls avoid duplicate decisions and retain explicit status changes', () => {
  const state = {
    workspaceId: 14, dataEpoch: 2, revision: 3, status: 'IN_PROGRESS',
    steps: { MEMBERS: 'COMPLETE', MEMBER_GROUPS: 'SKIPPED' }
  }
  assert.equal(canChangeSetupStep(state, 'MEMBERS', 'COMPLETE'), false)
  assert.equal(canChangeSetupStep(state, 'MEMBER_GROUPS', 'SKIPPED'), false)
  assert.equal(canChangeSetupStep(state, 'MEMBERS', 'SKIPPED'), true)
  assert.equal(canChangeSetupStep(state, 'CATALOG_SELECTION', 'COMPLETE'), true)
  assert.equal(canChangeSetupStep(state, 'INVALID_STEP', 'COMPLETE'), false)
  assert.equal(canChangeSetupStep(state, 'MEMBERS', 'PENDING'), false)
  assert.equal(canChangeSetupStep({ ...state, status: 'COMPLETED' }, 'MEMBERS', 'SKIPPED'), false)
  assert.equal(canChangeSetupStep(null, 'MEMBERS', 'COMPLETE'), false)
})

test('V49 step PATCH response is rejected if epoch or revision scope changed', () => {
  const before = { workspaceId: 14, dataEpoch: 4, revision: 10,
    status: 'IN_PROGRESS', steps: { MEMBERS: 'COMPLETE' } }
  const after = { ...before, revision: 11,
    steps: { ...before.steps, MEMBER_GROUPS: 'SKIPPED' } }
  assert.deepEqual(assertSetupStepTransition(before, after, 'MEMBER_GROUPS', 'SKIPPED'), after)
  for (const changed of [
    { ...after, workspaceId: 15 },
    { ...after, dataEpoch: 5 },
    { ...after, revision: 12 },
    { ...after, steps: { ...after.steps, MEMBER_GROUPS: 'COMPLETE' } },
    { ...after, steps: { ...after.steps, MEMBER_GROUPS: 'PENDING' } },
    { ...after, status: 'COMPLETED', steps: Object.fromEntries(SETUP_STEP_CODES.map(code => [code, 'COMPLETE'])) },
    { ...after, steps: { ...after.steps, MEMBERS: 'SKIPPED' } },
    { ...after, steps: { MEMBER_GROUPS: 'SKIPPED' } },
  ]) assert.throws(() => assertSetupStepTransition(before, changed, 'MEMBER_GROUPS', 'SKIPPED'))
  assert.throws(() => assertSetupStepTransition(before, after, 'UNRECOGNIZED', 'SKIPPED'))
  assert.throws(() => assertSetupStepTransition(before, after, 'MEMBER_GROUPS', 'PENDING'))
})

test('V49 step PATCH may add pending metadata but cannot finalize or overwrite other steps', () => {
  const before = { workspaceId: 14, dataEpoch: 4, revision: 10,
    status: 'NOT_STARTED', steps: {} }
  const after = { ...before, revision: 11, status: 'IN_PROGRESS',
    steps: { MEMBERS: 'COMPLETE', MEMBER_GROUPS: 'PENDING' } }
  assert.deepEqual(assertSetupStepTransition(before, after, 'MEMBERS', 'COMPLETE'), after)
  const alreadyDone = { ...after, status: 'COMPLETED',
    steps: Object.fromEntries(SETUP_STEP_CODES.map(code => [code, 'COMPLETE'])) }
  assert.throws(() => assertSetupStepTransition(alreadyDone, {
    ...alreadyDone, revision: 12
  }, 'MEMBERS', 'COMPLETE'))
})
