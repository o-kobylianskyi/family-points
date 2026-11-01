import { backupRow } from './family-data-qa-helpers.js'
import { test, expect } from '@playwright/test'

const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD
const listPath = /\/workspaces\/\d+\/protective-backups\?/
const detailPath = /\/workspaces\/\d+\/protective-backups\/[0-9a-f-]{36}(?:\?.*)?$/
const snapshotId = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'

async function login(page) {
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(username)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/,{timeout:12000})
  if (new URL(page.url()).pathname === '/choose-family') throw Error('Explicit isolated family selection required')
}

test.describe('V50 backup metadata detail fail-closed', () => {
  test.skip(!username || !password, 'Requires isolated QA manager')
  test('valid scoped detail is read-only; mismatched workspace fails closed', async ({ page }) => {
    await login(page)
    const writes = []
    page.on('request', req => {
      if (req.method() !== 'GET' && /\/workspaces\/\d+\/(protective-backups|restores|data-reset\/(prepare|confirm|challenge|execute))/.test(new URL(req.url()).pathname)) writes.push(req.url())
    })
    let activeId
    await page.route(listPath, route => {
      activeId = Number(new URL(route.request().url()).pathname.match(/\/workspaces\/(\d+)\//)[1])
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify([{
        ...backupRow(activeId,snapshotId,{selectedCategories:['PRIVATE_TEMPLATES']})
      }])})
    })
    let detailCalls=0
    await page.route(detailPath, route => {
      detailCalls++
      const wrong = detailCalls > 1
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({
        ...backupRow(wrong ? activeId+1 : activeId,snapshotId,{selectedCategories:['PRIVATE_TEMPLATES']}),
      })})
    })
    await page.goto('/settings')
    const panel=page.locator('[aria-label="Family data safety planning"]')
    const toggle=panel.getByRole('button',{name:/View metadata|Переглянути запис|Просмотреть запись|Eintrag ansehen/}).first()
    await expect(toggle).toBeVisible()
    await toggle.click()
    await expect(panel.getByRole('region').getByText('PRIVATE_TEMPLATES')).toBeVisible()
    await expect(panel.getByRole('region').getByText(/does not prove recoverability|не підтверджує можливість відновлення|не подтверждает возможность восстановления|bestätigt keine Wiederherstellbarkeit/)).toBeVisible()
    await toggle.click()
    await toggle.click()
    await expect(panel.getByRole('region').getByRole('alert')).toBeVisible()
    expect(detailCalls).toBe(2)
    expect(writes).toEqual([])
  })

  test('foreign-family row poisons the entire list, never partially renders',async ({page})=>{
    await login(page)
    await page.route(listPath,route=>{
      const id=Number(new URL(route.request().url()).pathname.match(/\/workspaces\/(\d+)\//)[1])
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify([
        backupRow(id,snapshotId),
        backupRow(id+1,'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',{state:'VERIFIED',restoreVerified:true})
      ])})
    })
    await page.goto('/settings')
    const panel=page.locator('[aria-label="Family data safety planning"]')
    await expect(panel.getByRole('alert')).toBeVisible()
    await expect(panel.getByRole('button',{name:/View metadata|Переглянути запис|Просмотреть запись|Eintrag ansehen/})).toHaveCount(0)
    await expect(panel.locator('input[type="file"]')).toHaveCount(0)
  })
})
