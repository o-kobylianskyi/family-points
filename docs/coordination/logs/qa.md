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
