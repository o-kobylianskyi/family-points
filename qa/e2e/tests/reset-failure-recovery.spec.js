import { test, expect } from '@playwright/test'

const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD
const forbiddenPattern = /\/data-reset\/(prepare|confirm|challenge|execute)$/

const previewPattern = /\/workspaces\/\d+\/data-reset\/preview(?:\?.*)?$/
function previewBody(route, count = 3) {
  const id = Number(new URL(route.request().url()).pathname.match(/\/workspaces\/(\d+)\//)[1])
  return {
    workspaceId: id, executionEnabled: false, completeDependencyPlan: false,
    alwaysPreserved: [],
    categories: { PRIVATE_TEMPLATES: { code: 'PRIVATE_TEMPLATES', recordCount: count, executable: false } },
  }
}
async function mockPreview(page) {
  await page.route(previewPattern, route => route.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify(previewBody(route)),
  }))
}



async function diagnoseResetPage(page, label) {
  const diagnostic = await page.evaluate(() => ({
    pathname: location.pathname,
    title: document.title,
    headings: [...document.querySelectorAll('h1,h2')].map(node => node.textContent?.trim()).slice(0, 20),
    panelCount: document.querySelectorAll('.family-reset-readiness').length,
    panelText: document.querySelector('.family-reset-readiness')?.textContent?.slice(0, 1000) || null,
    checkboxes: document.querySelectorAll('.family-reset-category input[type="checkbox"]').length,
    alerts: [...document.querySelectorAll('[role="alert"]')].map(node => node.textContent?.trim()).slice(0, 5),
    hasAccessToken: Boolean(localStorage.getItem('accessToken')),
  }))
  console.log('[RESET_QA_DIAGNOSTIC] ' + label + ': ' + JSON.stringify(diagnostic))
  return diagnostic
}

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
    // Do not visit Templates or Settings in setup: install routes first,
    // then navigate directly to Settings within the test.
    page.on('pageerror', error => console.log('[RESET_QA_PAGEERROR]', error.message))
    page.on('response', response => {
      if (/\/data-reset\/preview(?:\?|$)/.test(new URL(response.url()).pathname)) {
        console.log('[RESET_QA_PREVIEW_RESPONSE]', response.status(), response.url())
      }
    })
  })

  test('preview failure is fail-closed and Reload recovers', async ({ page }) => {
    const forbidden = []
    let intercepted = 0
    let recovering = false
    page.on('request', req => {
      if (forbiddenPattern.test(new URL(req.url()).pathname)) forbidden.push(req.url())
    })
    await page.route(/\/workspaces\/\d+\/data-reset\/preview(?:\?.*)?$/, async route => {
      intercepted++
      if (!recovering) {
        await route.fulfill({
          status: 503, contentType: 'application/json',
          body: JSON.stringify({ code: 'TEMPORARY_FAILURE' }),
        })
      } else {
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(previewBody(route)) })
      }
    })
    await page.goto('/settings', { waitUntil: 'domcontentloaded' })
    const panel = page.locator('.family-reset-readiness')
    await expect.poll(() => intercepted, { timeout: 12000 }).toBeGreaterThan(0)
    await diagnoseResetPage(page, 'after intercepted preview')
    await expect(panel.locator('.error-message')).toBeVisible({ timeout: 12000 })
    await expect(panel.locator('.family-reset-category input[type="checkbox"]')).toHaveCount(0)
    recovering = true
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
    await mockPreview(page)
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
    await diagnoseResetPage(page, 'before Plan selection')
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
  test('late Plan response cannot restore a plan after page navigation', async ({ page }) => {
    const forbidden = []
    page.on('request', req => {
      if (forbiddenPattern.test(new URL(req.url()).pathname)) forbidden.push(req.url())
    })
    let releasePlan
    const planArrived = new Promise(resolve => {
      releasePlan = { arrived: resolve }
    })
    let finishResponse
    const unblock = new Promise(resolve => { finishResponse = resolve })
    let interceptionCount = 0
    await mockPreview(page)
    await page.route(/\/workspaces\/\d+\/data-reset\/plan(?:\?.*)?$/, async route => {
      interceptionCount += 1
      releasePlan.arrived()
      await unblock
      const url = new URL(route.request().url())
      const id = url.pathname.match(/\/workspaces\/(\d+)\//)?.[1]
      const payload = route.request().postDataJSON()
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          workspaceId: Number(id), executable: false,
          selectedCategories: payload.categories,
          blockers: ['Execution is disabled until dependent FK graph and tenant-safe deletion order are approved'],
          preserved: [],
        }),
      })
    })
    try {
      await page.goto('/settings', { waitUntil: 'domcontentloaded' })
      const panel = page.locator('.family-reset-readiness')
      const checkbox = panel.locator('.family-reset-category input[type="checkbox"]').first()
      await expect(checkbox).toBeVisible({ timeout: 15000 })
      await checkbox.check()
      const button = panel.getByRole('button', {
        name: /Check dependencies|Перевірити залежності|Проверить зависимости|Abhängigkeiten prüfen/,
      })
      await button.click()
      await Promise.race([
        planArrived,
        new Promise((_, reject) => setTimeout(() => reject(new Error('Plan request never arrived')), 10000)),
      ])
      // Navigate away to invalidate the generation that issued the Plan.
      await page.goto('/templates', { waitUntil: 'domcontentloaded' })
      finishResponse()
      await page.goto('/settings', { waitUntil: 'domcontentloaded' })
      const nextPanel = page.locator('.family-reset-readiness')
      await expect(nextPanel.locator('.family-reset-category input[type="checkbox"]').first())
        .toBeVisible({ timeout: 15000 })
      await expect(nextPanel.locator('div[role="status"]')).toHaveCount(0)
      await expect(nextPanel.getByRole('button', {
        name: /Check dependencies|Перевірити залежності|Проверить зависимости|Abhängigkeiten prüfen/,
      })).toBeDisabled()
      expect(interceptionCount).toBe(1)
      expect(forbidden).toEqual([])
    } finally {
      finishResponse()
    }
  })

  test('late Preview cannot restore stale categories after leaving Settings', async ({ page }) => {
    const forbidden = []
    page.on('request', req => {
      if (forbiddenPattern.test(new URL(req.url()).pathname)) forbidden.push(req.url())
    })
    let release
    const unblock = new Promise(resolve => { release = resolve })
    let started
    const arrived = new Promise(resolve => { started = resolve })
    let hits = 0
    await page.route(/\/workspaces\/\d+\/data-reset\/preview(?:\?.*)?$/, async route => {
      hits++
      if (hits !== 1) { await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(previewBody(route)) }); return }
      started()
      await unblock
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          workspaceId: Number(new URL(route.request().url()).pathname.match(/\/workspaces\/(\d+)\//)[1]),
          executionEnabled: false, completeDependencyPlan: false,
          categories: { PRIVATE_TEMPLATES: { code: 'PRIVATE_TEMPLATES', recordCount: 999, executable: false } },
        }),
      }).catch(() => {}) // Navigation may cancel the abandoned request.
    })
    try {
      await page.goto('/settings', { waitUntil: 'domcontentloaded' })
      await arrived
      await page.goto('/templates', { waitUntil: 'domcontentloaded' })
      release()
      await page.goto('/settings', { waitUntil: 'domcontentloaded' })
      const panel = page.locator('.family-reset-readiness')
      await expect(panel.locator('.family-reset-category input[type="checkbox"]').first())
        .toBeVisible({ timeout: 15000 })
      // Current preview must come from the new request, not the abandoned sentinel.
      await expect(panel.getByText('999', { exact: true })).toHaveCount(0)
      await expect(panel.locator('.family-reset-category input[type="checkbox"]:checked')).toHaveCount(0)
      expect(hits).toBeGreaterThanOrEqual(2)
      expect(forbidden).toEqual([])
    } finally { release() }
  })

})
