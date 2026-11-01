# BKL-002 — Implementation packet: portable family backup and full/selective restore
**2026-11-01 | P0 | Source baseline 77d8d2d4 | Status: owner pickup required**
**User-approved scope:** BKL002_APPROVED_FILE_BACKUP_RESTORE_20261101.md. This is a concrete execution contract proposal, not deployed functionality or authorization to delete data.

## Priority and dependency order
**BE-BKP-01 P0 — portable exporter with verified recovery fixture.**
Implement family-scoped, versioned archive producer reading a consistent source snapshot under family epoch/transaction fencing; include declared category manifest, stable IDs and relationship mapping, workspace identity, schema revision, media payloads where applicable and integrity protection. Exclude global credentials and other-family data. Use bounded staging/streaming, reject archive over configured quota and unsafe paths. Generate checksums and authenticated encryption using existing ProtectiveBackupArtifactCodec only where it fits its 64 MiB plaintext cap; design streaming or bounded chunking for larger exports rather than silently truncating.

**Architecture decision before portable release:** portable exported files should support user-held decryption after app/server redeploy. Propose explicit user-supplied passphrase for downloadable export with versioned strong KDF parameters, authenticated encryption and no passphrase persistence; for automatic server-side 30-day snapshots use a server-managed key with independently recoverable secret provisioning. Existing AES-GCM codec is a cryptographic primitive, not end-user export key management. Key portability and password reset limitations need explicit approval.

**BE-BKP-02 P0 — verified backup + 30-day lifecycle.**
Use existing WorkspaceProtectiveBackupService registry and canonical source fingerprint, implement export worker, durable encrypted file storage local to host (no external cloud/object storage), physical file existence+size+manifest+authenticated decrypt+schema+referential validation, and disposable isolated restore rehearsal where feasible before marking VERIFIED. Attach verified snapshot to exact reset operation/epoch/fingerprint; stale data must invalidate authorization. Surface honest backup state. Implement TTL cleanup of actual file bytes AND metadata with audit/permission/active restore protections. Downloaded files are NOT revoked or deleted after 30 days.

**BE-RST-01 P0 — dependency-checked full/selective reset.**
Implement approved default matrix: in selective mode preselected suggested deletions task/reward/economy/groups/private templates/media; people/settings/currencies kept by default. FULL clears/reset all *permitted family-local* data but retains workspace ID, owner/admin access, global identity/auth, other families, global templates, immutable security audit. Return dependency blockers explicitly; never silently co-clear. Bind preview, proof, confirmed selection, verified backup, operation UUID and source generation; atomic DB mutation, epoch increment and outcome receipt. Existing HTTP 423 must remain until owner/financial/Google reauth+security plan has been approved and synthetic tests pass.

**BE-RSTOR-01 P0 — full/selective family file import.**
Upload -> quarantine -> authenticate/decrypt -> schema/source identity verify -> select categories -> side-effect-free preview (counts, conflicts, reference closure, replace/merge policy) -> explicit confirm+fresh auth -> protect current state -> apply tenant-scoped restore -> receipt/epoch. Default to fail closed for foreign-family archive; no copy between families unless separately approved. FULL means supported family-local archive content, not replacing global accounts, dropping tables, or restoring another family's data. SELECTIVE leaves unselected categories untouched; dependency conflicts return explicit blocking codes. Define per-category replace vs merge as a decision, default BLOCK where ambiguous.

## Minimal API design for owner to refine before clients code
- POST /workspaces/{id}/backups (create manual or pre-reset operation; UUID+epoch)
- GET /workspaces/{id}/backups (scoped list/status)
- GET /workspaces/{id}/backups/{snapshotId}
- GET /workspaces/{id}/backups/{snapshotId}/download (stream encrypted portable file, no-store, manager-auth, audit)
- POST /workspaces/{id}/restores/uploads (quarantined file, bounded, manager ACL; NO changes)
- POST /workspaces/{id}/restores/{uploadId}/preview (FULL/SELECTIVE, categories and conflict policy; NO changes)
- POST /workspaces/{id}/restores/{uploadId}/execute (one-use reauth, current epoch, source fingerprint, UUID; NO operation if blocked)
- GET /workspaces/{id}/restores/operations/{operationId} (recover exact outcome).
These paths are **proposals**; do not invoke from Web/Android until source commits+versioned API contract+CI exist.

## Owners and acceptance
- Backend: exporter, restore, transactional reset, APIs and synthetic disposable PostgreSQL tests as one coherent implementation series, documenting each materially complete packet.
- Architecture: sign full/selective FK/finance/owner matrix; approve key portability and per-category restore conflict modes. Until signed, keep destructive endpoint locked.
- Web: first build mock/read-only UX matching final matrix; enable file download/upload and restore preview only after verified endpoint, reset/restore execute only after security gate. Four languages.
- QA: synthetic roundtrips FULL and SELECTIVE on disposable workspaces; preserve unselected data; same account in two families, same-key replay, rejected tamper/wrong key/foreign file, 30-day expiry and archive loss, media, ledger integrity, backup-failure prevents reset, rollback.
- Android: source epoch and offline queue invalidation, no automatic destructive work.
- Release: local folder permissions, capacity limits, key provisioning, 30-day cleaner and monitoring only. **No external encrypted storage infrastructure**.
- Designer: UX clearly distinguishes Reset (destructive) from Restore (writes/replaces), required confirmation, 30-day *server* expiry vs downloaded file retention.

## Demonstration criterion
**Until a disposable family can export a file, lose its selected local records and restore those records from the exported file with all linked references preserved and a foreign family unchanged, do not call this feature finished.** No real reset or upload here.
