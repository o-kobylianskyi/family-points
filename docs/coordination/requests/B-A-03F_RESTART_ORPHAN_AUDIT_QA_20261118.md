# B-A-03f — read-only restart orphan inventory and registry audit

**Date:** 2026-11-18. **Owner:** Backend A. **State:** CODE COMMITTED, exact-SHA CI pending. **No automatic deletion.**

## Implementation
- `WorkspaceBackupLocalArtifactStore.inventory(workspaceId,limit)`: explicit workspace path, max 1000 entries, no symlink following, no plaintext or secret reads, no deletion. Reports UUID-named `.fpbackup`, `.upload-*.tmp`, malformed names, symlink entries, file sizes. Bounded inventory fails rather than silently truncating.
- `WorkspaceBackupOrphanAuditService.audit(workspaceId,maxFiles)`: read-only DB lookup per UUID against `workspace_protective_backups`. Codes: `REGISTERED_FILE_PRESENT`, `NO_REGISTRY_RECORD`, `REGISTRY_ARTIFACT_EVIDENCE_INCOMPLETE`, `EXPIRED_REGISTRY_FILE_PRESENT`, `DUPLICATE_REGISTRY_EVIDENCE`, `UNRECOGNIZED_OR_UNSAFE_FILE`. No scheduler, HTTP endpoint, promotions, restore unlock or delete.
- Regression tests: `WorkspaceBackupLocalArtifactStoreTest.inventoryIsReadOnlyAndFindsCrashOrphanTempAndRegularArchive`; `WorkspaceBackupOrphanAuditServiceTest` verifies missing registry, suspicious temp, current and expired registration and no delete.

## What this does NOT prove
- A registry-backed file may still be corrupt; this pass checks name and registry evidence, **not** disk SHA or archive decryption. Only the authenticated storage read/inspector can validate actual file integrity. Classification `REGISTERED_FILE_PRESENT` is **not VERIFIED**, restorable, or recoverable.
- In-flight backup may legitimately create a file before registry commit. Without lifecycle/lease/transaction coordination and grace interval, scanner evidence **cannot authorize physical deletion**.
- Files from abrupt power loss may be incomplete and remain on disk. Scanner reports them for manual investigation, no best-effort self-repair.
- Inventory counts are bounded and deliberately throw on overflow, not partial success.
- Existing protections `restorable=false`, `replaySupported=false`, Reset Execute HTTP 423, BETA HOLD preserved.

## QA commands
```bash
./mvnw -Dtest=WorkspaceBackupLocalArtifactStoreTest,WorkspaceBackupOrphanAuditServiceTest,WorkspaceBackupArtifactStagingServiceTest,WorkspaceBackupRegistryStagingCoordinatorTest,WorkspaceBackupStagingProxyIsolationPostgresTest,WorkspacePortableBackupConcurrentSnapshotPostgresTest test
```
Capture exact head SHA, job conclusions and test reports before asserting PASS. Previous B-A-03e last head may be queued and cannot be used for acceptance of this new audit.

## Next B-A-03g
- Add optional audit-side SHA verification strictly through recorded digest and authorized storage reader, distinguish corrupt vs missing bytes without surfacing archive contents.
- Add race/restart rehearsal (uncommitted registry, pending active backup, expired registry) and safe operator reconciliation policy with explicit age threshold, lease checks and separate approval. No automatic cleanup before architecture + QA gates.
- Wait for Architecture/Backend B ACK of B-A-02 FAMILY_MEMBERS source catalog before any portable format change.
