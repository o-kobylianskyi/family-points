import { readFileSync } from 'node:fs'
import { test, expect } from '@playwright/test'

// All scoped setup writes are intercepted. No real family setup/import is run.
// These synthetic tests are NOT acceptance of real Backend-enabled V49.
const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD
const catalog = JSON.parse(readFileSync(new URL('../../../src/main/resources/templates/catalog-v1.json', import.meta.url), 'utf8'))
const stepCodes = ['MEMBERS','MEMBER_GROUPS','WORK_NODE_GROUPS','CATALOG_SELECTION','TASK_REVIEW','REWARD_REVIEW','FINAL_REVIEW']
const completeSteps = Object.fromEntries(stepCodes.map(code=>[code,'COMPLETE']))
const text = {
  review: /Review selection|Переглянути вибране|Просмотреть выбранное|Auswahl prüfen/,
  finalize: /Finalize and import|Завершити й імпортувати|Завершить и импортировать|Abschließen und importieren/,
  receipt: /Check receipt|Перевірити квитанцію|Проверить квитанцию|Beleg prüfen/,
  unknown: /Previous result unknown|Результат попередньої операції невідомий|Результат предыдущей операции неизвестен|Vorheriger Vorgang unklar/,
  finished: /Import completed|Імпорт завершено|Импорт завершён|Import abgeschlossen/,
}
async function login(page) {
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(username)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, {timeout:12000})
  test.skip(new URL(page.url()).pathname === '/choose-family','Select isolated QA workspace explicitly')
}
for (const scenario of ['stale','completed','wrong-revision','wrong-epoch']) test(`V49 lost finalize response, valid receipt, server state ${scenario}: original UUID only`,async({page})=>{
  test.skip(!username || !password,'Disposable QA manager credentials required')
  await login(page)
  let workspaceId
  let finalizeCount=0
  let receiptCount=0
  let setupCount=0
  let receiptObserved=false
  let operation
  let codes
  const blockedWrites=[]
  const revision=7
  // No setup mutating request escapes the browser; the real epoch GET is also mocked.
  await page.route(/\/workspaces\/\d+\/(?:data-epoch|setup(?:\/.*)?)(?:\?.*)?$/, async route=>{
    const request=route.request()
    const url=new URL(request.url())
    const path=url.pathname
    const match=path.match(/\/workspaces\/(\d+)\//)
    const id=Number(match[1])
    workspaceId ??= id
    if(path.endsWith('/data-epoch') && request.method()==='GET')
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({workspaceId:id,dataEpoch:0})})
    if(path.endsWith('/setup') && request.method()==='GET') {
      setupCount++
      const finalized=receiptObserved && scenario!=='stale'
      const serverRevision=finalized ? (scenario==='wrong-revision' ? revision+2 : revision+1) : revision
      const serverEpoch=finalized && scenario==='wrong-epoch' ? 1 : 0
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({
        workspaceId:id,dataEpoch:serverEpoch,revision:serverRevision,
        status:finalized?'COMPLETED':'IN_PROGRESS',steps:completeSteps
      })})
    }
    if(path.endsWith('/setup/review') && request.method()==='POST') {
      const body=request.postDataJSON()
      codes=body.selectedCatalogCodes
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({
        workspaceId:id,dataEpoch:0,revision,setupStatus:'IN_PROGRESS',
        selectedCount:codes.length,toImportCount:codes.length,alreadyImportedCount:0,
        selectedCodes:codes,toImport:codes,alreadyImported:[],
        requiresExplicitConfirmation:true,createsActiveDefinitions:false
      })})
    }
    if(path.endsWith('/setup/finalize-import') && request.method()==='POST') {
      finalizeCount++
      const body=request.postDataJSON()
      operation={id:request.headers()['idempotency-key'],body,workspaceId:id}
      // Server is assumed to have committed; browser never receives its response.
      return route.abort('failed')
    }
    if(path.includes('/setup/finalize-import/receipts/') && request.method()==='GET') {
      receiptCount++
      receiptObserved=true
      const idFromPath=path.split('/').pop()
      if(idFromPath!==operation?.id) return route.fulfill({status:404,body:'{}'})
      return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({
        operationId:operation.id,replayed:true,workspaceId:id,dataEpoch:0,revision:8,
        setupStatus:'COMPLETED',importResult:{selectedCodes:codes,toImport:codes,alreadyImported:[]}
      })})
    }
    blockedWrites.push(request.method()+' '+path)
    return route.abort('blockedbyclient')
  })
  await page.route(/\/template-catalog(?:\?.*)?$/,route=>
    route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(catalog)}))
  await page.goto('/settings#initial-family-setup')
  const section=page.locator('#initial-family-setup')
  await expect(section.getByText(/7\/7/)).toBeVisible()
  await section.locator('div[style*="max-height"] input[type="checkbox"]').first().check()
  await section.getByRole('button',{name:text.review}).click()
  await expect(section.getByRole('button',{name:text.finalize})).toBeVisible()
  await section.locator('label input[type="checkbox"]').last().check()
  await section.getByRole('button',{name:text.finalize}).click()
  await expect(section.getByText(text.unknown)).toBeVisible()
  expect(finalizeCount).toBe(1)
  expect(operation?.id).toMatch(/^[0-9a-f-]{36}$/i)
  expect(operation.body.expectedRevision).toBe(revision)
  expect(operation.body.selectedCatalogCodes).toEqual(codes)
  const initialSetupGets=setupCount
  await section.getByRole('button',{name:text.receipt}).click()
  await expect.poll(()=>receiptCount).toBe(1)
  await expect.poll(()=>setupCount).toBeGreaterThan(initialSetupGets)
  if(scenario!=='completed') {
    await expect(section.getByText(text.unknown)).toBeVisible()
    await expect(section.getByText(text.finished)).toHaveCount(0)
  } else {
    await expect(section.getByText(text.finished)).toBeVisible()
    await expect(section.getByText(text.unknown)).toHaveCount(0)
  }
  expect(finalizeCount,'receipt lookup must never replay finalize POST').toBe(1)
  expect(blockedWrites,'No unknown scoped setup writes should be attempted').toEqual([])
})
