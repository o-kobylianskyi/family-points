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


## WEB-02 corrected two-tab browser fixture (2026-11-17)
- Test file `qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js` corrected in Web source commit `a0d87ee52ebed8b1e62903a910fc2bb010999189`. Previous file had doubled regex escape backslashes (28 pairs) that can break JavaScript parsing; do **not** run older fixture and mistake parser failure for application V49 defect.
- Run `node --check qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js` before Playwright. Then run the two tests with QA credentials, selected isolated family and pinned clean SHA, capturing actual PASS/FAIL/SKIP and POST counts. Browser execution still NOT_RUN by Web. No real family setup writes in the mocked cases.


## WEB-02 fixture syntax note (2026-11-17)
- Corrected two-tab browser spec blob `761d10edd052cb31a0a27f50b7ba974661ad3332` passes V8 syntax parser with module imports adapted for parsing only. Browser not executed. QA should run isolated authenticated Playwright on pinned SHA with expected one finalize POST, preserve competitor shared marker; report PASS/FAIL/SKIP and trace. Missing isolated credentials means SKIP, no real-family writes.


## WEB-02 missing-scope safety regression (2026-11-17)
- Source fix `69a5fc3e59060601ec568785da2fac2d70d8c964` and unit regression `ec425f80189ccd65e663a4f25a047ff6c9863d62` require nonempty account/workspace on stored pending V49 operation and caller. V8 direct source-level 10/10 bounded assertions PASS; not full Node/Playwright execution. QA run pinned `frontend npm run test:regression` and corrected two-tab Chromium before V49 signoff. Missing QA credentials => SKIP, not PASS. No real or existing family writes; BETA HOLD.


## WEB-02 V49 source regression executed evidence (2026-11-17)
- Fetched code+test blobs `30194a6a` and `fd724498`; executed 14 source-backed `setupFinalizeSafety.test.mjs` callbacks in V8 with JS assertion equivalents: 14 PASS, 0 FAIL. Not native `node --test`, not full npm regression, not browser; do not mark CI current-SHA green from this alone. QA still owns pinned Chromium `onboarding-v49-two-tab-synthetic.spec.js`, first syntax check then browser and POST count evidence. No real family mutation, BETA HOLD.


## WEB-02 V49 state workspace scope fix (2026-11-17)
- Fix `50f1f01328d3fbe0eaf60f1ff07902b232211c62`: reject absent/null/blank caller or response workspaceId in `assertSetupState`; regression `ee8f32b4935ea7decfaed1c41309aeb65bb93065` covers missing scope. Actual source-backed V8 14/14 test callbacks PASS on latest blobs, but **native Node/Playwright and same-SHA CI NOT_RUN/NOT_VERIFIED**. QA to run current pinned regression + corrected Chromium two-tab, report SHA and PASS/FAIL/SKIP; no real family mutations, BETA HOLD.


## WEB-02 V49 final review invariant (2026-11-18)
- Source `fd13aa0eda59eb81a52bf465a60cacea875b48d6` now invokes validated `assertSetupState` before accepting `assertSetupReview` and requires status IN_PROGRESS; tests `f6de28e0424eab7cbb0858306d5f582bd4869484` reject missing scope, null, NOT_STARTED and COMPLETED state. Source-backed V8 run 14/14 PASS; native Node/Chromium NOT_RUN. QA please run pinned full Node regressions and corrected two-tab Chromium; distinguish FAIL/SKIP and record exact source SHA, no existing family writes.


## WEB-02 CI syntax safeguard (2026-11-18)
- New source `3f36023d` and package wiring `fe5b5ad1`: Web `npm run test:regression` now performs `node --check` for every V49 Playwright spec, without credentials/browser/network. QA run `npm run test:qa-syntax` for isolated parse verification; browser two-tab still NOT_RUN; report source SHA, case outcomes, POST counts. Do not perform existing family mutations.


## WEB-02 verified push CI evidence (2026-11-18)
- GitHub Actions run 44260359035 @ fe5b5ad1 and run 44260568949 @ 201c486f: real Web logs show **V49 six Playwright fixtures syntax PASS, 33/33 native Node regression PASS, i18n/lint/build success, 3 nonblocking lint warnings**. Run 44260568949 full CI failed Backend Maven; no full green. Node syntax is not Chromium. QA execute actual `onboarding-v49-two-tab-synthetic.spec.js` 2 cases only with isolated QA account/selected workspace, record HTTP POST/GET counts, exact SHA and traces. No real family writes.


## WEB-02 two-tab receipt identity assertion (2026-11-18)
- Test patch `9e6a478246dddb0472c707026723a36da54d8f01` requires each of the two mocked Playwright receipt GET requests to match the **original** finalize Idempotency-Key UUID and workspace. Old test only counted receipt calls, which could mask a wrong-UUID GET. Syntax-only V8 PASS; real Chromium **NOT_RUN**. QA run on pinned clean SHA, record both case PASS/FAIL/SKIP, original UUID, POST count=1 and receipt same-UUID GET; do not disclose credentials or run real family writes. BETA HOLD.


## WEB-02 V49 pending marker persistence checks (2026-11-18)
- Browser synthetic `qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js` updated at `3354bcab3039d79aa5467ff9a040508431a06a80`: after lost finalize and GET receipt 404, local+shared pending operation IDs equal original finalize POST UUID; after foreign shared-marker takeover during valid receipt, original local marker remains original UUID and foreign shared marker survives. Existing assertions also require receipt GET exact original UUID and no duplicate finalize POST. V8 source syntax PASS; browser Chromium NOT_RUN, QA runs pinned clean SHA in isolated selected QA workspace, record PASS/FAIL/SKIP, POST count, original UUID/GET path and trace. Latest former Web-scoped run #44263459076 @9e6a478 6-fixture syntax/33 Node PASS, predates new test. No real family actions, BETA HOLD.


## WEB-02 two-tab route assertion reliability (2026-11-18)
- Test-only commit `ca87502121e19ebe90f25bb83e8f2560a07e068f`: both V49 browser specs record the receipt GET path in `context.route` handler and assert exact original UUID in test body, preventing route-callback assertion exceptions from masking errors as timeouts. V8 syntax PASS, Chromium NOT_RUN, latest Actions run queued when checked. QA run 2 scenarios in isolated selected QA workspace, report original UUID, GET path, POST count, PASS/FAIL/SKIP and trace; BETA HOLD.


## WEB-02 V49 synthetic workspace write guard (2026-11-18)
- Fixture commit `67aa36aa2ed82362e6b7c2df5f50f113fa724e3d` adds catch-all non-setup workspace mutation guard: all unexpected `/workspaces/<id>/...` POST/PUT/PATCH/DELETE must abort, while narrowly mocked setup/review/finalize and read-only GET retain existing behavior. Five V8 fake-route direct outcomes PASS, not Chromium. QA pinned browser run must verify no unhandled intercepted writes, original UUID, 1 finalize POST, exact receipt GET, no unauthorized family writes; failure trace. BKL-002/BETA HOLD.


## WEB-02 finalize revision+epoch additional QA checks (2026-11-18)
- Source patch `366737553301c7a831d41336c0d7f4eb3c58fb2e` adds checks to both two-tab mocked finalize scenarios: `expectedRevision === 7`, `X-Workspace-Data-Epoch === '0'` on the only original POST, plus prior UUID and exact receipt GET path; no unexpected writes. V8 syntax PASS, native Chromium NOT_RUN. At authoring run 44267564831 in progress. QA owns pinned isolated browser outcome and exact POST count. BETA HOLD.


## WEB-02 service worker safe mock isolation (2026-11-18)
- Patch `27b91a22311e2f02a1686ce93b06dfccdedf5711` sets Playwright `test.use({ serviceWorkers: 'block' })` for two-tab V49 synthetic only. Active Service Workers can intercept network requests before Playwright `context.route`, undermining the catch-all POST block; per-spec setting enforces route interception. V8 syntax PASS, Chromium NOT_RUN; QA executes only in isolated selected family with all writes intercepted, reports 2 cases and POST counts. BETA HOLD.


## WEB-02 V49 synthetic all-mutations guard (2026-11-18)
- Fixture `374af974349d4fbcfad14f88d1d2ed0a66cbb928` upgrades post-login Playwright route guard from only `/workspaces/:id/...` to **all URL paths**, aborting/logging unexpected POST/PUT/PATCH/DELETE; known V49 setup/review/finalize routes registered subsequently and mocked. Service Workers disabled. Direct extracted guard V8 simulation GET + five different write URLs = 6 expected outcomes PASS, **not Chromium**. QA should report unexpected auth-refresh writes as blocked FAIL/triage rather than silently sending them. Browser acceptance (2 actual cases, original UUID, epoch/revision, markers, POST count) still pending; BETA HOLD.
