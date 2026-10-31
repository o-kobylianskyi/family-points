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
  test('stale cross-tab notice prevents scoped POST before network dispatch', async ({ page, context }) => {
    await page.goto('/login')
    await page.locator('form input[type="text"]').fill(username)
    await page.locator('form input[type="password"]').fill(password)
    await page.locator('form button[type="submit"]').click()
    await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
    if (new URL(page.url()).pathname === '/choose-family') {
      throw new Error('Select an isolated QA family manually; test will not choose one.')
    }
    const workspaceId = await page.evaluate(() => {
      const raw = localStorage.getItem('currentUser')
      try { return JSON.parse(raw)?.workspaceId } catch { return null }
    })
    // Use the authenticated epoch request as the canonical workspace source.
    const seen = []
    page.on('request', req => {
      const id = new URL(req.url()).pathname.match(/^\/workspaces\/(\d+)\/data-epoch$/)?.[1]
      if (id) seen.push(id)
    })
    await page.reload()
    await expect.poll(() => seen.length, { timeout: 12000 }).toBeGreaterThan(0)
    const id = seen[0]
    if (workspaceId) expect(String(workspaceId)).toBe(id)
    const secondTab = await context.newPage()
    await secondTab.goto('/settings')
    const sent = []
    secondTab.on('request', req => {
      if (req.method() === 'POST' && new URL(req.url()).pathname === '/workspaces/' + id + '/data-reset/plan') {
        sent.push(req.url())
      }
    })
    await page.evaluate(value => {
      localStorage.setItem('fp:workspace-epoch-notice', JSON.stringify({
        workspaceId: value, previousEpoch: 0, dataEpoch: 1
      }))
    }, id)
    await expect(secondTab.getByRole('alert').filter({
      has: secondTab.getByRole('button', { name: /Reload|Перезавантажити|Перезагрузить|Neu laden/ })
    })).toBeVisible()
    const result = await secondTab.evaluate(async value => {
      const { apiRequest } = await import('/src/api/client.js')
      const token = localStorage.getItem('accessToken')
      if (!token) return 'MISSING_TOKEN'
      try {
        // Must reject in the client before the HTTP POST. No reset plan is executed.
        await apiRequest('/workspaces/' + value + '/data-reset/plan', {
          token, method: 'POST', body: { categories: ['PRIVATE_TEMPLATES'] }
        })
        return 'UNEXPECTED_SUCCESS'
      } catch (error) { return error?.message || 'UNKNOWN_ERROR' }
    }, id)
    expect(result).toBe('WORKSPACE_DATA_EPOCH_CHANGED')
    expect(sent, 'Stale workspace must not dispatch scoped mutation').toEqual([])
    await secondTab.close()
  })

})
