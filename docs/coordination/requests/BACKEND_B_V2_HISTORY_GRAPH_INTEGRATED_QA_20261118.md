# Backend B — IMP-01a.2 integrated real v2 source-history core (large package)

**Date:** 2026-11-18. **Status: SOURCE COMMITTED / EXACT-SHA QA PENDING.** This is NOT a complete all-23-category archive, Restore Engine, real owner permit or BETA release.

## Actual code delivered
- `PortableV2HistoryGraph.java` @`f0f01511`/`5ca806e7`: six historical categories bound to the SAME `family-points-export-v2` source snapshot and binary manifest root: `POINT_TYPES`, `TASK_DEFINITIONS`, `REWARD_DEFINITIONS`, `TASK_EXECUTIONS`, `REWARD_PURCHASES`, `POINT_LEDGER`. Rows are read by fixed server-owned, tenant-scoped SQL in a single source MVCC transaction; source category count/order/IDs and family-bound parent references are checked before sealing. Checks explicit optional historical member references, task/point currencies, reward/purchase ancestry and polymorphic TASK / REWARD_PURCHASE ledger source IDs. Unknown financial sources/refunds, missing original parent, reward-request backed purchases are **HARD BLOCK**, not dropped.
- Existing `PortableMemberAncestryV2.java` @`61fe594e` extended without changing original two-category vectors: recognizes **exactly** 2-category roster OR 8-category bounded source graph, refuses arbitrary 3–7/9+ category combinations, independently recomputes per-category content/closure SHA256, includes all 8 categories in the approved `PortableManifestRootV2` BE hash. No new protocol or second root library. Original two-category archives remain valid.
- Existing `WorkspacePortableAncestryV2Exporter.java` @`fe9b4341`: opt-in `exportWithHistory` pins same REPEATABLE_READ/epoch and appends 6 categories to original role+member rows before calculating/sealing root under existing FPB1 AES256-GCM. Old `export` and legacy v1 producer unchanged. Neither method enables Restore.
- `PortableV2HistoryGraphTest.java` @`93470dc2`/`c09003b2`/`f46534ec`: populated source with task, reward, purchase, multi-source point ledger, original active member, strict category/root parity and hard reject orphan/cross-tenant FK, omitted nullable, unsupported REFUND and request purchase, empty history.
- Existing `WorkspacePortableAncestryV2SourceLossPostgresTest.java` @`deccc2b8`: also produces full **bounded 8-category** v2 source file in a disposable PG18 source, stops/removes PostgreSQL, verifies historical file offline with existing authenticated inspector and preserves `restorable=false`. This PG case has zero financial rows by design; pure source-graph test has populated closure data.

## Important fail-closed boundaries
- Not all historical source categories/FKs are reviewed: group ledger/reputation/exchange/refunds, reward requests and obligations, media, actual historical TaskInstance snapshot economics and 17 unresolved member references still need explicit INCLUDE/DEPENDENCY/EXCLUDE disposition before a **FULL restorable archive** may be claimed. The current `exportWithHistory` is a bounded optional history snapshot, **not** final product backup.
- Even selected six categories are source provenance, not a target replay reconciler. Ledger economics/type parity, originator reauth, real RESTORE grant issuance and native target atomic ledger/epoch/receipt are separate pending major stages.
- New export methods do not create an HTTP route or change migrations, user accounts, cache, prod data or existing v1 behavior. No unsupported source is interpreted as complete. All execution test-only/disposable until Architecture, QA and owner signoff.

## Verification requested — one exact SHA, non-skipped CI
```powershell
mvn -Dtest=PortableManifestRootV2Test,PortableMemberAncestryV2Test,PortableV2HistoryGraphTest,WorkspacePortableAncestryV2SourceLossPostgresTest test
```
Then fully executed Java JDK21/PostgreSQL18, Web, Android, Compose, Release checks and exact SHA attestation **on the same immutable SHA**; capture tests/failures/errors/skipped and first stack. Specific note: existing golden `PortableMemberAncestryV2Test` 2/2 and full 749/749 passed in QA-068 CI [#44290621478](https://github.com/o-kobylianskyi/family-points/actions/runs/44290621478) @`89b0f8e1` **before** these source extension commits, not evidence that this package passed.

**HOLD:** `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD. Never execute live family reset or restore.
