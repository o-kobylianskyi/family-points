# FamilyPoints Web QA — Playwright

Isolated, opt-in browser smoke. This does not grant ChatGPT direct control of your desktop Chrome.

## Setup (Windows PowerShell)

```powershell
cd C:\Users\Oleh\IdeaProjects\family-points
git pull --ff-only origin main
cd qa\e2e
npm install  # creates local package-lock.json if not committed yet
npx playwright install chromium
$env:FP_QA_BASE_URL = 'http://localhost:5173'
npm test
```

The first unauthenticated test runs without secrets. For the authenticated tests **create a dedicated disposable QA account and QA family**, never reuse a real family's admin account. Prefer a local, private `.env` mechanism or environment variables outside Git; never commit credentials.

```powershell
$env:FP_QA_EMAIL = '<QA login>'
$env:FP_QA_PASSWORD = '<QA password>'
npm test
```

Only run credentials against a URL you control. Running against public DEV requires an HTTPS address, deployed compatible Backend, and authorization for that QA family. Do not automate real-family resets. This initial suite checks public login, QA-family templates, and Settings navigation only; reset preview/plan, family switching, epoch and UI assertions require verified selectors and a dedicated fixture. If login redirects to family choice, the suite deliberately fails closed.

Artifacts on failures: screenshots, videos, trace, and `playwright-report/`. Add these generated directories to your local Git ignore/exclude before committing; never publish auth traces or cookies as public CI artifacts.

Reset execution, payment, ledger, member deletion and active reward/task mutation are **out of scope** until separately authorized, safe, isolated E2E fixtures exist.

## Reproducible run / privacy

Record `git rev-parse HEAD` and `git status --short` before the run. `qa/e2e/package-lock.json` generated locally is not a test failure. Review it and commit it separately after verifying package integrity; do **not** commit browser reports, traces, recordings, cookies or credentials. These generated artifact paths are ignored by repository `.gitignore`.

For a stable, read-only full-suite run when Playwright's Chromium trace recorder reports `ENOENT ... recordingXX.trace` during browser teardown, disable traces for that run instead of weakening test assertions:

```powershell
npx playwright test --reporter=list --workers=1 --trace=off
```

Use `--trace=retain-on-failure` for targeted debugging if the trace recorder is healthy. An `ENOENT` trace teardown counts as a test-run failure until a fresh successful rerun.

**V49 feature flag:** the real seven-step server workflow and real manager catalog tests deliberately SKIP if Backend V49 is disabled. This is not acceptance. The separate `onboarding-v49-synthetic-readonly.spec.js` intercepts **GET-only** setup/catalog responses in the browser, using the bundled 142-template/six-pack catalog, to check seven-step controls and bulk/search UI without changing backend configuration or data. This synthetic coverage does not replace full server-enabled disposable-family acceptance.

To run those read-only scenarios only:

```powershell
npx playwright test tests/onboarding-v49-synthetic-readonly.spec.js tests/onboarding-v49-authorization-readonly.spec.js --reporter=list --workers=1 --trace=off
```

For complete cross-family and child checks, supply only **verified, disposable** QA data. Do not invent workspace IDs:

- `FP_QA_TEST2_USERNAME`, `FP_QA_TEST2_PASSWORD`, `FP_QA_TEST2_WORKSPACE_ID`, `FP_QA_FOREIGN_WORKSPACE_ID`
- `FP_QA_CHILD_USERNAME`, `FP_QA_CHILD_PASSWORD`

Never publish the environment values or browser traces. A zero-failure run with skipped tests is **partial acceptance**, not full V49/BKL-002 release approval.
