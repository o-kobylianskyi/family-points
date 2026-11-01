import { openResetPlanning } from './family-data-qa-helpers.js'
import { test, expect } from '@playwright/test'

// Synthetic browser safety, no real epoch mutation or data writes.
// A storage notification acts only on browser memory. Every write must fail
// BEFORE any scoped POST/PUT/PATCH/DELETE can reach a server.
const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD
const mutations = ['POST', 'PUT', 'PATCH', 'DELETE']

test.describe('epoch quarantine: all scoped write verbs', () => {
  test.skip(!username || !password, 'Requires isolated QA login')

  test('stale notice blocks all HTTP write verbs before dispatch', async ({ page, context }) => {
    const epochRequests = []
    page.on('request', req => {
      const match = new URL(req.url()).pathname.match(/^\/workspaces\/(\d+)\/data-epoch$/)
      if (match) epochRequests.push(match[1])
    })
    await page.goto('/login')
    await page.locator('form input[type="text"]').fill(username)
    await page.locator('form input[type="password"]').fill(password)
    await page.locator('form button[type="submit"]').click()
    await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
    if (new URL(page.url()).pathname === '/choose-family') {
      throw new Error('Isolated QA family must be explicitly selected')
    }
    // Re-loading triggers the active workspace's epoch handshake.
    await page.reload()
    await expect.poll(() => epochRequests.length, { timeout: 12000 }).toBeGreaterThan(0)
    const familyId = epochRequests[0]
    const observer = await context.newPage()
    await observer.goto('/settings')
    await openResetPlanning(observer)
    await expect(observer.locator('.family-reset-readiness')).toBeVisible({ timeout: 12000 })

    const dispatched = []
    observer.on('request', req => {
      const pathname = new URL(req.url()).pathname
      if (mutations.includes(req.method()) && pathname === '/workspaces/' + familyId + '/data-reset/plan') {
        dispatched.push(req.method() + ' ' + pathname)
      }
    })
    await page.evaluate(id => {
      localStorage.setItem('fp:workspace-epoch-notice', JSON.stringify({
        workspaceId: id, previousEpoch: 0, dataEpoch: 1,
      }))
    }, familyId)
    await expect(observer.getByRole('alert').filter({
      has: observer.getByRole('button', { name: /Reload|Перезавантажити|Перезагрузить|Neu laden/ }),
    })).toBeVisible()

    for (const method of mutations) {
      const result = await observer.evaluate(async ({ id, verb }) => {
        const { apiRequest } = await import('/src/api/client.js')
        const token = localStorage.getItem('accessToken')
        if (!token) return 'MISSING_TOKEN'
        try {
          await apiRequest('/workspaces/' + id + '/data-reset/plan', {
            token, method: verb,
            ...(verb === 'DELETE' ? {} : { body: { categories: ['PRIVATE_TEMPLATES'] } }),
          })
          return 'UNEXPECTED_SUCCESS'
        } catch (e) {
          return e?.message || 'UNKNOWN_ERROR'
        }
      }, { id: familyId, verb: method })
      expect(result, method + ' must be quarantined').toBe('WORKSPACE_DATA_EPOCH_CHANGED')
    }
    expect(dispatched, 'No write verb may be transmitted while quarantined').toEqual([])
    await observer.close()
  })
})
