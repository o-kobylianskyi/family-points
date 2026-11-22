import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import {
  rewardRecoveryCopy, rewardRecoveryMessages, rewardRecoveryStatusMessage
} from '../src/utils/rewardPurchaseRecoveryCopy.js'

test('four full locales cover each recovery and purchase hold message', () => {
  const required = [
    'heading', 'check', 'checking', 'unknown', 'unavailable', 'auth',
    'conflict', 'confirmed', 'redeemed', 'cancelled', 'blocked', 'operation', 'status', 'legacy', 'originalPrice'
  ]
  for (const language of ['uk', 'ru', 'de', 'en']) {
    assert.deepEqual(Object.keys(rewardRecoveryMessages[language]).sort(), [...required].sort())
    for (const text of Object.values(rewardRecoveryCopy(language))) assert.ok(text.trim().length > 0)
    assert.equal(rewardRecoveryCopy(language + '-DE'), rewardRecoveryMessages[language])
    assert.equal(rewardRecoveryStatusMessage('CONFIRMED_PURCHASE', { purchase: { status: 'REDEEMED' } }, language),
      rewardRecoveryMessages[language].redeemed)
    assert.equal(rewardRecoveryStatusMessage('HISTORICAL_PURCHASE_CANCELLED_HOLD', null, language),
      rewardRecoveryMessages[language].cancelled)
    assert.equal(rewardRecoveryStatusMessage('NOT_FOUND_NO_REPLAY', null, language),
      rewardRecoveryMessages[language].unavailable)
    assert.equal(rewardRecoveryStatusMessage('AUTH_REQUIRED_NO_REPLAY', null, language),
      rewardRecoveryMessages[language].auth)
    assert.equal(rewardRecoveryStatusMessage('CONFLICT_NO_REPLAY', null, language),
      rewardRecoveryMessages[language].conflict)
    assert.equal(rewardRecoveryStatusMessage('LEGACY_SNAPSHOT_MISSING_HOLD', null, language),
      rewardRecoveryMessages[language].legacy)
    assert.equal(rewardRecoveryStatusMessage('UNKNOWN_NO_REPLAY', null, language),
      rewardRecoveryMessages[language].unknown)
  }
  assert.equal(rewardRecoveryCopy('fr'), rewardRecoveryMessages.en)
})

test('recovery panel is explicitly read-only and scoped per user/family/member with same UUID', () => {
  const panel = readFileSync(new URL('../src/components/RewardPurchaseRecoveryPanel.jsx', import.meta.url), 'utf8')
  const page = readFileSync(new URL('../src/pages/PointsPage.jsx', import.meta.url), 'utf8')
  const modal = readFileSync(new URL('../src/components/PointOperationModal.jsx', import.meta.url), 'utf8')
  assert.match(panel, /readRewardPurchaseIntent\(sessionStorage, scope\)/)
  assert.match(panel, /inspectPendingRewardPurchase\(/)
  assert.match(panel, /receiptFetcher: \(family, operationId\)/)
  assert.match(panel, /getRewardPurchaseOperationReceipt\(getAccessToken\(\), family, operationId\)/)
  assert.match(panel, /generation\.current/)
  assert.ok(panel.includes('pending.expectedPriceAmount'))
  assert.ok(panel.includes('pending.expectedPointTypeCode'))
  assert.ok(panel.includes('pending.version === 1'))
  assert.match(page, /<RewardPurchaseRecoveryPanel/)
  assert.match(page, /accountId=\{currentUser\.accountId \?\? currentUser\.userId \?\? currentUser\.id\}/)
  assert.match(page, /rewardPurchaseEnabled=\{false\}/)
  assert.match(modal, /!rewardPurchaseEnabled/)
  assert.doesNotMatch(panel + page, /purchaseRewardForMember|saveRewardPurchaseIntent|removeItem\(/)
})

test('manual spend remains permission-scoped and distinct from disabled reward purchase', () => {
  const page = readFileSync(new URL('../src/pages/PointsPage.jsx', import.meta.url), 'utf8')
  assert.match(page, /if \(!canManagePoints\) throw new Error/)
  assert.match(page, /case 'spend':\s+await spendPoints\(/)
  assert.match(page, /case 'penalty':\s+await penalizePoints\(/)
  assert.match(page, /if \(!canPurchaseRewards\) throw new Error/)
  assert.match(page, /throw new Error\(rewardRecoveryCopy\(/)
})
