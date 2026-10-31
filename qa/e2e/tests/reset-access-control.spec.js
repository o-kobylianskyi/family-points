import { test, expect } from '@playwright/test'

// No credentials, family selection, or persistent mutations are involved.
test('unauthenticated reset preview is rejected', async ({ request }) => {
  const response = await request.get('http://localhost:8080/workspaces/14/data-reset/preview', {
    failOnStatusCode: false,
    timeout: 12000,
  })
  expect(response.status()).toBe(401)
})

test('settings reset panel cannot be reached while logged out', async ({ page }) => {
  await page.goto('/settings')
  await expect(page).toHaveURL(/\/login(?:\?|$)/, { timeout: 10000 })
  await expect(page.locator('.family-reset-readiness')).toHaveCount(0)
})

test('unauthenticated reset plan is denied', async ({ request }) => {
  const response = await request.post('http://localhost:8080/workspaces/14/data-reset/plan', {
    data: { categories: ['PRIVATE_TEMPLATES'] },
    failOnStatusCode: false,
    timeout: 12000,
  })
  expect(response.status()).toBe(401)
})

test('unauthenticated workspace epoch is denied', async ({ request }) => {
  const response = await request.get('http://localhost:8080/workspaces/14/data-epoch', {
    failOnStatusCode: false,
    timeout: 12000,
  })
  expect(response.status()).toBe(401)
})
