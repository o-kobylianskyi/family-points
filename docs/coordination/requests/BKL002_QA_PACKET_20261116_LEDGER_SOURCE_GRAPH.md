# BKL002-QA-20261116-17 — Authenticated POINT_LEDGER source graph ancestry (test-only)

**Status:** READY_FOR_QA. **Developer Maven/Docker:** NOT_RUN. **Owner:** Backend P0 / independent QA issue #15. No production Restore, Reset Execute, VERIFIED or family modifications.

## Implemented
- `DisposableLedgerSourceGraphVerifier.java` `5928292` in `src/test` ONLY. Read-only AES authenticated V50 archive preflight for POINT_LEDGER and POINT_TYPES. Confirms direct family ID, signed point currency IDs and codes, unique historic ledger IDs and nonzero member/denomination IDs.
- Explicitly supports **MANUAL with null source_id** or **TASK with positive source_id and matching TASK_EXECUTIONS.id + member_id within the same source archive**. A TASK source absent from archive or referencing another member is blocked; no invented identity remapping. No source/target writes, no payment/replay.
- Every other source_type (including REWARD_PURCHASE, REFUND, EXCHANGE, unknown/legacy) fails closed `LEDGER_SOURCE_TYPE_UNSUPPORTED` until its type→entity schema and financial semantics are independently proven.
- `DisposableLedgerSourceGraphVerifierTest.java` `b6cf1c3` authenticates synthetic AES envelopes, tests valid TASK and MANUAL rows, task FK missing, task member mismatch, missing source_id, MANUAL with illegal source_id, unsupported REWARD_PURCHASE and REFUND. Existing import inspector remains unrestorable, and production financial closure replaySupported=false.
- This does NOT extend `DisposablePointLedgerRestoreExecutor` to TASK. An authenticated source→task link is a necessary condition, not sufficient proof of target task, point transfer, balance history, ledger replay or recent owner authority.

## QA suggested batch
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=DisposableLedgerSourceGraphVerifierTest,DisposablePointLedgerCrossDatabasePostgresTest,WorkspacePointLedgerSourceClosurePostgresTest,DisposableRewardCategoryCrossDatabasePostgresTest,DisposableRewardRequirementCrossDatabasePostgresTest,DisposableRewardObligationCrossDatabasePostgresTest,ProtectiveBackupRewardHistoryClosureTest" test
git rev-parse HEAD
```

## Acceptance
1. SRC-GRAPH-01: signed POINT_TYPES+POINT_LEDGER and TASK_EXECUTIONS original task ID is available, task member equals ledger member. Metadata authentication is not equated with target ownership.
2. SRC-GRAPH-02: unknown TASK source ID, a task owned by different member, malformed source_id or MANUAL with nonnull source ID fails without writes.
3. SRC-GRAPH-03: unsupported types REWARD_PURCHASE/REFUND/EXCHANGE blocked; no false claim that source_type values are fully cataloged or polymorphic ledger can be replayed.
4. SRC-GRAPH-04: prior red Native Category CI correction `2539bf9`, reward obligations and currency archive QA #16 regressions remain in pinned exact-SHA run; actual test counts/errors/skips + SHA returned.
5. SRC-GRAPH-05: source family data untouched, no production endpoint changes, `restorable=false`, `replaySupported=false`, BETA HOLD HTTP423.

## Next backend boundary
Read actual enum/source_type producers and schema tests; model authenticated purchase/refund/exchange ancestry only with confirmed source type and target financial reconciliation, then proceed to test-only replay. This source verifier intentionally does not guess payment semantics or promote VERIFIED.
