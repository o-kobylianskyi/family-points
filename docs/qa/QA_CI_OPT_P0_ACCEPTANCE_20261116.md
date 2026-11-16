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
