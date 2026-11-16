import {readFileSync} from 'node:fs'
import {test, expect} from '@playwright/test'

// An enabled V49 server fixture for UI-only contract verification.
// Does NOT enable V49 on the real backend or execute any setup step/finalize.
// Real enabled-backend acceptance remains a separate, explicitly skipped gate.
const manager = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD
const catalog = JSON.parse(readFileSync(new URL('../../../src/main/resources/templates/catalog-v1.json', import.meta.url),'utf8'))
const steps = ['MEMBERS','MEMBER_GROUPS','WORK_NODE_GROUPS','CATALOG_SELECTION','TASK_REVIEW','REWARD_REVIEW','FINAL_REVIEW']
const forbidden = /\/workspaces\/\d+\/(?:setup\/(?:steps|complete|finalize-import)|templates\/imports|data-reset|protective-backups|tasks\/definitions|rewards|points)(?:\/|$)/

async function signIn(page){
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(manager)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/,{timeout:12000})
  test.skip(new URL(page.url()).pathname === '/choose-family', 'Choose isolated QA workspace explicitly')
}

test('V49 mocked seven-step state and real 142/6 catalog: selection, search and reload are write-free',async({page})=>{
  test.skip(!manager || !password,'Disposable manager QA credentials required')
  await signIn(page)
  const writes=[]
  page.on('request',request=>{
    if(!['GET','HEAD','OPTIONS'].includes(request.method()) &&
        forbidden.test(new URL(request.url()).pathname))
      writes.push(request.method()+' '+new URL(request.url()).pathname)
  })
  let setupReads=0
  let catalogReads=0
  await page.route(/\/workspaces\/\d+\/setup(?:\?.*)?$/,route=>{
    if(route.request().method()!=='GET') return route.abort()
    setupReads++
    const id=Number(new URL(route.request().url()).pathname.match(/\/workspaces\/(\d+)\/setup/)[1])
    return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({
      workspaceId:id,dataEpoch:0,revision:0,status:'NOT_STARTED',
      steps:Object.fromEntries(steps.map(step=>[step,'PENDING'])),completedAt:null
    })})
  })
  await page.route(/\/template-catalog(?:\?.*)?$/,route=>{
    if(route.request().method()!=='GET') return route.abort()
    catalogReads++
    return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(catalog)})
  })
  await page.goto('/settings#initial-family-setup')
  const section=page.locator('#initial-family-setup')
  await expect(section).toBeVisible()
  await expect(section.getByRole('button',{name:/Mark complete|Позначити виконаним|Отметить выполненным|Als erledigt markieren/})).toHaveCount(7)
  const pack=section.locator('select').nth(1)
  await expect(pack.locator('option')).toHaveCount(7)
  await pack.selectOption({index:1})
  const choices=section.locator('div[style*="max-height"] input[type="checkbox"]')
  await expect(choices.first()).toBeVisible()
  await section.getByRole('button',{name:/Select all matching|Вибрати всі знайдені|Выбрать все найденные|Alle Treffer auswählen/}).click()
  await expect(choices.first()).toBeChecked()
  await section.getByRole('searchbox').fill('no-such-catalog-entry-unique-000')
  await expect(section.getByRole('button',{name:/Select all matching|Вибрати всі знайдені|Выбрать все найденные|Alle Treffer auswählen/})).toBeDisabled()
  await section.getByRole('searchbox').clear()
  await expect(choices.first()).toBeChecked()
  expect(setupReads).toBeGreaterThan(0)
  expect(catalogReads).toBeGreaterThan(0)
  expect(writes,'UI-only mock must not authorize or dispatch setup/ledger mutations').toEqual([])
})

test('V49 invalid server setup metadata fails closed without offering step writes',async({page})=>{
  test.skip(!manager || !password,'Disposable manager QA credentials required')
  await signIn(page)
  const writes=[]
  page.on('request',request=>{
    if(!['GET','HEAD','OPTIONS'].includes(request.method()) &&
        forbidden.test(new URL(request.url()).pathname))
      writes.push(request.method()+' '+new URL(request.url()).pathname)
  })
  await page.route(/\/workspaces\/\d+\/setup(?:\?.*)?$/,route=>{
    if(route.request().method()!=='GET') return route.abort()
    const id=Number(new URL(route.request().url()).pathname.match(/\/workspaces\/(\d+)\/setup/)[1])
    return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({
      workspaceId:id,dataEpoch:0,revision:0,status:'COMPLETED',steps:{MEMBERS:'COMPLETE'},
    })})
  })
  await page.route(/\/template-catalog(?:\?.*)?$/,route=>
    route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(catalog)}))
  await page.goto('/settings#initial-family-setup')
  const section=page.locator('#initial-family-setup')
  await expect(section).toBeVisible()
  await expect(section.getByText(/Setup is unavailable or disabled|Налаштування недоступне|Настройка недоступна|Einrichtung ist nicht verfügbar/)).toBeVisible()
  await expect(section.getByRole('button',{name:/Mark complete|Позначити виконаним|Отметить выполненным|Als erledigt markieren/})).toHaveCount(0)
  expect(writes).toEqual([])
})

test('V49 lost step PATCH response reconciles using GET only without hidden replay',async({page})=>{
  test.skip(!manager || !password,'Disposable QA manager credentials required')
  await signIn(page)
  let patchCount=0, setupGetCount=0
  let serverRevision=0
  // V46 creates a new family with EMPTY steps. The Web must present all seven
  // pending choices before any PATCH, then reconcile the lost response via GET.
  const updatedSteps={}
  await page.route(/\/workspaces\/\d+\/setup(?:\/steps\/[^/?]+)?(?:\?.*)?$/,route=>{
    const url=new URL(route.request().url())
    const path=url.pathname
    if(route.request().method()==='PATCH' && path.includes('/steps/')){
      patchCount++
      const step=path.split('/').pop()
      const body=route.request().postDataJSON()
      if(patchCount===1 && step==='MEMBERS' && body.expectedRevision===0){
        updatedSteps.MEMBERS='COMPLETE'
        serverRevision=1
      }
      // Simulate successful server commit but response never delivered.
      return route.abort('failed')
    }
    if(route.request().method()!=='GET' || !path.endsWith('/setup'))return route.abort()
    setupGetCount++
    const workspaceId=Number(path.match(/\/workspaces\/(\d+)\/setup/)[1])
    return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({
      workspaceId,dataEpoch:0,revision:serverRevision,
      status:serverRevision?'IN_PROGRESS':'NOT_STARTED',steps:updatedSteps,
    })})
  })
  await page.route(/\/template-catalog(?:\?.*)?$/,route=>
    route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(catalog)}))
  await page.goto('/settings#initial-family-setup')
  const section=page.locator('#initial-family-setup')
  await expect(section.getByRole('button',{name:/Mark complete|Позначити виконаним|Отметить выполненным|Als erledigt markieren/})).toHaveCount(7)
  const initialGetCount=setupGetCount
  await section.getByRole('button',{name:/Mark complete|Позначити виконаним|Отметить выполненным|Als erledigt markieren/}).first().click()
  await expect(section.getByText(/1\/7/)).toBeVisible()
  expect(patchCount,'lost response must never trigger a second PATCH').toBe(1)
  expect(setupGetCount,'unknown PATCH outcome must be reconciled by a fresh GET').toBeGreaterThan(initialGetCount)
})
