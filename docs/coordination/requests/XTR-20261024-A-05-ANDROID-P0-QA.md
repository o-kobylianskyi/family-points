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
