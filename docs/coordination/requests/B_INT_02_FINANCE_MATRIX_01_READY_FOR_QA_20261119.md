# B-INT-02-FINANCE-MATRIX-01 — one frozen READY_FOR_QA (2026-11-19)

**Source:** draft [PR #37](https://github.com/o-kobylianskyi/family-points/pull/37), branch `backend-b/b-int-02-finance-matrix-01`, exact **`d708bd52d1738b3d401c71fb0312223a5fe26402`**, based on QA conditionally accepted B-INT-02-FIX [PR #36](https://github.com/o-kobylianskyi/family-points/pull/36) `e487d909d59ef7f0637bf04e47f911a14395861e`. No changes to frozen PR36, PR35, main production code, Retention/Web/Android, schema, RESTORE flags.

**Only new file:** `src/test/java/com/olehkobylianskyi/familypoints/service/WorkspaceNativeV2RestoreFinanceMatrixPostgresTest.java`. Production engine remains unchanged because no defect reproduced.

## Real disposable two-PG18 test
1. Two migrated PG18 containers with distinct original family ID and target family; create original **2 source members, 2 point types** `POINTS` and `MINUTES`, 2 task definitions, 2 reward definitions, 2 task instances, 2 reward purchases and **7** ledger transactions with polymorphic task/purchase source FK.
2. `WorkspacePortableAncestryV2Exporter.exportWithHistory` creates authenticated AES-GCM FPB1 eight-category source; **source PostgreSQL container stopped** before target consent/restore.
3. Target has 2 owner-reviewed member IDs and 2 reviewed currency IDs with matching code, different target-row identities from their source mappings. Real original-owner binding, BCrypt fresh password, signed selection/member digest, durable one-use operation permit. Real `WorkspaceNativeV2RestoreEngine.restore` under target PostgreSQL transaction remaps full native FK parent graph.
4. Exact four account/currency ledger totals asserted (member1/POINTS `+3`, member1/MINUTES `+7`, member2/POINTS `+11`, member2/MINUTES `-2`), 2 task and 2 reward definitions, 2 task instances, 7 ledger records, receipt/epoch+1 and immutable one-use behavior.
5. Target negative denials using real DB: wrong source decrypt key, modified ciphertext, member mapped to foreign workspace, currency mapped to wrong code, pre-existing target task definition (original row preserved and permit unconsumed), stale target data epoch, **DB-clock-expired stored permit** (no permit consumption); after restoring valid fixture, injected target rollback then two concurrent PG connections compete for one permit, **one commit / one denial**, one receipt, no duplicate ledger, replay rejected.

## Actual immutable exact-SHA CI evidence
[Actions #4644](https://github.com/o-kobylianskyi/family-points/actions/runs/44499403001) on `d708bd52`: **all 7 jobs SUCCESS**, Java 21 / PostgreSQL18 Testcontainers `778 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS`, Web/Android/Compose SUCCESS and full exact-SHA attestation SUCCESS. New `WorkspaceNativeV2RestoreFinanceMatrixPostgresTest` **1/1 PASS** (5.600s), source-loss original single-member test and Google budget tests continue passing as part of 778 suite. No duplicate CI required.

## QA scope and remaining limitations
**B-INT-02-FINANCE-MATRIX-01 READY_FOR_QA**, not BETA, deployment or full feature approval. Request independent source QA verify the exact new test and 2x2 source/target mappings, real source stop, monetary/currency parity, actual wrong tenant/epoch/permit DB negatives, and no production code changes. Underlying RESTORE support remains **bounded eight-category**; signed source with populated unsupported dependencies still fails closed rather than claiming universal restoration. Live externally signed provider Google ID-token JWK/audience/azp successful login E2E remains **UNVERIFIED**. Exact five-minute permit boundary is exercised via DB-expired permit but no real five-minute wait. No live family data or production endpoint used. PR remains Draft/unmerged.

**Safety:** `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, physical purge OFF, BETA HOLD. No merge, deployment or destructive action authorized.
