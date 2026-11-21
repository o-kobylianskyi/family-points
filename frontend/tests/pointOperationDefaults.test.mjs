import test from 'node:test'
import assert from 'node:assert/strict'
import {POINT_AMOUNT_PRESETS, initialPointOperationAmount} from '../src/utils/pointOperationDefaults.js'

test('Points modal quick amounts are increasing multiples of five',()=>{
  assert.equal(POINT_AMOUNT_PRESETS[0],5)
  assert.ok(POINT_AMOUNT_PRESETS.length >= 15)
  assert.ok(POINT_AMOUNT_PRESETS.every((v,i)=>Number.isInteger(v) && v === (i+1)*5))
})
test('Points modal defaults to last matching MANUAL operation per member and kind',()=>{
  const history=[
    {memberId:7,type:'EARN',sourceType:'TASK',amount:80},
    {memberId:8,type:'SPEND',sourceType:'MANUAL',amount:-60},
    {memberId:7,type:'SPEND',sourceType:'MANUAL',amount:-20},
    {memberId:7,type:'EARN',sourceType:'MANUAL',amount:35},
    {memberId:7,type:'SPEND',sourceType:'MANUAL',amount:-15},
    {memberId:7,type:'PENALTY',sourceType:'MANUAL',amount:-10},
  ]
  assert.equal(initialPointOperationAmount(history,'earn',7),35)
  assert.equal(initialPointOperationAmount(history,'spend',7),20)
  assert.equal(initialPointOperationAmount(history,'penalty',7),10)
  assert.equal(initialPointOperationAmount(history,'spend',8),60)
  assert.equal(initialPointOperationAmount(history,'spend',9),5)
  assert.equal(initialPointOperationAmount(history,'unknown',7),5)
})
test('Points modal refuses malformed history amount and non-manual entries as defaults',()=>{
  const rows=[
    {memberId:9,type:'EARN',sourceType:'MANUAL',amount:0},
    {memberId:9,type:'EARN',sourceType:'MANUAL',amount:Infinity},
    {memberId:9,type:'EARN',sourceType:'MANUAL',amount:100001},
    {memberId:9,type:'EARN',sourceType:'TASK',amount:10},
    {memberId:9,type:'EARN',sourceType:'MANUAL',amount:7},
  ]
  assert.equal(initialPointOperationAmount(rows,'earn',9),7)
  assert.equal(initialPointOperationAmount(null,'earn',9),5)
})

import {readFileSync} from 'node:fs'
test('successful point POST cannot be retried because the following history GET failed',()=>{
  const page=readFileSync(new URL('../src/pages/PointsPage.jsx',import.meta.url),'utf8')
  assert.match(page,/await refreshPoints\(\)\s*setError\(''\)\s*\} catch \{\s*setError\(t\('points\.refreshAfterMutationFailed'\)\)/)
  const modal=readFileSync(new URL('../src/components/PointOperationModal.jsx',import.meta.url),'utf8')
  assert.match(modal,/await onSubmit\(\{[\s\S]*?\}\)\s*onClose\(\)/)
  assert.match(modal,/stepperStep=\{5\}/)
  assert.match(modal,/POINT_AMOUNT_PRESETS\.map\(/)
})
