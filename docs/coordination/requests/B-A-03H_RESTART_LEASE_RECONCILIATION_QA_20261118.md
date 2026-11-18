# Backend A — B-A-03h: restart reconciliation, grace period and lease safety

**Date:** 2026-11-18 · **Owner:** Backend A · **Status:** code committed, exact-SHA full CI pending.
**Safety:** no production restore, no automatic garbage collection, no reset execute, no user-facing endpoint, no write SQL.

## Verified baseline
- Previous B-A-03g **FamilyPoints CI SUCCESS** at SHA `4f332030cd39fe7cdad69d14c9ea541fa77425ab`, Actions `44269850500`. This evidence does not apply automatically to subsequent commits.
- Flyway V50: scoped `workspace_protective_backups` with operation uniqueness, source digest, 30-day minimum expiry. V51: SHA/size/stored-at evidence. V52: `workspace_backup_restore_leases` with independently expiring lease, scoped by workspace and snapshot.
- Existing `WorkspaceBackupRetentionService.expireEligible` is a separate explicit operator/coordinator path and locks DB rows. It does not authorize this new read-only audit to delete.
- Physical filesystem publication and PostgreSQL registry commit are not atomic. A crash between them can create legitimate unregistered ciphertext or temporary upload files. An audit cannot tell whether a concurrent backup is still running from disk age alone.

## Implemented in this large packet
1. **No-follow modification timestamp:** `WorkspaceBackupLocalArtifactStore.InventoryEntry` gains `modifiedAt: Instant`, supplied by `Files.getLastModifiedTime(..., NOFOLLOW_LINKS)` for regular files. Legacy five-argument fixture constructor retained. No plaintext or ciphertext is exposed by inventory.
2. **Purely observational reconciliation:** `WorkspaceBackupReconciliationAssessmentService` accepts explicit workspace and bounded maxFiles, uses a fixed 24-hour minimum age before *review consideration*, then checks registry and active `workspace_backup_restore_leases`. Every record includes `deleteAuthorized=false`, including review candidates.
3. **Fail-closed classification:** `UNSAFE_FILE_HOLD`, `UNKNOWN_FILE_AGE_HOLD`, `FUTURE_FILE_TIMESTAMP_HOLD`, `RECENT_FILE_HOLD`, `UNRECOGNIZED_FILE_HOLD`, `DUPLICATE_REGISTRY_HOLD`, `IN_PROGRESS_REGISTRY_HOLD`, `ACTIVE_RESTORE_LEASE_HOLD`, `REGISTRY_OWNED_FILE_HOLD`, `UNREGISTERED_FILE_REVIEW_ONLY`, `STALE_TEMP_UPLOAD_REVIEW_ONLY`. Only exact `.upload-*.tmp` old remnants are temp candidates; arbitrary files never are.
4. **Source unit negatives:** staleness, unreadable age, future clock, stale orphan, crash temp, malformed names, active lease even with expired registry, in-progress registry, stale/expired registry still protected, tenant scope, bounded overflow, and no cleanup SQL or delete calls.
5. **Physical PG18 restart rehearsal:** `WorkspaceBackupReconciliationAssessmentPostgresTest` uses actual Flyway DB, workspace member + registry + lease, encrypted `.fpbackup` and old crash temp on disk. Verifies active lease hold, release of lease still holds `BACKING_UP`, eventual `EXPIRED` record remains protected, orphan and temp merely reviewed, neighboring workspace untouched, zero file deletion.

## Important invariants
- `*_REVIEW_ONLY` is NOT "safe to delete" and is NOT an authenticated archive or Restore eligibility. A separate independent explicitly authorized cleanup, after a fresh lock/recheck + lease and registry validation, is needed before physical deletion. This packet intentionally implements no such action.
- `REGISTERED_CIPHERTEXT_INTACT` from the separate B-A-03g audit is only digest/envelope evidence, not verified decryption or replay. B-A-03h doesn't infer it.
- A restart audit can race with a new writer. The 24h heuristic is deliberately conservative and not a proof of inactivity; never promote it to automatic deletion.
- `EXPIRED` does not grant ownership of a path to this service; approved `WorkspaceBackupRetentionService` remains the only existing expiry coordinator.
- The producer remains the owner of the still-proposed B-A-02 original `FAMILY_MEMBERS` archive contract. Wait for Architecture + Backend B written ACK before schema/format changes.
- Production gates unchanged: `restorable=false`, `replaySupported=false`, Reset Execute HTTP 423, BETA HOLD.

## QA command
```bash
./mvnw -Dtest=WorkspaceBackupReconciliationAssessmentServiceTest,WorkspaceBackupReconciliationAssessmentPostgresTest,WorkspaceBackupOrphanAuditServiceTest,WorkspaceBackupLocalArtifactStoreTest,WorkspaceBackupRetentionPostgresTest,WorkspaceBackupStagingProxyIsolationPostgresTest,WorkspacePortableBackupConcurrentSnapshotPostgresTest test
```
Verify exact SHA Actions job conclusions + Java test reports (including Testcontainers PostgreSQL 18), Web, Android, Compose as required, independently from README and prior green commits. Report any failure and never assert a source-only PASS.

## Next large package — B-A-03i / B-A-04 gate
- CI/PG18 triage for this SHA; add cross-workspace / concurrency race rehearsal at assessment time, detect registry-backed missing file (reverse mismatch), and ensure bounded audit resource usage.
- Add operator read-only combined summary (filesystem extra files AND registry missing bytes) with no sensitive details.
- Prepare Architecture + Backend B decision response for original member catalog, category closure and history; **do not** independently enable Restore.
