import { test, expect } from '@playwright/test'

const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD
const forbiddenPattern = /\/data-reset\/(prepare|confirm|challenge|execute)$/

test.describe('BKL-002 read-only error recovery', () => {
  test.skip(!username || !password, 'Requires isolated QA account credentials')

  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.locator('form input[type="text"]').fill(username)
    await page.locator('form input[type="password"]').fill(password)
    await page.locator('form button[type="submit"]').click()
    await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
    if (new URL(page.url()).pathname === '/choose-family') {
      throw new Error('QA family must be selected explicitly')
    }
  })

  test('preview failure is fail-closed and Reload recovers', async ({ page }) => {
    const forbidden = []
    let intercepted = 0
    page.on('request', req => {
      if (forbiddenPattern.test(new URL(req.url()).pathname)) forbidden.push(req.url())
    })
    await page.route(/\/workspaces\/\d+\/data-reset\/preview(?:\?.*)?$/, async route => {
      intercepted++
      if (intercepted === 1) {
        await route.fulfill({
          status: 503, contentType: 'application/json',
          body: JSON.stringify({ code: 'TEMPORARY_FAILURE' }),
        })
      } else {
        await route.continue()
      }
    })
    await page.goto('/settings', { waitUntil: 'domcontentloaded' })
    const panel = page.locator('.family-reset-readiness')
    await expect(panel.locator('.error-message')).toBeVisible({ timeout: 12000 })
    await expect(panel.locator('.family-reset-category input[type="checkbox"]')).toHaveCount(0)
    await panel.getByRole('button', {
      name: /Reload|Завантажити знову|Обновить|Erneut laden/,
    }).click()
    await expect(panel.locator('.family-reset-category input[type="checkbox"]').first())
      .toBeVisible({ timeout: 15000 })
    await expect(panel.locator('.error-message')).toHaveCount(0)
    expect(intercepted).toBeGreaterThanOrEqual(2)
    expect(forbidden).toEqual([])
  })

  test('invalid Plan response is rejected and never enables reset', async ({ page }) => {
    const forbidden = []
    let intercepted = 0
    page.on('request', req => {
      if (forbiddenPattern.test(new URL(req.url()).pathname)) forbidden.push(req.url())
    })
    await page.route(/\/workspaces\/\d+\/data-reset\/plan(?:\?.*)?$/, async route => {
      intercepted++
      const match = new URL(route.request().url()).pathname.match(/\/workspaces\/(\d+)\//)
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          workspaceId: Number(match[1]) + 1,
          executable: true, selectedCategories: ['PRIVATE_TEMPLATES'],
          blockers: [],
        }),
      })
    })
    await page.goto('/settings', { waitUntil: 'domcontentloaded' })
    const panel = page.locator('.family-reset-readiness')
    const first = panel.locator('.family-reset-category input[type="checkbox"]').first()
    await expect(first).toBeVisible({ timeout: 15000 })
    await first.check()
    await panel.getByRole('button', {
      name: /Check dependencies|Перевірити залежності|Проверить зависимости|Abhängigkeiten prüfen/,
    }).click()
    await expect(panel.locator('.error-message')).toBeVisible({ timeout: 12000 })
    await expect(panel.locator('div[role="status"]')).toHaveCount(0)
    expect(intercepted).toBe(1)
    expect(forbidden).toEqual([])
  })
})
