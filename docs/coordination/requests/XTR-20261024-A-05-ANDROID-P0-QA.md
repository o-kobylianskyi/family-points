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

## 2026-10-24 — CAL-LOADING-01: stable loading overlay / no flashing content
- User provided emulator screenshots: switching calendar period briefly inserts loading line and replaces 2/2/0 counters/events with placeholders, causing content to jump.
- Android commit a20469a: CalendarScreen no longer clears last confirmed event projection at start of API request; retains associated displayed date during fetch, atomically swaps result/date on success; `CancellationException` propagated; old agenda is dimmed with blocking centered progress overlay (no inserted loading row), localized weekday added for DAY, counters get 14dp horizontal padding. Error keeps last snapshot visible.
- QA CAL-LOADING-01: switch DAY/WEEK/MONTH and next/previous date under network throttling; previous agenda and metric cards must remain at the same positions beneath a translucent loading overlay; on success, new projection updates atomically; on HTTP 500 or offline, prior data stays, a visible error appears and next request is possible; stale cancelled response must never overwrite newer range.
- QA CAL-LOADING-02: on real emulator API 37 and small phone, selected DAY date contains localized weekday; event/metrics card captions do not touch border; spinner does not add height to LazyColumn; overlays do not allow double taps.
- General UI rule for other Android tabs: retain last authorized snapshot during refresh, dim+overlay only the updating content, keep session unchanged for supplemental endpoint failures; avoid full-page loader or clearing list until successful replacement. Audit Dashboard/Tasks/Members/Points/Rewards and return screen-specific defects as one P1 UX regression package; not yet implemented across all of those screens.
- Verify Android Gradle CI for final head before QA acceptance; device rendering test still required.

## 2026-10-24 — P0 global account isolation implementation (Android SQLite v3)
- Backend current authenticated `/api/me` now returns immutable `accountId` (Backend commit 84297eafc). Android `CurrentUserResponse` deserializes it; older offline profiles without ID are **not** authorized to queue or replay operations.
- Android `OfflinePointsQueueStore` upgrades to schema v3 with nullable `account_id`; new offline points entries require positive accountId and every query/delete is additionally filtered by account_id, workspace_id, viewer_id. Existing v1/v2 records migrate with NULL owner and remain durable **quarantined and excluded from all automatic replay**. No silent deletion or reassignment. Points screen displays a localized warning with number of legacy unknown-owner rows.
- `PointsRepository.synchronizePending` obtains current online server identity from /api/me and requires exact (accountId, workspaceId, memberId) match *before replay starts*. This also protects against locally forged or stale viewer attribution. Replay still confirms only server-successful operations, keeps denied/pending entries and refreshes affected balances.
- Paths updated: PointsScreen enqueue/view/manual replay; MainActivity reconnection automatic replay. Related commits: e130c9b, f42c02a, 44a3f24, 6342a19, 12a9350, 43c6fc9, 1bdf7d0.
- QA OFF-ACCOUNT-01: Enroll parent account A, queue +10 while offline, then log in to distinct account B (same workspace/member if test fixture supports) and reconnect. B sees 0 A operations and cannot replay/confirm them; A can after verified login.
- QA OFF-ACCOUNT-02: Switch workspaces with same account, ensure cross-workspace isolation and retries only for owner/workspace/viewer. Other member IDs remain target-only.
- QA OFF-ACCOUNT-03: Upgrade APK from pre-v3 with existing pending queue. Records survive; quarantine count visible; replay never sends old ownerless rows under any user. Data recovery requires explicit owner confirmation tooling; currently **no automatic recovery**.
- QA OFF-ACCOUNT-04: API /api/me returns absent/mismatched accountId, workspaceId, or memberId. New queued writes blocked without ID; existing operations not replayed or removed. Confirm no cross-account ledger mutations.
- QA OFF-ACCOUNT-05: Multi-item replay, 401/403 mid-stream, app restart, duplicate operation IDs remain idempotent; confirmed entries removed once; pending entries retained. Repeat with two currencies, parent role restrictions.
- Status: code delivered, awaiting exact-head Android CI and device + Backend integration QA, not RELEASE approved. Existing other cache stores may still be workspace/member scoped and need a separate privacy audit; this package scopes the **operation queue**.

## 2026-10-24 — MED-AVATAR-WEB-ANDROID: missing self avatar on Android
- User verified avatar set in Web is not visible in Android. Root-cause source finding: Android V37 uploaded image only appeared in MembersScreen; shared AppHeader previously rendered just member name and role, without any media reference/thumbnail.
- Android commits `4f18e172` and `590f5627`: shared AppHeader requests current MEMBER/AVATAR authenticated V37 binding for current workspace/member and shows MemberMediaAvatar on all header-bearing screens, clearing binding on account/workspace change. Thumbnail fills circular crop. Optional media errors never force logout.
- QA case MED-AVATAR-WEB-ANDROID-01: upload a JPG/PNG avatar on Web for signed-in member; open Android on same family, verify circular thumbnail next to username in header and in Members tab; image persists after navigation and sign-in (requires live V37 deployment).
- QA MED-AVATAR-WEB-ANDROID-02: switch to another account/family and verify former avatar disappears without cross-account leak; a 401/403 media failure must not log user out.
- Known limitation: Android's BitmapFactory cannot decode catalog SVG avatar defaults; upload JPG/PNG should render, SVG requires dedicated decoding and testing. Android avatar upload/picker not yet wired.
- CI for exact-head changes pending at creation of this note. No on-device PASS claim.

## 2026-10-24 — MED-AVATAR-CATALOG-01 Web catalog avatar missing in Android (user screenshots)
- Reproduced through source: Web fetches `MediaService.Ref.path` pointing to `/workspaces/{id}/media/catalog/{key}/preview` and shows catalog SVG. Android MemberMediaAvatar previously requested image only when `mediaId` non-null, so bound catalog avatars with `mediaId=null, catalogKey=avatar-*` always fell back to initials in both shared AppHeader and MembersScreen.
- Android adds MediaApi.catalogPreview authenticated endpoint, AndroidSVG dependency and capped SVG bytes rendering to 128x128 bitmap, then circular crop. Only safe catalogKey pattern is interpolated; DEFAULT without explicit binding remains initials.
- QA: assign catalog avatar to self via Web, open Android on same workspace/account, verify image is visible in header and member card (not letter), survive tab changes; use another avatar catalog option and reopen Android; check user photo uploads still work; wrong workspace, missing bearer, 401/403, offline must show initials, not crash. Verify CI dependency resolution and debug compile.
- Commits: 2c49ecc, a9b7d31, b038e5a. Device and end-to-end results pending.

## 2026-10-25 — MED-AVATAR-RESUME-01 Web-to-Android avatar refresh
- Verified green combined CI at head 14025beb3, covering earlier Android catalog SVG decoder.
- Problem: shared header and MembersScreen used LaunchedEffect keyed to login/workspace/member and had no app-resume refresh when Web modified an avatar. Android stayed stale until re-creation/navigation.
- Implemented lifecycle ON_RESUME refresh on both screens: shared AppHeader re-fetches own MEMBER/AVATAR, MembersScreen re-fetches authorized batch bindings without blanking already visible avatars. Member avatar map remember key now includes immutable accountId, preventing state carryover between two accounts sharing member/workspace IDs. Commits 4f92b224, b5aef37, d87a0b7.
- QA MED-AVATAR-RESUME-01: enter Android and confirm A avatar; background Android, update member avatar in Web to B, foreground Android; header AND member cards must show B without logging out. Repeat B->none (initial fallback), uploaded photo->catalog SVG, and offline/online transition.
- QA MED-AVATAR-RESUME-02: switch accounts/workspaces; old avatar must disappear and never be shown for another identity. Validate initial offline no media on first login, failed 401/403 media requests do not invalidate authenticated session.
- CI for commits following 14025beb3 requires fresh exact-head success; no on-device acceptance yet.
