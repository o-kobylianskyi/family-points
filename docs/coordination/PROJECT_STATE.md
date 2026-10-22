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

## 2026-10-22 — Backend intake update (SOURCE REVIEW ONLY)
- Backend acknowledged XTR-20261022-P-01 / IMP-0006 at source HEAD `89cbf1e026815dc59d2b1fc5ab9ce06549382524`; calendar + timed reward purchase rules remain **NOT IMPLEMENTED**.
- Found latest Flyway migration V34; existing reward purchase paths have no time gate, TaskService has member/day read-only virtual schedule, Workspace no IANA timezone field.
- Backend acceptance is ownership/inspection only, not a completed API contract or proof of tests/deployment. Backend remains responsible for draft DTO/timezone compat discussion, additive migration, both purchase paths, bounded calendar service and tenant negative tests. Web/Android consumers and Release have not approved IMP-0006.

## 2026-10-22 — Backend draft implementation of calendar (not deployed)
- XTR-20261022-P-01 / IMP-0006 moved from ACK to IN_PROGRESS. Backend opened [draft PR #4](https://github.com/o-kobylianskyi/family-points/pull/4), source `feature/backend-calendar-rewards-v1`, proposed Flyway V35 and REST calendar/reward-window/timezone endpoints. Java 21/Maven/PostgreSQL Testcontainers CI was SUCCESS at code SHA `6b11bbf6` (run 39853638907); code not merged, deployed or client-accepted.
- Web/Android must **review proposed API** in feature branch (docs/api/REWARD_CALENDAR_V1.md), not call it in current BETA. Important distinction: time gating acquisition vs actual reward usage, UTC default for families until set, unchanged legacy LocalDateTime quota/deadline semantics.
- Outstanding P0 tenant/auth gates, consumer reviews, Flyway upgrade, device checks and release approval remain OPEN. Source HEAD observation before this note: `e019c22401bd28b703ddd282c6b9f7b5a0cd201a`.

## 2026-10-22 — Backend PR #4 test state update
- Draft calendar source `feature/backend-calendar-rewards-v1` at `3199c031917938a45b64bf83818f48c29693ad79`: Java 21/Maven/Testcontainers (Flyway V35 verified), Web build and Compose checks all green in https://github.com/o-kobylianskyi/family-points/actions/runs/39854939259. Android job in progress. PR is **DRAFT** and not merged/deployed; XTR-P-01 moved to READY_FOR_REVIEW for cross-track signoff, not DONE. Default existing-family time zone UTC; no old purchase/quotas reinterpretation. Integration-branch HEAD observed prior to this note: `dc5e02fc1a549af7c6bc04373508d3643405056e`.

## 2026-10-22 — Media Architecture request and calendar hardening
- Backend filed `XTR-20261022-B-01` (from XTR-W-01/IMP-0007) to Architecture for entity rights, inheritance, private media storage and Release backup decisions; implementation is blocked until ADR and consumer review.
- Draft Backend calendar PR #4 added inactive reward read protection at `a83563e`; new-source CI pending on check, prior `3199c03` CI was fully green. No merge/deployment; IMP-0006 consumer reviews pending (Android source review acknowledged but blocked).
