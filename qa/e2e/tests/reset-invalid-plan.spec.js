import { test, expect } from '@playwright/test'

// Validation-only request: an unknown category must never create a reset plan,
// prepare an operation, issue a challenge, or modify family data.
const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD

test('reset plan rejects unknown category without destructive API calls', async ({ page }) => {
  test.skip(!username || !password, 'Requires a dedicated, isolated QA account.')

  const destructive = []
  page.on('request', request => {
    const url = new URL(request.url())
    if (/\/data-reset\/(prepare|confirm|challenge|execute)$/.test(url.pathname)) {
      destructive.push(`${request.method()} ${url.pathname}`)
    }
  })

  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(username)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
  if (new URL(page.url()).pathname === '/choose-family') {
    throw new Error('Select a disposable QA family explicitly before running this test.')
  }

  const [preview] = await Promise.all([
    page.waitForResponse(response =>
      /\/workspaces\/\d+\/data-reset\/preview$/.test(new URL(response.url()).pathname),
      { timeout: 15000 }
    ),
    page.goto('/settings', { waitUntil: 'domcontentloaded' }),
  ])
  expect(preview.status()).toBe(200)
  const match = new URL(preview.url()).pathname.match(/\/workspaces\/(\d+)\//)
  expect(match).not.toBeNull()
  const workspaceId = match[1]
  const token = await page.evaluate(() => localStorage.getItem('accessToken'))
  expect(token).toBeTruthy()

  const response = await page.request.post(
    new URL(`/workspaces/${workspaceId}/data-reset/plan`, preview.url()).toString(),
    {
      headers: { Authorization: `Bearer ${token}` },
      data: { categories: ['NOT_A_RESET_CATEGORY'] },
      failOnStatusCode: false,
      timeout: 12000,
    }
  )
  expect(response.status(), 'unknown categories must be rejected').toBe(400)
  expect(destructive).toEqual([])
})
