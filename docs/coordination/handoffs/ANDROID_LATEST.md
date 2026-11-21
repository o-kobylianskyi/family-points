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
