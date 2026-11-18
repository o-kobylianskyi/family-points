import { readdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { execFileSync } from 'node:child_process'

// Cross-platform syntax gate: no QA credentials, web server, browser or API
// calls are needed. Discover every onboarding V49 spec instead of maintaining
// an incomplete filename allowlist.
const here = dirname(fileURLToPath(import.meta.url))
const testDir = join(here, '../../qa/e2e/tests')
const specs = readdirSync(testDir)
  .filter(name => /^onboarding-v49-.*\.spec\.js$/.test(name))
  .sort()

if (specs.length === 0) {
  throw new Error('V49_SYNTAX_GATE_NO_SPECS')
}

for (const spec of specs) {
  execFileSync(process.execPath, ['--check', join(testDir, spec)], {
    stdio: 'inherit',
  })
}
console.log(`V49 Playwright syntax PASS: ${specs.length} fixtures`)
