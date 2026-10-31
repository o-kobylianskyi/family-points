import { test, expect } from '@playwright/test'

const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD

test.describe('BKL-002 negative reset checks (read-only)', () => {
  test.skip(!username || !password, 'Requires an isolated QA login')

  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.locator('form input[type="text"]').fill(username)
    await page.locator('form input[type="password"]').fill(password)
    await page.locator('form button[type="submit"]').click()
    await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
    if (new URL(page.url()).pathname === '/choose-family') {
      throw new Error('Choose an isolated QA family explicitly; test refuses automatic selection.')
    }
  })

  test('empty selection does not send plan or destructive requests', async ({ page }) => {
    const forbidden = []
    page.on('request', request => {
      const path = new URL(request.url()).pathname
      if (/\/data-reset\/(plan|prepare|confirm|challenge|execute)$/.test(path)) {
        forbidden.push(request.method() + ' ' + path)
      }
    })
    await page.goto('/settings', { waitUntil: 'domcontentloaded' })
    const panel = page.locator('.family-reset-readiness')
    const first = panel.locator('.family-reset-category input[type="checkbox"]').first()
    await expect(first).toBeVisible({ timeout: 15000 })
    const plan = panel.getByRole('button', { name: /Check dependencies|Перевірити залежності|Проверить зависимости|Abhängigkeiten prüfen/ })
    await expect(plan).toBeDisabled()
    await first.check()
    await expect(plan).toBeEnabled()
    await first.uncheck()
    await expect(plan).toBeDisabled()
    expect(forbidden).toEqual([])
  })

  test('preview never marks any category executable', async ({ page }) => {
    const previewRequest = page.waitForRequest(request =>
      request.method() === 'GET' && /\/workspaces\/\d+\/data-reset\/preview$/.test(new URL(request.url()).pathname),
      { timeout: 15000 })
    await page.goto('/settings', { waitUntil: 'domcontentloaded' })
    const request = await previewRequest
    const response = await page.request.get(request.url(), {
      headers: { Authorization: request.headers()['authorization'] },
      timeout: 12000,
    })
    expect(response.status()).toBe(200)
    const body = await response.json()
    expect(body.executionEnabled).toBe(false)
    expect(body.completeDependencyPlan).toBe(false)
    const categories = Object.values(body.categories || {})
    expect(categories.length).toBeGreaterThan(0)
    for (const category of categories) {
      expect(category.executable).toBe(false)
    }
  })
  test('reload keeps reset disabled and does not dispatch destructive actions', async ({ page }) => {
    const unsafe = []
    page.on('request', request => {
      const path = new URL(request.url()).pathname
      if (/\/data-reset\/(prepare|confirm|challenge|execute)$/.test(path)) unsafe.push(path)
    })
    await page.goto('/settings', { waitUntil: 'domcontentloaded' })
    const panel = page.locator('.family-reset-readiness')
    const first = panel.locator('.family-reset-category input[type="checkbox"]').first()
    await expect(first).toBeVisible({ timeout: 15000 })
    const originalCount = await panel.locator('.family-reset-category input[type="checkbox"]').count()
    await panel.getByRole('button', { name: /Reload|Завантажити знову|Обновить|Erneut laden/ }).click()
    await expect(first).toBeVisible({ timeout: 15000 })
    expect(await panel.locator('.family-reset-category input[type="checkbox"]').count()).toBe(originalCount)
    await expect(panel.getByRole('button', { name: /Check dependencies|Перевірити залежності|Проверить зависимости|Abhängigkeiten prüfen/ })).toBeDisabled()
    expect(unsafe).toEqual([])
  })

})
