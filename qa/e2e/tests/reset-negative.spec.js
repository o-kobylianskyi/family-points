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

  test('changing categories clears the previously calculated dependency plan', async ({ page }) => {
    const forbidden = []
    page.on('request', request => {
      if (/\/data-reset\/(prepare|confirm|challenge|execute)$/.test(new URL(request.url()).pathname)) {
        forbidden.push(request.method() + ' ' + request.url())
      }
    })
    await page.goto('/settings', { waitUntil: 'domcontentloaded' })
    const panel = page.locator('.family-reset-readiness')
    const checkbox = panel.locator('.family-reset-category input[type="checkbox"]').first()
    await expect(checkbox).toBeVisible({ timeout: 15000 })
    await checkbox.check()
    const planButton = panel.getByRole('button', { name: /Check dependencies|Перевірити залежності|Проверить зависимости|Abhängigkeiten prüfen/ })
    const planResponse = page.waitForResponse(response =>
      response.request().method() === 'POST' && /\/data-reset\/plan$/.test(new URL(response.url()).pathname),
      { timeout: 15000 }
    )
    await planButton.click()
    expect((await planResponse).status()).toBe(200)
    await expect(panel.locator('div[role="status"]')).toBeVisible()
    await checkbox.uncheck()
    await expect(panel.locator('div[role="status"]')).toHaveCount(0)
    await expect(planButton).toBeDisabled()
    expect(forbidden).toEqual([])
  })

  test('mismatched workspace preview is rejected without rendering categories', async ({ page }) => {
    const unsafe = []
    let intercepted = 0
    page.on('request', request => {
      if (/\/data-reset\/(plan|prepare|confirm|challenge|execute)$/.test(new URL(request.url()).pathname)) {
        unsafe.push(request.url())
      }
    })
    await page.route(/\/workspaces\/\d+\/data-reset\/preview(?:\?.*)?$/, async route => {
      intercepted += 1
      const current = Number(new URL(route.request().url()).pathname.match(/\/workspaces\/(\d+)\//)[1])
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          workspaceId: current + 1,
          executionEnabled: false,
          completeDependencyPlan: false,
          alwaysPreserved: [],
          categories: { PRIVATE_TEMPLATES: { code: 'PRIVATE_TEMPLATES', recordCount: 1, executable: false } },
        }),
      })
    })
    await page.goto('/settings', { waitUntil: 'domcontentloaded' })
    const panel = page.locator('.family-reset-readiness')
    await expect(panel).toBeVisible()
    await expect(panel.locator('.error-message')).toBeVisible({ timeout: 10000 })
    await expect(panel.locator('.family-reset-category input[type="checkbox"]')).toHaveCount(0)
    expect(intercepted).toBeGreaterThan(0)
    expect(unsafe).toEqual([])
  })

})
