# FamilyPoints — Backend B (Restore Engine) log

## 2026-11-18 — Owner assignment / direction handoff
- Project owner explicitly assigned this conversation **Backend B**. Architecture working split: Backend A owns **Backup & Portable Archive**; Backend B owns **Restore Engine**. Current older architecture GitHub coordination still lists a single Backend; seek Architecture formal ACK before calling this an approved shared ADR.
- Created [BACKEND_B_LATEST](../handoffs/BACKEND_B_LATEST.md) to preserve exact responsibilities, TEST-only prior BKL-002 #25–64, cross-track dependencies and safety gates. Do not overwrite legacy `logs/backend.md` history.
- Backend B handles target-side FULL/SELECTIVE restore, FK/ID mappings, tenant isolation, atomic rollback/idempotency and authorization consumption, source-vs-target integrity/recovery receipt. Backend A owns exporter, portable source schema/categories, artifact encryption/digest, retention and original source-member catalog. Request shared contract through Architecture before cross-track edits.
- Current V50 lacks portable `FAMILY_MEMBERS`; no signed original identity or verified owner reauth. Production reset Execute HTTP423, restorable/replaySupported false, BETA HOLD. Current exact-SHA four-component CI green NOT verified; do not claim.
- **Next: B-B-01** source→target restore dependency matrix and Backend A/Architecture schema needs; then independent QA synthetic PG18 target replay/rollback.

## 2026-11-18 — B-B-01 V50 restore dependency matrix
- Source-reviewed `WorkspacePortableBackupExporter` and `ProtectiveBackupImportInspector`, all 23 current portable categories, checked explicit original member/currency polymorphic parent blockers and RESTORE_ENGINE_NOT_IMPLEMENTED. Authenticated original FAMILY_MEMBERS still unavailable.
- Committed `docs/coordination/requests/BACKEND_B_RESTORE_DEPENDENCY_MATRIX_20261118.md` @`99f5f812` with per-category owner, restore FK and selective closure graph, safe two-pass conceptual ordering, target conflict and parent omission policies, exact Backend A/Architecture/QA gates.
- Appended formal request to A/Architecture; **NO** exporter/format change or real DB write. Next: B-B-02 PG18 target replay mapping and rollback tests after reviewing current consumer helpers; shared contract ACK remains pending, production restore HTTP423/BETA HOLD.
