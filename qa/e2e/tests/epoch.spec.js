import { test, expect } from '@playwright/test'

// UI-only epoch regression. Requires an explicitly isolated QA account/family.
// No reset, purchase, import, or database write is performed by these tests.
const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD

test.describe('authenticated workspace data-epoch browser protection', () => {
  test.skip(!username || !password, 'Provide isolated FP_QA_USERNAME and FP_QA_PASSWORD.')

  test('cross-tab epoch notification blocks only the active workspace UI', async ({ page, context }) => {
    const seen = []
    page.on('request', request => {
      const match = request.url().match(/\/workspaces\/(\d+)\/data-epoch(?:\?|$)/)
      if (match) seen.push(match[1])
    })

    await page.goto('/login')
    await page.locator('form input[type="text"]').fill(username)
    await page.locator('form input[type="password"]').fill(password)
    await page.locator('form button[type="submit"]').click()
    await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
    if (new URL(page.url()).pathname === '/choose-family') {
      throw new Error('A preselected disposable QA family is required. Never auto-select a real family.')
    }
    await expect.poll(() => seen.length, { timeout: 12000 }).toBeGreaterThan(0)
    const workspaceId = seen[0]
    await page.goto('/settings')
    const secondTab = await context.newPage()
    await secondTab.goto('/settings')

    // Unrelated tenant notices MUST NOT block this family's UI.
    const differentWorkspace = String(Number(workspaceId) + 1000000)
    await page.evaluate(id => {
      localStorage.setItem('fp:workspace-epoch-notice', JSON.stringify({
        workspaceId: id, previousEpoch: 0, dataEpoch: 1
      }))
    }, differentWorkspace)
    await expect(secondTab.getByRole('alert').filter({
      has: secondTab.getByRole('button', { name: /Reload|Перезавантажити|Перезагрузить|Neu laden/ })
    })).toHaveCount(0)

    // Simulates only the browser notification: no Backend epoch mutation.
    await page.evaluate(id => {
      localStorage.setItem('fp:workspace-epoch-notice', JSON.stringify({
        workspaceId: id, previousEpoch: 0, dataEpoch: 1
      }))
    }, workspaceId)
    const staleAlert = secondTab.getByRole('alert').filter({
      has: secondTab.getByRole('button', { name: /Reload|Перезавантажити|Перезагрузить|Neu laden/ })
    })
    await expect(staleAlert).toBeVisible()
    await expect(staleAlert.getByRole('button')).toBeEnabled()
    await secondTab.close()
  })
})
