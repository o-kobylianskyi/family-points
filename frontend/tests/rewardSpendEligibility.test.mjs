import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { eligibleDirectRewards, rewardScheduleIsOpen } from '../src/utils/rewardSpendEligibility.js'

const good = {
  id: 12, title: 'Cinema night', active: true, acquisitionMode: 'DIRECT',
  requiresApproval: false, priceAmount: 10, pointTypeId: 2, pointTypeCode: 'POINTS'
}

test('reward catalog fail-closed: only server-shaped direct active priced entries selectable', () => {
  const invalid = [
    { ...good, id: 0 }, { ...good, title: '' }, { ...good, active: false },
    { ...good, active: undefined }, { ...good, acquisitionMode: 'REQUEST' },
    { ...good, requiresApproval: true }, { ...good, requiresApproval: undefined },
    { ...good, priceAmount: -1 }, { ...good, priceAmount: '10' },
    { ...good, priceAmount: NaN }, { ...good, pointTypeId: null },
    { ...good, pointTypeCode: '' }, null
  ]
  assert.deepEqual(eligibleDirectRewards([good, { ...good, id: 13, priceAmount: 0 }, ...invalid]).map(x => x.id), [12, 13])
  assert.deepEqual(eligibleDirectRewards(null), [])
})

test('reward schedule validation rejects wrong reward, stale and missing confirmation', () => {
  assert.equal(rewardScheduleIsOpen({ rewardId: 12, scheduleOpenNow: true }, 12), true)
  assert.equal(rewardScheduleIsOpen({ rewardId: 99, scheduleOpenNow: true }, 12), false)
  assert.equal(rewardScheduleIsOpen({ rewardId: 12, scheduleOpenNow: false }, 12), false)
  assert.equal(rewardScheduleIsOpen({ rewardId: 12 }, 12), false)
  assert.equal(rewardScheduleIsOpen(null, 12), false)
})

test('reward spending modal only submits after authoritative schedule and preserves distinct manual spend', () => {
  const modal = readFileSync(new URL('../src/components/PointOperationModal.jsx', import.meta.url), 'utf8')
  const page = readFileSync(new URL('../src/pages/PointsPage.jsx', import.meta.url), 'utf8')
  assert.match(modal, /const canSubmitReward =/)
  assert.match(modal, /scheduleConfirmed/)
  assert.match(modal, /if \(!canSubmitReward\) return/)
  assert.match(modal, /setRewardAttempted\(true\)/)
  assert.match(modal, /await onSubmit\(\{ operation, rewardId: chosenReward\.id \}\)/)
  assert.match(page, /loadRewardAvailability=\{loadRewardAvailabilityForSpend\}/)
  assert.match(page, /rewardPurchaseEnabled=\\{false\\}/)
  assert.match(modal, /rewardPurchaseEnabled && !!chosenReward/)
  assert.doesNotMatch(page, /purchaseRewardForMember\\(/)
  assert.match(modal, /value="manual" disabled=\{!canManagePoints\}/)
  assert.match(modal, /useState\(canManagePoints \? 'manual' : 'reward'\)/)
  assert.match(page, /\{\(canManagePoints \|\| canPurchaseRewards\) &&/)
  assert.match(page, /if \(!canManagePoints\) throw new Error\('Point management permission required'\)/)
  assert.match(page, /case 'spend':\s+await spendPoints\(/)
  assert.match(page, /case 'penalty':\s+await penalizePoints\(/)
})
