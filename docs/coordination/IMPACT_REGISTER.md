# FamilyPoints — cross-track impact register

**Every chat must review this file after fetching current HEAD.** Change proposal is registered BEFORE breaking implementation. Consumer ACK = impact reviewed, not deployed. Status vocabulary: PROPOSED, ACK NEEDED, IN_PROGRESS, BLOCKED, READY FOR VERIFICATION, VERIFIED, CLOSED, SUPERSEDED. Mark consumer checkboxes only after active code/contract review and dated note in its journal.

## IMP-0001 — Soft Family visuals and family/member images
- Producer / date: Web + Architecture / 2026-10-22. Status: **ACK NEEDED**.
- Change: agreed Soft Family Web baseline, shared illustration/card direction; native Android should align visually without copying React. User-editable family cover/member photos are future requirements, **not proven API functionality**.
- Consumers: Android [x]; Backend [ ]; Release [ ]; Architecture [ ].
- Actions: Android compare real screenshots; Backend/Architecture define tenant-scoped image storage, upload/delete/auth/cache/API; Release verify asset and theme acceptance.
- Evidence: root development log 2026-10-22; Web dashboard/style changes `54b4203`, newer style commit `5cd66da` (repository inspection). Gate: no claim of final Web/Android parity or public release without verified acceptance.
- **Android review 2026-10-22 (ACK, NOT VERIFIED):** inspected Compose Theme and Android navigation/source; current theme is light-only, and native Soft Family visual parity/avatars are not complete. Media storage/API remains an Architecture+Backend dependency. See `logs/android.md`.

- **User-confirmed visual target (Android, 2026-10-22):** user supplied exact Light and Dark phone mockups as the Android Soft Family target. Android specification (both reference hashes, component anatomy, theme-setting behavior and open acceptance matrix): `docs/ANDROID_SOFT_FAMILY_UI_SPEC.md`. Visual agreement is NOT build/device acceptance and adds no new API contract.

- **User clarification (2026-10-22; Android + Web visual acceptance):** do NOT insert full mockup images or cropped cards with embedded text as UI. Rebuild live native/Web controls and dynamic data, adapt layout, isolate text-free illustration assets only, and localize all labels/content. Exact rules in `docs/ANDROID_SOFT_FAMILY_UI_SPEC.md` and `docs/FAMILYPOINTS_SOFT_FAMILY_DESIGN.md`; this is a design constraint, NOT evidence of implementation, testing or accepted visual parity.

## IMP-0002 — Registration, Google, username and invitation contracts
- Producer / date: Backend + Architecture / 2026-10-22. Status: **ACK NEEDED**.
- Change: anonymous username-availability matcher correction, Google auth/invite routes/CORS and registration feature flags affect clients and deployment.
- Consumers: Web [x]; Android [x]; Release [ ]; Architecture [ ].
- Actions: compare real `/auth/*` DTOs, error semantics, SecurityConfig, origin/env flags, active workspace; negative auth cases; Release verify tagged server vs actual BETA.
- **Web review 2026-10-22 (ACK, not verified):** React auth/Google/password API, AuthContext and Backend username/Google/SecurityConfig source reviewed at `43e990ead`; real BETA behavior and cross-client negative tests still pending. See `logs/web.md`.
- Evidence: root development log `a2a5bc5` and deployment journal 2026-10-21. Gate: source fixes do not imply deployed BETA success.
- **Android review 2026-10-22 (ACK, NOT VERIFIED):** inspected AuthApi/AuthSessionManager and Backend username-availability endpoint. Native password/Google/workspace APIs are present; username availability and invitation acceptance are absent from reviewed native AuthApi. Requires signed-device Google test and BETA contract checks. See `logs/android.md`.

## IMP-0003 — Android release runtime crash and APK publishing
- Producer / date: Android / 2026-10-22. Status: **BLOCKED pending real-device acceptance**.
- Change: release APK 0.1.0-beta.1 startup `IllegalAccessError` reported/reproduced; experimental scoped R8 optimization removed in source.
- Consumers: Release [ ]; Web [x]; Backend [ ].
- Actions: Android rebuild signed versionCode 2 using same key, in-place update/install and launch/login test on phone; Release verify SHA256, version, publication and rollback; clients re-check any API/download metadata change.
- **Web review 2026-10-22 (ACK; BLOCKED pending Android/Release):** `/download` APK/manifest/SHA Web sources reviewed at `43e990ead`; no signed APK/device/server validation by Web. See `logs/web.md`.
- Evidence: development log; source fix `ba0b135`; `docs/ANDROID_APK_DISTRIBUTION.md`. Gate: do **not** publish unverified APK.
- **Android producer review 2026-10-22 (ACK of source; STILL BLOCKED):** reviewed Gradle release source fix and Android release documentation. Real signed versionCode 2 upgrade/startup/login evidence is still required before Release publication. See `logs/android.md`.

## IMP-0004 — Global identity + isolated multi-workspace model
- Producer / date: Architecture / 2026-10-22. Status: **ACK NEEDED** (product design agreed; actual code needs review).
- Change: one global account can join separate families; invitation context persists across registration/Google flow; membership, rights, ledger and task references remain tenant-scoped.
- Consumers: Backend [ ]; Web [x]; Android [x]; Release [ ].
- Actions: two-family negative authorization/invite tests, correct switching/token scope, onboarding and invitations in both clients; release isolation gate.
- **Web review 2026-10-22 (ACK, not verified):** AuthContext, family selector and scoped API client reviewed at `43e990ead`; tenant-negative and invitation E2E tests remain pending. See `logs/web.md`.
- Evidence: `docs/MULTI_FAMILY_ONBOARDING_PLAN.md` and concept journal 2026-10-13.
- **Android review 2026-10-22 (ACK, NOT VERIFIED):** inspected workspace listing/activation, token switch validation and offline-scoped account behavior. Cross-family negative authorization and invitation E2E tests remain required. See `logs/android.md`.

## IMP-0005 — Offline Android mutation queue and point ledger
- Producer / date: Android + Architecture / 2026-10-22. Status: **PROPOSED / ACK NEEDED** (requirements, not verified implementation).
- Change: offline task/points changes and delayed sync need authoritative server consistency without duplicate earnings/charges or cross-family writes.
- Consumers: Backend [ ]; Android [x]; Web [x]; Release [ ].
- Actions: define idempotency key, order, conflict/retry/error semantics and test cases; Android implement/store queue; Web display server-authoritative state; Release verify duplicate/offline/restore paths.
- **Web review 2026-10-22 (ACK of proposal; BLOCKED):** existing task API reviewed at `43e990ead`; waiting for Architecture/Backend idempotent replay and reconciliation contract. Do not claim offline-authoritative UI yet. See `logs/web.md`.

- **Android review 2026-10-22 (ACK of code, OPEN):** inspected SQLite queue, PointsRepository replay, Backend PointController/PointService idempotent UUID operation path. Server code contains duplicate protection, but phone retry/restart/reconciliation and offline tasks are not verified. Architecture must still formalize the broader replay/conflict contract. See `logs/android.md`.


## IMP-0006 — Unified task and reward calendar + acquisition time windows
- Producer / date / status: Architecture / 2026-10-22 / **PROPOSED; ACK NEEDED** (product goal agreed, Backend API/DB details awaiting consumer review).
- Current → target: task virtual dated schedule exists but only daily per-member GET; reward catalog currently lacks time-based acquisition schedule. Add bounded read-only unified TASK/REWARD_WINDOW calendar projection, reward availability mode/windows, workspace timezone strategy and final server-side purchase gate in both direct and approved-request paths. All legacy rewards remain ALWAYS by default; old reward update clients must preserve configured schedules.
- Breaking/compatibility: primarily additive DTO/API/Flyway **if implemented correctly**; JVM LocalDateTime vs new workspace timezone and old PUT reset behavior are explicit migration hazards. No task existing occurrence/ledger semantic changes.
- Consumers: Backend [x]; Web [ ]; Android [ ]; Release [ ]; Architecture [x] (Backend ACK is source review/ownership, not final API compatibility or test acceptance).
- Consumer requirements: Backend owns service/schema/security/DTO and tests; Web owns calendar + manager editor + translated availability UX after current style milestone; Android owns Compose calendar, schedule display/editor after theme milestone and read-only offline view; Release independently checks migrations, tagged server and clients.
- Evidence: `docs/architecture/ADR-CAL-001.md`, `docs/coordination/requests/XTR-20261022-P-01.md`, `XTR-20261022-P-02.md`, `XTR-20261022-P-03.md`. Current source references TaskService.getMemberSchedule, TaskScheduleSlot, RewardDefinition/RewardService.
- **Backend ACK 2026-10-22:** received XTR-20261022-P-01; reviewed existing RewardService direct/request purchase transactions, reward DTO/entity, TaskService virtual day view, Workspace timezone absence and current migrations at `89cbf1e026815dc59d2b1fc5ab9ce06549382524`. Confirmed no reward time gate or unified calendar yet. Final timezone fallback, DST behavior, new DTOs and migration strategy require follow-up; **NO CODE/BUILD/TEST** in acknowledgment. See `logs/backend.md`.
- Tests/release gate: ONE_TIME/week windows, exact bounds, DST, tenant isolation, no ledger debit outside window, both purchase paths, existing task history/slots, old client compatibility, 2-family integration, Flyway and CI. **NO SOURCE/TEST/DEPLOY CLAIM**.

## IMP-0007 — Media avatars, family cover and task icon inheritance
- Producer / date / status: Web → Backend + Architecture / 2026-10-22 / **PROPOSED; ACK NEEDED**. Extends IMP-0001; not implemented.
- Old → new: no verified generic media upload contract → additive backend-owned catalog/upload/storage/asset-assignment API for member/account avatars, people-group avatars, task group/category icons, task-level override with category inheritance, and workspace family cover + optional workspace avatar.
- Consumers: Backend [x]; Architecture [ ]; Web [x]; Android [ ]; Release [ ]. Web [x] means initiating request / required visual contract acknowledged, **not deployed**.
- Required decisions: account-global vs member-in-workspace avatar, MemberGroup vs WorkNode/group/category, workspace media authorization, curated catalog IDs, file storage/backups/cache, derived effective image semantics and older client compatibility.
- **Backend review 2026-10-22 (ACK SCOPE, DESIGN BLOCKED):** read XTR-W-01; agrees to own implementation after Architecture media ADR and storage/release safety decisions. No code/schema/API acceptance or deployment implied. See Backend journal.
- Web review: approved Soft Family designs and task/category icon inheritance UX. **Backend receives XTR-20261022-W-01**; its chat must fetch and acknowledge independently, with Architecture ADR before final API/DB commitment.
- Evidence: `docs/coordination/requests/XTR-20261022-W-01.md`, related IMP-0001.
- Tests/release gate: cross-family isolation, inherited/overridden images, upload validation/EXIF stripping, default catalog and fallbacks, migration/backup and backward compatibility. No Backend implementation, CI or deployment yet.

- **Backend producer update 2026-10-22:** implementation underway in [draft PR #4](https://github.com/o-kobylianskyi/family-points/pull/4) on `feature/backend-calendar-rewards-v1`; initial Maven/PostgreSQL Testcontainers CI successful at `6b11bbf6`. Separate API draft `docs/api/REWARD_CALENDAR_V1.md` lives on feature branch. Flyway V35, direct and approved-request gates, bounded calendar and explicit time zone have been committed but **not merged or deployed**. Web/Android/Architecture/Release consumer ACK, broad tenant-negative/DB upgrade/device tests remain pending. Backend journal contains exact evidence. Do not assume endpoints exist on integration branch.

- **Backend CI follow-up 2026-10-22:** draft PR #4 source `3199c03` now passes Java 21/Maven/PostgreSQL Testcontainers tests, including clean Flyway V35 schema and calendar tenant negative unit checks (run 39854939259). Also passes Web and Compose jobs; Android remained running on observation. Still unmerged/not deployed; consumer review remains open. `RewardDefinitionResponse.availabilityMode` was added as compatible catalog hint; `GET .../availability` remains time-only authoritative status.

## Template for each next impact
```md
## IMP-000X — Summary
- Producer / date / status:
- Old → new contract; breaking or compatible:
- Consumers: Backend [ ]; Web [ ]; Android [ ]; Release [ ]; Architecture [ ] (only affected)
- Consumer review + exact SHA / date / action:
- Evidence: ADR, issue, PR, commit:
- Tests, migration, rollback, release gate:
```

Never remove an unresolved item to tidy the register. Close only after affected consumers acknowledge and required verification is evidenced.

