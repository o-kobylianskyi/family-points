import { test, expect } from '@playwright/test'

// WEB02_SYNTHETIC_COMPONENT_RACES_01.
// A localhost-only, no-login, no-real-DEV fixture. It does NOT assert live
// account switching, real role revocation, or any deployed environment.
test.use({ serviceWorkers: 'block' })

const F1 = 101
const F2 = 202
const ID1 = '11111111-1111-4111-8111-111111111111'
const ID2 = '22222222-2222-4222-8222-222222222222'
const TOKEN = actor => {
  const payload = Buffer.from(JSON.stringify({ exp: 4102444800, actor })).toString('base64url')
  return 'qa.' + payload + '.synthetic'
}

const deferred = () => {
  let resolve
  const promise = new Promise(done => { resolve = done })
  return { promise, resolve }
}
const metadata = (workspaceId, snapshotId, state = 'REQUESTED') => ({
  workspaceId, snapshotId, state,
  selectedCategories: ['tasks'], resetMode: 'SELECTIVE',
  restoreVerified: false, executable: false,
  createdAt: '2026-01-01T00:00:00Z', expiresAt: '2027-01-01T00:00:00Z'
})
const user = (actor, family, manager = true) => ({
  accountId: actor === 'A' ? 11 : 22,
  userId: actor === 'A' ? 11 : 22,
  id: actor === 'A' ? 11 : 22,
  username: 'synthetic-' + actor,
  workspaceId: family,
  permissions: manager ? ['MANAGE_WORKSPACE'] : ['VIEW_WORKSPACE'],
  role: manager ? 'OWNER' : 'MEMBER'
})
const json = (route, body, status = 200) =>
  route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })

function assertLocalFixture() {
  const candidate = new URL(process.env.FP_QA_BASE_URL || 'http://localhost:5173')
  if (candidate.protocol !== 'http:' ||
      !['localhost', '127.0.0.1'].includes(candidate.hostname) ||
      candidate.port !== '5173' || candidate.pathname !== '/') {
    throw new Error('WEB02_SYNTHETIC_LOCALHOST_5173_ONLY')
  }
  return candidate.origin
}

// All routes are installed BEFORE navigation. Every API request is fulfilled
// locally, and any non-local destination or mutation is aborted and audited.
// Vite frontend assets are the ONLY requests allowed to reach a local server.
async function mockApp(page, { list, detail, familyByActor, managementByActor } = {}) {
  const origin = assertLocalFixture()
  const blocked = []
  const unexpected = []
  const mockList = list || (async ({family}) => [metadata(family, family === F1 ? ID1 : ID2)])
  const mockDetail = detail || (async ({family, id}) => metadata(family, id))
  await page.addInitScript(token => {
    if (!localStorage.getItem('accessToken')) localStorage.setItem('accessToken', token)
    localStorage.setItem('lastActivityAt', String(Date.now()))
  }, TOKEN('A'))
  await page.context().route('**/*', async route => {
    const request = route.request()
    const url = new URL(request.url())
    const method = request.method()
    if (method !== 'GET' && method !== 'HEAD') {
      unexpected.push(method + ' ' + url.pathname)
      return route.abort('blockedbyclient')
    }
    // Treat every API path as an API regardless of origin. On a Vite
    // localhost proxy, /api or /workspaces must NEVER reach a real backend.
    const isApiPath = /^\/(?:api|auth|workspaces)(?:\/|$)/.test(url.pathname)
    const isMockApiHost = url.protocol === 'http:' &&
      ['localhost', '127.0.0.1'].includes(url.hostname) &&
      url.port === '8080'
    if (url.origin === origin && !isApiPath && !/^\/healthz(?:\/|$)/.test(url.pathname)) {
      // Only the Vite frontend navigation/static asset GETs stay on localhost.
      // Reject unknown app paths rather than relying on a backend proxy.
      const isFrontendAsset = url.pathname === '/' ||
        ['/settings', '/tasks'].includes(url.pathname) ||
        /^\/(?:src|node_modules|@vite|@react-refresh|assets)(?:\/|$)/.test(url.pathname) ||
        /^\/(?:favicon\.ico|vite\.svg)$/.test(url.pathname)
      if (isFrontendAsset) return route.continue()
      blocked.push('UNRECOGNIZED_LOCAL_RESOURCE ' + url.pathname)
      return route.abort('blockedbyclient')
    }
    if (!(url.origin === origin && isApiPath) && !isMockApiHost) {
      blocked.push(method + ' ' + url.origin + url.pathname)
      return route.abort('blockedbyclient')
    }
    const token = (request.headers().authorization || '').replace(/^Bearer /i, '')
    const actor = token === TOKEN('B') ? 'B' : 'A'
    if (token && token !== TOKEN('A') && token !== TOKEN('B')) {
      blocked.push('BAD_TOKEN ' + url.pathname)
      return json(route, {}, 401)
    }
    const family = familyByActor?.[actor] ?? (actor === 'A' ? F1 : F2)
    const manager = managementByActor?.[actor] ?? true
    if (url.pathname === '/api/me/workspaces')
      return json(route, [{workspaceId: F1}, {workspaceId: F2}])
    if (url.pathname === '/api/me')
      return json(route, user(actor, family, manager))
    const epoch = /^\/workspaces\/(\d+)\/data-epoch$/.exec(url.pathname)
    if (epoch) return json(route, {workspaceId: Number(epoch[1]), dataEpoch: 0})
    const listMatch = /^\/workspaces\/(\d+)\/protective-backups$/.exec(url.pathname)
    if (listMatch) {
      const requested = Number(listMatch[1])
      return json(route, await mockList({actor, family: requested, request}))
    }
    const detailMatch = /^\/workspaces\/(\d+)\/protective-backups\/([0-9a-f-]+)$/i.exec(url.pathname)
    if (detailMatch) {
      const requested = Number(detailMatch[1])
      return json(route, await mockDetail({actor, family: requested, id: detailMatch[2], request}))
    }
    // Other settings widgets may perform reads. Give an explicit harmless
    // non-success rather than letting any unmapped endpoint hit a real server.
    return json(route, {error: 'SYNTHETIC_UNMAPPED_READ'}, 404)
  })
  return { blocked, unexpected }
}
const panel = page => page.locator('section[aria-label="Family data safety planning"]')
async function settings(page) {
  await page.goto('/settings')
  await expect(panel(page)).toBeVisible()
  await expect(panel(page).getByText(/Server backup records|Серверні резервні копії|Резервные копии на сервере|Server-Sicherungen/)).toBeVisible()
}
async function audit(a) {
  expect(a.unexpected, 'No real or mocked write request is permitted').toEqual([])
  expect(a.blocked, 'No non-local origin is permitted').toEqual([])
}

test('WEB02 synthetic: manager F1 can view only F1 list and detail', async ({page}) => {
  const a = await mockApp(page)
  await settings(page)
  await expect(panel(page).getByText(ID1.slice(0, 8), {exact: false})).toBeVisible()
  await panel(page).getByRole('button', {name: /View metadata|Переглянути запис|Просмотреть запись|Eintrag ansehen/}).first().click()
  await expect(panel(page).getByText(/Status: REQUESTED|Статус: REQUESTED|Статус реєстру|Registerstatus/).first()).toBeVisible()
  await expect(panel(page)).not.toContainText(ID2.slice(0, 8))
  await audit(a)
})

test('WEB02 synthetic: delayed F1 list cannot paint after settings unmount', async ({page}) => {
  const held = deferred()
  let hits = 0
  const a = await mockApp(page, {list: async ({family}) => {
    hits++
    if (hits === 1) return held.promise
    return [metadata(family, ID2)]
  }})
  try {
    await page.goto('/settings')
    await expect.poll(() => hits).toBeGreaterThan(0)
    await page.goto('/tasks')
    held.resolve([metadata(F1, ID1)])
    await settings(page)
    await expect(panel(page).getByText(ID2.slice(0, 8), {exact: false})).toBeVisible()
    await expect(panel(page)).not.toContainText(ID1.slice(0, 8))
    await audit(a)
  } finally { held.resolve([]) }
})

test('WEB02 synthetic: delayed detail after collapse does not resurface', async ({page}) => {
  const held = deferred()
  let detailHits = 0
  const a = await mockApp(page, {detail: async ({family,id}) => {
    detailHits++
    return detailHits === 1 ? held.promise : metadata(family,id)
  }})
  try {
    await settings(page)
    const button = panel(page).getByRole('button', {name: /View metadata|Переглянути запис|Просмотреть запись|Eintrag ansehen/}).first()
    await button.click()
    await expect.poll(() => detailHits).toBe(1)
    await button.click()
    held.resolve(metadata(F1, ID1, 'VERIFIED'))
    await expect(button).toHaveAttribute('aria-expanded', 'false')
    await expect(panel(page).locator('[role="region"]')).toHaveCount(0)
    await audit(a)
  } finally { held.resolve(metadata(F1, ID1)) }
})

test('WEB02 synthetic: foreign and duplicate snapshot metadata fail closed', async ({page}) => {
  let n = 0
  const a = await mockApp(page, {list: async () => {
    n++
    return n === 1
      ? [metadata(F2, ID2)]
      : [metadata(F1, ID1), metadata(F1, ID1)]
  }})
  await settings(page)
  await expect(panel(page).getByRole('alert')).toContainText(/Could not load|Не вдалося|Не удалось|konnte nicht/)
  await expect(panel(page)).not.toContainText(ID2.slice(0, 8))
  await panel(page).getByRole('button', {name: /Refresh list|Оновити список|Обновить список|Liste aktualisieren/}).click()
  await expect(panel(page).getByRole('alert')).toBeVisible()
  await expect(panel(page)).not.toContainText(ID1.slice(0, 8))
  await audit(a)
})

test('WEB02 synthetic: reversed list completion never displays superseded metadata', async ({page}) => {
  const first = deferred()
  let hits = 0
  const a = await mockApp(page, {list: async ({family}) => {
    hits++
    return hits === 1 ? first.promise : [metadata(family, ID2)]
  }})
  try {
    await page.goto('/settings')
    // React StrictMode may mount effects twice; the first is deliberately
    // pending, while subsequent reads may legitimately finish immediately.
    await expect.poll(() => hits).toBeGreaterThan(0)
    // Reload replaces the entire React component tree, rather than faking
    // an in-place account or role transition.
    await page.reload()
    await expect(panel(page).getByText(ID2.slice(0, 8), {exact:false})).toBeVisible()
    first.resolve([metadata(F1, ID1)])
    await expect(panel(page)).not.toContainText(ID1.slice(0, 8))
    await audit(a)
  } finally { first.resolve([]) }
})

test('WEB02 synthetic: full reload as account B cannot show account A metadata', async ({page}) => {
  const a = await mockApp(page)
  await settings(page)
  await expect(panel(page).getByText(ID1.slice(0, 8), {exact:false})).toBeVisible()
  await page.evaluate(token => localStorage.setItem('accessToken', token), TOKEN('B'))
  await page.reload()
  await expect(panel(page).getByText(ID2.slice(0, 8), {exact:false})).toBeVisible()
  await expect(panel(page)).not.toContainText(ID1.slice(0, 8))
  await audit(a)
})
