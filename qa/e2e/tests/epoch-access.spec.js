import { test, expect } from '@playwright/test'

// Read-only HTTP checks. Never issue POST/PUT/PATCH/DELETE or change epoch.
const actors = [
  { label: 'Test', username: process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL,
    password: process.env.FP_QA_PASSWORD, own: '14', foreign: '11' },
  { label: 'Test2', username: process.env.FP_QA_TEST2_USERNAME || 'Test2',
    password: process.env.FP_QA_TEST2_PASSWORD, own: '11', foreign: '14' },
]
const origin = process.env.FP_QA_API_URL || 'http://localhost:8080'

for (const actor of actors) {
  test(actor.label + ' epoch read is scoped to its own QA family', async ({ page, request }) => {
    test.skip(!actor.username || !actor.password, 'QA credentials are required in local environment')
    await page.goto('/login')
    await page.locator('form input[type="text"]').fill(actor.username)
    await page.locator('form input[type="password"]').fill(actor.password)
    await page.locator('form button[type="submit"]').click()
    await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
    if (new URL(page.url()).pathname === '/choose-family') {
      throw new Error('Select the QA family manually; no automatic selection.')
    }
    const token = await page.evaluate(() => localStorage.getItem('accessToken'))
    expect(token).toBeTruthy()
    const headers = { Authorization: 'Bearer ' + token }
    const ownResponse = await request.get(origin + '/workspaces/' + actor.own + '/data-epoch',
      { headers, failOnStatusCode: false, timeout: 12000 })
    expect(ownResponse.status(), actor.label + ' own epoch endpoint').toBe(200)
    const ownBody = await ownResponse.json()
    expect(String(ownBody.workspaceId)).toBe(actor.own)
    expect(Number.isSafeInteger(ownBody.dataEpoch)).toBe(true)
    expect(ownBody.dataEpoch).toBeGreaterThanOrEqual(0)

    const foreignResponse = await request.get(origin + '/workspaces/' + actor.foreign + '/data-epoch',
      { headers, failOnStatusCode: false, timeout: 12000 })
    console.log(actor.label + ' foreign epoch status: ' + foreignResponse.status())
    expect([401, 403, 404], 'Other family epoch must not be disclosed').toContain(foreignResponse.status())
  })
}
