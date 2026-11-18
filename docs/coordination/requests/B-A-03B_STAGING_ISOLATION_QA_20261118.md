# B-A-03b — Backup capture transaction boundary and fail-closed staging

**Date:** 2026-11-18. **Track:** Backend A. **Safety:** production restore remains disabled; no reset, migration, deployment or real-family changes.

## Verified call chain and threat
`WorkspaceBackupRegistryStagingCoordinator.registerAndStage` (`@Transactional`) → `WorkspaceProtectiveBackupService.request` (source digest/registry) → `WorkspaceBackupArtifactStagingService.stage` (no transaction annotation) → `WorkspacePortableBackupExporter.export` (`@Transactional(REPEATABLE_READ)`).

Before B-A-03b, the coordinator began a default isolation transaction (typically PostgreSQL READ COMMITTED). Exporter's REQUIRED propagation **joins** that existing transaction; an annotation on the exporter alone cannot upgrade transaction isolation. At READ COMMITTED different SQL statements in initial digest, archived rows and final digest can see different snapshots. This requires fail-closed handling before registry/file side effects.

## Source commits
- `1520bcb`: coordinator `registerAndStage` now declares `@Transactional(isolation = Isolation.REPEATABLE_READ)`.
- `c7f89b8`: after verifying active Spring transaction/synchronization, coordinator requires transaction isolation recorded by Spring to equal `Connection.TRANSACTION_REPEATABLE_READ`. Weak (`READ_COMMITTED`) or unknown isolation throws `BACKUP_SNAPSHOT_ISOLATION_REQUIRED` **before** calling registry.request or staging. An outer lower-isolation transaction can no longer silently reuse this entry point.
- `9791430`: coordinator unit tests initialize repeatable-read fixture explicitly; add negative tests for READ_COMMITTED and null isolation, checking no interactions with registry/staging/storage. Existing staging/commit compensation tests remain.

## Scope and limitations
- This guard secures the `registerAndStage` path, not every possible direct call to `WorkspacePortableBackupExporter.export` or `WorkspaceBackupArtifactStagingService.stage`. The latter can be called directly in tests; production direct entry points must be audited separately.
- Spring `TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()` reflects Spring's transaction context. Full real PostgreSQL transactional connection/driver behavior and concurrent SQL-writer tests remain unverified.
- `@Transactional` REPEATABLE_READ does not on its own guarantee that the first snapshot is acquired after the family-row lock. Isolation/family epoch locking, concurrent writers bypassing the fence and early snapshot acquisition must be tested in PG18.
- Existing snapshot digest is broader than categories listed; original `FAMILY_MEMBERS` archival wire contract remains PROPOSED pending Architecture and Backend B ACK, and is not silently added here.
- No claim of Maven, Testcontainers or exact-SHA full CI success before actual evidence.

## QA targeted command and acceptance
`./mvnw -Dtest=WorkspaceBackupRegistryStagingCoordinatorTest,WorkspacePortableBackupExporterTransactionContractTest,WorkspacePortableBackupExporterPostgresTest test`

Independent PG18 staging test must create an actual Spring-proxied coordinator call and assert `SHOW transaction_isolation` equals `repeatable read`. Then force an explicit outer READ_COMMITTED transaction and require fail-closed without registry/file side effects. Concurrency test should use separate real database connections with in-flight source-row modifications; accept one coherent snapshot or a conflict, never a mixed graph. Record SHA and actual pass/fail. Run all required CI jobs on the same SHA before gate acceptance.

**Next B-A-03c:** add actual two-connection PostgreSQL 18 boundary test, review all direct exporter/staging entry points, and check handling of an epoch/row-lock acquired after a prior snapshot in the same transaction. Restore HTTP423/BETA HOLD unchanged.
