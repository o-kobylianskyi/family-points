import test from 'node:test'
import assert from 'node:assert/strict'
import {PURCHASE_STATUSES, validateRewardHistoryPage, appendRewardHistory} from '../src/utils/rewardHistoryPage.js'

const purchase = id => ({id,status:'READY',rewardTitle:'Old snapshot',priceAmount:0})
test('purchase history supports 26+ rows with overlapping cursor pages', () => {
  const first = Array.from({length:25},(_,i)=>purchase(100-i))
  const second = [purchase(76), purchase(75), purchase(74)]
  const one = validateRewardHistoryPage({items:first,hasMore:true,nextCursor:'opaque'})
  const two = validateRewardHistoryPage({items:second,hasMore:false,nextCursor:null})
  const merged = appendRewardHistory(one.items,two.items)
  assert.equal(merged.length,27)
  assert.equal(new Set(merged.map(x=>x.id)).size,27)
  assert.equal(merged[25].id,75)
  assert.equal(merged[0].rewardTitle,'Old snapshot')
})
test('no invented RECEIVED or DELIVERED status', () => {
  assert.deepEqual(PURCHASE_STATUSES,['PENDING_APPROVAL','READY','REDEEMED','CANCELLED'])
  for(const status of ['RECEIVED','DELIVERED','COMPLETED','EXPIRED']) {
    assert.throws(()=>validateRewardHistoryPage({items:[{...purchase(1),status}],hasMore:false,nextCursor:null}))
  }
})
test('fail closed on bad pages or missing cursor', () => {
  assert.throws(()=>validateRewardHistoryPage({items:[],hasMore:true,nextCursor:null}))
  assert.throws(()=>validateRewardHistoryPage({items:[purchase(0)],hasMore:false,nextCursor:null}))
  assert.throws(()=>validateRewardHistoryPage({items:[],hasMore:'false',nextCursor:null}))
  assert.deepEqual(validateRewardHistoryPage({items:[],hasMore:false,nextCursor:null}).items,[])
})
