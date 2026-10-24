# FamilyPoints — QA test board for INTERIM BETA

**Last assembled:** 2026-10-24 · **Source:** `origin/main` (verify live HEAD on every QA run). **Actual deployed BETA SHA / signed Android APK version:** NOT VERIFIED. Therefore listed tests below are recommended, **not yet passed**.

## Status legend
`READY_TO_TEST` = test scenario exists and test environment confirmed compatible; `BLOCKED` = prerequisite not confirmed/available; `NOT_VERIFIED` = code or environment uninspected in runtime; `PASSED` = manual recorded outcome on pinned version; `FAILED` = reproducible issue; `NEEDS_RETEST` = a fix is available but not manually reverified.

| ID | Urgency | Test | Where | Current status | Blocker / trigger |
|---|---|---|---|---|---|
| QA-001 | P0 | Child requests 30-min game reward; parent approves; child purchases; EXACTLY ONE debit; stays logged in | Android + BETA API/DB | BLOCKED | Confirm BETA actually deployed main V35 reward migration and fixed Android APK; use test family |
| QA-002 | P0 | Repeated tap/network repeat on approved reward request never debits twice; returns friendly 409 | Android + BETA API/DB | BLOCKED | QA-001 environment confirmed; review latest backend fix deployed |
| QA-003 | P0 | Android session survives brief 5xx/408/429, legitimate auth errors handled | Android on physical device + BETA | NOT_VERIFIED | Need compatible APK and controlled failure without exposing tokens |
| QA-004 | P0 | Offline +10/-10/custom points, reconnect and refresh = no duplicate transactions | Android on device + BETA | NOT_VERIFIED | Verify offline-enabled APK, test family ledger history |
| QA-005 | P0 | Family A/B switching isolates members, roles, balances, history, rewards and cached images | Web + Android + BETA | NOT_VERIFIED | Two test families sharing an adult global account |
| QA-006 | P0 | Release signed APK installs/upgrades on real handset, login/Google works, no startup crash | Physical Android phone | BLOCKED | Release provides signed candidate/version/checksum and prior installed version |
| QA-007 | P0 | Backend fresh + POPULATED DB migration V35 reward → V36 calendar → V37 media; backup & restore | Disposable PostgreSQL + Release | BLOCKED | Draft PR #4/#5 not yet merged; no destructive tests on live DB |
| QA-008 | P1 | Web create/edit task, Escape while saving, and role-appropriate completion/approval | Web browser + BETA | NOT_VERIFIED | Browser session, live API version, two roles |
| QA-009 | P1 | Android Dashboard/Tasks/Members/Points/Rewards pull-to-refresh finishes with actual request, not timeout | Android + BETA | NOT_VERIFIED | Verify latest main refresh fixes in APK; network simulation |
| QA-010 | P1 | Calendar real API, timezone and DST, task occurrence and reward window | DEV/isolated Backend → Web/Android when API available | BLOCKED | PR #4 draft/unmerged and consumers not accepted |
| QA-011 | P1 | Private family cover/avatars/icon, tenant ACL and catalog display | DEV/isolated Backend + clients | BLOCKED | PR #5 draft/unmerged; no claim media API is on BETA |
| QA-012 | P1 | Web dark/light responsive UI, image URLs, reward negotiation modal Esc/save flow | Browser + BETA | NOT_VERIFIED | Actual deployed web SHA and screenshots needed |
| QA-013 | P0 | End-to-end critical flow: new/login family, invite child, task, completion, approval, balance, reward | Web + Android + BETA | BLOCKED | Stable deployed API/APK and test identities needed |

**Backlog, NOT interim-BETA acceptance:** global family templates (IMP-0008), selective admin reset (IMP-0009), parent manual actual reward issuance/history corrections, fast personal task templates. Do not test unsupported features as if shipping.

## Freshness checks before issuing a test to the user
1. Read current `main` and recent commits, all five track journals, open PR/CI and actual Release note if available.
2. Compare Git commit versus real BETA deployed version and APK versionCode; `main` is **not necessarily BETA**. When unknown, ask tester to report exact installed version or Release log.
3. Prefer quick realistic tests runnable without new deploy (Web login, existing task editor, Android installed app checks) while marking version uncertainty; never claim success without user's observed result.
4. After manual result update row status, date, environment/build and add a reproducible issue to QA log if failed. The QA chat suggests next 3–5 tasks and a progress scale (verified pass / tested fail / blocked / not run).


## Web QA handoff: WEB-SF-01 — Soft Family full stabilization (2026-10-24)
- **Owner executing QA:** QA track. **Owner fixing failures:** Web. **Current state:** NOT_VERIFIED, waiting exact deployed Web SHA and manual viewport/theme tests. **Source suite:** `docs/coordination/QA_TEST_CASES.md#web-sf-01--complete-soft-family-visual-regression-package-handoff-from-web-2026-10-24`.
- Execute entire WEB-SF-01 matrix as a *single test package*, not isolated reports on one CSS rule: navigation, tasks, members/groups, points, rewards, auth/settings, keyboard, responsive sizes, 4 locales and unchanged approved Home. Record per-case PASS/FAIL with browser, viewport, theme, locale, screenshots and reproduction.
- Next Web cycle: consolidate **all** QA defects into one coherent correction package, check Web build/lint/i18n, submit exact SHA to QA for whole regression. Do not close on CI alone.
- Existing QA-012 remains NOT_VERIFIED until test reports are recorded; new features outside intermediate-release scope remain blocked.

## Critical Backend B-03/B-04/B-05 — QA intake (2026-10-24)
- **Pinned candidate:** draft PR #13, SHA `a19b2adfa68a3e8a4fc625239289bc01f9ef7efa` (Reward V35 + Calendar V36 + Media V37).
- **Automated evidence:** CI run 40110260210: Backend/PostgreSQL, Web and Compose passed; Android failed. A green `main` run on a different SHA does not validate PR #13.
- **Suite:** `docs/qa/QA_BACKEND_INTEGRATION_V35_V37.md` on PR #13; intake `docs/coordination/requests/XTR-20261024-B-03-QA.md` on `main`.
- **Status:** Manual Backend API QA **NOT_VERIFIED**, BETA database clone / signed APK / device / Release **BLOCKED**. No QA feedback or recorded manual failures were found at intake.
- **Check together:** child-approved 20-point reward exactly once and 409 retry without logout; two-family Media ACL/revoke/stale version; calendar 62/63 days, permissions/DST/availability windows; populated PostgreSQL V35→V36→V37; Android account session and signed APK. Report actual SHA/environment, expected/actual, redacted HTTP/SQL and screenshots.
- Collect defects into one return-to-Backend remediation batch on same PR. No merge/deploy or manual PASS inferred from code/CI alone.


### WEB-SF-01 audit outcome — 2026-10-24
- GitHub source-only audit logged at `docs/coordination/logs/qa.md`; consolidated handoff: [`WEB_SF_01_QA_REPORT.md`](WEB_SF_01_QA_REPORT.md).
- Ten WEB-SF-01 browser matrix blocks: **BLOCKED (0 PASSED)** due no authenticated browser session, no deployed BETA SHA, missing visual evidence. Local clone/npm blocked by runner DNS.
- Confirmed source defects: F01 P1 Ukrainian literal in reward negotiation reputation label; F02 P2 English-only shared number stepper accessibility labels. R01 repeated submit is a risk, **not** reproduced.
- Owner Web to fix in one batch; QA retests complete 4-locale × 2-theme responsive matrix at pinned deployed SHA. Release remains **NO-GO** for WEB-SF-01.


## CURRENT QA INTAKE / authoritative latest snapshot — 2026-10-24 evening
> This section supersedes older snapshot rows above where they refer to PR #13 as Draft or Calendar/Media migrations as not integrated. **CI success is not a user-acceptance PASS.** Actual BETA deployed SHA, signed APK version and real test identities remain unverified.

| ID | Priority | Owning track | QA acceptance scenario | Status | Prerequisites / evidence |
|---|---|---|---|---|---|
| QA-WEB-01 | P1 | Web | WEB-SF-01 seven main routes, two themes, uk/de/en/ru, 1440/1366/390/360 widths, 200% zoom, dialog keyboard and approved Home baseline | NEEDS_RETEST | Source fixes F01/F02 and synchronous modal mutation guards; real browser screenshots+exact SHA missing |
| QA-WEB-02 | P0 | Web | Reward V36 availability editor: 0..56 weekly windows; preserve all ID/values on edit; add/delete/disable, overlaps, limits, ONE_TIME +01/+02 offset round trip, DST, 403/409 and no debit outside window | BLOCKED | V36 verified deployed, authorized adult/child test identities, test wallet |
| QA-WEB-03 | P0 | Web | Media V37 cover/avatar/group/task icons: catalog + private upload <=5MiB, authorized bytes, 403/404/409, clear/default and A→B switch with delayed responses, revoke Blob URLs | BLOCKED | Deployed V37, two synthetic workspaces, browser Network/screenshots. Dashboard approved art unchanged |
| QA-WEB-04 | P1 | Web | Calendar V36 list/from-to 62 vs 63 day, type/member filters, workspace timezone manager permissions, DST and empty/virtual rows | BLOCKED | Deployed V36 and role-specific test accounts |
| QA-AND-01 | P0 | Android | AUTH-01..04, RW-01..02, OFF-01..03, REF-01..03, UI-01..02 per XTR-A-05 including transient 5xx / true 401 and offline queue exactly once | BLOCKED | Matching installed APK, BETA backend SHA/flags, test account + wallet; RW additionally requires deployed V35 |
| QA-AND-02 | P0 | Android+Backend | OFF-04 cross-*global-account* offline queue must never leak/replay, legacy queue ownership migration | BLOCKED | Immutable verified accountId contract and approved legacy migration required; never replay ambiguous queue |
| QA-AND-03 | P1 | Android | Calendar native day/week/month real V36 events, workspace switching/stale responses, timezone/permissions | BLOCKED | Compatible installed APK, V36 deployed, two workspaces |
| QA-AND-04 | P1 | Android | Media V37 typed API transport and authorization contract | NOT_VERIFIED | Source Retrofit transport exists; full native Media presentation/upload is NOT built; do not demand unavailable UI |
| QA-BE-01 | P0 | Backend+QA | Approved child reward -20 once, PURCHASED, repeat 409, ownership/cancellation guards, session retained after non-auth error | BLOCKED | Isolated or BETA pinned deployed V35 API, sanitized HTTP/ledger counts |
| QA-BE-02 | P0 | Backend+QA | V36 62/63-day calendar, timezone/DST/window eligibility & member permissions; V37 tenant-negative JWT/UUID, optimistic version, clear 404 and upload constraints | BLOCKED | Isolated deployed V36/V37 candidate with two synthetic families |
| QA-REL-01 | P0 | Release | Restore real backed-up **clone** before/after Flyway V35–V37, check preserved ledger+schema, repeat boot and rollback | BLOCKED | Explicit Release-approved disposable environment; synthetic Testcontainers green does not substitute BETA clone |
| QA-REL-02 | P0 | Release+Android | Signed APK incremental versionCode, checksum, in-place upgrade and on-device login/core smoke | BLOCKED | Release-approved signed candidate + real handset, no premature publication |
| QA-REL-03 | P0 | Release | BETA release acceptance, HTTPS health/auth, deployed SHA, cross-role E2E and rollback proof | BLOCKED | Release records exact deployed versions, approvals and all critical manual checks |

**Latest source handoffs checked:** `logs/web.md` RewardAvailabilityEditor full 0..56 weekly windows + Media EntityMedia/member/group/task + stale version/cover workspace transitions; `logs/android.md` Calendar V36, Retrofit V37 transport, XTR-A-05 P0 matrix; `logs/backend.md` V35/V36/V37 integration, PR #13 merged as previously independently verified; `logs/release.md` no confirmed BETA deployment. Source/automated integration must not be confused with deployed feature availability.

**Previously verified CI**: run 40117259515, exact integration candidate `c70d28d`, all four jobs green. Android journal also identifies run 40118508340 on `dbb4148` green; later Web Media/reward patches require their *own* pinned final SHA CI. Independent source i18n mandatory-key parity on 4 languages PASS (QA journal). No manual browser/handset PASSES yet.

**Next human testing wave (when matching build deployed):** WEB-SF-01 smoke and F01/F02/R01 retest; V36 RewardAvailability weekly windows + ONE_TIME; V37 Media A/B isolation; Android AUTH/REF; child -20 reward ledger. Manual cases may remain BLOCKED when BETA/APK stale. Report each failure once by track, deliver consolidated remediation package, then repeat failed + neighboring regression cases.
