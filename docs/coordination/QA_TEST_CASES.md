# FamilyPoints — reproducible user manual test cases (interim BETA)

**Rule:** Test on own test Workspace and synthetic child/adult accounts; do not intentionally spend real balances, remove family records or manipulate live DB without authorization. For each run record `date, workspace(test alias), Web URL, deployed SHA if known, APK version, device, status, actual output/screenshots`.

## QA-001 / QA-002 — Approved reward purchase, debit, no session loss (P0)
**Where:** BETA Android child account + adult confirmation on Android/Web, optional authorized Backend DB read-only verification.
**Prerequisites:** BETA has reward ledger CHECK migration V35 and corresponding compatible APK (confirm with Release). Child has 30+ test points. Test reward: “30 хвилин гри”, price 20, requires adult approval.
**Steps:** 1) Child submits reward request. 2) Parent confirms once. 3) Child taps purchase/get once; observe status/remaining balance and whether login is retained. 4) Return to purchases and points history. 5) Tap purchase again or re-open request, verify duplicate rejected cleanly.
**Expected:** request READY→PURCHASED / purchase READY, exactly one ledger `REWARD_PURCHASE -20`, balance reduces by 20 once, child remains authenticated, second attempt safely returns 409/not available with clear UI feedback, no 500 and no double charge. **Actual physical delivery/REDEEMED by adult is deferred BKL-003; do not classify its absence as a new interim feature bug.**
**Evidence:** starting/final test points, response status (secrets removed), one purchase id, child screen before/after and logs.

## QA-003 — Session durability (P0)
**Where:** Android physical device + BETA test account. **Steps:** refresh rewards/other screens during short controlled Backend unavailability/transient 500 or 429 (only with Release-approved test). **Expected:** transient network/server failure shows retry/notification without spontaneous logout; actual expired/revoked auth requires login; no hidden duplicate purchase retry.

## QA-004 — Offline points synchronization (P0)
**Where:** Android real device. **Steps:** online note test balances, switch airplane mode, add +10, subtract -10, add custom +20 in offline-enabled flow, reconnect, sync, reopen points history and refresh. **Expected:** exactly the intended ledger net +20, no duplicate operations, clear sync status; disallowed actions remain disallowed. Don't perform on non-test child wallet.

## QA-005 — Workspace isolation (P0)
**Where:** Web and Android with two test workspaces A and B (same adult account may belong to both). **Steps:** switch A/B, inspect member lists, balances, groups, rewards and selected member icons; try navigating old URLs. **Expected:** only currently authorized family data, no A cache/image/transactions in B, forbidden access appropriately denied; switching back restores legitimate state.

## QA-006 — Signed APK handset installation (P0)
**Where:** physical Android. **Steps:** record installed versionCode/name, signing package and backup; install signed newer APK **over** old without uninstall, run, login via password and Google if configured, navigate rewards/tasks/calendar, check crash/permission. **Expected:** upgrade succeeds, existing sessions/offline cache preserved where compatible, no IllegalAccessError and no unexpected logout. Record file hash and actual URL/version. Avoid replacing family production device without fallback.

## QA-007 — Migration and backup (Release-only P0)
**Where:** disposable PostgreSQL initialized from sanitized/test snapshot, never real BETA write. **Steps:** confirm original Flyway history; apply V35 reward → V36 calendar → V37 media in a compatible integrated candidate, check existing point transactions and created date/time/media records, restore backup and revalidate. **Expected:** no Flyway version collision/checksum mismatch, no data loss, repeat startup clean. Source draft PR #8 is TEST ONLY and must not be merged as deploy.

## QA-008 — Web task actions (P1)
**Where:** BETA Web desktop/mobile. **Steps:** create task with assigned child, choose amount, save, edit with slow network and press Escape while request pending; complete as child and approve as adult if required. **Expected:** no premature dismissal/duplicate task, proper validation, progress/points once only.

## QA-009 — Android pull refresh (P1)
**Where:** APK built with latest five-screen refresh patches. **Steps:** open Dashboard, Tasks, Members, Points, Rewards in turn, pull down during fast and throttled network. **Expected:** indicator follows actual primary network request and stops on success/error, no 1.2s fixed timeout, no session wipe from transient 500.

## QA-010 / QA-011 — Calendar and Media draft feature contracts (P1)
**Where:** isolated integrated Backend/DEV only after release candidate identified; not assumed on BETA. **Steps:** calendar daily/week slots, timezone and reward windows; media user upload/catalog, access after clear and forbidden other-family view. **Expected:** correct scoped DTO and no cross-family leakage, but acceptance is BLOCKED before Backend PR #4/#5 merge and consumer rollout.

## QA-012 / QA-013 — Browser and end-to-end release smoke
**Where:** BETA Web and signed Android test device. **Steps:** verify light/dark theme, responsive hero images and reward modal save/Escape; registration/login or invited join, task assignment/claim/start/complete with adult confirmation and points, reward request/purchase, logout/login. **Expected:** consistent state across Web/Android and ledger, permissions, and errors. Save screenshot/network evidence for any failure.
