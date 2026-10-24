# FamilyPoints Android — consolidated P0 QA handoff / execution matrix

**Owner:** Android; **Recipient:** Testing / QA; **State:** CANDIDATE PREPARATION — NOT QA-APPROVED, NOT RELEASE-READY.
**Baseline:** main branch. Confirm final SHA and green Android CI before starting acceptance.

## Package scope (already committed)
- Reward workflow: child purchase request → parent approval → child redemption. Android 401 on optional Rewards background fetch no longer removes active session. Backend prerequisite: fix Flyway CHECK for point_transactions.source_type=REWARD_PURCHASE (XTR-20261024-A-02). Android cannot assert end-to-end success before deployment.
- Session stability: transient server 5xx/408/429 during token refresh no longer clears session; a rejected 401 ends online authentication. Commit 03b7057 preserves password-enrolled local offline verifier on 401 rather than deleting local offline access because of an API denial. **No auto-retry of purchases.**
- Pull-to-refresh: all 5 main tabs signal primary load completion; nonce checks ignore stale load; on screen/identity/network change spinner is cleared. Include Tasks management tab.
- Offline points: replayMutex, UUID operation IDs, queue confirm only after API success; partial 4xx replay refreshes balances for preceding successful operations; denied entries kept; 403 UI explains permissions and retains queue.
- Localized currencies: pluralization and custom forms across task/reward/point detail; unit test PointAmountFormatterTest.
- Explicit blockers: account-level isolation requires immutable Backend account ID and a safe legacy-row migration (XTR-20261024-A-03); current queue uses workspace+member viewer identity only. Avoid multiaccount ambiguous replay until resolved.

## Required automated gates
1. GitHub CI Android/Gradle: compile debug and unit tests PASS for **same head SHA** as QA build; attach failing unit test report otherwise.
2. Backend integration/migration test: successful child reward redemption with REWARD_PURCHASE ledger entry, exact one debit, idempotency, correct 401/403/409.
3. BETA backend version and Flyway migrations recorded; APK points to tested backend endpoint. No destructive database operations.
4. Generate signed release APK with correct increasing versionCode and existing signing key; verify installation as update retaining user data.

## Manual test scripts (run with two verified test accounts / distinct roles)
**AUTH-01** Parent password login online; navigate all tabs; background app 5 minutes; restore. Expected: no unsolicited login when token still valid.
**AUTH-02** Brief disconnect/API 503 during foreground refresh, reconnect, repeat. Expected: cached UI remains, local offline enrollment persists, no revoked permissions.
**AUTH-03** Session 401 on authenticated foreground call. Expected: server session ends, no silent retry of an unsafe write; offline login still needs enrolled local password.
**AUTH-04** Google sign-in and subsequent password sign-in for linked account. Expected: same verified account/workspace, no unintended enrollment or account linking by email.
**RW-01** Child requests price-20 approval-required reward; adult approves; child redeems once. Expected: child receives reward, balance -20 and exactly one SPEND/REWARD_PURCHASE; no logout.
**RW-02** Attempt double tap/retry after ambiguous network failure. Expected: never double spend/fulfill; manual server-ledger inspection required.
**OFF-01** Parent offline queue +10, -5, +20 with two currencies to two members, restart app, reconnect. Expected: durable exact-once replay, queue empties only upon confirmed response, balances correct.
**OFF-02** Arrange first operation success then second denied 403 (permission revoked server-side). Expected: first balance refreshed and confirmed item removed; denied/unprocessed entries stay queued with visible message.
**OFF-03** Arrange first success then second 401 (expired/revoked session). Expected: first confirmed persisted, later rows untouched, user prompted for online login.
**OFF-04** Switch between two families and between two global accounts (same local device). Expected: no leakage/replay of another account's cached members/balances/operations. **BLOCKED until immutable account identity contract and migration**.
**REF-01** Swipe refresh Dashboard, Tasks MY, Tasks MANAGEMENT, Members, Points, Rewards. Expected: spinner lasts until primary load completes, refreshed data visible.
**REF-02** Start refresh then switch tabs/lose connection/change workspace. Expected: spinner cancels, stale callback cannot corrupt new screen.
**REF-03** Rapid refresh repetitions, delayed response, HTTP 500. Expected: no duplicate action or stale incorrect UI.
**UI-01** UK 0/1/2/5/10/11/21/24/101 custom currency in balances/history/rewards/tasks, then DE/EN/RU; expected grammatical labels.
**UI-02** Light/dark theme, small screen, dialog scroll, keyboard visibility and popups anchored.

## Defect reporting
For each failure QA sends: test ID, app versionCode & commit SHA, device/API level, server release SHA, steps, expected/actual, screenshot/logcat (remove credentials/tokens), Android/Backend owner, severity. Batch all P0 defects together in one QA report. Android fixes the entire set and returns one regression candidate.

## Handoff conditions
Do not mark READY FOR QA or RELEASE until the final SHA passes Android Gradle/unit tests and Backend critical reward DB blocker resolved for integration checks. This document is a complete test package for QA planning; blocked cases remain explicitly blocked.

## 2026-10-24 supplementary code / CI gate
- Android commit `f278f3a`: main-screen auth/replay effects now key on workspaceId, memberId and username, not memberId alone, so switching workspace with the same member ID re-evaluates authentication and queued operations. Source review only.
- CI run `40109129616` for commit `c779c31`: Android Gradle debug build failed at `Compile Android and run unit tests`; backend, web and compose jobs passed. Failure logs/annotations not accessible via the current GitHub fetch integration; do not ascribe an unverified root cause.
- New CI run `40109457645` for `f278f3a` was in progress at handoff writing time. QA must verify final run result and full build logs before running or signing off the acceptance matrix.

## 2026-10-24 — CI Kotlin compilation blocker resolved in source, awaiting CI
- Read full GitHub Actions Android job logs (run 40109967288, job 120919441840). Kotlin `:app:compileDebugKotlin` failed in PointsScreen.kt because SettingsRepository and PointNameFormDto lacked imports; remaining `currencyForms.map` property resolution errors were consequent.
- Fixed missing imports in commit `7f6818d`. A new GitHub CI run `40110311069` was in progress after commit, not yet verified green. QA acceptance must use a final passing commit and signed installable build, not this source-only observation.

## 2026-10-24 — Automated CI gate PASSED; QA execution handoff
- **Verified passing CI baseline:** commit `28e3884dd55f575bbdba44fa8126ec40386bb3c9`, Actions run `40110467692` (https://github.com/o-kobylianskyi/family-points/actions/runs/40110467692). Four jobs PASSED, including `Android / Gradle debug build` with `:app:assembleDebug :app:testDebugUnitTest`, Backend Maven/PostgreSQL, frontend and Docker Compose.
- **QA status:** READY FOR SOURCE/TEST-PLAN REVIEW and Android debug functional tests when a build from the verified SHA is available. Not QA PASSED; no signed APK or on-device result claimed. Later commits must have their own CI confirmation before use as a release baseline.
- **Integration block:** Backend source-level V35 reward test green does not prove the Flyway migration has been deployed to BETA. Reward redemption RW-01/02 require a verified backend deployment and observed ledger result. Offline cross-account OFF-04 requires account ID contract and legacy queue migration; remains blocked and must not be silently marked passed.
- **QA workflow:** Execute all nonblocked AUTH/REF/UI/OFF cases with matching build SHA. Deliver one batched defect report; Android will fix the full set, run CI and return a consolidated regression candidate. Release decision remains separately gated.

## 2026-10-24 — Latest verified CI QA baseline and requested defect return
- Current verified CI baseline: commit `703526e9962f5eafd137f9303b22eb70a5a699b4`, run `40111579949`. Android job `120924269114`: `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:assembleDebug` all SUCCESS. Other Backend, Web and Compose CI jobs also SUCCESS.
- No confirmed Android P0 acceptance-defect report returned in available GitHub issues or coordination files at this check. Do not claim physical phone test PASS or QA signoff; user-visible APK may still be older.
- QA **ACTION**: execute nonblocked AUTH/REF/UI/OFF cases against an APK corresponding to a verified SHA; return one report grouped by P0 severity and linked testcase IDs with sanitized logcat/screenshot and exact APK versionCode. For blocked RW-01/RW-02 and OFF-04, first verify deployed Backend Flyway reward constraint and immutable account-id/legacy-queue ownership plan, respectively.
- Android **NEXT**: consume actual QA report as one consolidated defect-fix/regression packet, then recheck CI and resubmit. Release gate remains CLOSED until QA/Backend integration and signed APK upgrade verification.

## 2026-10-24 — V37 Media Android avatar regression candidate
- Commits `87467c0`, `271820d`, `c15d751`, `8a73f16`: Android MediaApi reads private authenticated THUMB variants using bearer; MembersScreen batches authorized `MEMBER/AVATAR` V37 references with <=100 targets per request, renders private thumbnails without a disk/public image cache, and falls back to initials if missing, catalog SVG or denied. Supplemental 401/403 never tears down session. Media images are cleared by workspace/user changes, lost connectivity and effect cancellation.
- **MED-01** Parent sees uploaded member avatar; HTTP requests have Authorization; media is workspace-scoped. **MED-02** No avatar / catalog SVG => readable initials fallback. **MED-03** Switch family/global user => no previous avatar/binding survives. **MED-04** Disable network / 401/403 media => member list remains and login unaffected. **MED-05** 101+ members => two reference batches <=100. **MED-06** server variant uppercase `THUMB` matches API contract. **MED-07** upload and bind operations are NOT yet Android UI features and cannot be signed off.
- CI after `8a73f16` pending at writing. Do not claim V37 Media upload/editor/full functionality or release readiness.

## 2026-10-24 — P0 AUTH-TASK-401 confirmed user regression
- User report: in test environment, entering Android Tasks tab immediately returns to login.
- Source root cause: TasksScreen propagated HTTP 401 from task-list, member-list, management-list, and task-write feature endpoints directly to global `onUnauthorized`, which clears the entire session and returns to login. Task endpoint 401 alone does not prove `/api/me` token revocation.
- Android candidate commit `8ec4ed3`: all six TasksScreen HttpException paths now report localized HTTP failure in the Tasks screen, preserve current global session and offline verifier; no automatic retry of task writes. Global AuthSessionManager still owns genuine auth-token expiry/validation.
- QA **AUTH-TASK-401**: sign in on BETA, enter Tasks (My and Management), simulate 401 on any individual Tasks API; app must stay signed in, show HTTP failure without clearing Dashboard/Points/Members, and allow retry. Repeat 403 and 500, switching tabs, offline recovery. Separately revoke actual token and verify global auth expiration logic still works.
- Check Android Gradle CI for exact QA commit before claiming PASS. If the failure continues, collect sanitized Android HTTP route/status and backend logs for test timestamp. Do not include bearer tokens.
