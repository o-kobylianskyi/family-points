# BKL002-QA-20261116-13 — Native requirement replay (disposable source/target PostgreSQL)

**Status:** READY_FOR_QA. **Developer Maven/Docker:** NOT_RUN; no Java runtime in source-only GitHub connector. **Owner:** Backend; **independent QA:** Issue #15. No real-family data, no VERIFIED, no reset/restore HTTP.

## Implementation
- Test-only `src/test/java/com/olehkobylianskyi/familypoints/DisposableRewardRequirementRestoreExecutor.java` commit `e825afe0`. AES-authenticated source archive's `REWARD_REQUIREMENTS` is replayed into a distinct, locked family in disposable PostgreSQL under one TransactionTemplate. Source category set restricted to requirements, reward definitions, reward requests and task definitions. Source owner one-of exactly definition/request; positive IDs, archived parent presence and complete injective source→target definition/request/task maps required. Wrong-family target parents denied by direct DB check; target epoch=0, target family existing and no preexisting requirements. Preserves required/phase/type/task/time window/description/order; rebuilds auto-ID and verifies target row count.
- Independent 2-PG integration `src/test/java/com/olehkobylianskyi/familypoints/DisposableRewardRequirementCrossDatabasePostgresTest.java` commit `4088b5e`: source reward/requirement + test-only neighbor requirement in target + reconstructed target reward. Source encrypted archive exported and checked; NULL mapping and neighbor reward mapping fail before mutation; valid remap inserts one target record; duplicate replay and tampered file denied; neighbor/source retain original rows.
- Source code in `src/test` ONLY, no Spring @Service, HTTP, controller or production restore. No obligation/purchase/ledger replay, member reauth or verified restoration. This does NOT approve FINANCIAL native restore.

## QA targeted command (Java 21 / Docker / Testcontainers PostgreSQL 18)
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=DisposableRewardRequirementCrossDatabasePostgresTest,DisposableRewardCategoryCrossDatabasePostgresTest,WorkspaceRewardObligationArchiveClosurePostgresTest,ProtectiveBackupRewardHistoryClosureTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest,DisposableGroupLedgerCrossDatabasePostgresTest" test
git rev-parse HEAD
```

## Acceptance
- REQ-NATIVE-01: separate source and target PostgreSQL 18, Flyway through latest V52, AES-authenticated source family and scope; exactly one requirement copied and remapped new PK.
- REQ-NATIVE-02: historical phase, requirement_type, required, order and definition relation exact; target foreign parent denied without changes.
- REQ-NATIVE-03: null mapping denied SecurityException not NullPointer; zero target rows after denial; target source/neighbor untouched.
- REQ-NATIVE-04: replay twice denied, tampered file denied, count remains exactly one.
- REQ-NATIVE-05: verify earlier CI-red repairs: double source category count and immutable-map NPE; if failing send exact test and stack trace to Backend.
- REQ-NATIVE-06: QA records current SHA, actual test counters PASS/FAIL/ERROR/SKIP, environment and source defects in Issue #15. No acceptance from docs-only green or skipped Java job.

## Remaining P0
- Native obligations blocked category/reward replay needs explicit member, purchase, requirements and definition/category identity maps, plus immutable point/reputation ledger reconciliation and durable receipt.
- Owner fresh reauth / family-specific encrypted protected backup / verified physically restorable retention / atomic Reset Execute remain blocked (HTTP 423). BETA HOLD.
