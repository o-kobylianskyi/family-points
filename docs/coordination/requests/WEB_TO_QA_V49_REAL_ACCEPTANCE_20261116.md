# Web → QA — V49 real acceptance, operator-assisted (2026-11-16)

**Owner:** QA. **Implementer:** Web. **Operator:** user. This is a handoff, **not permission to execute**. Consult [DESIGNER_TO_QA_REAL_V49_ACCEPTANCE_20261116.md](DESIGNER_TO_QA_REAL_V49_ACCEPTANCE_20261116.md) for the consolidated V49 real campaign; do not duplicate independent manual execution requests to user.

## Newly completed synthetic browser evidence
- User reported `onboarding-v49-finalize-recovery-synthetic.spec.js` **2 PASS / 0 FAIL / 0 SKIP**, 5.4 seconds, Chromium: (1) receipt valid but server setup stale -> original UUID remains blocked; (2) receipt valid and status COMPLETED -> original UUID recovery succeeds. Local SHA was not printed alongside this output; don't attach to current main.
- Prior `onboarding-v49-synthetic-readonly.spec.js`: **3 PASS** including V46 empty step bootstrap, malformed state fail-closed, lost PATCH reply GET-only reconciliation.
- Extended synthetic receipt suite now tests **4** scenarios (stale, completed, wrong-revision, wrong-epoch). **New two cases are unverified**, not PASS. Web owns automated regressions; QA collects consolidated execution later.

## QA campaign: real feature-enabled V49, ONLY separately approved disposable DEV/QA workspace
1. **Preflight read-only (may run now):** exact SHA & environment, owner/child/foreign auth scope, `GET /setup` (feature enabled?), `dataEpoch`, `revision`, proper initial `steps: {}` with seven visible PENDING rows; verify no implicit write, 142 templates & 6 packs, UA/RU/DE/EN, mobile/light/dark.
2. **Operator-approved real setup (do not run until QA fixture and user approval):** confirm unique disposable workspace ID and initial empty ledger; explicitly mark/skip seven distinct steps. Each user action issues at most one PATCH with expectedRevision, GET same step/revision+1. Review catalog subset with one TASK and one REWARD and explicit confirmation; inspect exact ordered codes, toImport+alreadyImported partition, same epoch/revision, `createsActiveDefinitions=false`. Capture one idempotency UUID on finalization (redact credentials), durable receipt, `COMPLETED` and revision+1. Assert NO task/reward activation, purchases, ledger writes, balances changes, extra POST.
3. **Fault isolation:** lost PATCH reply after server commit -> GET-only revision reconciling and no replay. Lost finalize reply -> retained original UUID, GET receipt then GET completed state. Force stale completed state, wrong epoch/revision, malformed receipt, conflicting local/shared markers, two-tab completion. Expected: pending blocked until exact receipt and completed step state match; never new UUID or second finalize POST.
4. **Persistence and authorization:** reload/login/second tab, owner vs child vs foreign user, no data leakage. Same workspace selected only, no real-family/other-tenant changes. Record HTTP request counts, before/after private template vs active definition/ledger counts, server-side original UUID receipt where available.
5. **Stop/skip rules:** backend V49 flag disabled or no approved disposable fixture => `SKIPPED/BLOCKED`, not PASS. No BKL-002 reset/execute/restore, no public BETA/PROD, no live family, no destructive SQL. BKL-002 acceptance is separate gated campaign.

Report `CASE_ID | SHA | environment | redacted workspace | initial epoch/revision | request counts | expected | actual | PASS/FAIL/SKIP | evidence` in `docs/coordination/logs/qa.md`. QA sends specific verified Web defects back to Web and Backend contract issues to Backend, without stopping independent Web automation.

## Suggested QA read-only automation command (run only once QA schedules a consolidated campaign)

```powershell
cd C:\Users\Oleh\IdeaProjects\family-points
git pull --ff-only origin main
git rev-parse HEAD
git status --short
cd qa\e2e
npx playwright test tests/onboarding-v49-synthetic-readonly.spec.js tests/onboarding-v49-finalize-recovery-synthetic.spec.js --reporter=list --workers=1 --trace=off
```

This is synthetic UI coverage only. **No manual live-test request to operator from Web**; QA is the sole operator-facing coordinator.
