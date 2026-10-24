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


## WEB-SF-01 — Complete Soft Family visual regression package (handoff from Web, 2026-10-24)
**Scope lock:** existing Web only; do NOT alter approved Home hero/images, add new calendar/media/template/reset features, or treat Git main as deployed BETA without exact SHA.

**Preconditions:** Release/QA record deployed Web SHA and URL; authenticated parent and child test accounts; four languages uk/de/en/ru; desktop 1440×900 and 1366×768, mobile 390×844 and 360×800; latest Chrome or equivalent browser. Capture screenshots of light/dark for each case. Check browser console for runtime errors and horizontal overflow.

**Mandatory test matrix (run light + dark and desktop + mobile for each):**
1. **Navigation and tabs:** Dashboard (visual unchanged), Tasks, Members, Groups, Points, Rewards, Settings. Expected active/inactive/hover/focus visible, no white panels or text clipping, tab scroll works at 360px without horizontal document overflow.
2. **Tasks:** open task list, details, create/edit dialog, numeric amount combobox, date/time, executor/group selection, dropdown, actions. Expected all controls have themed readable borders/text, arrows remain inside one seamless shell, dropdown anchored, modal scroll and footer accessible on 360×800.
3. **Members and groups:** member cards/editor, child/parent role labels, nested group row and editor; test 40+ character names and DE locale. Expected wrap rather than overlap; controls at least usable with touch and tab focus; foreground/background semantic colors.
4. **Points:** award/deduct modal, shared NumberInput ± arrows and description; numeric typing and arrows ±1, min=1, empty state, invalid zero; Escape/backdrop/× while idle, slow-saving state. Expected no double-submit, controls aligned, no accidental close while saving, informative error, no duplicate balance mutation.
5. **Rewards:** catalog, add/edit form, request 'Хочу іншу нагороду', review and negotiation modal; required fields, nested number stepper, request and approval flows. Expected 14px field text inset, seamless number control, compact submit, readable dark/light, long labels fit mobile.
6. **Sign-in/onboarding/settings:** login Google/password and APK link spacing, member invite and roles, theme/language toggle, settings forms. Expected readable controls, focus ring, accessible buttons and no browser scroll traps.
7. **Keyboard/a11y:** sequential Tab/Shift+Tab through each open dialog, Escape when idle; while a submitted request is pending, do not close; check focus-visible indicator and keyboard access to arrows/buttons.
8. **Responsive/overflow:** repeat main 7 routes with mobile landscape or 768px tablet, 200% zoom and longest German translations. No clipped confirm/cancel/footer, no right-side hidden actions, long names wrap.
9. **Browser state:** refresh each route in both themes; switch between workspaces then back. No stale role/selected-user data or mixed theme, no white flash exposing confidential data.
10. **Regression baseline:** explicitly compare Home screen against user-approved screenshots/asset set and mark as no-change. Do NOT 'fix' its visuals without explicit user authorization.

**Evidence format per failed case:** case #, device/browser + viewport, theme+locale, URL/route, deployed SHA, steps, expected vs actual, screenshot, console errors (redacted), severity and owning track. A case is PASSED only after QA actually observes it; a successful Vite build is not a visual pass.

**Exit criteria:** all P0/P1 observed passes; any defects logged to Web; Web completes a *single consolidated remediation package*, then QA reruns **all failed cases plus neighboring regression cases** on updated deployed SHA. Repeat until green or explicitly documented release NO-GO. This section is an **assigned QA package**, not executed test results.
