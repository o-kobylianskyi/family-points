# FamilyPoints — Backend B (Restore Engine) log

## 2026-11-18 — Owner assignment / direction handoff
- Project owner explicitly assigned this conversation **Backend B**. Architecture working split: Backend A owns **Backup & Portable Archive**; Backend B owns **Restore Engine**. Current older architecture GitHub coordination still lists a single Backend; seek Architecture formal ACK before calling this an approved shared ADR.
- Created [BACKEND_B_LATEST](../handoffs/BACKEND_B_LATEST.md) to preserve exact responsibilities, TEST-only prior BKL-002 #25–64, cross-track dependencies and safety gates. Do not overwrite legacy `logs/backend.md` history.
- Backend B handles target-side FULL/SELECTIVE restore, FK/ID mappings, tenant isolation, atomic rollback/idempotency and authorization consumption, source-vs-target integrity/recovery receipt. Backend A owns exporter, portable source schema/categories, artifact encryption/digest, retention and original source-member catalog. Request shared contract through Architecture before cross-track edits.
- Current V50 lacks portable `FAMILY_MEMBERS`; no signed original identity or verified owner reauth. Production reset Execute HTTP423, restorable/replaySupported false, BETA HOLD. Current exact-SHA four-component CI green NOT verified; do not claim.
- **Next: B-B-01** source→target restore dependency matrix and Backend A/Architecture schema needs; then independent QA synthetic PG18 target replay/rollback.
