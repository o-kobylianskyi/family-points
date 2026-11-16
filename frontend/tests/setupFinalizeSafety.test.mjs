import test from 'node:test'
import assert from 'node:assert/strict'
import {validSetupFinalizeOperation, assertSetupFinalizeReceipt} from '../src/utils/setupFinalizeSafety.js'

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
