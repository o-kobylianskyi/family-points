import { test, expect } from '@playwright/test'

// Non-destructive V49 acceptance: never submit a finalize, step PATCH,
// group creation, task activation, reward creation, ledger write or reset.
// Missing disposable credentials / disabled V49 are SKIPPED, not PASS.
const managerName = process.env.FP_QA_USERNAME || process.env.FP_QA_EMAIL
const managerPassword = process.env.FP_QA_PASSWORD
const childName = process.env.FP_QA_CHILD_USERNAME
const childPassword = process.env.FP_QA_CHILD_PASSWORD
const forbidden = /\/workspaces\/\d+\/(?:setup\/(?:finalize-import|steps)|templates\/(?:imports\/idempotent|\d+\/activate-(?:task|reward))|member-groups|work-node-groups|rewards|tasks\/definitions|data-reset|protective-backups|points)(?:\/|$)/

async function signIn(page, name, password) {
  await page.goto('/login')
  await page.locator('form input[type="text"]').fill(name)
  await page.locator('form input[type="password"]').fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, {timeout:12000})
  test.skip(new URL(page.url()).pathname === '/choose-family', 'Select disposable QA workspace explicitly')
}

function captureWrites(page) {
  const requests = []
  page.on('request', request => {
    if (!['GET','HEAD','OPTIONS'].includes(request.method()) &&
        forbidden.test(new URL(request.url()).pathname)) {
      requests.push({method:request.method(),path:new URL(request.url()).pathname})
    }
  })
  return requests
}

test.describe('Issue #18 V49 read-only seven-step acceptance', () => {
  test('manager navigation and reload do not finalize, activate or change the ledger', async ({page}) => {
    test.skip(!managerName || !managerPassword, 'Requires disposable QA manager credentials')
    await signIn(page, managerName, managerPassword)
    const writes = captureWrites(page)
    await page.goto('/family-setup')
    await expect(page.locator('main.family-setup h1')).toBeVisible()
    await page.getByRole('link', {name:/V49/}).click()
    await expect(page).toHaveURL(/\/settings#initial-family-setup$/)
    await expect(page.locator('#initial-family-setup')).toBeVisible()
    await page.reload()
    await expect(page.locator('#initial-family-setup')).toBeVisible()
    expect(writes, 'navigation must never perform onboarding or economic mutations').toEqual([])
  })

  test('seven step server status is visible only when backend enables V49', async ({page}) => {
    test.skip(!managerName || !managerPassword, 'Requires disposable QA manager credentials')
    await signIn(page, managerName, managerPassword)
    const writes = captureWrites(page)
    await page.goto('/settings#initial-family-setup')
    const section = page.locator('#initial-family-setup')
    await expect(section).toBeVisible()
    const status = section.getByText(/Setup steps|Кроки налаштування|Шаги настройки|Einrichtungsschritte/)
    // The feature is disabled by default; disabled state is not a successful V49 acceptance.
    const visible = await status.isVisible().catch(() => false)
    test.skip(!visible, 'V49 disabled/unavailable by Backend: server-side steps cannot be accepted')
    await expect(section.getByRole('button',{name:/Mark complete|Позначити виконаним|Отметить выполненным|Als erledigt markieren/})).toHaveCount(7)
    expect(writes).toEqual([])
  })

  test('child cannot see manager-only setup or make protected writes', async ({page}) => {
    test.skip(!childName || !childPassword, 'Requires disposable QA child credentials')
    await signIn(page, childName, childPassword)
    const writes = captureWrites(page)
    await page.goto('/family-setup')
    await expect(page.locator('main.family-setup [role="alert"]')).toBeVisible()
    await expect(page.locator('main.family-setup button')).toHaveCount(0)
    await page.goto('/settings')
    await expect(page.locator('#initial-family-setup')).toHaveCount(0)
    expect(writes, 'child navigation must perform no manager writes').toEqual([])
  })
})
