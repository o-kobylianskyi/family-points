import { test, expect } from '@playwright/test'

// Supply a real second QA workspace ID that the authenticated QA actor does NOT belong to.
// Without this explicit fixture, do not invent another family's ID.
const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD
const foreignId = process.env.FP_QA_FOREIGN_WORKSPACE_ID

test.describe('BKL-002 server tenant isolation (read-only)', () => {
  test.skip(!username || !password || !foreignId,
    'Requires isolated QA credentials and explicit FP_QA_FOREIGN_WORKSPACE_ID')

  test('foreign workspace rejects preview and dependency plan', async ({ page }) => {
    expect(foreignId).toMatch(/^[1-9]\\d*$/)
    await page.goto('/login')
    await page.locator('form input[type="text"]').fill(username)
    await page.locator('form input[type="password"]').fill(password)
    await page.locator('form button[type="submit"]').click()
    await expect(page).not.toHaveURL(/\\/login(?:\\?|$)/, { timeout: 12000 })
    if (new URL(page.url()).pathname === '/choose-family') {
      throw new Error('Select the isolated QA family explicitly before running tenant tests.')
    }
    const previewCall = page.waitForRequest(r =>
      r.method() === 'GET' && /\\/workspaces\\/\\d+\\/data-reset\\/preview$/.test(new URL(r.url()).pathname),
      { timeout: 15000 })
    await page.goto('/settings', { waitUntil: 'domcontentloaded' })
    const ownUrl = new URL((await previewCall).url())
    const ownId = ownUrl.pathname.match(/\\/workspaces\\/(\\d+)\\//)?.[1]
    expect(ownId).toBeTruthy()
    expect(String(foreignId), 'Foreign workspace must differ from active workspace').not.toBe(String(ownId))
    const token = await page.evaluate(() => localStorage.getItem('accessToken'))
    expect(token).toBeTruthy()
    const base = ownUrl.origin + '/workspaces/' + foreignId
    const headers = { Authorization: 'Bearer ' + token }
    const preview = await page.request.get(base + '/data-reset/preview', {
      headers, failOnStatusCode: false, timeout: 12000,
    })
    expect([403, 404], 'Foreign preview cannot disclose data').toContain(preview.status())
    const plan = await page.request.post(base + '/data-reset/plan', {
      headers, data: { categories: ['PRIVATE_TEMPLATES'] },
      failOnStatusCode: false, timeout: 12000,
    })
    expect([403, 404], 'Foreign plan must reject before disclosing dependencies').toContain(plan.status())
  })
})
