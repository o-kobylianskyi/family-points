import { test, expect } from '@playwright/test'

// Read-only economy contract: verify nullable prices without applying them.
const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD

test('global economy suggestions remain optional and nullable', async ({ page }) => {
  test.skip(!username || !password, 'Requires isolated QA credentials.')
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(username)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
  if (new URL(page.url()).pathname === '/choose-family') throw new Error('Choose a dedicated QA family explicitly.')

  const token = await page.evaluate(() => localStorage.getItem('accessToken'))
  expect(token).toBeTruthy()
  const base = process.env.FP_QA_API_URL || 'http://localhost:8080'
  const response = await page.request.get(`${base}/template-catalog/economy-suggestions`, {
    headers: { Authorization: `Bearer ${token}` }, timeout: 12000,
  })
  expect(response.status()).toBe(200)
  const body = await response.json()
  expect(body.schema).toBe('familypoints.economy.suggestions.v1')
  expect(body.version).toBe(1)
  expect(body.currencyCode).toBe('POINTS')
  expect(body.autoApply).toBe(false)
  expect(body.workspaceMutation).toBe(false)
  expect(body.tasks).toHaveLength(110)
  expect(body.rewards).toHaveLength(32)
  expect(body.rewards.find(item => item.code === 'fp.reward.time.007')).toMatchObject({
    suggestedPricePoints: null, pricingState: 'UNSET', durationMinutes: 30,
  })
  for (const [items, field] of [[body.tasks, 'suggestedRewardPoints'], [body.rewards, 'suggestedPricePoints']]) {
    for (const item of items) {
      const amount = item[field]
      expect(amount === null || (Number.isSafeInteger(amount) && amount >= 0)).toBe(true)
      if (amount === null) expect(item.pricingState).toBe('UNSET')
    }
  }
})
