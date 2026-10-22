# FamilyPoints — shared status snapshot

**Snapshot date:** 2026-10-22. This is repository/log inspection only, **not live VPS/CI/device verification**. Always refresh current SHA before work.

- Repository: `o-kobylianskyi/family-points`.
- Active development branch observed: `feature/task-soft-delete`; HEAD immediately before initial coordination documentation: `5cd66da181003110f13d78967d3b78dbac88e401` (2026-10-22). This may change at any moment.
- Default branch `main` observed: `c6847c6389514aa2085951d2d07e5cd364b86fd9` (2026-10-08), behind the development branch with Android, deploy and current Web work.
- Root development log/checklist, concept log and Contabo log already existed; preserve prior work and actual runtime evidence.
- Five tracks now have dedicated journals. Architecture owns shared ADRs and cross-track priority; each producer enters impacts; consumers acknowledge reviews; Release owns publishing approval.

## Outstanding cross-track gates
- IMP-0001: Soft Family Web UI ↔ native Android parity, family/member pictures requiring scoped backend media design.
- IMP-0002: registration, username availability, Google/invite auth and CORS need verification across both clients and BETA.
- IMP-0003: real-device verification of corrected signed Android release after R8 startup crash before publishing another APK.
- IMP-0004: independent family workspace authorization and invitation flow across clients; test tenant isolation.
- IMP-0005: offline sync needs agreed idempotent point/task write contract before claiming release readiness.

No code, APK, server or DB deployment occurred when establishing this coordination system. Update this snapshot only with newly verified observations; append historical details to the track journals.

## 2026-10-22 — Calendar planning snapshot (design only)
- Cross-track IMP-0006 is OPEN / ACK NEEDED: unified TASK+REWARD_WINDOW calendar and time-gated reward acquisition. Design: `docs/architecture/ADR-CAL-001.md`.
- Backend first: XTR-20261022-P-01 (reward schedule/purchase gate/calendar DTO/schema/timezone compatibility). Web XTR-20261022-P-02 and Android XTR-20261022-P-03 planned **after** ongoing Soft Family styling milestones.
- Important audited gap: current TaskScheduleSlot scheduledTime is deadline; Workspace timezone not persisted, and reward limits/deadlines rely on JVM local clock. Must resolve before claiming calendar release readiness.
- This is a plan/document update only: no Java/React/Kotlin code changes, builds, tests, CI, DB, APK or VPS operation.
