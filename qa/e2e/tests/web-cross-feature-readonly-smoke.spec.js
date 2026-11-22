import { test, expect } from '@playwright/test'

// Every browser test in this file requires an explicitly provisioned disposable
// QA account/family. NO family writes, no real purchase, no onboarding/reset.
// Running without explicit isolation confirmation is a skipped test, not PASS.
const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD
const isolated = process.env.FP_QA_ISOLATED_WORKSPACE === '1'
const enabled = Boolean(username && password && isolated)
const languages = ['uk', 'ru', 'de', 'en']
const themes = ['light', 'dark']
const visibleRoutes = ['/tasks', '/rewards', '/points', '/calendar']
const forbidden = /\/workspaces\/\d+\//

async function signIntoDisposableFamily(page, language, theme) {
  await page.addInitScript(({ language, theme }) => {
    localStorage.setItem('language', language)
    localStorage.setItem('familypoints.theme.preference', theme)
  }, { language, theme })
  // Do not send ANY financial/task changes in synthetic read-only smoke.
  const mutations = []
  await page.route('**/workspaces/**', route => {
    const method = route.request().method()
    const path = new URL(route.request().url()).pathname
    if (forbidden.test(path) && !['GET', 'HEAD', 'OPTIONS'].includes(method)) {
      mutations.push(method + ' ' + path)
      return route.abort('blockedbyclient')
    }
    return route.continue()
  })
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(username)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
  test.skip(new URL(page.url()).pathname === '/choose-family',
    'Disposable QA family must be selected explicitly; no automatic family selection')
  return mutations
}

for (const language of languages) {
  for (const theme of themes) {
    test(`disposable read-only Web smoke: ${language}/${theme}, tasks rewards points calendar`, async ({ page }) => {
      test.skip(!enabled, 'Requires FP_QA_ISOLATED_WORKSPACE=1 and disposable QA credentials')
      test.setTimeout(90000)
      const mutations = await signIntoDisposableFamily(page, language, theme)
      for (const path of visibleRoutes) {
        await page.goto(path)
        await expect(page).toHaveURL(new RegExp(path.replace('/', '\\/') + '(?:\\?|$)'))
        await expect(page.locator('.page-content')).toBeVisible({ timeout: 15000 })
        await expect(page.locator('h1').first()).toBeVisible()
        const activeTheme = await page.evaluate(() => document.documentElement.dataset.theme)
        expect(activeTheme).toBe(theme)
        expect(mutations, 'Read-only smoke cannot mutate any QA workspace').toEqual([])
      }
      await page.goto('/rewards')
      await expect(page.getByText(/temporarily disabled|тимчасово вимкнено|временно отключена|vorübergehend|Sicherheitsprüfung/i).first()).toBeVisible()
      expect(mutations).toEqual([])
    })
  }
}

test('disposable mobile 390px viewport: protected shell navigation, themes, rewards and points read-only', async ({ page }) => {
  test.skip(!enabled, 'Requires FP_QA_ISOLATED_WORKSPACE=1 and disposable QA credentials')
  test.setTimeout(90000)
  await page.setViewportSize({ width: 390, height: 844 })
  const mutations = await signIntoDisposableFamily(page, 'uk', 'dark')
  for (const path of ['/tasks', '/points', '/rewards']) {
    await page.goto(path)
    await expect(page.locator('.page-content')).toBeVisible()
    const width = await page.evaluate(() => ({
      body: document.body.scrollWidth,
      viewport: document.documentElement.clientWidth,
    }))
    expect(width.body, `${path}: mobile page must not overflow horizontally`)
      .toBeLessThanOrEqual(width.viewport + 2)
    expect(mutations).toEqual([])
  }
})

test('browser financial POST guard: disabled catalog never sends legacy direct or approved-request purchase', async ({ page }) => {
  test.skip(!enabled, 'Requires FP_QA_ISOLATED_WORKSPACE=1 and disposable QA credentials')
  const mutations = await signIntoDisposableFamily(page, 'en', 'light')
  await page.goto('/rewards')
  const catalogBuy = page.locator('.rewards-grid button.primary-button')
  const count = await catalogBuy.count()
  // An empty disposable catalog is valid. If reward purchase buttons appear,
  // they MUST be disabled. Request submission remains a distinct workflow.
  for (let i = 0; i < count; i++) {
    const label = (await catalogBuy.nth(i).textContent() || '').trim()
    if (/Receive|Отримати|Получить|Einlösen/i.test(label)) {
      await expect(catalogBuy.nth(i)).toBeDisabled()
    }
  }
  expect(mutations).toEqual([])
})
