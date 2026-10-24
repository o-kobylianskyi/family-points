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
