# Backend B — DURABLE EXECUTION CHECKPOINT (current verified state)

**Last Architecture review:** 2026-11-18; read [architect dispatcher](../ARCHITECT_DISPATCH_LATEST.md) and [B active task](BACKEND_B_ACTIVE_ASSIGNMENT.md) on each `++`. This checkpoint replaces the original seed; source and current GitHub PR/CI override this file if newer.

## DONE — frozen B-INT-02 implementation (not QA accepted)
- Draft [PR #30](https://github.com/o-kobylianskyi/family-points/pull/30), branch `backend-b/b-int-02-integrated`, exact source **`b711371d6c78d77f1550002261ff0b6ac4bd941b`**. PR remains **UNMERGED** and GitHub currently reports `mergeable=false` against moving main; investigate integration/rebase conflict only on a new non-destructive QA integration candidate, never rewrite frozen PR SHA without coordination.
- Implemented bounded signed original-owner/reauth/RESTORE permit and native atomic target restore; supported scope and unsupported BLOCK categories: [canonical B handoff](../requests/B_INT_02_READY_FOR_QA_20261118.md).
- Executed on frozen PR SHA: [Actions #44392551759](https://github.com/o-kobylianskyi/family-points/actions/runs/44392551759), Maven PG18 **770 tests / 0F / 2E / 0S** (two Retention-owned fixture SQL CHECK errors), B native source-loss E2E **1/1 PASS**, original-owner PG18 **2/2 PASS**, Web/Android/Compose PASS. **Full all-component CI RED**. Not production ready.
- Retention owner already corrected its fixture SQL error on main at commit `a0210961687`; it is **not part of PR #30 SHA**. No combined exact-SHA full green verified.

## IN_PROGRESS / NEXT
- **Primary QA gate:** Architecture has activated QA for independent PR #30 B-INT-02 acceptance plus combined Retention-fixed integration candidate. Await one consolidated defects report; protect frozen PR code and no accidental source changes in its QA lane.
- **Backend B independent NEXT:** B-NEXT-01 read-only backend compatibility in separate nonoverlapping code/PR. Latest main commit `f82b59380155` documents this work; inspect actual branch/source before claiming it complete. On QA confirmed B-owned P0/P1, prioritize integrated B-INT-02-FIX and then resume NEXT.
- **Retention** separately owns Retention tests/service; no Backend B changes there. 
- **Blocked release:** full non-skipped combined CI pass; independent B package acceptance; unsupported category paths remain fail closed; BETA HOLD. No production Restore/Reset/destructive purge.

## Timeout-safe resume
1. Fetch PR #30 state, newest issue #15 comments and relevant branch/main commits. Determine DONE from actual code SHA, not text alone.
2. Do not repeat implemented B-INT-02. Resume B-NEXT-01 from last real source commit, or correct a new B-owned QA defect as one batch.
3. Before retrying a timed-out GitHub operation or CI run, look it up to avoid duplicate commits/runs.
4. Record next meaningful code checkpoint here with last SHA, exact affected files, tests PASS/FAIL/NOT_RUN and the NEXT action. Do not create a docs-only loop per microstep.

**Safety:** `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, physical purge OFF, BETA HOLD.


## Backend B follow-up — B-NEXT-01 frozen, B-NEXT-02 standalone
- **B-NEXT-01_READY_FOR_QA**: PR #31 `backend-b/b-next-01-compat-readonly`, SHA `bfa2f5dce8f6f02a5d43b1dff4a5d183f62b08aa`. Metadata registry + portable V1 + signed original V2 read-only adapters. 7/7 new tests PASS on [CI #4604](https://github.com/o-kobylianskyi/family-points/actions/runs/44395020403); full Java 772 tests / 1F / 0E / 0S, **Retention-only** fixture `WorkspaceBackupRetentionRollbackGapPostgresTest.committedExpiryRetainsBytesAndRepeatedSweepCannotReissueLease` line 179 `assertThrows(DataIntegrityViolationException)` expected but no exception. Web/Android/Compose PASS. Exact independent handoff comment [#6992170099](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6992170099). Branch diverged from newer main; Draft PR, no merge/release.
- **B-NEXT-02 IN_PROGRESS**: Draft [PR #32](https://github.com/o-kobylianskyi/family-points/pull/32), branch `backend-b/b-next-02-readonly-diagnostics`, HEAD `4257304d07d54d14ee0ab249ce11df649f1ecbee`. Only new read-only `WorkspaceRestoreReadOnlyDiagnosticsService` and 9-case `WorkspaceRestoreReadOnlyDiagnosticsServiceTest`. Tenant-fenced authenticated current workspace, receipt present/consistent or missing, permit pending/expired/consumed without receipt, V53 migration absent, no secrets/PII/HTTP/DB write/auto retry. Original CI #4610 found two ambiguous Mockito `queryForList` testCompile calls; corrected @`4257304d`. Follow-up exact-SHA [CI #4611](https://github.com/o-kobylianskyi/family-points/actions/runs/44402545887) in progress at checkpoint; no PASS asserted.
- **NEXT:** inspect #4611 Java compile + 9 tests, fix only own diagnostics files in same PR, then publish one `B-NEXT-02_READY_FOR_QA` with exact SHA. Independently QA tests frozen B-INT-02 SHA; any B-owned P0 preempts this work. BETA HOLD, restorable=false, replaySupported=false, Reset HTTP423, physical purge OFF.


## B-NEXT-02 final source handoff — 2026-11-18

**Status:** `B-NEXT-02_READY_FOR_QA` (not release or full-CI green). Frozen [Draft PR #32](https://github.com/o-kobylianskyi/family-points/pull/32) source `4257304d07d54d14ee0ab249ce11df649f1ecbee`. New stand-alone internal service/test only: `WorkspaceRestoreReadOnlyDiagnosticsService`, `WorkspaceRestoreReadOnlyDiagnosticsServiceTest`. Authenticated selected-family SQL read-only receipt and one-use permit state report; receipt epochs consistency; no secret/token/PII disclosure, HTTP route, retry, production activation, SQL writes or scheduler. 9 addressed test cases.

**Actual** [CI #4611](https://github.com/o-kobylianskyi/family-points/actions/runs/44402545887) exact SHA: B-NEXT-02 9/9 PASS; Web Android Compose SUCCESS; Java job and exact-SHA Full CI FAIL only due Retention-owned `WorkspaceBackupRetentionRollbackGapPostgresTest.committedExpiryRetainsBytesAndRepeatedSweepCannotReissueLease`, expecting a PostgreSQL constraint violation that is not thrown. B has not touched Retention files. Cross-stream handoff [Issue #15 comment #6992541967](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6992541967).

**QA combined candidate verification (not B-owned):** [PR #33](https://github.com/o-kobylianskyi/family-points/pull/33) SHA `6e8dfd1415f7a5e757a60b3703e1fead4a0e2044` [CI #44402585426](https://github.com/o-kobylianskyi/family-points/actions/runs/44402585426) Java FAIL on same Retention fixture; other 3 components PASS, exact-SHA full attestation FAIL. Formal B-INT-02 QA acceptance outstanding, frozen PR #30 untouched.

**NEXT/owner:** preauthorized B-NEXT-01 and B-NEXT-02 both source READY_FOR_QA. No additional independent B queue item assigned. **WAITING_ON_QA** for consolidated B-INT-02 defect report and Architecture's next assignment; any confirmed B-owned P0/P1 preempts. Keep PR #30/#31/#32 drafts unmerged, no restore execute/replay enable, Reset HTTP423, physical purge OFF, BETA HOLD.


## 2026-11-19 — B-INT-02-FIX-AUTH-01 P1 priority preemption

Architecture [Issue #15 comment #7004499357](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-7004499357) reactivated B after independent QA #7004443855 found `BINT02-AUTH-GOOGLE-BUDGET-01`: Google denied proofs did not count in durable RESTORE budget.

**Active source:** [Draft PR #36](https://github.com/o-kobylianskyi/family-points/pull/36), `backend-b/b-int-02-fix-auth-01`, based on **green combined QA PR #35 SHA `7a7b17fc`**. Latest commit `49b7a89dce7baddef576dec93a136938c1d2455d`. No edits to frozen PR #30/35, `main`, Retention or Web. In `WorkspaceRestorePermitService.issueGoogle` rejected signed token (Google's `ResponseStatusException`), stale/future issued-at, absent/mismatched linked identity now call existing REQUIRES_NEW `limiter.recordFailure` once before throwing. Owner/scope/other archive failures are excluded; verified proof calls `clearSuccess`. Independent `WorkspaceRestoreGoogleBudgetTest` 6 cases; `WorkspaceRestoreGoogleBudgetPostgresTest` actual Spring proxy plus PG18 persistence through outer rollback, five-attempt lock, tenant isolation, clear; extended two-PG18 source-loss E2E wrong key/tamper/foreign member/wrong currency and two-writer race.

**CI evidence at checkpoint:** First PR run [#4633](https://github.com/o-kobylianskyi/family-points/actions/runs/44493119781) failed `testCompile` only (checked `InterruptedException` in new concurrency test). Fixed in `49b7a89d`. Latest [#4634](https://github.com/o-kobylianskyi/family-points/actions/runs/44493294227) currently Java/PG18 IN_PROGRESS; Web+Compose already successful, Android in progress. DO NOT label code READY_FOR_QA until actual new test results and compile are confirmed. NEXT read #4634 exact-SHA Surefire, fix any B-owned defect in same PR, then one `B-INT-02-FIX_READY_FOR_QA` with actual results and blocked negative cases. No full CI duplication. `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, physical purge OFF, BETA HOLD.


## 2026-11-19 — B-INT-02-FIX-AUTH-01 final frozen QA handoff

**READY_FOR_QA_FIX / not release:** [Draft PR #36](https://github.com/o-kobylianskyi/family-points/pull/36), exact immutable source SHA `e487d909d59ef7f0637bf04e47f911a14395861e`, derived from green PR35; original PR30/35 not edited. Production `WorkspaceRestorePermitService.issueGoogle` now increments same dedicated Spring REQUIRES_NEW failed-attempt budget on rejected signed JWT, stale/future issued-at and missing/wrong linked subject, only once per verified owner rejected proof. No counting unrelated scope/auth/epoch/archive errors, no provider bypass.

**New/extended tests:** 6/6 Google budget mocks, 1/1 real PG18 Spring-managed budget against outer transaction rollback (five denials → sixth locked; independent family; success clear), 1/1 two-PG18 archived-source-loss native E2E extended wrong AES key, modified bytes, foreign mapped member, mismatched point code and two simultaneous target JDBC writers (one success/receipt, duplicate rejected). Pinned [CI #4636](https://github.com/o-kobylianskyi/family-points/actions/runs/44494288784): 777 Java tests 0F/0E/0S BUILD SUCCESS; Web, Android, Compose and Full exact-SHA attestation SUCCESS, all 7 CI jobs SUCCESS. No Retention, Web or production flags touched.

**Cross-stream delivery:** [Issue #15 #7004978791](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-7004978791) and [full canonical B-INT-02-FIX handoff](../requests/B_INT_02_FIX_AUTH_01_READY_FOR_QA_20261119.md). Remaining independent QA gaps: real provider/JWK token outcome, complete 2-members×2-currencies source restore, some unsupported nonempty category inputs. Do not claim them PASS without execution. Await QA/Architecture consolidated next action; no speculative new B work. **Production Restore OFF, Reset Execute HTTP423, physical purge OFF, BETA HOLD.**


## 2026-11-19 — B-INT-02-FINANCE-MATRIX-01 assigned by Architecture

Architect directive [Issue #15 #7005414411](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-7005414411) conditionally accepts only the tested one-member/one-currency B-INT-02 synthetic core in frozen PR #36, and preauthorizes broader independent real PG18 coverage. Source PR36 `e487d909d59ef7f0637bf04e47f911a14395861e` remains untouched.

**Current:** [Draft PR #37](https://github.com/o-kobylianskyi/family-points/pull/37), branch `backend-b/b-int-02-finance-matrix-01`, SHA `d708bd52d1738b3d401c71fb0312223a5fe26402`. Added ONLY `WorkspaceNativeV2RestoreFinanceMatrixPostgresTest.java`, real two PG18 Testcontainers authenticated sealed source→source stop→target, source/target roster 2 members and currencies POINTS/MINUTES, 2 task/reward parents, 2 instances/purchases, ledger 7 rows and four expected member×currency totals (+3,+7,+11,-2). Extended actual target denies wrong AES key, changed bytes, foreign member map, currency-code mismatch, preexisting target task, stale family epoch, DB-clock-expired persisted permit; then forced atomic rollback and synchronized concurrent same-permit two-writer race, exactly one success and receipt, replay denial. **Production code unchanged**, no migration, Retention/Web/frozen PR edits.

**Verification pending:** [Actions #4644](https://github.com/o-kobylianskyi/family-points/actions/runs/44499403001) source SHA `d708bd52`; workflow was IN_PROGRESS at checkpoint. DO NOT claim PASS or READY_FOR_QA until actual compile, PG18 E2E and exact-SHA results. If a test fails, make one targeted fix on PR37 and inspect new exact SHA. After complete PASS, one B-INT-02-FINANCE-MATRIX-01_READY_FOR_QA to Issue #15 + canonical handoff. Production Restore OFF, Reset Execute HTTP423, purge OFF, BETA HOLD.


## 2026-11-19 — B-INT-02-FINANCE-MATRIX-01 READY_FOR_QA

**FROZEN** isolated [Draft PR #37](https://github.com/o-kobylianskyi/family-points/pull/37), SHA `d708bd52d1738b3d401c71fb0312223a5fe26402`, based on prior QA-green/accepted-bounded PR36 `e487d909`. Added one PG18 source-loss financial 2-member×2-currency test and no production code. [Actual CI #4644](https://github.com/o-kobylianskyi/family-points/actions/runs/44499403001): **778/778 Java tests, 0F/0E/0S, BUILD SUCCESS**, all 7 GitHub jobs SUCCESS (Web/Android/Compose/exact SHA too). New `WorkspaceNativeV2RestoreFinanceMatrixPostgresTest` 1/1 PASS, real independent source & target PG18, source stopped after signed export, two members/2 currencies and 7 ledger rows, exact cross-product sums [+3,+7,+11,-2], real owner BCrypt permit and native FK remapping; negative wrong key, tamper, foreign member, wrong point code, preexisting target, stale epoch, expired DB permit, rollback and competing two PG writers exactly one receipt. No B production defect observed; no B production change.

Canonical [READY_FOR_QA handoff](../requests/B_INT_02_FINANCE_MATRIX_01_READY_FOR_QA_20261119.md) + [Issue #15 comment #7005680107](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-7005680107). **NEXT WAITING_ON_QA/Architecture** for review of this immutable SHA and any confirmed new B-owned defect; no additional speculative packages, no merge. Real provider Google token flow still unverified. Safety: `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, physical purge OFF, BETA HOLD.


## 2026-11-19 — FINANCE-ID-ALIAS-01 P2 QA coverage correction (in progress)

Per architect [Issue #15 #7006281903](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-7006281903) and QA [#7006219272](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-7006219272), correct one **test-only** risk: fresh source and target PG18 identity sequences could coincide, so previous 2×2 balances did not prove nonidentity native foreign-key remapping.

**Source checkpoint:** [Draft PR #38](https://github.com/o-kobylianskyi/family-points/pull/38), branch `backend-b/finance-id-alias-01-nonidentity`, based on immutable PR37 `d708bd52d1738b3d401c71fb0312223a5fe26402`, candidate SHA `e99b8402890c3a4e913d757ffb5345002a611231`. One modified file ONLY: `WorkspaceNativeV2RestoreFinanceMatrixPostgresTest.java`. Added legitimate foreign-family target padding for member, currency, task/reward definition, task instance and reward purchase IDs; assert original vs target IDs differ and no cross-source collision, inspect actual native target parent/child IDs + polymorphic TASK/REWARD_PURCHASE ledger `source_id` mapped to target, exact 2×2 sums [+3,+7,+11,-2], original negative and rollback/race scenarios unchanged; padding-family rows preserved. **No production code, schema, PR37, Retention or Web edits.**

**Verification:** existing PR-triggered [CI #4653](https://github.com/o-kobylianskyi/family-points/actions/runs/44505301209) on exact `e99b8402` IN_PROGRESS at checkpoint, Java+PG18 and Android pending, Web and Compose success. Do not claim targeted PG18 PASS or `FINANCE_ID_ALIAS_01_READY_FOR_QA` until actual Surefire result. NEXT inspect #4653, fix only test fixture if a genuine test failure appears, then one frozen QA handoff to Issue #15. No duplicate full CI. Safety: Restore and Reset Execute OFF, physical purge OFF, BETA HOLD.


## 2026-11-19 — FINANCE-ID-ALIAS-01 READY_FOR_QA (frozen source)

**P2 coverage issue CLOSED at producer test-evidence level**: new isolated [Draft PR #38](https://github.com/o-kobylianskyi/family-points/pull/38), exact source `e99b8402890c3a4e913d757ffb5345002a611231`, based on PR37 `d708bd52`, one test file changed. Legitimate other-family target padding forces nonidentity source→target IDs for 2 members, 2 currencies, task/reward/instance/purchase. Exact target FK and polymorphic point-ledger `source_id` verified with source counterpart ID inequality, source stopped after AES-GCM FPB1 archive, same 2x2 balances and all negative cases. No production code, DDL, PR37, Retention, Web change.

**Observed non-skipped [CI #4653](https://github.com/o-kobylianskyi/family-points/actions/runs/44505301209)**: Java 21/PG18 **778/778 PASS 0F/0E/0S**, new nonidentity matrix test **1/1 PASS**, all 7 GitHub CI jobs success including Android/Web/Compose/exact SHA attestation. Full [canonical READY_FOR_QA handoff](../requests/FINANCE_ID_ALIAS_01_READY_FOR_QA_20261119.md), Architect/QA routing [Issue #15 comment #7006496909](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-7006496909).

**NEXT:** `WAITING_ON_QA` independent PR38 diff/CI acceptance and conflict-aware Architecture integration assignment. No same-SHA repeated full CI, no speculative new feature. Release gates remain BETA HOLD, restorable/replaySupported false, Reset Execute HTTP423, physical purge OFF.


## 2026-11-19 — B-SECURITY-CLOSURE-02 (active, not yet QA handed off)

Architecture issue #15 [#7011348258](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-7011348258) assigned isolated synthetic security closure from PR39 accepted source `e6b65b85580a8fbb946853e03e8356b20abdef05`. New [Draft PR #41](https://github.com/o-kobylianskyi/family-points/pull/41) branch `backend-b/b-security-closure-02`, SHA `f1d722b6ad698d09ee661112a2edaf34b1f61f68`.

**Changed paths:** production `GoogleTokenVerifier.java` introduces package-private explicit JWKS URI injection seam for local-only tests, public Spring-selected `@Autowired` constructor remains pinned to `https://www.googleapis.com/oauth2/v3/certs`; no untrusted external JWKS URI/feature flag. New `GoogleTokenVerifierLocalJwksCryptoTest` tests true ephemeral RSA/RS256 signed JWTs against disposable local HTTP JWKS with bad signature/kid/issuer/audience/multi-audience azp/subject/time. New `WorkspacePortableUnsupportedSourcePostgresTest` populates real Flyway PostgreSQL18 `reputation_transactions` and verifies `exportWithHistory` rejects it pre-seal and no target permit/receipt/finance mutation. New `WorkspaceRestorePermitExactBoundaryTest` pure deterministic 5min/nanosecond validation; **PG DB wall-clock full 5-minute boundary NOT verified**. No production Restore route, permit/engine/Retention/Web or release changes.

**Verification currently pending:** [CI #4669](https://github.com/o-kobylianskyi/family-points/actions/runs/44546321751) exact SHA `f1d722b6`. Web and Compose completed SUCCESS; Java PG18 and Android IN_PROGRESS at last observation. No READY_FOR_QA until actual targeted security test evidence. Externally provider-issued Google ID-token and authorized browser flows remain operator-gated / unverified. Safety `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, physical purge OFF, BETA HOLD.
