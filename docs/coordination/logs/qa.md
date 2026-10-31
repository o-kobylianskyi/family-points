# FamilyPoints — Testing / QA journal

**Track:** QA · **Created:** 2026-10-24 · **Scope:** existing interim-BETA stabilization only

## Operating method
- At each user request "аналіз", "що тестувати", "далі", check `origin/main`, latest CI, open PRs #4/#5/#8 and logs of Backend/Web/Android/Release, and confirm whether actual deployed BETA server and APK versions are known. Update [live board](../QA_TEST_BOARD.md) with precise freshness and dependencies.
- Show the user no more than 3–5 top actionable tests at a time, with location, roles/accounts, steps and expected output; no deployment or unverified production data writes. Use synthetic test workspace/member/accounts rather than real family spending where practical.
- Use test statuses: `READY_TO_TEST`, `BLOCKED`, `NOT_VERIFIED`, `PASSED`, `FAILED`, `NEEDS_RETEST`. A manual test cannot become PASSED from CI or code inspection alone.
- When bug found, record reproducible issue here and route it to existing owner (B/W/A/R), with severity, exact BETA SHA/APK version, error HTTP/logs (secrets removed), screenshots and expected-vs-actual. Once owner fixes, require a new run on deployed fixed version; then close.

## Initial snapshot — 2026-10-24
- GitHub source inspected for QA setup. Most recent source/CI/workflow state is changeable; current actual VPS/APK not checked.
- Key P0: approved child reward request → purchase exactly once, 20-point debit, no unexpected logout. Backend current main has Flyway V35 reward purchase constraint fix plus a repeat-purchase conflict check; runtime deployment unverified.
- Key P0: login and transient Backend error Android session retention, offline sync without doubled points, multi-workspace isolation.
- Key P0: signed release APK on physical handset, DB V35 reward and candidate V36 calendar/V37 media migration, backup/restore, real BETA smoke; Web forms and Android pull refresh as regression.
- See QA_TEST_BOARD and QA_TEST_CASES. No test execution or pass claimed.


## 2026-10-24 — INCOMING Web package WEB-SF-01: QA assignment
- Web formally transferred the complete Soft Family stabilization visual regression suite to QA: `docs/coordination/QA_TEST_CASES.md` (WEB-SF-01), tracked on `docs/coordination/QA_TEST_BOARD.md`. Source handoff commit `d206fde`; board registration `a3e752e`.
- **QA action required:** check actual deployed Web SHA against tested source; then execute full test matrix light/dark × desktop/mobile × four locales, all existing main flows, group/task/reward dialogs, shared numeric fields, focus/scroll, and approved Home no-change. Record PASS/FAIL by case with screenshot/viewport/browser/theme/locale/exact SHA; no automatic PASS from build results.
- Provide one consolidated defect report to Web owner. Web should fix all returned defects in one batch and re-submit to QA; repeat until verified green. Report blockers where runtime or accounts are unavailable; do not claim tests were run without evidence. Scope frozen before interim release, no new feature inclusion.


## 2026-10-24 — WEB-SF-01 independent QA source audit + blockers
- Source of truth: `docs/coordination/QA_TEST_CASES.md` WEB-SF-01. Reviewed routes in `frontend/src/App.jsx`, `frontend/package.json`, `PointOperationModal.jsx`, `NumberInput.jsx`, `CreateTaskModal.jsx`, `TaskRewardNegotiationModal.jsx`, Web journal and board on GitHub main. Browser screenshots / light-dark × four-locale × viewport matrix NOT EXECUTED. Actual deployed BETA Web SHA NOT VERIFIED; test logins unavailable. Local repo clone and npm commands unavailable due DNS/network restriction in runner (git ls-remote failed). **All ten visual acceptance items BLOCKED, none PASSED.**
- Source finding WEB-SF-01-F01 (P1, Web): `frontend/src/components/TaskRewardNegotiationModal.jsx` renders literal Ukrainian `<span>Кількість</span>` for reputation amount. Reproduce by opening task reward request/review dialog with de/en/ru localization; label remains Ukrainian. Expected translation key in all four catalogs.
- Source finding WEB-SF-01-F02 (P2, Web): shared `frontend/src/components/NumberInput.jsx` stepper buttons have fixed English aria-label Increase/Decrease, rather than locale-specific accessible names. Reproduce with de/uk/ru screen reader or browser a11y tree. Expected localized and context-appropriate names.
- Risk WEB-SF-01-R01 (unconfirmed, Web/Backend): modal submit protections rely on React state `saving/submitting/loading`; stress-test near-simultaneous clicks + slow network, verify exactly one POST and one ledger mutation. Do not classify as reproduced defect without runtime.
- Code-only checks: seven primary authenticated routes confirmed by App.jsx; Escape/save guards exist in inspected task, point and negotiation modals. No executed browser observation, automated build, oxlint, i18n checker or deployed functional flow: those remain BLOCKED/NOT VERIFIED, not PASSED.
- Consolidated Web handoff: remediate F01+F02 in one Web package; investigate R01, verify theme/light-dark, 1440×900/1366×768/390×844/360×800, tablet/200% zoom, four locales, long DE and a11y/focus, all 10 matrix rows, screenshot evidence and exact deployed SHA. Approved Home appearance/assets must stay unchanged. Release status: NO-GO until real matrix evidence.


## 2026-10-24 — Web return: WEB-SF-01 F01/F02 remediation, NEEDS_RETEST
- Web reviewed this journal's independent audit and delivered consolidated source fix on `main`: F01 TaskRewardNegotiationModal reputation label uses the existing translated rewardNegUi.amount key; F02 shared NumberInput increases/decreases and control group use translated accessible names, with uk/de/en/ru locale entries (final source SHA of translation batch `d3e5078`; confirm latest main before test).
- **Change state:** F01 and F02 `NEEDS_RETEST` (NOT `PASSED`). QA verify all four languages in actual browser, DOM/accessibility names for arrows, and regression elsewhere using shared NumberInput. R01 remains NOT_VERIFIED and should be stress-tested with slow API/double clicks before release.
- Remaining 10-case light/dark/responsive WEB-SF-01 full matrix is still BLOCKED/NOT_VERIFIED absent confirmed deployed Web SHA/browser evidence. Do not close package on source inspection alone.


## 2026-10-24 — XTR-20261024-B-03-QA intake and independent CI verification
- Received Backend P0 consolidated integration V35→V36→V37, draft PR #13 head `a19b2adfa68a3e8a4fc625239289bc01f9ef7efa`; accepted test instructions in `docs/qa/QA_BACKEND_INTEGRATION_V35_V37.md` on `integration/backend-beta-v35-v37`. Candidate is **not deployed to BETA**; do not merge/deploy based on these results.
- Independently queried GitHub Actions run `40110260210` jobs: **Java 21/Maven/PostgreSQL Testcontainers SUCCESS**, **Web Vite production build SUCCESS** (locale completeness step SUCCESS), **BETA Docker Compose configuration SUCCESS**, **Android Gradle debug build FAILURE**. Results are CI evidence, not locally rerun tests.
- Independently inspected Android CI failure logs job `120920312767`: `PointsScreen.kt:79` unresolved `SettingsRepository`, line 99 unresolved `PointNameFormDto`, line 206 unresolved `pointNameForms`, and follow-on Kotlin type inference errors around lines 626/742–743. Confirmed Android integration build blocker; owner Android/Release, not evidence of Backend regression.
- Backend automated test suite is **CI PASS** on candidate (individual named test assertions not independently replayed); `QA_BACKEND_PASS` **NOT GRANTED** until isolated E2E, ownership/reward ledger and isolation/authorization checks with sanitized HTTP+ledger evidence. `QA_BETA_PASS` **NOT GRANTED** due failed Android CI, no signed device acceptance, no authorized populated BETA clone upgrade, and missing Web/Android consumer ACK.
- Manual test plan: 20-point approved reward single SPEND and repeat 409; pending/cancelled/cross-member/cross-family negatives; 62/63-day calendar and DST + manager permission; reward availability windows without debit; private media UUID/manifest/clear; stale expectedVersion/100-ref/MIME-size/cache tests; signed Android after Release approval. All **BLOCKED** awaiting isolated deployed candidate plus synthetic two-family identities; avoid destructive BETA writes.
- Follow-up: Android fix build in one pack; Backend provides isolated environment and final SHA; QA then collects actual E2E HTTP and ledger evidence. Overall gate **NO-GO**.


## 2026-10-24 — QA reprioritization after integration/Android/Web updates
- Independently checked PR #13: **MERGED** at head `c70d28d9812d13a85e2f6cfd62186fb344aff510`; prior draft/open snapshot is obsolete. Actions run `40117259515` four jobs all SUCCESS (Backend Maven/PostgreSQL, Android Gradle debug, Web/Vite, Compose), only for that pinned SHA. BETA deployed SHA/APK still unverified.
- Web F01/F02 source fixed and `NEEDS_RETEST`; R01 synchronous useRef locks added to three forms, requiring Network/ledger stress test. Browser visual suite WEB-SF-01 still NOT PASSED.
- Android latest log reports V36 calendar API integration on main without device verification; OFF-04 global-account queue identity remains BLOCKED pending immutable accountId/migration. Signed APK + real phone + BETA migrations not verified.
- Ordered manual execution: (1) WEB-SF-01 fast light/dark + locales + modal smoke at verified deployment, (2) Android AUTH-01/02 + REF-01 once matching APK, (3) OFF-01 test-family ledger exact-once if suitable installed version, (4) reward 20-point RW-01 only when Release confirms V35 BETA, (5) two-family workspace isolation. Never mutate non-test wallets. For failure log exact version, URL, screenshot, expected/actual and redacted network/logs.
- Overall Beta QA = NOT APPROVED pending P0 E2E and Release evidence. This is reprioritization/source+CI review, NOT new execution of manual scenarios.


## 2026-10-24 — Independent available automated QA verification
- Requested full automatic test sweep. Local runtime inspection: Node v22.16.0/npm v10.9.2/git available, **Maven, Gradle, Docker not available**, and `git ls-remote https://github.com/o-kobylianskyi/family-points.git HEAD` fails because github.com DNS cannot be resolved. Thus cannot clone source or locally run Vite build/oxlint, Maven/Testcontainers, Gradle, or browser E2E; these are **BLOCKED locally**, not passing or failing tests.
- **Executed independently using GitHub connector content**: fetched all four `frontend/src/i18n/locales/{uk,de,en,ru}.json`, parsed JSON and flattened keys, checked union parity with the same plural-key exceptions as repository `frontend/scripts/check-i18n.mjs`. Counts uk=935, de=933, en=933, ru=935; **zero missing mandatory keys per language**. **PASS** source translation key parity/JSON parsing (not translation visual correctness or full script execution).
- Requeried GitHub Actions run `40117259515` for pinned PR#13 candidate `c70d28d9812d13a85e2f6cfd62186fb344aff510`: Web Vite+i18n SUCCESS, Java Maven+PostgreSQL Testcontainers SUCCESS, Android Gradle debug/unit SUCCESS, Docker Compose SUCCESS. This is **verified CI historical evidence on pinned SHA**, not a newly run local suite and not a pass for later `main` commits.
- Latest repository commit found from commit search: `dbb41488fedbbcb650b10a64104533a8e2caae68` (documentation); no matching pull-request CI run or combined commit status returned for this specific SHA. Current main full test green **NOT VERIFIED**. Browser visual matrix, authenticated BETA reward/media/calendar APIs, signed APK handset, DB clone restore and privacy tests remain **BLOCKED**.
- No Release authorization or manual PASS; retest full suite on an exact current commit via CI/runner when available. Keep separate source-pass, historical-CI-pass and blocked environment statuses.


## 2026-10-24 evening — Intake reconciliation and unified test board
- Re-read latest Web, Android, Backend, Release and QA journals and reconciled source changes with prior tests. Updated `docs/coordination/QA_TEST_BOARD.md` authoritative evening snapshot (commit `4f5c709`) with 13 separately assigned acceptance streams including current Web V36 reward 56 weekly window editor, Media V37 cover/member/group/task bindings and delayed workspace switching, Android V36 calendar/V37 typed transport, XTR-A-05, Backend V35 ledger and V36/V37 isolation, Release clone migration and signed APK.
- Previous board entries referring to PR #13 as unmerged were marked superseded in the latest snapshot; exact BETA deployed SHA and APK still unknown. Do not claim these features deployed merely because main source contains them.
- No browser/physical-device/isolated deployed API run in this intake cycle. Status gates in board remain NEEDS_RETEST, NOT_VERIFIED or BLOCKED. QA readiness stays NO-GO. Next wave prioritizes WEB-SF-01, V36 reward scheduling, V37 media privacy, Android AUTH/REF and one-time reward debit only if appropriate deployment.


## 2026-10-24 late — Current available QA sweep
- Runner attempted local git clone access: `git ls-remote https://github.com/o-kobylianskyi/family-points.git HEAD` failed `Could not resolve host: github.com`; local Maven/Docker absent. Hence local full Web/build/lint, Java/Testcontainers, Android Gradle, browser and BETA API E2E **BLOCKED**; do not present historic CI as new execution.
- Executed GitHub-fetched source invariants: parsed all four current locale JSONs, mandatory-key union parity with plural exceptions **PASS** (uk=935, de=933, en=933, ru=935; missing=0 each). Code inspections **PASS for presence only**: no literal `>Кількість<` in TaskRewardNegotiationModal, synchronous `lock.current`-style guarding in TaskRewardNegotiationModal, PointOperationModal, CreateTaskModal; `revokeObjectURL` code in EntityMedia and FamilyCoverSettings. These are NOT end-to-end/browser behavior passes.
- Queried exact GitHub Actions jobs: run 40118508340 all four jobs SUCCESS; run 40119056029 all four SUCCESS; run 40119992292 Java/Web/Compose SUCCESS, Android CANCELLED; run 40120298412 Web SUCCESS, Java/Android CANCELLED, Compose completion not verified. **No final current main all-jobs green certified.**
- No newly reproduced runtime defects. High priority still WEB-SF-01 real browser 4 locales × light/dark + 360/390/1366/1440; RewardAvailabilityEditor V36 56-window/offset/409; Media V37 cross-workspace late response/private bytes; Android auth/offline/rewards; signed APK and DB clone gated by Release.


## 2026-10-25 — Incoming Android Media + account isolation QA review
- Re-read freshest Android XTR-A-05 handoff. **New cases accepted:** OFF-ACCOUNT-01..05 (accountId owner-verified SQLite v3 queue, cross-account A/B, quarantined ownerless legacy rows, mismatch /api/me, 401/403 partial replay); MED-AVATAR-WEB-ANDROID-01/02 (uploaded JPG/PNG in header/member cards, family switch); MED-AVATAR-CATALOG-01 (SVG catalog avatar through authenticated catalog preview); MED-AVATAR-RESUME-01/02 (header + member-card refresh on ON_RESUME after Web changes, no stale identity bleed). No physical APK/browser execution available, hence **BLOCKED**, not passed.
- Current Backend/media note: READ locking of media bindings and catalog sortOrder/tags introduced, unit tests added. GitHub run `40134239527` queried directly: Web SUCCESS, Maven/Postgres SUCCESS, Compose SUCCESS, **Android CANCELLED** — cannot claim global all-green on that run.
- Source-only checks: `RewardAvailabilityEditor.jsx` includes `isoInstant`, `FamilyCoverSettings.jsx` includes `revokeObjectURL`; `MemberMediaAvatar.kt` fetched. Presence of mechanisms is not behavioral acceptance.
- Regression should cover *uploaded* avatar and *catalog SVG* separately; profile/avatar atomicity and old legacy queue recovery are not silently implemented. Obtain exact signed/debug APK SHA and deployed V37 before device E2E.


## 2026-10-25 — User-requested full available test execution
- At latest retrieved repository commit `cf8ca5b241f3d60b1742c74d73447b4f95cf3655`, attempted local clone access: DNS failure `Could not resolve host: github.com`; Node/npm/git available, Maven/Gradle/Docker absent. Therefore **no local builds, Gradle instrumentation, Testcontainers, authenticated BETA API or browser tests executed**. Tests are BLOCKED, not passed.
- Independently executed GitHub-source assertions: parsed all four locale JSONs and mandatory-key parity with plural-exception rule: uk 935, de 933, en 933, ru 935, **0 missing per language PASS**. `frontend/src/api/mediaApi.js` includes explicit `cache:'no-store'` / binary handling; `RewardAvailabilityEditor.jsx` includes ISO validator; `MembersScreen.kt` contains **0 duplicate exact MediaRefDto imports**. These are presence/static PASS only and do NOT prove UI behavior, preview bytes, Kotlin compilation or reward-window correctness.
- Rechecked CI run `40134239527`: Web SUCCESS, Java/Maven/Postgres SUCCESS, Compose SUCCESS, Android CANCELLED. It is historic and not a fully passing latest-head CI. Cannot claim V38 Android gallery or latest Voxel image patch build passed. Android journal reports V38 gallery and possible Kotlin compile failure patched by removal of duplicate import; requires fresh compile diagnostics and CI validation.
- Incoming Web cache freshness: 60 corrected voxel PNG assets committed, Web media fetch no-store + Backend media controller no-store source change; actual deployed Backend serving correct bytes and on-device/browser visual correctness **BLOCKED** pending Release SHA and authorized media preview byte/hash checks.
- New release gate verdict: **NO-GO**, all human/E2E checks and exact current-head green CI remain outstanding. Consolidate any runtime bugs only when reproducible; no invented failures from missing tools.


## 2026-10-25 — QA continue: fresh CI + Web reward/catalog source
- Independently rechecked GitHub Actions run `40149287002`, baseline `213892eb`: **all 4 jobs SUCCESS** (Android Gradle debug/unit, Backend Maven/Testcontainers, Web Vite, Compose). Does not cover newer HEAD `42a27299f68f0e5c72c404dc036387c61a54aa40` or newest Web reward change `90aa3a6`.
- Re-ran static JSON mandatory-key parity on current GitHub files: uk 935, de 933, en 933, ru 935; missing mandatory keys 0 each, **source PASS**. Inspect `frontend/src/pages/RewardsPage.jsx`: synchronous per-reward `purchaseLocks.current` present, linked pending/request handler present (code-only presence). `EntityMedia.jsx` has Blob URL revoke cleanup. No runtime purchase/ledger verification performed.
- New handoff: Web reward mutually exclusive direct purchase vs linked READY_TO_PURCHASE request, verify only one correct CTA and one debit with child/adult + insufficient funds. Web catalog avatar staged large preview, Cancel does not commit, on workspace switch revoke preview; source fix `5e44946`.
- No confirmed current main exact-head CI / deployed BETA SHA / handset APK. No live API mutation or browser E2E run. Latest full-green historical run only; release still NO-GO.

## 2026-10-25 — Cross-track directive: EARLY BETA publication priority
**User decision (Architecture):** BETA is the place for testing. Prioritize rapid publication of the existing working feature set. Software implementation readiness includes Backend/Web/Android only; QA and Release are tracked as separate gates, not deducted from implementation percentage. Full manual E2E/regression, minor UX and catalog completeness, synthetic-data preservation rehearsal are NOT publication blockers. Keep minimum safety: green pinned CI, app/server starts, authenticated login/core route smoke, no known cross-workspace privacy leak, repeat points debit or cross-account offline replay, valid signed APK and basic install/launch, minimal DB snapshot before risky migrations. Existing test data may be reset eventually, but **no immediate deletion is authorized**. Templates/reset UI/history correction remain after this interim BETA. Decision record: `docs/coordination/logs/release.md` (2026-10-25); prior green SHA `77b183c98a6`, run `40177131970`.

**QA action:** BETA is the environment for ongoing testing, NOT something waiting for full manual regression PASS. Require only narrow pre-publish smoke for boot/login/core screens, cross-family privacy and duplicate point debits; after publication record defects, prioritize P0/P1 and test iteratively. No fake QA PASS.

## 2026-10-25 — Architecture task allocation: QA acceptance for planned backlog
**Shared checklist:** `docs/coordination/PRODUCT_BACKLOG_EXECUTION_CHECKLIST.md`. BKL-007 currency regression may be tested in live BETA: missing/disabled/hidden standard denominations and 1/2/5/11/21/22/25 forms across languages; preserve custom forms. Post-BETA new feature acceptance: tasks/rewards templates, selective reset with protected accounts, parent backdated corrections and ledger deltas, timer 5/2/1 once-only alerts, expiry and app launch/absent app/permissions, cross-account isolation. Require pinned deployments, no automatic pass; BETA is for testing and not gated by entire new backlog.

## 2026-10-25 — QA assignment: FamilyPoints DEV PostgreSQL MCP access via Contabo/WireGuard
**Owner:** QA (requested by user). **Status:** ASSIGNED / NOT IMPLEMENTED. Backend continues BKL-007 independently.
- Goal: enable ChatGPT regular Chat custom MCP integration to query **local Windows DEV PostgreSQL** read-only. Starlink has CGNAT/no public IP; existing Contabo VPS and WireGuard used for unrelated 1C connectivity. Prefer reusing existing protected infrastructure, NOT creating multiple tunnels; do not change or interrupt 1C/BAS/WireGuard existing routes.
- Start with READ-ONLY network/config inventory and ChatGPT custom MCP form (HTTPS URL and supported authentication). **Do not assume earlier mentioned 94.131.240.32 is the user's VPS IP**; user explicitly corrected that assumption. Discover actual host/address without publishing passwords or VPN private keys.
- Proposed topology: ChatGPT HTTPS custom MCP -> dedicated authorized Nginx route on Contabo -> existing WireGuard -> Windows DEV MCP service -> PostgreSQL localhost. Alternatively secure outbound relay if forwarding not compatible; no direct public PostgreSQL port 5432. MCP authentication and TLS mandatory, minimize HTTP exposure, audit and rate-limit queries. Use separate PostgreSQL least-privilege role with SELECT only, restrict schema and statements; no connection to BETA/Production. Protect secrets in local env files only.
- Deliver: verified HTTPS/MCP handshake with custom Chat connector, read-only SELECT 1, schema and point_types tests, redacted network/permission evidence, zero write access, installation/runbook and rollback steps. Clearly distinguish configured vs actually Chat-callable. Coordinate risky Nginx/VPN/firewall actions and user authorization first; **no deployment, network change, access expansion, or credentials requested in chat authorized by this note**.
- Backend BKL-007 owner proceeds with source changes/tests on DEV independently; QA owns MCP infrastructure investigation and eventual validation. Share readiness via journal and direct user handoff.


## 2026-10-25 — QA formally accepts DEV DB MCP connectivity work
- User explicitly requested QA accept Backend assignment. **Owner QA, state ACCEPTED / READ-ONLY DISCOVERY; connectivity NOT CONFIGURED or TESTED.**
- Scope: Windows local DEV PostgreSQL through authenticated HTTPS remote MCP over existing Contabo/WireGuard where technically compatible. Starlink CGNAT; no public PostgreSQL port, no additional unapproved tunnels, no 1C/BAS routing alteration. Never treat unverified 94.131.240.32 as actual VPS host.
- Checked ChatGPT available external SQL integrations: hosted Neon/Supabase plugins do **not** constitute a connection to user's self-hosted DEV DB. A dedicated custom MCP endpoint is still required. First inventory existing WireGuard peer routing, Contabo hostname/Nginx virtual hosts, Windows DEV PostgreSQL listen address and any MCP service in read-only mode; redact host secrets, connection URLs and keys from logs.
- Build security plan: dedicated PostgreSQL role with only required SELECT privileges, statement timeout, controlled exposed tools, authentication/TLS at MCP endpoint; no write tools, no operations on BETA/production. Initial checks (1) HTTPS handshake, (2) identity authorization, (3) SELECT 1, (4) read-only point_types and schema, (5) denial of writes and tenant-related limitations, (6) restart/rollback documentation. No infrastructure changes, role creation, tunnel or deployment performed here; obtain user/Release approval before privileged network/config mutations.
- Backend remains owner of BKL-007 application logic and unit tests; QA owns discovery, access design and subsequent safe verification.

## 2026-10-26 — QA handoff: BKL-007 currencies, DEV SQL MCP CONNECTED
**Status:** READY FOR QA REGRESSION, NOT QA PASS. **Owner:** QA.
- Earlier notes about MCP being unconfigured are superseded: Backend chat actually invoked connected `FamilyPoints_DEV_PostgreSQL` tools and verified `database_health`, `SELECT 1`, and read-only tables for local DEV `family_points` using role `fp_dev_mcp`. No SQL mutations.
- Current GitHub CI run 40243051918 at commit b2c923441b2adf093fb3072e12ae097e003522de: SUCCESS (backend/Maven+Testcontainers, web/Vite, Android/Gradle, compose).
- Read-only DEV baseline: family 1 POINTS/COPPER/SILVER/GOLD each have all 4 forms (16/16) after user opened economy Web settings; all active, SIMPLE mode. Existing customized uk/ru forms for coin denominations remain as generic `монета/монети/монет` (expected preservation). Families 11,12,13 currently have POINTS only, zero persisted forms, likely have not had economy API accessed since updated backend; inspect, do not silently mutate SQL.
- **Acceptance cases:** (1) logged-in manager creates custom `CRYSTALS` in Web; persists in matching family only; (2) set and re-read UK/RU/EN/DE forms, sample amounts 1,2,5,11,21,22,25; (3) edit name/sort order and deactivate/reactivate nondefault, no deletion of transactions; (4) default POINTS deactivation denied; (5) non-manager cannot POST/PUT or view list-all; (6) cross-family ID spoofing gets denied and cannot change foreign currency; (7) balances/ledger totals remain identical before and after currency settings changes; (8) existing family 11/12/13 gets four standard currencies and 16 forms after authorized economy API access without changing SIMPLE mode; (9) reload and repeat requests are idempotent; (10) manually customized forms preserved; (11) Web and Android consumption of active currencies verified. Avoid test data creation in existing family without explicit QA approval; use expendable DEV fixture family for mutations.
- **Operational caution:** MCP query tool is read-only, so use it for inspection, not fix-ups. If no authorized account context for family 11/12/13, report BLOCKED rather than claiming complete. No BETA/production changes or deploys authorized. Pin tested backend+frontend commit SHA and attach screenshots/errors to QA defect log.
- **Known design review:** currently SIMPLE remains unchanged even though four types are provisioned; ensure expected product UX for choosing other types is agreed with Architect. Current application API POST creates custom currency but does not auto-create custom name forms (must edit forms UI).


## 2026-10-26 — QA BKL-007 direct DEV MCP SQL baseline (no mutations)
- Via connected FamilyPoints_DEV_PostgreSQL query_sql verified role fp_dev_mcp and DEV database family_points on PostgreSQL 18.1. All queries SELECT-only; no SQL writes, BETA changes or API/browser acceptance.
- Four families observed: family 1 has active POINTS/COPPER/SILVER/GOLD; family 11/12/13 each only active POINTS. All economy_settings remain SIMPLE and their default point type is active, belongs to same family and is POINTS (4/4).
- Family 1 has 16 point_type_name_forms: 4 standard currencies x uk/ru/en/de; no duplicate (family_id,code) point types and no duplicate (family_id,code,language) forms across returned DEV rows. Existing uk/ru COPPER/SILVER/GOLD use generic coin forms (moneta equivalents), treat as user-customized forms to preserve, NOT automatically a defect.
- PENDING/NOT VERIFIED: family 11/12/13 API-triggered idempotent bootstrap; exact 1/2/5/11/21/22/25 runtime formatting; custom CRYSTALS create/edit/deactivate, role/tenant-negative HTTP, ledger immutability and Web/Android rendering. Do not create test rows in non-fixture families.
- Scope of PASS strictly DEV SQL invariants; overall BKL-007 not QA PASS.


## 2026-10-26 — BKL-007 manual Web + DEV MCP QA follow-up
- Family 1 custom point type CR (id 8, name Test crystal in Ukrainian) created through Web; Ukrainian form row id 17 persisted, then updated in place; deactivation followed by reactivation passed, final active=true. Existing POINTS/COPPER/SILVER/GOLD remain active, and no duplicate currency codes or per-language forms observed.
- Default POINTS deactivation attempted manually; Web displayed Cannot deactivate the default currency. Backend guard passed; verified default POINTS active for all four families in DEV DB. WEB LOCALIZATION DEFECT P2: raw English error on Ukrainian UI; localize error in uk/ru/en/de, ideally stable backend error code rather than matching English string.
- Read-only tenant-integrity SQL found zero point transaction links to other family currency, zero invalid family default currency links and zero within-family duplicate currency codes. This is database invariant PASS, NOT an HTTP cross-tenant authorization PASS.
- Family 11/12/13 still only POINTS without saved forms; API bootstrap remains unverified. Ledger baseline unchanged after custom currency lifecycle: family 1 POINTS 184 transactions signed sum 9662, SILVER 1 signed sum 5; family 12 POINTS 1 signed sum 111; family 13 POINTS 2 signed sum 0. Signed sums are not wallet balances and must not combine currencies.
- Pending: authenticated cross-tenant/role API negative cases, POINTS default guard in non-manager context, four-language format 1/2/5/11/21/22/25, Web/Android actual runtime, API seed idempotence. Do not mark entire BKL-007 QA PASS.


## 2026-10-26 — Independent QA discovery: starter catalog and DEV template-table visibility
- Directly parsed main `src/main/resources/templates/catalog-v1.json`: 142 distinct templates (110 TASK, 32 REWARD), 6 packs, all 142 titles/descriptions present in uk/ru/de/en, pack references valid; 65 templates belong to no starter pack (needs product intent confirmation, not assumed defect).
- DEV PostgreSQL Flyway history shows V39 and V40 success. `pg_catalog.pg_tables` and `to_regclass` confirm `public.workspace_private_templates` and `public.template_activation_requests` exist, but SELECT COUNT(*) via fp_dev_mcp fails on both, and list_tables omitted them. Suspected new-table grants/privilege visibility missing; raw PostgreSQL error not returned by MCP wrapper, so classify BLOCKED / investigate rather than definitively privilege denial. No data changes performed.
- Documentation handoff still says catalog-v1.json is empty, stale against current source; request update. Next independent work: inspect templates read privileges, source/CI contracts, negative tenant tests once authorized fixture available.

## 2026-10-26 — Coordinated sprint assignment (QA)
**Source of truth:** `docs/coordination/CURRENT_PRIORITY_SPRINT.md`. **P0:** log CI backend duration defect exactly 30 vs 0 (run 40389295693); after Backend fix rerun regression TASK/REWARD drafts, timed duration and import/activation, 142 catalog entries, six packs, permissions and repeated import. BKL-007: family1 4 currencies/16 forms verified DEV SQL, families11/12/13 lazy backfill pending, tenant negative and 1/2/5/11/21/22/25 formatting. **P1:** targeted Web+Android smoke tied to exact DEV SHA. Report specific reproducible bugs, no blanket QA gate for BETA.


## 2026-10-26 — Targeted TIME seed regression and V39/V40 permission diagnosis
- Parsed current catalog-v1.json: 32 REWARD seeds, exactly 8 rewardKind=TIME with positive integer durationMinutes (3 x 15, 5 x 30); no non-TIME reward carries a nonzero duration. SOURCE CATALOG PASS only; controller mapping/draft/activation runtime NOT_VERIFIED. Sprint CI previously observed 30 to 0 in WorkspaceTemplateControllerTest; Backend owns correction.
- Exact DB privilege probe for current_user fp_dev_mcp: has_table_privilege(...workspace_private_templates, SELECT)=false and has_table_privilege(...template_activation_requests, SELECT)=false. Tables exist; V39/V40 applied. CONFIRMED QA MCP read privilege blocker for new tables. Backend/DB owner should grant narrow SELECT on these two public tables to fp_dev_mcp if approved. No grants or mutations done by QA.


## 2026-10-26 — V39/V40 QA read access restored
- Verified via connected DEV MCP `database_health` user fp_dev_mcp on family_points PostgreSQL 18.1; `describe_table(workspace_private_templates)` succeeds.
- Live SELECT COUNT(*) confirms workspace_private_templates=0, template_activation_requests=0; no private template rows by workspace/kind. Source global catalog (142 suggestions) is separate from private library, so zero rows is a valid pre-import baseline, not proof of failure.
- User granted SELECT in pgAdmin; no QA data mutations. Follow up with authenticated DEV import using approved disposable test family and compare baseline/idempotency; HTTP runtime not yet tested.


## 2026-10-26 — QA catalog cross-pack audit (source only)
- Parsed current catalog-v1.json: 142 templates, no definition.source vs kind mismatches, no invalid code/version fields, no invalid TIME duration.
- 65 templates appear in no starter pack; 32 appear in multiple packs. Cross-pack overlap is an explicit QA regression risk: selecting overlapping packs must yield one private record per (workspace_id,catalog_code), not one per pack; verify through authenticated DEV import and API list when compatible build is available. Neither condition alone is classified a defect.
- All 32 reward definitions have priceAmount null; null means unset/not free. Do not auto-activate as free or infer prices. This is source-only PASS, not UI/API acceptance.


## 2026-10-26 — TPL-03A overlapping starter packs source audit
- Cross-compared all six packs from current catalog-v1.json. early_school vs mixed_age have exactly 32 shared of 32 entries each (identical membership); early_school vs older_children share 31/32 (union 33); preschool vs early_school share 24 (union 40). Six pack-pairs overlap; adult_personal and family_shared have no overlap in inspected combinations.
- Proposed DEV authenticated API test TPL-03A: selecting both early_school + mixed_age must import only 32 workspace-private catalog codes, repeat import adds 0, and ledger/reward-purchase/task-definition counts remain unchanged. These are expectations, NOT a performed HTTP PASS. Designer to confirm identical packs are intentional rather than catalogue curation oversight. No DB changes.

## Architecture dispatch 2026-10-26 (late)
Read `docs/coordination/DISPATCH_20261026_LATE.md` as the current cross-chat priority assignment. **QA owner:** P0: template TPL-01..15 plus TPL-03A overlap/replay/tenant, currencies per-family backfill; targeted evidence and no blanket BETA hold.


## 2026-10-26 — Late Architecture dispatch QA intake
- Received docs/coordination/DISPATCH_20261026_LATE.md. QA critical additions: TPL-03A shared starter-pack import dedup; test replaying same activation operation UUID against a *different* template ID must be rejected without creating an unrelated definition; null reward price must never be treated as free; explicit-only activation and family A/B access. Test only against authenticated disposable DEV workspaces, source/SQL invariant alone cannot certify REST security.
- Ran read-only DEV SQL grouping workspace_private_templates by workspace_id/catalog_code: zero duplicate groups. This is current database invariant PASS, not a successful import E2E. No DB mutations.
- Android P0 current CI/emulator secondary-list collapse assigned Android; current signed Huawei BETA is separate from DEV emulator. Designer numerical economy still not approved. BKL-007 older-family backfill pending.


## 2026-10-26 — TPL-03A / TPL-12 live DEV DDL invariant check
- Read-only DEV MCP checks show workspace_private_templates=0 and template_activation_requests=0 at observation; no API import was executed. Verified live pg_indexes: uq_private_template_catalog_origin UNIQUE (workspace_id,catalog_code) WHERE catalog_code IS NOT NULL; template_activation_requests_pkey UNIQUE (workspace_id,operation_id); index idx_template_activation_template(workspace_id,template_id). Schema matches V39/V40 sources.
- TPL-14 security caveat: template_activation_requests.template_id FK targets private_templates.id only; DB schema alone does not enforce that referenced private_templates.workspace_id equals activation_requests.workspace_id. Backend tenant guard must be proven by authenticated negative API/Testcontainers tests. This is a defense-in-depth gap/risk to review, NOT reproduced unauthorized data access.
- State remains PENDING for HTTP cross-tenant, replay, overlapping-pack import, no active tasks/rewards/ledger writes. No mutations.

## 2026-10-26 — User reprioritization: reset BEFORE next BETA, economy v0.7 IN DEVELOPMENT
Read `docs/coordination/REVISED_BETA_SCOPE_20261026.md`; it supersedes previous parked status of BKL-002. **QA:** P0 disposable-family end-to-end reset → setup → selective import → explicit activation; cross-family/auth/ledger/idempotency/4 currency checks. Planning/assignment only: no data deletion authorized.


## 2026-10-26 — TPL-03 repeat-import DEV SQL check
- After user repeated selection/import UI step, live DEV workspace 1 private library remains 3 TASK codes: fp.task.family.010, fp.task.family.020, fp.task.hygiene.001, each count 1. Duplicate catalog-code groups 0. template_activation_requests count 0. Financial/control totals unchanged: point_transactions 188, reward_purchases 47, reward_definitions 3.
- SQL baseline PASS (no duplicate/no finance changes). UI retry message and precise HTTP import response not independently captured; do not call complete HTTP TPL-03A (32-code overlapping packs) PASS. No QA write operations.


## 2026-10-26 — TPL-08 DEV readiness probe after user continuation
- Live read-only DEV workspace 1 still holds only 3 TASK private templates (fp.task.family.010, fp.task.family.020, fp.task.hygiene.001); expected fp.reward.time.002 absent. Activation requests remain 0; reward_definitions 3, reward_purchases 47, point_transactions 188 unchanged.
- TPL-08 TIME 30-minute creation draft not testable yet: no imported TIME source in workspace 1 and no Web/API draft response. State PENDING (not defect). Next manual action: select fp.reward.time.002 in catalog, import privately, open creation draft, confirm defaultDurationMinutes=30 with null price and no activation.

## 2026-10-26 — Current BETA architecture dispatch
Read `docs/coordination/CURRENT_BETA_DISPATCH_20261026.md`. **QA:** P0 end-to-end authorized disposable-family reset → wizard → one template import; tenant/ledger/currency/replay guards. Economy v0.7 preliminarily accepted for DEV; reset BKL-002 mandatory before next BETA, not an authorization to erase any family.


## 2026-10-26 — TPL-05 structural tenant-isolation probe
- Live read-only DEV SQL confirms 3 private TASK templates all in workspace 1; no other workspace private rows. TPL-08 fp.reward.time.002 still absent. template_activation_requests=0, point_transactions=188, reward_definitions=3.
- pg_constraint confirms workspace_private_templates.workspace_id FK -> families(id) ON DELETE CASCADE; activation_requests primary key (workspace_id,operation_id), separate FK (workspace_id) -> families(id), FK (template_id) -> private_templates(id). The latter is not a composite tenant+template FK, so DB constraints alone cannot certify matching tenants. Source/API negative cross-family test remains required; no breach reproduced.
- TPL-05 DB structural checks PASS, end-to-end authorization PENDING. No DB mutations.


## 2026-10-26 — New private REWARD import and TPL-05 user walkthrough
- User responded ++ to TPL-05 family switching walkthrough, with no reported visible cross-family leak; browser HTTP denial not independently observed, so security API test stays PENDING.
- New read-only DEV observation: workspace 1 now holds 3 TASK and 1 REWARD private template. New id 4 code fp.reward.activity.020, ITEM reward 'Кулінарний вечір разом', definition.priceAmount=null, durationMinutes=null. Activation request count 0, reward_definitions=3 and reward_purchases=47 unchanged. Private import did not auto-activate reward. No other workspace private templates.
- TPL-08 fp.reward.time.002 still not imported; cannot validate TIME 30-minute draft. No QA data mutation.

## 2026-10-27 — Architecture chat handoff assignment
Read `docs/coordination/ARCHITECTURE_CHAT_HANDOFF_20261027.md` and `docs/coordination/REVISED_BETA_SCOPE_20261026.md`. **QA:** P0 test bounded reset on authorized disposable DEV family (not existing user family), epoch replay, protected identities, select/import one/new/duplicate, ledgers/currency, TIME reward; verify CI first. Report exact SHA, tests and next blocker; no destructive reset authorized by this assignment.


## 2026-10-31 — Architecture QA checkpoint: reset/dataEpoch deployment gap
- Reviewed docs/coordination/ARCHITECTURE_CHAT_HANDOFF_20261027.md, REVISED_BETA_SCOPE_20261026.md and Backend/Web logs. P0 BKL-002 reset preview/execute + epoch atomicity, stale offline replay protection, and authorized disposable-family reset/onboarding smoke are next pre-BETA gates. Source logs report V42 migration and epoch GET/filter, but no safe execute yet.
- Live connected DEV read-only SQL: information_schema.columns shows no families.data_epoch column; flyway_schema_history includes successful V41, no V42 row. Therefore the connected DEV DB is not migrated to source V42; do not attempt epoch >0 tests or reset here until compatible deployment/migration verified by Backend/Release. DEV private templates=4, activation requests=0, point_transactions=198 (previous observed 188; +10 transactions occurred outside QA, cause unverified). Do not attribute ledger change to imports or declare a defect without attribution.
- QA status PENDING/BLOCKED for BKL-002 end-to-end and epoch regression; no DB writes, resets or deployment performed. Next owner Backend/Release: verify DEV target and exact SHA, execute approved migration safely, provide explicit disposable fixture and safe reset contract; QA then tests auth, tenant, replay, counters, currencies, catalog/TIME.


## 2026-10-31 — BKL-002 follow-up DEV verification
- Read-only DEV probe still shows NO families.data_epoch column (information_schema), private templates=4, activation requests=0. Flyway version query was safety-blocked; last successful probe had V41 and no V42. No claim of updated DEV schema.
- Backend journal now includes transaction-mandatory WorkspaceEpochTransactionGuard and SELECT data_epoch FOR UPDATE, but expressly NOT wired into all mutation transactions; BKL-002 reset challenge/execute contract published as proposal only (not implemented). This does not make reset safe or ready for E2E.
- QA remains BLOCKED for epoch>0 / destructive reset; Backend/Release to pin running DEV SHA and migrate schema, then implement atomic execute and lock write paths. No database mutations or reset by QA.


## 2026-10-31 — BKL-002 read-only preview/plan QA intake
- Current safe API contract is GET /workspaces/{workspaceId}/data-reset/preview, POST /plan; /prepare and /confirm are non-executable, and /confirm is acknowledgment, not reauthentication. /challenge and /execute proposed only. No reset authorization.
- P0 QA pending: scoped category counts/preserved identities/executable=false, empty plan, plan dependencies/blockers, cross-family and non-admin deny, A-B-A stale response, rapid selection race, four-language blockers. No authenticated browser/API evidence this pass.
- Live DEV read-only SQL: Flyway V42 absent and families.data_epoch absent; private templates count=4. Activation table query blocked. BKL-002 runtime/epoch E2E blocked until compatible deployment and transactional guard integration. No database mutations.


## 2026-10-31 — QA Automation Playwright bootstrap
- Added isolated qa/e2e/package.json, playwright.config.js, tests/smoke.spec.js and README.md on main. Public login test plus credential-gated templates/settings read-only smoke. Missing QA credentials skip protected tests; `/choose-family` intentionally fails closed so an existing family is never selected automatically.
- No credentials stored, no reset/import/delete/activate write flows, no live browser execution or CI run claimed. Next: create dedicated disposable QA account/family, install browser on Windows and run tests with environment-only secrets; then add source-verified selectors and read-only reset preview/plan. Protect browser traces/cookies from public artifacts.


## 2026-10-31 — User Windows Playwright authenticated smoke PASS
- User ran qa/e2e npm test locally against localhost:5173 with dedicated QA credentials: Chromium 3/3 PASS (public login 727ms; authenticated templates 1.8s; authenticated settings 1.6s; total 5.0s). Verified from pasted command output only, not CI or full feature acceptance.
- Next QA automation: introduce read-only reset preview and catalog data assertions with bounded selectors, then four-locale and tenant isolation; no destructive reset, no credentials in Git.


## 2026-10-31 — Playwright catalog/read-only reset checks
- Added qa/e2e/tests/catalog-readonly.spec.js at commit 923fded8: TPL-01 response asserts 142 templates / 6 packs / 110 TASK / 32 REWARD and unique codes; settings navigation asserts no reset challenge/confirm/execute POST. All read-only. Local Windows tests PENDING (prior original smoke 3/3 PASS only). No credentials or data mutations.


## 2026-10-31 — Windows QA Playwright 6/6 PASS
- User ran qa/e2e npm test with dedicated QA account and running local Web: 6 Chromium tests passed in 10.6s, zero failures/skips. Catalog TPL-01 API 142/6/110 TASK/32 REWARD; read-only settings no reset execute request; cross-tab epoch UI event guard; public login, private templates navigation, settings navigation. User-provided console output is evidence, not a pinned CI run.
- Scope limitation: cross-tab UI test is not real Backend epoch>0 reset/replay concurrency verification; read-only settings smoke is not reset preview/plan authorization or execution. BKL-002 full reset E2E remains blocked pending safe Backend and DEV V42 migration. No QA data mutations.


## 2026-10-31 — Playwright reset preview/plan and locale expansion
- Added qa/e2e/tests/reset-preview-i18n.spec.js commit 8347ac6: public login language switch uk/ru/de/en and authenticated read-only /data-reset/preview -> POST /plan with one selected category. Assert preview executionEnabled=false, plan executable=false, workspace same, blockers array, and no /prepare,/challenge,/confirm,/execute requests.
- Browser tests newly added, NOT YET RUN on user Windows. Requires QA account with MANAGE_WORKSPACE and active compatible DEV Backend; failing preview due to missing V42/backend is environment blocker to diagnose, not proof of reset safety failure. No deletion, only plan POST.


## 2026-10-31 — Windows Playwright 8-test reset preview failure
- User reported 7/8 PASS, with BKL-002 read-only preview/plan test timing out at 30 seconds while awaiting Playwright previewResponse.json() after receiving HTTP 200 headers. Other catalog, locale, epoch UI, login and settings tests PASS. This may mean slow/uncompleted response body, not necessarily invalid JSON; plan endpoint not reached.
- Updated qa/e2e/tests/reset-preview-i18n.spec.js commit 97f12e2 with a 60s budget and clearer HTTP URL/status/content-type body-read diagnostics. No claim this fixes backend latency; pending targeted rerun and trace. No reset executed or data modified.


## 2026-10-31 — Repeat BKL-002 preview response body failure workspace 14
- User reran headed isolated Playwright test: GET http://localhost:8080/workspaces/14/data-reset/preview response headers HTTP 200 application/json, but Playwright response.json did not resolve; after 34.2 s page/context/browser closed and response.json raised Target page/context/browser has been closed. Distinguish incomplete response/body from premature browser closure; no proven Backend defect yet. Reset /plan not reached; no delete action.
- Updated Playwright test at c52f40b to bound body wait to 8s, include visible reset panel text/response URL/status/content-type diagnostic, while preserving non-destructive safety. Next rerun isolated test; inspect Network response and Backend runtime if body remains pending. No DB mutations.


## 2026-10-31 — BKL-002 reset preview reproducible runtime defect (workspace 14)
- User reran headed Playwright after navigation wait fix: GET http://localhost:8080/workspaces/14/data-reset/preview yields HTTP 200 application/json headers but response body remains unreadable >8 seconds (BODY_NOT_READY). Web reset panel shows 'Could not load data.' rather than category counts. Test failed in 10.3 seconds; /plan not exercised, no destructive requests.
- Repro is independent of full page load; previous headed run hung until browser context closed. Backend/runtime owner should check response stream completion/JSON serialization of reset preview, dev running SHA and Flyway level, server logs; Web must remain fail-closed. Not yet proof that root cause is solely Backend until raw HTTP body inspected.
- QA keeps BKL-002 preview/plan FAIL/BLOCKED. No data mutations.


## 2026-10-31 — Manual Chrome Reset Preview succeeds; Playwright diagnosis corrected
- User supplied Chrome DevTools screenshot on localhost:5173/settings: GET /workspaces/14/data-reset/preview displays a parsed JSON object with workspaceId=14, executionEnabled=false, completeDependencyPlan=false, alwaysPreserved and categories. This disproves prior categorical claim that Backend cannot serialize the preview JSON. Repeated preview requests may occur.
- Revised qa/e2e/tests/reset-preview-i18n.spec.js at 2349698 to assert rendered category checkboxes first, then separately fetch/parse the authenticated preview using Playwright API request, avoiding first response-header capture. Pending rerun. Actual /plan and reset remain untested; no data deletion.


## 2026-10-31 — Root cause found: resetApi read() omits await
- User's latest Playwright: independently authenticated reset preview API parses JSON, but React shows 'Could not load data', browserErrors=[] and no category checkboxes. Source audit identified exact Web bug: frontend/src/api/resetApi.js `read(response)` received Promise<Response> from `apiRequest()` and immediately inspected `response.ok`, causing false RESET_API_ERROR. Fixed with `response = await response` in commit ef8d6446. Affects preview and plan readers.
- Fix committed but NOT YET runtime-tested. Next user action: pull main, let Vite hot reload, rerun isolated reset preview/plan Playwright. No reset or DB changes.
