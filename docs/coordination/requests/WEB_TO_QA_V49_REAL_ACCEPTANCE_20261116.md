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


## WEB-02 incremental cross-tab regression handoff (2026-11-16; code `c731a06`)

**New source hardening:** `ownsSetupFinalizeMarkers()` checks that session and shared browser markers both match the entire immutable operation. After staging and immediately before invoking `finalizeSetupImport`, Web re-reads markers and fails closed on missing/foreign/competing values. This is best-effort local browser defense, NOT a cross-tab atomic transaction: Backend expectedRevision/dataEpoch/idempotency enforcement remains mandatory.

QA synthetic/browser checks (intercept all V49 writes, do not touch any real family):
1. **Two-tabs competing UUID:** stage tab A original UUID, then overwrite shared marker from tab B with another valid UUID before A dispatch. Expected: A attempts **zero** finalize POST after mismatch, UI remains pending/blocked; never creates a replacement UUID.
2. **Foreign marker fields:** change one of workspaceId/accountId/epoch/revision/ordered codes, keeping UUID equal. Expected: zero finalize POST, no cleanup of competing shared marker.
3. **Same identity:** session and shared values match exact staged operation, single finalize POST allowed; lost response remains pending and GET receipt+completed state required before clearing.
4. **Cross-tab receipt cleanup:** valid completion in tab A may clear only matching markers; a foreign marker inserted before cleanup is preserved/blocked. No second POST in either tab.
5. **Missing marker or storage failure:** missing shared/session, corrupted JSON, or storage access denied must fail closed with no newly generated finalize POST. Backend reconciliation remains required after an uncertain sent request.

Run as synthetic fixtures at pinned exact SHA and report PASS/FAIL/SKIP separately; real feature-enabled workflow remains only in independently authorized disposable QA workspace. No BKL-002 execute/restore, PROD or BETA.


## WEB-02 automated two-tab suite committed (2026-11-16)
- Spec: `qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js`, latest test SHA `fe64edb351ea5c30fce080c8385129ca6c99452e`.
- QA synthetic run (not a real V49 acceptance): `cd qa/e2e && npx playwright test tests/onboarding-v49-two-tab-synthetic.spec.js --reporter=list --workers=1 --trace=off`. Run only in configured QA environment with disposable QA manager credentials and selected isolated workspace; test intercepts scoped setup/data-epoch endpoints.
- Verify two tests individually: lost-response A retains UUID and tab B never POSTs; competing UUID injected during receipt GET remains in storage despite original receipt. Record exact source SHA, request counts, PASS/FAIL/SKIP and failure trace.
- **NOT_RUN** by Web; failures route back to Web. No operator request outside consolidated QA campaign, no destructive BKL-002 execute/restore.


## WEB-02 CI audit coverage note (2026-11-16)
- Directly observed actual Web job steps on runs `43947907960`, `43953661904`, `43953986968`: i18n, Node regression, lint and Vite build all SUCCESS. These runs do **not** execute newly added `onboarding-v49-two-tab-synthetic.spec.js`; overall full CI FAIL from Backend and (in later runs) Compose, unrelated to observed Web steps. No newest-HEAD or Playwright PASS claim.
- QA still owns pinned-SHA two-tab synthetic browser execution and independent disposable-family real acceptance. Record exact SHA, environment, request counts and PASS/FAIL/SKIP.


## WEB-V49-READINESS audit pickup (2026-11-16)
- Web published source-vs-execution matrix `docs/coordination/requests/WEB_V49_READINESS_AUDIT_20261116.md` at `75ed5265`. Do not carry historical operator 47/47 or Actions Web job PASS forward to newest two-tab SHA as VERIFIED. Run pinned node source + mocked Playwright V49 scenarios in the QA consolidated campaign, record HTTP counts and traces, report failures to Web.
- Full real V49 and BKL-002 remain separately gated; no operator request from Web and no real data changes.
