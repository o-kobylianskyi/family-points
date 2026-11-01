import { test, expect } from '@playwright/test'

// Non-mutating acceptance for the new Task/Reward lost-response protections.
// These smoke cases DO NOT submit a valid form: a real POST-loss test must
// intercept all create endpoints and explicitly simulate the lost reply.
const username = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const password = process.env.FP_QA_PASSWORD
const createPath = /^\/workspaces\/\d+\/(?:tasks\/definitions|rewards)$/

async function login(page) {
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(username)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/,{timeout:12000})
  test.skip(new URL(page.url()).pathname === '/choose-family', 'Select an isolated QA family before running')
}

function captureCreatePosts(page) {
  const sent=[]
  page.on('request',req=>{
    if(req.method()==='POST' && createPath.test(new URL(req.url()).pathname)) sent.push(req.url())
  })
  return sent
}

test.describe('Issue18 Task/Reward creation opens without implicit writes',()=>{
  test.skip(!username || !password, 'Requires isolated QA manager credentials')

  test('task create deep link never auto-submits a task definition',async({page})=>{
    await login(page)
    const posts=captureCreatePosts(page)
    await page.goto('/tasks?create=1')
    await expect(page).not.toHaveURL(/\/login(?:\?|$)/)
    await expect(page.locator('body')).toBeVisible()
    await page.reload()
    await expect(page).not.toHaveURL(/\/login(?:\?|$)/)
    expect(posts,'task creation is never implicit on navigation or reload').toEqual([])
  })

  test('reward management navigation never auto-submits reward creation',async({page})=>{
    await login(page)
    const posts=captureCreatePosts(page)
    await page.goto('/rewards')
    await expect(page).not.toHaveURL(/\/login(?:\?|$)/)
    await page.reload()
    await expect(page).not.toHaveURL(/\/login(?:\?|$)/)
    expect(posts,'reward creation is never implicit on navigation or reload').toEqual([])
  })
})
