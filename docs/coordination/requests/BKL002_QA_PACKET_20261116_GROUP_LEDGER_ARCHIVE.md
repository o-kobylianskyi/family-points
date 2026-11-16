# BKL002-QA-20261116-02 — Group financial ledger encrypted archive coverage

**Status:** READY_FOR_QA, execution NOT_RUN by Backend. **Priority:** P0. **Producer:** Backend. **Consumer:** QA/Testing. **Issue:** #15. **Source baseline:** group-ledger feature commits `f66da9d`, `cea2c17`, `c4a862b`, `3b321e8`, `58eb5a7`, tests `04276da`, `d9312b2`, `53e0068`, `ace2a83`. QA must record the exact selected HEAD SHA before executing. These commits do not authorize any destructive operation.

## Contract delivered
- `GROUP_POINT_LEDGER` is a new optional *encrypted source export* category for `group_point_transactions`, tenant-scoped strictly by `member_groups.family_id`.
- Included in snapshot digest selection, protective backup registry whitelist and encrypted portable exporter whitelist; no Flyway migration or existing record mutation.
- Offline import inspector checks group PK dependency against selected `MEMBER_GROUPS` (missing/dangling -> block) and positive currency ID; every group ledger row yields `CURRENCY_REBIND_REQUIRED`. `restorable=false` remains mandatory.
- Financial closure counts group ledger as exportable but **NOT replayable**; explicit `RESTORE_GROUP_LEDGER_REPLAY_UNIMPLEMENTED` and `RESTORE_FINANCIAL_NATIVE_REPLAY_UNIMPLEMENTED:GROUP_POINT_LEDGER`. Immutable balances are not touched.
- `WorkspacePortableBackupExporter` does not yet implement restoring/clearing any economic row. This is not a full-family backup/replay proof.

## QA environment / exact command
Java 21, Maven, Docker Desktop/Testcontainers PostgreSQL 18, isolated test containers and synthetic fixtures only. No real workspaces; no deploy.
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=WorkspaceGroupLedgerPortableExportPostgresTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest,WorkspaceBackupArtifactRegistryPostgresTest,WorkspaceBackupRegistryStagingCoordinatorTest,WorkspaceBackupArtifactStagingServiceTest,WorkspaceDisposableRestoreRehearsalPostgresTest,DisposableCrossDatabasePortableRestorePostgresTest,WorkspaceBackupRetentionPostgresTest" test
```

## Test acceptance / expected behavior
- **FIN-ARC-01:** export and inspect authenticated AES file from source family; confirm count=2, amounts 15/-4 and original point type; family-only balance 11. Row IDs and source context retained.
- **FIN-ARC-02:** another family's +99 group ledger is never exported or altered. Wrong target family inspection throws. No tenant data mutation.
- **FIN-ARC-03:** export group ledger *without MEMBER_GROUPS* yields `MISSING_CATEGORY:MEMBER_GROUPS`, not restorable; invalid/dangling group FK is blocked.
- **FIN-ARC-04:** positive currency reference leads to `CURRENCY_REBIND_REQUIRED:GROUP_POINT_LEDGER.point_type_id`; zero or invalid point type ID produces `INVALID_REFERENCE`.
- **FIN-ARC-05:** even selected exportable group ledger yields `replaySupported=false` and explicit native replay blocker. No `VERIFIED`, no release gate.
- **FIN-ARC-06:** original V51/V52 staging, local storage, retention lease and independent cross-db native group/media/template test regressions remain passing.

## Limitations / escalations
- Existing snapshot hashing is broad workspace-scoped and does not establish complete ledger closure; full financial ledger source/fk/owner/currency replay and target conflict/epoch still absent.
- `GROUP_POINT_LEDGER` rows can include source `point_type_id` and `group_id`, both are NOT portable target authority; rewrite must occur only within audited explicit owner/target mapping in future native restore engine.
- No automatic run in the QA chat occurs from creating this file. QA must pick it up and independently report SHA, tests run/fail/error/skip, CI/test environment, action items. Backend fast compile/tests are **NOT_RUN** here, because no developer-side JDK/Docker execution is available via this connector.
- On P0 data loss, cross-tenant, immutable ledger or auth failure: fix immediately; other non-destructive code may proceed in parallel. No live family data, no destructive reset, no `VERIFIED` promotion, no BETA deployment.

**Release:** BETA HOLD; reset execute HTTP 423.
