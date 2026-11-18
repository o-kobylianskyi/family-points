# FamilyPoints Web — rolling latest handoff

**New role chat:** FamilyPoints — Web Frontend (WEB-02), pickup 2026-11-16 Europe/Berlin after old chat reached hard cap. **Repo:** `o-kobylianskyi/family-points`; integration branch `main`. **HEAD inspected at pickup:** `e51c0972aa3dd7a2dd5960cb5e450b40bae4750c` (2026-11-16 20:25Z). Other tracks push concurrently: re-fetch HEAD before every edit. Old emergency handoff: [20261116_WEB_CONTINUE_AFTER_LIMIT.md](20261116_WEB_CONTINUE_AFTER_LIMIT.md).

## Source and decisions checked
- Read `AGENTS.md`, `docs/coordination/README.md`, chat-capacity pilot, Web journal, Web QA packet and issue #18/#20. Release status must not be inferred from commits alone.
- Source checked: `frontend/src/utils/setupFinalizeSafety.js` and `frontend/tests/setupFinalizeSafety.test.mjs` (current file content fetched at pickup); `frontend/src/utils/setupCatalogSelection.js`, its node test file, and `frontend/package.json`.
- Latest Web/Designer commits after prior screenshot: `2717222` and `442a458` guard step PATCH epoch/revision/step; `cb5c44f` centralizes repeated-step guard; `5c969dc` covers filtered partial deselect. **Do not duplicate these.**
- CI optimization issue #20 adds `npm run test:regression` and lint to main CI. Full same-SHA jobs should be confirmed through Release with exact run ID and executed steps.

## WEB-02 package 1 (2026-11-16)
- **Source commit:** `e5b66b4df4dda057c40316fc0abfbcf4cb5eff25` removes dead `allMatchingSelected` derivation from `InitialFamilySetupReview.jsx`, now that the actual Deselect matching button correctly uses `hasSelectedSetupMatch`. No API, UX or security semantics intentionally changed. This cleans an obsolete computed state in the regression area.
- **Validation:** source-only inspection, CI/Node/Vite/lint NOT_RUN in this chat; same-SHA CI awaits observed jobs. Review commit on current HEAD before additional edits.
- **Continuity:** rolling handoff originally created `bc84f6ef`; capacity checkpoint `4650a470`. This revision reflects new source SHA.

## WEB-02 package 2 — V49 PATCH invariant regression (2026-11-16)
- Read actual `frontend/src/api/setupApi.js`, `frontend/src/utils/setupFinalizeSafety.js`, node safety tests, and synthetic browser specs `onboarding-v49-epoch-race-synthetic.spec.js`, `onboarding-v49-loading-retry-synthetic.spec.js`, `onboarding-v49-finalize-recovery-synthetic.spec.js`. These synthetic specs already cover epoch-stale, 503 retry, and four finalize receipt outcomes. **Do not repeat them.**
- Found unguarded legitimate-HTTP-but-invalid step response: same target step/epoch/revision could unexpectedly change other decisions or prematurely return setup COMPLETED. Source fixes `c36dacd`, `87d34f4` require successful PATCH to be IN_PROGRESS and leave existing other decisions untouched; lost-PATCH GET reconciliation returns UNCONFIRMED on same violations. New explicit PENDING entries remain allowed. Tests added `2b706f4`, `6d8f829`.
- **Latest new source/test SHA:** `6d8f8293a755e10be0833a65c848603b71d48cfd`. All four commits on `main`. Validation **NOT_RUN** (GitHub connector writes only, no repo runner attached to this chat); CI same-SHA **NOT_VERIFIED**, no PASS claims. QA can run automated synthetic Playwright alongside broader suite; QA owns real disposable workspace E2E.
- Next: inspect feedback from actual same-SHA CI/source tests, resolve any detected failures, then check cross-tab localStorage race and existing source readiness. No real-family changes, BETA HOLD.

## WEB-02 package 3 — cross-tab finalize marker ownership (2026-11-16)
- HEAD checked at pickup: `15c0c62e801aac7e22af12d01445230c2821468b` (other tracks may have newer commits).
- Examined `frontend/src/components/InitialFamilySetupReview.jsx`, `setupFinalizeSafety.js`, safety unit tests, shared `api/client.js`. Found that staged finalize markers were not rechecked immediately before the one POST after competing tab changes.
- Source: `c60f6d3` defines `ownsSetupFinalizeMarkers(session, shared, operation)` requiring both markers to match all immutable fields; `2ab9791` validates immediately after staging and before finalize dispatch. On mismatch fail closed, preserve markers rather than deleting a competing tab's operation.
- Regression `c731a06b8ae518daf8f87e91d17c65ff60f1f931`: tests same/foreign UUID, account, workspace, epoch, revision, ordered codes, absent and blocked markers. QA synthetic browser adversarial scenarios appended to `WEB_TO_QA_V49_REAL_ACCEPTANCE_20261116.md` commit `bed5098`.
- **Status:** source/test committed, no Node/Playwright/browser execution or same-SHA Actions evidence in this chat (**NOT_RUN / NOT_VERIFIED**). Frontend browser marker check is non-atomic across tabs; Backend revision/idempotency/dataEpoch remains final authority. No real-family activity, BKL-002/BETA HOLD.
- Next: obtain actual test/CI evidence, fix failures as found, add an automated synthetic two-tab browser test if QA coverage missing, review mark-clearing races conservatively.
- Rolling handoff updated in this batch; last inspected HEAD after source packet: `bed509893f65e0a13699594f0599b44e238beda2`. Re-read main on next pickup.

## WEB-02 package 4 — two-tab synthetic Playwright (2026-11-16)
- Startup HEAD `09a7a9e6b556545c6d5454f10f49563125f3b0ec`; source/test at `fe64edb351ea5c30fce080c8385129ca6c99452e`.
- Added `qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js`: two real browser tabs in one Playwright context, mocked all setup/data-epoch/catalog requests after QA login. Case A: tab A loses finalize response; tab B sees shared pending, cannot dispatch another finalize, GET receipt 404 keeps same UUID. Case B: tab B changes shared UUID during tab A's receipt lookup; tab A must preserve competing marker and remain blocked. No actual setup writes.
- Initial commit `381f3f4`, test injection ordering corrected `fe64edb` so foreign marker is inserted **during in-flight receipt recovery**, before cleanup (not before clicking a now-disabled receipt button).
- **Execution:** NOT_RUN; browser runner unavailable in this GitHub connector-only session. CI actual same-SHA jobs NOT_VERIFIED; no PASS claim. Needs QA to run on a pinned SHA in the consolidated synthetic campaign. Avoid assuming authenticated second tab shares sessionStorage; this is one browser context with shared localStorage, so auth/selected workspace precondition is checked.
- Next: inspect QA test output for selectors/auth and race timing, fix failures, then inspect CI complete/scoped job evidence and remaining Web release defects. BKL-002/BETA HOLD remains.

## WEB-02 package 5 — GitHub Actions evidence audit (2026-11-16)
- Current `main` at audit entry: `b415039a84517d602dbbf089f13ba89de42b70ca`. Four-component run IDs obtained from Release journal and verified directly using actual GitHub Actions **job step** results:
  - [43947907960](https://github.com/o-kobylianskyi/family-points/actions/runs/43947907960): Web regression/lint/i18n/build executed SUCCESS, Android executed SUCCESS, Compose executed SUCCESS; Backend FAILED, full attestation FAILED.
  - [43953661904](https://github.com/o-kobylianskyi/family-points/actions/runs/43953661904): Web regression/lint/i18n/build executed SUCCESS, Android SUCCESS; Backend FAILED; Compose PowerShell parser FAILED, subsequent Compose tests SKIPPED; attestation FAILED.
  - [43953986968](https://github.com/o-kobylianskyi/family-points/actions/runs/43953986968): same Web/Android executed SUCCESS; Backend FAILED, Compose parser FAILED, attestation FAILED.
- These runs **predate the later two-tab browser spec `fe64edb` and are not a current HEAD proof**. Playwright test is not covered by the Web Node regression job: V49 two-tab synthetic remains **NOT_RUN**. No new Web runtime defect has been confirmed from these runs, therefore no speculative source rewrite.
- Release owns Compose parser and CI gate; Backend owns PG test failures. QA owns actual browser synthetic run with pinned SHA and real disposable V49 acceptance. BKL-002/BETA HOLD remains.
- Next Web priority: reconcile actual two-tab Playwright trace/results when available; preserve single-POST and pending markers, fix only reproduced defects; verify a newer exact-SHA CI run with real executed steps.

## WEB-02 package 6 — V49 readiness evidence audit (2026-11-16)
- Source intake HEAD `03a9de2a1788b18c2434312783efc68e7d6332d6`. Read latest QA Web historical 47/47 output and QA handoff QA-025. The 47/47 operator result is not immutable same-SHA with later V49 and two-tab code; no real current-head acceptance evidence.
- Published [WEB_V49_READINESS_AUDIT_20261116.md](../requests/WEB_V49_READINESS_AUDIT_20261116.md) `75ed5265`, evidence matrix and exact follow-up automated QA procedure; Web source/tests remain COMMITTED but two-tab Playwright NOT_RUN. Historical Actions Web/i18n/lint/build 3 runs PASS; no newer same-SHA current evidence.
- **No verified Web defect was reported in QA.** Therefore no speculative behavior refactor in this batch. QA owns pinned Node+Playwright and operator-only real disposable family acceptance; Web fixes reproducible defects when received. Backend/Release BKL002 and exact full-gate separate; BETA HOLD.
- Next: consume QA exact-SHA outcomes/Playwright traces, fix confirmed Web regressions and rerun targeted tests; keep handoff current.

## WEB-02 package 7 — verified newer complete CI, QA V49 status (2026-11-17)
- Independently fetched actual GitHub Actions jobs for [run 44044271962](https://github.com/o-kobylianskyi/family-points/actions/runs/44044271962), exact source SHA `8c397d0cbe7d68fd52d5b3f6e18a3ed44cda8d2a` as independently recorded by QA. Web locale check/source-backed regression/lint/Vite build: all executed **SUCCESS**. Backend Maven, Android Gradle, Compose parser/release safety, Full CI exact-SHA evidence and CI outcome reporting: all jobs executed **SUCCESS**. QA log records Maven 479 tests with zero failure/error/skip and 55 Release synthetic cases, from actual logs.
- Newest sampled moving main at audit pickup `6724c186d929bc45fa9e069f45cfffc9162c70c1`, later than green source SHA. Do not call this an exact-main PASS; subsequent Backend #25–#27 source tests have no observed matching full CI.
- QA independently source-reviewed `qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js` (QA-031), but **did not execute Chromium Playwright**. Same applies to operator-authorized real feature-enabled seven-step V49 acceptance. New Web-specific verified defect: none in observed CI. QA owns test execution, Web fixes any reproduced trace. BKL-002 protected reset/restore and Beta release remain HOLD.
- Next Web work: consume QA exact-SHA two-tab fixture results, audit test fixture reliability and correct confirmed failures only; coordinate with Release for new same-SHA non-skipped CI following Backend source commits.

## WEB-02 package 8 — V49 two-tab Playwright source parser fix (2026-11-17)
- Source reviewed QA-031; no executed V49 two-tab browser result yet. Independently re-read `qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js` and identified **28 doubled backslash pairs** in regex literals, e.g. `/\\\\/login/` and `/7\\\\/7/`, generated by previous escaped transport. These prevent intended JS regular expression parsing/matching. Corrected in `a0d87ee52ebed8b1e62903a910fc2bb010999189` by reducing each doubled pair to a single regex escape, without changing request mocks, UI or Back-end behavior.
- Browser test execution **NOT_RUN** here: QA must run `node --check qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js` and the actual two-tab Playwright suite on pinned SHA, then capture PASS/FAIL/SKIP, trace and network POST count. Prior read-only source inspection is not an E2E PASS; the new change fixes a source-level fixture defect.
- Latest independently observed full CI run [44044271962](https://github.com/o-kobylianskyi/family-points/actions/runs/44044271962) PASS on older `8c397d0`, not a current HEAD + browser suite PASS. BKL-002/BETA HOLD remains.
- Next: inspect QA test run; fix environment/auth or browser race only with real failure evidence, then target completed V49 acceptance.

## WEB-02 package 9 — two-tab fixture syntax validation (2026-11-17)
- Latest main at pickup `875720e961a154ec1b7a4d3be36b1dd2d5ee2ca1`. QA-032 [handoff](../handoffs/QA_LATEST.md) confirms review of Web regex fix `a0d87ee`, no executed Chromium result.
- Independently fetched corrected `qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js` blob `761d10edd052cb31a0a27f50b7ba974661ad3332`. Validated with JS V8 `new Function` parser after replacing top-level imports and `import.meta` **for syntax parsing only**: **PASS**. Not an actual Node module check, Chromium browser run, or V49 acceptance.
- No confirmed UI/runtime failure. QA remains execution owner; avoid extra manual/operator test requests. No source modifications in this checkpoint. Last all-four full CI PASS is historic run `44044271962` at `8c397d0`; newer Backend commits require new exact-SHA job evidence.
- Next: consume QA two-tab test PASS/FAIL/SKIP + failure trace or establish isolated browser test harness under QA; investigate real failures only. No BETA/PROD or family data, BETA HOLD.

## WEB-02 package 10 — V49 scope-less pending marker rejection (2026-11-17)
- Pickup HEAD `4edef5944f0cdd2ca030896d169d3088d0486863`; QA-033 handoff still shows corrected two-tab Chromium **NOT_RUN** (auth-selected QA workspace prerequisite).
- Independently found `validSetupFinalizeOperation(op, workspaceId, accountId)` accepted a missing scope if **both** fields were `undefined` or `null` because `String(undefined)===String(undefined)`. Hardened source at `69a5fc3e59060601ec568785da2fac2d70d8c964`: explicit nonempty scope checks for caller and stored operation (account+workspace). Added negative unit regressions `ec425f80189ccd65e663a4f25a047ff6c9863d62`, including direct validation, ownership and blocked pending reconciliation.
- Evaluated actual fetched source through V8 (replacing only export declarations) on 10 bounded scenarios: **10/10 PASS** (positive, missing account/workspace/marker for `undefined`, `null`, `''`). Not full `npm run test:regression`, not Jest/Node runner or cloud GitHub CI.
- Frontend V49 real feature-enabled QA remains separately operator-approved disposable-only. Current HEAD full CI after this patch NOT_VERIFIED; last verified green `44044271962` pinned to older `8c397d0`. **BETA HOLD**.
- Next: QA collects exact-SHA Node+Chromium test evidence; Web fixes reproduced UI failures and checks any new post-change CI steps.

## WEB-02 package 11 — executed source-backed V49 safety tests (2026-11-17)
- Last observed main HEAD before package `eb1421b64f1981315da86c2d663326712abafd69`. QA handoff QA-033 has no corrected two-tab Chromium outcome; last independently witnessed all-four full CI PASS `44044271962` at old `8c397d0`.
- Independently fetched `frontend/src/utils/setupFinalizeSafety.js` blob `30194a6a4d303a5fea423c3d8d1a15522e03a43c` and `frontend/tests/setupFinalizeSafety.test.mjs` blob `fd7244986b67b32f6972b26639783719a2c94aeb`; executed all 14 actual test callbacks with V8 harness, replacing only JS export/import syntax and using equivalent `equal/deepEqual/throws` assertion functions. **14 PASS / 0 FAIL**, including missing account/workspace, double-tab pending identity, receipt ordering, PATCH transition and no premature COMPLETED. This is **source-backed V8 harness**, NOT Node's official `node --test` or full `npm run test:regression`, not Chromium and not same-SHA GitHub Actions.
- No new confirmed Web code failure, no source changes this batch. Browser synthetic `onboarding-v49-two-tab-synthetic.spec.js` remains **NOT_RUN**, QA owns pinned isolated execution and report. No real family/PROD/BETA writes; BKL-002/BETA HOLD.
- Next: consume QA Chromium results and new exact-SHA CI run; fix confirmed defects, re-evaluate readiness.

## WEB-02 package 12 — reject unbound workspace in V49 state (2026-11-17)
- Starting HEAD `bc5b1885cb185f7e489aec5fd9706023616aeac8`; QA-034 reports corrected two-tab Chromium **NOT_RUN**, latest observed full four-job CI still `44044271962` at historic `8c397d0`, current SHA not proven.
- Static review found `assertSetupState` accepted absent `workspaceId` when both caller scope and response lacked it due to `String(undefined)===String(undefined)`, analogous to resolved pending marker scope issue. Product source fix `50f1f01328d3fbe0eaf60f1ff07902b232211c62` enforces nonempty caller and response workspace before equality; tests `ee8f32b4935ea7decfaed1c41309aeb65bb93065` assert reject for undefined/null/empty and retain positive state validity.
- Executed all **14** actual `setupFinalizeSafety.test.mjs` callbacks with source fetched from GitHub in V8 assertion-equivalent harness: **14/14 PASS / 0 FAIL** at blobs `0d2ab991dded36f326a57d882d464d0131b3fd0a` (source), `ee1a17fedbaf160c34147584d725cffff3bd67d2` (tests). Not a Node test runner, not Playwright, not complete Web CI on SHA. No PROD/BETA/real family action, BKL-002/BETA HOLD.
- Next: QA run exact-SHA native Node and two-tab Chromium in isolated context; Web inspect any actual failure traces and fix only verified defects; coordinate next CI candidate with Release.

## WEB-02 package 13 — V49 review requires authenticated in-progress setup state (2026-11-18)
- Pickup HEAD `1ba17931b4db7581b13c3a13b9ca4fe1e139c144`; latest QA-036 handoff still says Chromium two-tab `NOT_RUN`, feature-enabled user-assisted real V49 not authorized, last independently verified four-job CI run `44044271962` at older `8c397d0`.
- Source audit found `assertSetupReview` previously compared preview values to possibly unvalidated `state` fields, including missing workspace+review fields with matching `undefined`; source `fd13aa0eda59eb81a52bf465a60cacea875b48d6` now invokes `assertSetupState(state,state?.workspaceId)` and requires status `IN_PROGRESS` before trusting preview.
- Test `f6de28e0424eab7cbb0858306d5f582bd4869484` denies matching forged unbound state, null state, NOT_STARTED and COMPLETED state; valid IN_PROGRESS remains accepted.
- Actual fetched source `67dd0b03424f850de0ea9242f38c23924a0c24b5` + test blob `de0c9826f01a89e7f1ef8981ab685917f2a74174` executed all 14 source test callbacks in V8 assertion-equivalent harness: **14 PASS / 0 FAIL**. NOT native Node, Playwright, all-Web build or exact-current-SHA cloud CI. No user families or PROD/BETA touched, BETA HOLD.
- Next: obtain QA pinned native Node and two-tab Chromium case outcomes; fix confirmed failures, then readiness audit with full CI exact candidate SHA.

## WEB-02 package 14 — V49 QA fixture syntax gate integrated into Web CI (2026-11-18)
- Intake main HEAD `fa02cca11a41945ce7eedbdd2c8c705b0740b7e6`. Latest QA-037 has **not** executed the two-tab Chromium test; historical full CI `44044271962` at older `8c397d0`, current SHA still NOT_VERIFIED. BKL-002 remains HOLD.
- Confirmed Web workflow runs `npm run test:regression` but did **not** check Playwright fixture syntax; previous V49 two-tab regex error was not caught there. Source `frontend/scripts/check-v49-fixtures.mjs` commit `3f36023d649f2f801d346fda7e41e4f89b339c7b` scans all `qa/e2e/tests/onboarding-v49-*.spec.js` and runs `node --check` on each, aborts on any parse failure or zero matched fixtures, without credentials/browser/HTTP.
- `frontend/package.json` commit `fe5b5ad13c62d4c04d78352c23e4911dcf1f121b` prepends this check to existing `test:regression` (already invoked by Web GitHub Actions CI) and provides independent `npm run test:qa-syntax`. No changes to product UI/API, Playwright's runtime/credential gates or any real family.
- **Execution status**: committed and CI-wired; latest-commit Node, full Web CI, and Chromium results NOT_RUN/NOT_VERIFIED in this chat. Previous source-backed V8 14/14 belongs to earlier code blobs, not this new CI gate.
- Next: inspect actual CI job step for first newer Web-selected run and obtain pinned QA Chromium 2-case result with HTTP counts; fix confirmed fixture/runtime errors. No PROD/BETA deploy, BKL-002/BETA HOLD.

## WEB-02 package 15 — real latest Web CI evidence verified (2026-11-18)
- Discovered actual push Actions runs via connected generic GitHub GET: `GET /repos/o-kobylianskyi/family-points/actions/runs?branch=main`; read actual job steps and decoded logs, not inferred from green workflow badges.
- **[Run 44260359035](https://github.com/o-kobylianskyi/family-points/actions/runs/44260359035)** exact `fe5b5ad13c62d4c04d78352c23e4911dcf1f121b`, Web job `134652011939`: `npm run test:regression` executes new syntax gate **V49 Playwright syntax PASS: 6 fixtures**, native Node **33 tests, 33 passed, 0 failed, 0 skipped**; i18n, lint, Vite production build also **SUCCESS**. Lint emitted **3 warnings, 0 errors** (one `InitialFamilySetupReview.jsx:141`, two `FamilyDataSafetyPlan.jsx:53/66`, synchronous setState in React effects). This was a **scoped Web** workflow, Backend/Compose heavy test steps SKIPPED and full attestation SKIPPED; do not call full CI green.
- **[Run 44260568949](https://github.com/o-kobylianskyi/family-points/actions/runs/44260568949)** exact `201c486f83c84231d0a2b50bd8513f40b94f0f1c` (use full SHA from exact run metadata before any future candidate): Web job `134652692877` executed same syntax **6/6**, native Node **33/33 PASS**, i18n/lint/build **SUCCESS**, 3 lint warnings/0 errors; Android and Compose jobs SUCCESS. Backend Maven **FAIL**, Full CI attestation and outcome report FAIL. Backend/Release own failure; Web is not blocker in this run.
- This **does not execute browser Playwright**, including corrected two-tab 2-case synthetic. QA-038 still reports V49 Chromium **NOT_RUN** and real disposable enabled-server onboarding not authorized. `node --check` proves only syntax, not actual Playwright behavior.
- No source change this package: newest Web syntax gate is truly running in CI. No PROD/BETA or real family actions. BKL-002 protected restore/VERIFIED and BETA remain HOLD.
- Next: consume QA pinned Chromium two-tab scenario results (single original POST, immutable UUID, competing marker untouched), triage verified browser defects, coordinate exact candidate SHA with Release.

## Implemented vs verified
- Implemented in source: seven-step V49 wizard, 142 catalog/6 packs, parent-gated review, durable original finalize UUID, GET-only recovery after lost replies, cross-tab mismatch blocking, no implicit writes; step epoch/revision protection.
- Historic user-side Playwright evidence: 3/3 read-only synthetic and 2/2 finalize-recovery synthetic PASS, **not pinned to current HEAD**. Old screenshot reports Web node source 31/31 PASS on `758dfe4`, Vite PASS, lint 4 warnings/0 errors; **historic chat report, not rerun here**.
- This pickup performed **source inspection only**. Node/Vite/Playwright and current SHA Actions **NOT_RUN / NOT_VERIFIED**. Do not call CI green until inspecting actual jobs and steps.
- Real feature-enabled V49 E2E: **QA-owned, pending** approved disposable workspace. QA packet: [WEB_TO_QA_V49_REAL_ACCEPTANCE_20261116.md](../requests/WEB_TO_QA_V49_REAL_ACCEPTANCE_20261116.md) and Designer's consolidated campaign.
- Security gate **BKL-002 HTTP 423/BETA HOLD**: no PROD/BETA deploy, no destructive reset/restore/execute or writes to actual families.

## Next safe Web package
1. Inspect current V49 UI, `setupApi`, and Playwright synthetic specs (epoch race, GET 503 retry, wrong receipt revision/epoch) for scenario holes. No duplicate Web/Designer fixes.
2. Add a cohesive bounded Web source/test regression package for an actual uncovered defect; run `cd frontend && npm run test:regression && npm run lint && npm run i18n:check && npm run build` on exact SHA if a runner is available. Otherwise mark NOT_RUN; route automated Playwright run to QA with exact SHA.
3. Hand real multi-tab, role ACL, lost-response, selective-import and mobile/theming acceptance to QA only; operator tests stay aggregated there.
4. Inspect GitHub Actions full vs scoped steps on exact candidate SHA; separate current HEAD and test SHA. Check issue #18 and #20.
5. Every significant batch refresh this rolling handoff plus `chat-capacity/WEB.md` and track journal; keep files self-contained. Do not promise precise ChatGPT context percentage.

## New chat opener
```text
Продовжуємо FamilyPoints Web після перенесення чату. Прочитай AGENTS.md,
docs/coordination/handoffs/WEB_LATEST.md, docs/coordination/logs/web.md
та перевір новий main SHA GitHub. Продовжуй V49 source+synthetic regression,
передавай всі реальні E2E QA. BKL-002/BETA HOLD. Після кожного пакета
оновлюй rolling handoff і chat-capacity WEB log, не вигадуй % ліміту.
```
