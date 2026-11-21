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
