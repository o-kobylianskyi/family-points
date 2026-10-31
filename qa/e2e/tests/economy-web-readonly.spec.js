import { test, expect } from '@playwright/test'

// Read-only browser regression: browsing catalog never applies recommendation,
// imports templates, activates definitions, or changes balances.
const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD

test('template catalog displays optional economy hints without any writes', async ({ page }) => {
  test.skip(!username || !password, 'Requires isolated QA account.')
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(username)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
  if (new URL(page.url()).pathname === '/choose-family') {
    throw new Error('A preselected isolated QA family is required.')
  }
  const writes = []
  page.on('request', request => {
    const url = new URL(request.url())
    if (['POST','PUT','PATCH','DELETE'].includes(request.method()) &&
        (/\/templates(?:\/|$)/.test(url.pathname) || /\/rewards(?:\/|$)/.test(url.pathname))) {
      writes.push(request.method() + ' ' + url.pathname)
    }
  })
  const loaded = page.waitForResponse(response =>
    /\/template-catalog\/economy-suggestions$/.test(new URL(response.url()).pathname),
    { timeout: 15000 }
  )
  await page.goto('/templates', { waitUntil: 'domcontentloaded' })
  expect((await loaded).status()).toBe(200)
  await expect(page.locator('.template-grid .template-card').first()).toBeVisible({ timeout: 15000 })
  await expect(page.getByText(/Потрібне рішення батьків|Нужно решение родителей|Elternfreigabe nötig|Parent review required/).first()).toBeVisible()
  expect(writes, 'Browsing should never apply suggestions or mutate private data').toEqual([])
})
