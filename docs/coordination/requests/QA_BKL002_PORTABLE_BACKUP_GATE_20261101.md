# QA P0 — BKL-002 portable encrypted backup, FULL/SELECTIVE reset and restore

**2026-11-01 | Source: BKL002_APPROVED_FILE_BACKUP_RESTORE_20261101.md (Architecture); BKL002_PORTABLE_BACKUP_IMPLEMENTATION_20261101.md (Backend).**

**Status: ACCEPTANCE PLAN ONLY, no functional PASS.** The user approved product scope, not destructive execution. Existing reset /execute must remain blocked until a disposable-database gate is fully green and Architecture/Release approve. Never run on persistent Test workspace 14, Test2 workspace 11, BETA or PROD.

## Scenario matrix (isolated disposable PostgreSQL, seeded A/B families)

| ID | Priority | Action | Required evidence |
|---|---|---|---|
| BKP-01 | P0 | Export FULL encrypted backup of A, verify contents and restore on disposable A | Manifest version/epoch/category counts, references, media bytes, exact key scope; B unchanged |
| BKP-02 | P0 | Export SELECTIVE categories of A, import only selected subset | Unselected rows, balances, membership and unrelated settings unchanged; deterministic dependency preview |
| BKP-03 | P0 | Attempt import archive from B into A | Reject foreign workspace binding before writes, no information disclosure |
| BKP-04 | P0 | Corrupt encrypted body/manifest or use wrong passphrase/key | Authentication failure; no preview success, no mutation |
| BKP-05 | P0 | Restart app, rotate deployment version and recover user-held download | Approved portable key policy; test lost-key behavior; no plaintext secret in file |
| BKP-06 | P0 | Archive too large, malformed ZIP/path traversal, duplicate manifests, wrong schema | Bounded rejection, no zip slip/zip bomb, no DB mutation |
| BKP-07 | P0 | Create backup but break file creation, verify, decrypt or write to disk | Never VERIFIED; Reset Execute blocked and existing data intact |
| BKP-08 | P0 | Trigger FULL reset of disposable A after valid VERIFIED backup | Approved retain set: workspace ID, owner/admin access, global auth/catalog, immutable audit, B unchanged |
| BKP-09 | P0 | Trigger SELECTIVE reset with defaults and custom category combination | Explicit category list, no implicit co-deletion; blocked combinations unchanged |
| BKP-10 | P0 | Restore FULL from file into disposable A after erasing supported category fixtures | All references/media/ledger invariants restored, epoch increment once, audit and receipt durable |
| BKP-11 | P0 | Restore SELECTIVE subset while unrelated category changed since export | Preserved current unselected data; conflicts block rather than silently overwrite |
| BKP-12 | P0 | Fault injection between write groups, during audit, epoch update or receipt | Atomic rollback: no partial rows, same epoch, unchanged audit, B unaffected |
| BKP-13 | P0 | Double-submit same UUID, changed payload, concurrent reset and restore | One effect or stable replay receipt; mismatched request 409, no double mutation |
| BKP-14 | P0 | Snapshot expiry 30 days, active download/restore at expiry boundary, manual file export | Server bytes/registry cleaned safely; active operation protected; user-held file not falsely claimed expired |
| BKP-15 | P0 | Financial balances, points, reputation, purchased-but-unfulfilled rewards | Architect-approved ledger/entitlement policy preserved and reconciled, never invented |
| BKP-16 | P0 | Same global account in A and B, admin/member role changes during backup/restore | Owner survives; foreign B not locked or mutated; permission revocation fails closed |
| BKP-17 | P0 | Stale generation N vs N+1 and Android offline replay after reset/restore | Epoch fence rejects stale writes and queue remains HELD, no replay |
| BKP-18 | P1 | Four-locale Web FULL/SELECTIVE reset/restore and 30-day notice | Clearly differentiated destructive action vs restore, accessible confirmation, no execute before API gate |

## Evidence standard and dependencies

1. Backend must publish final versioned API/DTO and approved auth/owner, category FK closure, finance, conflict (replace/merge), key portability, and fresh Google reauth decisions. Proposed endpoints are not executable contracts.
2. Run integration suite with disposable PostgreSQL at pinned SHA, migration V50+ as applicable. Record backup manifest schema, test counts, isolated A/B data checksums, transaction/rollback and foreign-family proofs.
3. Verify download decrypt/reimport after server restart, actual temporary file existence, byte deletion + metadata TTL, safe key storage; no external cloud/object/off-host storage is in approved scope.
4. Assert reset fails closed unless a VERIFIED recoverable protective backup is bound to exact operation/epoch/fingerprint.
5. Mark QA PASS only with actual reports for the **same** Backend code SHA; CI compile, standalone AES-GCM codec tests or side-effect-free Preview are not enough.
6. Device runtime is deferred by user; Android synthetic epoch/outbox coverage remains independent and required.

**Current disposition:** BLOCKED pending Backend implementation and architectural acceptance; do not trigger reset execution or upload on Test/Test2.
