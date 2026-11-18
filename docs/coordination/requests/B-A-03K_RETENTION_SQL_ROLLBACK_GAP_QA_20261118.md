# B-A-03k — PostgreSQL 18 physical unlink vs SQL rollback rehearsal
Date: 2026-11-18. Owner: Backend A (Backup/Portable Archive and encrypted retention).
State: SOURCE COMMITTED; exact-SHA CI/JUnit result pending.

## Production hazard demonstrated in disposable fixture
`WorkspaceBackupRetentionService.expireEligible` uses PostgreSQL `FOR UPDATE OF p SKIP LOCKED` and a no-active-lease predicate, then invokes `WorkspaceBackupLocalArtifactStore.deleteAuthorizedExpired` **before** changing SQL registry state to `EXPIRED`. An external filesystem unlink is not part of the database transaction. If SQL fails/rolls back after successful unlink, a registry row can remain `BACKING_UP` (or another pre-expiry state) despite the encrypted archive now being physically missing. Prior read-only reverse audit can detect this; it **cannot repair lost ciphertext**.

## Code/test package
- `WorkspaceBackupRetentionRollbackGapPostgresTest`: Testcontainers PostgreSQL 18 + Flyway + actual `@TempDir` encrypted archive. Inserts only synthetic workspace role/member, family-scoped expired registry with valid digest/size/stored-at. Calls real `expireEligible(1)` in an explicit Spring SQL transaction, confirms file is unlinked, then deliberately throws to force rollback. Asserts SQL state remains `BACKING_UP`, ciphertext remains absent, reverse registry audit flags `REGISTRY_CIPHERTEXT_INVALID` (currently includes missing/unsafe), and unrelated family stays untouched.
- Second test injects a fail-closed disk unlink exception via scoped mocked local storage; SQL transaction rolls back and registry does NOT falsely claim `EXPIRED`.
- Existing `WorkspaceBackupRetentionPostgresTest` exercises active restore lease as retention exclusion, cross-tenant lease release denial, and valid expiry progression. B-A-03h/i/j exercised independent read-only orphan and TOCTOU scans.

## Critical non-goals / unresolved P0
- This deliberately captures a preexisting failure mode. No automated cleanup, production SQL modification, recreation of missing encrypted data, HTTP entrypoint, scheduler or Restore grant is added.
- An already physically deleted encrypted backup **cannot be restored from its original file by SQL rollback**. A future approved retention design should use a staged tombstone/quarantine or durable deletion intent with idempotent reconciliation, transaction/lease locking, retention authorization, and verified alternate copy before destroying the sole source. Any such design requires Architecture, Release and QA review. Do not solve this by retroactively setting registry VERIFIED or fabricating a replacement digest.
- Detection of absent bytes is currently expressed as `REGISTRY_CIPHERTEXT_INVALID` via the underlying `BACKUP_FILE_MISSING_OR_UNSAFE` SecurityException; this is evidence of missing data, not automated recovery.
- Active lease/race after row lock must remain protected by the existing scoped coordinator, no unaudited direct delete calls.
- `FAMILY_MEMBERS` B-A-02 portable source contract has conditional Backend B consumer ACK but not final Architecture schema/version/privacy signoff.
- Release gates: `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD.

## QA and CI
Run on disposable Docker/PostgreSQL18:
```bash
./mvnw -Dtest=WorkspaceBackupRetentionRollbackGapPostgresTest,WorkspaceBackupRetentionPostgresTest,WorkspaceBackupRegistryPresenceAuditServiceTest,WorkspaceBackupRegistryPresenceRacePostgresTest,WorkspaceBackupReconciliationRacePostgresTest test
```
Collect test counts, assertions, failure stack traces, exact-source SHA and required Java/Web/Android/Compose job status. Prior B-A-03j handoff SHA `bbc5d866aa4c38d46445fe393f8bca8b3a9a101e` GitHub Actions `44274509971` concluded SUCCESS, but predates this new test.

## Follow-on B-A-03l
1. Review current exact-SHA CI; resolve any fixture failures.
2. Design (not enable) crash-safe retention state machine `eligible → quarantine → db-confirmed → purge` with verified second copy and no false delete assurances.
3. Test second-workspace and valid-leases against proposed staged deletion path.
4. Obtain Architecture + Backend B vNext member catalog signoff before modifying archive fields.
