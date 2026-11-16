# WEB-V49-READINESS — evidence-based Web readiness audit
Date: 2026-11-16 (Europe/Berlin). Owner: Web / WEB-02. Status: SOURCE_READY_FOR_TARGETED_QA, **NOT ACCEPTED FOR BETA**.
Intake main HEAD: `03a9de2a1788b18c2434312783efc68e7d6332d6`. Re-fetch on next pickup due to concurrent tracks.

## Evidence and precise boundaries
| Area | Evidence | Verdict |
|---|---|---|
| Web regression/i18n/lint/Vite | Actual executed SUCCESS steps in Actions [43947907960](https://github.com/o-kobylianskyi/family-points/actions/runs/43947907960), [43953661904](https://github.com/o-kobylianskyi/family-points/actions/runs/43953661904), [43953986968](https://github.com/o-kobylianskyi/family-points/actions/runs/43953986968) | HISTORICAL PASS, not latest SHA |
| Operator local Web CI parity | QA log: `081d670cfdd9be9aef96664371c87ab3262d09a3`; 16 node PASS, i18n EXIT 0, lint clean, Vite build PASS | HISTORICAL LOCAL PASS |
| Authenticated browser E2E | QA log: 47/47 PASS, later operator HEAD `f27592ae89d1bdf1418fbfc1650bd1d8db20c24a` without clean tree or proof no intervening change | HISTORICAL PASS, checkout not immutably tied |
| V49 PATCH invariants | Source `c36dacd`/`87d34f4`; tests `2b706f4`/`6d8f829` | COMMITTED, not post-change executed |
| V49 marker ownership pre-POST | Source `c60f6d3`/`2ab9791`, tests `c731a06` | COMMITTED, not post-change executed |
| Two-tab synthetic Playwright | `qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js` @ `fe64edb351ea5c30fce080c8385129ca6c99452e` | COMMITTED, NOT_RUN |
| Epoch race, GET 503, finalize receipt synthetic variants | Existing V49 Playwright fixtures | Latest variants NOT independently verified |
| Full real V49 | Issue #18, Web+Designer QA handoff | BLOCKED until separately authorized disposable workspace |
| Full four-component Beta CI/BKL-002 | Issues #20/#15; Release/Backend own | HOLD |

## Core Web acceptance targets
- Seven-step parent-only V49 and epoch/revision fencing; 142 catalog templates, six packs, four languages; read-only display/review.
- PATCH lost reply uses GET-only reconciliation, never auto-replay; no unrelated step mutation or premature COMPLETED.
- One explicit finalize with immutable original UUID, ordered catalog codes, durable receipt and authoritative completed state before pending cleanup.
- Conflicting tab-local/shared markers block new POST, preserve foreign markers; cross-tab browser guard is best-effort, Backend remains final authority.
- Private template import only, no active task/reward, balances, purchases or ledger effect.

## Next QA automated campaign (no real V49 writes)
1. Pin `git rev-parse HEAD` and `git status --short`, preserve local unique files.
2. In `frontend`: `npm run i18n:check && npm run test:regression && npm run lint && npm run build`.
3. From `qa/e2e`, after checking exact fixture names, run:
```powershell
npx playwright test tests/onboarding-v49-synthetic-readonly.spec.js tests/onboarding-v49-epoch-race-synthetic.spec.js tests/onboarding-v49-loading-retry-synthetic.spec.js tests/onboarding-v49-finalize-recovery-synthetic.spec.js tests/onboarding-v49-two-tab-synthetic.spec.js --reporter=list --workers=1 --trace=retain-on-failure
```
4. Record `CASE_ID | SHA | environment | fixture | HTTP request counts | PASS/FAIL/SKIP | trace`. Missing isolated QA credentials/workspace means SKIP, not PASS.
5. Separate real feature-enabled campaign only under QA with explicit operator approval of a disposable NEW family; BKL-002 reset/restore is separately gated, not included.

## Ownership / gate
- Web owns confirmed React/API-client defects; QA owns executable browser scenarios and manual real acceptance with operator; Backend owns server idempotency/tenant/dataEpoch; Release owns full exact-SHA CI and deployment safety.
- No latest-Web PASS assertion, no actual real QA activity from Web, no destructive execute/restore, **BETA HOLD**.
