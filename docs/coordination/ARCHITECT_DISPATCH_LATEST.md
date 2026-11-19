# FamilyPoints — CURRENT ARCHITECT DISPATCH (read FIRST on every `++`)

**Effective:** 2026-11-18 18:26 CET. **Authoritative control for live workstream activation and assignments.** Owner communicates primarily with Architecture; owner starts role chats by sending only `++`. GitHub is the durable coordination queue. The user need not copy technical assignments into chats.

## LATEST ARCHITECT CONTROL — 2026-11-19: B-INT-02/RETENTION FULL CI GREEN

**MAJOR GATE CLEARED:** independent QA combined draft [PR #35](https://github.com/o-kobylianskyi/family-points/pull/35), immutable `7a7b17fca9937efcc4e74893082ceb25638a9657`, [Actions #44490270791](https://github.com/o-kobylianskyi/family-points/actions/runs/44490270791) **ALL SEVEN JOBS SUCCESS**, non-skipped exact-SHA attestation PASS, Maven/Postgres18 **770/770 PASS (0 failures/errors/skips)**; native two-PG18 source-loss Restore **1/1 PASS**, Original Owner **2/2 PASS**, Retention rollback-gap **4/4 PASS**, Retention Postgres **2/2 PASS**, Web/Android/Compose SUCCESS. Retention V51 fixture correction @`d61c87c` included exactly in candidate. PR #35 is **DRAFT/UNMERGED**, original B PR #30 and independent B-NEXT PR31/PR32 still frozen; do not imply all PRs on main or release approved.

**NEXT OWNER ACTION: activate QA `++` ONLY.** [QA active assignment](handoffs/QA_ACTIVE_ASSIGNMENT.md) now directs ONE bounded high-risk security/negative acceptance matrix for B-INT-02 without repeating all-four CI: real server provider Google freshness/subject/audience where testable, 5-minute permit TTL/15-minute rollback-resistant rate limit, simultaneous two-writer permit race, multiple members/currencies and ledger parity, foreign-family mapping, wrong source key/tamper, stale epoch, unsupported nonempty categories, preexisting target conflict. Report PASS/FAIL/UNVERIFIED per scenario on exact SHA; only reproducible B-owned defect -> ONE B correction package. If browser/operator identity needed, separate operator packet, no real family. After B security gate, QA may test frozen B-NEXT-01/02 and WEB-02, while WEB-01 Chromium remains separately operator-blocked.

**WAIT:** Backend B (no confirmed B-owned defect), Backend Retention (integrated PG18 now green, no physical purge authorized), Web, Android, Designer, Release; Backend A/C/D CLOSED. **BETA HOLD remains:** `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, purge OFF, no merged/deployed Restore. Do not accidentally launch old red PR runs or duplicate full CI.

## LATEST ARCHITECT AUTOMATION DECISION — 2026-11-19 (overrides earlier manual test gate)

QA [Issue #15 operator packet #7003947260](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-7003947260) confirmed that available connected GitHub actions **cannot dispatch a focused Maven-only workflow** and local QA/Retention sessions cannot run Docker. **Architecture replaces manual prerequisite with one non-redundant automated integration gate.** This decision intentionally supersedes the earlier instruction “no Full CI until focused PG18”: one NEW immutable combined B-INT-02 PR30 + corrected Retention `d61c87c` source candidate, with ONE full non-skipped CI, gives both corrected PG18 class evidence and all-component evidence. No blind reruns of old SHA; no source loosening or production risk. Owner does NOT need to run Maven/Docker locally.

**ACTIVATE QA `++` NOW**: read updated [QA_ACTIVE_ASSIGNMENT.md](handoffs/QA_ACTIVE_ASSIGNMENT.md), create new draft isolated integration candidate containing ONLY frozen B PR30 code plus exact Retention corrected test blob `2698ae32787683fb1a34f96d65bebd7dfe523879` from `d61c87c`; verify diffs, then one PR-triggered full CI. Report exact Java Retention class F/E/S, full Maven, Web, Android, Compose and attestation; on FAIL first stack and owning stream; on PASS continue B security negatives/acceptance. Frozen PR30/31/32/33/34 remain unmerged; do not edit main beyond coordination. Retention WAIT for actual new failure; Backend B WAIT for B-owned negative failure; Web/Android/Designer/Release WAIT. Physical purge OFF, `restorable=false`, `replaySupported=false`, Reset HTTP423, BETA HOLD.

## LIVE DISPATCH — 2026-11-19 (RET-CHECK-03 source complete, QA runner next)

**Verified:** Backend Retention source-only correction `d61c87c795aa166f2871ea59df0233ee93dba928` / ledger `c2507976` committed to main. Exact V51 CHECK semantics and hybrid retained artifact fixture now reviewed; see [Retention #7003753445](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-7003753445). Latest Retention [#7003852119](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-7003852119) explicitly says NO executable Maven/PG18 evidence: this lane has no local runner or test container. No new CI for this corrected blob is confirmed. Historic QA PR34 [#44406980588](https://github.com/o-kobylianskyi/family-points/actions/runs/44406980588) is **770 tests / 1 error** on **OLD fixture** and cannot count as verification. Do NOT repeatedly activate Retention to write another correction.

**ACTIVATE NOW: QA `++` ONLY.** [QA_ACTIVE_ASSIGNMENT.md](handoffs/QA_ACTIVE_ASSIGNMENT.md) now prioritizes focused disposable PG18 proof of latest Retention fixture at immutable corrected blob: try an available *targeted* GitHub Actions runner (not old rerun); if unavailable, write a separate exact `OPERATOR ACTION REQUIRED — RET-CHECK-03-PG18` for a machine with Maven+Docker. Do not start another expensive Full CI prior to focused proof. On focused PASS, QA makes ONE new combined B PR30 + corrected Retention candidate and ONE full exact-SHA four-component CI. QA still owes high-risk B negative security coverage; no B acceptance solely from main happy-path E2E.

**WAIT:** Backend Retention (source correct awaiting proof), Backend B (frozen PR30, PR31, PR32; fix confirmed B-owned faults only), Web, Android, Designer, Release; Backend A/C/D CLOSED. No production Restore/Reset/retention purge, deploy or merge; `restorable=false`, `replaySupported=false`, HTTP423, BETA HOLD.

## LIVE DISPATCH — 2026-11-19 09:12 CET (supersedes earlier checkpoints)

**Verified latest QA #6993294126:** separate QA [PR #34](https://github.com/o-kobylianskyi/family-points/pull/34) exact `ca5a8fbe3119ea520a7cd773e9b50bef8583fd0a`, [full CI #44406980588](https://github.com/o-kobylianskyi/family-points/actions/runs/44406980588): **770 Maven/PG18 tests, 0 failures, 1 error, 0 skipped**. Native B-INT-02 source-loss Restore **1/1 PASS**, owner foundation **2/2 PASS**, Web+Android+Compose SUCCESS, full attestation FAIL. Sole error Retention-only `WorkspaceBackupRetentionRollbackGapPostgresTest.committedExpiryRetainsBytesAndRepeatedSweepCannotReissueLease:180` attempting `storage_key=NULL` on `EXPIRED` with non-null evidence, rejected by `ck_protective_backup_artifact_evidence_complete`. Prior PR33 showed inverse assumption about second null evidence update; **must derive fixture behavior from actual Flyway CHECK and row state rather than alternating assertThrows**. Current main source fix `a8181ba` is **falsified for this tested fixture**. No B-owned failing test in this run.

**NOW**:
1. **ACTIVATE Backend Retention with `++` ONLY** — [RET active assignment](handoffs/BACKEND_RETENTION_ACTIVE_ASSIGNMENT.md) now contains precise `RET-CHECK-03`: inspect exact V50–V53 PG CHECK and fixture, make ONE deterministic retention-only correction, then get actual focused PG18 test evidence; post `RET_CHECK_03_READY_FOR_QA`. Do not weaken CHECK, don't trigger full CI before focused proof.
2. **QA WAIT** until Retention code handoff and targeted evidence, then `++` to build ONE new immutable combined candidate (frozen B-INT-02 PR30 + fixed Retention) and one non-skipped full gate. QA may separately design missing high-risk B negative coverage, but cannot declare release/acceptance without actual results. WEB-01 manual Chromium remains separately blocked.
3. **Backend B WAIT**; frozen B-INT-02 PR30, B-NEXT-01 PR31, B-NEXT-02 PR32 ready for independent review but no more speculative B tasks. Confirmed B-owned QA P0/P1 triggers one consolidated B-FIX pass, not more microcycles.
4. Web / Android / Designer / Release WAIT; Backend A/C/D CLOSED.

**Safety**: `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, physical purge OFF, BETA HOLD. No live writes, merges, deployment or unapproved manual tests.

## CURRENT ACTUAL CONTROL — 2026-11-18 22:44 CET (overrides older checkpoints below)

**Verified latest independent QA**: [QA_B_INT_02_REPORT #6992310038](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6992310038), plus [CI #4612 follow-up #6992543385](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6992543385). B-INT-02 draft [PR #30](https://github.com/o-kobylianskyi/family-points/pull/30) frozen `b711371d6c78`, not merged. QA separate compatible draft [PR #33](https://github.com/o-kobylianskyi/family-points/pull/33) `6e8dfd1415f7` is mergeable, but its [full CI #44402585426](https://github.com/o-kobylianskyi/family-points/actions/runs/44402585426) finished **770 Maven / 1 failure / 0 errors / 0 skips**, Web+Android+Compose PASS, full attestation FAIL. **Exact sole failure:** Retention `WorkspaceBackupRetentionRollbackGapPostgresTest.committedExpiryRetainsBytesAndRepeatedSweepCannotReissueLease:188`, expected `DataIntegrityViolationException` when `artifact_sha256=NULL` in terminal `EXPIRED` state but PG18 accepted it. This is the SECOND invalid-evidence assertion, not the prior `storage_key=NULL` case. This is **not a Backend B code failure**.

**Retention response ALREADY COMMITTED while reviewing:** [`a8181ba9b3b298`](https://github.com/o-kobylianskyi/family-points/commit/a8181ba9b3b2980b0f7756384d3e1d431856cf90) to `main`, changes the RET-only fixture to accept schema-specific `EXPIRED` metadata nullability with read-only `BLOCK_DIGEST` classification, while retaining `BACKING_UP` invalid-evidence CHECK denial; accompanying coordination `94a1305`. **Fix is NOT YET VERIFIED via PG18 at combined SHA** and is NOT in frozen PR #33. No further code edit for the already fixed failure unless new test evidence.

**Backend B independently progressed:** [B-NEXT-01 PR #31](https://github.com/o-kobylianskyi/family-points/pull/31) SHA `bfa2f5d`, 7/7 targeted PASS, whole 772/1F Retention; [B-NEXT-02 PR #32](https://github.com/o-kobylianskyi/family-points/pull/32) SHA `4257304d`, 9/9 targeted PASS, same Retention whole CI failure. BOTH draft, independent, frozen; DO NOT merge or mark all-green. B-INT-02 production holds unchanged.

**Owner next `++` activation and dependencies:**
1. **QA — ACTIVATE / PRIORITY 1**: first verify the `a8181ba` Retention fix with focused disposable PG18 tests on one exact SHA or logs. If verified, prepare ONE new, separate combined candidate retaining frozen B-INT-02 source and updated Retention fixture (do not rewrite PR30, do not force merge). Run ONE non-skipped all-four exact-SHA CI. Independently record missing target Restore security coverage (provider freshness/subject, 5min TTL, concurrent permit, multi-member/multi-currency, foreign tenant, wrong key/tamper/stale epoch). These are P1 UNVERIFIED; only a confirmed code defect goes to B; no premature release approval. Queue separate PR31/PR32 QA after critical B gate.
2. **Backend Retention — CONTINUE**: review actual single failure from #44402585426, ensure `a8181ba` addresses it, seek focused PG18 evidence, report `RET_FIX_READY_FOR_QA` with exact SHA/expected assertion. Do not create further speculative test-only rewrites or weaken DB constraint. Physical deletion OFF.
3. **Backend B — WAIT for consolidated B-owned QA defects** (PR30, PR31, PR32 frozen); do not create B-NEXT-03 or duplicate CI. If QA confirms code defect, one integrated fix pass; otherwise no action.
4. **Web — WAIT** (WEB-01 operator Chromium blocked; WEB-02 source READY_FOR_QA, WEB-03 only actual confirmed independent defects). Android, Designer, Release WAIT; A/C/D CLOSED.

**Release HOLD:** `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, retention purge OFF, BETA HOLD. No live database mutation, deployment or production restore authorized by this update.

## MOST RECENT ARCHITECT CHECK — B-INT-02 QA GATE (2026-11-18)

**Use this section instead of older historical checkpoints below.** Newest observed main before this coordination update `f82b59380155`: Backend B documented independent B-NEXT-01. B-INT-02 **FROZEN** draft PR [#30](https://github.com/o-kobylianskyi/family-points/pull/30) exact `b711371d6c78d77f1550002261ff0b6ac4bd941b`, QA acceptance still **NOT REPORTED** in Issue #15 at Architecture check. [CI #44392551759](https://github.com/o-kobylianskyi/family-points/actions/runs/44392551759) had 770/0F/2E/0S; two Retention fixture errors fixed on main @`a0210961`, not on frozen PR; **no combined green verified**. GitHub reports PR #30 `mergeable=false` against moving main: QA/Architecture must inspect actual merge conflict and create safe separate integration candidate SHA before full-gate claims. Do not force merge or rewrite frozen evidence.

**ACTIVATE NOW**: (1) **QA `++` FIRST**: independent B-INT-02 PR #30 source/PG18/security review, identify merge integration issues, one consolidated QA report; then one non-skipped combined SHA full pipeline with Retention corrected fixture. (2) **Backend Retention `++`**: verify corrected PG18 tests and handle any retention-only CI failures, no purge. (3) **Backend B `++`**: resume independent B-NEXT-01 from actual separate source; B-INT-02-FIX only after QA B-owned defects; maintain frozen branch and no conflicting edits. (4) Web WEB-03 only verified new issue, otherwise WAIT. Android/Designer/Release WAIT. B A/C/D CLOSED.

**Safety unchanged:** production Restore disabled, Reset Execute HTTP423, physical purge OFF, BETA HOLD; no manual user test required to proceed with B-INT-02 synthetic/PG18 QA. WEB-01 Chromium remains separate operator-blocked QA item.

## CURRENT DISPATCH CHECKPOINT — 2026-11-18, after QA_BASELINE_READY

**Architecture verified latest GitHub Issue #15:** QA posted `QA_BASELINE_READY` (comment [6986423111](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6986423111)); its nonblocking baseline work is **COMPLETE**. Last actual fully executed verification reported by QA: [CI #44346461826](https://github.com/o-kobylianskyi/family-points/actions/runs/44346461826) QA PR SHA `425fbc8e`, Maven/PG18 **763 tests 0F/0E/0S**, Web/Android/Compose/exact SHA SUCCESS, but this is **not current main HEAD**. Historical QA negative draft [PR #27](https://github.com/o-kobylianskyi/family-points/pull/27) at `053ef322`: 766 tests, 2 failures (synthetic `authorization_header`, nested `x-api-key` aliases), kept UNMERGED; B must preserve fail-closed privacy and disposition within his large package, no speculative QA interruption.
- **OWNER: activate / continue Backend B with `++` NOW**, [B active work file](handoffs/BACKEND_B_ACTIVE_ASSIGNMENT.md). At this check, no B-INT-02 implementation commits or `READY_FOR_QA` after the new dispatcher were observed. B should do the full implementation pass, not another docs-only planning response.
- **QA: WAIT / DO NOT ACTIVATE** now. QA will be reactivated by Architecture **only once B posts `READY_FOR_QA`**, to run one aggregate formal acceptance. Baseline phase complete; no duplicate CI.
- **Web and new Backend Retention are optionally activatable now on independent tasks; Android, Designer, Release WAIT; Backend A/C/D CLOSED**.
- **Architecture next trigger:** B handoff `READY_FOR_QA` or one genuine blocking contract issue. No background process implied; owner manually starts the B chat with `++`.
- **Safety unchanged:** `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD.

## ARCHITECT LIVE CHECKPOINT — 2026-11-18 19:57 CET

Architecture reviewed newest `main` through `18afa543d37e` and latest Issue #15 handoffs. **NEW independently ready Web package and parallel Retention code changes are confirmed; Backend B's B-INT-02 READY_FOR_QA is NOT confirmed**.

- **ACTIVATE QA NOW** with `++` for **WEB-01** independent Chromium five-case package [Issue #15 comment 6987315284](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6987315284); exact source fix SHA `6cc24e98dbc4`. Web Node **39/39**, lint/i18n/build PASS on [run #44366630311](https://github.com/o-kobylianskyi/family-points/actions/runs/44366630311) but Chromium NOT_RUN. Run synthetic 5-case against authorized disposable QA login; if no credential/session, record BLOCKED and request operator action **separately**, never use real family. Do not wait for Backend B to finish. Collect one WEB-01 PASS/FAIL/blocked handoff and then inspect Retention targeted verification when evidence is available.
- **CONTINUE Backend B NOW** with `++`: B-INT-02 full native Original Owner/permit/atomic Restore package. No new substantive B-INT-02 code after previous main review observed. No docs-only intermediate handoff. Do NOT touch retention-owned files.
- **CONTINUE Backend Retention NOW** with `++`: RET-01 SQL-first/never-unlink commits `36c0216` and RET-02 read-only `ef9f5af`; latest `18afa543` test compilation disambiguation on main. Requires real targeted PG18 compile/tests, then `RET-01_READY_FOR_QA` / `RET-02_READY_FOR_QA` when honest; **physical purge finalizer remains BLOCKED**, no scheduler/purge enable. Do not create another speculative cleanup design without evidence.
- **CONTINUE Web NOW** with `++` only on **WEB-02**, because WEB-01 `READY_FOR_QA` is frozen @`6cc24e98`; WEB-02 must not edit WEB-01 source/fixture under QA. WEB-03 if confirmed independent faults only.
- **WAIT** Android, Designer, Release. **CLOSED** Backend A/C/D.
- **Dependency management:** QA can test WEB-01 while B codes Restore, Retention runs own targeted PG18, Web codes WEB-02. QA must not demand full B-INT-02 test until `READY_FOR_QA`; QA may accept Retention separately upon handoff. Use one aggregate defect report per frozen package and preserve original SHA attribution. Prior baseline QA PR 763/763 PASS is not current HEAD attestation.
- **Operator action:** none at present. If Web Chromium needs a real authorized session, QA must separately produce `OPERATOR ACTION REQUIRED` packet with disposable environment, steps, evidence and risks; Architecture will deliver it.
- **Safety:** `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, physical deletion OFF, BETA HOLD, no production data mutations or deployment.

## CURRENT ACTIVE DISPATCH — B-INT-02 READY_FOR_QA (2026-11-18)

**Verified from GitHub:** Backend B posted [Issue #15 handoff 6991088205](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6991088205). Draft [PR #30](https://github.com/o-kobylianskyi/family-points/pull/30) source is on **separate branch** `backend-b/b-int-02-integrated`, frozen **`b711371d6c78d77f1550002261ff0b6ac4bd941b`**; it is NOT merged into `main`. [Full CI #44392551759](https://github.com/o-kobylianskyi/family-points/actions/runs/44392551759) for that SHA: Maven PG18 **770/0F/2E/0S**, fail due ONLY two Retention fixture CHECK errors; B two-PG18 encrypted source loss→native restore E2E **1/1 PASS**, owner PG18 **2/2 PASS**; Web/Android/Compose PASS, full attestation FAIL. Retention fixture correction `a0210961687` is committed to **main** (outside frozen PR SHA) and has no verified combined full run yet. Web separately posted WEB-02_READY_FOR_QA `6dfbc802`, scoped 40/40 Node PASS, browser unverified.

**Owner activate NOW:**
- **QA `++` — HIGHEST PRIORITY:** independently accept/reject B-INT-02 frozen PR #30 using [QA_ACTIVE_ASSIGNMENT.md](handoffs/QA_ACTIVE_ASSIGNMENT.md) and the [canonical B packet](requests/B_INT_02_READY_FOR_QA_20261118.md). One coherent review/targeted E2E/security/financial report; collect actual evidence not source-only PASS. Verify fixed Retention on its own SHA, then require **one combined integration SHA** containing both B PR source and Retention correction for a non-skipped all-four exact-SHA CI. **Do not declare accepted or merge PR solely on historical 770-test CI**. No production unlock.
- **Backend B `++` — NEXT INDEPENDENT:** after frozen `READY_FOR_QA`, start preauthorized **B-NEXT-01** only in files *not in frozen PR*; on a QA-confirmed B P0, B-INT-02-FIX preempts B-NEXT. No extra B integration edits until QA first defect report.
- **Backend Retention `++` — VALIDATE FIX:** own `a0210961` fixture change, inspect targeted PG18 and help QA obtain a combined exact-SHA green run. No weakened CHECK or destructive purge; if further Retention errors, fix only its own files.
- **Web `++` — WEB-03 conditional only:** WEB-01 and WEB-02 frozen; do not edit those shared files during QA. Investigate only proven independent catalog/i18n defects, otherwise `NO_WORK/WAIT`.

**WAIT:** Android / Designer / Release; Backend A/C/D CLOSED. WEB-01 real Chromium remains separately operator-blocked; do not require owner to run now for B-INT-02. Architecture owns exact PR integration gate, QA assignment and release decision. Previous checkpoints below are historical and superseded by this live section. `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, physical purge OFF, BETA HOLD.

## LIVE CRITICAL-PATH RECOVERY / CI TRIAGE — 2026-11-18 21:28 CET

**Newer factual progress overrides old no-code observations**: Backend B has real implementation code; architect fetched [commit `b711371d6c78`](https://github.com/o-kobylianskyi/family-points/commit/b711371d6c78d77f1550002261ff0b6ac4bd941b) touching `WorkspaceNativeV2RestoreEngine`, `WorkspaceOriginalOwnerEnrollmentService`, `WorkspaceRestorePasswordLimiter`, `WorkspaceRestorePermitService` and migrations/PG tests. Backend B [Issue #15 comment 6990884639](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6990884639) reports disposable two-PG18 native encrypted source-loss E2E **1/1 PASS**, but complete CI [#44390867357](https://github.com/o-kobylianskyi/family-points/actions/runs/44390867357) @`b5248e65` **770 tests / 11 failures / 23 errors**, Maven RED, Web/Android/Compose successful, full attestation RED. Root failures are not all attributable to Retention. Newer exact SHA [#44392551759](https://github.com/o-kobylianskyi/family-points/actions/runs/44392551759) @`b711371d` was IN_PROGRESS on Architecture check; **do not assert PASS**.

**ACTIVATE / CONTINUE**:
- **Backend B `++`**: resume from actual committed source/CI, NOT from original B-INT-02 start. Fill [B durable checkpoint](handoffs/BACKEND_B_EXECUTION_CHECKPOINT.md) with DONE/IN_PROGRESS/NEXT/test/run details. Diagnose remaining own Java errors from latest CI; retain genuine Restore E2E. Do not edit Retention tests. B-INT-02 full `READY_FOR_QA` not yet issued.
- **Backend Retention `++`**: fix only two specifically reported Retention fixture errors in `WorkspaceBackupRetentionRollbackGapPostgresTest` (SQL `UPDATE ... SET storage_key=NULL` violates `ck_protective_backup_artifact_evidence_complete` at CI #44390867357). Design a valid SQL fixture using existing schema instead of weakening DB constraint or modifying B code; preserve SQL-first no-unlink and physical purge OFF. Commit targeted fix, inspect newer `b711` CI to avoid duplicate fixes. Report SHA and targeted evidence.
- **QA**: WEB-01 Chromium **BLOCKED on authorized disposable authenticated session**, [operator-only request #6988763962](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6988763962). Do not claim test execution; WAIT for operator approval/DEV fixture. QA may independently inspect latest full CI if instructed, but avoid duplicate full triggers.
- **Web**: continue independent WEB-02; **WAIT** Android/Designer/Release; A/C/D CLOSED.

**Timeout rule**: `++` recovers from last actual commit and checkpoint, not a resumed suspended tool/program state. Before rerunning timed-out write or test inspect whether it already succeeded. Prefer meaningful source commits and checkpoints to long unpersisted turns. No unsafe live execution: `restorable=false`, `replaySupported=false`, Reset HTTP423, physical purge OFF, BETA HOLD.

## AUTONOMOUS QUEUES + PARALLEL DISPATCH — architect-approved 2026-11-18

**Owner policy:** Architecture should give each active stream a CURRENT TASK plus NEXT TASKS where independent; at each future `++` stream reads current GitHub file and continues its earliest safe unfinished queue item. User need not relay new tasks. Architecture proposes additional streams only for **genuinely independent file/contract ownership**; idle streams close or WAIT. Every stream records its `READY_FOR_QA` separately. A `READY_FOR_QA` does **not** prohibit the author from moving on to the next independent pre-authorized item while QA tests it; the author MUST NOT edit the frozen QA package's files or merge overlapping changes.

### LIVE activation suggestions (owner starts chats manually)
| Stream | NOW / NEXT | Canonical file | Non-overlap rule |
|---|---|---|---|
| **Backend B** | **CONTINUE** `++` for large B-INT-02; then auto B-NEXT-01 backend compatibility, then B-NEXT-02 safe diagnostics; QA P0 fixes preempt | [BACKEND_B_ACTIVE_ASSIGNMENT.md](handoffs/BACKEND_B_ACTIVE_ASSIGNMENT.md) | Owns v2, real owner auth and atomic restore. **Never** edit retention service/test files; no Web UI edits |
| **Backend Retention** (NEW independent backend stream) | **ACTIVATE OPTIONAL NOW**; RET-01 storage/SQL crash-consistent retention; auto RET-02 retry/reconciliation | [BACKEND_RETENTION_ACTIVE_ASSIGNMENT.md](handoffs/BACKEND_RETENTION_ACTIVE_ASSIGNMENT.md) | Only retention service/store with explicit coordination and retention-only tests; no v2/permit/restore source; NO destructive live purge |
| **Web** (existing stream) | **ACTIVATE OPTIONAL NOW**; WEB-01 V49 two-tab finalize safety; auto WEB-02 backup account-scoped UI; optional WEB-03 only confirmed UI defects | [WEB_ACTIVE_ASSIGNMENT.md](handoffs/WEB_ACTIVE_ASSIGNMENT.md) | `frontend/` and V49-specific UI fixture; no Backend, shared migrations, QA source-loss |
| **QA** | **ACTIVATE `++` for frozen WEB-01**; then wait for next accepted handoff | [QA_ACTIVE_ASSIGNMENT.md](handoffs/QA_ACTIVE_ASSIGNMENT.md) | Aggregated independent evidence, not micro-test interruptions |
| Android / Designer / Release | WAIT | historical only | Activate on demonstrated need / final release |
| Backend A/C/D | CLOSED | historical | No reopening unless owner decision |

**Current activation is controlled by the LIVE CHECKPOINT above:** Backend B, Backend Retention, Web WEB-02 and QA WEB-01 may run asynchronously. Opening extra threads is a recommendation; Architecture cannot start sessions automatically. If user starts only B, independent queues remain available later.

### Dependency precedence (do not change)
- RET-01 and WEB-01 are independent of B-INT-02; run alongside it. **Only QA acceptance of the same exact SHA** applies to each package; keep shared test ownership explicit.
- RET-01 must not edit RESTORE exporter or original-owner SQL. Web-01 must not invent backend RESTORE endpoints or flip feature flags.
- After B-INT-02 implementation reaches READY_FOR_QA, QA runs its full gates; B may begin B-NEXT-01 **only** in non-overlapping files, otherwise WAIT. QA returns a single corrective list; B must prioritize P0/B-INT-02-FIX before B-NEXT work.
- QA may test RET-01/WEB-01 before B completes B-INT-02, one scope at a time. No broad full CI per intermediary commit.
- Human-auth/browser/phone tests are separate `OPERATOR ACTION REQUIRED` packets through Architecture, never hidden in ordinary stream handoffs.
- Never assert ready-for-release based on isolated package PASS. Preserve BETA HOLD and disabled RESTORE/RESET and physical purge.

## Mandatory stream bootstrap and every subsequent `++`
1. Read this file from `main` **at the start of every active turn**. Then read only your current role assignment file below, Issue [#15](https://github.com/o-kobylianskyi/family-points/issues/15) for the latest architect directive, and relevant source/CI. **Do not ask owner to paste tasks.**
2. Perform the **largest safe substantive portion** of the currently assigned work autonomously. `++` = continue the latest active assignment; not permission for production data deletion, deployment, reset, destructive backup retention, or release.
3. Record actual changed files, commit SHA, targeted test evidence, incomplete/blocked tasks, and a short handoff **in your role assignment or an append-only linked handoff**, plus concise Issue #15 comment for cross-stream blocker/READY_FOR_QA. No docs-only loop if implementation is expected.
4. **Streams cannot wake/activate each other or work in background**. They hand off asynchronously in GitHub; **Architecture** tells owner which chat to start/continue/close. No stream should assume a GitHub comment itself starts a role.
5. When blocked by another stream: post one precise blocker with file/contract/SHA, stop only the dependent portion, work on independent subitems; do **not** ask owner to coordinate, and do not silently weaken security.
6. The only owner-visible manual-action instruction is a **separate, explicit operator request**. See protocol below.

## CURRENT stream status and routing
| Stream | Owner instruction | Canonical work file | Condition |
|---|---|---|---|
| Architecture | ACTIVE: sole coordinator/acceptance router | **this file** | Coordinates dependency gates and calls activation |
| Backend B | **ACTIVATE / CONTINUE** with `++` | [BACKEND_B_ACTIVE_ASSIGNMENT.md](handoffs/BACKEND_B_ACTIVE_ASSIGNMENT.md) | Deliver B-INT-02 big implementation; no micro gates |
| QA | **ACTIVATE `++`** for WEB-01 READY_FOR_QA (B baseline independent) | [QA_ACTIVE_ASSIGNMENT.md](handoffs/QA_ACTIVE_ASSIGNMENT.md) | Parallel nonblocking baseline; full gate after B handoff |
| Backend A | CLOSED | historical only | All mandatory tasks inherited by B |
| Backend C | CLOSED | historical only | All mandatory tasks inherited by B |
| Backend D | CLOSED | historical only | All mandatory tasks inherited by B |
| Backend Retention | OPTIONAL ACTIVATE `++` | [BACKEND_RETENTION_ACTIVE_ASSIGNMENT.md](handoffs/BACKEND_RETENTION_ACTIVE_ASSIGNMENT.md) | RET-01 then RET-02, independent from B |\n| Web | OPTIONAL ACTIVATE `++` | [WEB_ACTIVE_ASSIGNMENT.md](handoffs/WEB_ACTIVE_ASSIGNMENT.md) | WEB-01 then WEB-02; no backend edits |\n| Android / Designer / Release | WAIT | historical | Activate on concrete need |

## Step and dependency graph
**Stage 01 (parallel NOW):** Backend B implements B-INT-02; Retention validates RET-01/RET-02; Web advances WEB-02; QA independently checks frozen WEB-01. QA does **not** stop B or require a full CI per subcommit.

**Gate A — Backend B READY_FOR_QA**: one coherent reviewed working tree/main SHA including complete agreed functionality, targeted compile/PG tests, supported/blocked categories and migration/backward-compatibility report. Only this state activates QA's B-INT-02 full-package test phase; QA may independently test other frozen WEB/RET packages earlier. A code-committed partial archive is not READY_FOR_QA.

**Stage 02 (QA):** one pinned non-skipped exact-SHA Java 21/Postgres18 + Web/Android/Compose + full attestation run; disposable source-db-loss E2E with production security implementation path (never real family). Output **one aggregated defect report** by severity, reproduction, first stack, ownership.

**Stage 03 (Backend B):** one large corrective pass for all reproducible defects; focused checks, then Stage 02 exact SHA rerun. Independent fixes can be committed separately, not accepted as one unsafe monolith.

**Gate B — Release eligibility:** only after QA independently accepts full end-to-end security/finance/fail-closed evidence, Architecture instructs Release and (if needed) Web/Android real device acceptance. **No auto-production unlock**.

## Operator-only protocol — ALWAYS separate from ordinary updates
When genuine human action is needed, author a dedicated note with:
- `OPERATOR ACTION REQUIRED` and clear issue ID;
- why automation/synthetic tests cannot establish evidence;
- whether it is disposable DEV-only or touches live assets (live writes need distinct explicit approval);
- exact step-by-step instructions, expected result, rollback/safety, data the operator must return;
- **which stream** receives the response (normally QA) and what Architecture will do on receipt.
Do NOT blend manual test instructions into normal task reports; do NOT ask operator to activate streams just to forward tool output. Never use a real family for destructive testing absent explicit separate approval.

## Safety / evidence contract
Current production Restore `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, **BETA HOLD**. No live family mutation, deployment, signed APK publication or destructive backup-retention purge authorized. Latest historically verified all-four baseline in earlier QA-068: #44290621478 @`89b0f8e1`, Maven 749/749; more recent historical v2 implementation and QA fixes are **not** automatically covered. Before stating a new full PASS, inspect actual non-skipped jobs and exact SHA, not a green badge.

**Architecture updates only one current dispatcher and role work file when stage/owner changes.** Avoid stale LATEST pointers: this file supersedes older A/B/C/D splitting language and older micro-package/full-ci-every-commit instructions.
