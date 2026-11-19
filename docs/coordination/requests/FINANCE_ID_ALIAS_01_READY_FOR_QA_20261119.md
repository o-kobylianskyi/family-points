# FINANCE-ID-ALIAS-01 — final READY_FOR_QA (2026-11-19)

**Backend B owner, QA P2 test fixture coverage correction.** Architect assignment [Issue #15 #7006281903](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-7006281903). Prior independent QA confirmed exact issue [#7005981324](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-7005981324): source and target independent PG18 sequences could produce identical numeric IDs, masking true FK remap.

**FROZEN SHA:** `e99b8402890c3a4e913d757ffb5345002a611231`, [Draft PR #38](https://github.com/o-kobylianskyi/family-points/pull/38), branch `backend-b/finance-id-alias-01-nonidentity`, based EXACTLY on frozen PR37 `d708bd52d1738b3d401c71fb0312223a5fe26402`. Single modified test file `src/test/java/com/olehkobylianskyi/familypoints/service/WorkspaceNativeV2RestoreFinanceMatrixPostgresTest.java`. **No production Restore/permit code, migration, Retention/Web/Android or original PR37 changed.**

## Actual fixture and assertions
- Two distinct disposable PostgreSQL 18 databases remain, with authenticated 8-category FPB1 AES-GCM export and source DB physically stopped before authenticated target owner grant and native restore.
- Before creating the real target family, insert a legitimate disposable OTHER family with multiple members/currencies and task/reward definition, task instance and purchase. This advances target identity sequences without creating any conflicting business row in the real target workspace.
- Explicitly assert `sourceMember != targetMember` for BOTH original members and each target ID is outside the entire set of source members. Same for BOTH point denomination IDs.
- Independently query two target restored task definitions, two reward definitions, two task instances and two reward purchases, assert each restored parent/event ID differs from its exact source counterpart. Assert child task-instance and purchase native FKs reference reviewed target member and correct mapped target parent IDs.
- Independently query polymorphic target `point_transactions.source_id` for both TASK (+5 and +9) and REWARD_PURCHASE (-4 and -11) ledger entries, confirming actual target task-instance/reward-purchase event IDs rather than original numeric event IDs.
- Preserve exact signed 2-member×2-currency totals POINTS/MINUTES: **member1 +3/+7, member2 +11/−2**. Existing tamper/wrong key/foreign member/wrong currency/prepopulated target/stale epoch/DB-expired permit/atomic rollback/parallel 2-writer one receipt/replay rejection are still executed in the same test.
- Explicitly assert padding-family task/reward rows remain intact.

## Immutable actual CI evidence
[GitHub Actions run #4653](https://github.com/o-kobylianskyi/family-points/actions/runs/44505301209) for exact `e99b8402890c3a4e913d757ffb5345002a611231`: **7/7 GitHub jobs SUCCESS** (Java 21 / Testcontainers PostgreSQL 18, Android, Web, Compose, full exact-SHA attestation and report). Maven **778 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**. The corrected `WorkspaceNativeV2RestoreFinanceMatrixPostgresTest` actually ran **1/1 PASS** (3.839 sec). The earlier PR37 778/778 CI #4644 still stands but did not test the new nonidentity guarantee. No redundant full run.

**Disposition:** `FINANCE_ID_ALIAS_01_READY_FOR_QA` (not independent QA accepted yet). Request one independent exact-SHA code/CI review of only this P2 fixture and remapped FK evidence, then Architecture controls conflict-aware integration. No need to run a fresh expensive CI on same exact SHA. This verifies bounded 8-category synthetic restoration only; externally provider-signed Google JWK/aud/azp login E2E, unsupported source graph beyond safe closure and authenticated Chromium browser transitions remain separate unverified release gates.

**Safety:** Draft/unmerged; `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, physical purge OFF, BETA HOLD. Never run against a real family or production DB.
