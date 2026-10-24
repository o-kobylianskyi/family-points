# FamilyPoints — shared status snapshot

## 2026-10-24 — Template library and starter family setup (DESIGNED ONLY)
- New cross-track planning packet: `docs/architecture/ADR-TEMPLATES-001.md`, IMP-0008, XTR-20261024-P-01 Backend / -02 Web / -03 Android / -04 Release. Optional initial wizard + existing-family template library; packs + selected individual groups/tasks/reward categories/rewards; user preview/edit/member mapping, explicit idempotent import.
- Current code provisions roles and base points on family creation and provides task economy presets and suggestion flow; it does **not** have full reusable global template-catalog/import. Existing onboarding plan contains a compatible proposed M4 but is not product implementation.
- Task folder WorkNode(GROUP), people MemberGroup and RewardCategory are distinct. Install copies tenant-local editable data with no automatic ledger/purchase, child account or invented member. Dependencies on calendar PR #4 and media work may require compatibility review.
- Documentation committed via PR #7, implementation/consumer ACK, DB migration/tests and release **not done**; inspect new Git main HEAD before work. Previous dated main SHA examples are historical only.


## 2026-10-24 — Verified cross-track audit at main `89a14240` (GitHub source/CI only)
- `main` confirmed default and canonical, currently `89a142400b78002fb814896d23ab049a2a25b390`. Its GitHub Actions run `40083755080` succeeded across Java21/Maven/PostgreSQL Testcontainers, Web Vite, Android Gradle and Compose config at this exact SHA. `main` currently has **no branch protection**; direct changes can land before CI/review.
- Android Soft Family remote branch `9088cf3` is now fully included via merged PR #6 `a961624` on 2026-10-23 19:00 UTC. Remote compare `main...feature/android-soft-family-structure` has 0 commits ahead and 106 behind, meaning no remote-only Android commits remain; user-PC unpushed/untracked content remains unknowable by GitHub and must not be claimed secured. Recent Android version/about + manual update-check code `24e44dd`/`37913aa` was committed to main and built by CI; **signed beta.9 APK install/publication NOT VERIFIED**.
- All 11 named `frontend/public/assets/soft-family/reference/*.webp` now exist in the `main` Git tree, including `hero-family.webp`; previous "assets missing in Git" finding is obsolete. Browser quality/transparency/privacy and visual screenshot acceptance still unverified. Web numeric/stepper and dark-mode fixes followed in main; successful CI does not mean screenshot parity.
- Backend calendar draft PR #4 `a83563e` is OPEN/DRAFT, 8 commits ahead of main and 141 behind; Backend media draft PR #5 `e7e18bc` is OPEN/DRAFT, 5 ahead and 121 behind. Calendar V35 and Media V36 still NOT in main, merged, or deployed. Backend requires contract review, target-level ACL/security, media catalog/WebP/etc, combined migration/upgrade/tenant tests and Release approval. Branch compare counts are historical commit differences, NOT an actual conflict/mergeability verdict.
- Calendar UI design remains AGREED, not implemented; Android/Web calendar displays await real Backend API. Release journal remains at 2026-10-22 intake; no current live VPS, database version, signed APK or browser screenshots were checked in this GitHub audit. Separate BETA release remains gated.
- Current priorities: P0 protect main with required PR checks and force-push block; confirm any local Android-only work; P1 drive Backend PR #4/#5 through integration/test into main; Web visual asset acceptance; Android signed release handoff and actual device checks. Nothing deployed or merged by this documentation-only audit.


## 2026-10-24 — Family admin selective reset (DESIGN ONLY)
- [ADR-FAMILY-RESET-001](../architecture/ADR-FAMILY-RESET-001.md) defines proposed password-confirmed, owner-preserving reset with checklist, exact dry-run counts, dependency checks and optional restart of the SAME first-family/template wizard. Global account/other family data untouched; stale Android offline queues invalidated through workspace generation.
- New cross-track `IMP-0009`; Backend XTR-20261024-P-05, Web -06, Android -07, Release -08. Backend API, migrations, delete logic, security tests, client UI and deployment **NOT IMPLEMENTED/VERIFIED**.
- Password-only Google account compatibility, device policy reset and ledger history/backup retention require track security review. Older project-state entries and SHA snapshots must be read as history.
## Current branch authority — 2026-10-23 (supersedes the historic branch lines below)
- **`main` is the only canonical GitHub source branch**, also repo default. On 2026-10-23 it was safely fast-forwarded from `c6847c6` to former integration HEAD `393f3ee3` (1996 additional commits). Subsequent ADR-COORD-005 and migration documents were committed directly to `main`. Always fetch live `origin/main` HEAD.
- Pre-promotion integration run `39865885022` passed CI; later `main` changes need their own CI checks. Ordinary `main` push does NOT deploy BETA (release workflow gated separately).
- Draft Backend calendar PR #4 / media PR #5 now target `main` but are unmerged. Android remote topic branch diverged; the user reports 18 local-only commits and uncommitted Android/Web files that GitHub cannot access. Must protect and merge safely on developer PC ([instructions](MAIN_BRANCH_MIGRATION.md), XTR-20261023-P-05 Android, XTR-20261023-P-06 Web).
- Older dated entries describing `feature/task-soft-delete` as the integration branch remain historical evidence only, NOT current guidance.


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

## 2026-10-23 — Media status snapshot (source and docs review, not runtime)
- Integration branch reviewed at `d7783ff106b6c6b48fe745073fe2c2ab102afcd5` before ADR decision. Backend media XTR-W-01 / IMP-0007 remains NOT IMPLEMENTED; Backend had requested Architecture response XTR-B-01. ADR-MEDIA-001 now defines v1 workspace-scoped image ownership and **PostgreSQL metadata+BYTEA binary storage**; consumer/Release reviews, code, Flyway, media API and live deploy are still pending.
- Web reference assets issue is distinct: `scripts/web/import-dashboard-reference-assets.ps1` imports 11 WebP from external ZIP; these 11 are missing from committed integration branch. Two existing hero/promo WebP files remain checked into `frontend/public/assets/soft-family/`. Web can use static graphics independently of Backend.

## 2026-10-23 — Media tasks routed, source/CI status
- Four receiving-track requests `XTR-20261023-P-01` Backend, `-02` Web, `-03` Android and `-04` Release now filed; read them via `docs/coordination/requests/README.md`.
- At inspection, Backend Media PR #5 `e7e18bc` remains draft with CI success (run 39863949599), unmerged; calendar PR #4 V35 likewise unmerged. Media migration renamed V36; combined V35/V36 integration and old DB upgrade still need proof. Private target ACL, WebP, catalog binaries, DTO and cleanup remain open.
- The 11 later Web static reference assets were not present in Git's `frontend/public/assets/soft-family/reference/`; static design package independent of Backend DB media. This snapshot is source/CI inspection, NOT running BETA confirmation.
