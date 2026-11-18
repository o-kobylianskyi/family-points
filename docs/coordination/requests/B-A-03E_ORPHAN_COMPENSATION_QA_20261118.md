# B-A-03e — encrypted backup orphan compensation

**Date:** 2026-11-18. **Owner:** Backend A. **Status:** SOURCE COMMITTED / exact-SHA CI pending.

## Reviewed failure modes
1. `WorkspaceBackupLocalArtifactStore.store` links the temporary upload into final `.fpbackup`, then deletes temp, chmods destination and returns. Errors after `Files.createLink` previously left an already-published destination even though `store()` failed. Earlier file with the same destination MUST remain untouched.
2. `WorkspaceBackupArtifactStagingService.stage` stores the encrypted bytes then reads them back, checks SHA and verifies the signed metadata. On failure after `storage.store` returned, stage throws before returning to `WorkspaceBackupRegistryStagingCoordinator`. The coordinator registers rollback compensation only after stage returns, leaving an orphan.
3. SQL and filesystem are not a single transaction. A cleanup failure must be observable; durable orphan reconciliation remains a separate release gate.

## Changes
- `4f1afe5`: local artifact store tracks whether this invocation successfully created the destination hard link, and whether publication completed. If publication fails in between, finally attempts to delete only its own new file. Existing file on `FileAlreadyExistsException` is never removed. Partial temp files are still cleaned.
- `84a95df`: after a successful storage result, staging wraps readback+validation+receipt creation and attempts `deleteAuthorizedExpired(workspaceId,snapshotId)` on RuntimeException/Error. Re-throws the original exception; any cleanup RuntimeException is appended as suppressed, not silently discarded.
- `2b716c7`: regression tests for failed storage readback after publication and for cleanup failure with preserved original exception.

## Validation
```bash
./mvnw -Dtest=WorkspaceBackupArtifactStagingServiceTest,WorkspaceBackupLocalArtifactStoreTest,WorkspaceBackupRegistryStagingCoordinatorTest,WorkspaceBackupStagingProxyIsolationPostgresTest,WorkspacePortableBackupConcurrentSnapshotPostgresTest test
```
For exact-SHA acceptance record JUnit totals, failing stack traces, and all required Actions job conclusions. At initial observation prior B-A-03d run `44268359058` reported workflow SUCCESS; current B-A-03e run remains unverified.

## Limitations
- These changes handle ordinary Java exceptions after publication; forced process termination / power loss requires independent disk-orphan reconciliation.
- No durable audit of cleanup failure added here; suppressed exception preserves programmatic evidence but operational collection/metrics remains P0.
- Deletion is scoped to the exact UUID/family that this invocation has just stored, never to a selected family backup unrelated to the call. Original protected registry/retention/lease rules remain separate.
- `FAMILY_MEMBERS` portable source contract B-A-02 requires Architecture + Backend B ACK before altering archive schema.
- No reset, restore execute, deployment or real-family writes. `restorable=false`, `replaySupported=false`, HTTP423 and BETA HOLD unchanged.

**Next B-A-03f:** verified physical file compensation/reconciliation tests with intentionally failed checksum/readback and simulated process restart, then progress to member source schema only after approval.
