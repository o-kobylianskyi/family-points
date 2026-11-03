import { test, expect } from '@playwright/test'

const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD
const historyPath = /\/workspaces\/\d+\/rewards\/history(?:\?.*)?$/
const historyTab = /^(Reward history|Історія нагород|История наград|Belohnungsverlauf)$/
const moreLabel = /^(Load more|Показати ще|Показать ещё|Mehr laden)$/

const purchase = (id) => ({
  id, rewardDefinitionId:71, memberId:1, memberName:'QA member',
  rewardTitle:'QA reward '+id, pointTypeId:1, pointTypeCode:'PTS',
  priceAmount:5, durationMinutes:null, status:'READY',
  purchasedAt:'2026-11-01T09:00:00', resolvedAt:null,
})

async function enterHistory(page) {
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(username)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/,{timeout:12000})
  test.skip(new URL(page.url()).pathname === '/choose-family','Isolated QA family must be selected manually')
  await page.goto('/rewards')
  const tab=page.getByRole('button',{name:historyTab})
  await expect(tab).toBeVisible({timeout:15000})
  await tab.click()
  await expect(page.locator('section.reward-history-panel')).toBeVisible()
}

function assertNoWrites(page) {
  const writes=[]
  page.on('request',req=>{
    const path=new URL(req.url()).pathname
    if(req.method()!=='GET' && /\/workspaces\/\d+\/rewards(?:\/|$)/.test(path))
      writes.push(req.method()+' '+path)
  })
  return writes
}

test.describe('P1 purchase-only reward history v1: synthetic read-only acceptance',()=>{
  test.skip(!username || !password,'Requires isolated QA credentials; no real purchases performed')

  test('26 purchase rows paginate; overlaps deduplicated; no purchase POST',async({page})=>{
    const writes=assertNoWrites(page)
    let initialCalls=0, cursorCalls=0
    await page.route(historyPath,route=>{
      const url=new URL(route.request().url())
      const cursor=url.searchParams.get('cursor')
      if (cursor) cursorCalls++
      else initialCalls++
      const rows=cursor?[purchase(25),purchase(26)]:Array.from({length:25},(_,i)=>purchase(i+1))
      return route.fulfill({status:200,contentType:'application/json',
        body:JSON.stringify({items:rows,nextCursor:cursor?null:'qa-cursor-page-2',hasMore:!cursor})})
    })
    await enterHistory(page)
    const panel=page.locator('section.reward-history-panel')
    await expect(panel.locator('article.reward-history-item')).toHaveCount(25)
    await panel.getByRole('button',{name:moreLabel}).click()
    await expect.poll(()=>cursorCalls, {message:'Load more must issue one cursor GET'}).toBe(1)
    try {
      await expect(panel.locator('article.reward-history-item')).toHaveCount(26)
    } catch (error) {
      // Only expose synthetic fixture labels/counts; never dump URLs, headers, storage or credentials.
      const count=await panel.locator('article.reward-history-item').count()
      const has26=await panel.getByText('QA reward 26').count()
      const alerts=await panel.getByRole('alert').allTextContents()
      const loadMore=await panel.getByRole('button',{name:moreLabel}).count()
      throw new Error(`RH-01: initialGET=${initialCalls}, cursorGET=${cursorCalls}, DOM=${count}, row26=${has26}, alerts=${JSON.stringify(alerts)}, loadMoreButton=${loadMore}`, {cause:error})
    }
    await expect(panel.getByText('QA reward 26')).toBeVisible()
    await expect(panel.locator('article.reward-history-item').filter({hasText:'QA reward 25'})).toHaveCount(1)
    expect(initialCalls).toBeGreaterThanOrEqual(1)
    expect(cursorCalls).toBe(1)
    expect(writes).toEqual([])
  })

  test('invalid history page fails closed and retry recovers without mutations',async({page})=>{
    const writes=assertNoWrites(page)
    let calls=0, retryRequested=false
    await page.route(historyPath,route=>{
      calls++
      // Initial mount may legitimately issue more than one GET (React StrictMode).
      // Keep every pre-retry response malformed; only the explicit Retry can recover.
      return route.fulfill({status:200,contentType:'application/json',
        body:JSON.stringify(retryRequested?
          {items:[purchase(3)],nextCursor:null,hasMore:false}:
          {items:[purchase(1)],nextCursor:null,hasMore:true})})
    })
    await enterHistory(page)
    const panel=page.locator('section.reward-history-panel')
    await expect(panel.getByRole('alert')).toBeVisible()
    await expect(panel.locator('article.reward-history-item')).toHaveCount(0)
    retryRequested=true
    await panel.getByRole('button',{name:/^(Retry|Повторити завантаження|Повторить загрузку|Erneut laden)$/}).click()
    await expect(panel.getByText('QA reward 3')).toBeVisible()
    expect(calls).toBeGreaterThanOrEqual(2)
    expect(writes).toEqual([])
  })

  test('HTTP 503 produces a visible read-only error; no invented receipt',async({page})=>{
    const writes=assertNoWrites(page)
    await page.route(historyPath,route=>route.fulfill({status:503,contentType:'application/json',body:'{}'}))
    await enterHistory(page)
    const panel=page.locator('section.reward-history-panel')
    await expect(panel.getByRole('alert')).toBeVisible()
    await expect(panel.locator('article.reward-history-item')).toHaveCount(0)
    expect(writes).toEqual([])
  })
})
