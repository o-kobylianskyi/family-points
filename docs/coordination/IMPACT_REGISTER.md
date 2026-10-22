# FamilyPoints — cross-track impact register

**Every chat must review this file after fetching current HEAD.** Change proposal is registered BEFORE breaking implementation. Consumer ACK = impact reviewed, not deployed. Status vocabulary: PROPOSED, ACK NEEDED, IN_PROGRESS, BLOCKED, READY FOR VERIFICATION, VERIFIED, CLOSED, SUPERSEDED. Mark consumer checkboxes only after active code/contract review and dated note in its journal.

## IMP-0001 — Soft Family visuals and family/member images
- Producer / date: Web + Architecture / 2026-10-22. Status: **ACK NEEDED**.
- Change: agreed Soft Family Web baseline, shared illustration/card direction; native Android should align visually without copying React. User-editable family cover/member photos are future requirements, **not proven API functionality**.
- Consumers: Android [ ]; Backend [ ]; Release [ ]; Architecture [ ].
- Actions: Android compare real screenshots; Backend/Architecture define tenant-scoped image storage, upload/delete/auth/cache/API; Release verify asset and theme acceptance.
- Evidence: root development log 2026-10-22; Web dashboard/style changes `54b4203`, newer style commit `5cd66da` (repository inspection). Gate: no claim of final Web/Android parity or public release without verified acceptance.

## IMP-0002 — Registration, Google, username and invitation contracts
- Producer / date: Backend + Architecture / 2026-10-22. Status: **ACK NEEDED**.
- Change: anonymous username-availability matcher correction, Google auth/invite routes/CORS and registration feature flags affect clients and deployment.
- Consumers: Web [x]; Android [ ]; Release [ ]; Architecture [ ].
- Actions: compare real `/auth/*` DTOs, error semantics, SecurityConfig, origin/env flags, active workspace; negative auth cases; Release verify tagged server vs actual BETA.
- **Web review 2026-10-22 (ACK, not verified):** React auth/Google/password API, AuthContext and Backend username/Google/SecurityConfig source reviewed at `43e990ead`; real BETA behavior and cross-client negative tests still pending. See `logs/web.md`.
- Evidence: root development log `a2a5bc5` and deployment journal 2026-10-21. Gate: source fixes do not imply deployed BETA success.

## IMP-0003 — Android release runtime crash and APK publishing
- Producer / date: Android / 2026-10-22. Status: **BLOCKED pending real-device acceptance**.
- Change: release APK 0.1.0-beta.1 startup `IllegalAccessError` reported/reproduced; experimental scoped R8 optimization removed in source.
- Consumers: Release [ ]; Web [x]; Backend [ ].
- Actions: Android rebuild signed versionCode 2 using same key, in-place update/install and launch/login test on phone; Release verify SHA256, version, publication and rollback; clients re-check any API/download metadata change.
- **Web review 2026-10-22 (ACK; BLOCKED pending Android/Release):** `/download` APK/manifest/SHA Web sources reviewed at `43e990ead`; no signed APK/device/server validation by Web. See `logs/web.md`.
- Evidence: development log; source fix `ba0b135`; `docs/ANDROID_APK_DISTRIBUTION.md`. Gate: do **not** publish unverified APK.

## IMP-0004 — Global identity + isolated multi-workspace model
- Producer / date: Architecture / 2026-10-22. Status: **ACK NEEDED** (product design agreed; actual code needs review).
- Change: one global account can join separate families; invitation context persists across registration/Google flow; membership, rights, ledger and task references remain tenant-scoped.
- Consumers: Backend [ ]; Web [x]; Android [ ]; Release [ ].
- Actions: two-family negative authorization/invite tests, correct switching/token scope, onboarding and invitations in both clients; release isolation gate.
- **Web review 2026-10-22 (ACK, not verified):** AuthContext, family selector and scoped API client reviewed at `43e990ead`; tenant-negative and invitation E2E tests remain pending. See `logs/web.md`.
- Evidence: `docs/MULTI_FAMILY_ONBOARDING_PLAN.md` and concept journal 2026-10-13.

## IMP-0005 — Offline Android mutation queue and point ledger
- Producer / date: Android + Architecture / 2026-10-22. Status: **PROPOSED / ACK NEEDED** (requirements, not verified implementation).
- Change: offline task/points changes and delayed sync need authoritative server consistency without duplicate earnings/charges or cross-family writes.
- Consumers: Backend [ ]; Android [ ]; Web [x]; Release [ ].
- Actions: define idempotency key, order, conflict/retry/error semantics and test cases; Android implement/store queue; Web display server-authoritative state; Release verify duplicate/offline/restore paths.
- **Web review 2026-10-22 (ACK of proposal; BLOCKED):** existing task API reviewed at `43e990ead`; waiting for Architecture/Backend idempotent replay and reconciliation contract. Do not claim offline-authoritative UI yet. See `logs/web.md`.

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
