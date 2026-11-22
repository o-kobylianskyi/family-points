import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { currencyQuantity, standardCurrencyForms } from '../src/utils/formatCurrencyAmount.js'
import { applyTheme } from '../src/utils/theme.js'
import { rewardRecoveryCopy } from '../src/utils/rewardPurchaseRecoveryCopy.js'

const src = path => readFileSync(new URL('../src/' + path, import.meta.url), 'utf8')
const locales = Object.fromEntries(['uk', 'ru', 'de', 'en'].map(language =>
  [language, JSON.parse(src('i18n/locales/' + language + '.json'))]
))

test('main authenticated routes keep tasks, rewards, points, templates, calendar and settings reachable', () => {
  const app = src('App.jsx')
  for (const path of ['/tasks', '/rewards', '/points', '/templates', '/calendar', '/settings', '/members']) {
    assert.ok(app.includes('path="' + path + '"'), path)
  }
  for (const name of ['TasksPage', 'RewardsPage', 'PointsPage', 'CalendarPage']) {
    assert.ok(app.includes('import(\'./pages/' + name + '\')'), name)
  }
  assert.match(app, /<ProtectedRoute>/)
  assert.match(app, /<ThemeInitializer \/>/)
})

test('four locales have all main navigation and transactional flow keys', () => {
  const keys = [
    'navigation.dashboard', 'navigation.tasks', 'navigation.rewards',
    'navigation.points', 'navigation.settings', 'points.spend',
    'points.history', 'points.refreshAfterMutationFailed',
    'tasks.createButton', 'rewardsUi.catalog', 'rewardsUi.requests',
    'rewardsUi.received', 'common.cancel'
  ]
  for (const [language, dictionary] of Object.entries(locales)) {
    for (const key of keys) {
      const value = key.split('.').reduce((obj, part) => obj?.[part], dictionary)
      assert.equal(typeof value, 'string', language + ':' + key)
      assert.ok(value.trim(), language + ':' + key)
    }
    assert.ok(rewardRecoveryCopy(language).cancelled.length > 0)
  }
  const switcher = src('components/LanguageSwitcher.jsx')
  const initial = src('i18n/index.js')
  for (const language of Object.keys(locales)) {
    assert.match(switcher, new RegExp("code: '" + language + "'"))
    assert.ok(initial.includes("import " + language + " from"))
  }
})

test('currency quantities handle zero, one, slavic 2-4 / 11-14 / 21 and four locales', () => {
  const samples = [
    [0, 'uk', 'балів'], [1, 'uk', 'бал'], [2, 'uk', 'бали'],
    [4, 'uk', 'бали'], [5, 'uk', 'балів'], [11, 'uk', 'балів'],
    [14, 'uk', 'балів'], [21, 'uk', 'бал'], [22, 'uk', 'бали'],
    [1, 'ru', 'балл'], [3, 'ru', 'балла'], [12, 'ru', 'баллов'],
    [1, 'de', 'Punkt'], [2, 'de', 'Punkte'],
    [1, 'en', 'point'], [5, 'en', 'points']
  ]
  for (const [amount, language, ending] of samples) {
    assert.equal(currencyQuantity(amount, { code: 'POINTS', language }), amount + ' ' + ending)
  }
  for (const code of ['POINTS', 'COPPER', 'SILVER', 'GOLD']) {
    for (const language of Object.keys(locales)) {
      assert.ok(standardCurrencyForms[code][language].one)
      assert.ok(standardCurrencyForms[code][language].many)
    }
  }
  const custom = [{ language: 'de', one: 'Stern', few: 'Sterne', many: 'Sterne' }]
  assert.equal(currencyQuantity(2, { code: 'STAR', name: 'Star', forms: custom, language: 'de' }), '2 Sterne')
  assert.equal(currencyQuantity(0, { code: 'STAR', name: 'Star', forms: custom, language: 'de' }), '0 Sterne')
  assert.equal(currencyQuantity('bad', { code: 'POINTS' }), 'bad')
})

test('light/dark/system theme resolves actual DOM preference without network', () => {
  const previousWindow = globalThis.window
  const previousDocument = globalThis.document
  const html = { dataset: {} }
  let systemDark = false
  globalThis.window = { matchMedia: () => ({ matches: systemDark }) }
  globalThis.document = { documentElement: html }
  try {
    applyTheme('light')
    assert.equal(html.dataset.theme, 'light')
    applyTheme('dark')
    assert.equal(html.dataset.theme, 'dark')
    applyTheme('system')
    assert.equal(html.dataset.theme, 'light')
    systemDark = true
    applyTheme('system')
    assert.equal(html.dataset.theme, 'dark')
  } finally {
    if (previousWindow === undefined) delete globalThis.window
    else globalThis.window = previousWindow
    if (previousDocument === undefined) delete globalThis.document
    else globalThis.document = previousDocument
  }
})

test('responsive and both theme source tokens cover shell, mobile navigation and tab overflow', () => {
  const appCss = src('App.css')
  const navCss = src('Navigation.css')
  const layout = src('components/AppLayout.jsx')
  assert.match(appCss, /\[data-theme="dark"\]/)
  assert.match(appCss, /--fp-bg:/)
  assert.match(appCss, /--fp-surface:/)
  assert.match(appCss, /--fp-text:/)
  assert.match(appCss, /@media\s*\(max-width:\s*(?:560|600|700|760|800)px\)/)
  assert.match(navCss, /@media\s*\(max-width:\s*720px\)/)
  assert.match(navCss, /overflow-x:\s*auto/)
  assert.match(navCss, /flex-wrap:\s*wrap/)
  assert.match(layout, /mobileMenuOpen/)
  assert.match(layout, /setMobileMenuOpen/)
  assert.match(layout, /<Outlet\s*\/>/)
})

test('reward catalog cannot access old non-idempotent purchase or approved request charge', () => {
  const reward = src('pages/RewardsPage.jsx')
  const points = src('pages/PointsPage.jsx')
  const modal = src('components/PointOperationModal.jsx')
  assert.ok(!reward.includes('purchaseReward('))
  assert.ok(!reward.includes('purchaseRewardRequest('))
  assert.ok(!reward.includes('purchaseReward,'))
  assert.ok(!reward.includes('purchaseRewardRequest,'))
  assert.equal((reward.match(/disabled=\{true\}/g) || []).length, 3)
  assert.match(reward, /const purchaseOnce = async \(\) =>/)
  assert.match(reward, /rewardRecoveryCopy\(language\)\.blocked/)
  assert.ok(!points.includes('purchaseRewardForMember('))
  assert.ok(points.includes('rewardPurchaseEnabled={false}'))
  assert.match(modal, /rewardPurchaseEnabled && !!chosenReward/)
  assert.match(points, /case 'spend':\s+await spendPoints\(/)
  assert.match(points, /case 'penalty':\s+await penalizePoints\(/)
  // Request submission/approval are distinct operations; do not hide them
  // while a direct financial purchase is disabled.
  assert.ok(reward.includes('createRewardRequest('))
  assert.ok(reward.includes('approveRewardRequest('))
})

test('task and reward views use shared currency formatting and no hidden reset/restore route', () => {
  const tasks = src('pages/TasksPage.jsx')
  const rewards = src('pages/RewardsPage.jsx')
  const points = src('pages/PointsPage.jsx')
  assert.ok(tasks.includes('useCurrencyQuantity'))
  assert.ok(rewards.includes('currencyQuantity'))
  assert.ok(points.includes('useCurrencyQuantity'))
  assert.ok(tasks.includes('CreateTaskModal'))
  assert.ok(rewards.includes('RewardPurchaseHistory'))
  assert.ok(points.includes('RewardPurchaseRecoveryPanel'))
  assert.ok(!src('App.jsx').includes('path="/restore/execute"'))
})

test('theme preference radio labels are localized across all four languages', () => {
  const preference = src('components/ThemePreference.jsx')
  assert.match(preference, /useTranslation\(\)/)
  assert.match(preference, /aria-label=\{labels\.group\}/)
  for (const language of ['uk', 'ru', 'de', 'en']) {
    assert.ok(preference.includes(language + ': { title:'), language)
  }
  for (const mode of ['system', 'light', 'dark']) {
    assert.ok(preference.includes("['" + mode + "', labels." + mode + ']'))
  }
})
