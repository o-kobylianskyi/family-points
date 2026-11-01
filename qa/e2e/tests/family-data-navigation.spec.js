import { test, expect } from '@playwright/test'

const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD

test('family data has one protected entry point with three read-only modes', async ({ page }) => {
  test.skip(!username || !password, 'Requires isolated QA credentials')
  const writes = []
  page.on('request', req => {
    const path = new URL(req.url()).pathname
    if (/\/data-reset\/(?:prepare|confirm|challenge|execute)|\/restore\/(?:apply|execute)|\/protective-backups\/(?:export|upload)/.test(path) || (req.method() !== 'GET' && /\/protective-backups(?:\/|$)/.test(path))) writes.push(`${req.method()} ${path}`)
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
  const full = panel.locator('input[name="safety-reset-mode"][type="radio"]').first()
  await full.check()
  const blockedRows = panel.locator('label').filter({ hasText: /Заблоковано|Заблокировано|gesperrt|Blocked pending/i })
  await expect(blockedRows.first()).toBeVisible()
  await expect(panel.locator('section[aria-label="Safety gates"]')).toBeVisible()
  await tabs.nth(2).click()
  await expect(panel.locator('section.family-reset-readiness')).toHaveCount(0)
  await expect(panel.locator('section[aria-label="Safety gates"]')).toBeVisible()
  await tabs.nth(0).click()
  await expect(panel.locator('section.family-reset-readiness')).toHaveCount(0)
  await expect(panel.getByText(/30 days|30 днів|30 дней|30 Tage/i)).toBeVisible()
  expect(writes).toEqual([])
})

test('family data navigation must not expose destructive controls to child accounts', async ({ page }) => {
  test.skip(!process.env.FP_QA_CHILD_USERNAME || !process.env.FP_QA_CHILD_PASSWORD, 'Requires isolated QA child credentials')
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(process.env.FP_QA_CHILD_USERNAME)
  await page.locator('form input[type="password"]').fill(process.env.FP_QA_CHILD_PASSWORD)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
  test.skip(new URL(page.url()).pathname === '/choose-family', 'Requires selected QA family')
  await page.goto('/settings')
  await expect(page.locator('section[aria-label="Family data safety planning"]')).toHaveCount(0)
  await expect(page.locator('section.family-reset-readiness')).toHaveCount(0)
})
