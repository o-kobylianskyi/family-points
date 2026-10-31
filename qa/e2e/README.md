# FamilyPoints Web QA — Playwright

Isolated, opt-in browser smoke. This does not grant ChatGPT direct control of your desktop Chrome.

## Setup (Windows PowerShell)

```powershell
cd C:\Users\Oleh\IdeaProjects\family-points
git pull --ff-only origin main
cd qa\e2e
npm install
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
