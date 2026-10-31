import { test, expect } from '@playwright/test'

const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD

test.describe('read-only catalog and reset safety', () => {
  test.skip(!username || !password, 'QA username/password are required.')

  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.locator('form input[type="text"]').fill(username)
    await page.locator('form input[type="password"]').fill(password)
    await page.locator('form button[type="submit"]').click()
    await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
    if (new URL(page.url()).pathname === '/choose-family') {
      throw new Error('Select an isolated QA family manually first; refusing automatic workspace selection.')
    }
  })

  test('TPL-01: catalog API supplies 142 suggestions and 6 packs', async ({ page }) => {
    const responsePromise = page.waitForResponse(r =>
      new URL(r.url()).pathname.endsWith('/template-catalog') && r.request().method() === 'GET',
      { timeout: 15000 },
    )
    await page.goto('/templates')
    const response = await responsePromise
    expect(response.status(), 'catalog HTTP status').toBe(200)
    const body = await response.json()
    expect(Array.isArray(body.templates), 'catalog.templates must be an array').toBe(true)
    expect(Array.isArray(body.packs), 'catalog.packs must be an array').toBe(true)
    expect(body.templates).toHaveLength(142)
    expect(body.packs).toHaveLength(6)
    expect(new Set(body.templates.map(t => t.code)).size).toBe(142)
    expect(body.templates.filter(t => t.kind === 'TASK')).toHaveLength(110)
    expect(body.templates.filter(t => t.kind === 'REWARD')).toHaveLength(32)
    await expect(page.locator('.templates-page')).toBeVisible()
  })

  test('BKL-002: settings visit never initiates reset execution', async ({ page }) => {
    const dangerous = []
    page.on('request', request => {
      const path = new URL(request.url()).pathname
      if (request.method() !== 'GET' && /\/data-reset\/(execute|confirm|challenge)$/.test(path)) {
        dangerous.push(`${request.method()} ${path}`)
      }
    })
    await page.goto('/settings')
    await expect(page).toHaveURL(/\/settings(?:\?|$)/)
    await expect(page.locator('body')).toBeVisible()
    expect(dangerous, 'Settings navigation must not execute, confirm or challenge reset').toEqual([])
  })
})
