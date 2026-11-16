import {readFileSync} from 'node:fs'
import {test,expect} from '@playwright/test'

// Browser fixture: only login uses real QA; all setup API requests intercepted.
// No implicit step or finalize write is allowed.
const user=process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password=process.env.FP_QA_PASSWORD
const catalog=JSON.parse(readFileSync(new URL('../../../src/main/resources/templates/catalog-v1.json',import.meta.url),'utf8'))
const retryText=/Retry loading|Повторити завантаження|Повторить загрузку|Erneut laden/
const unavailableText=/Setup is unavailable or disabled|Налаштування недоступне|Настройка недоступна|Einrichtung ist nicht verfügbar/
const completeText=/Mark complete|Позначити виконаним|Отметить выполненным|Als erledigt markieren/

test('V49 failed GET bootstrap retry is GET-only and restores empty seven-step wizard',async({page})=>{
  test.skip(!user||!password,'Requires disposable QA manager credentials')
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(user)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/,{timeout:12000})
  test.skip(new URL(page.url()).pathname==='/choose-family','Select isolated QA family explicitly')
  let stateGets=0,catalogGets=0
  const unexpectedWrites=[]
  page.on('request',req=>{
    if(/\/workspaces\/\d+\/setup(?:\/|$)/.test(new URL(req.url()).pathname) &&
       !['GET','HEAD','OPTIONS'].includes(req.method()))
      unexpectedWrites.push(req.method()+' '+new URL(req.url()).pathname)
  })
  await page.route(/\/workspaces\/\d+\/setup(?:\?.*)?$/,route=>{
    if(route.request().method()!=='GET')return route.abort('blockedbyclient')
    stateGets++
    if(stateGets===1)return route.fulfill({status:503,contentType:'application/json',body:'{}'})
    const id=Number(new URL(route.request().url()).pathname.match(/\/workspaces\/(\d+)\/setup/)[1])
    return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({
      workspaceId:id,dataEpoch:0,revision:0,status:'NOT_STARTED',steps:{}
    })})
  })
  await page.route(/\/template-catalog(?:\?.*)?$/,route=>{
    if(route.request().method()!=='GET')return route.abort('blockedbyclient')
    catalogGets++
    return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(catalog)})
  })
  await page.goto('/settings#initial-family-setup')
  const section=page.locator('#initial-family-setup')
  await expect(section.getByText(unavailableText)).toBeVisible()
  await expect(section.getByRole('button',{name:completeText})).toHaveCount(0)
  await section.getByRole('button',{name:retryText}).click()
  await expect(section.getByRole('button',{name:completeText})).toHaveCount(7)
  await expect(section.getByText(/0\/7/)).toBeVisible()
  await expect(section.getByText(unavailableText)).toHaveCount(0)
  expect(stateGets).toBe(2)
  expect(catalogGets).toBe(2)
  expect(unexpectedWrites).toEqual([])
})
