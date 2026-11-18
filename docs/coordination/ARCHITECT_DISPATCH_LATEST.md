# FamilyPoints — CURRENT ARCHITECT DISPATCH (read FIRST on every `++`)

**Effective:** 2026-11-18 18:26 CET. **Authoritative control for live workstream activation and assignments.** Owner communicates primarily with Architecture; owner starts role chats by sending only `++`. GitHub is the durable coordination queue. The user need not copy technical assignments into chats.

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
