import { test, expect } from '@playwright/test'

const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD

test('family data has one protected entry point with three read-only modes', async ({ page }) => {
  test.skip(!username || !password, 'Requires isolated QA credentials')
  const writes = []
  page.on('request', req => {
    if (/\/data-reset\/(?:prepare|confirm|challenge|execute)|\/restore\/(?:apply|execute)|\/protective-backups\/(?:export|upload)/.test(req.url())) writes.push(req.url())
  })
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(username)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
  test.skip(new URL(page.url()).pathname === '/choose-family', 'Requires selected QA family')
  await page.goto('/settings')
  const panel = page.locator('section[aria-label="Family data safety planning"]')
  test.skip(await panel.count() === 0, 'Manager-only safety panel')
  await expect(panel).toHaveCount(1)
  const tabs = panel.locator('button[aria-pressed]')
  await expect(tabs).toHaveCount(3)
  await expect(tabs.filter({ hasText: /backup|резерв|sicher|копи/i })).toHaveCount(1)
  await tabs.nth(1).click()
  await expect(panel.locator('section.family-reset-readiness')).toHaveCount(1)
  await expect(panel.locator('section[aria-label="Safety gates"]')).toBeVisible()
  await tabs.nth(2).click()
  await expect(panel.locator('section.family-reset-readiness')).toHaveCount(0)
  await expect(panel.locator('section[aria-label="Safety gates"]')).toBeVisible()
  await tabs.nth(0).click()
  await expect(panel.locator('section.family-reset-readiness')).toHaveCount(0)
  expect(writes).toEqual([])
})
