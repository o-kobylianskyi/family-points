import { test, expect } from '@playwright/test'

const primary = {
  username: process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL,
  password: process.env.FP_QA_PASSWORD,
  ownId: process.env.FP_QA_OWN_WORKSPACE_ID || '14',
  foreignId: process.env.FP_QA_FOREIGN_WORKSPACE_ID,
}
const secondary = {
  username: process.env.FP_QA_TEST2_USERNAME || 'Test2',
  password: process.env.FP_QA_TEST2_PASSWORD,
  ownId: process.env.FP_QA_TEST2_WORKSPACE_ID,
  foreignId: primary.ownId,
}
const apiOrigin = process.env.FP_QA_API_URL || 'http://localhost:8080'
// API security filters may return 401 for a token lacking target-workspace scope.
// An authorized own-workspace control in the primary test distinguishes this from missing credentials.
const allowedDenial = [401, 403, 404]

async function login(page, actor) {
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(actor.username)
  await page.locator('form input[type="password"]').fill(actor.password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 12000 })
  if (new URL(page.url()).pathname === '/choose-family') {
    throw new Error('QA workspace must be explicitly selected; refusing automatic family selection.')
  }
  return page.evaluate(() => localStorage.getItem('accessToken'))
}

async function verifyDenied(request, token, workspaceId) {
  const headers = { Authorization: 'Bearer ' + token }
  const base = apiOrigin + '/workspaces/' + workspaceId + '/data-reset'
  const preview = await request.get(base + '/preview', { headers, failOnStatusCode: false })
  console.log('Foreign workspace preview status:', preview.status())
  expect(allowedDenial, 'Cross-family preview must reject access').toContain(preview.status())
  const plan = await request.post(base + '/plan', {
    headers, data: { categories: ['PRIVATE_TEMPLATES'] }, failOnStatusCode: false,
  })
  console.log('Foreign workspace plan status:', plan.status())
  expect(allowedDenial, 'Cross-family plan must reject access').toContain(plan.status())
}

test('Test cannot view or plan reset in Test2 family', async ({ page, request }) => {
  test.skip(!primary.username || !primary.password || !primary.foreignId,
    'Configure primary QA login and verified foreign QA workspace ID')
  expect(primary.ownId).toMatch(/^[1-9]\d*$/)
  expect(primary.foreignId).toMatch(/^[1-9]\d*$/)
  expect(primary.foreignId).not.toBe(primary.ownId)
  const token = await login(page, primary)
  expect(token).toBeTruthy()
  // Verify the user has an authorized preview in their own QA family first.
  const own = await request.get(apiOrigin + '/workspaces/' + primary.ownId + '/data-reset/preview', {
    headers: { Authorization: 'Bearer ' + token }, failOnStatusCode: false,
  })
  expect(own.status()).toBe(200)
  await verifyDenied(request, token, primary.foreignId)
})

test('Test2 child cannot access reset administration in either family', async ({ page, request }) => {
  test.skip(!secondary.password || !secondary.ownId,
    'Configure FP_QA_TEST2_PASSWORD and verified FP_QA_TEST2_WORKSPACE_ID')
  expect(secondary.ownId).toMatch(/^[1-9]\d*$/)
  expect(secondary.ownId).not.toBe(secondary.foreignId)
  const token = await login(page, secondary)
  expect(token).toBeTruthy()
  const headers = { Authorization: 'Bearer ' + token }
  // Child role must not be able to inspect administrative preview even in own family.
  const ownPreview = await request.get(apiOrigin + '/workspaces/' + secondary.ownId + '/data-reset/preview', {
    headers, failOnStatusCode: false,
  })
  console.log('Test2 own workspace reset preview status:', ownPreview.status())
  expect(allowedDenial).toContain(ownPreview.status())
  await verifyDenied(request, token, secondary.foreignId)
})
