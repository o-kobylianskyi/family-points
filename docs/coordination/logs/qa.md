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


## 2026-10-31 — BKL-002 Preview/Plan Playwright regression PASS after Web fix
- User reran isolated `npx playwright test tests/reset-preview-i18n.spec.js -g "preview and dependency plan"` after frontend/src/api/resetApi.js missing-await fix ef8d6446: 1/1 Chromium PASS (2.1s test, 3.0s run). Verified runtime read-only preview category rendering, one category dependency plan, `executionEnabled=false`/`executable=false`, no prepare/challenge/confirm/execute.
- This closes browser regression caused by unresolved Promise<Response>. The full 8-test suite has NOT been rerun after fix, and BKL-002 destructive execute, epoch transaction safety, cross-tenant and onboarding are NOT covered by this PASS.


## 2026-10-31 — Complete Playwright smoke suite PASS
- User confirmed complete local Playwright suite: **8/8 PASS**, following Web reset API Promise awaiting fix.
- Coverage includes login/settings, catalog readonly constraints, synthetic epoch UI, four public-login locales, and non-destructive reset preview/dependency plan.
- Status: smoke green; BKL-002 destructive operations and real data epoch transaction tests remain unverified, and are not authorized on persistent QA families.
- Next QA plan: add negative, read-only tests for access control, invalid reset selections, stale response UI and cross-workspace isolation, and rerun full suite after adding each group.


## 2026-10-31 — Negative access-control tests staged (not yet run)
- Added qa/e2e/tests/reset-access-control.spec.js (commit 56e34c9): unauthenticated GET /workspaces/14/data-reset/preview must be 401, and logged-out navigation to /settings must deny access to reset panel. No credentials and no mutations.
- Baseline full smoke: 8/8 PASS as confirmed by user. New tests are **pending execution**; do not count them as passing until user runs them.


## 2026-10-31 — Full local Playwright suite 10/10 PASS
- User reported all ten Chromium tests PASS with QA credentials configured; no skips or failures in listed results.
- Includes 142-template catalog, reset safety, synthetic epoch notification, anonymous preview 401, logged-out settings guard, four locales, BKL-002 read-only preview/plan, login and authenticated templates/settings.
- BKL-002 destructive reset, transactional rollback and cross-family isolation remain OUT OF SCOPE and unverified. Proceed only with read-only/validation-negative checks on existing workspace.


## 2026-10-31 — BKL-002 negative read-only checks staged
- Added `qa/e2e/tests/reset-negative.spec.js` commit f989c6f: selection must be nonempty before Plan enabled, unchecking disables it and must dispatch no reset POST; preview JSON must mark execution disabled and each category non-executable.
- Two tests added; **not yet executed**. Previously 10/10 Chromium PASS remains the last confirmed suite. Expected next run 12 total, subject to local result.
- Reset /execute remains blocked; no database deletion or reset mutations were run.


## 2026-10-31 — Access-control regression extended to 14 tests
- User replied `13+`; no full numbered test output supplied, so exact executed pass count cannot be independently confirmed.
- Added 2 anonymous negative tests in `qa/e2e/tests/reset-access-control.spec.js` (commit 419dee2): POST reset/plan and GET workspace/data-epoch each require 401 without JWT. These must not make any authorized changes.
- Repository suite now defines 14 tests (prior 12 + 2), pending full local execution. Reset execute is still disabled.


## 2026-10-31 — Reload safety regression staged
- User reported `15+` in reply to prior QA run but without full console summary; treat as positive progress, exact all-pass count not independently verified.
- Added a new isolated QA Playwright test (commit 374b479) confirming reset Preview Reload preserves category count, clears selection (Plan remains disabled), and emits no prepare/confirm/challenge/execute requests. Test not yet run by user.
- Next: run `npm test` with QA credentials set. No destructive operations authorized.


## 2026-10-31 — Cross-workspace reset response rejection (synthetic, pending run)
- User reported `16+` without full test output; latest exact run summary not independently available.
- New Playwright negative test `mismatched workspace preview is rejected without rendering categories` (commit 502414c): intercepts GET preview, returns synthetically valid 200 JSON but workspaceId different from requested; asserts error panel, no category checkboxes, and no reset POST. No persistent database changes.
- This verifies UI fail-closed on mismatched response workspace only; it does NOT constitute server-side cross-tenant authorization proof. Pending local test.


## 2026-10-31 — Full Playwright suite 18/18 PASS
- User screenshot confirms Chromium `18 passed (27.1s)` with no failures/skips. Includes read-only unknown reset category rejection, cleared dependency-plan on selection change, mismatched workspace preview rejection, and all prior smoke/access tests.
- BKL-002 full destructive reset remains NOT APPROVED and untested; synthetic mismatched workspace preview does not demonstrate backend tenant isolation. Next priority is scoped cross-family authorization proof without deletion.


## 2026-10-31 — Tenant isolation QA fixture staged
- Full Playwright baseline 18/18 PASS per screenshot (27.1 seconds).
- Added `qa/e2e/tests/reset-tenant-isolation.spec.js` (commit abcf1c1), which checks GET /preview and POST /plan on a distinct, real **QA-only** workspace inaccessible to the QA user; expected 403 or 404. This test is skipped unless `FP_QA_FOREIGN_WORKSPACE_ID` explicitly supplies a second workspace ID. Never invent or probe other real families.
- No destructive endpoint is called. Passing would prove these two authorization checks only, not full tenant safety or reset execution.


## 2026-10-31 — Bidirectional Test/Test2 tenant security spec staged
- Updated `qa/e2e/tests/reset-tenant-isolation.spec.js` in commit 9530672: fixed JavaScript regex escaping and added two independent auth tests. Test (own workspace 14 by default) must receive own preview 200 and be rejected for verified foreign workspace; Test2 (child role) must be denied reset admin preview in its own QA workspace and must be denied preview and plan for workspace 14.
- Env: `FP_QA_FOREIGN_WORKSPACE_ID`, `FP_QA_TEST2_WORKSPACE_ID`, `FP_QA_TEST2_PASSWORD`; Test2 username defaults to Test2. Do not assign Test2 workspace 11 until verified; screenshot indicated request URL /workspaces/11 but was not complete proof. Tests skip when fixture unavailable.
- New tests NOT YET EXECUTED; last confirmed full Playwright run 18/18 PASS. Only GET preview and POST plan; no destructive operations.


## 2026-10-31 — Tenant-isolation 401 status discovered
- User ran both Test/Test2 cross-family tests: both failed assertion expecting 403/404 because foreign preview returned HTTP 401. Test primary own workspace reset preview returned 200 before foreign request, confirming working authentication on its own family. Test2 own reset preview assertion accepted denial (403/404), then foreign returned 401.
- Security interpretation: 401 is denial, not disclosure; however, its use for authenticated cross-workspace access differs from conventional 403/404 and requires Backend contract review. Updated Playwright acceptance to [401,403,404] and added status logs (commit 7744dca); no PASS claimed until rerun. POST /plan never reached in earlier failing runs.


## 2026-10-31 — Bidirectional Test/Test2 tenant isolation PASS
- User ran `npx playwright test tests/reset-tenant-isolation.spec.js` with both QA accounts configured: **2 passed (3.9s)**.
- Test own family 14 Preview 200; Test accessing Test2 family 11: Preview 401, Plan 401.
- Test2 child own family 11 Preview 403; Test2 accessing Test family 14: Preview 401, Plan 401.
- No reset execution or deletion. Confirmed endpoint-specific authorization, not comprehensive tenant isolation. Backend should review cross-tenant 401 versus 403/404 response semantics.
- Next QA: real dataEpoch/current-generation and stale-write fencing tests on disposable fixtures, only with explicit safeguards.


## 2026-10-31 — Real dataEpoch read-only tenant tests staged
- Added `qa/e2e/tests/epoch-access.spec.js` (commit 3dc7402), two Chromium tests: Test family 14 and Test2 family 11 must each get own authenticated `GET /data-epoch` 200, valid workspaceId and safe nonnegative integer dataEpoch, and denial (401/403/404) for other family.
- No mutations/epoch increments/offline replay. Pending local test run. Last confirmed cross-family Preview/Plan isolation 2/2 PASS. Future stale-write concurrency assertions require disposable fixtures and explicit gate.


## 2026-10-31 — dataEpoch real HTTP tenant tests 2/2 PASS
- User local `npx playwright test tests/epoch-access.spec.js`: 2 passed (4.7s). Test and Test2 each read own authenticated `dataEpoch` successfully (workspace IDs 14 and 11 respectively), validation of nonnegative safe integer passed; cross-family GET `data-epoch` returned HTTP 401 for both directions.
- This proves scoped epoch reading for the two QA identities, not stale-write fencing or atomic reset concurrency. Next stage: non-mutating UI synthetic stale-generation checks; any true stale-write mutation test needs disposable fixtures and deliberate approval.


## 2026-10-31 — Stale dataEpoch client write-block test staged
- Added Playwright synthetic cross-tab epoch test to `qa/e2e/tests/epoch.spec.js` (commit a01ed97). After authenticated login and current workspace observation, a second tab receives an epoch notice. The test invokes browser-side `apiRequest` with a POST plan payload but requires rejection `WORKSPACE_DATA_EPOCH_CHANGED` before any HTTP POST is sent.
- No backend mutation or reset occurs; test **not yet run locally**. This checks Web fencing, not backend transaction race safety.


## 2026-10-31 — Malformed epoch notification regression staged
- Added a third Playwright case in `qa/e2e/tests/epoch.spec.js` (commit 437880f): malformed JSON and nonnumeric workspace ID in cross-tab epoch notice must not create a stale-workspace alert. Browser-only; no Backend mutation.
- Previous newly added stale-notice POST-prevention case still awaits local test result; do not claim epoch.spec.js passes before rerun. Run full `npx playwright test tests/epoch.spec.js` with isolated QA account credentials.


## 2026-10-31 — Browser dataEpoch regression 3/3 PASS
- User ran `npx playwright test tests/epoch.spec.js`: **3 passed (11.7s)**. Cross-tab active-workspace notice PASS (3.2s); synthetic stale notice rejects scoped POST before network dispatch PASS (3.7s); malformed JSON/non-numeric workspace ID does not quarantine UI PASS (2.7s).
- The suite performs no database changes and does not demonstrate backend concurrent stale-write transaction fencing. Next: review Backend PostgreSQL integration coverage and devise disposable-data acceptance gate before any real mutation test.


## 2026-10-31 — Backend dataEpoch transaction audit (source/CI evidence)
- Reviewed guard implementation commit 9e328b5: `@Transactional(MANDATORY)` and `SELECT data_epoch FROM families WHERE id=? FOR UPDATE`, compares expected epoch, returns HTTP 409 on mismatch; missing row 404. Unit test commit db6f4f2 covers missing/legacy header, stale/current epoch, missing family, with mocked JDBC (NOT real concurrent row locks).
- Backend coordination log reports GitHub Actions run 41234461782 at exact SHA 4d88716a: four jobs success, 313 backend tests, 39 Spring/JPA/PostgreSQL scenarios; later source integration and deployment not implied. It reports actual family-fence waits/revalidation for invitation/revocation and reset-content snapshot cases, but not proof that every client write path is fenced or that atomic execute is available.
- QA gaps: evidence-based inventory of ALL workspace mutations guarded by transactional epoch; simultaneous transaction commit/rollback race specifically across reset and ordinary write; fault-injection at commit boundary and orphan/ledger integrity; cross-family lock isolation; Android queued stale operations; deployment parity.
- Keep reset execute disabled. Do not run destructive tests in Test (14) or Test2 (11). Request Backend to publish coverage matrix and disposable PostgreSQL concurrency fixtures before QA signs off.


## 2026-10-31 — Backend acceptance matrix reviewed against current CI
- Reconciled Backend's source write inventory (136 HTTP mutation mappings), disposable V45 retention/FK metadata (70 application tables/156 FKs), and green CI b4f62fc9 (329 tests, PointMutationTransactionPostgresTest 54 PASS, upgrade proof test 1 PASS).
- QA conclusion: representative PostgreSQL epoch/contention/proof correctness evidence exists; inventory is not a full end-to-end route or deployed executor proof. Deletion policy, owner/global-account/ledger retention, Google fresh reauth, atomic execute/epoch/onboarding, Android stale queue and release gating remain OPEN.
- Published `docs/coordination/requests/BKL002_QA_ACCEPTANCE_GAPS_20261031.md` at 6b6472c. No reset, schema mutation, or deployed changes.

## 2026-11-01 — Web QA operating policy (user directive)
- Browser-based/Web UI tests are *conditionally accepted for development flow*, not independently executed or PASS. Record each as `CONDITIONAL / QA_HANDOFF` with exact tests and remaining verification; do not block ordinary Web implementation solely for user-run Playwright. QA owns eventual browser execution and final acceptance.
- GitHub CI and repository-scope tests: Web owner must run/check them independently where available, examine actual GitHub Actions run/job status and reports; report evidence or explicit `NOT VERIFIED` if Actions dispatch/CI run is inaccessible. Never convert a missing workflow run into PASS. Current GitHub connector exposes workflow-run read/rerun, not dispatch; push commits trigger main CI workflow by configuration, but success must still be observed.
- Handoff: qa/e2e/tests/settings-runtime.spec.js and template-category-i18n.spec.js require authenticated isolated QA account; screenshots show Settings runtime ReferenceError was corrected and user confirmed Settings now works; no automatic reward creation/purchase/reset tests on real families. Full template selection parent/child QA remains conditional pending explicit execution.


## 2026-11-01 — Reset Preview/Plan failure-recovery regression staged
- Added `qa/e2e/tests/reset-failure-recovery.spec.js` (commits ed86622, 617c5c7): simulated HTTP 503 for Preview must display fail-closed error, no categories, and recover on manual Reload; synthetic HTTP 200 with wrong workspaceId plus executable=true Plan must be rejected with error, not accepted as a safe plan. Assert no prepare/confirm/challenge/execute requests.
- All interceptions are browser-local; no server data mutation. Two new cases **await local Playwright execution**; no PASS assumed. Existing confirmed browser dataEpoch 3/3 and server tenant/epoch 2/2 remain separate baselines.


## 2026-11-01 — BKL-002 Preview/Plan failure recovery 2/2 PASS
- User reran `npx playwright test tests/reset-failure-recovery.spec.js` with QA credentials: **2 passed (7.4s)**, no skips. Simulated HTTP 503 Preview failed closed and recovered through Reload (2.8s); malformed cross-workspace executable Plan was rejected and did not enable reset (3.7s).
- Tests use Playwright interception; no backend reset or data mutation. Next: stale/out-of-order dependency plan response regression.


## 2026-11-01 — Delayed reset Plan navigation-invalidation test staged
- Added third non-destructive Playwright case to `qa/e2e/tests/reset-failure-recovery.spec.js` (commits 151db34, e56a9ca, final title 7913d5b): hold browser-intercepted POST /plan, navigate to Templates to invalidate old Settings component, release response, reopen Settings and verify no stale plan is restored, no selected category and no prepare/confirm/challenge/execute calls.
- Earlier 2/2 reset recovery PASS is confirmed; new third delayed-response test is **NOT YET RUN**. It covers page-navigation invalidation, not in-place checkbox changes (checkboxes are disabled while busy).


## 2026-11-01 — QA architect/task check + delayed Preview test staged
- Reviewed architecture coordination and latest QA journal: architect's standing QA ownership covers multi-family ACL, safe reset/epoch, Web/Android parity, and consumer integration. No newer specific QA assignment was found in architecture journal. BKL-002 remains fail-closed; no execute authorization or data wipe.
- Earlier delayed Plan navigation test still awaits local run, not PASS. Added 4th browser test `late Preview cannot restore stale categories after leaving Settings` to `qa/e2e/tests/reset-failure-recovery.spec.js` (commit d30c3ea). Delays a synthetic Preview response, navigates away, then reloads Settings and verifies fresh categories/no stale selection or destructive endpoints. No Backend modification.
- New 3rd/4th cases remain PENDING local execution. Existing first two cases 2/2 PASS are unchanged. After these run, reconcile browser QA handoff, auth and CI status.


## 2026-11-01 — Reset recovery regression 4/4 FAIL; common panel precondition investigated
- User local full `reset-failure-recovery.spec.js` run: four failures (14.8, 17.2, 16.8, 18.2s). First could not see `.family-reset-readiness .error-message`; other three could not see first `.family-reset-category` checkbox. None reached intended assertions. Exact screenshot/trace only exist on user's workstation, not inspected here.
- Latest `SettingsPage.jsx` renders `<FamilyResetReadiness />` only when currentUser.permissions contains MANAGE_WORKSPACE or ADMIN_OVERRIDE. This is a plausible shared cause (or auth/navigation/runtime readiness issue), **not yet confirmed**. Previously first two tests passed, so status regressed in user's latest environment.
- Added explicit precondition to QA fixture after Settings navigation: wait for panel, then report pathname, heading and panel count plus required permissions if unavailable. Commits 82b3b2e, 7640883. No Backend/Web permissions changed; test results stay FAIL, not SKIPPED or PASS.
- Next: user run first test with --headed / updated precondition; use browser trace/error-context and actual roles to disambiguate hidden panel vs frontend crash vs stale auth. No destructive mutations.


## 2026-11-01 — Reset recovery 1/1 recheck FAIL; QA setup sequencing fixed
- User reran first case with `--headed`: panel precondition succeeded, but `.error-message` absent after 12s. Thus previous hypothesis that Reset section is absent due to permissions was not supported by this run.
- Found QA-fixture issue: `beforeEach` navigated to Settings and triggered Preview **before** each test registered Playwright route interception; test subsequently revisited Settings. Changed fixture to land on Templates; each case registers routes and only then enters Settings. First case also asserts interception was hit. GitHub commit a2378a5.
- New code NOT YET verified by local run. Keep 4/4 previous FAIL and first-case 1/1 recheck FAIL historical; no backend/security failures established from those assertions. Next: local rerun first case, then full 4-case suite.


## 2026-11-01 — Four Reset recovery tests still fail; deterministic Preview fixtures staged
- User reran full 4-case suite after route-order correction: **4 FAIL**, with first test interception observed yet `.error-message` never displayed, other cases lacking category checkboxes. Therefore route-order-only fix was insufficient; previous backend-vs-UI root cause remains unresolved.
- QA test update fc80c67: deterministic browser-local Preview 200 fixture for Plan tests; first failure scenario returns 503 until manual recovery, then mocked Preview 200; late Preview test also uses mocked fresh response. This removes live backend preview state/permissions from *most* test setup, but actual component rendering remains under test. No destructive endpoints called; no production Web/Backend patch.
- New code NOT YET EXECUTED; 4 FAIL remains latest observed outcome. If still failing, obtain `error-context.md` and screenshot/trace to identify actual rendered DOM and network response rather than guessing.


## 2026-11-01 — Playwright error-context inspected; browser-state diagnostic added
- User supplied `error-context.md` from failing Reset Recovery first case. File contains the failed `.error-message` assertion and source excerpt, not a DOM/accessibility snapshot, screenshot, trace events or response body. It cannot establish whether panel was mounted or why the component had no error. No definitive root cause claimed.
- Instrumented `qa/e2e/tests/reset-failure-recovery.spec.js` (commit 9e19e4d): log Settings pathname, headings, reset panel count/text, checkbox count, alerts, token presence; log Preview status/URL and page errors. No secret/token value is printed and no Backend writes. Run focused first test to diagnose; four failures remain latest verified result.


## 2026-11-01 — Root diagnostic: Settings React crash INVALID_WORKSPACE_ID
- User ran focused test: `[RESET_QA_PAGEERROR] INVALID_WORKSPACE_ID` twice, intercepted `GET /workspaces/14/data-reset/preview` 503 twice, and DOM diagnostic `{pathname:'/settings', headings:[],panelCount:0,checkboxes:0,alerts:[],hasAccessToken:true}`. The 503 mock was delivered but React page tree is blank due to uncaught JavaScript error; asserting `.error-message` is downstream symptom, not demonstrated Reset API bug.
- Source `frontend/src/api/setupApi.js` contains a synchronous `INVALID_WORKSPACE_ID` guard, but **the stack trace was not provided**, so origin/caller is NOT conclusively known; do not claim setupApi is the crashing call without trace. The Reset panel permission hypothesis is superseded.
- QA source cleanup: remove automatic `/templates` page visit in `beforeEach` (69d447f), and use `/points` as neutral navigation path in delayed-response cases (cfbf740). Both fixes need local rerun and might not resolve Settings crash; if `INVALID_WORKSPACE_ID` remains, Web owner must obtain stack and fix render-time path. No Backend writes, no Reset execute.


## 2026-11-01 — Reset Preview recovery focused rerun PASS
- User ran `npx playwright test tests/reset-failure-recovery.spec.js --grep "preview failure"`: **1 passed (2.8s overall)**, case duration 1.9s. Mock Preview yielded 503 twice; diagnostic showed Settings mounted with panelCount=1, no categories, localized error visible, token present; Reload yielded 200 and recovered. No INVALID_WORKSPACE_ID page errors in this run.
- Removal of extraneous pre-test Templates navigation is associated with restored test behavior. This confirms one case only; remaining three cases in the four-case suite are pending rerun, not PASS. No backend writes or reset execution.


## 2026-11-01 — Reset Preview/Plan recovery full suite 4/4 PASS
- User reran focused Preview failure case: **1 passed (2.8s overall)**, repeat acceptance of 503 fail-closed and manual Reload recovery.
- User then ran `npx playwright test tests/reset-failure-recovery.spec.js`: **4 passed (10.9s)**. Preview 503/Reload 2.2s; invalid Plan response 2.1s; delayed Plan after page navigation 3.3s; delayed Preview after page navigation 2.6s.
- Browser intercepted Preview 503 and 200; earlier INVALID_WORKSPACE_ID crash absent in shown latest runs. Test 2 diagnostic snapshot was taken before React rendered (panelCount 0 at that instant) but final assertion passed; that snapshot alone is not a regression.
- All four are non-destructive browser tests. This does NOT prove deployed Reset Execute, PostgreSQL atomic deletion, or user-account authorization universally. Next QA: full Playwright suite and isolate any failures with exact test/trace evidence.


## 2026-11-01 — Full browser regression 28 PASS / 1 FAIL / 3 SKIPPED, Web filter fix staged
- User ran `npx playwright test`: **32 total, 28 passed, one failed, three skipped (1.1m)**. Reset recovery all four PASS; dataEpoch browser and anonymous reset safety passed. Test2 epoch-access and both tenant-isolation tests SKIPPED in this PowerShell run because optional Test2/foreign workspace variables were missing (previous isolated runs had passed with variables configured).
- Only FAIL: `template-category-i18n.spec.js` expects category selector to retain `chores` after switching locale but observed `ALL`. Source `TemplatesPage.jsx` catalog initialization effect resets `categoryFilter('ALL')` and depends on unstable `getAccessToken` function identity; second media effect also has redundant function dependency. Fixed both effects to depend only on workspaceId; token is read fresh when effects start. Commit 61fc5f2. No backend or data writes.
- Browser fix pending local test+CI acceptance; do NOT claim new test PASS. Run `npx playwright test tests/template-category-i18n.spec.js`, then full suite with both QA credentials and foreign IDs set. Need check Actions job on exact commit separately.


## 2026-11-01 — Full Playwright recheck: 29 PASS / 0 FAIL / 3 SKIPPED
- User first reran `npx playwright test tests/template-category-i18n.spec.js`: **1 PASS (3.5s)**; selected `chores` persists across UA/RU/DE/EN after Web effect dependency fix `61fc5f2`.
- User then ran full `npx playwright test`: **32 tests, 29 PASS, 0 FAIL, 3 SKIPPED (50.3s)**. All Reset Preview/Plan recovery, epoch browser, negative auth, catalog, smoke and Settings checks passed.
- The three skips are Test2 epoch GET and two cross-tenant reset isolation cases; the full-run environment did not satisfy their `FP_QA_TEST2_PASSWORD` / `FP_QA_TEST2_WORKSPACE_ID` / `FP_QA_FOREIGN_WORKSPACE_ID` conditions. Prior isolated tenant tests passed earlier, but this run is NOT 32/32 and is NOT fresh bidirectional coverage.
- No destructive Reset executed; source/browser acceptance only. Next: configure second account credentials in same PowerShell session (never expose passwords), rerun three tests and then full suite for 32/32 if no failures. CI and deployed SHA are separate gates.


## 2026-11-01 — Test2 tenant isolation and dataEpoch: 4/4 PASS
- User ran `npx playwright test tests/epoch-access.spec.js tests/reset-tenant-isolation.spec.js` with two QA accounts: **4 passed (9.4s)**, zero skipped in this targeted run.
- Test own epoch access PASS and Test2 own epoch access PASS; each attempted foreign workspace epoch returned HTTP 401.
- Test reset Preview/Plan into Test2 workspace: both HTTP 401 (PASS). Test2 child attempting own-family reset Preview: HTTP 403; foreign-family Preview/Plan: HTTP 401 (PASS).
- Combined historical coverage: prior full suite 29 PASS / 3 SKIPPED / 0 FAIL; these three previously skipped cases plus one repeated epoch test now PASS in a separate focused run. Thus all 32 test definitions have recent passing runs, but a single full-suite 32/32 run has NOT been shown. Keep production Reset Execute, Android device/offline and CI on exact SHA as separate unverified gates.


## 2026-11-01 — FULL WEB E2E QA GATE GREEN: 32/32 PASS
- User executed full `cd qa/e2e && npx playwright test` with Test and Test2 QA accounts configured: **32 passed (59.0s), 0 failed, 0 skipped**, one Chromium worker.
- Coverage: catalog 142 suggestions and six packs; optional economy hints; Test/Test2 scoped epoch read and foreign 401; browser epoch quarantine and prevention of stale POST; unauthorized reset preview/plan/epoch rejection; four deterministic non-destructive Reset failure/recovery cases; reset negative plan checks and locale previews; bidirectional tenant isolation (Test foreign Preview/Plan 401; Test2 own admin Preview 403 and foreign Preview/Plan 401); settings/version smoke; template category localization persists across UK/RU/DE/EN.
- CONFIRMED milestone: current Web Playwright E2E suite is green and previous three skipped cases are resolved in same run. Does NOT authorize executing reset: BKL-002 destructive/atomic reset, retention backup/restore policy and device Android offline/dataEpoch require independent acceptance, as do CI on exact SHA and deployment validation.
- Next QA track: Android offline/dataEpoch synthetic regression, BKL-002 backend execute readiness and retention restore contract, cross-client epoch consistency. No destructive QA workspace actions performed.


## 2026-11-01 — Next synthetic QA tranche opened after 32/32 Web E2E PASS
- New Playwright file `qa/e2e/tests/epoch-all-verbs.spec.js` (17c2b86) extends stale cross-tab epoch denial from POST-only to **POST, PUT, PATCH, DELETE** against a single read-only reset plan route. Tests are browser-only: upon epoch notice, all verbs must fail with WORKSPACE_DATA_EPOCH_CHANGED **before any HTTP mutation is sent**. First code commit, NOT YET RUN; do not add to confirmed 32/32 baseline without a new full run.
- Android/Backend acceptance handoff `docs/coordination/requests/QA_ANDROID_OFFLINE_EPOCH_RECEIPT_GATE_20261101.md` (3711e8e): 12 acceptance scenarios across offline epoch changes, family switch, network fail-closed, roles, V48 receipts/UUID, HELD terminal journal, nullable TIME data, reset safety; includes exact-head Gradle commands and ownership, separate BKL-002 blockers.
- Reviewed Android journal: latest Android synthetic receipt/epoch tests are SOURCE ONLY, exact-head Gradle/CI, device and DEV behavior still unverified; Backend BKL-002 execute423 and retention/owner policy still BLOCKED. No Android QA PASS or Reset Execute PASS claimed; no destructive operation performed.
- Next: local `npx playwright test tests/epoch-all-verbs.spec.js` and full suite (expected 33 test definitions), Android owner synthetic Gradle evidence, Backend gate delta review.


## 2026-11-01 — QA 33++ user checkpoint + Architecture reset/backup additions reviewed
- User sent `33++` following addition of one Playwright `epoch-all-verbs.spec.js` case on top of the previously verified 32/32 suite. Interpret as user-reported new total 33 checks PASS; no raw Playwright log or exact SHA was attached, so record **USER-REPORTED 33/33, CONSOLE EVIDENCE PENDING**, distinct from independently inspected prior 32/32 log. No Android PASS implied.
- Checked recent Architecture commit 77d8d2d4 `BKL002_APPROVED_FILE_BACKUP_RESTORE_20261101.md`. Product decision: **NO external/cloud/off-host backup requirement**; instead manager-controlled encrypted portable export and full/selective file import/restore; local temporary server backup with 30-day TTL; user-downloaded copies cannot be remotely deleted. Full reset deletes allowed family-local content but preserves owner/global auth/workspace ID/immutable audit/other families; selective defaults delete tasks/rewards/economy/groups/templates/media but retain non-owner participants/settings/currencies. All require dependency and finance safety gates.
- Backend implementation handoff 9b8f2462 `BKL002_PORTABLE_BACKUP_IMPLEMENTATION_20261101.md` proposes endpoints and recovery flow; proposals are NOT public executable API or deployment approval. Existing crypto codec and related tests cover primitives, not entire exporter/restore. BKL-002 execute remains BLOCKED until validated recoverable backup, FK policy, rollback, idempotent receipt, epoch, owner/Google-auth, key portability and isolated DB tests are verified.
- Created P0 QA acceptance `docs/coordination/requests/QA_BKL002_PORTABLE_BACKUP_GATE_20261101.md` (f999446): 18 specific disposable PG scenarios covering portable roundtrip, FULL/SELECTIVE, tamper/wrong key, cross-family, quota/schema, 30-day file+registry TTL, transaction fault/rollback, balances/entitlements, owner and Android epoch. No destructive tests on Test(14)/Test2(11) or BETA/PROD. Next Android exact-head Gradle and Backend disposable-fixture proof; confirm raw 33/33 console output if needed.


## 2026-11-01 — BKL-002 V50 registry synthetic QA cases staged
- Inspected newest Backend source: `ProtectiveBackupImportInspector` added for offline encrypted archive inspection; `ProtectiveBackupArtifactCodec` roundtrip/tamper tests exist, but no verified complete export→file import→restore→reset path demonstrated. Web `FamilyDataSafetyPlan` now displays manager-only `GET /workspaces/{id}/protective-backups?limit=25` metadata, and distinguishes REQUESTED from VERIFIED+restoreVerified and EXPIRED. Backend only promises metadata at this gate; actual file export/download/restore APIs are not present as verified public contract.
- Added `qa/e2e/tests/backup-registry-readonly.spec.js` (d9d2185): 2 browser-synthetic tests checking REQUESTED/VERIFIED/EXPIRED rendering, foreign workspace row exclusion, refresh, no file upload and no dangerous mutations; HTTP503 shows fail-closed error state. This uses Playwright route mocks, does NOT create a backup or restore. **NOT YET EXECUTED**, no PASS counted. With prior 33 definitions, the proposed next full Playwright suite has 35 tests.
- Next operator run: `npx playwright test tests/backup-registry-readonly.spec.js`, then full suite. P0 remains disposable PostgreSQL integration/manifest/key/retention and Architecture final restore conflict policy; reset execute remains blocked.


## 2026-11-01 — Web QA full regression user-reported 35/35 PASS
- User reported `2 passed` for `qa/e2e/tests/backup-registry-readonly.spec.js` and `35 passed` for complete `npx playwright test` suite. Record **35 PASS, 0 FAIL by user report**; detailed console output/exact run SHA not supplied. Previous detailed baseline 32/32 remained independently visible in chat.
- The two new browser scenarios cover V50 backup registry REQUESTED/VERIFIED/EXPIRED synthetic rendering, tenant-scoped list filtering, refresh, HTTP 503 fail-closed and absence of file/destructive actions. This does NOT validate an actual encrypted file backup, full/selective restore, 30-day server cleanup or BKL-002 executor.
- Web E2E acceptance complete at current user-reported 35-case suite. Next P0 QA: real disposable PostgreSQL full/selective backup export/import and isolation acceptance after Backend publishes a functioning, versioned executor; Android synthetic epoch/receipt and exact-SHA CI evidence remain independent.


## 2026-11-01 — Architect follow-up + Backend seven-category portable export QA
- Prior Web E2E 35/35 PASS is user-reported. Newest Backend commits include `WorkspacePortableBackupExporter` seven-category tenant-scoped encrypted read-only export and PostgreSQL test `WorkspacePortableBackupExporterPostgresTest` (0d9baca); prior fixes ensure family row lock before epoch validation and source digest recheck. **Source tests exist, but current exact-HEAD CI PASS and user-held file/import/restore HTTP roundtrip have not been verified**. Do not conflate exporter with full restore or Reset Execute.
- Architecture approved requirements remain in `BKL002_APPROVED_FILE_BACKUP_RESTORE_20261101.md`: portable encrypted file, server-local 30 days, FULL/SELECTIVE and fail-closed tenant/FK/financial semantics. No new completed executor authorization or restore conflict/key policy signoff found in Architecture log; original owner/finance and rollout gates stay open.
- Web BKL002-04 now adds tenant-fenced GET backup detail and changes list semantics to reject entire mixed-family response instead of silently dropping rows. QA created `qa/e2e/tests/backup-detail-readonly.spec.js` (43a89b7): 2 synthetic tests covering legitimate metadata GET, cross-family detail fail-closed, foreign list fail-closed, no file upload/unsafe writes. **UNRUN**, do not count as PASS. Next full browser suite expected 37 (prior 35+2) if all test files intact.
- Next QA: focused browser tests; inspect Backend current portable exporter PostgreSQL test+CI on pinned SHA, add independent negative coverage for archived FK associations and stale epoch only on disposable PostgreSQL. No reset/restore on QA Test14/Test2.


## 2026-11-01 — 37-case Web regression: 36 PASS / 1 FAIL; tenant-strict fixture corrected
- User supplied detailed Playwright console: 37 tests, **36 passed, 1 failed (1.2m)**, none skipped reported. Both new `backup-detail-readonly.spec.js` checks passed; `epoch-all-verbs.spec.js` passed.
- Failed positive registry case `backup-registry-readonly.spec.js` expected visible snapshot `11111111` but saw none. The fixture mixed three current-family backups with one foreign-family record; newer `backupApi.js` intentionally rejects the *entire* malformed cross-tenant list (fail closed), superseding older silent filtering. The security behavior is correct; positive fixture was obsolete.
- Fixed positive fixture to supply only same-family records, preserving REQUESTED/VERIFIED/EXPIRED assertions. Cross-family poisoned-list rejection stays covered by separate `backup-detail-readonly.spec.js` negative case; no relaxation of application tenant checks. Commit f438c9a. **Fix not yet rerun, 36/37 remains latest observed complete-suite result.**
- Next: run focused registry spec, then full 37-case Playwright. Destructive reset/export/restore remain separately gated and never exercised on Test/Test2.


## 2026-11-01 — Full Web Playwright suite 37/37 PASS (user report)
- User reported **37 passed** after pulling `f438c9a` (correct positive registry fixture). Thus the former 36 PASS / 1 FAIL is superseded by user-confirmed **37 PASS, 0 FAIL** for the 37-case suite; full raw console/exact local SHA not supplied in this message.
- Cross-tenant fail-closed rejection remains covered by `backup-detail-readonly.spec.js`; positive registry covers valid same-family REQUESTED/VERIFIED/EXPIRED.
- This is Web E2E only, NOT disposable PostgreSQL backup-export/restore acceptance nor approval for BKL-002 Reset Execute. Next QA P0: backend encrypted exporter FK-bound seven-category fixture, source digest/epoch, foreign family and tamper, and exact-head CI; full/selective restore remains unverified.


## 2026-11-01 — PostgreSQL restore preview negative coverage added (source only)
- Inspected fresh Backend commits: read-only `WorkspacePortableRestorePreviewService` (d539041f) with PostgreSQL integration test (98ef86a3); tests for source archive tenant mismatch, target stale epoch, existing group conflicts and no mutation. New Android receipt guard deals with 404/timeouts as UNCERTAIN, not evidence of device/runtime acceptance.
- Added second PostgreSQL regression `rejectsMissingSelectionAbsentCategoryAndUnsupportedMappingWithoutWrites` to `WorkspacePortableRestorePreviewPostgresTest` (ea8c418, assertion cleanup 811dcce): empty selection 400; missing category 400; POINT_LEDGER archive selection returns executable=false and `RESTORE_CATEGORY_MAPPING_REQUIRED` while epoch and both families' group data remain unchanged. Test only manipulates disposable Testcontainers PostgreSQL, not persistent QA workspaces.
- **Not executed in this tool session**: source commit is NOT a CI PASS. Run Maven test on exact HEAD with Docker/PostgreSQL Testcontainers and retain failing traces. Restore preview is non-executable until approved FK/remap/financial logic and actual import, reset and transaction/rollback tests pass. Web Playwright 37/37 PASS remains last user-confirmed browser baseline.


## 2026-11-01 — PostgreSQL restore preview integration 2/2 PASS (operator local)
- User executed targeted Maven `WorkspacePortableRestorePreviewPostgresTest`: **Tests run 2, Failures 0, Errors 0, Skipped 0; BUILD SUCCESS**. This verifies two disposable-PostgreSQL preview scenarios including tenant/epoch/conflict rejection and invalid/unsupported selection fail-closed with no data changes.
- Exact checked-out local SHA and XML artifact not provided, so this is user-reported local acceptance, not proof CI on current origin/main.
- This does NOT approve portable full/selective restore execute, actual export-download-import roundtrip, rollback or BKL-002 destructive reset. Next independent tests: wrong-key/tamper, stale archive selection, duplicate ID and bounded payload behavior, PostgreSQL foreign workspace preservation.


## 2026-11-01 — Portable restore encrypted archive negative PostgreSQL suite staged
- Following user-confirmed 2/2 PostgreSQL restore preview PASS, QA added `WorkspacePortableRestoreArchiveSecurityPostgresTest` (0765b38): one real disposable PostgreSQL Testcontainers scenario covering independently generated wrong AES key, ciphertext last-byte tamper, trailing envelope bytes, invalid FPB magic, foreign-family archive binding, plus unchanged both families' group names and dataEpoch.
- An attempted append to shared `WorkspacePortableRestorePreviewPostgresTest` hit GitHub 409 because another track concurrently changed its blob. Avoided overwriting and created a separate test class instead. No persistent QA/production DB writes.
- New test SOURCE ONLY, **NOT YET EXECUTED**. Run Maven with Docker: `./mvnw.cmd -Dtest=WorkspacePortableRestoreArchiveSecurityPostgresTest test`; preserve console/XML result and exact SHA. Restore execution remains blocked.


## 2026-11-01 — Maven compile blocker, concurrent Backend fix verified
- User ran `mvnw.cmd -Dtest=WorkspacePortableRestoreArchiveSecurityPostgresTest test`; Maven stopped at javac in `ProtectiveBackupImportInspector.java:122`, shadowed local `byte[] bytes` conflicts with method `inspect(byte[] bytes,...)`. Test did not run, no PostgreSQL security PASS.
- Re-read current GitHub main file: offending local now `mediaBytes`, both length validation and zero-fill use new name. Concurrent Backend commit `9b0efac1a` explicitly fixes this compile issue. No duplicate QA code edits needed.
- Operator action: `git pull --ff-only origin main` then rerun targeted Maven command. If other compile errors emerge, preserve output; do not claim success before actual test result.


## 2026-11-01 — Archive Security Maven rerun BUILD SUCCESS (user-reported)
- User reports **BUILD SUCCESS** after pulling concurrent Backend compile fix `9b0efac1a` and rerunning targeted `WorkspacePortableRestoreArchiveSecurityPostgresTest`. Unlike prior attempt, Maven now completes. Exact Surefire tests-run/failure/skip counters and SHA were not supplied; log as targeted build success, do not invent a count.
- Target suite source checks wrong AES key, ciphertext tamper, trailing bytes, invalid portable magic, foreign-workspace archive denial and unchanged disposable A/B rows and dataEpoch. Last detailed Web baseline 37/37 and Restore Preview 2/2 unchanged.
- Next QA: confirm Surefire count if needed, review newest exporter/inspector/preview changes and test schema/duplicate-ID/FK-negative behavior on disposable PostgreSQL only. BKL-002 execute remains blocked.


## 2026-11-01 — Architecture dispatch 21:34 CET reviewed, QA critical path updated
- Read `docs/coordination/DISPATCH_20261101_2134.md` (commit 56faf1a87). Architecture/coordination instructs QA **prioritize end-to-end recoverable .fpbackup PostgreSQL roundtrip** instead of indefinite isolated negatives. Evidence must identify exact SHA and CI; prior local Web 37/37, PG restore-preview 2/2 and archive security BUILD SUCCESS are **not proof of main HEAD**.
- Required integration gate once backend supports it: source family export -> decrypt -> staging -> reconstruct FULL/SELECTIVE -> verify FK graph and IDs, media bytes/checksums, ledger evidence -> compare original target, ensure foreign family/global auth untouched, transactional rollback on failure, durable UUID receipt and epoch fencing.
- Restore execute and Reset Execute **not approved**. Architecture still owes ADR signoff for selective replace-vs-merge, economic evidence, group/media remap, shared-account/owner preservation, key/passphrase recovery, and TTL semantics. Backend owes actual executor, no tests may simulate success by only replaying rows into a staging fixture.
- New QA strategy: do not add redundant synthetic negatives when acceptance is blocked by missing executor; track Backend implementation/ADR and prepare explicit disposable-PostgreSQL acceptance conditions. Retain Web 37/37 and earlier QA reports as user-reported only. No destructive Test14/Test2/Beta/Prod actions.


## 2026-11-01 — P0 roundtrip progress audit at 80dc44fe7
- Latest Backend commit `80dc44fe7` adds `archivedGroupAndMediaBytesRoundtripThroughSqlWithRollbackAndTenantFence` to `WorkspacePortableRelationalRehearsalPostgresTest`: real encrypted export of MEMBER_GROUPS + MEDIA_ASSETS + MEDIA_BINDINGS, decryption, hash/byte comparison, FK-constrained **temporary staging tables**, rollback, and neighbor-family preservation. Prior companion `stagesFamilyGroupsAndDetectsBrokenReferencesWithFullRollback` stages group links. This is a useful **partial relational rehearsal, not a successful FULL/SELECTIVE restore into live domain tables**: it deliberately calls `status.setRollbackOnly()` and makes no Restore Execute call.
- Gap against Architect dispatch `DISPATCH_20261101_2134.md`: no demonstrated archive -> actual family domain reconstruction/commit -> compare for all approved categories; ledger economic correctness, member/group/task FK remap, UUID durable receipt, full/selective merge policy, protective snapshot and expiry remain gate items. Do NOT declare BKL-002 unblocked.
- Next QA run on disposable Docker/Testcontainers: `mvnw.cmd -Dtest=WorkspacePortableRelationalRehearsalPostgresTest test` and record exact SHA and test counters. Once Backend provides actual restore executor, build complete FULL/SELECTIVE roundtrip acceptance instead of multiplying only preview unit negatives.
- Current evidence: Web 37/37 PASS and restore preview PostgreSQL 2/2 PASS by operator; archive security targeted BUILD SUCCESS by operator; new relational rehearsal is source reviewed but **not yet run/verified in QA**.


## 2026-11-01 — BKL-002 MAXIMUM consolidated QA P0 acceptance packet
- Read newest Designer BKL002 UX v1 and gap audit, and Backend relational rehearsal enhancements `82e9854626`, `5c3bd7a091` (invalid FK must abort), plus Web FULL reset retention correction `0b0d0d1858`. These are source commits, not QA PASS or current-head CI.
- Published **one comprehensive acceptance contract** `docs/coordination/requests/QA_BKL002_END_TO_END_ACCEPTANCE_GATE_20261101.md` (`88289e0db`): 30 integrated disposable PostgreSQL executor security/financial/FK/rollback/tenant/idempotency/TTL/reauth tests (E01–E30), 12 Web/API UX/identity/read-only checks (W01–W12), canonical dual-family fixtures, exact-SHA evidence requirements, explicit STOP/BLOCKED rules, owner deliverables and safe aggregate Maven command. This is deliberate large-batch QA instead of repeated small test additions.
- Current read-only 37/37 user PASS + Restore Preview 2/2 local PASS + Archive Security BUILD SUCCESS retained as **earlier local evidence only**. New FK staging test and latest Web design changes not yet verified in QA; new entire acceptance gate intentionally **not yet executable** until actual Backend FULL/SELECTIVE restore engine and Architecture decisions exist.
- P0 remains **HOLD**; do not trigger reset, deletion or restore in Test 14, Test2 11, Beta, Prod or any persistent DB. Next QA receives pinned Backend executor and consumes entire matrix in one coherent run.


## 2026-11-01 — Consolidated disposable PostgreSQL Maven QA BUILD SUCCESS (operator report)
- Operator replied **BUILD SUCCESS** to requested aggregate command covering `WorkspacePortableRestoreArchiveSecurityPostgresTest`, `WorkspacePortableRestorePreviewPostgresTest`, `WorkspacePortableRelationalRehearsalPostgresTest` and `WorkspacePortableBackupExporterPostgresTest` after pulling main. Count of executed cases, skips and exact local HEAD were not provided; **do not invent 4/4 or test-run totals**.
- This supports compile and execution of the existing isolated PostgreSQL exporter/inspector/preview/staging/rollback test package, **not** committed actual FULL/SELECTIVE family restore in domain tables or destructive Reset Execute. Follow integrated P0 E01-E30 / W01-W12 acceptance contract; do not run destructive tests on persistent QA families.


## 2026-11-01 — Consolidated QA for actual rollback-only rehearsal service (P0 partial)
- Inspected latest Backend `WorkspaceDisposableRestoreRehearsal` (23cf5df4): it decrypts archive, stages **every available category as JSONB** and compares JSON equality/counts, but relationally reconstructs only MEMBER_GROUPS + MEDIA_ASSETS + MEDIA_BINDINGS in TEMP tables. Explicit `status.setRollbackOnly()` prevents permanent changes. It **is not a real FULL/SELECTIVE restore executor** and gives no VERIFIED certification; later apply, ledger, owned category FK graphs still missing.
- Added `WorkspaceDisposableRestoreRehearsalPostgresTest` (`a486a019`), using actual exported encrypted archive + group/media blob + binding, invoking **the real reusable service**, verifying returned counts, checked variant, neighbor-family rejection, missing-slice fail-closed and unchanged source/neighbor rows/blob/epoch after rollback. Test uses Docker Testcontainers only. SOURCE ADDED, **NOT RUN**.
- Web has newly-added `family-data-navigation.spec.js` 2 definitions and onboarding fixes; prior Web 37/37 is historical and does not cover these new changes. Designer identified FULL mode retention/multi-panel issues; source fix `0b0d0d1858` not yet E2E verified.
- Architect dispatch still P0: **full domain restore and verified protection**; large QA matrix documented in `QA_BKL002_END_TO_END_ACCEPTANCE_GATE_20261101.md`. Next single run of latest isolated PG aggregate + targeted reusable-service test, then fresh Web suite, record exact SHA and actual Surefire counters. Keep BKL-002 Reset Execute BLOCKED.


## 2026-11-01 — New integrated rehearsal test compile failure and correction
- User ran aggregate 5-class Maven command. Maven compiled 325 main Java sources successfully but **testCompile failed** at `WorkspaceDisposableRestoreRehearsalPostgresTest.java:22`: `org.testcontainers.postgresql.PostgreSQLContainer` in this project is **non-generic**; new QA test erroneously used `PostgreSQLContainer<?>` and `new PostgreSQLContainer<>()`. None of the five tests ran in that attempt.
- Fixed the QA-only declaration to raw `PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine")` in commit `79857568`. No production code modification, no current PASS claim. Request same aggregate targeted Maven re-run after pull; retain exact SHA and Surefire counters.


## 2026-11-01 — Five-class disposable PostgreSQL QA aggregate BUILD SUCCESS (operator report)
- User confirmed **BUILD SUCCESS** after the `PostgreSQLContainer` non-generic testCompile fix `79857568`, in response to rerunning the five-class aggregate `WorkspacePortableRestoreArchiveSecurityPostgresTest,WorkspacePortableRestorePreviewPostgresTest,WorkspacePortableRelationalRehearsalPostgresTest,WorkspacePortableBackupExporterPostgresTest,WorkspaceDisposableRestoreRehearsalPostgresTest`.
- Targeted Maven goal completed successfully; exact Surefire test counts, skipped tests, local git SHA and CI URL not supplied. Record as user-reported local success, not current-head CI proof.
- This accepts available isolated encrypted exporter/import inspection, read-only preview, group/media JSONB/FK staging, rollback rehearsal safety coverage. It does NOT demonstrate persisted FULL/SELECTIVE domain restore, verified protective pre-reset snapshot, durable receipt, physical TTL or authorized Reset Execute. BKL-002 remains HOLD under architecture integration matrix E01-E30.


## 2026-11-01 evening — ACK latest Architecture dispatch for QA (Issue #15 + Issue #18)
- Read `docs/coordination/DISPATCH_20261101_EVENING_RESTORE_ONBOARDING.md` (dispatch commit 8b88453b): **BKL-002 AND Issue18 P0 before next beta**, actual portable FULL/SELECTIVE restore apply and reset safety, plus coherent onboarding family->group->catalog selection->import->explicit activation. QA to use disposable PostgreSQL only, no Test/Test2/Beta/production reset, and document exact pinned SHA with all four CI jobs.
- Reviewed current source progression: Backend native group/catalog media restoration is still described as rollback-only rehearsal in dispatch; do not certify final restore. Latest observed native roundtrip test commit `4be78dd2e9` is NEW source and needs actual PG result; subsequent commits invalidate claims about exact HEAD until pinned CI passes.
- QA acceptance strategy adjusted to **two comprehensive P0 gates**: (1) E01-E30 BKL002 actual executor, archive corruption blocked reset and no cross-tenant mutation; (2) Issue18 real disposable onboarding family+group+catalog+import+explicit activation, user-visible reconciliation/idempotency and permission checks. Android device testing deferred by user. Exact SHA, Surefire/Playwright output and 4-job CI completion required for release.
- Current user-confirmed evidence: earlier Web 37/37 PASS; 5-class disposable PostgreSQL aggregate BUILD SUCCESS. Neither certifies newer HEAD or real restore apply. Issue15/18 remain OPEN. QA ACK; blockers are Backend production restore/HTTP file/reset, Architecture restore policy ADR and stable green CI.


## 2026-11-01 — Combined P0 integration pickup: native committed slice + Issue18 read-only browser regression
- Rechecked latest GitHub before QA work: Backend commit `4be78dd2e` adds `WorkspacePortableNativeCommitRoundtripPostgresTest`, which **commits** remapped MEMBER_GROUPS + catalog MEDIA_BINDINGS in a separate disposable PostgreSQL family and checks source/neighbor untouched and conflicting second transaction rollback. This is meaningful *test-only* native INSERT/COMMIT evidence, **not a production Restore Execute service** and not full categories. No local/CI PASS for this newest class yet.
- Reviewed source `FamilySetupPage.jsx` (new commit sequence 96c3cb3/50ce30f/d6de3a6): fresh import preview required before POST, ambiguous POST reconciliation, account/workspace-scoped drafts, explicit group creation and catalog fail-closed; Issue18 persistent wizard completion/receipt contract still missing. New read-only `qa/e2e/tests/onboarding-nondestructive-gates.spec.js` (`e3ed032e5`) contains **3 new browser tests**: page/step/reload sends no writes; malformed catalog denies import; group GET 503 cannot create group while catalog navigation remains accessible. This uses existing QA manager, performs NO write actions. **NOT EXECUTED**.
- Combined next execution packet (Docker + running local frontend/backend): `mvnw.cmd \"-Dtest=WorkspacePortableNativeCommitRoundtripPostgresTest,WorkspacePortableNativeApplyPostgresTest,WorkspaceDisposableRestoreRehearsalPostgresTest,WorkspacePortableRestoreArchiveSecurityPostgresTest,WorkspacePortableRestorePreviewPostgresTest,WorkspacePortableRelationalRehearsalPostgresTest,WorkspacePortableBackupExporterPostgresTest\" test`; `cd qa/e2e && npx playwright test tests/onboarding-nondestructive-gates.spec.js tests/family-data-navigation.spec.js`; then full browser suite if focused green. Capture `git rev-parse HEAD`, Surefire run/failure/skip counts, screenshots/trace and complete CI jobs on same candidate commit.
- **Release/Reset HOLD:** test-only disposable native commit is not approved full/selective restore; remaining gaps include all-category FK and ledger/media bytes, authenticated production file API, durable receipt + idempotency, pre-reset restore-tested backup, owner/auth policy/reauth/TTL and architecture signoff. No persistent Test/Test2/Beta/production writes.


## 2026-11-01 — Focused Web P0 onboarding + family data: 3 PASS, 2 SKIPPED, 0 FAIL
- Operator supplied raw Playwright result `npx playwright test tests/onboarding-nondestructive-gates.spec.js tests/family-data-navigation.spec.js`: **5 total: 3 passed, 2 skipped, 0 failed, 21.5s**.
- All three Issue18 onboarding non-destructive tests PASS: no automatic import/activation on navigation/reload, invalid catalog fail closed, member-group GET HTTP503 does not authorize group writes and template navigation remains available.
- Both `family-data-navigation.spec.js` tests are SKIPPED (not accepted as passed): manager one-protected-entry/3 modes; child denies safety UI. Source includes runtime skip for choose-family or manager panel not mounted; child also skips absent FP_QA_CHILD_USERNAME/FP_QA_CHILD_PASSWORD. Raw console does not give exact branch. Need `npx playwright test tests/family-data-navigation.spec.js --reporter=list` and inspect runtime skip with appropriate QA identities or modify test to make manager skip an assertion once chosen-family manager session is assured. Do not invent credentials or use destructive operations.
- This new 5-test result does not replace historical full Web suite 37/37 at older commit, and does not certify current HEAD or release. BKL002 actual full/selective restore still HOLD.


## 2026-11-01 — 42-case Web full regression 26 PASS / 14 FAIL / 2 SKIPPED, consolidated QA repair
- User attached full Playwright console: **42 total, 26 passed, 14 failed, 2 skipped, 5.9m**. Three new Issue18 onboarding tests passed; direct cross-family reset authorization remained correctly 401/403. This supersedes 37/37 old-SHA browser baseline for current run.
- Root cause #1: Web BKL002 UI moved `FamilyResetReadiness` under the second **Reset** tab; initial Settings tab is **Backup**. Legacy `reset-*`, `epoch-all-verbs` specs assumed Reset preview fetched and `.family-reset-readiness` mounted during `page.goto('/settings')`, thus widespread timeouts. Added shared `family-data-qa-helpers.js` for explicit safe navigation and updated reset-negative, reset-failure-recovery, reset-invalid-plan, reset-preview-i18n and epoch-all-verbs tests. The requested preview is now awaited after navigating to Reset; no unsafe reset calls introduced.
- Root cause #2: Backend/Web strict protective backup metadata now requires `selectedCategories`, `restoreVerified`, `executable`, valid `resetMode`, `createdAt`, `expiresAt`, UUID. Old synthetic fixture lacked fields, so correctly rejected; added `backupRow` helper and repaired backup-detail/backup-registry positive/negative fixtures. Verified label assertion matches current `Registry status: verified`; restore test explicitly opens third tab. Tenant poison/fail-closed assertions retained.
- Root cause #3: `family-data-navigation.spec.js` manager test used immediate `count()===0` to skip while Settings lazily loads; now awaits visible panel. Child test remains legitimately SKIPPED when isolated child credentials unavailable; do not fake child access acceptance.
- Commits: shared helper eaff7200; 5 reset specs d28a6ff/cf02b0d/ef2a96c/7a3ec21/4bc982d; metadata fixes 3e45f25/e7bb429/4132340; lazy panel skip fix 82c8523. **All changes SOURCE ONLY / NOT YET RE-EXECUTED.** Request targeted browser batch then full run, pinned SHA. Production Reset/Restore still BLOCKED.


## 2026-11-01 — Web full regression 40 PASS / 1 FAIL / 1 SKIPPED, strict locator corrected
- Operator supplied exact 42-case Playwright output: **40 PASS, 1 FAIL, 1 SKIPPED, 1.8m**. The 13 previous tab/fixture failures are resolved; backup detail, epoch quarantine, all reset recovery/negative/ACL, onboarding tests and manager three-panel navigation pass. Child-UI navigation test still skipped for unavailable child-specific credentials; API child ACL tests did pass.
- Only remaining FAIL is Playwright strict locator in `backup-registry-readonly.spec.js` matching four `Metadata only` paragraphs rather than unique registry intro. Corrected to an exact semantic regex for the single REQUESTED/registry introductory paragraph across UA/RU/DE/EN in `5d6e152`; no app/backend security change.
- **Fix SOURCE ONLY / not yet rerun**; next run targeted backup-registry and then whole Playwright suite to capture final counters and pinned SHA. BKL002 actual full/selective Restore Execute remains blocked despite successful read-only regression.


## 2026-11-01 — Full Web E2E 41 PASS / 1 SKIPPED / 0 FAIL (operator confirmed)
- Operator ran `npx playwright test tests/backup-registry-readonly.spec.js`: **2/2 PASS**, after unique registry intro locator correction `5d6e152`.
- Then operator ran `npx playwright test`: **42 discovered: 41 PASS, 1 SKIPPED, 0 FAIL, 3.3m**. Manager's three read-only data safety panels, backup registry/detail isolation, reset failure recovery, epoch quarantine (all write verbs), access control, template catalog, 3 Issue18 non-destructive onboarding tests all PASS. Only child browser navigation test SKIPPED; direct Test2 child reset API ACL 403 PASS.
- This **accepts Web E2E test suite on user's local checkout at run time**, not current moving HEAD CI or actual authorized Restore/Reset. Local git SHA not provided. New Web Task/Reward POST reconciliation commits appeared afterward; rerun current SHA for release gate. No persistent family reset/restore performed.
- QA next P0 focus: Issue18 end-to-end import+explicit activation on throwaway family and Backend BKL002 actual FULL/SELECTIVE production executor; stable green 4-job CI before release. Hold destructive execution and beta deployment.


## 2026-11-01 — Next maximum integrated QA package: Issue18 ambiguous POST + BKL002 native restore
- Rechecked latest production source: `CreateTaskModal.jsx` committed changes `4abe976`, `6ead045`, `9ee058d` distinguish confirmed POST vs failed refresh, quarantine ambiguous create responses, disable Save; `RewardsPage.jsx` `eee23eb` prevents blind repeat reward POST. These changes postdate operator full Playwright 41 PASS / 1 SKIPPED; **not yet executed or CI-certified**. They do not prove durable server receipts.
- Read Backend `WorkspacePortableMediaBindingPreflightPostgresTest` and newest Backend journal: preflight rejects invalid encrypted media binding identities and verifies tenant/epoch; Backend reports four-class targeted Maven BUILD SUCCESS; QA has not independently run latest same SHA. Production FULL/SELECTIVE apply still BLOCKED.
- Published one coherent P0 dual-workstream acceptance matrix `docs/qa/QA_P0_ACTIVATION_AND_RESTORE_INTEGRATION_20261101.md` (`c29cd9f`): **18** activation/onboarding fault-injection cases (task/reward lost POST, duplicate submission, stale identity, preview import replay, group creation, child ACL, non-destructive safety); **12** portable domain restore/security cases (FULL/SELECTIVE/ledger/FK/media/tenant/epoch/receipt/TTL/protective backup). Critical: synthetic route mocks, no writes to persistent QA workspaces, distinguish source review from passing tests.
- Current acceptance: operator local browser 41 PASS/1 SKIPPED and targeted PostgreSQL earlier BUILD SUCCESS, but latest task/reward create code has no QA execution evidence. Next execute safe same-SHA regression and focused isolated Postgres; release HOLD until all four CI jobs and actual executor/Architecture policy.


## 2026-11-01 — Issue18 Task/Reward uncertain POST source audit + first no-write browser regression
- Inspected actual create paths `CreateTaskModal.handleSubmit` and `RewardsPage.createCatalogReward`. Task modal sets `unknownWrite=true` before POST, clears only on success, disables Save; `onCreated` refresh failure does not change confirmed POST. Reward page sets `rewardWriteUnknown=true` before POST, clears only on success. Both are client-side in-memory fences, **not durable server receipts**; reload, manual form cancellation, account switch and server idempotency still require separate end-to-end evidence.
- Added non-destructive two-case Playwright test `qa/e2e/tests/create-flows-no-implicit-post.spec.js` (`f667a26`, selector hardening `5f740fef`): task `/tasks?create=1` and reward `/rewards` navigation/reload must never dispatch scoped TaskDefinition or Reward POST. Requires QA login but no writes, no synthetic successful create or destructive operation. SOURCE ONLY, **NOT RUN**. Do not claim lost-response A01/A07 complete from these smoke cases.
- Rechecked Backend commits `ac932f929` / `faaf755b8`: polymorphic media target refs checked against archive content/scope in preflight; current test source still requires exact SHA Maven and full domain Restore Execute. Next test packet should exercise same-SHA Playwright, fixed isolated Testcontainers media preflight and actual restore executor when published.


## 2026-11-01 — 44-case Playwright run invalidated by credential-gated skips
- Operator reported `npx playwright test`: **44 discovered, 6 PASS, 38 SKIPPED, 0 FAIL (25.3s)**. The 6 passing are unauthenticated/public access/login tests, while 38 credential-dependent tests—including two newest Task/Reward navigation cases—were skipped. This is **not** a 44-case green regression and does not invalidate earlier locally confirmed 41 PASS/1 SKIP on a different environment/session.
- Most likely root cause: missing `FP_QA_USERNAME` or `FP_QA_EMAIL`, or missing `FP_QA_PASSWORD` in current PowerShell process. The child browser test additionally needs `FP_QA_CHILD_USERNAME` and `FP_QA_CHILD_PASSWORD`; don't put actual passwords in log or chat. Verify presence as Boolean only; restore credentials via user's existing secure QA process, never invent them.
- Run `npx playwright test` again after environment is restored and pin `git rev-parse HEAD`; do not record either new Task/Reward smoke test as PASS yet. BKL002 real restore and beta remain HOLD.


## 2026-11-02 — Architecture ADR-FAMILY-RESET-002 accepted + Reward History P1 QA dispatch
- Reviewed approved `docs/architecture/ADR-FAMILY-RESET-002.md`: owner binding, shared global identity, FULL unsupported fail-closed, ZERO_BALANCE_KEEP_HISTORY vs dependency-closed CLEAR_ECONOMIC_HISTORY, SELECTIVE REPLACE_SELECTED only (MERGE unsupported), fresh 5-min proof exact family name/Google OIDC, Argon2id/passphrase file strategy, verified recoverable protective backup, 30-day server-local physical retention, idempotent receipt and epoch. **This resolves policy questions only: Restore/Reset Execute and Beta still HOLD.**
- Reviewed Architect reward history dispatch `REWARD_HISTORY_IMPLEMENTATION_GATE_20261102.md` and committed purchase-only v1 contract `docs/api/REWARD_PURCHASE_HISTORY_V1.md`. Confirmed source endpoint /rewards/history with bounded cursor paging and workspace/member ACL; no REQUEST/OBLIGATION, no delivery acknowledgement, no false status from REDEEMED. No POST/ledger mutation authorized. HTTP PG and consumer tests still pending.
- Published full dual-priority QA handoff `docs/qa/QA_DISPATCH_20261102_OWNER_RESET_REWARD_HISTORY.md` commit `411975b`: 20 ADR reset/restore acceptance gates, 5 Issue18 gates, 12 purchase-history v1 tests and 4 evidence gates. Clearly distinguishes source audit vs execution. Latest user Web 44 discovered 6 PASS/38 SKIPPED because QA credentials likely missing; earlier 41 PASS/1 SKIP is last comprehensive user-run regression. Do not certify two new Task/Reward tests yet.
- Next QA focus: Backend disposable PG keyset/history ACL and encrypted native restore; Web latest Task/Reward non-destructive and history consumer; Android synthetic cache isolation; exact-SHA CI. No destructive tests on persistent families.
