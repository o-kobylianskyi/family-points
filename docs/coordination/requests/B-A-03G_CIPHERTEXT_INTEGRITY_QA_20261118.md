# Backend A B-A-03g — Ciphertext integrity and orphan audit QA (2026-11-18)

Status: code committed; exact-SHA CI pending. Owner Backend A. No new HTTP endpoint, cron, automatic deletion or production Restore.

## Changes
- Local storage `verifyStoredCiphertext(workspaceId,snapshotId,expectedSha256)` reuses the existing bounded, no-follow file reader, validates SHA-256 and source workspace/snapshot envelope, zeroes transient ciphertext bytes. **No decryption** or access to backup keys.
- `WorkspaceBackupOrphanAuditService.audit` now distinguishes `REGISTERED_CIPHERTEXT_INTACT`, `CIPHERTEXT_INTEGRITY_FAILED` and `CIPHERTEXT_READ_FAILED`; formerly `REGISTERED_FILE_PRESENT` gave no byte-integrity assurance. Expired records with intact encrypted bytes remain `EXPIRED_REGISTRY_FILE_PRESENT`. Invalid/missing SHA is `REGISTRY_ARTIFACT_EVIDENCE_INCOMPLETE`.
- Tests verify a physically tampered encrypted file is detected but never removed; mocked registry tampering, missing SHA, already-expired record and read-only behavior.
- A checked ciphertext digest is **not** successful authenticated decryption, owner authorization, replay compatibility or Restore readiness. `restorable=false`, `replaySupported=false`, HTTP 423, BETA HOLD unchanged.

## QA
```bash
./mvnw -Dtest=WorkspaceBackupLocalArtifactStoreTest,WorkspaceBackupOrphanAuditServiceTest,WorkspaceBackupArtifactStagingServiceTest,WorkspaceBackupRegistryStagingCoordinatorTest,WorkspaceBackupStagingProxyIsolationPostgresTest,WorkspacePortableBackupConcurrentSnapshotPostgresTest test
```
Record exact SHA, test counts and each required CI job; no success claim without results. No automatic operator cleanup: a file can be in-flight between disk publication and registry commit, an expired registry may still have an active lease, and disk inventory cannot prove safe deletion. Next package: restart/race rehearsal with age/lease evidence and explicit operator approval, plus physical cleanup/retention safety tests. Approval of B-A-02 FAMILY_MEMBERS portable format still pending Architecture + Backend B.
