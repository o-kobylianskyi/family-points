# FamilyPoints QA — CI-OPT P0 acceptance and release evidence (2026-11-16)

**Owners:** QA (acceptance), CI/Release (GitHub Actions), Android (#19), Backend (#15), Web/Onboarding (#18).  
**Sources:** architecture `docs/coordination/DISPATCH_20261116_P0_BETA_RECOVERY.md`; GitHub issue #20; `.github/workflows/ci.yml`, `.github/workflows/android-p0.yml`, `.github/workflows/beta-release.yml`.

## Confirmed operator evidence (not release authorization)

- Authenticated local Playwright suite: **47/47 passed, 0 skipped, 0 failed** (submitted 2026-11-16, repeat 1.7 min). Covers Web read-only BKL-002, Test/Test2 child/tenant ACL, epoch, catalog and GET-only purchase history.
- Earlier operator reported targeted Android Gradle and disposable Backend Maven **BUILD SUCCESS**. Individual JVM/Surefire counts and exact SHA for those runs not supplied.
- At prior QA check, GitHub Actions run **43886667072** had detector SUCCESS + Compose SUCCESS, Java/Web/Android CANCELLED. This is neither all-green nor proof of individual code failure.
- The local full 47-case E2E suite is independent of `frontend/package.json`'s CI `npm run test:regression`: the latter runs Node tests, not browser Playwright. Never conflate the two.
- No verified full protected FULL/SELECTIVE production Reset/Restore Execute; BKL-002 remains HOLD.

## CI-OPT #20 proof matrix — required evidence, not assumed PASS

| Gate | Isolated scenario | Pass condition |
| --- | --- | --- |
| C01 | docs-only ordinary push | `changes` emits backend/android/web/compose false; named required jobs finish SUCCESS with scope summary, no heavy steps. A cancelled job does not count. |
| C02 | frontend-only push | Web `i18n:check`, `test:regression`, `lint`, `build` execute; backend and Android heavy steps do not. |
| C03 | android-only push | Android full JVM/APK main-job executes; independent Android P0 targeted job also executes. No relevant job prematurely cancelled. |
| C04 | shared/unknown-path or `workflow_dispatch` | All four heavy components selected and completed SUCCESS **on one SHA**. A manual run protects from superseding push cancellation. |
| C05 | Android lost-response specific tests | JUnit XML contains each exact method once, status PASS; failure/error/skip zero; original UUID preserved, GET receipt recovery, no implicit repost, account/workspace/epoch isolation. |
| C06 | BETA dispatch on branch, not annotated tag | `validate-tag` and final `release-gate` reject; no production deploy. |
| C07 | Annotated controlled non-production BETA tag | All five required validations SUCCESS and final `release-gate` SUCCESS on same commit; tag check alone does not authorize release. Do not publish a release tag as a test without separate Release approval. |
| C08 | Test artifact security | No passwords, JWTs, authorization headers, real-family data or Playwright traces published as public CI artifacts. |
| C09 | Same-SHA matrix | Record source SHA, workflow ID, per-job conclusion, test counts, skipped tests, timestamp; report failed/cancelled separately rather than green. |

## Current source boundaries

- `ci.yml` always declares the stable `changes`, Java, Web, Android and Compose jobs, and routes heavy steps based on conservative changed-file detection. It runs **Node-based Web source regressions**, not authenticated Playwright.
- `android-p0.yml` isolates the three lost-response tests and asserts they were executed using generated JUnit XML. Full Android JVM + APKs remain main-CI work and optional manual P0 workflow.
- `beta-release.yml` requires annotated tag, backend verify, Web i18n/regression/lint/build, Android JVM/assemble, Compose and a final all-success gate. Never trigger with a real release tag for exploratory QA.
- A source commit and a cancelled workflow do not establish successful execution.

## QA next execution package

1. Obtain a complete **manually dispatched FamilyPoints CI** run at a fixed chosen SHA; record every named job SUCCESS and whether scope outputs are true. Run exactly once for the candidate, after stabilization of moving main.
2. Collect independent `Android P0 / targeted lost-response regressions` XML evidence and all three exact methods' PASS on the same intended candidate or explicitly relate them to tested SHAs.
3. Run `npm ci && npm run i18n:check && npm run test:regression && npm run lint && npm run build` from `frontend`, and authenticated `npx playwright test --reporter=list` from `qa/e2e`, pinning local HEAD; Browser PASS is supporting evidence, not a substitute for CI.
4. Backend owner: disposable PostgreSQL test suites for encrypted archive, tenant/FK scope, corruption, restore/readback, receipt and rollback. Record current production unsupported modes and HTTP 423, not claim actual full protected reset.
5. Only after the Backend safe API and explicit scoped authorization: disposable family end-to-end backup → reset → restore → onboarding with proofs of owner authentication, verified protective snapshot, financial/ledger invariants, epoch concurrency and isolation.

**Status:** QA Web 47/47 operator PASS; CI-OPT routing acceptance and same-SHA full green NOT YET VERIFIED; Android #19 CI and BKL-002 #15 release gates remain OPEN. **BETA HOLD.**


## Addendum — 2026-11-16 current P0 evidence and new V49 Playwright gate

- Verified **GitHub Actions FamilyPoints CI run [43888684640](https://github.com/o-kobylianskyi/family-points/actions/runs/43888684640)** using actual run-job conclusions: `changes` SUCCESS, Java/PostgreSQL SUCCESS, Web/Vite SUCCESS, Android/Gradle SUCCESS, BETA Compose SUCCESS. Web owner journal independently records tested SHA `6a5e1adf876ada8dccf143f1a8439793b6781a5b`, Node regression 16/16 and lint 0 warnings/errors for that run. This **closes the evidence gap for one completed all-success main CI run**; it does not prove every component's heavy steps ran (scope routing may skip them) nor C01-C04 routing matrix, Android independent P0 XML proof or BETA release tag.
- Operator local `git rev-parse HEAD`: `081d670cfdd9be9aef96664371c87ab3262d09a3`. Local `npm run i18n:check` PASS (untranslated candidates nonblocking), `test:regression` **16/16 PASS**, `lint` **0 warnings/errors**, `build` PASS; earlier browser suite **47/47 PASS**. Later source changes cannot be called tested at this old SHA.
- Web added `qa/e2e/tests/onboarding-v49-authorization-readonly.spec.js` (source commit `e5bd54eb`), three new safety checks: manager V47→V49 link and reload without writes; server V49 seven steps only when Backend actually exposes them (otherwise explicit **SKIP**); child cannot access manager setup. This file postdates the operator's 47-test run, therefore the new checkout's expected collected count is **50** (47 existing + 3 new), assuming no other test additions. Do not claim 50 PASS or an enabled V49 when server is unavailable.
- Safe local new-head check: record `git rev-parse HEAD`, run `npm run test:regression` in `frontend`, run `npx playwright test --reporter=list` in `qa/e2e`; report exact pass/fail/skip and allow disabled server V49 check to SKIP honestly. Do not run real restore/reset/finalize/import against existing families.
- P0 #19 Android exact three lost-response JUnit XML and #15 complete owner-authenticated verified protective backup + recovery remain pending independent acceptance. **BETA HOLD**.


## STOP-SHIP Web QA regression — operator 50-case Playwright run 2026-11-16

- Uploaded full local `npx playwright test --reporter=list` run: **50 collected, 30 PASS, 20 FAIL, 0 SKIPPED, ~6.2m**. PowerShell printed `QA regression package: PASS` *after* `throw "Playwright failed"` because a subsequent command was executed; do not misinterpret this line. Actual Web E2E **RED**.
- Cluster: BKL002 safety planning, backup metadata, reset failure recovery, epoch all verbs, settings runtime, V49 manager navigation and server status; all fail to locate a Settings section such as `section[aria-label="Family data safety planning"]`, `#initial-family-setup`, or `section[aria-label="Build information"]`. Repeated `[RESET_QA_PAGEERROR] true is not a function` appears, strongly indicating a shared client runtime error; exact browser stack/causal source file NOT captured in console. Passing unrelated routes, cross-tenant Test/Test2 ACL, purchase-only history, V49 child authorization.
- Diagnose in source, not via 20 individual expectation relaxations. First collect actual browser `pageerror` **stack** from an isolated `settings-runtime.spec.js` and screenshot/context, verify running Vite dev server sources match current `git rev-parse HEAD` (restart stale dev server if necessary), then bisect Settings components/auth context. Do not skip tests, weaken reset safety/role permissions, clear actual family data, or enable execute.
- Prior CI all-jobs SUCCESS run 43888684640 at older SHA does **not** supersede newer Web E2E RED. Web QA acceptance is revoked on candidate until root cause is repaired and 50-case suite is rerun successfully (honest V49 disabled SKIP accepted).


## QA operator rerun after Settings ASI fix — 2026-11-16

- Operator ran isolated `settings-runtime.spec.js`: **1 PASS**. New complete Playwright collection: **51 tests, 48 PASS, 1 FAIL, 2 SKIPPED (1.8m)**. The previous cascade of 20 Settings runtime failures no longer reproduced, including all previous Settings failures except one instrumentation teardown error.
- Single failing case `reset-failure-recovery.spec.js` (preview 503 → reload 200): console shows expected read-only fail-closed UI and 200 recovery, but Playwright **browserContext.close** failed with `ENOENT` for missing `.playwright-artifacts-0/traces/...recording57.trace`. Do not claim functional acceptance solely from 503/200; test officially FAIL. Isolate rerun with `--trace=off` to distinguish Playwright trace artifact lifecycle from product failure, preserving assertions.
- Two V49 tests legitimately SKIP because seven-step server status and catalog bulk selection are not enabled/available for this QA workspace. A disabled backend is not accepted V49 feature parity. Role/tenant authorization and catalog/read-only tests passed.
- No assertion weakening or reset/restore execution authorized. Exact local HEAD of this 51-case run was not included. Retain BETA HOLD for Backend BKL002 and full onboarding as separate gates; Web E2E candidate is not yet all-pass.


## 2026-11-16 — Operator targeted Reset Recovery rerun PASS (trace-disabled)

- Operator reran `npx playwright test tests/reset-failure-recovery.spec.js --reporter=list --workers=1 --trace=off`: **4 PASS, 0 FAIL**, 10.6 s. Scenarios: preview HTTP 503 / Reload recovery 200, malformed Plan fail-closed, late Plan after navigation, late Preview after leaving Settings. No destructive execute.
- This distinguishes the earlier `browserContext.close ENOENT ... .playwright-artifacts-0/traces/recording57.trace` from a reproducible Reset Recovery assertion failure under trace-disabled conditions; underlying trace artifact lifecycle still needs investigation. It does not retroactively convert prior 51-case Playwright full run (48 PASS / 1 FAIL / 2 SKIP) into all-green.
- Next verify one full 51+-case E2E with `--trace=off` and pinned HEAD. Two server-disabled V49 cases remain legitimate SKIP until disposable Backend V49 enabled; do not count those as PASS. Keep BKL002 production execute and BETA HOLD.


## Operator full Playwright rerun — 2026-11-16, trace disabled

- Operator attached complete console output: `npx playwright test --reporter=list --workers=1 --trace=off`, **51 collected / 49 PASS / 0 FAIL / 2 SKIPPED, 1.3 minutes**, no command failure. Previous one-off trace teardown ENOENT did not recur when trace capture was disabled.
- Accepted as successful run of **all 49 executed scenarios**: BKL002 readonly preview/plan failures and recovery, backup metadata tenant fencing, four-locale UI smoke, epoch write-verb quarantine, child and cross-tenant 401/403 ACL, purchase-only Reward History GET-only, task/reward non-implicit writes, and Settings runtime.
- Two V49 manager workflow scenarios SKIPPED due to unavailable server-side feature for this QA workspace: (1) seven-step status; (2) catalog six-pack search/bulk selection until explicit confirmation. Do not count skipped cases as feature acceptance.
- This is **not 51/51 PASS** and does not prove full V49 onboarding or executable BKL002 backup/reset/restore. Trace tooling reliability remains open. Exact local HEAD in this execution not supplied (no SHA in pasted terminal output). Android exact three XML tests and same-SHA full required CI remain independently gated; BETA HOLD.


## Operator post-run SHA evidence — 2026-11-16

- Operator supplied `git rev-parse HEAD` immediately after successful full authenticated browser run: **`c00d81418c84ce66e897890e4ec2e9c072ada057`**; `git status --short` contains **only untracked** `qa/e2e/package-lock.json`, `qa/e2e/playwright-report/`, `qa/e2e/test-results/` (no tracked modifications shown).
- Nearby submitted `npx playwright test --reporter=list --workers=1 --trace=off` completed **51 collected / 49 PASS / 0 FAIL / 2 SKIPPED**. Associate run with reported post-run checkout SHA; absent test-time SHA print means no cryptographic proof HEAD did not move during run. Do not mislabel as `51 PASS`.
- GitHub commit `c00d814` confirmed. At verification, `main` was 4 commits ahead; new `onboarding-v49-synthetic-readonly.spec.js` means later main has expanded browser scenarios not covered by the 51 collected tests. Revalidate on an exact release candidate; never reset or activate a real family.


## QA pickup — Android Linux 98/98 and synthetic V49 expansion (2026-11-16)

- Verified GitHub Actions run [43893026715](https://github.com/o-kobylianskyi/family-points/actions/runs/43893026715): independent Android P0 job completed SUCCESS. Android owner evidence records exact checkout `09da72316257c4ce17a34caf57f8a7864605582f`, parsed JUnit artifact 30 suites / **98 testcases, 0 failures, 0 errors, 0 skipped**; all three exact lost-response cases unique/PASS, plus main Debug and AndroidTest APK assembly successful in optional manual full suite. No connected-device instrumentation claim. Issue #19 remains open pending same-SHA multi-job main CI and sign-off.
- New source `qa/e2e/tests/onboarding-v49-synthetic-readonly.spec.js` added **one** Playwright case after operator's `c00d814` 51-case run. It mocks enabled V49 seven-step server state and reads the actual 142-entry/six-pack catalog to exercise selection/search/reload safety without writes. This is a synthetic UI contract, **not** actual Backend V49 enablement. On a checkout including this commit, collected E2E count should be at least **52**, if no other cases are introduced.
- **Next user acceptance:** pull main after checking local changes, pin SHA, `cd qa/e2e; npx playwright test tests/onboarding-v49-synthetic-readonly.spec.js --reporter=list --workers=1 --trace=off`; then full suite with `--trace=off`. Maintain truthful 2+ V49 server-disabled SKIPS and never authorize real family reset/activation.
- Release still HOLD: BKL002 full owner-bound VERIFIED restore and production guarded execute not accepted; CI-OPT file-scope acceptance separate.


## Operator full V49 synthetic + Web browser acceptance — 2026-11-16

- Attached operator terminal: `npx playwright test --reporter=list --workers=1 --trace=off` **52 collected / 50 PASS / 0 FAIL / 2 SKIPPED**, 1.5 minutes, exit 0. New `onboarding-v49-synthetic-readonly.spec.js` case PASS, demonstrating synthetic enabled seven-step server state with real catalog 142 suggestions / 6 packs; search, bulk-selection and non-writing navigation.
- All 49 previously accepted executed browser checks also PASS: BKL002 preview/plan errors, security, tenancy, stale epoch and Rewards History. Two real server-dependent V49 scenarios still **SKIPPED**, not acceptance of actual server-enabled full onboarding.
- `--trace=off` avoids the earlier Playwright teardown ENOENT; trace artifact issue is unresolved. Do not publish browser trace/media or QA credentials.
- No tested SHA was printed inside this newly submitted execution log; latest known prior operator HEAD `c00d814...` predates synthetic test addition and cannot be attributed to this new 52-case run. Confirm exact operator HEAD and status before same-SHA signoff. BKL002 execute and BETA remain HOLD.


## Operator SHA after 50-pass Playwright — 2026-11-16

- User supplied root `git rev-parse HEAD` **`4d70145e00fdf8ed070c0e75af687b7ab6d2ffff`** after the immediately preceding full E2E `52 collected / 50 PASS / 2 SKIPPED / 0 FAIL` run (`--trace=off`). GitHub confirms this commit exists, message `fix(backup): accept JDBC PostgreSQL timestamptz timestamp representation in lease expiry check`.
- Operator `git status --short` reported only `?? qa/e2e/package-lock.json` (untracked; no modified tracked files). Do not commit it unintentionally. Prior E2E run itself did not print HEAD at test invocation; associate evidence with post-run checkout without claiming a guaranteed immutable tested SHA.
- GitHub comparison at acceptance time: `main` **4 commits ahead** of `4d70145e`; new Backend test, QA, release journal updates not covered by this checkout. This browser test does not accept Backend durable verified restore, full live V49, Android on device, or all-jobs exact-head CI. **BETA / production reset HOLD**.


## QA V52 retention/lease next acceptance — 2026-11-16

- Architect dispatch confirms P0 BKL002 requires tenant-scoped protected backup, owner reauth and physical retention before any reset execute. Existing code still returns HTTP 423 for execute; do not treat local staging as VERIFIED recoverability.
- Backend owner V52 packet: Flyway V52 `workspace_backup_restore_leases`; `WorkspaceBackupRetentionService` tenant/snapshot lease, max 15-minute bounded TTL, manual `expireEligible` max 100, SQL `FOR UPDATE SKIP LOCKED`, on-disk deletion with evidence and canonical key checks. **Automatic schedule and crash-safe orphan reconciliation absent**; synthetic VERIFIED state only for isolated Postgres tests.
- Operator attempt at SHA `1a3dde2ccde1b0e61bcf6d16358ae59434affaaa` failed in Java **testCompile** with ten overloaded JUnit assertions (no test ran); fixed by test-only `ab588a538960`. V52 local evidence must be rerun after this fix.
- QA Windows target command: `git pull --ff-only origin main` after status check; print `git rev-parse HEAD`; run `.\\mvnw.cmd "-Dtest=WorkspaceBackupRetentionPostgresTest,WorkspaceBackupArtifactRegistryPostgresTest,WorkspaceBackupRegistryStagingCoordinatorTest,WorkspaceBackupArtifactStagingServiceTest,WorkspaceBackupLocalArtifactStoreTest,WorkspaceRestoreReadinessMatrixTest,DisposableComposedNativeRestorePostgresTest,WorkspaceResetProofUpgradePostgresTest,TaskExcuseConstraintUpgradeTest,PopulatedDatabaseUpgradeTest,LegacyV35ConstraintRepairTest,WorkspaceSetupPostgresTest" test`. Report Surefire tests/failures/skips and compile status against exact SHA, not only Windows console success.
- Check negative assertions: unrecoverable REQUESTED/BACKING_UP must never become VERIFIED or receive lease; active lease bars cleanup, untrusted/noncanonical path cannot be unlinked; tenant A cannot release B; expired snapshot gets EXPIRED only after legitimate file lifecycle; migration V52 preserves existing rows and invariants. Never execute cleanup on real/DEV family data; tests only disposable Testcontainers.
- Keep browser QA 52 collected/50 PASS/2 server-V49 SKIP separate. Android Linux 98/98 green independent. BETA + production BKL002 execute HOLD.


## 2026-11-16 19:08 CET — Operator V52 PostgreSQL local acceptance PASS

- User supplied Maven terminal summary after the requested 12-class V52 Testcontainers/PostgreSQL suite: **Tests run: 25, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS**. Duration `01:12 min`; Maven finished `2026-11-16T19:08:08+01:00`. PowerShell `$LASTEXITCODE` guard did not throw and printed `V52 Backend QA: PASS`.
- Accept **operator-reported local targeted V52 test suite PASS** following test-only fix `ab588a5` for boxed assertions, covering requested retention/lease, encrypted artifact registry/staging, restore readiness/disposable Postgres, and Flyway migration regressions. Do not attribute each assertion to an individual test without per-class Surefire XML.
- Exact HEAD during this test not included in submitted output; ask for `git rev-parse HEAD` and `git status --short` to associate with source. No same-SHA GitHub four-job CI evidence implied. Production Reset Execute remains HTTP 423 and BETA HOLD; V52 success does not verify owner-bound archive recovery or automated retention.


## 2026-11-16 — V52 post-test SHA and V53 rehearsal assessment QA gate

- Operator local 12-class V52 Testcontainers package finished 2026-11-16 19:08:08 CET: **25 tests PASS, 0 FAIL/ERROR/SKIP; BUILD SUCCESS**. Immediately afterward local HEAD was `d3d6e6bb0a880cb43d25fc1d98b76e0a4874b641`, only untracked `qa/e2e/package-lock.json`, no tracked edits. This is **post-run checkout evidence**, not a timestamped pre-run SHA proof. `main` was already 16 commits ahead when compared; earlier run does not cover newer code.
- Latest Backend internal rehearsal assessment (`d03627c`, `5a837c4`, `0b2ec94`) uses persisted encrypted local:v1 artifact evidence and exact supported `MEMBER_GROUPS + MEDIA_ASSETS + MEDIA_BINDINGS` slice, verifies checksum/manifest and executes rollback-only PostgreSQL temp-table rehearsal. Returns `ROLLBACK_ONLY_REHEARSED` / `verifiedForRestore=false`; **never VERIFIED** and does not modify production data. This is NOT independent fresh DB full archive restore and does not unlock reset.
- **Next QA suite (source new, not yet operator-verified):** `WorkspaceBackupRehearsalAssessmentServiceTest,WorkspaceDisposableRestoreRehearsalPostgresTest,WorkspacePortableRelationalRehearsalPostgresTest,WorkspaceBackupRetentionPostgresTest,WorkspaceBackupArtifactRegistryPostgresTest,WorkspaceBackupRegistryStagingCoordinatorTest,WorkspaceBackupArtifactStagingServiceTest,WorkspaceBackupLocalArtifactStoreTest`. Require exact `git rev-parse HEAD`, Maven counts, test errors/skips and same-SHA protected CI separately. Tests run only with disposable PostgreSQL/Testcontainers; no production restore/reset/cleanup.
- Independently accepted Web E2E **50 PASS, 2 SKIP** and Android Linux **98/98 PASS** from earlier candidate revisions; new source changes to V49 validation must receive fresh acceptance. Release `HOLD`, production Reset Execute HTTP 423.
