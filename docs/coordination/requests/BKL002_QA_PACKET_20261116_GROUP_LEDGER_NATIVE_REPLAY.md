# BKL002-QA-20261116-03 — Bounded disposable native group ledger replay

**Status:** READY_FOR_QA; **developer execution:** NOT_RUN (no Maven/Docker runner in linked GitHub action environment). **Priority:** P0. **Backend source:** `3dcd07c`, `6bebbfe`; **test-only:** `13f06bb`, `fde127a`. **Consumer:** QA/Testing; #15. Pin exact Git SHA on pickup; no real workspace data.

## Scope and contract
- `DisposableGroupLedgerRestoreExecutor` exists in `src/test` only. It reads AES-authenticated `.fpbackup` source data with exactly `GROUP_POINT_LEDGER` and `MEMBER_GROUPS` categories, and may commit source MANUAL group ledger rows to fresh target **disposable** PostgreSQL with strict, caller-supplied source→target group and point-type ID remaps. No authorization or production restoration feature exists here.
- Unsupported source_type other than `MANUAL`, populated `source_id`, missing/malformed row keys, group mapping gaps, currency mapping gaps, cross-family target mapped group or currency, non-fresh target epoch, pre-existing target ledger, duplicate replay, and equal old/new transaction IDs must fail closed.
- Replay checks count, exact source amounts, target group cumulative balance, source snapshot ID, and does not mutate neighbor/source database. Does NOT prove fully reconciled financial history, immutable audit replay, lifecycle obligation mapping, account ownership, or durable VERIFIED.
- Exporter adds `BACKUP_GROUP_LEDGER_FOREIGN_CURRENCY` check for group ledger whose point_type belongs to other family (legacy corrupted FK). Financial closure leftover unreachable export blocker code corrected to explicit native replay blocker.
- **Known limitation:** key/code provenance not independently asserted by test-only native executor: integration test independently compares source and target currency codes before passing mappings. A production exporter must include/attest immutable per-currency source code mapping and independent target policy before allowing replay.

## QA command
Java 21, Docker Desktop/Testcontainers PostgreSQL 18, isolated synthetic DBs:
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=DisposableGroupLedgerCrossDatabasePostgresTest,WorkspaceGroupLedgerPortableExportPostgresTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest,DisposableCrossDatabasePortableRestorePostgresTest,DisposableComposedNativeRestorePostgresTest" test
```

## Acceptance targets
- FIN-REPLAY-01: two independent PostgreSQL 18 containers, Flyway V52 on each, source archived ledger +15 and -4, target native replay keeps group balance 11, remaps original transaction IDs and group/point types.
- FIN-REPLAY-02: source remains 11 and unrelated neighbor family remains 99; cross-tenant group/currency mapping is denied; no new rows in failed target.
- FIN-REPLAY-03: replay same snapshot second time fails, target history remains exactly two transactions.
- FIN-REPLAY-04: encrypted source archive scope and currency references checked, broken group/foreign currency fail closed before production exposure.
- FIN-REPLAY-05: financial closure still returns replaySupported=false and does not permit financial RESTORE or VERIFIED.
- FIN-REPLAY-06: all existing group archive, linked native media/template and prior lease tests remain green at exact SHA; record PASS/FAIL/SKIP and report problems to #15.

**Release:** HTTP423 reset/execute and BETA HOLD. User has not authorized real data reset, import or deploy. QA must pick up explicitly; creating this file does not run another chat.
