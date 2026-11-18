# B-A-03i — operator-only orphan reconciliation and concurrent time-of-check proof

Date: 2026-11-18. Owner: Backend A. Producer/source and on-disk retention safety only. No Restore Engine, no automatic deletion, no production access.

## Proven baseline
- B-A-03h exact SHA `041fc3b4aa158203f811ce0a0ccda986705dda17`, Actions run `44271220339`: workflow SUCCESS, four main component jobs green (independent test execution details must be checked in job logs). Earlier SHA `9b71ce0` FAILED, so use exact-SHA status only.
- Backend B's `BACKEND_B_TO_A_MEMBER_SOURCE_CONSUMER_ACK_20261118.md` conditionally accepts privacy-minimized original `FAMILY_MEMBERS` as hidden source catalog, without account credentials or identity inference. Architecture approval and signed vNext wire-format decision remain outstanding. No archive schema changes in this packet.

## Test-only code
- `WorkspaceBackupReconciliationRacePostgresTest`: disposable PostgreSQL 18 + Flyway + two distinct JDBC data sources and synchronization latches. Three negative safety cases:
  1. Registry `BACKING_UP` commit occurs between file inventory and registry SELECT. Scan must classify `IN_PROGRESS_REGISTRY_HOLD` rather than an unregistered orphan, never delete, unrelated family untouched.
  2. Previous inventory classified an old file `UNREGISTERED_FILE_REVIEW_ONLY`. After a registry insert, fresh scan becomes `IN_PROGRESS_REGISTRY_HOLD`; original immutable finding still has `deleteAuthorized=false`.
  3. A restore lease is inserted and committed between inventory and registry SELECT; assessment recognizes `ACTIVE_RESTORE_LEASE_HOLD`, activeLease=true, no delete.
- All race writers are bounded by 15-second timeout with a finally latch release and thread-pool shutdown; no real data touched or HTTP endpoints created.
- Existing B-A-03h physical file fixture covers stale temporary upload, active lease, lease release, expired registry retention and cross-family fencing. Existing B-A-03g ciphertext SHA audit catches modified files without decrypting.

## Operator SOP — advisory only, no destructive command
1. Run `WorkspaceBackupReconciliationAssessmentService.assess`, `WorkspaceBackupOrphanAuditService.audit`, and `WorkspaceBackupRegistryPresenceAuditService.audit` against an explicitly selected workspace, with bounded max count and a documented **read-only** execution context.
2. Preserve exact workspace/snapshot, encrypted-file digest, registry operation ID and state, capture timestamp, file modification time, active lease and current job state. Redact encrypted payloads, keys, tokens, member/account PII; never forward plaintext.
3. Classify anomalies: `*_HOLD` means investigate and preserve; `*_REVIEW_ONLY` means **human investigation only**, not deletion. Only timestamped, old, syntactically recognized remnants are even review candidates; 24 hours is minimum consideration age, not proof of inactivity.
4. Before any future deletion implementation: obtain Architecture/Release/QA signed approval; validate authenticated user/operator scope; require exclusive work-item identity, fresh PostgreSQL row lock under the same transaction as lease/registry/operation-status inspection; re-stat/no-follow and hash the file; deny active/unknown leases and concurrent in-progress operations; ensure file identity has not changed since approval; durable audit receipt and multi-worker idempotent compensation. Treat registry-not-found separately: no row exists to lock, so a workspace-level advisory lock/single-worker lease would be required.
5. Until a separately reviewed and tested cleanup capability exists, **do not remove `.fpbackup` or `.upload-*.tmp` automatically** from orphan findings. Existing `WorkspaceBackupRetentionService.expireEligible` is a separate bounded policy and must not be called as a workaround for orphan deletion.
6. A PASS CI proves code built/tests ran on that SHA; does not prove production Restore or a beta release. Keep `restorable=false`, `replaySupported=false`, Reset Execute HTTP 423, BETA HOLD.

## QA command
```bash
./mvnw -Dtest=WorkspaceBackupReconciliationRacePostgresTest,WorkspaceBackupReconciliationAssessmentPostgresTest,WorkspaceBackupReconciliationAssessmentServiceTest,WorkspaceBackupRegistryPresenceAuditServiceTest,WorkspaceBackupOrphanAuditServiceTest,WorkspaceBackupLocalArtifactStoreTest,WorkspaceBackupRetentionPostgresTest test
```

Required: exact-SHA Java/PostgreSQL + Web + Android + Compose results, test count and no accidental skipped core tests. Confirm no file deletion in any race; preserve observed registry rows and unrelated family.

**Next large package B-A-03j:** capture CI defects, ensure repository callsite audit for retention/cleanup and accidental writable code paths, add registry/lease race detection for the reverse audit, and prepare staged source catalog changes once Architecture gives named B-A-02 ACK. No speculative vNext wire format.
