# Backend A — B-A-03j · reverse registry audit race and retention callsite review

Date: 2026-11-18. Track Backend A. All checks and code below are **read-only observation**, not authorization for recovery or destruction.

## Confirmed baseline
- Previous B-A-03i exact-SHA `5f91d269a19de8c4710fd7fffb8454f5a5759470` Actions `44273553572`: overall CI SUCCESS; verify component job steps separately for any release claim.
- Backend B conditional producer-consumer ACK to original logical member source is documented in `docs/coordination/requests/BACKEND_B_TO_A_MEMBER_SOURCE_CONSUMER_ACK_20261118.md`. Architecture's versioned portable member source schema and privacy contract still not explicitly approved, so existing archive format unchanged.

## Delivered source/test changes
- `WorkspaceBackupRegistryPresenceRacePostgresTest` uses Testcontainers PostgreSQL 18 and two independent JDBC connections; pauses ciphertext verification while another connection updates the `artifact_sha256` of the exact family/snapshot. A second audit catches the changed digest without file deletion or cross-family mutation.
- `WorkspaceBackupRegistryPresenceAuditService.audit` now re-reads `state`, `storage_key`, and `artifact_sha256` for the same workspace/snapshot **after** successful ciphertext integrity preflight. If the row vanished, changed, or duplicated, it emits `REGISTRY_CHANGED_DURING_AUDIT` rather than `REGISTRY_CIPHERTEXT_PRESENT`.
- Updated unit tests cover concurrent status+digest change; PG18 race test now requires the initial audit itself to classify `REGISTRY_CHANGED_DURING_AUDIT`, with a subsequent audit rejecting mismatched new SHA. Other corrupt/missing tests retain fail-closed behavior. This is a best-effort double observation, not transactionally durable evidence: further changes after the second SELECT are possible.
- No new controller, scheduler, migration, physical deletion, extra source identity, or RESTORE behavior.

## Inspected physical delete callsites
1. `WorkspaceBackupArtifactStagingService.stage`: on readback/envelope failure after successful store, deletes exact newly stored snapshot; retains original exception, suppresses cleanup failure.
2. `WorkspaceBackupRegistryStagingCoordinator.registerAndStage`: registered transaction completion callback removes exact staged snapshot only after rollback. Does not invoke orphan audit as a delete selector.
3. `WorkspaceBackupRetentionService.expireEligible`: explicit operator/coordinator bounded retention path; row `FOR UPDATE SKIP LOCKED`, expiry state, active-lease absence and scoped storage key check; deletes before SQL state transition, so crash/SQL commit failure after deletion is **non-atomic filesystem/database gap**. Must not enable automatic scheduling or treat a prior audit finding as authority to execute this method.
4. `WorkspaceBackupLocalArtifactStore.store`: a partial publication exception after creating new hard link attempts to delete only that link; never overwrite or delete existing destination in a collision.
5. `WorkspaceBackupLocalArtifactStore.deleteAuthorizedExpired`: low-level exact-file deletion primitive, explicitly not its own authorization engine. Existing orphan/reconciliation/presence audit services have **zero** delete callsites.

This enumerates these verified production service files; it is **not** claimed to be an exhaustive repository-wide lexical search of every possible dynamic invocation.

## QA targeted suite
```bash
./mvnw -Dtest=WorkspaceBackupRegistryPresenceRacePostgresTest,WorkspaceBackupRegistryPresenceAuditServiceTest,WorkspaceBackupReconciliationRacePostgresTest,WorkspaceBackupReconciliationAssessmentPostgresTest,WorkspaceBackupReconciliationAssessmentServiceTest,WorkspaceBackupLocalArtifactStoreTest,WorkspaceBackupRetentionPostgresTest test
```
Record real exact-SHA Maven test counts and four component job statuses. TestContainers needs a runnable Docker daemon. All test data must remain disposable. No actual family reset, no production file deletion.

## Remaining gates
- Read-only audits, including `REGISTRY_CIPHERTEXT_PRESENT`, **never** authorize Restore, Reset, or delete. Recheck still races; a future separate authorized cleanup needs database family lock, row/lease lock, immutable operation receipt, fresh file inode/hash evidence, and separate security/QA review.
- Architecture approval for B-A-02 versioned original member catalog, tombstone/role ancestry, complete source FK closure, privacy minimization. Backend B's conditional ACK is **not** equivalent to Architecture approval.
- PROD `restorable=false`, `replaySupported=false`, Reset Execute HTTP 423, BETA HOLD.

## Next large package
B-A-03k: exact-SHA CI triage and physical-retention deletion/SQL rollback mismatch rehearsal; implement protective preflight only with safe operator/Architecture decision, otherwise stay in test-only domain. If Architecture ACKs B-A-02, begin versioned member source DTO, producer/inspector contract tests in a separate pinned slice without enabling restore.
