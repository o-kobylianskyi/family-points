import { backupRow, openRestorePlanning } from './family-data-qa-helpers.js'
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
    // A foreign-family row must reject the entire response. That negative
    // case is covered by backup-detail-readonly.spec.js; this fixture is valid.
    let loads = 0
    await page.route(registry, route => {
      loads++
      const id = Number(new URL(route.request().url()).pathname.match(/\/workspaces\/(\d+)\//)[1])
      return route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify([
          backupRow(id,'11111111-1111-4111-8111-111111111111',{selectedCategories:['PRIVATE_TEMPLATES']}),
          backupRow(id,'22222222-2222-4222-8222-222222222222',{state:'VERIFIED',resetMode:'FULL',restoreVerified:true,executable:true,selectedCategories:['TASK_DEFINITIONS']}),
          backupRow(id,'33333333-3333-4333-8333-333333333333',{state:'EXPIRED',resetMode:'FULL'}),
        ])
      })
    })
    await page.goto('/settings')
    const panel = page.locator('[aria-label="Family data safety planning"]')
    await expect(panel).toBeVisible()
    await expect(panel.getByText('11111111', { exact:false })).toBeVisible()
    await expect(panel.getByText('22222222', { exact:false })).toBeVisible()
    await expect(panel.getByText('33333333', { exact:false })).toBeVisible()
    // REQUESTED must never be presented as recoverable merely because it is a row.
    await expect(panel.locator('p').filter({ hasText: /Metadata only\. REQUESTED|Це лише реєстр станів|Это только реестр статусов|Nur Statusregister\. REQUESTED/ })).toHaveCount(1)
    await expect(panel.locator('strong').filter({hasText:/Статус реєстру: перевірено|Статус реестра: проверено|Registerstatus: verifiziert|Registry status: verified/})).toHaveCount(1)
    await panel.getByRole('button', {name:/Refresh list|Оновити список|Обновить список|Liste aktualisieren/}).click()
    await expect.poll(() => loads).toBeGreaterThanOrEqual(2)
    await openRestorePlanning(page)
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
  test('failed registry/detail reads recover after explicit retry without stale backup claims', async ({ page }) => {
    await login(page)
    const writes = []
    page.on('request', request => {
      const path = new URL(request.url()).pathname
      if (!['GET','HEAD','OPTIONS'].includes(request.method()) &&
          /\/workspaces\/\d+\/(?:protective-backups|data-reset|restore)(?:\/|$)/.test(path))
        writes.push(request.method() + ' ' + path)
    })
    const snapshot = '44444444-4444-4444-8444-444444444444'
    let registryReads = 0
    await page.route(registry, route => {
      registryReads++
      if (registryReads === 1) return route.fulfill({status:503,contentType:'application/json',body:'{}'})
      const id = Number(new URL(route.request().url()).pathname.match(/\/workspaces\/(\d+)\//)[1])
      return route.fulfill({
        status:200,contentType:'application/json',
        body:JSON.stringify([backupRow(id,snapshot,{state:'VERIFIED',restoreVerified:true,executable:true,selectedCategories:['PRIVATE_TEMPLATES']})])
      })
    })
    let detailReads = 0
    await page.route(/\/workspaces\/\d+\/protective-backups\/44444444-4444-4444-8444-444444444444(?:\?.*)?$/, route => {
      detailReads++
      if (detailReads === 1) return route.fulfill({status:503,contentType:'application/json',body:'{}'})
      const id = Number(new URL(route.request().url()).pathname.match(/\/workspaces\/(\d+)\//)[1])
      return route.fulfill({status:200,contentType:'application/json',
        body:JSON.stringify(backupRow(id,snapshot,{state:'VERIFIED',restoreVerified:true,executable:true,selectedCategories:['PRIVATE_TEMPLATES']}))
      })
    })
    await page.goto('/settings')
    const panel = page.locator('[aria-label="Family data safety planning"]')
    await expect(panel.getByRole('alert')).toBeVisible()
    await expect(panel.getByText('44444444', {exact:false})).toHaveCount(0)
    await panel.getByRole('button',{name:/Refresh list|Оновити список|Обновить список|Liste aktualisieren/}).click()
    await expect(panel.getByRole('alert')).toHaveCount(0)
    await expect(panel.getByText('44444444', {exact:false})).toBeVisible()
    await panel.getByRole('button',{name:/View metadata|Переглянути запис|Просмотреть запись|Eintrag ansehen/}).click()
    await expect(panel.getByRole('alert')).toBeVisible()
    await expect(panel.getByText(/This record does not prove recoverability|Цей запис не підтверджує можливість відновлення|Запись не подтверждает возможность восстановления|Dieser Eintrag bestätigt keine Wiederherstellbarkeit/)).toHaveCount(0)
    await panel.getByRole('button',{name:/Refresh details|Оновити деталі|Обновить детали|Details aktualisieren/}).click()
    await expect(panel.getByRole('alert')).toHaveCount(0)
    await expect(panel.getByText(/This record does not prove recoverability|Цей запис не підтверджує можливість відновлення|Запись не подтверждает возможность восстановления|Dieser Eintrag bestätigt keine Wiederherstellbarkeit/)).toBeVisible()
    expect(registryReads).toBe(2)
    expect(detailReads).toBe(2)
    expect(writes).toEqual([])
  })

  test('stale detail response cannot appear under a different selected backup', async ({ page }) => {
    await login(page)
    const writes = []
    page.on('request', request => {
      const path = new URL(request.url()).pathname
      if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method()) &&
          /\/workspaces\/\d+\/(?:protective-backups|data-reset|restores)(?:\/|$)/.test(path)) {
        writes.push(request.method() + ' ' + path)
      }
    })
    const first = '55555555-5555-4555-8555-555555555555'
    const second = '66666666-6666-4666-8666-666666666666'
    await page.route(registry, route => {
      const workspaceId = Number(new URL(route.request().url()).pathname.match(/\/workspaces\/(\d+)\//)[1])
      return route.fulfill({status: 200, contentType: 'application/json',
        body: JSON.stringify([backupRow(workspaceId, first), backupRow(workspaceId, second)])})
    })
    let releaseFirst
    const firstRequest = new Promise(resolve => {
      releaseFirst = resolve
    })
    let firstStarted
    const firstArrived = new Promise(resolve => { firstStarted = resolve })
    await page.route(/\/workspaces\/\d+\/protective-backups\/(?:55555555|66666666)-/, async route => {
      const url = new URL(route.request().url())
      const workspaceId = Number(url.pathname.match(/\/workspaces\/(\d+)\//)[1])
      const id = url.pathname.split('/').at(-1)
      if (id === first) {
        firstStarted()
        await firstRequest
      }
      await route.fulfill({status: 200, contentType: 'application/json',
        body: JSON.stringify(backupRow(workspaceId, id, {selectedCategories: [id === first ? 'TASK_DEFINITIONS' : 'PRIVATE_TEMPLATES']}))})
    })
    await page.goto('/settings')
    const panel = page.locator('[aria-label="Family data safety planning"]')
    await expect(panel.getByText('55555555', {exact:false})).toBeVisible()
    await expect(panel.getByText('66666666', {exact:false})).toBeVisible()
    // Prefer the per-row buttons in DOM order: list order comes from the mocked server.
    const viewButtons = panel.getByRole('button', {name:/View metadata|Переглянути запис|Просмотреть запись|Eintrag ansehen/})
    await viewButtons.nth(0).click()
    await firstArrived
    await viewButtons.nth(1).click()
    const secondDetail = panel.locator('div[role="region"]').last()
    await expect(secondDetail.getByText('PRIVATE_TEMPLATES')).toBeVisible()
    const lateResponse = page.waitForResponse(response =>
      response.url().includes('/protective-backups/' + first) && response.status() === 200)
    releaseFirst()
    await lateResponse
    await expect(secondDetail.getByText('PRIVATE_TEMPLATES')).toBeVisible()
    await expect(secondDetail.getByText('TASK_DEFINITIONS')).toHaveCount(0)
    expect(writes).toEqual([])
  })

})
