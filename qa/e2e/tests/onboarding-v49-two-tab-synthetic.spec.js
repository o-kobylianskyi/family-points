import {readFileSync} from 'node:fs'
import {test, expect} from '@playwright/test'

// Playwright routing does not intercept requests handled by a Service Worker.
// Disable service workers in these mocked tests so workspace write guards
// and expected epoch/receipt routes cannot be bypassed by an active worker.
test.use({ serviceWorkers: 'block' })

// Only authentication uses QA. All scoped setup/epoch traffic is mocked.
// This file never sends setup mutations to a real workspace.
const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD
const catalog = JSON.parse(readFileSync(new URL('../../../src/main/resources/templates/catalog-v1.json', import.meta.url), 'utf8'))
const steps = Object.fromEntries(
  ['MEMBERS','MEMBER_GROUPS','WORK_NODE_GROUPS','CATALOG_SELECTION','TASK_REVIEW','REWARD_REVIEW','FINAL_REVIEW'].map(code => [code,'COMPLETE'])
)
const ui = {
  review:/Review selection|Переглянути вибране|Просмотреть выбранное|Auswahl prüfen/,
  finalize:/Finalize and import|Завершити й імпортувати|Завершить и импортировать|Abschließen und importieren/,
  receipt:/Check receipt|Перевірити квитанцію|Проверить квитанцию|Beleg prüfen/,
  unknown:/Previous result unknown|Результат попередньої операції невідомий|Результат предыдущей операции неизвестен|Vorheriger Vorgang unklar/,
  finished:/Import completed|Імпорт завершено|Импорт завершён|Import abgeschlossen/,
}


// Defense in depth: even if the UI later adds another workspace mutation,
// this synthetic fixture cannot write it to the selected QA workspace.
// The narrower setup route is registered later and takes precedence.
async function blockUnexpectedWorkspaceWrites(context, blocked) {
  await context.route(/\/workspaces\/\d+\//, route => {
    const request=route.request()
    if (['POST','PUT','PATCH','DELETE'].includes(request.method())) {
      blocked.push(request.method()+' '+new URL(request.url()).pathname)
      return route.abort('blockedbyclient')
    }
    return route.continue()
  })
}

async function signIn(page) {
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(username)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/,{timeout:12000})
  test.skip(new URL(page.url()).pathname === '/choose-family', 'Choose an isolated QA family before this test')
}

async function prepare(page) {
  await page.goto('/settings#initial-family-setup')
  test.skip(new URL(page.url()).pathname === '/login' || new URL(page.url()).pathname === '/choose-family',
    'The second tab needs an authenticated selected QA family')
  const section = page.locator('#initial-family-setup')
  await expect(section.getByText(/7\/7/)).toBeVisible()
  await section.locator('.initial-setup-template-list input[type="checkbox"]').first().check()
  await section.getByRole('button',{name:ui.review}).click()
  await expect(section.getByRole('button',{name:ui.finalize})).toBeVisible()
  await section.locator('label input[type="checkbox"]').last().check()
  return section
}

test('V49 two tabs: first lost finalize keeps original UUID; second tab never reposts', async ({page,context}) => {
  test.skip(!username || !password,'Disposable QA manager credentials required')
  await signIn(page)
  let finalizeCount=0, operation=null, receiptCount=0
  const receiptPaths=[]
  const unexpected=[]
  await blockUnexpectedWorkspaceWrites(context, unexpected)
  await context.route(/\/workspaces\/\d+\/(?:data-epoch|setup(?:\/.*)?)(?:\?.*)?$/, route => {
    const req=route.request(), path=new URL(req.url()).pathname
    const workspaceId=Number(path.match(/\/workspaces\/(\d+)\//)[1])
    if(path.endsWith('/data-epoch') && req.method()==='GET')
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({workspaceId,dataEpoch:0})})
    if(path.endsWith('/setup') && req.method()==='GET')
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({
        workspaceId,dataEpoch:0,revision:7,status:'IN_PROGRESS',steps
      })})
    if(path.endsWith('/setup/review') && req.method()==='POST') {
      const codes=req.postDataJSON().selectedCatalogCodes
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({
        workspaceId,dataEpoch:0,revision:7,setupStatus:'IN_PROGRESS',
        selectedCount:codes.length,toImportCount:codes.length,alreadyImportedCount:0,
        selectedCodes:codes,toImport:codes,alreadyImported:[],
        requiresExplicitConfirmation:true,createsActiveDefinitions:false
      })})
    }
    if(path.endsWith('/setup/finalize-import') && req.method()==='POST') {
      finalizeCount++
      operation={id:req.headers()['idempotency-key'],workspaceId,codes:req.postDataJSON().selectedCatalogCodes,
        revision:req.postDataJSON().expectedRevision,epochHeader:req.headers()['x-workspace-data-epoch']}
      return route.abort('failed')
    }
    if(path.includes('/setup/finalize-import/receipts/') && req.method()==='GET') {
      receiptCount++
      receiptPaths.push(path)
      return route.fulfill({status:404,contentType:'application/json',body:'{}'})
    }
    unexpected.push(req.method()+' '+path)
    return route.abort('blockedbyclient')
  })
  await context.route(/\/template-catalog(?:\?.*)?$/,route =>
    route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(catalog)}))
  const first=await prepare(page)
  const secondPage=await context.newPage()
  const second=await prepare(secondPage)
  await first.getByRole('button',{name:ui.finalize}).click()
  await expect(first.getByText(ui.unknown)).toBeVisible()
  await expect(second.getByText(ui.unknown)).toBeVisible()
  await expect(second.getByRole('button',{name:ui.finalize})).toHaveCount(0)
  expect(finalizeCount).toBe(1)
  expect(operation?.id).toMatch(/^[0-9a-f-]{36}$/i)
  expect(operation.revision).toBe(7)
  expect(operation.epochHeader).toBe('0')
  // Receipt lookup of the SAME UUID is safe. A missing receipt must not
  // grant either tab permission to produce a new operation.
  await second.getByRole('button',{name:ui.receipt}).click()
  await expect.poll(()=>receiptCount).toBe(1)
  expect(receiptPaths).toEqual([`/workspaces/${operation.workspaceId}/setup/finalize-import/receipts/${operation.id}`])
  await expect(second.getByText(ui.unknown)).toBeVisible()
  // A 404 receipt leaves both original markers intact. A second tab must
  // not silently turn an unknown finalize into a fresh operation.
  const originalMarkers=await page.evaluate(()=>{
    const key=Object.keys(localStorage).find(k=>k.startsWith('fp:setup:pending:') && k.endsWith(':shared'))
    if(!key) return null
    const sessionKey=key.slice(0,-':shared'.length)
    return {
      shared:JSON.parse(localStorage.getItem(key)),
      session:JSON.parse(sessionStorage.getItem(sessionKey)),
    }
  })
  expect(originalMarkers?.shared?.operationId).toBe(operation.id)
  expect(originalMarkers?.session?.operationId).toBe(operation.id)
  expect(finalizeCount).toBe(1)
  expect(unexpected).toEqual([])
  await secondPage.close()
})

test('V49 two tabs: competing shared marker survives original receipt cleanup',async({page,context})=>{
  test.skip(!username || !password,'Disposable QA manager credentials required')
  await signIn(page)
  let op=null, finalizeCount=0, receiptCount=0, injectionDone=false
  const receiptPaths=[]
  let sharedKey, competing, secondPage
  const blocked=[]
  await blockUnexpectedWorkspaceWrites(context, blocked)
  await context.route(/\/workspaces\/\d+\/(?:data-epoch|setup(?:\/.*)?)(?:\?.*)?$/,async route=>{
    const req=route.request(),path=new URL(req.url()).pathname
    const workspaceId=Number(path.match(/\/workspaces\/(\d+)\//)[1])
    if(path.endsWith('/data-epoch') && req.method()==='GET')
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({workspaceId,dataEpoch:0})})
    if(path.endsWith('/setup') && req.method()==='GET')
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({
        workspaceId,dataEpoch:0,revision:7,status:receiptCount?'COMPLETED':'IN_PROGRESS',
        steps
      ,...(receiptCount?{revision:8}:{})})})
    if(path.endsWith('/setup/review') && req.method()==='POST'){
      const codes=req.postDataJSON().selectedCatalogCodes
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({
        workspaceId,dataEpoch:0,revision:7,setupStatus:'IN_PROGRESS',
        selectedCount:codes.length,toImportCount:codes.length,alreadyImportedCount:0,
        selectedCodes:codes,toImport:codes,alreadyImported:[],
        requiresExplicitConfirmation:true,createsActiveDefinitions:false
      })})
    }
    if(path.endsWith('/setup/finalize-import') && req.method()==='POST'){
      finalizeCount++
      op={id:req.headers()['idempotency-key'],codes:req.postDataJSON().selectedCatalogCodes,workspaceId,
        revision:req.postDataJSON().expectedRevision,epochHeader:req.headers()['x-workspace-data-epoch']}
      return route.abort('failed')
    }
    if(path.includes('/setup/finalize-import/receipts/') && req.method()==='GET'){
      receiptCount++
      receiptPaths.push(path)
      // The foreign tab replaces the marker AFTER recovery begins but BEFORE
      // the receipt is returned. The original tab must not delete it.
      competing=await secondPage.evaluate(()=>{
        const key=Object.keys(localStorage).find(k=>k.startsWith('fp:setup:pending:') && k.endsWith(':shared'))
        if(!key) return null
        const original=JSON.parse(localStorage.getItem(key))
        const foreign={...original,operationId:'b489ea25-4e10-48ee-b8ea-7a9f4b2e3456'}
        localStorage.setItem(key,JSON.stringify(foreign))
        return {key,foreign}
      })
      sharedKey=competing?.key
      injectionDone=!!competing
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({
        workspaceId,operationId:op.id,replayed:true,dataEpoch:0,revision:8,
        setupStatus:'COMPLETED',
        importResult:{selectedCodes:op.codes,toImport:op.codes,alreadyImported:[]}
      })})
    }
    blocked.push(req.method()+' '+path)
    return route.abort('blockedbyclient')
  })
  await context.route(/\/template-catalog(?:\?.*)?$/,route =>
    route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(catalog)}))
  const first=await prepare(page)
  secondPage=await context.newPage()
  await secondPage.goto('/settings#initial-family-setup')
  await expect(secondPage.locator('#initial-family-setup').getByText(/7\/7/)).toBeVisible()
  await first.getByRole('button',{name:ui.finalize}).click()
  await expect(first.getByText(ui.unknown)).toBeVisible()
  expect(finalizeCount).toBe(1)
  expect(op?.revision).toBe(7)
  expect(op?.epochHeader).toBe('0')
  await first.getByRole('button',{name:ui.receipt}).click()
  await expect.poll(()=>injectionDone).toBe(true)
  await expect.poll(()=>receiptCount).toBe(1)
  expect(op?.id).toMatch(/^[0-9a-f-]{36}$/i)
  expect(receiptPaths).toEqual([`/workspaces/${op.workspaceId}/setup/finalize-import/receipts/${op.id}`])
  await expect(first.getByText(ui.unknown)).toBeVisible()
  await expect(first.getByText(ui.finished)).toHaveCount(0)
  const actual=await page.evaluate(key=>JSON.parse(localStorage.getItem(key)),sharedKey)
  expect(actual).toEqual(competing.foreign)
  // The first tab's session-local marker is still its own original request,
  // while the shared marker belongs to the competing operation.
  const local=await page.evaluate(key=>{
    const sessionKey=key.slice(0,-':shared'.length)
    const value=sessionStorage.getItem(sessionKey)
    return value ? JSON.parse(value) : null
  },sharedKey)
  expect(local?.operationId).toBe(op.id)
  expect(local?.operationId).not.toBe(competing.foreign.operationId)
  expect(finalizeCount).toBe(1)
  expect(blocked).toEqual([])
  await secondPage.close()
})
