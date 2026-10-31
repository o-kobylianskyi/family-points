import { test, expect } from '@playwright/test'

// Read-only regression for category display labels; API category codes remain unchanged.
const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD
const expected = [
  ['uk', 'Домашні справи'],
  ['ru', 'Домашние дела'],
  ['de', 'Hausarbeit'],
  ['en', 'Chores'],
]

test('template catalog localizes category options in four locales', async ({ page }) => {
  test.skip(!username || !password, 'Requires isolated authenticated QA account.')
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(username)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
  if (new URL(page.url()).pathname === '/choose-family') throw new Error('Select an isolated QA family explicitly.')
  await page.goto('/templates')
  const selects = page.locator('.template-toolbar select')
  await expect(selects.first()).toBeVisible()
  await expect(selects.nth(1).locator('option[value="chores"]')).toHaveCount(1)
  for (const [locale, label] of expected) {
    const switcher = page.locator('select.language-switcher')
    await expect(switcher).toBeVisible()
    await switcher.selectOption(locale)
    await expect(page.locator('.template-toolbar select').nth(1).locator('option[value="chores"]')).toHaveText(label)
  }
})
