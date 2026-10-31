import { test, expect } from '@playwright/test'

const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD

test('public login page renders', async ({ page }) => {
  await page.goto('/login')
  await expect(page.locator('form input[type="password"]')).toBeVisible()
  await expect(page.locator('form input[type="text"]')).toBeVisible()
})

test.describe('authenticated QA-only smoke', () => {
  test.skip(!username || !password, 'Set FP_QA_USERNAME and FP_QA_PASSWORD for isolated QA account.')

  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.locator('form input[type="text"]').fill(username)
    await page.locator('form input[type="password"]').fill(password)
    await page.locator('form button[type="submit"]').click()
    try {
      await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
    } catch (error) {
      const message = (await page.locator('.login-card .error-message').textContent().catch(() => null))?.trim()
      throw new Error(message ? `QA login rejected: ${message}` : `QA login did not navigate (URL: ${page.url()}). Check Backend/API availability and Playwright trace.`, { cause: error })
    }
    if (new URL(page.url()).pathname === '/choose-family') {
      throw new Error('QA account needs a preselected isolated QA family; never automatically choose a real family.')
    }
  })

  test('private template page loads without destructive action', async ({ page }) => {
    await page.goto('/templates')
    await expect(page).toHaveURL(/\/templates(?:\?|$)/)
    await expect(page.getByText(/Бібліотека шаблонів|Template library|Vorlagenbibliothek|Библиотека шаблонов/).first()).toBeVisible()
    // Read-only smoke; do not import, activate or delete.
  })

  test('settings page is reachable; do not execute reset', async ({ page }) => {
    await page.goto('/settings')
    await expect(page).toHaveURL(/\/settings(?:\?|$)/)
    // No destructive controls are clicked.
  })
})
