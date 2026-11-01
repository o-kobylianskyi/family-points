# BKL-002 — Approved reset matrix and file backup import/export revision (2026-11-01)

**Decision status:** User-approved product requirements. Backend/Architecture still need dependency-safe technical mapping; no destructive operation or deployment authorized.
**Supersedes storage assumptions in:** BKL002_BACKUP_RESET_SCOPE_20261101.md and earlier dispatch entries where they require off-host encrypted object storage.
**Owners:** Architecture P0, Backend P0, Web P0, QA P0, Android P1, Release/DevOps only for runtime storage/ops.

## User-approved decisions

1. **Cancel external encrypted backup storage.** Do not provision cloud/object/off-host storage or block BETA on it. Local server temporary storage and user-managed exported file are sufficient scope. Encrypt exported backups and protect locally held temporary artifacts; retain key handling and authorization requirements.
2. **File backup export and import are REQUIRED.** A manager must be able to download an encrypted portable backup file and later upload it to restore the same selected family, subject to secure binding/authorization and schema compatibility. No raw full-database `pg_dump` import through tenant UI.
3. **Import/restore must support FULL and SELECTIVE**, parallel to full/selective reset. User picks exactly which compatible data categories to restore, with a dry-run/conflict/dependency preview. Unselected categories stay unchanged; no silent implicit deletion/merge or cross-family writes.
4. **Approve default reset matrix** as provisional product behavior, with further fine-tuning during implementation:

| Category | SELECTIVE default | FULL mode |
| --- | --- | --- |
| Task definitions, schedules, executions, history | DELETE | DELETE |
| Reward definitions, requests, purchases | DELETE | DELETE |
| Balances, points/reputation and relevant economic history | DELETE, dependency-safe policy | DELETE, dependency-safe policy |
| People groups and task folders | DELETE | DELETE |
| Private templates | DELETE | DELETE |
| Family-uploaded media | DELETE where dependency-safe | DELETE where dependency-safe |
| Non-owner family participants | RETAIN | DELETE where dependency-safe; owner remains |
| Workspace currencies/config | RETAIN | DELETE/reset-to-default where dependency-safe |
| Family settings | RETAIN | RESET to defaults where dependency-safe |
| Global account/auth, family ID, guaranteed owner/admin access, other families, global catalog, immutable security audit | ALWAYS RETAIN | ALWAYS RETAIN |

**Important:** the SELECTIVE column is the *initial suggested selection*, not an automatic unconfirmed operation. For FULL all allowed family-local data is cleared/reset, while exceptions are shown explicitly. Underlying transaction/retention law and FK safety may require blocking a requested combination; never silently broaden scope.

5. **Protective backup before reset**: verified recoverable snapshot, retention 30 days from creation, fail closed if creation/verification fails. If an exported file is held by the user, local TTL cannot delete that downloaded copy: explain this clearly.
6. **Independent manual backup management**: create backup, list, download file; upload/import via full or selective restore. Built-in automatic backup before reset remains a P0 requirement; full/selective file restore also P0 for a credible rollback path. Scheduled periodic backups are outside this P0 scope.

## Backend contract to design and implement

- Implement versioned encrypted portable **family-scoped archive** with manifest schemaVersion, workspace binding, snapshot ID, source epoch, category manifest, integrity MAC/hash, required media/object bytes and data dependencies. The existing AES-GCM codec is a foundation but not itself an export/import service.
- Design key portability: user-downloadable file must be decryptable after application restarts and without storing secrets in the file. Use securely managed server key or user-provided export passphrase with explicit recovery consequences; Architecture selects policy and tests lost-key behavior. Avoid storing encryption keys alongside downloadable blobs.
- Export must include enough data and identifiers to reconstruct relationships without importing global account credentials or overwriting unrelated families.
- Restore flow: **upload -> verify integrity/authorization -> preview category counts/conflicts/FK/retention -> select FULL or SELECTIVE -> explicit confirmation + fresh reauth -> protective snapshot of current target state -> apply transactionally or via durable compensating workflow -> audit -> increment dataEpoch -> client cache/offline quarantine -> receipt/result**.
- Restore authorization is family owner/admin per approved authority, plus safe identity binding. Restoring other-family data is rejected unless an explicit approved migration/copy policy is added later. No direct PostgreSQL dump restore in shared database.
- FULL restore means full supported family-local content from the file, *not* dropping DB or users; SELECTIVE restore applies only requested categories. Define conflict strategy per category (replace vs merge) explicitly in preview; **default fail-closed on ambiguous conflicts**, no silent overwrite.
- Status must distinguish REQUESTED/BACKING_UP/EXPORTED/VERIFIED/FAILED/EXPIRED and RESTORE_PREVIEW/RESTORE_APPLIED/RESTORE_FAILED as appropriate; don't claim VERIFIED without actual recoverability validation.
- 30-day TTL for copies held on the FamilyPoints server; cleanup actual bytes plus registry, ensure active downloads/restores protected. Downloaded copies belong to user and are not expirable remotely; no false promise that those files vanish after 30 days.
- Avoid unbounded memory processing: existing codec has 64 MiB plaintext cap; use bounded/streaming archive, limits, quotas and safe ZIP/path handling or explicit rejection for oversized data.

## UX and ownership

- **Architecture P0:** update ADR-FAMILY-RESET-001 retention/deletion matrix, identity/financial/audit rules and portable key strategy, replacing off-host requirement.
- **Backend P0:** implement portable export/import, verified backup, full/selective reset/restore with epoch/family safe transactions, FK-safe mapping, idempotency and tests.
- **Web P0:** settings > Family data with Full/Selective Reset, download backup, upload file, Full/Selective Restore, review of what is deleted/restored/retained and real snapshot expiry.
- **Android P1:** epoch/offline queue invalidation; optional read-only backup status; no background file upload or destructive queue.
- **QA P0:** disposable PG synthetic roundtrip, cross-tenant isolation, partial import (only selected categories), conflicts, wrong key/tamper, upload limit, old schema, 30-day expiry, retries and ledger invariants. Browser tests conditional per user policy.
- **Release / DevOps P1:** only existing server local storage directory permissions, disk size/monitoring, reliable TTL cleanup, key provisioning/backup and deployment configuration. No external cloud storage procurement/setup.
- **Designer P1:** 4 locales, clear visual separation of Reset versus Restore, full/partial selection, safety warnings, expiry messaging.

## Hard safety boundaries

- No reset without a VERIFIED recoverable pre-reset snapshot and approved execution policy.
- Do not claim a generic file-based restore can reverse all external side effects; clearly identify items outside scope.
- No user data deletion or restoration has been authorized by this decision alone. Existing `/execute` stays disabled until safety/QA gates pass.
