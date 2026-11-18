# B-A-03d — Staging bypass defense and Spring-proxied PostgreSQL integration

**Date:** 2026-11-18. **Owner:** Backend A. **State:** CODE COMMITTED / QA PENDING. Restore, Reset Execute, and BETA remain blocked.

## Scope/source evidence
- Audited `WorkspaceBackupRegistryStagingCoordinator.registerAndStage` → `WorkspaceBackupArtifactStagingService.stage` → `WorkspacePortableBackupExporter.export`.
- Coordinator is Spring transactional REPEATABLE_READ and rejects unknown or weak caller isolation (B-A-03b).
- Staging service previously had no transaction requirement: callers could enter directly and invoke exporter with no atomic registry/file workflow. Its unit tests previously used direct Java instances without transaction context.

## Implementation
- `6570877`: `WorkspaceBackupArtifactStagingService.stage` checks active Spring transaction + synchronization + REPEATABLE_READ before accessing exporter or files. No storage or archive-format changes; no new HTTP endpoints.
- `a1b0dca`: existing stage unit fixtures explicitly initialize repeatable-read Spring transaction synchronization, clean up after each test; add READ_COMMITTED, null isolation and no transaction negatives, all asserting no exporter/storage interactions.
- `94de34f` + `a714796`: new `WorkspaceBackupStagingProxyIsolationPostgresTest` uses Testcontainers PostgreSQL 18, Flyway and a real Spring `TransactionInterceptor` with annotation attributes around coordinator. Verifies database `SHOW transaction_isolation` is repeatable read when proxied coordinator starts its transaction; an outer READ_COMMITTED transaction fails before registry/staging/storage interactions.
- Existing B-A-03c `WorkspacePortableBackupConcurrentSnapshotPostgresTest` separately exercises two real JDBC connections with an unfenced writer and a pinned repeatable-read source snapshot.

## Test acceptance
```bash
./mvnw -Dtest=WorkspaceBackupArtifactStagingServiceTest,WorkspaceBackupRegistryStagingCoordinatorTest,WorkspaceBackupStagingProxyIsolationPostgresTest,WorkspacePortableBackupConcurrentSnapshotPostgresTest,WorkspacePortableBackupExporterTransactionContractTest,WorkspacePortableBackupExporterPostgresTest test
```
All tests require actual pinned-SHA success, not a queued run or merely a committed test source. For comprehensive release CI, also verify Java, Web, Android and Compose on the same SHA, not just a workflow badge with skipped heavy jobs.

## Residual threats / follow-up
- No exhaustive repository-wide lexical callsite search was available in this pass. Verified production chain only; independent direct staging now rejects missing transaction by design.
- The stage check validates Spring's declared isolation; the PG18 proxy test verifies real physical isolation for coordinator. Other exotic customized transaction managers have not been evaluated.
- Encrypted file compensation remains coordinator-owned. Direct stage within another legitimate REPEATABLE_READ transaction can still create an orphan if the surrounding transaction rolls back; storage reconciliation remains necessary, and no production automatic VERIFIED/Restore unlock is permitted.
- `FAMILY_MEMBERS` privacy/identity archive extension remains **PROPOSED**, awaiting explicit Architecture + Backend B acceptance of `B-A-02_PORTABLE_MEMBER_SOURCE_CONTRACT_20261118.md`.
- P0 continued: original account/membership provenance, historical ledger source closure, retention/recovery tests and consent-gated real restore.

**Next:** B-A-03e CI defect triage for new test suite; investigate orphan compensation at any authorized direct-stage callsites. After schema approval, B-A-04 portable member source catalog and inspector.
