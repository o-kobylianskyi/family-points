# Backend B — Full history-core v2 integrated package, pinned QA request

**Date 2026-11-18.** This file is a **single final [full-ci] trigger** for the integrated code, NOT a claim of tests run or completed product Restore.

## Concrete deliverables
1. New `PortableV2HistoryGraph.java` @f0f01511 / @5ca806e7 / @15996c9f: six real historical categories under the same IMP-01 manifest: POINT_TYPES, TASK_DEFINITIONS, REWARD_DEFINITIONS, TASK_EXECUTIONS, REWARD_PURCHASES, POINT_LEDGER. Exact source IDs, family/source member ancestry, task original assignee, all four task-definition actor references, point types, reward/purchase lineage and polymorphic TASK/REWARD_PURCHASE ledger parent proof. Unknown REFUND/EXCHANGE/request purchase or omitted source parent → FAIL CLOSED. Fixed tenant-scoped JDBC; cross-family task/purchase rows hidden by member JOIN are rejected before collection, never silently discarded.
2. Existing `PortableMemberAncestryV2` @61fe594e: supported exact 2 (legacy v2 ancestry-only) or 8 category bounded history (2 roster + 6 finance/task) subsets, deterministic content+dependency digests included in SAME `PortableManifestRootV2` binary SHA256; no new hashing protocol. Every root is recalculated on the decoded AES-GCM authenticated plaintext.
3. Existing `WorkspacePortableAncestryV2Exporter.exportWithHistory` @fe9b4341: real opt-in single REPEATABLE_READ epoch-pinned export, same FPB1 envelope and key, legacy `export` remains unchanged. No HTTP route or mutation.
4. Existing `ProtectiveBackupImportInspector.AncestryV2Inspection` @9bcff432: immutable verified row counts, authentic `snapshotDigest=FPB1 manifest.contentSha256` and independently verified `manifestRootDigest`, `verifiedSourceArchiveV2()` uses C-owned `WorkspaceRestorePermitScopeV2.SourceArchive` with exact source workspace/schema/snapshot/epoch. **This is a typed source descriptor, NOT an owner-issued grant.** C canonical member mapping digest remains via reviewed exact original roster.
5. New `PortableV2HistoryGraphTest` @93470dc2 / @f46534ec: populated financial/task graph, tampering and cross-family/orphan/omitted optional categories/unsupported refund/request with explicit fail-closed outcome. Real `WorkspacePortableAncestryV2SourceLossPostgresTest` @deccc2b8 / @f590f0ff / @f7ca2217: both v2 modes use real disposable PostgreSQL18 then SOURCE DB STOP; offline inspector verifies immutable category counts, C SourceArchive root/bytes, retains existing tamper/wrong key/wrong family and mapping tests.

## QA acceptance command (JDK21, Docker PostgreSQL18)
```powershell
mvn -Dtest=PortableV2HistoryGraphTest,PortableManifestRootV2Test,PortableMemberAncestryV2Test,WorkspacePortableAncestryV2SourceLossPostgresTest test
```
Then real **non-skipped full four-component** Maven PG18, Web, Android, Compose, Testcontainers2 preflight, exact-SHA attestation. Pin *same latest immutable SHA* containing `15996c9fe4a42ca1ef782d3e84f741d236ea98d7` and this request; record run ID, job, total tests/failures/errors/skipped, first compilation/error stack, explicit named class outcomes. If red, Backend B resolves only reproducible owned failures and reruns full gate. A green scoped CI with skipped Maven is not sufficient.

QA-068 verified preceding full [#44290621478](https://github.com/o-kobylianskyi/family-points/actions/runs/44290621478) @89b0f8e1: **749/749 PASS** for ancestry-only source. It does NOT cover this 8-category new code.

## Critical definition of done vs unsupported scope
**Implemented:** genuine bounded v2 history source capture/cryptographic inspect with closed original member/task/reward/currency/point ledger parents, not complete full-category vNext. The user requested **all** historical categories; this package does NOT yet add all 23 legacy categories or 17 unresolved source-member FK dispositions. Media/group/reputation/reward obligations/refunds/exchange/task audit/reward requests remain not approved for full restore. Do not silently claim entire step 1 is complete or turn on `restorable`.

**Next:** after real green same-SHA QA, expand existing exporter+inspector to the remaining reviewed category graph with privacy/FK classification or explicitly block unsupported categories under the final contract. Then only after Step 1 complete proceed to server-side owner reauth/one-use grant and native atomic restore/finance/epoch/receipt.

**PRODUCTION HARD HOLD:** `restorable=false`, `replaySupported=false`, RESET Execute HTTP423, BETA HOLD. No live-family Restore, migration, filesystem purge or deployment.
