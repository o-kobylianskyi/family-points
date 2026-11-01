import { test, expect } from '@playwright/test'

const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD

test('settings renders without build-info ReferenceError after repository restore', async ({ page }) => {
  test.skip(!username || !password, 'Requires isolated QA credentials')
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(username)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
  if (new URL(page.url()).pathname === '/choose-family') throw new Error('A designated isolated QA workspace is required.')
  await page.goto('/settings')
  await expect(page.locator('section[aria-label="Build information"]')).toBeVisible()
  await expect(page.locator('section[aria-label="Build information"] dd')).toHaveCount(4)
  expect(errors.filter(message => /__FP_BUILD_INFO__|SettingsPage/.test(message))).toEqual([])
})
