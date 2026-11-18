# Backend B QA-075 — both P0 corrections: exact-SHA full CI acceptance

**2026-11-18 · Source implemented, actual new CI result PENDING.**

## P0 1 — genuine source tenant guard
- Product fix in `PortableV2HistoryGraph.attest` @76773efd: every source row of `POINT_TYPES`, `TASK_DEFINITIONS`, `REWARD_DEFINITIONS` has mandatory numeric `family_id` matching source workspace; missing/null/numeric-string/foreign rejected before canonical SHA/root.
- Existing active `PortableV2HistoryGraphTest` @0d7c7a40 adds 12 independent negative cases + positive 8-category digest verification, matching independent QA Draft PR #21. QA PR remains unmerged until QA decides.

## P0 2 — real PostgreSQL18 nonempty source loss
- Actual migrated JDBC fixture `WorkspacePortableAncestryV2SourceLossPostgresTest` @911d9317 inserts original member/role, point type, task definition, reward definition, task instance, reward purchase, three point transactions (TASK +5 / REWARD_PURCHASE -4 / MANUAL +2), and separate neighbor family currency.
- Uses actual checked JPA schema column names and `PointTransactionSourceType`/`PointTransactionType` enum values; exports `exportWithHistory` using source PG18 REPEATABLE_READ, then `container.stop()` and inspects sealed FPB1 v2 WITHOUT DATABASE.
- Assert 8 authenticated category counts (1/2 roster, 1 each of 5 other history categories, 3 POINT_LEDGER), positive original C SourceArchive manifest/content digests, no neighbor currency or member name, immutable verified map, `restorable=false`.
- Negative @c9bcfc19: re-seal modified first ledger `source_id` while retaining old source manifest/root and prove inspector denies offline. Original wrong key/family/ciphertext and roster mutation tests remain.
- **QA status**: fixture is code delivered, NOT an observed PG18 PASS until targeted and full exact-SHA logs confirm. If SQL fixtures reveal schema/default drift, fix only verified first stack, never relax provenance or finance invariants.

## Tests to run on same immutable SHA
```powershell
mvn -Dtest=PortableV2HistoryGraphTest,PortableManifestRootV2Test,PortableMemberAncestryV2Test,WorkspacePortableAncestryV2SourceLossPostgresTest test
```
Then one **FULL non-skipped** Maven PG18 + Web + Android + Compose and exact-SHA attestation. Report run ID, SHA, total F/E/S, first error stack and actual nonempty source-loss test results. This commit is the full CI trigger after final fixes.

QA-068 prior baseline #44290621478 @89b0f8e1 was 749/749 but predates QA-075 changes. Independent QA Draft PR #21 should be run after product changes without merging before green. No production Restore, Reset Execute HTTP423, `restorable=false`, `replaySupported=false`, BETA HOLD.