import { test, expect } from '@playwright/test'

const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD
const nextName = /^(Next|Далі|Далее|Weiter)$/
const previewName = /^(Preview import|Перевірити імпорт|Проверить импорт|Import prüfen)$/
const writes = /\/workspaces\/\d+\/(?:templates\/imports|member-groups|data-reset\/(?:execute|confirm|challenge)|protective-backups)(?:\/|$)/

async function managerLogin(page) {
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(username)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, {timeout: 12000})
  test.skip(new URL(page.url()).pathname === '/choose-family', 'Requires selected isolated QA family')
}

function monitorWrites(page) {
  const calls=[]
  page.on('request', request => {
    if (request.method() !== 'GET' && writes.test(new URL(request.url()).pathname))
      calls.push(request.method()+' '+new URL(request.url()).pathname)
  })
  return calls
}

test.describe('Issue18 onboarding non-destructive acceptance', () => {
  test.skip(!username || !password, 'Requires isolated QA manager login')

  test('opening onboarding and navigating to templates never commits or activates', async ({page}) => {
    await managerLogin(page)
    const calls=monitorWrites(page)
    await page.goto('/family-setup')
    await expect(page.locator('main.family-setup h1')).toBeVisible()
    await page.getByRole('button',{name:nextName}).click()
    await expect(page.locator('main.family-setup input[type="search"]')).toBeVisible()
    await expect(page.getByRole('button',{name:previewName})).toBeDisabled()
    await page.reload()
    await expect(page.locator('main.family-setup h1')).toBeVisible()
    expect(calls).toEqual([])
  })

  test('invalid catalog response fails closed before import even if user navigates', async ({page}) => {
    await managerLogin(page)
    const calls=monitorWrites(page)
    await page.route(/\/template-catalog(?:\?.*)?$/, route =>
      route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({templates:null,packs:[]})}))
    await page.goto('/family-setup')
    await expect(page.locator('main.family-setup [role="alert"]').first()).toBeVisible()
    await page.getByRole('button',{name:nextName}).click()
    await expect(page.getByRole('button',{name:previewName})).toBeDisabled()
    expect(calls).toEqual([])
  })

  test('unavailable group GET does not authorize group writes or automatic template import', async ({page}) => {
    await managerLogin(page)
    const calls=monitorWrites(page)
    await page.route(/\/workspaces\/\d+\/member-groups(?:\?.*)?$/, route =>
      route.fulfill({status:503,contentType:'application/json',body:'{}'}))
    await page.goto('/family-setup')
    await expect(page.locator('main.family-setup h1')).toBeVisible()
    await expect(page.getByText(/Groups are unavailable|Групи зараз недоступні|Группы сейчас недоступны|Gruppen sind derzeit nicht verfügbar/i)).toBeVisible()
    await expect(page.getByRole('button',{name:/^(Create group|Створити групу|Создать группу|Gruppe erstellen)$/})).toHaveCount(0)
    await page.getByRole('button',{name:nextName}).click()
    await expect(page.locator('main.family-setup input[type="search"]')).toBeVisible()
    expect(calls).toEqual([])
  })
})
