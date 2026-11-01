import { test, expect } from '@playwright/test'

const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD
const registry = /\/workspaces\/\d+\/protective-backups(?:\?.*)?$/
const forbidden = /\/workspaces\/\d+\/(?:data-reset\/(?:prepare|confirm|challenge|execute)|restores(?:\/|$)|backups(?:\/|$))/

async function login(page) {
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(username)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
  if (new URL(page.url()).pathname === '/choose-family') {
    throw new Error('Select an isolated QA family explicitly; test will not select one.')
  }
}

test.describe('V50 backup registry: metadata only, never executable', () => {
  test.skip(!username || !password, 'Requires isolated QA login')

  test('REQUESTED, VERIFIED and EXPIRED are differentiated without file actions', async ({ page }) => {
    await login(page)
    const writes = []
    page.on('request', req => {
      if (req.method() !== 'GET' && forbidden.test(new URL(req.url()).pathname)) writes.push(req.url())
    })
    let loads = 0
    await page.route(registry, route => {
      loads++
      const id = Number(new URL(route.request().url()).pathname.match(/\/workspaces\/(\d+)\//)[1])
      return route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify([
          { workspaceId:id,snapshotId:'11111111-1111-4111-8111-111111111111',state:'REQUESTED',resetMode:'SELECTIVE',restoreVerified:false,selectedCategories:['PRIVATE_TEMPLATES'] },
          { workspaceId:id,snapshotId:'22222222-2222-4222-8222-222222222222',state:'VERIFIED',resetMode:'FULL',restoreVerified:true,selectedCategories:['TASK_DEFINITIONS'] },
          { workspaceId:id,snapshotId:'33333333-3333-4333-8333-333333333333',state:'EXPIRED',resetMode:'FULL',restoreVerified:false,selectedCategories:[] },
          { workspaceId:id+1,snapshotId:'44444444-4444-4444-8444-444444444444',state:'VERIFIED',restoreVerified:true },
        ])
      })
    })
    await page.goto('/settings')
    const panel = page.locator('[aria-label="Family data safety planning"]')
    await expect(panel).toBeVisible()
    await expect(panel.getByText('11111111', { exact:false })).toBeVisible()
    await expect(panel.getByText('22222222', { exact:false })).toBeVisible()
    await expect(panel.getByText('33333333', { exact:false })).toBeVisible()
    await expect(panel.getByText('44444444', { exact:false })).toHaveCount(0)
    // REQUESTED must never be presented as recoverable merely because it is a row.
    await expect(panel.getByText(/Metadata only|лише реєстр|только реестр|Statusregister/)).toBeVisible()
    await expect(panel.getByText('Verified', { exact: true })).toHaveCount(1)
    await panel.getByRole('button', {name:/Refresh list|Оновити список|Обновить список|Liste aktualisieren/}).click()
    await expect.poll(() => loads).toBeGreaterThanOrEqual(2)
    await panel.getByRole('button', {name:/Restore from file|Відновлення з файлу|Восстановление из файла|Aus Datei wiederherstellen/}).click()
    await expect(panel.locator('input[type="file"]')).toHaveCount(0)
    expect(writes).toEqual([])
  })

  test('HTTP failure is visible and does not enable reset or restore', async ({ page }) => {
    await login(page)
    const writes = []
    page.on('request', req => {
      if (req.method() !== 'GET' && forbidden.test(new URL(req.url()).pathname)) writes.push(req.url())
    })
    await page.route(registry, route => route.fulfill({
      status:503,contentType:'application/json',body:JSON.stringify({code:'TEMPORARY_UNAVAILABLE'}),
    }))
    await page.goto('/settings')
    const panel = page.locator('[aria-label="Family data safety planning"]')
    await expect(panel.getByRole('alert')).toBeVisible({timeout:12000})
    await expect(panel.locator('input[type="file"]')).toHaveCount(0)
    expect(writes).toEqual([])
  })
})
