import {readFileSync} from 'node:fs'
import {test,expect} from '@playwright/test'

// Entire V49 setup PATCH is mocked; no step write reaches the real server.
const username=process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password=process.env.FP_QA_PASSWORD
const catalog=JSON.parse(readFileSync(new URL('../../../src/main/resources/templates/catalog-v1.json',import.meta.url),'utf8'))
const completed=/Mark complete|Позначити виконаним|Отметить выполненным|Als erledigt markieren/
const error=/Setup changed|Дані налаштування змінилися|Данные изменились|Einrichtungsdaten geändert|Could not review|Не вдалося перевірити|Не удалось проверить|Auswahl konnte nicht/

test('V49 stale-epoch step PATCH response and GET cannot advance wizard or replay write',async({page})=>{
  test.skip(!username || !password,'Requires disposable QA manager credentials')
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(username)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/,{timeout:12000})
  test.skip(new URL(page.url()).pathname==='/choose-family','Select disposable QA family explicitly')
  let patchCount=0,setupReads=0
  await page.route(/\/workspaces\/\d+\/setup(?:\/steps\/[^/?]+)?(?:\?.*)?$/,route=>{
    const req=route.request()
    const path=new URL(req.url()).pathname
    const id=Number(path.match(/\/workspaces\/(\d+)\//)[1])
    if(req.method()==='GET' && path.endsWith('/setup')){
      setupReads++
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({
        workspaceId:id,dataEpoch:setupReads>1?1:0,revision:setupReads>1?1:0,
        status:setupReads>1?'IN_PROGRESS':'NOT_STARTED',
        steps:setupReads>1?{MEMBERS:'COMPLETE'}:{}
      })})
    }
    if(req.method()==='PATCH' && path.endsWith('/steps/MEMBERS')){
      patchCount++
      const body=req.postDataJSON()
      expect(body).toMatchObject({expectedRevision:0,status:'COMPLETE'})
      // Simulate a stale response from another workspace generation.
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({
        workspaceId:id,dataEpoch:1,revision:1,status:'IN_PROGRESS',
        steps:{MEMBERS:'COMPLETE'}
      })})
    }
    return route.abort('blockedbyclient')
  })
  await page.route(/\/template-catalog(?:\?.*)?$/,route=>
    route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(catalog)}))
  await page.goto('/settings#initial-family-setup')
  const section=page.locator('#initial-family-setup')
  await expect(section.getByRole('button',{name:completed})).toHaveCount(7)
  await expect(section.getByText(/0\/7/)).toBeVisible()
  await section.getByRole('button',{name:completed}).first().click()
  await expect(section.getByRole('alert')).toContainText(error)
  await expect(section.getByText(/0\/7/)).toBeVisible()
  expect(setupReads,'response validation failure requires read-only reconciliation').toBeGreaterThan(1)
  expect(patchCount,'epoch change cannot replay the step mutation').toBe(1)
})
