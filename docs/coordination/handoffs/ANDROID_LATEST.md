# Android rolling handoff — 2026-11-21, STEP66

Track: FamilyPoints Android (new continuation chat). Repository: `o-kobylianskyi/family-points`.
Baseline main SHA: `df1ff9bdb897ea2e7acdc59f2f849f4087369150`.
Isolated source-only branch: `android/reward-receipt-policy-step66`.

## Observed source
- `OfflinePointsQueueStore` already quarantines pending point operations on workspace epoch change, keeps original UUID, blocks enqueue/replay while held. `PointsRepository.synchronizePending` verifies account/member/workspace and epoch before replay; this was a code audit, NOT execution proof.
- Current `RewardsApi` and `RewardsRepository` still expose legacy non-idempotent purchase routes; do not wire them to new reward-selection UX. No Android purchase receipt journal is currently connected.
- Architect STEP66 in Issue #15 comment 7069359182: Android must use a persisted per-intent UUID, online-only purchase, receipt GET recovery, no automatic uncertain POST retry. Backend PR57 V54 remains DRAFT/unmerged and financial PG proof is BLOCKED; Web PR54 is separate.

## Implemented on this isolated branch (NOT integrated)
- `RewardPurchaseReceiptPolicy.kt`: pure fail-closed identity/scope/epoch/price/type guard, never retries purchase.
- `RewardPurchaseReceiptPolicyTest.kt`: 5 JUnit methods for exact receipt, zero price, mismatch, scope/epoch drift, malformed values and no automatic POST.
- No networking, UI, persistent journal, purchase mutation or deployment performed.
- CI and local Gradle were NOT run; test PASS is NOT claimed.

## Next Android package
1. After Backend PR57 wire DTO and real PostgreSQL ledger/rollback acceptance, implement durable encrypted/tenant-scoped intent journal with stable operation UUID across restart. Offline reward purchase is prohibited.
2. Add isolated API GET receipt and idempotent POST endpoints without enabling unsafe legacy/replay behavior; honor current actor/selected workspace/epoch guard. Error 401/403/404/409 and I/O uncertainty retain original intent and HOLD.
3. Integrate parent/member reward selection, currency, authenticated permission, four locales and role-specific UI with explicit user action only. Distinguish reward purchase from manual points spend.
4. Add HTTP fixture/unit/instrumented tests, then QA independent validation and exact-head Actions checks. Device/APK has NOT been verified here.

## Critical holds
BETA HOLD; no merge/tag/deploy, no reset/restore execute, no family DB changes, no automatic repeated POST. Backend PR57 fails exact-head Java CI on legacy migration-count assertions per Issue #15 STEP66; ledger PG test mocked. Only synthetic source-level draft work authorized.

## Chat capacity observation
2026-11-21; new Android chat generation; exact total turns/characters unknown, partial visible coverage; no trustworthy context-usage percentage; no hard-limit warning observed.

## Restart prompt
"Приймай напрям FamilyPoints Android. Прочитай AGENTS.md, docs/coordination/handoffs/ANDROID_LATEST.md, останню архітектурну STEP66 директиву в Issue #15 і current main. Продовжуй Draft Android reward receipt candidate, перевір PR57 backend contract/CI; no merge/deploy."

## STEP67 continuation — 2026-11-21
- Architect STEP67: Issue #15 comment 7069421907 supersedes prior provisional PR57 status. Backend PR57 exact SHA 02e1d422 failed Java CI (770 tests, 10 failures migration count fixtures; real point ledger acceptance still unproven). Clients may prepare read-only UX but no financial mutation.
- New unintegrated code in Draft PR58:
  - `network/RewardPurchaseReceiptApi.kt`: typed read-only GET receipt endpoint and response DTO matching PR57 draft wire envelope; intentionally not registered in ApiClient or called by UI.
  - `data/RewardPurchaseRecoveryState.kt`: validates nested authoritative purchase by exact UUID, reward/member, currency type, workspace, account and epoch; 200 alone remains CHECKING, 401/403/404/409/5xx remain NEEDS_REVIEW; NEVER auto POST or silently clear original uncertain intent.
  - `i18n/RewardPurchaseRecoveryStrings.kt`: UX review/check labels in UK, RU, DE, EN, intentionally not wired to purchasing buttons.
  - `RewardPurchaseRecoveryStateTest.kt` and `RewardPurchaseRecoveryStringsTest.kt`: 5 new source test methods, plus prior policy tests (total 10 authored across three classes).
- Source-only: no Gradle/CI or instrumented/device execution performed in this batch; no claim of PASS, no APK, no deployment.
- PR58 intentionally remains DRAFT; no runtime journal, no POST wiring, no UI integration, no adapter active. Before acceptance: Kotlin build/test and independent QA, PR57 real financial PG + gate, actor epoch and original-UUID persistence across restart, receipt 404/409 handling and user-visible held review, manual spend vs reward purchase separation.
- Chat capacity: current conversation partial available; no measurable context %; no hard-cap warning.

## STEP68 continuation — 2026-11-21
- Architect Issue #15 comment 7069819439: PR58 original source-only policy exact earlier HEAD 9d7adde4 CI had Android Gradle BUILD SUCCESSFUL, but QA found nonblank status acceptance unsafe. Backend PR57 later HEAD c972c5feb9789939053452ef726e137e98361e7c still failed one `WorkspaceSetupPostgresTest` assertion (772 tests / 1F), therefore financial APIs are NOT authorized for runtime.
- Fixed `RewardPurchaseReceiptPolicy.kt`: only canonical lower-case UUID and Backend-purchased state `READY` accepted; CANCELLED/FAILED/unknown refused. Added tests for these negatives.
- Added `RewardPurchaseIntentStore.kt`: dedicated identity-only local SQLite journal (`fp_reward_purchase_intents.db`) with immutable UUID, account/workspace/member/reward/epoch/SELF|MANAGER, PREPARED/UNCERTAIN/HELD/CONFIRMED. Duplicate unresolved same reward/actor refused; original intent preserved across restart; scoped reads; mismatch/epoch drift quarantines; cannot reopen HELD or CONFIRMED; no network, no queue, no delete, no retry. CONFIRMED requires actual validated immutable receipt + matching verified scope, not a boolean.
- Added `RewardPurchaseIntentSqliteTest.kt` instrumented tests: reopen persistence, account/family scoping, UUID immutability, epoch quarantine, uncertain retention, verified-only receipt confirmation, foreign-account denied.
- All new changes are Draft PR58 only, no main merge and no runtime integration. Previous code-only receipt API remains unregistered, read-only UX strings not wired. No POST, no owner DB/device writes.
- **Tests in THIS STEP68 batch: authored, NOT EXECUTED.** Do not inherit old PR58 CI green (that attested previous head). Need exact new-head Android JVM & instrumentation compile/execution CI, named reports, and QA; UI/online flow remains prohibited pending PR57 financial PG acceptance.
- New chat capacity: total available context % unknown; no trustworthy count.

## STEP69 CI verification — 2026-11-22
- **Exact Android Draft PR58 source HEAD `7420e8cc0de0a07261b9e28c4c54288acf81545e`**, [automatic Actions #44954229020](https://github.com/o-kobylianskyi/family-points/actions/runs/44954229020) completed SUCCESS. Android job `136998705018`: actual Gradle commands `:app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest` executed and BUILD SUCCESSFUL, 76 tasks executed. Android JVM unit tests passed at task level; debug and AndroidTest APK **assembled**. Test APK was **not executed** on emulator/device; no instrumentation PASS or test count from raw log. CI Android, router, per-commit passed, Java/Web/Compose heavy steps SKIPPED by path routing, and Full exact-SHA attestation SKIPPED. Artifact `android-test-reports` id `14727211519` contains test reports (named XML not independently parsed in this batch). No repeat CI launched.
- **Backend Draft PR57 independent context:** source `084f4df182015e21421fb2e0784164f0cbe414ed`, [automatic Actions #44954121427](https://github.com/o-kobylianskyi/family-points/actions/runs/44954121427) completed 7/7 SUCCESS, actual Maven 772/772, zero fails/errors/skips; this is narrow source/CI, NOT independent financial QA or V53+V54 integrated acceptance.
- **No new Android source changed in STEP69.** PR58 remains DRAFT/unmerged, runtime receipt GET not registered and purchase POST not wired; synthetic SQLite/device test execution pending. Retain safety holds: BETA HOLD, no real family write, no merge/deploy/restore/reset.
- Next safe Android acceptance: independent QA inspect named JVM XML and run SQLite `RewardPurchaseIntentSqliteTest` on controlled Android emulator/device; prove persisted UUID, canonical status allowlist and epoch drift. Then read-only intent review UX, **without** POST or any generic spend; after Backend financial QA/Architecture, separately authorize runtime integration.
- Capacity: no reliable chat token percentage; current discussion continuation handoff committed regularly.

## STEP70 — 2026-11-22, source-only read-only reward journal UI
- Architect latest STEP69 Issue #15 #7084116039: read-only parity allowed, no financial POST or runtime purchase enablement; Backend must determine READY->REDEEMED/CANCELLED receipt interpretation. QA #7084050011 confirmed Android SQLite instrumented tests are authored but **NOT_EXECUTED**, and noted Web READY+REDEEMED vs Android READY-only parity question.
- Added on existing PR58 branch:
  - `ui/screens/RewardPurchaseIntentReviewModel.kt`: verified account/workspace-scoped projection of identity-only journal; no cross-account records; no automatic purchase actions.
  - `ui/screens/RewardPurchaseIntentReviewSection.kt`: standalone non-navigated Compose read-only section, localized UK/RU/DE/EN, displays local state/reward/member/UUID; no button, callback, API call or financial mutation. **Not wired** into active RewardsScreen until separate QA/architecture decision.
  - `test/.../RewardPurchaseIntentReviewModelTest.kt`: 3 authored JVM tests for account/tenant scoping, invalid state and disabled financial actions.
- **This STEP70 source change is not tested yet:** latest new-head CI must be checked; no device/emulator available through this GitHub-only channel, therefore SQLite `RewardPurchaseIntentSqliteTest` remains NOT_RUN on device. APK compiled on former HEAD 7420e8cc; do not transfer that verdict to new source SHA.
- Backend PR57 current previous accepted SHA 084f4df source green 772; newer c165bed source had in-flight run at STEP69 and Architect subsequently reports 775 PG tests 0F; this still needs QA and canonical lifecycle decision.
- Strict holds: no merge, no deploy, no host/real-family data, no purchase POST, no generic spend workaround; BETA HOLD.
- Next: verify automatic CI for exact STEP70 source; have QA run actual connectedDebugAndroidTest on explicitly approved disposable emulator and report seven named SQLite cases. After backend status decision align Android/Web receipt interpretation; keep purchase UI inactive until financially accepted.

## STEP71 — 2026-11-22, canonical receipt status parity
- **Authority:** Architect STEP70 Issue #15 comment 7084189621, Backend B lifecycle source comment 7084156883. Durable receipt GET dereferences original purchase ID and returns current status; a READY purchase can later become REDEEMED or CANCELLED, while historical debit may exist even after cancellation.
- Updated `RewardPurchaseReceiptPolicy.kt` allowlist: READY and REDEEMED pass only full original canonical UUID/account/workspace/member/reward/epoch/currency/price/purchase ID matching; CANCELLED, PENDING_APPROVAL, FAILED and unknown cannot become positive confirmed purchase.
- `RewardPurchaseRecoveryState.kt` now distinguishes CONFIRMED_PURCHASE, CONFIRMED_REDEEMED, CANCELLED_HISTORY_HOLD, NEEDS_REVIEW. Historical cancellation is classified ONLY if full original identity matches under synthetic READY status; otherwise remains NEEDS_REVIEW. No automatic POST or clearing uncertain intent.
- `RewardPurchaseRecoveryStrings.kt` and isolated non-navigated `RewardPurchaseIntentReviewSection.kt` include distinct redeemed/cancelled historical UX in UK/RU/DE/EN, optional verified receipt-state projection; still **no active RewardsScreen wiring**.
- Updated JVM tests `RewardPurchaseReceiptPolicyTest`, `RewardPurchaseRecoveryStateTest`, `RewardPurchaseRecoveryStringsTest` with READY/REDEEMED acceptance, historical-cancelled matching/foreign mismatch, PENDING_APPROVAL/FAILED/invalid rejection, canonical UUID and no auto retry.
- **New STEP71 exact-source CI was IN_PROGRESS at first check**, do NOT infer green from old run. No new runtime API, POST, live family change, merge, deploy or manual CI rerun. Android SQLite instrumented tests remain NOT_EXECUTED on emulator/device; source + AndroidTest APK compilation is not execution.
- Backend PR57 also advanced to SHA 96bdfe09c55ba07d5f090ccd09f83a68e6b9713a for lifecycle SQL-backed test; QA must check new exact-head before future active client financial approval.
- Next: inspect newest exact PR58 CI Android test task+build and named test XML evidence, independent QA source review, controlled disposable device/emulator for SQLite instrumentation with explicit approval. Maintain BETA HOLD; no financial writes.

## GLOBAL PACKAGE C — consolidated Android parity candidate (2026-11-22)
Authority: Architect global execution directive #7085429894 (large coherent packages; no micro-step reports); Architect receipt lifecycle #7084189621.

### Actual source package on Draft PR58, main unchanged
- **Live Android PointsScreen:** existing `spend` selector previously called legacy non-idempotent `RewardsRepository.purchaseForMember` for a reward selection. Removed that actual call; selected reward now shows explicit financial QA hold and cannot submit; **manual points spend remains independently available**. `RewardSpendSelectionPolicy` filters direct rewards by MANAGE_REWARDS/ADMIN_OVERRIDE, online availability, active reward, active matching point currency/code, nonnegative price, no approval/request-only; invalid/stale selection cannot fall back to manual spend. JVM tests for mode/role/offline/currency/amount.
- **Live RewardsScreen:** direct reward and approved-request legacy financial purchase callbacks no longer invoke `purchaseReward` or `purchaseRequest` while backend financial QA is incomplete; show explicit localized hold rather than claiming success. Nonfinancial catalog viewing and requests remain unchanged. Legacy repository methods remain for compatibility but are not invoked by these two screens.
- **Journal:** `RewardPurchaseIntentStore` SQLite holds immutable operation UUID/account/workspace/member/reward/epoch/route/status across restart; duplicate unresolved same reward/member blocked; scope-only read. Updated `quarantineEpoch(accountId,workspaceId,epoch)` to prevent touching other accounts in same family, with eighth instrumented cross-account test. Same UUID cannot silently be replaced/reposted. No journal deletion on GET errors or cancellation.
- **Read-only UI:** `PointsScreen` displays `RewardPurchaseIntentReviewSection` for locally existing records of only authenticated account + family, fail-closed for null account. Statuses READY purchased/not fulfilled, REDEEMED previously purchased/used, CANCELLED historical debit unknown; `RewardPurchaseRecoveryState` and four locale text remain. Offline data is clearly local.
- **GET-only candidate:** `RewardPurchaseReadOnlyRepository` validates authenticated access token identity before/after GET, verifies workspace dataEpoch before/after, uses only `RewardPurchaseReceiptApi.GET`, maps 401/403/404/409/network to NEEDS_REVIEW and never updates/clears local uncertain journal. `ApiClient` registers read-only transport; `LIVE_RECEIPT_UI_ENABLED=false`. No active network receipt button yet; code candidate.
- **Future POST draft only:** `RewardPurchaseIdempotentDraftApi` declares PR57 scoped POST /purchase/idempotent + /purchase-for-member/idempotent with immutable Idempotency-Key/epoch, but is **NOT registered in ApiClient and NOT callable by live UI**. `RewardPurchaseIntentPlanner` validates member/manager permission, reward mode, currency, price, epoch, online; `FINANCIAL_POST_GATE_OPEN=false`, no POST execution. Manual debit is never used as a reward purchase substitute.

### CI and QA scope
- Last **prior** PR58 STEP71 pre-package head `e206bc663d70e053eb1881d81833f5d542b44dcc`: automatic GitHub Actions #45063846580 SUCCESS (Android JVM test task and APK compile). This verdict DOES NOT apply to new source changes.
- During initial package CI several temporary per-commit builds failed: missing `ApiClient.rewardPurchaseReceiptApi` at source `0a176777...` (fixed on `dab8aec...`), and nullable `CurrentUserResponse.accountId` in `PointsScreen` at `ef325310...` (fixed on `3f96025...`). Original failure logs reviewed, do not claim latest PASS until exact SHA.
- New package JVM test classes: `RewardSpendSelectionPolicyTest`, `RewardPurchaseIntentPlannerTest`, `RewardPurchaseReadOnlyRepositoryPolicyTest`, plus prior receipt policy/recovery/locales/projection. New `RewardPurchaseIntentSqliteTest` instrumented test added cross-account epoch quarantine assertion.
- **Device runtime:** `connectedDebugAndroidTest` never run through existing CI, no emulator/physical device access explicitly authorized; eight SQLite instrumentation cases remain NOT_RUN. Compiling AndroidTest APK is NOT a device test PASS.
- Backend PR57 has proven scoped real PG concurrency/one debit and lifecycle on prior exact SHA, but cross-client authenticated HTTP/tenant/role/Flyway V53→V54 combined release gate is still not accepted. **BETA HOLD**, PR58 Draft/unmerged, no deployment, no restore/reset/purge or real family financial write.

### Acceptance path and next independent package
1. Inspect CI on **final source commit** (after last `RewardSpendSelectionPolicyTest` change, SHA `ddd078838b3b2c484c5df9cde76f831215d0a69b`, then null-account fix `3f96025...`; later edits may supersede). Require Android :app:testDebugUnitTest, :app:assembleDebug and :app:assembleDebugAndroidTest exact SHA evidence; review named tests in report artifact if accessible.
2. Independent QA on approved disposable Android emulator/device: run and record eight `RewardPurchaseIntentSqliteTest` tests, account-switch/restart/epoch drift and no wallet mutation; UI currency/role/offline/manual spend regression.
3. Only after Backend PR57 real HTTP security + finance receipt QA, combined V53/V54 test, and Architect explicit purchase activation decision: separately enable idempotent online POST, durable UUID before first send and read-only receipt reconcile; NEVER automatic uncertain replay.

### GLOBAL PACKAGE C — final source CI evidence / QA handoff (2026-11-22)
- **EXACT Android source SHA `ddd078838b3b2c484c5df9cde76f831215d0a69b`**, [automatic GitHub Actions #45068122783](https://github.com/o-kobylianskyi/family-points/actions/runs/45068122783) **COMPLETED SUCCESS**. Router, Android, Web/Java/Compose scoped jobs, per-commit all SUCCESS; Full exact-SHA attestation SKIPPED because path-scoped Android-only change (not a full four-component PASS).
- Android job `137348811206` raw log: `:app:compileDebugKotlin`, `:app:assembleDebugAndroidTest`, `:app:testDebugUnitTest`, `:app:assembleDebug` all execute, **BUILD SUCCESSFUL in 1m 35s**. Uploaded Android test report artifact **14772187560**. No named JUnit XML test counts extracted in this check. AndroidTest APK only assembled, NOT run on emulator/device.
- Earlier temporary CI failures on intermediate commits were resolved by the final source SHA (missing GET transport getter and nullable account ID). No manual CI dispatched.
- **Disposition:** `ANDROID_GLOBAL_PACKAGE_C_SOURCE_READY_FOR_QA / DEVICE_SQLITE_NOT_RUN / PURCHASE_POST_HOLD`. Source tests/build proven at task level; independent QA still required for manual spend vs reward selection in both themes/four locales, synthetic lost-response GET, account/epoch/restart, eight device SQLite instrumentation tests. No production/host/real-family install, no merge or deployment.

## NEXT INDEPENDENT PACKAGE — READ-ONLY LOST-RESPONSE/SESSION QA (2026-11-22)
- Authority: Architect global directive #7085429894 and global execution follow-up #7085663784: deliver substantial source/test packages, only meaningful QA handoffs. No financial POST activation or production/owner device.
- Draft PR58 HEAD source checkpoint `130d9d65190907f0064ca444f68082e0598e1910` includes:
  - New `RewardReceiptReadOnlyReconciler.kt`: injected GET-only dataEpoch/receipt GET transport, immutable persisted UUID, exact account/workspace scope, token checked before/after each read, generation checked before/after receipt. On status 401/403/404/409/5xx/network, scope drift, token rotation, forged receipt: NEEDS_REVIEW, original journal retained, NEVER auto POST, no local updates.
  - Existing `RewardPurchaseReadOnlyRepository` refactored to delegate to the same injectable reconciler with real ApiClient GET endpoints; live UI gate still OFF.
  - `RewardReceiptReadOnlyReconcilerTest.kt`: six authored source JVM tests covering exact original UUID and exactly two epoch GETs+one receipt GET, 401/403/404/409/500 and lost response, missing/rotated token, account/workspace/epoch mismatch, READY/REDEEMED/CANCELLED and rejected statuses, foreign/forged receipt and confirmed-intent no-op. No financial writes.
  - `RewardReceiptTransportMockWebServerTest.kt`: two authored local HTTP transport regression tests exercising real Retrofit interface with MockWebServer; verify only GET (original UUID, Authorization, epoch before/after) and 404 never leads to POST or retry.
- **Current exact-source automatic Actions** [#45070445707](https://github.com/o-kobylianskyi/family-points/actions/runs/45070445707) for source SHA `130d9d6519...`: Android Gradle IN_PROGRESS at first inspection; do not inherit prior green. Earlier PR58 SHA `0263ca87503d2c1715b20486709e9782b0671418` [#45068396269] completed success for prior source only.
- **Distinct remaining QA gates**: durable original intent stores UUID+actor/family/member/reward/epoch but not immutable currency/price snapshot; backend is currently authoritative for confirmed currency/price. Before enabling any idempotent POST, prove original fingerprint/selection financial snapshot and mismatch UX, actor/role/epoch, exact authenticated lost-response on disposable systems. No on-device SQLite test runner approved, eight Android instrumented SQLite cases NOT_RUN; `:app:assembleDebugAndroidTest` only compiles.
- Disposition pending exact-head CI: `ANDROID_READONLY_LOST_RESPONSE_SOURCE_CANDIDATE / FINANCIAL_POST_HOLD / DEVICE_NOT_RUN`. Main unchanged, PR58 Draft/unmerged, BETA HOLD.

### FINANCIAL SNAPSHOT HARDENING — SQLite v2 / fail-closed legacy rows
- Audit identified that old reward intent v1 stored only UUID, actor/workspace/member/reward/epoch but no immutable point type or price. That was insufficient to independently compare a historical receipt to what the user selected before a purchase.
- Source updated on same Draft PR58: `RewardPurchaseIntentStore` database version 2 with additive `expected_point_type_id`, `expected_point_type_code`, `expected_price_amount` columns; `onUpgrade(1,2)` **ALTER TABLE only, NO DROP/CLEAR**; previously persisted v1 rows keep NULL snapshot and original UUID, remain visible but cannot be positively confirmed. Every **new** `prepare` requires explicit positive point type ID, nonblank code and nonnegative original price, stored transactionally with UUID. `RewardPurchaseIntentPlanner` now sets these fields.
- `RewardPurchaseReceiptPolicy.evaluate` demands exact original persisted pointTypeId, code and price match, in addition to UUID/actor/workspace/member/reward/epoch/status; no retroactive price inference from changed catalog. `RewardPurchaseReadOnlyRepository` and `RewardReceiptReadOnlyReconciler` thread the immutable snapshot through.
- Added negative JVM mismatched price/type/absent snapshot tests, persistence test, and new Android instrumented `legacyV1RowsSurviveUpgradeButCannotClaimVerifiedFinancialSnapshot` (v1→v2 data intact, no accidental financial confirmation), as well as planner snapshot test. Existing instrumented suite now has ten authored cases; **on-device execution remains NOT_RUN**.
- Most recent source test-only commit `836d83281dbcec4ddd4e5183b92f035982c131a3` (followed by documentation-only commit). Automatic exact-new-head Android CI `#45070731834` was **IN_PROGRESS** on first check. Do NOT transfer previous source CI PASS from `130d9d65` or `0263ca87` to this schema-changing head.
- HARD HOLDS: BETA HOLD; no financial POST, no original UUID replay, no deletion/production/owner device/Contabo/merge/deploy. Independent QA must actually execute ten SQLite Android instrumentation cases on separately approved disposable emulator/device and verify v1 upgrade; two GET MockWebServer JVM tests and injected no-replay tests in CI. Source+unit green does not imply on-device acceptance.
