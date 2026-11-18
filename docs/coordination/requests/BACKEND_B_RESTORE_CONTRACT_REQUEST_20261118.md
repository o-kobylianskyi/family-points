# Backend B → Architecture / Backend A — Restore consumer contract request

**Date:** 2026-11-18. **Priority:** BKL-002 P0 / BETA HOLD. **Status:** REQUESTED, pending Architect and Backend A ACK. **Originator:** Backend B, assigned by project owner. No direct producer changes authorized by this request.

## Split proposed for approval
- Backend A: Backup / Portable Archive producer (source schema, capture, authenticated original member/role inventory, format migration, encryption/digests, retention).
- Backend B: Restore Engine consumer (full/selective apply, original→target identity FK mapping, transactional restore/rollback, replay resistance, target-state verification, recovery receipts).

## Exact existing technical blocker
- `WorkspacePortableBackupExporter.SCOPED_TABLES` omits a `FAMILY_MEMBERS` export category.
- `ProtectiveBackupImportInspector.CATEGORIES` rejects `FAMILY_MEMBERS` under existing V50 and marks member references `IDENTITY_REBIND_REQUIRED`.
- Positive archived member IDs do not prove original-family ownership after loss of the source database; role/name equality cannot authenticate a person.
- Backend B test-only two-PostgreSQL roster/review receipt/protected finance replay currently requires source DB still available or synthetic owner key, and cannot authorize real restore.

## Backend A deliverables requested
1. Propose additive/versioned authenticated original membership ancestry (member IDs, family ownership, role/type, soft-delete and historical transition policy), without exposing credentials, child personal data or secret auth material. Specify privacy and redaction policy.
2. Source category completeness list with FK parents for TASK_EXECUTIONS, REWARD_PURCHASES, POINT_LEDGER, REPUTATION_LEDGER, GROUP_POINT_LEDGER, exchange request/rates, MEDIA, member groups and relevant ownership; empty/legacy representations and source row counts.
3. Signed contract for immutable IDs and source category digest, source snapshot UUID/epoch, artifact compatibility, selective dependency closure, format evolution and historical REFUND source mapping. Document unsupported cases explicitly.
4. Test fixtures with same-role members, soft-deleted/inactive members, unrelated second family, broken currency and missing ledger ancestry. Do not change V50 importer silently.

## Architecture decisions requested
- Actual stable human/member identity mapping and owner reauth for target restore, including shared global account, reactivated/deleted users, cross-family and full-vs-selective policy.
- Mandatory category closure and atomicity boundaries; whether partial restore may preserve existing account/member/role records.
- Approval nonce TTL, idempotent operation keys, persistent audit/receipts and exact authorized source→target map.
- Contract ownership + file/module boundaries; any shared DTO/DB schema change entered in IMP register with consumers.

## Backend B commitments / QA criteria
- Acknowledge A's versioned artifact contract and implement target-side consumer only after sign-off; no direct exporter/portable format changes without consent.
- Synthetic PG18 source/target tests of FK replay, injective mapping and wrong-family negatives, crash rollback, idempotency and cross-category integrity before enabling API.
- Produce exact SHA Maven/Testcontainers evidence and QA Issue #15 handoff. Never infer green current CI from older runs.
- Production Restore stays `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD. This request does not authorize production mutations.

**Backend B handoff:** [BACKEND_B_LATEST](../handoffs/BACKEND_B_LATEST.md).

## B-B-01 consumer evidence — 2026-11-18
- Completed exact V50 export/inspector source audit and 23-category matrix: `docs/coordination/requests/BACKEND_B_RESTORE_DEPENDENCY_MATRIX_20261118.md` @`99f5f812`. No implementation or producer source mutated.
- Architecture/Backend A please ACK the **mandatory graph edges**, source original-membership trust, historical REFUND/REPUTATION/GROUP source mapping, selective omission semantics, media polymorphic target mapping, and phase ordering before production restore contract work.
- All missing original signed membership and cross-category field requirements remain REQUESTED; current inspector explicitly says `RESTORE_ENGINE_NOT_IMPLEMENTED`. No release permission implied.
