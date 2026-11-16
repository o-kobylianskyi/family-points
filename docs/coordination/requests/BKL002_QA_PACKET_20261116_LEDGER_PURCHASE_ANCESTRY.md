# BKL002-QA-20261116-18 — Source-verified REWARD_PURCHASE ledger ancestry

**Status:** READY_FOR_QA, developer Maven/Docker NOT_RUN. Backend P0 → independent QA, issue #15. Test-only read-only financial verifier; absolutely no production replay, reset, VERIFIED or real-family modifications.

## Verified product source semantics (from production code)
- `PointTransactionSourceType` enum includes `MANUAL`, `TASK`, `TASK_MILESTONE`, `TASK_PENALTY`, `LEARNING`, `ACHIEVEMENT`, `ACCESS_PURCHASE`, `REWARD_PURCHASE`, `POINT_EXCHANGE`, `REFUND`, `PENALTY_REVERSAL`, `SYSTEM`. Do not claim any unsupported type is handled.
- `RewardService.purchaseReward` and request-driven purchase path call `pointService.spend(..., PointTransactionSourceType.REWARD_PURCHASE, purchase.getId(), ...)`. `PointService.spend` persists a **negative** amount and `PointTransactionType.SPEND`. Thus `REWARD_PURCHASE.source_id` is purchase ID, not reward definition, reward request, task or receipt ID.
- A valid source proof therefore needs both `POINT_LEDGER`, `POINT_TYPES`, `REWARD_PURCHASES` in the **same authenticated source-family V50 archive** and must establish: ledger `source_id == reward_purchases.id`, same member ID, same point_type ID, `ledger.amount == -purchase.price_amount` with strictly positive int-range price, and transaction `type=SPEND`. This does not imply a verified target member/currency/purchase identity.

## Source code changes
- `DisposableLedgerSourceGraphVerifier.java` prior `5928292`, stricter TASK definition and bounded amounts `79731d7`, purchase proof `c99371a`: READ-ONLY AES preflight. Links TASK to source task instance + owning TASK_DEFINITIONS in source tenant. MANUAL with null source_id. New REWARD_PURCHASE original purchase ID, member, currency and exact signed amount/type. REFUND, POINT_EXCHANGE, TASK_MILESTONE etc still fail closed until actual producers and history are mapped. No database queries or writes, no target native replay.
- `DisposableLedgerSourceGraphVerifierTest.java` `68bcae0`, corrected fixture syntax `b609809`, purchase tests `87de74a`: authenticated source TASK, missing/foreign task definition, out-of-int amount, valid REWARD_PURCHASE and wrong member/price/point_type/amount/sign/type. Existing source missing, type unsupported and MANUAL negative tests retained.
- Product code and database schema unchanged. `WorkspaceFinancialRestoreClosureService.replaySupported=false`; production `restorable=false` remains. No production restoration of the purchase/ledger graph can be inferred from a PASS.

## Pinned QA run (Java21)
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=DisposableLedgerSourceGraphVerifierTest,DisposablePointLedgerCrossDatabasePostgresTest,DisposableRewardCategoryCrossDatabasePostgresTest,DisposableRewardRequirementCrossDatabasePostgresTest,DisposableRewardObligationCrossDatabasePostgresTest,ProtectiveBackupRewardHistoryClosureTest,WorkspacePointLedgerSourceClosurePostgresTest" test
git rev-parse HEAD
```

Acceptance: exact current HEAD pinned before/after; test report totals FAIL/ERROR/SKIP and reproducible failures to #15; verified source purchase positive and all negatives; previous category source sorting fix `2539bf9` and financial #16 still pass. If Java or Compose failed, do not mark all-four CI green based on docs-only run.

**Still P0-blocked:** target purchase/source_id remapping, refund/exchange historical semantics, active currency policy (code alone is insufficient), full point+group+reputation ledger reconstruction, durable protected backup/VERIFIED and fresh owner reauth. Reset execute HTTP423, BETA HOLD.
