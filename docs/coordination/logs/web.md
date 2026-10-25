# FamilyPoints — Web journal

New batches: React/Vite UI, Soft Family parity, accessibility, i18n and REST consumption. Retain existing Web entries in root development log.

## 2026-10-22 — Track initialized (DOCUMENTATION ONLY)
- Web's Soft Family dashboard/theme redesign is in current development history; this setup does not assert tested parity or deployment.
- Incoming: IMP-0001 visual/native parity and images; IMP-0002 auth/registration; IMP-0004 workspace switch; IMP-0005 authoritative synced state.
- Next: test light/dark/mobile layouts against agreed design, record screenshots, declare any Backend media contract needs before coding.


## 2026-10-22 — Web track ownership accepted; coordination onboarding

- **Assigned scope:** React/Vite frontend, layout, Soft Family light/dark design parity, responsiveness, accessibility, localization, Web API client consumption and web-only UX tests. Android UI/Compose, Java/DB/REST ownership, shared architectural contracts and all production releases remain with their respective tracks.
- **Reviewed integration HEAD:** `43e990ead5ab06b287066336292c17a2bd29b7b7` (observed 2026-10-22, before this journal update) on `feature/task-soft-delete`; checked all new coordination files, AGENTS, existing dev/checklists, concept/operations history and relevant Web/backend source. `main` still points to older `c6847c6389`. Future batches must refresh HEAD.
- **IMP-0001 (Web producer, ACK of scope):** visual Soft Family approved; Web hero/sidebar visual match remains unverified. Web-specific parts include dashboard, themes, illustrated icons, responsive cards; Backend/Architecture must decide tenant-scoped avatar and family-cover media contract before upload functionality; Android adapts natively.
- **IMP-0002 (Web consumer: ACK / NOT VERIFIED):** reviewed `frontend/src/api/authApi.js`, `accountApi.js`, `AuthContext.jsx`, Backend `UsernameAvailabilityController.java`, `GoogleAccountLinkController.java` and `SecurityConfig.java` at reviewed HEAD. Public username matcher is committed; actual BETA exposure/Google feature flags, errors and auth regression tests remain unverified.
- **IMP-0003 (Web consumer: ACK / BLOCKED pending Android+Release):** reviewed `DownloadAppPage.jsx` manifest, APK HEAD and SHA paths; Web owns UI and metadata presentation, NOT the signed APK or publishing. Signed package/device verification and Release gate remain external.
- **IMP-0004 (Web consumer: ACK / NOT VERIFIED):** reviewed `AuthContext.jsx`, `ChooseFamilyPage.jsx`, `authApi.js` workspace list/activate/onboarding and `taskApi.js` scoped URLs. Cross-family isolation and invitation E2E negative tests pending Backend/Release validation.
- **IMP-0005 (Web consumer: ACK of proposal, BLOCKED):** read backend-authoritative ledger requirement and `taskApi.js` existing workspace-scoped endpoints. Offline idempotent replay/conflict contract is not yet agreed/implemented; Web must not display speculative synchronized balances.
- **Current Web blockers:** uncommitted `family-hero.webp` and `family-promo.webp` images exist only in separately provided local ZIP (repo currently has SVG fallback only); Web screenshot parity, Vite lint/build, cross-page light/dark contrast, and four-language theme UI checks remain pending. No runtime, server, CI, VPS or DB verification was performed in this onboarding batch.
- **Next Web work:** confirm source-controlled artwork and visual acceptance of dashboard at desktop/mobile/light/dark, then standardize Members/Tasks/Points/Rewards/Settings. Register cross-track impact BEFORE changing any REST/media/auth contract. Commit only Web changes on an isolated `feature/web-*` branch as concurrency increases, integrate via reviewed PR.
- **Release hold:** only Release/DevOps decides deploy/tag/APK publication. No changes to Contabo, Flyway, Java, Android or signing in this batch.

## 2026-10-22 — Consolidated Web into single local Vite environment
- User confirmed one frontend environment: `family-points/frontend` on `localhost:5173`. No separate Web server/port required.
- Integrated Web-owned avatar component/CSS and member/tasks/points adoption, plus the verified two hero/promo WebP binaries **directly** into integration branch `feature/task-soft-delete` as commit `7077035b1371026414ad3887c8aa0af84c160fe9`, preserving all current Backend/Android/integration files. Seven exact paths transferred; no wholesale branch merge.
- Old Web branch `feature/web-soft-family` retained as archive for inspection; PR #2 to close as superseded, not to merge; `scripts/web/install-soft-family-assets.ps1` remains only in old Web branch, because binary assets are now tracked in integration and no longer need manual install.
- To view: start Vite from `family-points/frontend` on 5173 after `git pull --ff-only origin feature/task-soft-delete`. Do not delete a Windows worktree until checked for uncommitted files and removed through `git worktree remove`.
- Source integrated; CI/browser verification still pending. No server deployment.

## 2026-10-22 — Dashboard defects reported again; confirmed missing implementation
- User verified local 5173 after pull. Previous integration transferred illustrations/avatar infrastructure, **not** the requested reference exact-match fixes. Acknowledged misleading earlier completion language.
- Committed CSS correction `95159c47` to integration: sidebar promo image constrained within its panel; hero artwork uses contain and matches hero bounds instead of negative bottom/oversized height. Responsive overrides added; requires screenshot check.
- Outstanding: extract exact logo and graphic icons from provided reference only where original asset provenance and clean transparent extraction can be verified; do not call redrawn SVGs exact matches. Hero composition, responsive and both theme visual verification still open.
- No Vite/browser test run by assistant; no Backend/Android/DB/release changes.


## 2026-10-22 — Dashboard reference crop integration (PARTIAL — binary package local only)
- Inspected Backend integration branch and journal at `5d9fcbf4`: Backend has acknowledged media XTR-W-01 / IMP-0007 but API, schema, binary store and release backup are BLOCKED by Architecture ADR. **NO media database/API upload performed or claimed**. Separate calendar PR #4 is not a media implementation.
- Modified dashboard JSX, Sidebar and App.css to consume explicit `/assets/soft-family/reference/*` asset names with legacy SVG/image fallback. Removed duplicated main hero description. Adjusted hero and bottom-left promo panel; preserve real API data.
- Extracted an 11-asset WebP archive directly from user-approved reference image in this chat, including brand mark, 4 large stats, 4 quick-action icons, hero crop and full sidebar promo. Package not yet committed to GitHub binary storage; user must extract at repository root. **Source code without this package falls back to old graphics** — do not report full visual parity.
- Still requires actual browser side-by-side verification, exact alpha edges and responsiveness; extracted icons are reference crops and may contain partial background. No DB, VPS, Android, Maven or runtime smoke test.

## 2026-10-22 — User screenshot: reference assets visible, crop seams remain
- User screenshots confirm screenshot-cropped art is finally loading: bottom-left family promo, family hero and quick-action icons. Visible defects: rectangular hero raster background seam; duplicate/offset promo edge; hard square cropping around quick action icons. These are raster extraction limitations, not Git pulling failures.
- CSS commit `8fb40a51`: mask hero left/right seams, normalize promo frame (remove outer border), apply radial fade on quick action/empty-state icon crops, preserve theme and dynamic values. This is a workaround, not verified pixel parity or original transparent assets.
- Screenshot check after pull remains required; backend media DB upload API is still unimplemented, so images remain static assets. No deployment or build claimed.

## 2026-10-22 — Approved header plan vs browser fact
- User supplied crop pair: approved header stretches across available dashboard area and uses 3 distinct lines (welcome, white subtitle, teal unique motto) plus family art centered right and a right-side handwritten decoration; browser actual capped dashboard at `page-content max-width:1400px`, omitted subtitle and positioned artwork flush right without decoration.
- Updated 4 locale JSON files with `dashboardLive.familyMotto`, restored `dashboardLive.description` as subtitle (previous removal was incorrect), updated hero decoration, and added dashboard-scoped full-width CSS with responsive hero image and copy layout. Relevant commits `a67d522`, `f9f04b4`.
- Static asset photo is a screenshot crop, so perfect transparency/handwriting remains unavailable; handwritten text is typography approximation, not copied image. No browser run or full visual acceptance claimed. Deployment/Backend unaffected.

## 2026-10-23 — Dashboard header: image is full-section background
- User specifically requested hero art to be the background of the **whole header block**, not a separate image on its right. Preserved manually updated visual asset bytes: no files under `frontend/public/assets` created, replaced or deleted.
- Removed standalone `<img className="fp-hero-art">` from DashboardPage and set CSS `background-image` on `.fp-dashboard .fp-hero` using the same `/assets/soft-family/reference/hero-family.webp` URL with `background-size: cover`, position and gradient overlay for text contrast. Adjusted responsive styles and dark/light backgrounds. Original calligraphy/text remain HTML.
- Integration directly on `main`, no server deploy or database change. Build and actual browser screenshot validation pending; `cover` can crop edges at aspect ratios and may need separate user-approved positioning adjustment.

## 2026-10-23 — Approved Home visual baseline and branch workflow (documentation only)
- User confirmed the Home/dashboard visual design is finished: hero, cards, icons, sidebar and light-theme hover. Preserve all approved styles and manually edited image assets. Do not change Home appearance without explicit approval. Next Web scope: bug fixing on other pages.
- User requires `main` as default active development branch for all tracks; temporary branches only when an exceptional, documented safety need exists. Shared AGENTS.md, coordination protocol and README have been updated.
- The local Windows checkout was last reported on `feature/android-soft-family-structure` with 40 unpushed commits; this GitHub-only policy change does not switch it, transfer local commits or alter any images. Previous Web light-theme hover fix `a4f1672` was confirmed locally by user. No source, tests or deploy in this documentation update.


## 2026-10-24 — Web consumer contract review for Backend PR #4/#5 and templates (ACK WITH BLOCKERS)
- Reviewed Backend draft sources `docs/api/REWARD_CALENDAR_V1.md` on `feature/backend-calendar-rewards-v1`, `docs/api/MEDIA_V1.md` on `feature/backend-media-assets-v1`, draft PR #4/#5 descriptions, Backend coordination log, ADR-TEMPLATES-001 / XTR-20261024-P-01/-02 and IMP-0006/0007/0008. This is **contract review**, not compiled integration, CI approval, API deployment or feature delivery.
- **Calendar PR #4 — Web semantic contract ACK / integration BLOCKED until merged, tested and released.** Web accepts read-only `GET /workspaces/{workspaceId}/calendar` as bounded (62 inclusive days, 2500 events) TASK deadlines and REWARD_WINDOW acquisition events; TASK virtual rows (`instanceId=null`) are never stored/created by GET. `scheduleOpenNow` is only a time gate, not purchase eligibility. Accept separate manager-only availability PUT, 409 `REWARD_OUTSIDE_AVAILABILITY_WINDOW` handled without optimistic debit, and IANA workspace timezone with legacy UTC default. **Backend action:** freeze exact Java DTO JSON (including event keys, dates, enum statuses, response/errors and sorting/pagination/truncation behavior); verify visibility for other members and workspace switching; test boundary, DST and full quota rollback; explain behavior when event cap is reached; publish two-workspace API tests and migration V35 plus calendar/media V36 combined upgrade evidence. Web UI integration waits for an accepted exact contract and endpoint availability; do not render fabricated calendar totals.
- **Media PR #5 — Web product architecture ACK / production integration NOT APPROVED.** Accept separation of global reusable `CATALOG` (versioned, localized, no workspace-private quota) from family-private `UPLOAD` (JWT-scoped normalized JPEG/PNG variants), stable catalog keys, inherited task icons, explicit override/remove and optimistic expectedVersion/409. Bundled 15 SVG catalog previews are Backend draft only and must not replace user-approved `frontend/public/assets/soft-family/reference/` dashboard art. For private image `GET .../variants/{variant}`, browser `<img src>` cannot send Bearer token: Web will fetch authorized bytes and make/revoke Blob URLs per workspace/session, or use an explicitly reviewed safe delivery alternative.
- **Media Backend blockers requiring answer/fix before Web implementation/ACK:** finalize effectiveImage reference + slot, source type, inherited/default indication, version/tombstone fields in existing member, group, workspace and task DTOs; stable targetType/slot enums and per-target view/bind permission matrix incl. child and delegated group roles; upload type/MIME/errors/file-size and WebP handling (current draft only JPEG/PNG, while Web static assets are WebP); catalog `previewPath` format and authentication/cache lifecycle for SVG; 404/403/409 behavior, conditional/version updates, cross-family byte isolation, owner cleanup and image invalidation, quotas/GC, tests and Release backups. Current PR #5 remains DRAFT, no client integration or BETA deploy.
- **Template library IMP-0008 — Web task accepted for planning; Backend implementation not present.** Web will implement an optional four-locale Soft Family wizard, versioned catalog selection, preview/edit/skip/resume, explicit confirm and meaningful 403/409/retry UI when Backend XTR-20261024-P-01 offers real catalog/preview/idempotent confirm APIs. Must never fabricate family members, permissions, starter task instances or ledger transactions; existing family content remains untouched until confirmed install. Web does not interpret TaskSuggestionController or SIMPLE/NORMAL/HARD economic presets as template catalog.
- **Handoff to Backend/Architecture/Release:** treat this as **Web consumer review recorded, conditional semantic ACK only**. Calendar and media feature-readiness gates remain OPEN. Backend can continue PR #4/#5 and template B request; send final contract+CI+combined Flyway results for client integration. Release remains independently gated; do not merge draft PRs or deploy by this review alone.


## 2026-10-24 — User-directed intermediate release scope freeze
- User explicitly directed all tracks to **finish/pull through only already-listed tasks before the next intermediate release**. Do **not** accept or implement newly proposed features prior to that release. Prior planning suggestion to start the calendar as a new Web package is superseded by this decision.
- Web priority: finish outstanding defects/regressions of current functionality and approved Soft Family styling; audit existing task flows (members/groups, tasks, balances, rewards, login/onboarding/settings), light/dark mode, access controls, four locales, responsive layouts, build/CI. Keep approved Home/dashboard visuals and user-supplied reference assets unchanged.
- Existing Architecture tasks (calendar PR #4, media PR #5, template/reset XTR packets) retain their recorded ownership/status. **Do not silently promote blocked work to release-ready**: integrate only if API is already safely merged, client-compatible, verified and explicitly inside the agreed release scope. Otherwise retain documented blockers and defer functionality rather than fabricate data or expand scope.
- Release readiness requires objective smoke/regression evidence and handoff to Release/DevOps; no Web-triggered BETA or PROD publication. This is a **scope freeze and reprioritization**, not task completion, blanket approval of draft PRs, or a claim tests passed.


## 2026-10-24 — Intermediate release Web stabilization: point operation modal Escape
- Audited current frontend against existing modal dismissal behavior: `PointOperationModal.jsx` already closes on overlay/cancel but lacked Escape handling; the user previously requested Escape for modal dismissal. Fixed in `main` commit `86e2185`: while open and not submitting, Escape invokes existing `onClose` (listener cleaned up on rerender/unmount). Submission in progress is not interrupted by Escape; no API, currency ledger, approved Home dashboard or images changed.
- Remaining release validation: browser verify award/deduct flows, modal Escape before submission, error states, locale text, numeric stepper, light/dark themes. Latest GitHub Actions results are not a browser acceptance test. This is a targeted bug fix, not new functionality or release authorization.


## 2026-10-24 — Intermediate release stabilization: task reward negotiation Escape
- Audited existing modal controls in the current `main`: MemberEditor and GroupEditor already contain Escape listeners; `TaskRewardNegotiationModal` lacked one, although it has cancel/close actions. Commit `ec19794` adds a cleaned-up Escape listener invoking existing `onClose` only when not `saving`. No request data, reward approval, points ledger or Backend behavior changed.
- Existing release checklist still OPEN: Web build/lint/i18n, browser verification for dialog keyboard behavior (including nested dialogs and save-in-flight), dark/light visual regression, task/reward/points authorization and transaction smoke, account login and onboarding. Do not mark the entire current task stack complete from source-only review.
- Freeze remains in force: no new calendar/media/template/reset features before intermediate release; approved Home design/assets untouched.


## 2026-10-24 — Release stack: task editor dismissal safety
- Audited CreateTaskModal, PointOperationModal, and TaskRewardNegotiationModal against Escape semantics. Found an existing regression in CreateTaskModal: window key listener unconditionally invoked onClose even while asynchronous task create/edit save was in progress. Commit `2474a1e` relocates the listener after the save-state declaration and registers it only when `loading=false`; the listener is cleaned up on loading changes/unmount. No task mutation or API semantics changed.
- PointOperationModal and TaskRewardNegotiationModal already avoid Escape dismissal during an active save. This is a **source-level fix only**: no claim of tested browser interactions, passing latest CI, or release readiness.
- Intermediate release stack **OPEN**: confirm Web build/lint/i18n, dark/light modes, modal keyboard/save-in-flight interaction, existing task/reward/points flows, cross-role access, responsive layout, and final Release handoff. Approved Home images/layout remain frozen. No new modules before the intermediate release.


## 2026-10-24 — Pre-release Web: point-operation in-flight modal safety
- Audited current `NumberInput` integration and `PointOperationModal`. Found that, although Escape was blocked during submission, backdrop click and header × still closed the modal during pending point mutation; form submit lacked an immediate repeat-submit guard. Fixed in `main` commit `809a07f`: prevent backdrop/× dismissal while submitting and reject duplicate submit attempts in the active submitting state. Existing cancel button and Escape protections preserved; no backend transaction logic or dashboard assets modified.
- Source inspection only. **Still open:** verify browser double-click and delayed-request paths, point ledger results, reward approval flows, CI/Web build/lint/i18n, responsive and role-based smoke. Do not claim release readiness before those tests.


## 2026-10-24 — Release stack: task-reward negotiation duplicate submission guard
- Source audit confirmed TaskRewardNegotiationModal disabled submit visually during `saving` but had no immediate submit guard, and the header × remained clickable in-flight. Commit `10188d8` adds `if (saving) return` before payload/remote submission and disables header close during save, aligning with the existing Escape/cancel protection. No reward request APIs, ledger operations or approved dashboard files changed.
- Pending validation: browser double-click/delayed-response, review/create paths, backend idempotency and role permissions; build/lint/i18n and release smoke remain open. Source-level fix alone does not constitute verified readiness.


## 2026-10-24 — Pre-release Web task editor submission guard
- Reviewed existing CreateTaskModal create/edit form. Found that active `loading` already blocked Escape, but header × stayed enabled and submit handler did not immediately reject a second submit during save. Commit `2e36a0a` adds a save-state guard to submit and disables close button while loading. No change to task payload, backend contract, accepted dashboard styling or images.
- Still to verify: browser double-click and slow API, successful task creation/edit, server-side idempotency, all four locales, responsive layout, Web lint/build/i18n, and cross-role regression before release. Source-level patch only; release stack remains OPEN under freeze.


## 2026-10-24 — One-pack Soft Family stabilization pass (existing screens only)
- User requested finish the **entire existing interface stabilization workstream in one coherent package**, with no new features before intermediate release. Scope audit located hardcoded light borders / underspecified hover/focus/mobile styling in `Navigation.css` and `group-v2.css`, plus possible viewport overflow and dark modal consistency issues in `App.css`.
- Changed on `main`: `42f5ff5` navigation tabs use semantic Soft Family tokens, contrast/keyboard focus and mobile horizontal scroll; `026b22a` group editor list items, responsive row wrapping and themed surfaces; `8b09a22` shared modal dark fields, screen-safe scroll heights, mobile spacing and focus-visible affordances. No changed Home/dashboard hero/asset selectors, no image edits, no API/DB modification.
- This package covers source-level theme/responsive fixes, **NOT fully verified browser visual acceptance**. Still require side-by-side screenshots of each non-Home route in light/dark at desktop and narrow mobile widths, keyboard focus/Escape, long DE strings, group editor and modal scrolling; confirm Web build/lint/i18n for exact final SHA. Previously observed failed overall CI run `40107993643` had Web production build SUCCESS but Android compile FAILED: not evidence of a Web compile regression. Do not mark the interface workstream completely accepted without its browser validation.
- Intermediate-release scope freeze remains; current blocked calendar/media/template/reset functionality not incorporated. Send Web source SHA and validation requirements to QA/Release after CI verification.


## 2026-10-24 — Soft Family package CI and CSS specificity regression follow-up
- Exact GitHub Actions run `40108419150` on `60a0a0a`: **Web / Node / Vite production build SUCCESS**; Java/Maven and Compose configuration SUCCESS; overall workflow FAILURE caused by **Android / Gradle debug build**. This is not a Web production build failure. Full Web lint/i18n/browser screenshots are not covered by that verified job.
- Source inspection uncovered collision between the newly added dark-modal `input` surface selector and nested shared `NumberInput`: its internal field could be painted separately rather than inheriting one seamless shared shell. Fix `15b2ebf` restores a transparent, borderless child number field inside dark modals, leaving the 44px shared shell and arrows unchanged.
- **Soft Family interface stabilization source changes are packaged**; acceptance remains **pending browser/QA visual checks** on non-approved-Home routes at light/dark desktop/mobile sizes, including modal scroll, German overflow, navigation, groups, rewards and shared numeric fields. No deployment approval. Freeze on new features and approved Home images remains in effect.


## 2026-10-24 — User correction: atomic large-package Web→QA→Web cycles REQUIRED
- User rejected incremental 2–3 CSS edits and unsupported rising readiness percentages. From now: select one existing critical workstream, audit it end-to-end, implement a **single coherent substantial fix package** with tests, build/lint/i18n, journal and exact SHA; then explicitly hand the whole package to QA. **Next response/cycle** starts by reading QA results and fixing all reported failures together, followed by full regression re-test. Move to the next workstream only when the previous one is accepted or blockers explicitly documented. No new feature scope before intermediate release.
- Existing Soft Family code stabilization was committed in several earlier micro-fixes; rather than falsely claiming completed visual QA, Web now submitted consolidated suite **WEB-SF-01** in `docs/coordination/QA_TEST_CASES.md` and registered it in `docs/coordination/QA_TEST_BOARD.md`. QA to cover all existing pages, both themes, mobile/tablet/desktop, four locales, accessibility and stable approved Home. Actual browser test outcomes still NOT_VERIFIED; Release must identify deployed SHA.
- **Handoff sent through GitHub**; QA reads the board and performs explicit testing. Code source is not equivalent to deployed BETA; no silent green statuses, no release authorization.


## 2026-10-24 — Consolidated QA remediation WEB-SF-01-F01/F02 (resubmit)
- Read independent QA source audit from `docs/coordination/logs/qa.md`: F01 hardcoded Ukrainian reputation amount label in task reward negotiation, F02 fixed English numeric stepper screen-reader labels. Both reproduced through source inspection, no completed browser matrix. Fixed **both findings in one logical batch**: TaskRewardNegotiationModal now uses existing `rewardNegUi.amount`; shared NumberInput now obtains `increase`, `decrease` and group `controls` labels from i18next with four locale catalog entries uk/de/en/ru. Code + translations committed in `main`, final localization SHA `d3e5078`.
- QA risk R01 (near-simultaneous POST requests) is **not runtime reproduced** and remains a test case, not a closed fix. UI state guards already exist; Backend idempotency independently required.
- Handoff: QA retest F01/F02 with language switching and accessibility tree; re-run all WEB-SF-01 matrix items after Release confirms deployed SHA. No assertion of browser, lint, i18n or exact-SHA CI pass here. Approved Home and image assets untouched; no BETA publish.


## 2026-10-24 — WEB-SF-01 R01 preventative consolidated mutation guard package
- Rechecked current QA journal/report: F01/F02 already source-fixed and remain NEEDS_RETEST; browser matrix 0 verified passes and BETA deployed Web SHA unknown. Prior main CI run 40117445277 at 94e3f43 passed Web, Backend, Compose, Android, but is NOT CI evidence for the new Web changes.
- Audited three critical forms and found React state checks alone cannot synchronously exclude near-simultaneous submit handlers before the next render. Added a synchronous useRef submit lock (taken immediately before async mutation, released in finally) to PointOperationModal, CreateTaskModal and TaskRewardNegotiationModal, retaining existing validation, disabled controls, Escape guards, request semantics and error handling. Commits: 27eec1a, b52d721, 3d70240. Dashboard and approved assets untouched.
- R01 is now source-mitigated, NOT reproduced nor E2E verified; do NOT claim API idempotency or exactly-once ledger guarantees. QA must test rapid double click/Enter with throttled API and confirm exactly one POST plus one ledger operation for point award/deduct, task create/edit and task reward request/review. Also retest F01/F02, four locales, both themes, mobile/desktop matrix and focus/disabled state. Record target CI run and exact tested deployed SHA before PASSED. WEB-SF-01 remains NO-GO pending browser acceptance.


## 2026-10-24 — Backend consumer integration packet (Web Calendar V36 / Media V37 / reward windows)
- User explicitly requested connecting the newly caught-up Backend. Re-read current Backend integration log, Calendar/Media contracts, latest main. Backend V35/V36/V37 source integration is present in main, but exact deployed BETA SHA and real Web E2E approval remain unconfirmed.
- Implemented dedicated `calendarApi.js`: bounded workspace-scoped calendar GET, workspace timezone GET/PUT; `CalendarPage.jsx` authenticated read-only list with date/type filter and privileged timezone editing; /calendar route and four-locale sidebar navigation. Calendar events are server-provided (no fake instances, no optimistic purchase eligibility); 62-day range enforced.
- Implemented `mediaApi.js`: catalog, manifest, effective binding, CAS binding PUT/DELETE, authenticated Blob URL fetching and multipart upload. `FamilyCoverSettings.jsx` provides actual cover selection, JPEG/PNG upload and restore-default in existing Settings; respects MANAGE_WORKSPACE or ADMIN_OVERRIDE; revokes Blob URLs when workspace changes/unmount; approved default dashboard hero asset is NOT modified and default remains static.
- Added reward availability GET/PUT consumers in `rewardApi.js`. Schedule form editing is not exposed yet: existing reward purchase UX must not claim that scheduleOpenNow means purchasable. No new Backend endpoints invented, no ledger contract changed.
- Commits: 2909dc8 calendar API; af157a7 media API; 57e0cb0 calendar view; 583df13 route; 8d26c4c/2231943 navigation; 57379ce availability API; 047a46d cover settings; 995cbea settings integration.
- QA release blockers: exact integrated SHA, Web build/lint/i18n on final SHA, actual BETA deploy, authenticated browser tests: 62/63-day boundaries, selected workspace timezone and permissions, calendar event privacy/virtual occurrence, media catalog binding, multi-tenant authorization, authenticated Blob byte access and revoked URL on workspace switch, 5MiB upload validation, 409 stale-version conflict, default cover fallback and approved Home no-change. No claim of acceptance from source integration.


### Web Backend V36 reward editor follow-up, 2026-10-24
- Added `RewardAvailabilityEditor.jsx` for MANAGE_REWARDS context on existing Rewards management edit screen; AJAX loads actual reward availability, updates ALWAYS/ONE_TIME/RECURRING_WEEKLY independently from ordinary reward PUT. Preserves remaining weekly windows when editing first weekly slot. Requires explicit ISO 8601 UTC offset for ONE_TIME to avoid silently guessing DST interpretation. Commit sequence 93d138e, 1df9154, 21e6176, ce7c92d. No client-side guarantee of purchase eligibility; Backend 409 is authoritative.
- QA follow-up: verify weekly existing multiple windows survive editing; one-time input offset, 409 violations, and role authorization; test unchanged reward create/edit and other currencies. Browser acceptance and deployed SHA are NOT verified. New integration package run currently pending; no PASS recorded until completed.


## 2026-10-24 — Backend Web integration QA follow-up, critical validation correction
- Latest exact integration CI run 40118508340 on dbb41488 PASSED all four workflows; it does not constitute browser acceptance.
- Post-integration source regression audit identified a confirmed defect in RewardAvailabilityEditor: the ISO 8601 regular expression was doubly escaped and rejected valid times, while initialization removed an existing offset, making round-trip save unsafe. Fixed date validation, retained original UTC offsets and added finite instant comparisons in commit c3cff160.
- FamilyCoverSettings media fetch now discards stale async result URL safely on workspace effect cleanup and clears previous error state when workspace changes; commit 1a71e45. Blob URL revocation remains mandatory.
- All WEB-SF-01 and Backend V36/V37 authenticated BETA browser checks remain NOT_VERIFIED, exact deploy SHA unknown. QA test ONE_TIME existing +01/+02 read/edit/save round trip, conflicting window 409, family switching during delayed media response, and object URL revocation. No production deployment authorization from this entry.


## 2026-10-24 — Web reward schedule editor full weekly-window packet
- Starting baseline main 69ecf313, all four CI jobs PASS run 40119056029, but no manual BETA browser acceptance or deployed SHA evidence.
- Completed existing Backend V36 consumer flow in `frontend/src/components/RewardAvailabilityEditor.jsx` commit d4507ad. Replaced first-weekly-window-only editor with all 0..56 weekly windows: distinct IDs, day and local times, effective date bounds, enabled toggle, add/remove, round-trip full array, preservation of server-issued windowId and values; blocking client validation for malformed hours/days/date ranges and overlapping enabled windows. One-time windows preserve explicit ISO 8601 UTC offsets; four locales covered. Synchronous submission lock; Backend remains authoritative on 403/409/quotas/DST.
- Required manual browser QA: edit existing multiple weekly windows without losing others; add/delete/disable; overlapping and effective-date boundary; 56-window limit; timezone DST spring/autumn one-time offset; stale save 409; workspaces A/B; child permissions; long German labels/360px dark-light; reward purchase outside availability -> 409 with no point debit. Do not count build as browser pass.
- No Home/dashboard/image/style changes, no BETA deploy. Source fix submitted for CI and QA. Do not mark WEB-SF-01 or V36 accepted until pinned deployed runtime evidence.


## 2026-10-24 — Media V37 explicit cover end-to-end consumer packet
- Confirmed main SHA 9b180a9 had all four CI jobs green in GitHub Actions run 40119559180 before this patch. No deployed BETA exact Web SHA/browser screenshots available.
- Hardening in `frontend/src/api/mediaApi.js` commit 67f4859: authorized image requests must match selected workspace prefix, reject traversal/absolute/query URL, and require image MIME before creating Blob URL. `FamilyCoverSettings.jsx` now passes active workspace ID, commit b6ac40e.
- Dashboard integration commit ccbc2ea: GET workspace media manifest and only if cover.source !== DEFAULT plus authorized path, request authenticated image bytes as revocable Blob URL. Render an explicit custom family cover with existing hero text/geometry; on DEFAULT/error keep **precisely existing CSS artwork/hero** with no style override. On workspace switch/unmount discard late results and revoke object URL. No changes to reference assets or Dashboard CSS.
- QA manual acceptance: standard default hero screenshot comparison unchanged on both themes and 360/1440px; custom catalog/upload cover visible after settings-save and reload, switch family A→B never shows A's cover, logout/return never reuses URL; restore default, expired token/403/404/error show original default. Check GET image authorization is same workspace and correct COVER variant; inspect network requests and objectURL lifecycle.
- This package is source-level integration only; full visual/E2E QA NOT_VERIFIED and no deployment authorized by Web.


## 2026-10-24 — Media V37 member/group/task client integration packet
- User explicitly deferred manual QA until later. Continue coherent current-stack source/automated integration without claiming browser QA PASS.
- Baseline current main at start 5029c48f (Media cover packet CI 40119992292 in progress); no confirmed deployed BETA SHA.
- Created reusable `frontend/src/components/EntityMedia.jsx` for scoped Media binding GET, catalog metadata GET, authorized binary via Blob URL, privileged catalog choice, JPEG/PNG <=5MiB upload, expectedVersion CAS, clear override, and cleanup of late responses/blob URLs on workspace/entity changes; four-locale control text. Commit bca8d5b.
- Wired MEMBER AVATAR and MEMBER_GROUP ICON in `MembersPage.jsx` (08c5b99), group ICON in `GroupPage.jsx` (3949c6e), TASK ICON in `TaskDetailsPage.jsx` (87d25c4). Existing approved Dashboard art/layout/assets unchanged; no new server APIs or schema mutations. Edit actions are shown only for respective manager permissions; Backend remains authoritative for delegated role exceptions and ACL.
- Automated GitHub Actions on final main required; manually test uploaded and catalog member/group/task imagery, revocation after switch A/B, changing permissions after rendering, stale 409, not-found 404, role-restricted GET and oversized/wrong MIME upload. Current QA tests intentionally pending; no Release PASS from source/build alone.


## 2026-10-24 — Media V37 stabilization: workspace switch and stale CAS package
- Source reviewed on live main, previous concurrent CI run 40120298412 still executing at audit time; no manual QA performed by user direction.
- EntityMedia now tracks its target workspace/type/id/slot across async writes, preventing successful/caught mutation from updating the wrong family/entity screen after switching; HTTP 409 stale expectedVersion prompts a fresh binding GET via revision trigger, while retaining error. Code commit 3c8d429.
- FamilyCoverSettings now uses synchronous mutation lock and selected-workspace identity to prevent stale cross-family state transitions; on HTTP 409 fetches the new manifest without implicitly retrying a write. Code commit 043e33b.
- No Backend APIs, Home reference assets, task/reward ledger semantics or dashboard styles changed. Automated CI and manual browser screenshots still separate release gates. QA later: delayed upload A→B, rapidly repeated upload/bind, stale version 409, failed authorization and default cover fallback.


## 2026-10-24 — User reported Calendar Web UI unusable: replaced technical list with real visual calendar
- User screenshot: Calendar was a raw list of hundreds of repeated PLANNED tasks with no visual dates/timeline, inline timezone input dominating page, default HTML-ish controls. This is confirmed visual UX defect in previous `CalendarPage.jsx`, not API failure.
- Replaced raw event dump with three calendar views: day (00–23 timeline by actual event deadline or reward-window start, untimed section), week (seven dated columns/cards), month (dated grid with compact event chips and drill down). Interactive previous/next/today controls, type filtering, event navigation to task/reward routes, compact settings panel for workspace timezone, four locale labels. Source: `CalendarPage.jsx` e6713a0; Soft Family scoped responsive stylesheet `CalendarPage.css` ea2b48b.
- Day task scheduledTime/dueAt is explicitly DEADLINE, not start. Do not draw arbitrary start-to-end occupancy. Reward event startsAt identifies availability window start only. No synthetic task instances.
- On day/week/month switch do not temporarily show 'no events' while API loads: preserve last content and dim pending grid with subtle indicator. Remaining limitation: current view will temporarily show previous selection's grid until new data arrives; QA pending. Test dense recurrent tasks; 24h boundaries, month edges, nav, four locales and mobile horizontal scroll; auth and permissions inherited from Backend.
- User asked for actual calendar and normal day progression. Manual QA deferred globally by previous instruction, but UI screenshot shows previous design cannot be accepted. Current source package not yet browser-approved and no BETA deploy.


## 2026-10-24 — User screenshot regression: member card destroyed by inline Media controls
- Screenshot from user shows native file inputs, selectors, 'no image' text overlaid across narrow member cards; member name displaced and upload widgets visually leaking into neighbors. Confirmed root cause: previous Web change mounted a permanently expanded EntityMedia after the default FamilyAvatar for each member.
- Restored one clean member avatar position and moved image selection/upload/reset to compact pencil-triggered modal in `EntityMedia` (d2c2565) and `MembersPage` (819d975). Scoped Soft Family CSS `EntityMedia.css` (7804b47) imported at f72194c. Existing avatar fallback persists; no duplicate 'no image' on card and file controls never render inline in member grid. Modal width bounded and file buttons styled for themes.
- Keep original member card click for editing member details; pencil stops bubbling, so media action opens separately. Standard Dashboard CSS/art untouched.
- Manual browser QA deferred by user, but screenshot regression is a clear rejection of previous layout. Verify member cards with 3+ members, avatar upload modal, role edit click, responsive widths and light/dark, eventual rendering after refresh. CI exact integrated SHA must pass independently.


## 2026-10-24 — Screenshot fixes: task header and family cover editor
- The user provided two screenshots showing remaining raw Media forms in Task Details and Family Cover Settings. Earlier image mockups were NOT code changes; confirmed this explicitly and shipped actual source corrections.
- Task details now uses compact EntityMedia thumbnail/placeholder and pencil-triggered modal, removing inline select/native file upload from task header (5565f31); scoped task header styles (e79faf6).
- Family cover settings now displays wide existing preview, human-readable catalog labels, responsive cover option controls and visually hidden native file input behind styled upload action (215b83d/8e48eee); scoped CSS (4c02046). No invented remote image preview for incomplete catalog choices; only selectable entries with verified previewPath are presented.
- Backend Media API, selected-workspace scope and approved Home artwork stay untouched. CI and user/browser QA remain independently required; no release claim.


## 2026-10-24 — Screenshot defect: media modal jumps when mouse moves
- User supplied two screenshots showing modal attached beside member card and then centered depending on mouse hover. Root cause: .member-card[role=button]:hover uses transform and creates containing block for nested position:fixed overlay.
- Fix: render EntityMedia modal via React createPortal(..., document.body), outside transformed member card; viewport-fixed, centered, independent of hover transform. Commits 3b70db2 and following EntityMedia.css fix. No backend or approved dashboard assets touched. Recheck modal center across mouse movement, all member cards, Escape/focus/scroll, themes and mobile in later manual QA.


## 2026-10-24 — User screenshot: internal Media catalog labels and unrelated media choices
- Screenshot: open member avatar select shows raw i18n IDs `media.catalog.avatarSunflower` etc and unrelated family cover, group, task assets. This is confirmed consumer UX/validation defect, independent of Media Backend catalog.
- EntityMedia filter now restricts catalog choices to the target kind (MEMBER→AVATAR; MEMBER_GROUP→GROUP; TASK→TASK/TASK_CATEGORY), while excluding unselectable entries and entries with no previewPath. English/Ukrainian/German/Russian friendly labels for current catalog IDs replace raw `media.catalog.*` identifiers. Source commit f510593.
- Backend still validates catalog eligibility; no claim user-visible browser PASS. QA later: check avatar select shows only 9 avatar choices, all labels localized, selection persists, other Media entity types still have valid catalog entries, 403/409 enforced. New CI exact commit required.


## 2026-10-24 — Screenshot defect: changed member avatar does not propagate to Points or header
- User screenshot confirms own avatar displays in Members, but Points selector, Points balance hero and global account header continue showing initials; root cause disconnected legacy views plus FamilyAvatar rejecting authenticated `blob:` URLs.
- Added `MemberAvatar.jsx` (951599e) as secure effective MEMBER/AVATAR lookup with authorized fetch, workspace/account scoping and Blob URL revocation. Adopted it in Points selector + selected member summary (c968c9c), global account header (8fe54b4).
- Updated FamilyAvatar to render trusted private Blob URLs (e673338). MemberAvatar listens for same-workspace/member image updates (57b9a0a); EntityMedia broadcasts after successful MEMBER binding changes (c83f1ce), triggering refresh in still-mounted header and any mounted selectors. On logout/workspace switch, effect cleanup revokes old image URLs; stale async loads discarded.
- Verify after actual deploy: avatar shown in Members, Points selector + large summary, global top-right account; change photo and remain on same screen, switch family, logout/login, missing image fallback, revoked privileges. Backend remains Media authorization authority; no server APIs changed.
- CI exact SHA and manual browser verification separate gates; no Release claim.


## 2026-10-24 — Member avatar UX approved Variant E
- User selected Variant E and explicitly required changing avatar inside the existing Edit Member modal rather than any separate dialog. Implemented an inline EntityMedia editor mode with visible current avatar and 'Змінити' action, expanding the catalog/upload/reset controls only inside MemberEditor. Removed pencil in member list via read-only compact EntityMedia; member card click still opens existing edit dialog.
- Source commits c91f4c9 EntityMedia mode, ed6fa83 MembersPage, 69d5561 MemberEditor, CSS follow-up. Backend Media write happens on avatar choice/upload and updates shared avatar views through existing event. Member profile's Save remains independent, so a selected avatar is saved immediately; later profile Cancel does not roll back avatar. QA must explicitly validate this behavior before acceptance.
- Manual QA deferred; test modal 360px, role permissions, avatar update without closing edit, member profile save/delete, cross-workspace privacy, member add modal with no entity ID, and after cancel. No separate modal added.


## 2026-10-24 — MemberEditor staged avatar save (user-required)
- User rejected immediate Media API writes from Variant E and explicitly required change ONLY on 'Зберегти' for existing member.
- Implemented draft-only inline EntityMedia selection (catalog/clear/file), stored in parent MemberEditor; no upload/PUT/DELETE on selection, Cancel, close or Escape. Uploaded JPEG/PNG preview is temporary local object URL, revoked on change/unmount. Changes applied by MembersPage saveMember only after member form Save, and cross-screen member-avatar-changed fired only after successful Media binding. Commits 2c8be5e, e3b712b, 9bc0804.
- Limitation: Backend endpoints for member profile and Media binding are separate; the Save handler sequentially updates member profile then avatar. If profile update succeeds but Media upload/binding fails, the modal stays open and shows the error, but earlier profile changes may already be persisted. True atomic save requires Backend transaction API and is not claimed.
- Browser QA deferred: select catalog/file/clear then Cancel (no HTTP mutation); submit successful saves both; upload failure and 409 remains in editor; change family mid-dialog; image preview URL cleanup; save with no avatar draft does no Media write.


## 2026-10-24 — CROSS-TEAM HANDOFF: Global avatar catalog and visual picker (Architecture / Backend / Web / Android / QA)

**Status: PRODUCT REQUIREMENT CONFIRMED; implementation pending cross-team coordination.** User will notify other direction chats to read this log. This is a global FamilyPoints media concern, NOT Web-only. Do not interpret the current technical catalog as the final avatar bank.

### User-confirmed problems and acceptance
- Existing member-avatar editor offers a raw text select with technical labels / unhelpful names; user must see **rendered thumbnail images in a gallery**, not guess from labels.
- Current curated avatar bank appears heavily adult/female-biased; it lacks a sufficiently varied selection for **adult men, adult women, boys, girls, and gender-neutral / universal options**. Extend actual curated image assets, not just names/metadata.
- All users may select any suitable avatar; grouping and recommendations are **discovery/order only**, never a gender/age restriction. PARENT should prioritize adult imagery and CHILD should prioritize younger imagery, but all groups remain accessible.
- Avatar editing belongs **inside the existing "Редагувати учасника" (Edit Member) modal**, Variant E (avatar next to "Змінити" button). No separate avatar modal or intrusive edit pencil on member cards.
- Choosing a catalog image, selecting a file, or clearing the avatar is **draft-only**. No Media API mutation until the user presses the **main member form "Зберегти"**. Cancel/close/Escape discards all avatar changes; local file previews are temporary and revoked.
- Selected image must display consistently in Web Member cards, Points selectors/detail, account header, and corresponding Android screens after save; no fallback to initials where a valid effective image exists.

### Architecture / Planning — owner of the common decision
- Define platform-wide avatar-bank taxonomy: adult male/female, boy/girl, universal/neutral (prefer descriptive *optional* tags and free selection, avoid mandatory user gender fields).
- Define versioned global catalog asset keys, categories/tags, stable sorting, display/localization metadata, and asset provenance/licensing. Catalog is shared across workspaces; uploaded photos remain private to their own family.
- Define Save semantics and conflict/error model between member profile and media binding. Current Web uses separate Backend transactions; if the member update succeeds and image binding fails, partially committed profile is possible. Decide whether a consolidated atomic backend endpoint is necessary.

### Backend / Media API — implementation owner
- Grow curated **actual usable imagery** across the above categories (varied ages/appearances, neutral choices), ensuring vetted image provenance and previews.
- Expose metadata usable by *both* Web and Android, e.g. stable `key`, `category=AVATAR`, localized `labelKey`/display names, tags/age recommendation, `sortOrder`, `selectable`, authenticated `previewPath`, version. Extend the existing Media catalog schema without breaking V37 clients if practical. Do NOT pretend a null preview renders.
- Keep JWT authorization, strict family isolation for uploads, CAS `expectedVersion`, file type/size validations. Clarify category eligibility and per-family upload retention; assess atomic member + avatar Save or compensating design.
- Coordinate the API contract first; do not impose a Web-specific classification server-wide.

### Web — implementation owner
- Replace the current <select> with a responsive visual gallery of **image thumbnails** (selection outline/check, grouping/filtering, usable fallback when catalog empty), using JWT-authenticated preview reads and bounded/revoked Blob URLs; display no technical `media.catalog.*` keys.
- Keep the gallery *inside MemberEditor*, Variant E, with staged selections/uploads/clear only committed by the existing main Save action. Preserve permissions and ensure the currently saved avatar, draft preview and error states are understandable.
- Keep existing accepted Soft Family Dashboard art untouched. Avoid reintroducing inline file inputs on member cards.

### Android — implementation owner
- Implement the same server catalog and visual avatar gallery, grouping/recommendations, effective-image rendering across profile/member/points views and offline-safe cache isolation between accounts/workspaces.
- Stage edits until the relevant screen's Save; no immediate mutation on thumbnail tap. Follow shared backend contract rather than duplicating Web-only label maps.

### QA — verification owner (manual tests may arrive later)
- Catalog coverage by adult man/woman, boy/girl, universal; image previews visible, selectable and properly localized (UK/DE/EN/RU); all categories accessible to all users.
- Selecting an image/file/clear then Cancel must yield **zero media writes** and preserve old image on reopen/other clients; Save succeeds, propagates to Header, Members, Points and Android.
- Check mismatched permissions, stale-version HTTP 409, upload failures, image fallback, large catalog scrolling, desktop/mobile themes, switches family A↔B, logout, private photo isolation and revocation.
- Test partial member-profile success + failed avatar Save; do not mark atomicity PASS until Backend contract supports it.

### Existing groundwork / important caveat
- Existing Web Variant E, draft-only selection and Save sequence were implemented in prior commits in `MemberEditor.jsx`, `EntityMedia.jsx`, `MembersPage.jsx`; **the current text-select and limited curated assets remain unsatisfactory** and this new task explicitly supersedes that UI.
- Initial avatar catalog includes named color/avatar styles; a more balanced **real image bank** needs coordination and actual asset generation/curation.
- No release acceptance or automated/manual test PASS is claimed by this handoff.


## 2026-10-24 — Screenshot defect: profile dropdown persists after outside click
- User screenshot confirms top-right user profile dropdown remains open when clicking elsewhere. AppLayout used uncontrolled native `<details>` without outside-dismiss behavior.
- Fix in AppLayout.jsx commit 8425009: controlled profileOpen, document pointerdown listener closes only when clicked outside ref, Escape closes, route and workspace changes reset; profile toggle remains clickable, Settings link closes menu. Event listeners added only while open and cleaned on close/unmount. No profile layout or avatar changes.
- QA later: click account opens, click outside closes, click inside does not prematurely close, Escape, Settings navigation, workspace switch, mobile layout, no immediate reopening.


## 2026-10-24 — Screenshot QA fix: member avatars on Tasks page
- User screenshot: current account header correctly displays updated photo while member selector on Tasks still shows initials. Source audit found TasksPage imported the legacy presentation-only FamilyAvatar rather than authenticated MemberAvatar.
- Replaced avatar rendering for every member in Tasks member-selector with shared `MemberAvatar memberId={member.id}`. Uses existing authenticated Media V37 effective bindings, account/workspace isolation, revocable Blob URLs and `fp:member-avatar-changed` event. No Backend or layout changes. Code commit `ce70ce504584a8ac0c33f00d5fd7ff11fa638011`.
- QA deferred: Tasks tab selector displays uploaded and catalog avatars for multiple members; after member edit Save photo updates; cross-workspace swap clears old photos; missing photo renders initial; both mobile and desktop. Build/CI on exact SHA to check separately.


## 2026-10-24 — Dashboard stat-card arrow optical centering
- User screenshot shows right chevron visually off-center inside the circular indicator on home dashboard stat card.
- Kept existing 27x27 badge and its approved placement; wrapped glyph in an inner span (DashboardPage.jsx commit 00d5363) and applied scoped optical x/y translation with flex centering (App.css commit c8fbfde). No other Dashboard layout/assets modified.
- QA later: compare stat arrow centering in light/dark at desktop/mobile; confirm no regressions to stat card click targets or 480px hide behavior. CI exact SHA remains required.


## 2026-10-24 — Sidebar order: Members moved above Settings
- Per user screenshot/request, removed Members navigation item from the main/top links and inserted it into sidebar-bottom immediately before Settings. Download link remains as before; dynamic groups, Tasks, Calendar, Points, Rewards and routing unchanged. Code commit `f0d7599e33a521ee4913271108f9b156b46bebf3`.
- QA after deploy: bottom navigation order Download → Members → Settings, active state and mobile sidebar, long localized labels; build CI separately.


## 2026-10-25 — Calendar settings timezone selector
- User screenshot flagged raw editable `UTC` textbox with cramped controls. Replaced with a readable curated IANA timezone dropdown (Europe/Berlin, Europe/Kyiv, other common zones, UTC) showing city/country labels and exact zone IDs; the server-provided timezone remains selectable even if outside the curated list. Only privileged roles can change it; writes still use existing Calendar V36 timezone API on Save, no auto-setting from browser locale.
- Commits 441f969 and following CalendarPage.css scoped style. Does not modify event scheduling or Backend. QA deferred: existing UTC and Europe/Berlin, other server-returned zone retained, disabled for non-admin, long German options, both themes and mobile, DST correctness.


## 2026-10-25 — Reward catalog button lower-right alignment
- User screenshot requested 'Отримати нагороду' button at bottom-right of catalog reward card.
- Scoped catalog action container with reward-card-actions (RewardsPage.jsx commit 6ecd4c9), flex column card and bottom-pushed, right-justified action row in App.css (0f87e35). Applies to catalog reward cards only; keeps actual purchase/request/edit handlers, enabled states, and other reward panels unchanged. Test narrow responsive cards and rewards with variable description lengths.


## 2026-10-25 — Cross-team: Task categories as configurable templates, not Reward categories
- User proposes Categories subtab and category-driven task defaults including time, repetition/one-time schedule, delegation and other parameters. Screenshot is Rewards > Settings > Reward Categories; task settings must NOT be attached to reward-category DTO. Architecture handoff and source-of-truth: `docs/coordination/logs/architecture.md`, commit `0e7cd08c3cd5785fcf2e26eb570fcbc0b1ceb57c`.
- Backend prerequisite: separate workspace-scoped TaskCategory / defaults model, CRUD, policy for primary category vs multiple, precedence/snapshot semantics, compatible migration, task schedule-slot mapping and ACL. Current CreateTaskModal already handles recurrenceType, delegationAllowed and daily slot times independently, so Web should apply backend defaults as *prefill*, not force values or mutate previously created instances.
- Web follow-up when contract available: dedicated Tasks > Management > Categories subtab with template editor, task-category selection in create/edit, visible defaults and explicit user overrides. Existing Rewards > Settings > Categories may be separated into a reward-category subtab independently, but must not be conflated with task templates.
- Status BACKLOG/BLOCKED_ON_BACKEND_CONTRACT under interim BETA feature freeze. No user-visible feature implemented in this commit; do not report done or claim browser QA.


## 2026-10-25 — USER CORRECTION: Rewards category presets are the actual request

**Supersedes the interpretation in the preceding 2026-10-25 task-category handoff.** User initially mixed up tasks and rewards; intended to improve **REWARDS** category management and reward form properties. Task grouping is a possible later independent enhancement, not a current request for task template inheritance.

**Primary Web/Backend direction**: Rewards > Settings > Categories dedicated subtab; each RewardCategory can define property presets and applicable fields for reward types. Examples:
- One-time bonus: no forced schedule, redemption once via limits, perhaps no duration.
- Game/play-time reward: TIME_BASED, duration editable, optional specific availability periods; quota/cooldown if chosen.
- Repeatable standard rewards: optional cooldown, approval, access restrictions and purchase limits.
Show only relevant fields but never silently discard persisted data; apply category-provided defaults on explicit selection, preserve overrides. Reward kind, acquisition mode, availability and purchase count are separate dimensions. Existing RewardsPage uses rewardKind, defaultDurationMinutes, acquisitionMode, cooldownSeconds, quota fields, requirements and RewardAvailabilityEditor; category template metadata/validation needs Backend contract before code implementation.

**Secondary**: optional grouping of TASKS for discovery/organization; no obligation to implement inherited schedule/delegation defaults or a TaskCategory API. Architecture journal has the authoritative corrected product handoff. Manual QA and implementation remain pending.


## 2026-10-25 — Screenshot defect: member avatar Save returns "Authentication is required", native modal scrollbar
- Screenshot: user attempts member/avatar save; both Members page and Edit Member modal display the same backend error "Authentication is required", and the form has a bright native scrollbar in dark theme.
- Root HTTP 401 origin **not conclusively established** from screenshot alone: it may be member PUT or Media upload/binding. Client uses the same access token for both. Added explicit saveToken preflight and distinct errors for member-profile PUT vs Media stage so next repro identifies failing phase; Media error mentions that profile may have committed (no atomic Backend endpoint) and modal stays open. Code commit 3c2a401. Do not claim the 401 is fixed until endpoint/network response checked.
- The modal form scrollbar now has scoped theme-aware scrollbar-color and WebKit track/thumb rules instead of light native scrollbar. QA check all themes and mobile; note browser scrollbar support variations.
- QA repro: ensure logged-in session is active, open edit, select catalog image/file, Save, capture DevTools Network method/path/status (without sharing Bearer/JWT), distinguish PUT /members/{id} vs POST /media/uploads vs PUT /media/bindings. Check whether refresh resolves stale session; investigate backend auth mapping if error persists. Pending QA, no release PASS.


## 2026-10-25 — Avatar Save HTTP 401 reproduced by screenshot; remove redundant profile update
- Screenshot after diagnostic patch proves member profile PUT succeeded and avatar Media action returned "Authentication is required". Backend `RestAuthenticationEntryPoint` generates this exact text for HTTP 401; `JwtAuthenticationFilter` rejects missing/invalid/scoped JWT. No evidence of Media 403 or asset validation failure.
- Web was **always** calling PUT /members/{id} even for avatar-only edit before Media upload/binding. Changed `MembersPage.saveMember` to compare name, memberType and workspaceRoleId against the original and skip profile PUT when unchanged (commit bd9739c). This eliminates an unnecessary mutation/token-side effect for avatar-only Save. Media operations still occur only on primary Save.
- This is a plausible cause elimination, **not proof that HTTP 401 is resolved**. Need retest and, if still failing, inspect Network method+URL+status of 401 (without token) and Backend warning `JWT rejected for <METHOD> <URI>` using sanitized reason. If any member profile edit mutates account's selected workspace/session/JWT identity, Backend must define token refresh or consolidated atomic member+avatar mutation endpoint. Changing profile plus avatar remains non-atomic; no automatic fake PASS.
- QA: avatar-only catalog Save should skip PUT /members; upload-only should skip profile PUT; profile-only should PUT; changed role+avatar tests stale token case; Cancel no mutation. No production release authorization.


## 2026-10-25 — Second scrollbar screenshot / unresolved Media JWT 401
- User screenshot after prior changes still shows a pale/unstyled scrollbar along the right side of the Edit Member form. Earlier scoped scrollbar used CSS vars that were visually too similar/default on Chromium. Updated explicit thumb/track styling on **actual scrolling form** plus fallback modal/backdrop, with theme-aware `scrollbar-color` and WebKit scrollbar pseudo-elements and color-scheme for dark/light (commit f9916258). No form geometry or profile behaviors changed.
- **Media 401 remains unresolved** on user's localhost:5173; screenshot alone does not reveal failing endpoint. Previous source analysis confirms Media API PUT/upload includes bearer token, while Backend authentication entry point emits this specific HTTP 401 when request is unauthenticated. Do not claim authorization fixed or bypass JWT. Needed next: DevTools Network failing Media method/path/status, backend `JWT rejected for ...` warning without sharing credentials, check scoped JWT expiry and browser origin/proxy. All manual QA pending.


## 2026-10-25 — Avatar media upload size-sensitive failure reproduced and mitigated
- User reports original 1254×1254 transparent PNG (~2.08 MB) failed with "Authentication is required", but optimized 768×768 transparent PNG (~701 KB) uploaded and saved successfully. Small 256×256 PNG (~84 KB) also succeeded. Thus **file size/encoding sensitivity is confirmed in user observations, but specific reverse-proxy failure code is NOT yet captured**; do not assert JWT is definitively broken or definitively healthy.
- Added browser-side `optimizeMediaUpload` for multipart JPEG/PNG larger than 850 KiB: resize longest side down through 768/640/512/384/320 until encoded PNG (alpha preserved) or JPEG fits below conservative 850 KiB target; transparent controls and user file untouched, small files pass through. Connected centrally to `frontend/src/api/mediaApi.js` so both member/cover/media upload flows use it. Commits f5a32f0, 3acef75, 61f1906.
- Backend/Release follow-up: check Nginx `client_max_body_size` at each Web/API ingress; HTTP 413 or routed authentication responses must be investigated against sanctioned 5 MiB backend upload max, keeping authentication intact. Do not recommend bypassing auth. Note optimizing for avatars is pragmatic, but full-resolution family covers may need separate upload policy so 2048px COVER remains sharp.
- QA later: original failing PNG now saves and alpha remains; file below threshold byte-for-byte unchanged; invalid image and >5MiB rejected; test crop/appearance and mobile performance. CI build and deployed SHA still pending.


## 2026-10-25 — Actionable image-upload errors instead of misleading generic responses
- User confirmed optimized PNG uploaded while original larger PNG failed. Requested an appropriate user-visible error for oversized / unprocessable uploads.
- Media upload client maps verified HTTP **413** to a dedicated "Зображення завелике для сервера" message and network transport failures to a connection message (commit 09105fda). Image preprocessing now emits user-readable errors for unsupported file, over 5 MiB, failed PNG/JPEG decode, missing canvas and optimization unable to meet size target (commit 7f62c25).
- 401 remains an authentication error unless server provides reliable body-size diagnostics; **do not misrepresent every 401 as oversized upload**. Backend/Release should align proxy request-body limit with supported image upload size and return HTTP 413 instead of a misleading auth error for rejected oversized payloads. Web-only fix cannot guarantee intermediary status mapping. QA try original oversized PNG, optimized PNG, >5 MiB and wrong MIME, simulated HTTP 413, network failure, invalid session.


## 2026-10-25 — Reward receive CTA wrong card family corrected
- User screenshot confirmed 'Отримати нагороду' remained at bottom-LEFT. Earlier change (catalog reward-card-actions) targeted the wrong card family. Screenshot is Rewards > Requests: `reward-request-card` with status 'Можна отримати', member name and 'Ціна' field.
- Fixed the actual `READY_TO_PURCHASE` request receive action: added `reward-request-claim` in RewardsPage.jsx (b135a91); set request-card flex column and that button `align-self:flex-end; margin-top:auto; margin-left:auto` scoped to request list in App.css (6cba946). Existing request authorization/purchase handlers and other buttons unchanged. Previous catalog rule remains.
- Manual QA later: Requests/READY_TO_PURCHASE with one claim, long description, variable heights and mobile width; compare Catalog separately. Build CI and deployed SHA not yet verified. 


## 2026-10-25 — User screenshot fix: real visual avatar picker in MemberEditor
- User screenshot reconfirms member edit dialog still has a native textual avatar select. Replaced specifically **MEMBER + inlineEditor** select with a responsive, accessible thumbnail grid backed by authenticated per-item `previewPath` (no unauthed direct `img` requests). Tile URLs are Blob-based and revoked on unmount/workspace changes; selection outlined with check and aria-pressed. Source: AvatarCatalogTile.jsx 22c5766, EntityMedia.jsx 2628d5a, EntityMedia.css 1c69c7d.
- This works with currently renderable catalog images. A wider male/female/child/neutral bank and category filters still depend on global Backend+Designer catalog completion; do not claim that bank already exists.
- Crucial existing Save-only draft behavior retained: clicking tiles changes only `avatarDraft`, no binding mutation; pressing MemberEditor main Save commits; Cancel discards. Upload/clear controls still available inside same modal. Non-member Media controls retain existing select.
- Manual QA deferred: previews for all catalog keys actually display in browser, selected tile borders, dark/light, desktop/mobile, locale, keyboard, family isolation, network auth, Save/Cancel; no visual E2E PASS claimed.


## 2026-10-25 — V38 avatar subfolders do not automatically appear in Media API; Web category tabs
- User screenshot from another track reports PNG folders such as `animals/`, `monsters/`, `players/` committed but missing in Web avatar selector. Independently inspected current main: `src/main/resources/media/catalog/` has many PNGs in nested folders, whereas `MediaService.java` registers a hardcoded `V38_AVATARS` set of 30 and explicitly constructs those 30 `CatalogItem` entries. Only registered keys are bindable/renderable via `catalogPreview`. Do not assume new assets appear in the API because they exist in resources.
- Web implemented *actual* category filter chips above authenticated avatar image grid, driven by server catalog `tags` and only shows groups present in the response (ALL, HUMAN, ANIMAL, PLAYER, MONSTER, ROBOT, VEHICLE); four-language labels and aria-pressed. Source commits `55142e2`, `8c6eaa9`. No new ad hoc image URLs, undocumented keys, or direct unauthenticated resources.
- **Backend follow-up P1**: safely discover approved PNG files under catalog resource tree or maintain generated reviewed registry, including nested folders; ensure stable case-sensitive keys, no collisions/unsafe paths, category tags (HUMAN/ANIMAL/PLAYER/MONSTER etc.), IANA-independent sortOrder, label keys, previews as image/png, binding allowlist matches discovery; honor existing key compatibility and security authorization. `MediaController.catalogPreview` declares produces image/svg+xml even for PNG V38 — validate and correct returned content type. Add exhaustive manifest vs resource integrity tests (no silent omissions, duplicates, or missing binaries); user requested new categories automatically follow folder organization rather than manual registration.
- Web category tabs are ready for new catalog data, but the full bank is still **BLOCKED_ON_BACKEND**; Android same contract. Manual QA and final CI/deployed SHA pending; no claims of screenshot match.


## 2026-10-25 — Nested catalog avatars now registered in Backend source
- User confirmed images under `src/main/resources/media/catalog/animals`, `friendly_mobs`, `monsters`, `players` are missing from Web gallery. Source tree shows 101 PNGs total (41 root, 17 animals, 4 friendly_mobs, 27 monsters, 12 players). Existing V38 registry covered only 30.
- Updated Backend MediaService to discover additional PNG resources under nested classpath media/catalog, assign stable basename keys, tags matching folder, preview paths, and list in same catalog endpoint; existing 30 keys retained. Catalog bind allowlist now includes discovered keys, and controller sets PNG content type for PNG responses. Web added FRIENDLY_MOB category chip to existing tag-based visual picker. Commits e843e63, a588034, 9853a20, f284239.
- CAUTION: compile/test and end-to-end deployment of latest SHA still not verified; runtime classpath resolver's duplicate/packaging behavior and filename collisions must be tested, along with workspace isolation, preview 200 image/png, binding 200, and no missing files. This update is source-only and needs Backend/QA review before Release.


## 2026-10-25 — UPDATED Voxel artwork not visible on test website (Web + Backend + Release)
- User reports corrected 60 voxel PNGs committed/pushed to main, original same file paths/keys; actual commit `880869c711da763eb89ee5d575783f8765810ee4` verified via GitHub. DO NOT regenerate assets or rename catalog keys.
- Confirmed Web fetches authenticated Media API catalog preview via `AvatarCatalogTile -> fetchAuthorizedMediaObjectUrl -> apiRequest` and Blob URLs, not direct static GitHub assets. Backend `MediaController.catalogPreview` sent `Cache-Control: private, max-age=86400`, permitting same URL to remain stale for 24 hours.
- Implemented optional `cache` fetch option in `frontend/src/api/client.js` (85dafa7) and enforced `cache: 'no-store'` for authenticated media image reads in mediaApi.js (2217355). Backend MediaController PNG/SVG catalog responses changed to `private, no-store` (37c8631). Existing JWT/workspace boundaries unaffected.
- **Release/DevOps explicit task:** verify running test Backend image/container SHA includes original PNG replacement `880869c` and the no-store fix; rebuild/redeploy Backend as authorized (new files reside in Java resources and are served by running JVM, Web deploy alone cannot update old bytes). Restart relevant Backend containers/process according to deployment process; avoid unrelated environments. Then test a representative PNG against new artifact under authenticated catalog preview, compare SHA-256 bytes with checked-in file (e.g. animals/voxel_28_fox.png), verify Content-Type image/png and Cache-Control private,no-store. Test Web after page reload and browser Network Disable cache. Compare if old bytes still served: deployment/image drift or intermediary cache/CDN; if bytes new but UI stale: SPA stale loaded Blob/page state. Do not assert deployment performed.
- QA acceptance: all 60 corrected avatar images map to same keys, no old crop fragments; both new tab and existing session after reload; Firefox/Chrome; workspace isolation, offline handling, old bindings work. CI current SHA and manual visual check not yet passed.


## 2026-10-25 — Media member-avatar catalog selection preview / CI latest
- Verified GitHub Actions run 40149287002 at SHA 213892eb: Java/Postgres, Web Vite, Android Gradle and BETA Compose all SUCCESS. This is CI on that SHA, not runtime QA/deployed proof.
- Fixed MemberEditor/EntityMedia UX issue: selecting a server-catalog avatar previously only highlighted the tile while existing large member avatar preview stayed unchanged before Save. On staged catalog selection, fetch selected authorized preview, show it immediately in existing edit form and revoke temporary Blob URL on change, close and workspace switch. Upload draft preview and clear behavior remain; server binding only mutated by main Save (commit 5e44946). No new artwork/keys.
- QA manual acceptance: choose 2 catalog tiles and see large preview change immediately; Cancel leaves saved avatar untouched; Save persists; rapid switches/workspace swap cannot leak old image; dark/light/mobile; expired session graceful fallback. Latest patch needs fresh CI on exact SHA.


## 2026-10-25 — Rewards purchase stabilization: mutually exclusive claim paths
- Source review `RewardsPage.jsx` shows `purchaseOnce` already uses synchronous per-reward `purchaseLocks.current` Set; no need to implement duplicate guard again.
- Found actionable overlap in Rewards Catalog rendering: for a reward with a currently pending/approved linked request, both `READY_TO_PURCHASE` request-claim button and generic direct-purchase button could render when reward definition is DIRECT/no-approval. Two distinct user pathways for the same reward make UX ambiguous; parallel operation keys coincide but do not resolve wrong purchase type. Source fix `90aa3a6`: direct purchase CTA shown only when `myRequestForReward(reward.id)` is absent. Existing request-claim/action handlers, backend ledger and permissions untouched.
- QA scenarios: direct reward without request shows one Obtain button; READY_TO_PURCHASE request shows exactly one Obtain action that purchases *the approved request*; REQUESTED/PENDING request does not offer direct bypass; double click hits one endpoint; ledger debits once; re-render after refresh has no duplicate. Test adult/child accounts and insufficient funds. No manual E2E PASS/deployment claimed. Latest previous full CI success run 40149287002 on 213892eb; new fix requires exact SHA build verification.

## 2026-10-25 — Cross-track directive: EARLY BETA publication priority
**User decision (Architecture):** BETA is the place for testing. Prioritize rapid publication of the existing working feature set. Software implementation readiness includes Backend/Web/Android only; QA and Release are tracked as separate gates, not deducted from implementation percentage. Full manual E2E/regression, minor UX and catalog completeness, synthetic-data preservation rehearsal are NOT publication blockers. Keep minimum safety: green pinned CI, app/server starts, authenticated login/core route smoke, no known cross-workspace privacy leak, repeat points debit or cross-account offline replay, valid signed APK and basic install/launch, minimal DB snapshot before risky migrations. Existing test data may be reset eventually, but **no immediate deletion is authorized**. Templates/reset UI/history correction remain after this interim BETA. Decision record: `docs/coordination/logs/release.md` (2026-10-25); prior green SHA `77b183c98a6`, run `40177131970`.

**WEB action:** Freeze new UX/features for this early BETA. Fix only broken login/navigation, critical purchase paths, severe Media data/privacy issues or deploy blockers. Minor UI, gallery completeness, polish and broad visual QA can be tested after publication.
