import { test, expect } from '@playwright/test'

const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD

test('four locales: public login labels render', async ({ page }) => {
  await page.goto('/login')
  const language = page.locator('select.language-switcher')
  for (const [locale, label] of [
    ['uk', 'Українська'], ['ru', 'Русский'], ['de', 'Deutsch'], ['en', 'English'],
  ]) {
    await language.selectOption(locale)
    await expect(language).toHaveValue(locale)
    await expect(page.locator('.login-card form')).toBeVisible()
    await expect(page.locator('.login-card form button[type="submit"]')).not.toBeEmpty()
    await expect(page.locator('option[value="' + locale + '"]')).toHaveText(label)
  }
})

test.describe('reset preview/plan (no deletion)', () => {
  test.skip(!username || !password, 'Requires isolated QA login.')

  test('preview and dependency plan are non-executable', async ({ page }) => {
    test.setTimeout(60_000)
    const unsafe = []
    page.on('request', request => {
      const path = new URL(request.url()).pathname
      if (/\/data-reset\/(execute|confirm|challenge|prepare)$/.test(path)) unsafe.push(request.method() + ' ' + path)
    })
    await page.goto('/login')
    await page.locator('form input[type="text"]').fill(username)
    await page.locator('form input[type="password"]').fill(password)
    await page.locator('form button[type="submit"]').click()
    await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
    if (new URL(page.url()).pathname === '/choose-family') throw new Error('QA family must be selected explicitly; test will not choose a workspace.')

    // The app may mount/retry preview more than once. Assert against the
    // response that actually populates the UI, not the first HTTP 200 headers.
    await page.goto('/settings', { waitUntil: 'domcontentloaded', timeout: 10000 })
    const panel = page.locator('.family-reset-readiness')
    await expect(panel).toBeVisible()
    const token = await page.evaluate(() => localStorage.getItem('accessToken'))
    if (!token) throw new Error('Authenticated QA session has no access token')
    const previewUrl = await page.evaluate(() => performance.getEntriesByType('resource')
      .map(entry => entry.name).find(url => /\/workspaces\/\d+\/data-reset\/preview$/.test(new URL(url).pathname)))
    if (!previewUrl) throw new Error('No reset preview request observed in browser resources')
    const apiResponse = await page.request.get(previewUrl, {
      headers: { Authorization: `Bearer ${token}` },
      timeout: 12000,
    })
    expect(apiResponse.status(), 'independent authenticated reset preview').toBe(200)
    const preview = await apiResponse.json()
    const checkboxes = panel.locator('.family-reset-category input[type="checkbox"]')
    try {
      await expect(checkboxes.first()).toBeVisible({ timeout: 8000 })
    } catch (error) {
      const text = (await panel.innerText().catch(() => '')).slice(0, 500)
      throw new Error(`RESET_WEB_RENDER_FAIL: API preview parsed, but no categories rendered. panel=${JSON.stringify(text)}; url=${previewUrl}`, { cause: error })
    }
    expect(preview.executionEnabled).toBe(false)
    expect(preview.workspaceId).toBeTruthy()
    expect(preview.categories && typeof preview.categories).toBe('object')

    await expect(panel.locator('.family-reset-category input[type="checkbox"]').first()).toBeVisible()
    await expect(panel.locator('button', { hasText: /Check dependencies|Перевірити залежності|Проверить зависимости|Abhängigkeiten prüfen/ })).toBeDisabled()

    const first = panel.locator('.family-reset-category input[type="checkbox"]').first()
    await first.check()
    const planRequest = page.waitForResponse(r => /\/workspaces\/\d+\/data-reset\/plan$/.test(new URL(r.url()).pathname) && r.request().method()==='POST', { timeout: 15000 })
    await panel.locator('button', { hasText: /Check dependencies|Перевірити залежності|Проверить зависимости|Abhängigkeiten prüfen/ }).click()
    const planResponse = await planRequest
    expect(planResponse.status()).toBe(200)
    let plan
    try {
      plan = await planResponse.json()
    } catch (error) {
      throw new Error(`Reset plan response body was not readable: HTTP ${planResponse.status()}, URL ${planResponse.url()}, content-type ${planResponse.headers()['content-type'] || 'missing'}. Check Backend logs and Playwright trace.`, { cause: error })
    }
    expect(String(plan.workspaceId)).toBe(String(preview.workspaceId))
    expect(plan.executable).toBe(false)
    expect(Array.isArray(plan.blockers)).toBe(true)
    expect(unsafe, 'No reset prepare/challenge/confirm/execute requests').toEqual([])
  })
})
