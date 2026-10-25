# FamilyPoints — Product backlog execution checklist (2026-10-25)

**Owner:** Architecture / Planning. **Status:** PLANNED, NOT IMPLEMENTED. **Source:** [POST_INTERIM_BETA_BACKLOG](POST_INTERIM_BETA_BACKLOG.md).  
**Release policy:** Existing early BETA continues; no feature rollout or database reset without its own tested release packet. This planning document authorizes preparation/review, **not automatic implementation or deployment**. Assign each cross-track feature to one responsible producer and explicit consumers. 
**Note:** BKL-007 missing currencies is an existing-functionality regression candidate and may be addressed in BETA stabilization; all other feature packages are post-BETA expansion.

## Ordered implementation packages and acceptance

- [ ] **P0 / BETA stabilization — BKL-007 Standard currencies and quantity declension.** Audit POINTS/COPPER/SILVER/GOLD on API/database and UI across old/new workspaces; identify missing vs disabled vs hidden, restore without duplicate rows or changing balances. Auto-propose/initialize default 1/few/many forms in UK/RU/EN/DE, respect overrides and quantity rules 1/2/5/11/21/22/25. Existing Web suggestions aren't saved; display fallback handles only POINTS; Android fields use saved forms only.
- [ ] **P1 / BKL-001 + BKL-004 — Starter and personal TASK templates.** Define reusable schema, create from existing task, preview/edit/assign, initial family presets and permissions; never duplicate historical executions.
- [ ] **P1 / BKL-006 — REWARD templates.** Personal reward presets and family starter packs; preserve purchase modes, price/currency, eligibility, approval, availability windows, TIME/item distinctions, limits and cooldown; preview and edit before activation.
- [ ] **P1 / BKL-002 — Selective family data reset.** Explicit category checkboxes (tasks, executions, rewards, ledgers, media, groups), impact preview, administrator authorization/password, destructive confirmation, repeat onboarding; account/auth/secrets protected by default. User tolerates synthetic data loss but **has not authorized immediate reset**.
- [ ] **P1 / BKL-003 + BKL-005 — Adult fulfillment and history adjustments.** Parent/delegated manager records reward delivery and factual dates/times, adjusts task acceptance/completion/approval/re-evaluation retrospectively. Append-only correction audit with actor, reason, recordedAt vs occurredAt; balance delta entries rather than altering past ledger.
- [ ] **P1 / BKL-008 — Timed reward countdown.** Start only at actual consumption, persist server-authoritative expiry; device/web countdown, one short beep at 5m, two at 2m, three at 1m, distinct expiry alert, no duplicate alerts after resume/reconnect; foreground/background OS notification limits; parent-scoped actions.
- [ ] **P1 / BKL-009 — Installed Android app reward.** Device app picker imports label/icon/package name; permission-scoped metadata and safe image preview on Web; save stable Android package name; when user starts granted reward, atomically start BKL-008 time session and launch installed app from foreground. Missing-app fallback, repeat taps idempotent, no false claim automatic app lock on expiry.

- [ ] **P1 / DESIGNER — Initial seed-content bank (schema-dependent).** After Backend/Architecture approve template schema, prepare age-appropriate task and reward preset collections, first-run family starter packs, UK/DE/EN/RU localizations and matching reviewed icons/artwork; deliver schema-valid versioned seed manifest, no direct DB writes. Brief: `docs/coordination/requests/DESIGNER_INITIAL_SEED_CONTENT.md`.

## Responsibility / ordered handoff

| Track | Responsibilities | Completion evidence |
|---|---|---|
| Architecture | Contracts/priority: currency restoration policy, template vs instance, audit events, timed session vs availability window, app-device identity and access rules | Approved short ADR/contract revisions and dependency decisions |
| Backend | BKL-007 source of standard currencies/forms; template APIs/persistence BKL-001/004/006; selective-reset transaction/preview BKL-002; audit + compensating ledger BKL-003/005; authoritative BKL-008 sessions and BKL-009 app metadata binding | Unit/Postgres/Testcontainers incl. tenant, idempotency, backward compatibility, migration |
| Web | Currency type/forms UX; templates save/instantiate/edit; reset checklist and confirmation; adult correction screens; timer/expiry notification (best-effort); app name/icon rendering and rules | Vite/i18n build, error/409 handling, 4 locales, usable flows |
| Android | Built-in currency UI/forms; templates/rewards; scoped reset where authorized; adult corrections; persistent countdown/5-2-1 audible alerts; installed-app picker/label/icon/package and direct launch | Gradle/unit, on-device smoke/notification/Doze, permission and cross-account tests |
| QA | Verify each finished feature package on matching Backend/Web/APK SHA; no fictional support or automatic PASS; reproduce regressions then retest | Acceptance matrix per package, security/negative paths, regression |
| Release | Publish individual post-BETA increments after green CI + scoped smoke; version pin, manifest, schema/signer consistency; no unsolicited destructive data reset | Deployed SHA/APK version and quick smoke evidence |

## Dependencies / strict sequencing
1. BKL-007 triage independently and early (regression), without waiting for new feature packages.
2. Backend contracts first for BKL-001/004/006; Web and Android can use stable DTOs concurrently.
3. Shared audit/ledger support BKL-003/005 precedes retrospective mutation UI and reward timers.
4. BKL-008 timed session contract precedes BKL-009 launch-on-use integration.
5. QA verifies each package, Release publishes increments. New work does not block already deployed early BETA.

## Status rules
- [ ] PLANNED -> [ ] IN_PROGRESS -> [ ] IMPLEMENTED -> [ ] CI_PASS -> [ ] DEVICE/WEB_QA -> [x] RELEASED only with linked commit/run and deployed version.
- Track journals contain owner-specific work; this file is cross-track source of truth. Never check off implementation based on planning commit.
